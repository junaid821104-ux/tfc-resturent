package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.EmailDocIdHelper
import com.example.data.model.IndianPhoneValidator
import com.example.data.model.LoginRole
import com.example.data.model.ManagerAccount
import com.example.data.model.SavedAddress
import com.example.data.model.SupportTicket
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
        const val BOOTSTRAPPED_ADMIN_EMAIL = "junaid545ansrai@gmail.com"
    }

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in before accessing user records.")

    private fun currentEmailDocId(): String? {
        val user = auth.currentUser ?: return null
        val email = user.email?.trim().orEmpty()
        if (!user.isEmailVerified || email.isBlank()) return null
        return EmailDocIdHelper.fromEmail(email)
    }

    fun isBootstrappedSuperAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email?.trim()?.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) == true &&
            user.isEmailVerified
    }

    private suspend fun isAuthorizedOwner(user: FirebaseUser): Boolean {
        if (user.email?.trim()?.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) == true &&
            user.isEmailVerified
        ) {
            return true
        }
        val uidExists = db.collection("admins").document(user.uid).get().await().exists()
        if (uidExists) return true
        val emailDoc = currentEmailDocId()
        return emailDoc != null && db.collection("admins").document(emailDoc).get().await().exists()
    }

    private suspend fun isAuthorizedManager(user: FirebaseUser): Boolean {
        val uidMgr = db.collection("managers").document(user.uid).get().await()
            .toObject(ManagerAccount::class.java)
        if (uidMgr?.active == true) return true
        val emailDoc = currentEmailDocId() ?: return false
        val emailMgr = db.collection("managers").document(emailDoc).get().await()
            .toObject(ManagerAccount::class.java)
        return emailMgr?.active == true
    }

    private suspend fun resolveAuthoritativeRole(user: FirebaseUser): LoginRole = when {
        isAuthorizedOwner(user) -> LoginRole.OWNER
        isAuthorizedManager(user) -> LoginRole.MANAGER
        else -> LoginRole.CUSTOMER
    }

    suspend fun ensureUserProfile(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(user.uid)
        return try {
            val resolvedRole = resolveAuthoritativeRole(user)
            val snap = docRef.get().await()
            val googleName = user.displayName?.trim()?.takeIf { it.isNotBlank() } ?: "TFC User"
            val googleEmail = user.email?.trim().orEmpty().take(120)
            val googlePhoto = user.photoUrl?.toString().orEmpty().take(500)

            if (!snap.exists()) {
                val payload = mapOf(
                    "uid" to user.uid,
                    "name" to googleName.take(80),
                    "email" to googleEmail.ifBlank { "${user.uid}@tfc.local" },
                    "phoneNumber" to "",
                    "profileImage" to googlePhoto,
                    "role" to resolvedRole.raw,
                    "isActive" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val hasCanonicalUid = snap.contains("uid")
                val existingRole = snap.getString("role").orEmpty()
                if (!hasCanonicalUid) {
                    val existingName = snap.getString("name")
                        ?.takeIf { it.isNotBlank() }
                        ?: snap.getString("displayName")?.takeIf { it.isNotBlank() }
                        ?: googleName
                    val existingPhone = snap.getString("phoneNumber")
                        ?.takeIf { it.isNotBlank() }
                        ?: snap.getString("phone").orEmpty()
                    val formattedPhone = IndianPhoneValidator.toE164(existingPhone) ?: ""
                    val existingImage = snap.getString("profileImage")
                        ?.takeIf { it.isNotBlank() }
                        ?: snap.getString("photoUrl").orEmpty()
                        ?: googlePhoto
                    docRef.update(
                        mapOf(
                            "uid" to user.uid,
                            "name" to existingName.take(80),
                            "email" to (snap.getString("email")?.takeIf { it.isNotBlank() } ?: googleEmail).take(120),
                            "phoneNumber" to formattedPhone.take(30),
                            "profileImage" to existingImage.take(500),
                            "role" to resolvedRole.raw,
                            "isActive" to (snap.getBoolean("isActive") ?: true),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else if (existingRole != resolvedRole.raw) {
                    docRef.update(
                        mapOf(
                            "role" to resolvedRole.raw,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun verifyRoleAccess(intendedRole: LoginRole): Result<LoginRole> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val emailLabel = user.email?.takeIf { it.isNotBlank() } ?: "This Google account"

        return try {
            when (intendedRole) {
                LoginRole.CUSTOMER -> {
                    ensureUserProfile()
                    val actual = resolveAuthoritativeRole(user)
                    Result.success(actual)
                }
                LoginRole.MANAGER -> {
                    if (isAuthorizedManager(user) || isAuthorizedOwner(user)) {
                        ensureUserProfile()
                        Result.success(if (isAuthorizedOwner(user)) LoginRole.OWNER else LoginRole.MANAGER)
                    } else {
                        Result.failure(
                            IllegalAccessException(
                                "$emailLabel is not registered as an active TFC Manager. Ask the Restaurant Owner to add your Google email in Owner Dashboard -> Managers."
                            )
                        )
                    }
                }
                LoginRole.OWNER -> {
                    if (isAuthorizedOwner(user)) {
                        ensureUserProfile()
                        Result.success(LoginRole.OWNER)
                    } else {
                        Result.failure(
                            IllegalAccessException(
                                "$emailLabel is not authorized as the TFC Restaurant Owner."
                            )
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
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

    suspend fun updateUserProfile(fullName: String, phoneInput: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val cleanName = fullName.trim()
            require(cleanName.length in 1..80) { "Full name must be 1–80 characters" }
            val normalizedE164 = IndianPhoneValidator.toE164(phoneInput)
                ?: throw IllegalArgumentException("Enter a valid 10-digit Indian mobile number (+91, starting with 6–9)")
            ensureUserProfile()
            docRef.update(
                mapOf(
                    "name" to cleanName,
                    "phoneNumber" to normalizedE164,
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
        val uidFlow = db.collection("managers").document(uid)
            .snapshots()
            .map { if (it.exists()) it.toObject(ManagerAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) else null }
            .catch { emit(null) }

        val emailDocId = currentEmailDocId()
        val emailFlow = if (emailDocId != null && emailDocId != uid) {
            db.collection("managers").document(emailDocId)
                .snapshots()
                .map { if (it.exists()) it.toObject(ManagerAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) else null }
                .catch { emit(null) }
        } else {
            flowOf(null)
        }

        emitAll(
            combine(uidFlow, emailFlow) { byUid, byEmail ->
                listOfNotNull(byUid, byEmail).firstOrNull { it.active } ?: byUid ?: byEmail
            }
        )
    }

    fun observeIsOwner(userId: String): Flow<Boolean> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val emailDocId = currentEmailDocId()

        val uidAdminFlow = db.collection("admins").document(uid)
            .snapshots()
            .map { it.exists() }
            .catch { emit(false) }

        val emailAdminFlow = if (emailDocId != null && emailDocId != uid) {
            db.collection("admins").document(emailDocId)
                .snapshots()
                .map { it.exists() }
                .catch { emit(false) }
        } else {
            flowOf(false)
        }

        emitAll(
            combine(uidAdminFlow, emailAdminFlow) { uidAdmin, emailAdmin ->
                isBootstrappedSuperAdmin() || uidAdmin || emailAdmin
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
                "userEmail" to (auth.currentUser?.email?.takeIf { it.isNotBlank() } ?: "").take(120),
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

    // --- OWNER EXCLUSIVE OPERATIONS ---

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
        val normalizedEmail = manager.email.trim().lowercase()
        val normalizedPhone = if (manager.phone.isBlank()) {
            ""
        } else {
            IndianPhoneValidator.toE164(manager.phone) ?: manager.phone.trim()
        }
        val targetId = if (isEdit) {
            manager.id.ifBlank { manager.userId }.trim()
        } else {
            EmailDocIdHelper.fromEmail(normalizedEmail)
        }
        return try {
            val adminUid = requireUserId()
            require(EmailDocIdHelper.isValidEmail(normalizedEmail)) {
                "Enter a valid Google email address for the manager"
            }
            require(targetId.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) {
                "Invalid manager identifier"
            }
            require(manager.name.trim().length in 1..80) { "Manager name is required" }
            val docRef = db.collection("managers").document(targetId)
            if (!isEdit) {
                docRef.set(
                    mapOf(
                        "userId" to targetId,
                        "name" to manager.name.trim(),
                        "email" to normalizedEmail.take(120),
                        "phone" to normalizedPhone.take(30),
                        "active" to manager.active,
                        "canViewOrders" to manager.canViewOrders,
                        "canManageOrders" to manager.canManageOrders,
                        "canViewMenu" to manager.canViewMenu,
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
                        "email" to normalizedEmail.take(120),
                        "phone" to normalizedPhone.take(30),
                        "active" to manager.active,
                        "canViewOrders" to manager.canViewOrders,
                        "canManageOrders" to manager.canManageOrders,
                        "canViewMenu" to manager.canViewMenu,
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
            handleFirestoreError(e, OperationType.WRITE, "managers/$targetId")
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
