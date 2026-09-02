package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.LitoralNovelasTheme
import com.example.ui.viewmodel.DramaViewModel
import com.example.ui.viewmodel.VideoUploadViewModel

class MainActivity : ComponentActivity() {
    private val dramaViewModel: DramaViewModel by viewModels()
    private val uploadViewModel: VideoUploadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LitoralNovelasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    MainScreen(
                        dramaViewModel = dramaViewModel,
                        uploadViewModel = uploadViewModel
                    )
                }
            }
        }
    }
}
