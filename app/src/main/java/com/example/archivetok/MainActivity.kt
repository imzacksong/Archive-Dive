package com.example.archivetok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.archivetok.ui.theme.ArchiveTokTheme
import com.example.archivetok.ui.VideoFeedScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.archivetok.ui.VideoFeedViewModel
import com.example.archivetok.ui.OnboardingScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArchiveTokTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: VideoFeedViewModel = hiltViewModel()
                    var isOnboardingCompleted by remember { mutableStateOf(viewModel.isOnboardingCompleted()) }

                    if (!isOnboardingCompleted) {
                        OnboardingScreen(
                            onFinish = { selectedTags ->
                                viewModel.completeOnboarding(selectedTags)
                                isOnboardingCompleted = true
                            }
                        )
                    } else {
                        VideoFeedScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
