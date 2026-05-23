package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.StreamHubDashboard
import com.example.ui.StreamHubAuthScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.StreamHubViewModel
import com.example.viewmodel.StreamHubViewModelFactory

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        // Grab the lazy unified repository from Application class
        val appRepository = (application as StreamHubApplication).repository
        
        // Instantiate our StreamHubViewModel using its Factory
        val viewModel: StreamHubViewModel by viewModels {
            StreamHubViewModelFactory(appRepository)
        }
        
        setContent {
            MyApplicationTheme {
                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = com.example.ui.CosmicBlack // Base background color
                ) { innerPadding ->
                    if (currentUser == null) {
                        StreamHubAuthScreen(
                            viewModel = viewModel,
                            onAuthSuccess = {
                                // Handled automatically by Flow
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    } else {
                        StreamHubDashboard(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
