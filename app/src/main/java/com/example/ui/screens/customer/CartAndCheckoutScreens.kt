package com.example.ui.screens.customer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.Coupon
import com.example.data.model.DeliveryZone
import com.example.data.model.Order
import com.example.data.model.OrderType
import com.example.data.model.PaymentMethod
import com.example.data.model.RestaurantSettings
import com.example.data.model.SavedAddress
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FoodPhoto
import com.example.ui.components.QuantityStepper
import com.example.ui.components.VegTypeBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcJetBlack
import com.example.ui.theme.TfcPureWhite
import com.example.ui.theme.TfcSoftRed
import com.example.ui.theme.TfcSuccessGreen
import com.example.ui.viewmodel.CartSummary
import com.example.ui.viewmodel.CheckoutDraft
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun CartScreen(
    cartSummary: CartSummary,
    checkoutDraft: CheckoutDraft,
    settings: RestaurantSettings,
    couponsState: UiState<List<Coupon>>,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCookingInstructionsChange: (String) -> Unit,
    onApplyCoupon: (String) -> Unit,
    onRemoveCoupon: () -> Unit,
    onBrowseMenu: () -> Unit,
    onProceedToCheckout: () -> Unit
) {
    var couponInput by remember { mutableStateOf("") }
    val activeCoupons = ((couponsState as? UiState.Success)?.data ?: emptyList()).filter { it.active }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cart_screen")
    ) {
        if (cartSummary.lines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    title = "Your TFC Cart is Empty",
                    subtitle = "Add crispy buckets, burgers, or sides from the kitchen menu to start an order.",
                    actionLabel = "Browse Menu",
                    onAction = onBrowseMenu
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Your Cart (${cartSummary.itemCount} items)",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                items(cartSummary.lines, key = { it.cartKey }) { line ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FoodPhoto(
                                imageUrl = line.foodItem.imageUrl,
                                contentDescription = line.foodItem.name,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    VegTypeBadge(line.foodItem.foodType)
                                    Text(
                                        text = line.foodItem.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                if (line.customizationSummary.isNotBlank()) {
                                    Text(
                                        text = line.customizationSummary,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(line.lineTotal),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TfcCrimson,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                QuantityStepper(
                                    quantity = line.quantity,
                                    onIncrement = { onIncrement(line.cartKey) },
                                    onDecrement = { onDecrement(line.cartKey) },
                                    tagPrefix = "cart_${line.cartKey}"
                                )
                                TextButton(
                                    onClick = { onRemove(line.cartKey) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Remove item",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Remove", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // Cooking instructions
                item {
                    OutlinedTextField(
                        value = checkoutDraft.cookingInstructions,
                        onValueChange = onCookingInstructionsChange,
                        label = { Text("Cooking Instructions (Optional)") },
                        placeholder = { Text("Extra spicy dip, no onions, well-done fries...") },
                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cooking_instructions_input")
                    )
                }

                // Coupon section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = couponInput,
                                onValueChange = { couponInput = it.uppercase() },
                                label = { Text("Promo / Coupon Code") },
                                leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("coupon_code_input")
                            )
                            Button(
                                onClick = {
                                    if (couponInput.isNotBlank()) {
                                        onApplyCoupon(couponInput)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TfcJetBlack),
                                modifier = Modifier
                                    .height(54.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("apply_coupon_button")
                            ) {
                                Text("Apply")
                            }
                        }

                        if (checkoutDraft.appliedCoupon != null) {
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Applied ${checkoutDraft.appliedCoupon.code} (-${formatCurrency(cartSummary.discountAmount)})",
                                        style = MaterialTheme.typography.labelLarge.copy(color = TfcSuccessGreen)
                                    )
                                    IconButton(onClick = onRemoveCoupon, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove coupon")
                                    }
                                }
                            }
                        } else if (activeCoupons.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(activeCoupons, key = { it.id }) { coupon ->
                                    AssistChip(
                                        onClick = { onApplyCoupon(coupon.code) },
                                        label = { Text("Use ${coupon.code}") }
                                    )
                                }
                            }
                        }
                    }
                }

                // Bill summary
                item {
                    BillSummaryCard(cartSummary = cartSummary, orderType = checkoutDraft.orderType)
                }
            }

            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (cartSummary.subtotal < settings.minimumOrderAmount) {
                        Text(
                            text = "Add ${formatCurrency(settings.minimumOrderAmount - cartSummary.subtotal)} more to reach the ${formatCurrency(settings.minimumOrderAmount)} minimum order.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TfcCrimson
                        )
                    }
                    Button(
                        onClick = onProceedToCheckout,
                        enabled = settings.orderingOpen && cartSummary.subtotal >= settings.minimumOrderAmount,
                        colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("proceed_to_checkout_button")
                    ) {
                        Text(
                            text = if (!settings.orderingOpen) {
                                "Restaurant Ordering Closed"
                            } else {
                                "Proceed to Checkout • ${formatCurrency(cartSummary.totalAmount)}"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartSummary: CartSummary,
    checkoutDraft: CheckoutDraft,
    settings: RestaurantSettings,
    addressesState: UiState<List<SavedAddress>>,
    zonesState: UiState<List<DeliveryZone>>,
    isPlacingOrder: Boolean,
    onBack: () -> Unit,
    onSetOrderType: (OrderType) -> Unit,
    onSelectSavedAddress: (SavedAddress) -> Unit,
    onSetManualAddressMode: (Boolean) -> Unit,
    onUpdateManualAddress: (String, String, Double?, Double?) -> Unit,
    onSaveNewAddress: (SavedAddress) -> Unit,
    onSelectZone: (String) -> Unit,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
    onPlaceOrder: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedAddresses = (addressesState as? UiState.Success)?.data ?: emptyList()
    val zones = ((zonesState as? UiState.Success)?.data ?: emptyList()).filter { it.active }
    val selectedZone = zones.firstOrNull { it.id == checkoutDraft.selectedZoneId } ?: zones.firstOrNull()

    var gpsLoading by remember { mutableStateOf(false) }
    var gpsStatusMsg by remember { mutableStateOf<String?>(null) }
    var manualLabel by remember { mutableStateOf("Home") }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            gpsLoading = true
            gpsStatusMsg = null
            scope.launch {
                val resolved = fetchCurrentAddress(context)
                gpsLoading = false
                if (resolved != null) {
                    onUpdateManualAddress(
                        resolved.first,
                        checkoutDraft.manualLandmark,
                        resolved.second,
                        resolved.third
                    )
                    gpsStatusMsg = "GPS location detected"
                } else {
                    gpsStatusMsg = "Could not determine address from GPS. Please enter manually."
                }
            }
        } else {
            gpsStatusMsg = "Location permission denied. Enter your address manually below."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("checkout_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Button(
                    onClick = onPlaceOrder,
                    enabled = !isPlacingOrder && cartSummary.lines.isNotEmpty() && settings.orderingOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(54.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("place_order_button")
                ) {
                    if (isPlacingOrder) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = TfcPureWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Place Order • ${formatCurrency(cartSummary.totalAmount)}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("checkout_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Order Type Selection (Home Delivery vs Self Pickup)
                Text("Order Fulfillment", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OrderTypeOptionCard(
                        title = "Home Delivery",
                        subtitle = "${selectedZone?.estimatedMinutes ?: 30} mins",
                        icon = Icons.Default.DeliveryDining,
                        selected = checkoutDraft.orderType == OrderType.DELIVERY,
                        onClick = { onSetOrderType(OrderType.DELIVERY) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("order_type_delivery")
                    )
                    OrderTypeOptionCard(
                        title = "Self Pickup",
                        subtitle = "Zero delivery fee",
                        icon = Icons.Default.Storefront,
                        selected = checkoutDraft.orderType == OrderType.PICKUP,
                        onClick = { onSetOrderType(OrderType.PICKUP) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("order_type_pickup")
                    )
                }

                // 2A. Delivery Address Selection OR 2B. Pickup Selection
                if (checkoutDraft.orderType == OrderType.DELIVERY) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Delivery Address", style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(
                                onClick = {
                                    val fineGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    val coarseGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (fineGranted || coarseGranted) {
                                        gpsLoading = true
                                        scope.launch {
                                            val resolved = fetchCurrentAddress(context)
                                            gpsLoading = false
                                            if (resolved != null) {
                                                onUpdateManualAddress(
                                                    resolved.first,
                                                    checkoutDraft.manualLandmark,
                                                    resolved.second,
                                                    resolved.third
                                                )
                                                gpsStatusMsg = "GPS location detected"
                                            } else {
                                                gpsStatusMsg = "GPS location unavailable right now."
                                            }
                                        }
                                    } else {
                                        locationLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                enabled = !gpsLoading,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("use_gps_button")
                            ) {
                                if (gpsLoading) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text("Use Current GPS")
                            }
                        }

                        if (gpsStatusMsg != null) {
                            Text(
                                text = gpsStatusMsg ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = TfcCrimson
                            )
                        }

                        if (savedAddresses.isNotEmpty()) {
                            savedAddresses.forEach { addr ->
                                val isSelected = !checkoutDraft.useManualAddress &&
                                    (checkoutDraft.selectedAddressId == addr.id ||
                                        (checkoutDraft.selectedAddressId == null && addr.defaultAddress))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectSavedAddress(addr) },
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isSelected) TfcCrimson else MaterialTheme.colorScheme.outline
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) TfcSoftRed else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onSelectSavedAddress(addr) },
                                            colors = RadioButtonDefaults.colors(selectedColor = TfcCrimson)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(addr.label, style = MaterialTheme.typography.titleSmall)
                                            Text(addr.fullAddress, style = MaterialTheme.typography.bodySmall)
                                            if (addr.landmark.isNotBlank()) {
                                                Text(
                                                    "Landmark: ${addr.landmark}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        TextButton(
                            onClick = { onSetManualAddressMode(!checkoutDraft.useManualAddress || savedAddresses.isEmpty()) },
                            modifier = Modifier.testTag("toggle_manual_address_button")
                        ) {
                            Text(
                                if (checkoutDraft.useManualAddress || savedAddresses.isEmpty()) {
                                    "Entering address manually"
                                } else {
                                    "+ Enter a new address manually"
                                },
                                color = TfcCrimson
                            )
                        }

                        if (checkoutDraft.useManualAddress || savedAddresses.isEmpty()) {
                            OutlinedTextField(
                                value = checkoutDraft.manualAddress,
                                onValueChange = {
                                    onUpdateManualAddress(it, checkoutDraft.manualLandmark, null, null)
                                },
                                label = { Text("Full Street Address") },
                                placeholder = { Text("Apartment, street, area...") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_address_input")
                            )
                            OutlinedTextField(
                                value = checkoutDraft.manualLandmark,
                                onValueChange = {
                                    onUpdateManualAddress(checkoutDraft.manualAddress, it, null, null)
                                },
                                label = { Text("Landmark (Optional)") },
                                placeholder = { Text("Near central park, opposite bank...") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("landmark_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = manualLabel,
                                    onValueChange = { manualLabel = it },
                                    label = { Text("Label") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = {
                                        if (checkoutDraft.manualAddress.trim().length >= 3) {
                                            onSaveNewAddress(
                                                SavedAddress(
                                                    label = manualLabel.ifBlank { "Home" },
                                                    fullAddress = checkoutDraft.manualAddress.trim(),
                                                    landmark = checkoutDraft.manualLandmark.trim(),
                                                    zoneId = selectedZone?.id ?: "",
                                                    latitude = checkoutDraft.manualLat,
                                                    longitude = checkoutDraft.manualLng,
                                                    defaultAddress = savedAddresses.isEmpty()
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text("Save Address")
                                }
                            }
                        }

                        // Delivery Zones
                        if (zones.isNotEmpty()) {
                            Text("Delivery Zone", style = MaterialTheme.typography.titleMedium)
                            zones.forEach { zone ->
                                val isSelected = selectedZone?.id == zone.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectZone(zone.id) },
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isSelected) TfcCrimson else MaterialTheme.colorScheme.outline
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) TfcSoftRed else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { onSelectZone(zone.id) },
                                                colors = RadioButtonDefaults.colors(selectedColor = TfcCrimson)
                                            )
                                            Column {
                                                Text(zone.name, style = MaterialTheme.typography.titleSmall)
                                                Text(
                                                    "Est. ${zone.estimatedMinutes} mins • Min ${formatCurrency(zone.minOrderAmount)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = formatCurrency(zone.fee),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                color = TfcCrimson,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Self Pickup card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TfcSoftRed),
                        border = BorderStroke(1.dp, TfcCrimson.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = TfcCrimson)
                                Text(
                                    text = "TFC Restaurant Pickup Counter",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = settings.restaurantAddress,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = settings.pickupInstructions,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TfcCrimson, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Ready in ~${settings.estimatedPickupMinutes} mins",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(50)) {
                                    Text(
                                        text = "Delivery Fee: FREE (₹0)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TfcSuccessGreen,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Payment Selection
                Text("Payment Method", style = MaterialTheme.typography.titleMedium)
                val paymentChoices = if (checkoutDraft.orderType == OrderType.DELIVERY) {
                    listOf(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.ONLINE_PAYMENT)
                } else {
                    listOf(PaymentMethod.CASH_AT_PICKUP, PaymentMethod.ONLINE_PAYMENT)
                }
                paymentChoices.forEach { method ->
                    val selected = checkoutDraft.paymentMethod == method
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPaymentMethod(method) }
                            .testTag("payment_${method.name.lowercase()}"),
                        border = BorderStroke(
                            1.5.dp,
                            if (selected) TfcCrimson else MaterialTheme.colorScheme.outline
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) TfcSoftRed else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectPaymentMethod(method) },
                                colors = RadioButtonDefaults.colors(selectedColor = TfcCrimson)
                            )
                            Icon(Icons.Default.Payments, contentDescription = null, tint = TfcCrimson)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(method.raw, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    method.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 4. Final Summary
                BillSummaryCard(cartSummary = cartSummary, orderType = checkoutDraft.orderType)
            }
        }
    }
}

@Composable
private fun OrderTypeOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(2.dp, if (selected) TfcCrimson else MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) TfcSoftRed else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) TfcCrimson else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) TfcCrimson else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BillSummaryCard(
    cartSummary: CartSummary,
    orderType: OrderType
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Order Summary", style = MaterialTheme.typography.titleMedium)
            BillRow("Subtotal", formatCurrency(cartSummary.subtotal))
            if (cartSummary.discountAmount > 0) {
                BillRow("Coupon Discount", "-${formatCurrency(cartSummary.discountAmount)}", TfcSuccessGreen)
            }
            BillRow(
                label = if (orderType == OrderType.PICKUP) "Delivery Fee (Pickup)" else "Delivery Fee",
                value = if (cartSummary.deliveryFee == 0.0) "FREE (₹0)" else formatCurrency(cartSummary.deliveryFee)
            )
            BillRow("Estimated Time", "~${cartSummary.estimatedMinutes} mins")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Amount", style = MaterialTheme.typography.titleMedium)
                Text(
                    formatCurrency(cartSummary.totalAmount),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TfcCrimson,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }
    }
}

@Composable
private fun BillRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(color = valueColor, fontWeight = FontWeight.SemiBold))
    }
}

@SuppressLint("MissingPermission")
private suspend fun fetchCurrentAddress(context: Context): Triple<String, Double, Double>? {
    return try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val location = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            ?: client.lastLocation.await()
            ?: return null
        val addressText = withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val list = Geocoder(context, Locale.getDefault())
                    .getFromLocation(location.latitude, location.longitude, 1)
                val first = list?.firstOrNull()
                first?.getAddressLine(0) ?: "${location.latitude}, ${location.longitude}"
            } catch (_: Exception) {
                "${location.latitude}, ${location.longitude}"
            }
        }
        Triple(addressText, location.latitude, location.longitude)
    } catch (_: Exception) {
        null
    }
}
