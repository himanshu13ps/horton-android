package com.himanshu13ps.horton

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.himanshu13ps.horton.data.NoteDatabase
import com.himanshu13ps.horton.ui.AppScreen
import com.himanshu13ps.horton.ui.MainViewModel
import com.himanshu13ps.horton.service.RecordingService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = NoteDatabase.getDatabase(this)
        
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return MainViewModel(db.noteDao()) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        setContent {
            val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
            
            // Check for missing models on startup
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.checkModelsExist(this@MainActivity)
            }

            AppScreen(
                viewModel = viewModel,
                onStartRecording = {
                    val intent = Intent(this, RecordingService::class.java).apply {
                        action = RecordingService.ACTION_START
                    }
                    startService(intent)
                },
                onStopRecording = {
                    val intent = Intent(this, RecordingService::class.java).apply {
                        action = RecordingService.ACTION_STOP
                    }
                    startService(intent)
                }
            )
        }
    }
}
