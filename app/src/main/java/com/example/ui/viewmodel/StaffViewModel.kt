package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.MenuRepository
import com.example.data.firebase.OrderRepository
import com.example.data.firebase.UserRepository
import com.example.data.firebase.toReadableFirestoreMessage
import com.example.data.model.Coupon
import com.example.data.model.DeliveryZone
import com.example.data.model.FoodCategory
import com.example.data.model.FoodItem
import com.example.data.model.ManagerAccount
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.RestaurantSettings
import com.example.data.model.SupportTicket
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class StaffViewModel(
    private val menuRepository: MenuRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    val managerRecord: StateFlow<ManagerAccount?> =
        userRepository.observeMyManagerRecord(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val isSuperAdmin: StateFlow<Boolean> =
        userRepository.observeIsOwner(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(userRepository.isBootstrappedSuperAdmin())
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000L),
                userRepository.isBootstrappedSuperAdmin()
            )

    private val canReadAllOrders: Flow<Boolean> = combine(isSuperAdmin, managerRecord) { admin, mgr ->
        admin || (mgr?.active == true && (mgr.canViewOrders || mgr.canManageOrders || mgr.canViewSales))
    }

    val allOrders: StateFlow<UiState<List<Order>>> =
        canReadAllOrders.flatMapLatest { allowed ->
            if (!allowed) flowOf(UiState.Success(emptyList()))
            else orderRepository.observeAllOrders().toUiStateFlow("Failed to load orders")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val allManagers: StateFlow<UiState<List<ManagerAccount>>> =
        isSuperAdmin.flatMapLatest { admin ->
            if (!admin) flowOf(UiState.Success(emptyList()))
            else userRepository.observeAllManagers().toUiStateFlow("Failed to load managers")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val allUsers: StateFlow<UiState<List<UserProfile>>> =
        isSuperAdmin.flatMapLatest { admin ->
            if (!admin) flowOf(UiState.Success(emptyList()))
            else userRepository.observeAllUsers().toUiStateFlow("Failed to load customers")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val allSupportTickets: StateFlow<UiState<List<SupportTicket>>> =
        isSuperAdmin.flatMapLatest { admin ->
            if (!admin) flowOf(UiState.Success(emptyList()))
            else userRepository.observeAllSupportTickets().toUiStateFlow("Failed to load support tickets")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun updateOrderStatus(
        orderId: String,
        newStatus: OrderStatus,
        rejectionReason: String? = null,
        paymentStatus: String? = null
    ) {
        viewModelScope.launch {
            val res = orderRepository.updateOrderStatus(orderId, newStatus, rejectionReason, paymentStatus)
            res.fold(
                onSuccess = { _events.emit("Order updated to ${newStatus.raw}") },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Failed to update order")) }
            )
        }
    }

    fun toggleFoodAvailability(itemId: String, available: Boolean) {
        viewModelScope.launch {
            val res = menuRepository.toggleFoodAvailability(itemId, available)
            res.fold(
                onSuccess = { _events.emit(if (available) "Item marked In Stock" else "Item marked Out of Stock") },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Failed to update stock")) }
            )
        }
    }

    fun saveFoodItem(item: FoodItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val res = menuRepository.saveFoodItem(item)
            res.fold(
                onSuccess = {
                    _events.emit("Saved ${item.name}")
                    onDone()
                },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Failed to save item")) }
            )
        }
    }

    fun deleteFoodItem(itemId: String) {
        viewModelScope.launch {
            val res = menuRepository.deleteFoodItem(itemId)
            res.fold(
                onSuccess = { _events.emit("Food item deleted") },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Failed to delete item")) }
            )
        }
    }

    fun saveCategory(category: FoodCategory, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val res = menuRepository.saveCategory(category)
            res.fold(
                onSuccess = {
                    _events.emit("Category saved")
                    onDone()
                },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Failed to save category")) }
            )
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            val res = menuRepository.deleteCategory(categoryId)
            res.fold(
                onSuccess = { _events.emit("Category deleted") },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Failed to delete category")) }
            )
        }
    }

    fun saveCoupon(coupon: Coupon, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val res = menuRepository.saveCoupon(coupon)
            res.fold(
                onSuccess = {
                    _events.emit("Coupon ${coupon.code.uppercase()} saved")
                    onDone()
                },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Failed to save coupon")) }
            )
        }
    }

    fun deleteCoupon(couponId: String) {
        viewModelScope.launch {
            val res = menuRepository.deleteCoupon(couponId)
            res.fold(
                onSuccess = { _events.emit("Coupon deleted") },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Failed to delete coupon")) }
            )
        }
    }

    fun saveDeliveryZone(zone: DeliveryZone, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val res = menuRepository.saveDeliveryZone(zone)
            res.fold(
                onSuccess = {
                    _events.emit("Delivery zone saved")
                    onDone()
                },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Failed to save zone")) }
            )
        }
    }

    fun deleteDeliveryZone(zoneId: String) {
        viewModelScope.launch {
            val res = menuRepository.deleteDeliveryZone(zoneId)
            res.fold(
                onSuccess = { _events.emit("Delivery zone deleted") },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Failed to delete zone")) }
            )
        }
    }

    fun saveRestaurantSettings(settings: RestaurantSettings) {
        viewModelScope.launch {
            val res = menuRepository.saveRestaurantSettings(settings)
            res.fold(
                onSuccess = { _events.emit("Restaurant settings updated") },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Failed to save settings")) }
            )
        }
    }

    fun saveManager(manager: ManagerAccount, isEdit: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val res = userRepository.saveManager(manager, isEdit)
            res.fold(
                onSuccess = {
                    _events.emit("Manager ${manager.name} saved")
                    onDone()
                },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Failed to save manager")) }
            )
        }
    }

    fun deleteManager(managerId: String) {
        viewModelScope.launch {
            val res = userRepository.deleteManager(managerId)
            res.fold(
                onSuccess = { _events.emit("Manager removed") },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Failed to remove manager")) }
            )
        }
    }

    fun resolveSupportTicket(ticketId: String, status: String) {
        viewModelScope.launch {
            val res = userRepository.resolveSupportTicket(ticketId, status)
            res.fold(
                onSuccess = { _events.emit("Ticket marked $status") },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Failed to update ticket")) }
            )
        }
    }

    fun initializeStarterCatalog(currentOwnerPhone: String = "") {
        viewModelScope.launch {
            val res = menuRepository.initializeStarterCatalog(currentOwnerPhone)
            res.fold(
                onSuccess = { _events.emit("TFC Restaurant menu, zones & settings initialized!") },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Failed to initialize catalog")) }
            )
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(fallbackMessage: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                emit(UiState.Error(rootError.toReadableFirestoreMessage(fallbackMessage)))
            }
}
