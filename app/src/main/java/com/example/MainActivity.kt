package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.notifications.OrderAlarmPlayer
import com.example.notifications.OrderAlertManager
import com.example.ui.screens.customer.TfcAppRoot
import com.example.ui.theme.TfcRestaurantTheme

class MainActivity : ComponentActivity() {
    private var openOwnerOrdersRequest by mutableStateOf(false)
    private var notificationLaunchToken by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        OrderAlertManager.ensureChannels(this)
        handleIncomingIntent(intent)
        setContent {
            TfcRestaurantTheme {
                TfcAppRoot(
                    openOwnerOrdersRequest = openOwnerOrdersRequest,
                    notificationLaunchToken = notificationLaunchToken,
                    onConsumeOwnerOrdersRequest = { openOwnerOrdersRequest = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(OrderAlertManager.EXTRA_OPEN_OWNER_ORDERS, false) == true) {
            OrderAlarmPlayer.stop()
            openOwnerOrdersRequest = true
            notificationLaunchToken++
        }
    }
}
