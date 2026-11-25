package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PaymentMethod
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class OrderRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in before accessing orders.")

    fun observeUserOrders(userId: String): Flow<List<Order>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        emitAll(
            db.collection("orders")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.LIST, "orders")
                    throw error
                }
        )
    }

    fun observeOrder(orderId: String): Flow<Order?> = flow {
        val uid = requireUserId()
        require(orderId.isNotBlank()) { "Order ID cannot be blank" }
        val docRef = db.collection("orders").document(orderId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    fun observeAllOrders(): Flow<List<Order>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("orders")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.LIST, "orders")
                    throw error
                }
        )
    }

    suspend fun placeOrder(
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        orderType: OrderType,
        items: List<OrderItem>,
        subtotal: Double,
        discountAmount: Double,
        deliveryFee: Double,
        totalAmount: Double,
        couponCode: String?,
        cookingInstructions: String?,
        deliveryAddress: String?,
        landmark: String?,
        deliveryZoneId: String?,
        deliveryZoneName: String?,
        deliveryLat: Double?,
        deliveryLng: Double?,
        estimatedMinutes: Long,
        paymentMethod: PaymentMethod,
        paymentStatus: String
    ): Result<Order> {
        val docRef = db.collection("orders").document()
        return try {
            val uid = requireUserId()
            require(items.isNotEmpty() && items.size <= 50) { "Order must contain 1–50 items" }
            val actualDeliveryFee = if (orderType == OrderType.PICKUP) 0.0 else deliveryFee.coerceAtLeast(0.0)
            val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val shortCode = UUID.randomUUID().toString().replace("-", "").take(4).uppercase()
            val orderNumber = "TFC-$dateStamp-$shortCode"

            val payload = mutableMapOf<String, Any?>(
                "orderNumber" to orderNumber,
                "userId" to uid,
                "customerName" to customerName.trim().take(80).ifBlank { "TFC Guest" },
                "customerPhone" to customerPhone.trim().take(30),
                "customerEmail" to customerEmail.trim().take(120),
                "orderType" to orderType.raw,
                "status" to OrderStatus.ORDER_PLACED.raw,
                "items" to items.map { it.toMap() },
                "subtotal" to subtotal.coerceAtLeast(0.0),
                "discountAmount" to discountAmount.coerceIn(0.0, subtotal.coerceAtLeast(0.0)),
                "deliveryFee" to actualDeliveryFee,
                "totalAmount" to totalAmount.coerceAtLeast(0.0),
                "couponCode" to couponCode?.trim()?.take(32)?.takeIf { it.isNotBlank() },
                "cookingInstructions" to cookingInstructions?.trim()?.take(300)?.takeIf { it.isNotBlank() },
                "deliveryAddress" to deliveryAddress?.trim()?.take(300)?.takeIf { it.isNotBlank() },
                "landmark" to landmark?.trim()?.take(120)?.takeIf { it.isNotBlank() },
                "deliveryZoneId" to deliveryZoneId?.trim()?.take(128)?.takeIf { it.isNotBlank() },
                "deliveryZoneName" to deliveryZoneName?.trim()?.take(80)?.takeIf { it.isNotBlank() },
                "deliveryLat" to deliveryLat,
                "deliveryLng" to deliveryLng,
                "estimatedMinutes" to estimatedMinutes.coerceIn(1L, 300L),
                "paymentMethod" to paymentMethod.raw,
                "paymentStatus" to paymentStatus,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            val cleanPayload = payload.filterValues { it != null } as Map<String, Any>
            docRef.set(cleanPayload).await()

            Result.success(
                Order(
                    id = docRef.id,
                    orderNumber = orderNumber,
                    userId = uid,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    customerEmail = customerEmail,
                    orderType = orderType.raw,
                    status = OrderStatus.ORDER_PLACED.raw,
                    items = items,
                    subtotal = subtotal,
                    discountAmount = discountAmount,
                    deliveryFee = actualDeliveryFee,
                    totalAmount = totalAmount,
                    couponCode = couponCode,
                    cookingInstructions = cookingInstructions,
                    deliveryAddress = deliveryAddress,
                    landmark = landmark,
                    deliveryZoneId = deliveryZoneId,
                    deliveryZoneName = deliveryZoneName,
                    deliveryLat = deliveryLat,
                    deliveryLng = deliveryLng,
                    estimatedMinutes = estimatedMinutes,
                    paymentMethod = paymentMethod.raw,
                    paymentStatus = paymentStatus
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun cancelCustomerOrder(orderId: String): Result<Unit> {
        val docRef = db.collection("orders").document(orderId)
        return try {
            requireUserId()
            require(orderId.isNotBlank())
            docRef.update(
                mapOf(
                    "status" to OrderStatus.CANCELLED.raw,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(
        orderId: String,
        newStatus: OrderStatus,
        rejectionReason: String? = null,
        paymentStatus: String? = null,
        estimatedMinutes: Long? = null
    ): Result<Unit> {
        val docRef = db.collection("orders").document(orderId)
        return try {
            requireUserId()
            require(orderId.isNotBlank())
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.raw,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!rejectionReason.isNullOrBlank()) {
                updates["rejectionReason"] = rejectionReason.trim().take(200)
            }
            if (!paymentStatus.isNullOrBlank()) {
                updates["paymentStatus"] = paymentStatus
            }
            if (estimatedMinutes != null) {
                updates["estimatedMinutes"] = estimatedMinutes.coerceIn(1L, 300L)
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    private fun logScopedFlowError(
        error: Throwable,
        uid: String,
        operationType: OperationType,
        path: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
