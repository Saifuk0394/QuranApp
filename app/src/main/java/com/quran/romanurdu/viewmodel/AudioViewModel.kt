package com.quran.romanurdu.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.quran.romanurdu.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AudioState {
    data object Idle : AudioState()
    data object Loading : AudioState()
    data class Playing(val surahNumber: Int, val ayahIndex: Int) : AudioState()
    data class Paused(val surahNumber: Int, val ayahIndex: Int) : AudioState()
    data class Error(val message: String) : AudioState()
}

/**
 * Manages verse-by-verse audio playback for a surah.
 *
 * Strategy: fetch per-ayah MP3 URLs from AlQuran.cloud (ar.alafasy edition),
 * queue them in ExoPlayer as a playlist. When a media item transitions,
 * update the currentAyahIndex so the UI highlights the correct verse.
 */
class AudioViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        // Mishary Rashid Al-Afasy — highest quality, most popular reciter on the API
        const val RECITER = "ar.alafasy"
    }

    private val _audioState = MutableStateFlow<AudioState>(AudioState.Idle)
    val audioState: StateFlow<AudioState> = _audioState

    private val _currentAyahIndex = MutableStateFlow(-1)
    val currentAyahIndex: StateFlow<Int> = _currentAyahIndex

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var player: ExoPlayer? = null
    private var currentSurahNumber: Int = -1
    // Cached audio URLs: surah → list of per-ayah URLs in order
    private val audioUrlCache = mutableMapOf<Int, List<String>>()

    private fun getPlayer(context: Context): ExoPlayer {
        return player ?: ExoPlayer.Builder(context).build().also { p ->
            player = p
            p.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val idx = p.currentMediaItemIndex
                    _currentAyahIndex.value = idx
                    val surah = currentSurahNumber
                    _audioState.value = if (p.isPlaying)
                        AudioState.Playing(surah, idx)
                    else
                        AudioState.Paused(surah, idx)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            _audioState.value = AudioState.Idle
                            _currentAyahIndex.value = -1
                        }
                        Player.STATE_READY -> {
                            val idx = p.currentMediaItemIndex
                            _audioState.value = if (p.isPlaying)
                                AudioState.Playing(currentSurahNumber, idx)
                            else
                                AudioState.Paused(currentSurahNumber, idx)
                        }
                        else -> {}
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    val idx = p.currentMediaItemIndex
                    val surah = currentSurahNumber
                    _audioState.value = if (isPlaying)
                        AudioState.Playing(surah, idx)
                    else if (p.playbackState != Player.STATE_ENDED)
                        AudioState.Paused(surah, idx)
                }
            })
        }
    }

    fun playSurah(surahNumber: Int, startAyahIndex: Int = 0) {
        viewModelScope.launch {
            _isLoading.value = true
            _audioState.value = AudioState.Loading
            try {
                val urls = audioUrlCache.getOrPut(surahNumber) {
                    fetchAudioUrls(surahNumber)
                }
                if (urls.isEmpty()) {
                    _audioState.value = AudioState.Error("No audio available for this surah")
                    _isLoading.value = false
                    return@launch
                }

                val ctx = getApplication<Application>().applicationContext
                val p = getPlayer(ctx)
                currentSurahNumber = surahNumber

                p.stop()
                p.clearMediaItems()
                urls.forEachIndexed { idx, url ->
                    p.addMediaItem(MediaItem.fromUri(url))
                }
                p.seekTo(startAyahIndex, 0)
                p.prepare()
                p.playWhenReady = true
                _currentAyahIndex.value = startAyahIndex
            } catch (e: Exception) {
                _audioState.value = AudioState.Error("Could not load audio: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playFromAyah(ayahIndex: Int) {
        player?.let { p ->
            p.seekTo(ayahIndex, 0)
            p.play()
            _currentAyahIndex.value = ayahIndex
        }
    }

    fun togglePlayPause() {
        player?.let { p ->
            if (p.isPlaying) p.pause() else p.play()
        }
    }

    fun nextAyah() {
        player?.let { p ->
            if (p.hasNextMediaItem()) {
                p.seekToNextMediaItem()
                p.play()
            }
        }
    }

    fun previousAyah() {
        player?.let { p ->
            if (p.hasPreviousMediaItem()) {
                p.seekToPreviousMediaItem()
                p.play()
            }
        }
    }

    fun stop() {
        player?.stop()
        player?.clearMediaItems()
        _audioState.value = AudioState.Idle
        _currentAyahIndex.value = -1
    }

    private suspend fun fetchAudioUrls(surahNumber: Int): List<String> {
        val response = RetrofitClient.api.getSurahAudio(surahNumber, RECITER)
        val edition = response.data.firstOrNull() ?: return emptyList()
        return edition.ayahs.mapNotNull { it.audio }
    }

    override fun onCleared() {
        super.onCleared()
        player?.release()
        player = null
    }
}
