package com.example.bigdata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bigdata.ui.ParkingMapScreen
import com.example.bigdata.ui.theme.BigdataTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BigdataTheme {
                ParkingMapScreen()
            }
        }
    }
}
