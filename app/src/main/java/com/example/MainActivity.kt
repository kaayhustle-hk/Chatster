package com.example

import android.os.Bundle
import android.content.Intent
import android.net.Uri
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
    
    private lateinit var viewModel: StreamHubViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        // Grab the lazy unified repository from Application class
        val appRepository = (application as StreamHubApplication).repository
        
        // Instantiate our StreamHubViewModel using its Factory
        val vm: StreamHubViewModel by viewModels {
            StreamHubViewModelFactory(appRepository)
        }
        viewModel = vm
        
        handleDeepLink(intent)
        
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        try {
            val uri = intent?.data ?: return
            if (uri.scheme == "streamhub" && uri.host == "auth") {
                // Implicit OAuth returns access token in fragment part of URI
                val fragment = uri.fragment ?: uri.encodedFragment
                val accessToken = fragment?.split("&")?.firstOrNull { it.startsWith("access_token=") }?.substringAfter("access_token=")
                val code = if (uri.isHierarchical) uri.getQueryParameter("code") else null
                
                val token = accessToken ?: code
                if (token != null) {
                    val platformRaw = uri.path?.removePrefix("/") ?: "twitch"
                    viewModel.handleOAuthToken(platformRaw, token)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
