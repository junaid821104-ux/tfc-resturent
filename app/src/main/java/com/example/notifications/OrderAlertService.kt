package com.example.notifications

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.R
import com.example.data.firebase.UserRepository
import com.example.data.model.EmailDocIdHelper
import com.example.data.model.Order
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OrderAlertService : Service() {
    companion object {
        private const val TAG = "OrderAlertService"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ordersListener: ListenerRegistration? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = OrderAlertManager.buildMonitorNotification(this)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    OrderAlertManager.MONITOR_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(OrderAlertManager.MONITOR_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Foreground transition failed: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }

        attachOwnerOrderListenerIfNeeded()
        return START_STICKY
    }

    private fun attachOwnerOrderListenerIfNeeded() {
        if (ordersListener != null) return
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            stopSelf()
            return
        }

        val db = FirebaseFirestore.getInstance(getString(R.string.firestore_database_id))
        serviceScope.launch {
            val isOwner = verifyOwnerAuthorization(db, currentUser.uid, currentUser.email, currentUser.isEmailVerified)
            if (!isOwner) {
                Log.w(TAG, "Stopping OrderAlertService: signed-in user is not authorized Owner.")
                OrderAlarmPlayer.stop()
                stopSelf()
                return@launch
            }

            if (ordersListener == null) {
                ordersListener = db.collection("orders")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(TAG, "Order monitor listener error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot == null) return@addSnapshotListener
                        val orders = snapshot.toObjects(
                            Order::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        OrderAlertManager.handleLiveOrdersUpdate(
                            context = this@OrderAlertService,
                            orders = orders,
                            triggerAlarmAndNotification = true
                        )
                    }
            }
        }
    }

    private suspend fun verifyOwnerAuthorization(
        db: FirebaseFirestore,
        uid: String,
        email: String?,
        emailVerified: Boolean
    ): Boolean {
        val cleanEmail = email?.trim().orEmpty()
        if (emailVerified && cleanEmail.equals(UserRepository.BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)) {
            return true
        }
        return try {
            val uidAdmin = db.collection("admins").document(uid).get().await().exists()
            if (uidAdmin) return true
            if (emailVerified && cleanEmail.isNotBlank()) {
                val emailDoc = EmailDocIdHelper.fromEmail(cleanEmail)
                db.collection("admins").document(emailDoc).get().await().exists()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Owner verification check failed: ${e.message}")
            false
        }
    }

    override fun onDestroy() {
        ordersListener?.remove()
        ordersListener = null
        serviceScope.cancel()
        super.onDestroy()
    }
}
