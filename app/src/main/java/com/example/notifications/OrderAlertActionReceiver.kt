package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.R
import com.example.data.firebase.OrderRepository
import com.example.data.model.OrderStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OrderAlertActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val orderId = intent.getStringExtra(OrderAlertManager.EXTRA_ORDER_ID).orEmpty()
        when (intent.action) {
            OrderAlertManager.ACTION_SILENCE_ALARM -> {
                OrderAlarmPlayer.stop()
                if (orderId.isNotBlank()) {
                    OrderAlertManager.cancelOrderNotification(context, orderId)
                }
            }
            OrderAlertManager.ACTION_ACCEPT_ORDER -> {
                OrderAlarmPlayer.stop()
                if (orderId.isBlank() || Firebase.auth.currentUser == null) return
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = FirebaseFirestore.getInstance(
                            context.applicationContext.getString(R.string.firestore_database_id)
                        )
                        val result = OrderRepository(db).updateOrderStatus(orderId, OrderStatus.ACCEPTED)
                        OrderAlertManager.cancelOrderNotification(context, orderId)
                        CoroutineScope(Dispatchers.Main).launch {
                            val msg = if (result.isSuccess) {
                                "Order accepted"
                            } else {
                                "Open TFC Owner Dashboard to update this order"
                            }
                            Toast.makeText(context.applicationContext, msg, Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
