package com.ilya.safarshots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilya.safarshots.core.settings.AppSettingsRepository
import com.ilya.safarshots.design.GeoMapShotsTheme
import com.ilya.safarshots.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: AppSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.observe().collectAsStateWithLifecycle()
            GeoMapShotsTheme(dynamicColor = settings.dynamicColor) {
                AppNavGraph(modifier = Modifier)
            }
        }
    }
}
