package com.example.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import java.util.Locale

object OrderAlertManager {
    private const val TAG = "OrderAlertManager"

    const val CHANNEL_MONITOR_ID = "tfc_order_monitor_v1"
    const val CHANNEL_NEW_ORDER_ID = "tfc_new_order_alarm_v1"

    const val MONITOR_NOTIFICATION_ID = 41001
    private const val NEW_ORDER_BASE_ID = 42000

    const val EXTRA_OPEN_OWNER_ORDERS = "extra_open_owner_orders"
    const val EXTRA_ORDER_ID = "extra_order_id"
    const val ACTION_SILENCE_ALARM = "com.example.tfc.ACTION_SILENCE_ALARM"
    const val ACTION_ACCEPT_ORDER = "com.example.tfc.ACTION_ACCEPT_ORDER"

    private val lock = Any()
    private var baselineSeeded = false
    private val knownOrderIds = mutableSetOf<String>()
    private val notifiedOrderIds = mutableSetOf<String>()

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val monitorChannel = NotificationChannel(
            CHANNEL_MONITOR_ID,
            "TFC Owner Order Listener",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps live Firestore monitoring active for incoming TFC customer orders."
            setShowBadge(false)
        }

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val alarmChannel = NotificationChannel(
            CHANNEL_NEW_ORDER_ID,
            "TFC New Order Alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Instant loud alarm alerts when a customer places a new TFC order."
            enableLights(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 600L, 250L, 600L, 250L, 800L)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
            if (alarmSound != null) {
                setSound(
                    alarmSound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
        }

        manager.createNotificationChannel(monitorChannel)
        manager.createNotificationChannel(alarmChannel)
    }

    fun buildMonitorNotification(context: Context): Notification {
        ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_OWNER_ORDERS, true)
        }
        val openPending = PendingIntent.getActivity(
            context,
            1001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val silenceIntent = Intent(context, OrderAlertActionReceiver::class.java).apply {
            action = ACTION_SILENCE_ALARM
        }
        val silencePending = PendingIntent.getBroadcast(
            context,
            1002,
            silenceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_MONITOR_ID)
            .setSmallIcon(R.drawable.ic_notification_tfc)
            .setContentTitle("TFC Owner Order Alerts Active")
            .setContentText("Listening in real time for new customer orders.")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .addAction(0, "Open Orders", openPending)
            .addAction(0, "Silence Alarm", silencePending)
            .build()
    }

    fun handleLiveOrdersUpdate(
        context: Context,
        orders: List<Order>,
        triggerAlarmAndNotification: Boolean
    ) {
        val newlyArrived = synchronized(lock) {
            if (!baselineSeeded) {
                knownOrderIds.addAll(orders.map { it.id })
                baselineSeeded = true
                emptyList()
            } else {
                val fresh = orders.filter { order ->
                    order.id.isNotBlank() &&
                        order.id !in knownOrderIds &&
                        order.id !in notifiedOrderIds &&
                        OrderStatus.fromRaw(order.status) == OrderStatus.ORDER_PLACED
                }
                knownOrderIds.addAll(orders.map { it.id })
                fresh.forEach { notifiedOrderIds.add(it.id) }
                fresh
            }
        }

        if (triggerAlarmAndNotification && newlyArrived.isNotEmpty()) {
            newlyArrived.forEach { order ->
                postNewOrderNotification(context, order)
            }
            OrderAlarmPlayer.playLoudAlarm(context)
        }

        // Cancel notifications / silence alarm if no Order Placed items remain pending
        val stillPending = orders.any { OrderStatus.fromRaw(it.status) == OrderStatus.ORDER_PLACED }
        if (!stillPending && OrderAlarmPlayer.isPlaying.value) {
            OrderAlarmPlayer.stop()
        }
    }

    fun postNewOrderNotification(context: Context, order: Order) {
        ensureChannels(context)
        val appContext = context.applicationContext
        val hasNotifyPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!hasNotifyPerm) return

        val notifId = notificationIdForOrder(order.id)
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_OWNER_ORDERS, true)
            putExtra(EXTRA_ORDER_ID, order.id)
        }
        val openPending = PendingIntent.getActivity(
            appContext,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acceptIntent = Intent(appContext, OrderAlertActionReceiver::class.java).apply {
            action = ACTION_ACCEPT_ORDER
            putExtra(EXTRA_ORDER_ID, order.id)
        }
        val acceptPending = PendingIntent.getBroadcast(
            appContext,
            notifId + 1,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val silenceIntent = Intent(appContext, OrderAlertActionReceiver::class.java).apply {
            action = ACTION_SILENCE_ALARM
            putExtra(EXTRA_ORDER_ID, order.id)
        }
        val silencePending = PendingIntent.getBroadcast(
            appContext,
            notifId + 2,
            silenceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val itemCount = order.items.sumOf { it.quantity }.coerceAtLeast(1L)
        val typeLabel = if (OrderType.fromRaw(order.orderType) == OrderType.PICKUP) "Self Pickup" else "Home Delivery"
        val itemsSummary = order.items.joinToString(", ") { "${it.quantity}x ${it.name}" }
        val totalStr = String.format(Locale.US, "₹%.0f", order.totalAmount)
        val bigText = buildString {
            append("${order.customerName} (${order.customerPhone.ifBlank { order.customerEmail }})\n")
            append("$typeLabel • $totalStr • ${order.paymentMethod}\n")
            append(itemsSummary)
            if (!order.deliveryAddress.isNullOrBlank()) {
                append("\nAddress: ${order.deliveryAddress}")
            }
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_NEW_ORDER_ID)
            .setSmallIcon(R.drawable.ic_notification_tfc)
            .setContentTitle("New Order ${order.orderNumber} • $totalStr")
            .setContentText("${order.customerName} • $itemCount item(s) • $typeLabel")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(0, "Accept Order", acceptPending)
            .addAction(0, "Silence Alarm", silencePending)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(notifId, notification)
        } catch (se: SecurityException) {
            Log.w(TAG, "Missing notification permission: ${se.message}")
        }
    }

    fun cancelOrderNotification(context: Context, orderId: String) {
        if (orderId.isBlank()) return
        NotificationManagerCompat.from(context.applicationContext)
            .cancel(notificationIdForOrder(orderId))
    }

    fun notificationIdForOrder(orderId: String): Int =
        NEW_ORDER_BASE_ID + (orderId.hashCode() and 0x7FFF)

    fun startServiceSafely(context: Context) {
        val appContext = context.applicationContext
        ensureChannels(appContext)
        val intent = Intent(appContext, OrderAlertService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
        } catch (e: Exception) {
            // Android 12+ may disallow foreground service starts from certain background states;
            // live in-app Firestore listener in AuthenticatedTfcShell remains active.
            Log.w(TAG, "Unable to start OrderAlertService: ${e.message}")
        }
    }

    fun stopService(context: Context) {
        val appContext = context.applicationContext
        OrderAlarmPlayer.stop()
        runCatching {
            appContext.stopService(Intent(appContext, OrderAlertService::class.java))
        }
    }

    fun reset() {
        OrderAlarmPlayer.stop()
        synchronized(lock) {
            baselineSeeded = false
            knownOrderIds.clear()
            notifiedOrderIds.clear()
        }
    }
}
