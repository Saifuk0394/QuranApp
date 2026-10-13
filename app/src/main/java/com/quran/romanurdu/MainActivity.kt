package com.quran.romanurdu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quran.romanurdu.ui.screens.AsmaScreen
import com.quran.romanurdu.ui.screens.BookmarksScreen
import com.quran.romanurdu.ui.screens.HadithScreen
import com.quran.romanurdu.ui.screens.KhatmScreen
import com.quran.romanurdu.ui.screens.PrayerTimesScreen
import com.quran.romanurdu.ui.screens.QiblaScreen
import com.quran.romanurdu.ui.screens.ReaderScreen
import com.quran.romanurdu.ui.screens.SearchScreen
import com.quran.romanurdu.ui.screens.SetupScreen
import com.quran.romanurdu.ui.screens.SurahListScreen
import com.quran.romanurdu.ui.theme.QuranAppTheme
import com.quran.romanurdu.viewmodel.AudioViewModel
import com.quran.romanurdu.viewmodel.HadithViewModel
import com.quran.romanurdu.viewmodel.QuranViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: QuranViewModel = viewModel()
            val hadithViewModel: HadithViewModel = viewModel()
            val audioViewModel: AudioViewModel = viewModel()
            val darkMode by viewModel.darkMode.collectAsState()
            val isSetupComplete by viewModel.isSetupComplete.collectAsState()

            QuranAppTheme(darkTheme = darkMode) {
                val navController = rememberNavController()
                val startDestination = if (isSetupComplete) "reader" else "setup"

                NavHost(navController = navController, startDestination = startDestination) {

                    composable("setup") {
                        SetupScreen(viewModel = viewModel, navController = navController)
                    }

                    composable("reader") {
                        ReaderScreen(
                            viewModel = viewModel,
                            navController = navController,
                            audioViewModel = audioViewModel
                        )
                    }

                    composable("surah_list") {
                        SurahListScreen(viewModel = viewModel, navController = navController)
                    }

                    composable("bookmarks") {
                        BookmarksScreen(viewModel = viewModel, navController = navController)
                    }

                    composable("hadith") {
                        HadithScreen(
                            navController = navController,
                            hadithViewModel = hadithViewModel
                        )
                    }

                    // ── v5 screens ──────────────────────────────────────────

                    composable("asma") {
                        AsmaScreen(navController = navController)
                    }

                    composable("qibla") {
                        QiblaScreen(navController = navController)
                    }

                    composable("search") {
                        SearchScreen(
                            navController = navController,
                            viewModel = viewModel,
                            hadithViewModel = hadithViewModel
                        )
                    }

                    // ── v6 screens ──────────────────────────────────────────

                    composable("prayer_times") {
                        PrayerTimesScreen(navController = navController)
                    }

                    composable("khatm") {
                        KhatmScreen(viewModel = viewModel, navController = navController)
                    }
                }
            }
        }
    }
}
