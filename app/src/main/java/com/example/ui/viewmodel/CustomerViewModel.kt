package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.DefaultPaymentGateway
import com.example.data.firebase.MenuRepository
import com.example.data.firebase.OnlinePaymentRequest
import com.example.data.firebase.OnlinePaymentResult
import com.example.data.firebase.OrderRepository
import com.example.data.firebase.PaymentGateway
import com.example.data.firebase.UserRepository
import com.example.data.firebase.toReadableFirestoreMessage
import com.example.data.model.CartLineItem
import com.example.data.model.Coupon
import com.example.data.model.DeliveryZone
import com.example.data.model.DiscountType
import com.example.data.model.FoodAddon
import com.example.data.model.FoodCategory
import com.example.data.model.FoodItem
import com.example.data.model.FoodVariant
import com.example.data.model.ManagerAccount
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderType
import com.example.data.model.PaymentMethod
import com.example.data.model.RestaurantSettings
import com.example.data.model.SavedAddress
import com.example.data.model.SupportTicket
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.formatCurrency
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

data class CartSelection(
    val itemId: String,
    val selectedVariant: FoodVariant? = null,
    val selectedAddons: List<FoodAddon> = emptyList(),
    val quantity: Int
)

data class CheckoutDraft(
    val orderType: OrderType = OrderType.DELIVERY,
    val selectedAddressId: String? = null,
    val useManualAddress: Boolean = false,
    val manualAddress: String = "",
    val manualLandmark: String = "",
    val manualLat: Double? = null,
    val manualLng: Double? = null,
    val selectedZoneId: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH_ON_DELIVERY,
    val cookingInstructions: String = "",
    val appliedCoupon: Coupon? = null
)

data class CartSummary(
    val lines: List<CartLineItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val itemCount: Int = 0,
    val estimatedMinutes: Long = 25L
) {
    fun quantityForItem(itemId: String): Int =
        lines.filter { it.foodItem.effectiveId == itemId }.sumOf { it.quantity }
}

class CustomerViewModel(
    private val menuRepository: MenuRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val currentUserId: String,
    private val paymentGateway: PaymentGateway = DefaultPaymentGateway()
) : ViewModel() {

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    private val _isPlacingOrder = MutableStateFlow(false)
    val isPlacingOrder = _isPlacingOrder.asStateFlow()

    private val _isSavingProfile = MutableStateFlow(false)
    val isSavingProfile = _isSavingProfile.asStateFlow()

    val categories: StateFlow<UiState<List<FoodCategory>>> =
        menuRepository.observeCategories().toUiStateFlow("Failed to load categories")

    val foodItems: StateFlow<UiState<List<FoodItem>>> =
        menuRepository.observeFoodItems().toUiStateFlow("Failed to load menu")

    val coupons: StateFlow<UiState<List<Coupon>>> =
        menuRepository.observeCoupons().toUiStateFlow("Failed to load offers")

    val deliveryZones: StateFlow<UiState<List<DeliveryZone>>> =
        menuRepository.observeDeliveryZones().toUiStateFlow("Failed to load delivery zones")

    val settings: StateFlow<RestaurantSettings> =
        menuRepository.observeRestaurantSettings()
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(RestaurantSettings())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), RestaurantSettings())

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        userRepository.observeUserProfile(currentUserId).toUiStateFlow("Failed to load profile")

    val userProfile: StateFlow<UserProfile?> =
        userProfileState
            .map { (it as? UiState.Success)?.data }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val addresses: StateFlow<UiState<List<SavedAddress>>> =
        userRepository.observeAddresses(currentUserId).toUiStateFlow("Failed to load addresses")

    val myOrders: StateFlow<UiState<List<Order>>> =
        orderRepository.observeUserOrders(currentUserId).toUiStateFlow("Failed to load orders")

    val supportTickets: StateFlow<UiState<List<SupportTicket>>> =
        userRepository.observeSupportTickets(currentUserId).toUiStateFlow("Failed to load support tickets")

    val myManagerRecord: StateFlow<ManagerAccount?> =
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

    private val _cartSelections = MutableStateFlow<Map<String, CartSelection>>(emptyMap())
    private val _checkoutDraft = MutableStateFlow(CheckoutDraft())
    val checkoutDraft = _checkoutDraft.asStateFlow()

    val cartSummary: StateFlow<CartSummary> = combine(
        _cartSelections,
        foodItems,
        _checkoutDraft,
        deliveryZones,
        settings
    ) { selections, itemsState, draft, zonesState, restSettings ->
        val allItems = (itemsState as? UiState.Success)?.data ?: emptyList()
        val itemMap = allItems.associateBy { it.effectiveId }
        val lines = selections.values.mapNotNull { sel ->
            val item = itemMap[sel.itemId]
            if (item != null && sel.quantity > 0 && item.isAvailable) {
                val resolvedVariant = sel.selectedVariant?.let { sv ->
                    item.variants.firstOrNull { it.name == sv.name } ?: sv
                }
                val resolvedAddons = sel.selectedAddons.map { sa ->
                    item.addons.firstOrNull { it.name == sa.name } ?: sa
                }
                CartLineItem(
                    foodItem = item,
                    quantity = sel.quantity,
                    selectedVariant = resolvedVariant,
                    selectedAddons = resolvedAddons
                )
            } else null
        }
        val subtotal = lines.sumOf { it.lineTotal }.round2()
        val coupon = draft.appliedCoupon?.takeIf { it.active && subtotal >= it.minOrderAmount }
        val discount = if (coupon == null) 0.0 else {
            when (DiscountType.fromRaw(coupon.discountType)) {
                DiscountType.PERCENT -> {
                    val raw = subtotal * (coupon.discountValue / 100.0)
                    if (coupon.maxDiscount > 0.0) min(raw, coupon.maxDiscount) else raw
                }
                DiscountType.FLAT -> min(coupon.discountValue, subtotal)
            }.round2()
        }
        val zones = (zonesState as? UiState.Success)?.data?.filter { it.active } ?: emptyList()
        val activeZone = zones.firstOrNull { it.id == draft.selectedZoneId } ?: zones.firstOrNull()
        val deliveryFee = when {
            lines.isEmpty() -> 0.0
            draft.orderType == OrderType.PICKUP -> 0.0
            else -> (activeZone?.fee ?: restSettings.baseDeliveryFee).round2()
        }
        val total = (subtotal - discount + deliveryFee).coerceAtLeast(0.0).round2()
        val eta = if (draft.orderType == OrderType.PICKUP) {
            restSettings.estimatedPickupMinutes
        } else {
            activeZone?.estimatedMinutes ?: 30L
        }
        CartSummary(
            lines = lines,
            subtotal = subtotal,
            discountAmount = discount,
            deliveryFee = deliveryFee,
            totalAmount = total,
            itemCount = lines.sumOf { it.quantity },
            estimatedMinutes = eta
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CartSummary())

    init {
        viewModelScope.launch {
            userRepository.ensureUserProfile()
        }
    }

    fun observeSingleOrder(orderId: String): Flow<UiState<Order?>> =
        orderRepository.observeOrder(orderId)
            .map<Order?, UiState<Order?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                emit(UiState.Error(root.toReadableFirestoreMessage("Failed to track order")))
            }

    private fun selectionKey(
        itemId: String,
        variant: FoodVariant?,
        addons: List<FoodAddon>
    ): String {
        val vKey = variant?.name?.trim().orEmpty()
        val aKey = addons.map { it.name.trim() }.sorted().joinToString(",")
        return "$itemId|$vKey|$aKey"
    }

    fun addToCart(
        item: FoodItem,
        quantity: Int = 1,
        selectedVariant: FoodVariant? = item.variants.firstOrNull(),
        selectedAddons: List<FoodAddon> = emptyList()
    ) {
        if (!item.isAvailable) {
            viewModelScope.launch { _events.emit("${item.name} is currently out of stock") }
            return
        }
        val itemId = item.effectiveId
        val key = selectionKey(itemId, selectedVariant, selectedAddons)
        _cartSelections.update { current ->
            val existingQty = current[key]?.quantity ?: 0
            val nextQty = (existingQty + quantity).coerceAtMost(50)
            current + (key to CartSelection(itemId, selectedVariant, selectedAddons, nextQty))
        }
        viewModelScope.launch { _events.emit("Added ${item.name} to cart") }
    }

    fun incrementCartKey(cartKey: String) {
        _cartSelections.update { current ->
            val existing = current[cartKey] ?: return@update current
            current + (cartKey to existing.copy(quantity = (existing.quantity + 1).coerceAtMost(50)))
        }
    }

    fun decrementCartKey(cartKey: String) {
        _cartSelections.update { current ->
            val existing = current[cartKey] ?: return@update current
            val next = existing.quantity - 1
            if (next <= 0) current - cartKey else current + (cartKey to existing.copy(quantity = next))
        }
    }

    fun incrementFirstLineForItem(itemId: String) {
        val matching = _cartSelections.value.entries.firstOrNull { it.value.itemId == itemId }
        if (matching != null) {
            incrementCartKey(matching.key)
        }
    }

    fun decrementFirstLineForItem(itemId: String) {
        val matching = _cartSelections.value.entries.lastOrNull { it.value.itemId == itemId }
        if (matching != null) {
            decrementCartKey(matching.key)
        }
    }

    fun removeFromCart(cartKey: String) {
        _cartSelections.update { it - cartKey }
    }

    fun clearCart() {
        _cartSelections.value = emptyMap()
        _checkoutDraft.update { it.copy(cookingInstructions = "", appliedCoupon = null) }
    }

    fun updateCookingInstructions(text: String) {
        _checkoutDraft.update { it.copy(cookingInstructions = text.take(300)) }
    }

    fun setOrderType(type: OrderType) {
        _checkoutDraft.update { draft ->
            val defaultPayment = when {
                type == OrderType.PICKUP && draft.paymentMethod == PaymentMethod.CASH_ON_DELIVERY ->
                    PaymentMethod.CASH_AT_PICKUP
                type == OrderType.DELIVERY && draft.paymentMethod == PaymentMethod.CASH_AT_PICKUP ->
                    PaymentMethod.CASH_ON_DELIVERY
                else -> draft.paymentMethod
            }
            draft.copy(orderType = type, paymentMethod = defaultPayment)
        }
    }

    fun selectSavedAddress(address: SavedAddress) {
        _checkoutDraft.update {
            it.copy(
                selectedAddressId = address.id,
                useManualAddress = false,
                selectedZoneId = address.zoneId.takeIf { z -> z.isNotBlank() } ?: it.selectedZoneId
            )
        }
    }

    fun setManualAddressMode(useManual: Boolean) {
        _checkoutDraft.update { it.copy(useManualAddress = useManual) }
    }

    fun updateManualAddress(address: String, landmark: String, lat: Double? = null, lng: Double? = null) {
        _checkoutDraft.update {
            it.copy(
                useManualAddress = true,
                manualAddress = address,
                manualLandmark = landmark,
                manualLat = lat ?: it.manualLat,
                manualLng = lng ?: it.manualLng
            )
        }
    }

    fun selectDeliveryZone(zoneId: String) {
        _checkoutDraft.update { it.copy(selectedZoneId = zoneId) }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _checkoutDraft.update { it.copy(paymentMethod = method) }
    }

    fun applyCouponCode(rawCode: String) {
        val code = rawCode.trim().uppercase()
        val available = (coupons.value as? UiState.Success)?.data ?: emptyList()
        val match = available.firstOrNull { it.code.equals(code, ignoreCase = true) && it.active }
        if (match == null) {
            viewModelScope.launch { _events.emit("Coupon '$code' is invalid or inactive") }
            return
        }
        val subtotal = cartSummary.value.subtotal
        if (subtotal < match.minOrderAmount) {
            viewModelScope.launch {
                _events.emit("Minimum order of ${formatCurrency(match.minOrderAmount)} required for ${match.code}")
            }
            return
        }
        _checkoutDraft.update { it.copy(appliedCoupon = match) }
        viewModelScope.launch { _events.emit("Coupon ${match.code} applied!") }
    }

    fun removeCoupon() {
        _checkoutDraft.update { it.copy(appliedCoupon = null) }
    }

    fun reorderFromOrder(order: Order) {
        val allItems = (foodItems.value as? UiState.Success)?.data ?: emptyList()
        val availableItems = allItems.filter { it.isAvailable }.associateBy { it.effectiveId }
        val nextMap = mutableMapOf<String, CartSelection>()
        order.items.forEach { line ->
            val live = availableItems[line.itemId]
            if (live != null) {
                val variant = live.variants.firstOrNull { it.name == line.variantName }
                val key = selectionKey(live.effectiveId, variant, emptyList())
                nextMap[key] = CartSelection(
                    itemId = live.effectiveId,
                    selectedVariant = variant,
                    quantity = line.quantity.toInt().coerceIn(1, 50)
                )
            }
        }
        if (nextMap.isEmpty()) {
            viewModelScope.launch { _events.emit("None of those items are currently available") }
        } else {
            _cartSelections.value = nextMap
            viewModelScope.launch { _events.emit("Order items added to cart") }
        }
    }

    fun placeOrder(onSuccess: (Order) -> Unit) {
        val summary = cartSummary.value
        val draft = _checkoutDraft.value
        val restSettings = settings.value
        if (!restSettings.orderingOpen) {
            viewModelScope.launch { _events.emit("TFC Restaurant ordering is currently closed") }
            return
        }
        if (summary.lines.isEmpty()) {
            viewModelScope.launch { _events.emit("Your cart is empty") }
            return
        }
        if (summary.subtotal < restSettings.minimumOrderAmount) {
            viewModelScope.launch {
                _events.emit("Minimum order amount is ${formatCurrency(restSettings.minimumOrderAmount)}")
            }
            return
        }

        val savedList = (addresses.value as? UiState.Success)?.data ?: emptyList()
        val chosenSaved = savedList.firstOrNull { it.id == draft.selectedAddressId }
            ?: savedList.firstOrNull { it.defaultAddress }
            ?: savedList.firstOrNull()

        val finalAddress: String?
        val finalLandmark: String?
        val finalLat: Double?
        val finalLng: Double?
        val zones = (deliveryZones.value as? UiState.Success)?.data?.filter { it.active } ?: emptyList()
        val activeZone = zones.firstOrNull { it.id == draft.selectedZoneId } ?: zones.firstOrNull()

        if (draft.orderType == OrderType.DELIVERY) {
            if (draft.useManualAddress || chosenSaved == null) {
                if (draft.manualAddress.trim().length < 3) {
                    viewModelScope.launch { _events.emit("Please select or enter a delivery address") }
                    return
                }
                finalAddress = draft.manualAddress.trim()
                finalLandmark = draft.manualLandmark.trim()
                finalLat = draft.manualLat
                finalLng = draft.manualLng
            } else {
                finalAddress = chosenSaved.fullAddress
                finalLandmark = chosenSaved.landmark
                finalLat = chosenSaved.latitude
                finalLng = chosenSaved.longitude
            }
            if (activeZone != null && summary.subtotal < activeZone.minOrderAmount) {
                viewModelScope.launch {
                    _events.emit("${activeZone.name} requires a minimum order of ${formatCurrency(activeZone.minOrderAmount)}")
                }
                return
            }
        } else {
            finalAddress = restSettings.restaurantAddress
            finalLandmark = null
            finalLat = null
            finalLng = null
        }

        val profile = userProfile.value
        viewModelScope.launch {
            _isPlacingOrder.value = true
            val paymentOutcome = paymentGateway.preparePayment(
                draft.paymentMethod,
                OnlinePaymentRequest(
                    orderNumber = "PENDING",
                    amount = summary.totalAmount,
                    customerEmail = profile?.email ?: ""
                )
            )
            val paymentStatus = when (paymentOutcome) {
                is OnlinePaymentResult.ReadyForOrder -> paymentOutcome.paymentStatus
                is OnlinePaymentResult.Failed -> {
                    _isPlacingOrder.value = false
                    _events.emit(paymentOutcome.message)
                    return@launch
                }
            }

            val orderItems = summary.lines.map { line ->
                OrderItem(
                    itemId = line.foodItem.effectiveId,
                    name = line.foodItem.name,
                    price = line.unitPrice,
                    quantity = line.quantity.toLong(),
                    imageUrl = line.foodItem.imageUrl,
                    vegType = line.foodItem.foodType,
                    variantName = line.selectedVariant?.name.orEmpty(),
                    addonsSummary = line.selectedAddons.joinToString(", ") { it.name }
                )
            }
            val result = orderRepository.placeOrder(
                customerName = profile?.effectiveName?.ifBlank { "TFC Customer" } ?: "TFC Customer",
                customerPhone = profile?.effectivePhone ?: "",
                customerEmail = profile?.email ?: "",
                orderType = draft.orderType,
                items = orderItems,
                subtotal = summary.subtotal,
                discountAmount = summary.discountAmount,
                deliveryFee = summary.deliveryFee,
                totalAmount = summary.totalAmount,
                couponCode = draft.appliedCoupon?.code,
                cookingInstructions = draft.cookingInstructions,
                deliveryAddress = finalAddress,
                landmark = finalLandmark,
                deliveryZoneId = if (draft.orderType == OrderType.DELIVERY) activeZone?.id else null,
                deliveryZoneName = if (draft.orderType == OrderType.DELIVERY) activeZone?.name else null,
                deliveryLat = finalLat,
                deliveryLng = finalLng,
                estimatedMinutes = summary.estimatedMinutes,
                paymentMethod = draft.paymentMethod,
                paymentStatus = paymentStatus
            )
            _isPlacingOrder.value = false
            result.fold(
                onSuccess = { order ->
                    clearCart()
                    onSuccess(order)
                },
                onFailure = { error ->
                    _events.emit(error.toReadableFirestoreMessage("Failed to place order"))
                }
            )
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            val res = orderRepository.cancelCustomerOrder(orderId)
            res.fold(
                onSuccess = { _events.emit("Order cancelled") },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Could not cancel order")) }
            )
        }
    }

    fun saveAddress(address: SavedAddress, selectAfterSave: Boolean = true) {
        viewModelScope.launch {
            val res = userRepository.saveAddress(address)
            res.fold(
                onSuccess = { newId ->
                    if (selectAfterSave) {
                        _checkoutDraft.update {
                            it.copy(
                                selectedAddressId = newId,
                                useManualAddress = false,
                                selectedZoneId = address.zoneId.takeIf { z -> z.isNotBlank() } ?: it.selectedZoneId
                            )
                        }
                    }
                    _events.emit("Address saved")
                },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Could not save address")) }
            )
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            val res = userRepository.deleteAddress(addressId)
            res.fold(
                onSuccess = { _events.emit("Address deleted") },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Could not delete address")) }
            )
        }
    }

    fun completeProfile(fullName: String, tenDigitIndianMobile: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isSavingProfile.value = true
            val res = userRepository.updateUserProfile(fullName, tenDigitIndianMobile)
            _isSavingProfile.value = false
            res.fold(
                onSuccess = {
                    _events.emit("Profile saved! Welcome to TFC Restaurant.")
                    onDone()
                },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Could not save profile")) }
            )
        }
    }

    fun updateProfile(name: String, phone: String) {
        viewModelScope.launch {
            _isSavingProfile.value = true
            val res = userRepository.updateUserProfile(name, phone)
            _isSavingProfile.value = false
            res.fold(
                onSuccess = { _events.emit("Profile updated") },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Could not update profile")) }
            )
        }
    }

    fun submitSupportTicket(subject: String, message: String, orderNumber: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            val res = userRepository.submitSupportTicket(subject, message, orderNumber)
            res.fold(
                onSuccess = {
                    _events.emit("Support request sent! Our team will respond shortly.")
                    onDone()
                },
                onFailure = { _events.emit(it.toReadableFirestoreMessage("Could not submit support ticket")) }
            )
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(fallbackMessage: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                emit(UiState.Error(rootError.toReadableFirestoreMessage(fallbackMessage)))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private fun Double.round2(): Double = (this * 100.0).roundToInt() / 100.0
}
