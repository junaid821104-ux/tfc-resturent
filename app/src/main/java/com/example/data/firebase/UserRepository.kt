package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AdminAccount
import com.example.data.model.ManagerAccount
import com.example.data.model.SavedAddress
import com.example.data.model.SupportTicket
import com.example.data.model.UserProfile
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

class UserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "alfianaj2016@gmail.com"
    }

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in before accessing user records.")

    fun isBootstrappedSuperAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email == BOOTSTRAPPED_ADMIN_EMAIL && user.isEmailVerified
    }

    suspend fun ensureUserProfile(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(user.uid)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mutableMapOf<String, Any?>(
                    "userId" to user.uid,
                    "displayName" to (user.displayName?.takeIf { it.isNotBlank() } ?: "TFC Customer").take(80),
                    "email" to (user.email ?: "").take(120),
                    "phone" to (user.phoneNumber ?: "").take(30),
                    "photoUrl" to user.photoUrl?.toString()?.take(500),
                    "role" to "customer",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                @Suppress("UNCHECKED_CAST")
                docRef.set(payload.filterValues { it != null } as Map<String, Any>).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    suspend fun updateUserProfile(displayName: String, phone: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            require(displayName.trim().length in 1..80) { "Name must be 1–80 characters" }
            docRef.update(
                mapOf(
                    "displayName" to displayName.trim(),
                    "phone" to phone.trim().take(30),
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

    fun observeMyManagerRecord(userId: String): Flow<ManagerAccount?> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val docRef = db.collection("managers").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(ManagerAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    fun observeMyAdminRecord(userId: String): Flow<AdminAccount?> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val docRef = db.collection("admins").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(AdminAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    fun observeAddresses(userId: String): Flow<List<SavedAddress>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("addresses")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SavedAddress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.defaultAddress }
                }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.LIST, "addresses")
                    throw error
                }
        )
    }

    suspend fun saveAddress(address: SavedAddress): Result<String> {
        val col = db.collection("addresses")
        val docRef = if (address.id.isBlank()) col.document() else col.document(address.id)
        return try {
            val uid = requireUserId()
            require(address.label.trim().length in 1..40) { "Label must be 1–40 characters" }
            require(address.fullAddress.trim().length in 3..300) { "Address must be at least 3 characters" }
            if (address.id.isBlank()) {
                val payload = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "label" to address.label.trim(),
                    "fullAddress" to address.fullAddress.trim(),
                    "landmark" to address.landmark.trim().take(120),
                    "zoneId" to address.zoneId.trim().take(128),
                    "latitude" to address.latitude,
                    "longitude" to address.longitude,
                    "defaultAddress" to address.defaultAddress,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                @Suppress("UNCHECKED_CAST")
                docRef.set(payload.filterValues { it != null } as Map<String, Any>).await()
            } else {
                val payload = mutableMapOf<String, Any?>(
                    "label" to address.label.trim(),
                    "fullAddress" to address.fullAddress.trim(),
                    "landmark" to address.landmark.trim().take(120),
                    "zoneId" to address.zoneId.trim().take(128),
                    "latitude" to address.latitude,
                    "longitude" to address.longitude,
                    "defaultAddress" to address.defaultAddress,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                @Suppress("UNCHECKED_CAST")
                docRef.update(payload.filterValues { it != null } as Map<String, Any>).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(addressId: String): Result<Unit> {
        val docRef = db.collection("addresses").document(addressId)
        return try {
            requireUserId()
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeSupportTickets(userId: String): Flow<List<SupportTicket>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("support_tickets")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.LIST, "support_tickets")
                    throw error
                }
        )
    }

    suspend fun submitSupportTicket(
        subject: String,
        message: String,
        orderNumber: String?
    ): Result<Unit> {
        val docRef = db.collection("support_tickets").document()
        return try {
            val uid = requireUserId()
            require(subject.trim().length in 2..100) { "Subject must be 2–100 characters" }
            require(message.trim().length in 5..1000) { "Message must be 5–1000 characters" }
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "userEmail" to (auth.currentUser?.email ?: "").take(120),
                "subject" to subject.trim(),
                "message" to message.trim(),
                "orderNumber" to orderNumber?.trim()?.take(40)?.takeIf { it.isNotBlank() },
                "status" to "Open",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            docRef.set(payload.filterValues { it != null } as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- SUPER ADMIN EXCLUSIVE OPERATIONS ---

    fun observeAllManagers(): Flow<List<ManagerAccount>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("managers")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ManagerAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.LIST, "managers")
                    throw error
                }
        )
    }

    suspend fun saveManager(manager: ManagerAccount, isEdit: Boolean): Result<Unit> {
        val targetUid = manager.userId.trim()
        require(targetUid.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) {
            "Valid Firebase User ID is required"
        }
        val docRef = db.collection("managers").document(targetUid)
        return try {
            val adminUid = requireUserId()
            require(manager.name.trim().length in 1..80) { "Manager name is required" }
            if (!isEdit) {
                docRef.set(
                    mapOf(
                        "userId" to targetUid,
                        "name" to manager.name.trim(),
                        "email" to manager.email.trim().take(120),
                        "phone" to manager.phone.trim().take(30),
                        "active" to manager.active,
                        "canManageOrders" to manager.canManageOrders,
                        "canManageMenu" to manager.canManageMenu,
                        "canViewSales" to manager.canViewSales,
                        "createdBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to manager.name.trim(),
                        "email" to manager.email.trim().take(120),
                        "phone" to manager.phone.trim().take(30),
                        "active" to manager.active,
                        "canManageOrders" to manager.canManageOrders,
                        "canManageMenu" to manager.canManageMenu,
                        "canViewSales" to manager.canViewSales,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteManager(managerId: String): Result<Unit> {
        val docRef = db.collection("managers").document(managerId)
        return try {
            requireUserId()
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.LIST, "users")
                    throw error
                }
        )
    }

    fun observeAllSupportTickets(): Flow<List<SupportTicket>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("support_tickets")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logScopedFlowError(error, uid, OperationType.LIST, "support_tickets")
                    throw error
                }
        )
    }

    suspend fun resolveSupportTicket(ticketId: String, status: String): Result<Unit> {
        val docRef = db.collection("support_tickets").document(ticketId)
        return try {
            requireUserId()
            docRef.update(
                mapOf(
                    "status" to status,
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
