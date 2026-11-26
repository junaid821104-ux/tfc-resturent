package com.example.ui.screens.staff

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Coupon
import com.example.data.model.DeliveryZone
import com.example.data.model.FoodCategory
import com.example.data.model.FoodItem
import com.example.data.model.ManagerAccount
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.RestaurantSettings
import com.example.data.model.SupportTicket
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.FoodPhoto
import com.example.ui.components.LoadingView
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.VegTypeBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcJetBlack
import com.example.ui.theme.TfcPureWhite
import com.example.ui.theme.TfcSoftRed
import com.example.ui.theme.TfcSuccessGreen

enum class OwnerSection(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    ORDERS("Orders", Icons.Default.ReceiptLong),
    FOOD_MENU("Food Menu", Icons.Default.Fastfood),
    CATEGORIES("Categories", Icons.Default.Category),
    ADD_FOOD("Add Food", Icons.Default.AddCircle),
    COUPONS("Coupons", Icons.Default.LocalOffer),
    MANAGERS("Managers", Icons.Default.ManageAccounts),
    SALES("Sales", Icons.Default.Analytics),
    SETTINGS("Restaurant Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    isSuperAdmin: Boolean,
    ordersState: UiState<List<Order>>,
    categoriesState: UiState<List<FoodCategory>>,
    foodItemsState: UiState<List<FoodItem>>,
    couponsState: UiState<List<Coupon>>,
    zonesState: UiState<List<DeliveryZone>>,
    settings: RestaurantSettings,
    managersState: UiState<List<ManagerAccount>>,
    usersState: UiState<List<UserProfile>>,
    ticketsState: UiState<List<SupportTicket>>,
    isAlarmPlaying: Boolean,
    hasNotificationPermission: Boolean,
    openOrdersToken: Int,
    onSilenceAlarm: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onBack: () -> Unit,
    onInitializeStarterCatalog: () -> Unit,
    onUpdateOrderStatus: (String, OrderStatus, String?, String?) -> Unit,
    onToggleFoodStock: (String, Boolean) -> Unit,
    onSaveCategory: (FoodCategory, () -> Unit) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveFoodItem: (FoodItem, () -> Unit) -> Unit,
    onDeleteFoodItem: (String) -> Unit,
    onSaveCoupon: (Coupon, () -> Unit) -> Unit,
    onDeleteCoupon: (String) -> Unit,
    onSaveDeliveryZone: (DeliveryZone, () -> Unit) -> Unit,
    onDeleteDeliveryZone: (String) -> Unit,
    onSaveSettings: (RestaurantSettings) -> Unit,
    onSaveManager: (ManagerAccount, Boolean, () -> Unit) -> Unit,
    onDeleteManager: (String) -> Unit,
    onResolveTicket: (String, String) -> Unit
) {
    BackHandler { onBack() }

    var selectedSection by remember { mutableStateOf(OwnerSection.DASHBOARD) }
    var editingFood by remember { mutableStateOf<FoodItem?>(null) }
    var showFoodDialog by remember { mutableStateOf(false) }

    LaunchedEffect(openOrdersToken) {
        if (openOrdersToken > 0) {
            selectedSection = OwnerSection.ORDERS
        }
    }

    val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TFC Owner Dashboard") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("admin_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAlarmPlaying) {
                        Button(
                            onClick = onSilenceAlarm,
                            colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("topbar_silence_alarm_button")
                        ) {
                            Icon(Icons.Default.VolumeOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Silence")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!isSuperAdmin) {
            BoxWithConstraints(Modifier.padding(innerPadding)) {
                EmptyStateView(
                    title = "Restaurant Owner Access Only",
                    subtitle = "Sign in with the pre-authorized Restaurant Owner Google account."
                )
            }
            return@Scaffold
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("admin_dashboard_screen")
        ) {
            val isWide = maxWidth >= 680.dp
            Row(modifier = Modifier.fillMaxSize()) {
                if (isWide) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        OwnerSection.entries.forEach { section ->
                            NavigationRailItem(
                                selected = selectedSection == section,
                                onClick = { selectedSection = section },
                                icon = { Icon(section.icon, contentDescription = section.title) },
                                label = { Text(section.title, maxLines = 1) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (!isWide) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(OwnerSection.entries, key = { it.name }) { section ->
                                val selected = selectedSection == section
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedSection = section },
                                    label = { Text(section.title) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = section.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TfcCrimson,
                                        selectedLabelColor = TfcPureWhite,
                                        selectedLeadingIconColor = TfcPureWhite
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("owner_section_${section.name.lowercase()}")
                                )
                            }
                        }
                    }

                    if (isAlarmPlaying) {
                        Surface(
                            color = TfcCrimson,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = TfcPureWhite)
                                    Text(
                                        text = "New order alarm ringing!",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = TfcPureWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                OutlinedButton(
                                    onClick = onSilenceAlarm,
                                    border = BorderStroke(1.dp, TfcPureWhite),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("silence_alarm_button")
                                ) {
                                    Text("Silence Alarm", color = TfcPureWhite)
                                }
                            }
                        }
                    }

                    when (selectedSection) {
                        OwnerSection.DASHBOARD -> OwnerDashboardHomeSection(
                            ordersState = ordersState,
                            foodItemsState = foodItemsState,
                            categoriesCount = categories.size,
                            managersCount = (managersState as? UiState.Success)?.data?.size ?: 0,
                            hasNotificationPermission = hasNotificationPermission,
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            onAddNewFoodClick = { selectedSection = OwnerSection.ADD_FOOD },
                            onInitializeStarterCatalog = onInitializeStarterCatalog,
                            onNavigateSection = { selectedSection = it },
                            onUpdateOrderStatus = onUpdateOrderStatus,
                            onSilenceAlarm = onSilenceAlarm
                        )
                        OwnerSection.ORDERS -> OwnerOrdersSection(
                            ordersState = ordersState,
                            onUpdateOrderStatus = onUpdateOrderStatus,
                            onSilenceAlarm = onSilenceAlarm
                        )
                        OwnerSection.FOOD_MENU -> OwnerFoodMenuSection(
                            foodItemsState = foodItemsState,
                            onAddNewFood = { selectedSection = OwnerSection.ADD_FOOD },
                            onEditFood = { item ->
                                editingFood = item
                                showFoodDialog = true
                            },
                            onToggleStock = onToggleFoodStock,
                            onDeleteFood = onDeleteFoodItem
                        )
                        OwnerSection.CATEGORIES -> OwnerCategoriesSection(
                            categoriesState = categoriesState,
                            onSaveCategory = onSaveCategory,
                            onDeleteCategory = onDeleteCategory
                        )
                        OwnerSection.ADD_FOOD -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Column(
                                    modifier = Modifier
                                        .widthIn(max = 640.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("+ ADD NEW FOOD", style = MaterialTheme.typography.headlineMedium, color = TfcCrimson)
                                    Text(
                                        "New dishes save directly to Firestore and appear immediately across the TFC Customer App.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OwnerFoodEditorContent(
                                        initial = null,
                                        categories = categories,
                                        submitButtonLabel = "Save Menu Item",
                                        resetAfterSave = true,
                                        onSave = { item, done ->
                                            onSaveFoodItem(item) {
                                                done()
                                                selectedSection = OwnerSection.FOOD_MENU
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        OwnerSection.COUPONS -> OwnerCouponsSection(
                            couponsState = couponsState,
                            onSaveCoupon = onSaveCoupon,
                            onDeleteCoupon = onDeleteCoupon
                        )
                        OwnerSection.MANAGERS -> OwnerManagersSection(
                            managersState = managersState,
                            onSaveManager = onSaveManager,
                            onDeleteManager = onDeleteManager
                        )
                        OwnerSection.SALES -> OwnerSalesSection(
                            ordersState = ordersState,
                            usersState = usersState,
                            ticketsState = ticketsState,
                            onResolveTicket = onResolveTicket
                        )
                        OwnerSection.SETTINGS -> OwnerSettingsSection(
                            settings = settings,
                            zonesState = zonesState,
                            onSaveSettings = onSaveSettings,
                            onSaveZone = onSaveDeliveryZone,
                            onDeleteZone = onDeleteDeliveryZone
                        )
                    }
                }
            }
        }
    }

    if (showFoodDialog) {
        FoodItemEditorDialog(
            initial = editingFood,
            categories = categories,
            onDismiss = { showFoodDialog = false },
            onSave = { item ->
                onSaveFoodItem(item) { showFoodDialog = false }
            }
        )
    }
}

@Composable
private fun OwnerDashboardHomeSection(
    ordersState: UiState<List<Order>>,
    foodItemsState: UiState<List<FoodItem>>,
    categoriesCount: Int,
    managersCount: Int,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onAddNewFoodClick: () -> Unit,
    onInitializeStarterCatalog: () -> Unit,
    onNavigateSection: (OwnerSection) -> Unit,
    onUpdateOrderStatus: (String, OrderStatus, String?, String?) -> Unit,
    onSilenceAlarm: () -> Unit
) {
    val orders = (ordersState as? UiState.Success)?.data ?: emptyList()
    val foodItems = (foodItemsState as? UiState.Success)?.data ?: emptyList()
    val nonCancelled = orders.filter { OrderStatus.fromRaw(it.status) != OrderStatus.CANCELLED }
    val totalRevenue = nonCancelled.sumOf { it.totalAmount }
    val activeOrders = orders.count { !OrderStatus.fromRaw(it.status).isTerminal }
    val newPendingOrders = orders.filter { OrderStatus.fromRaw(it.status) == OrderStatus.ORDER_PLACED }
    var rejectingOrder by remember { mutableStateOf<Order?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (hasNotificationPermission) Color(0xFFDCFCE7) else TfcSoftRed
                ),
                border = BorderStroke(
                    1.dp,
                    if (hasNotificationPermission) TfcSuccessGreen.copy(alpha = 0.45f) else TfcCrimson.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (hasNotificationPermission) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (hasNotificationPermission) TfcSuccessGreen else TfcCrimson
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasNotificationPermission) {
                                "Owner Live Order Alarm Active"
                            } else {
                                "Enable Notification Permission"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (hasNotificationPermission) {
                                "Plays loud alarm & sends instant background notification for every new customer order."
                            } else {
                                "Grant notification permission so new TFC orders alert your phone even in background."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (!hasNotificationPermission) {
                        Button(
                            onClick = onRequestNotificationPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("grant_notifications_button")
                        ) {
                            Text("Enable")
                        }
                    }
                }
            }
        }

        if (newPendingOrders.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Incoming Orders Waiting (${newPendingOrders.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TfcCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    TextButton(onClick = { onNavigateSection(OwnerSection.ORDERS) }) {
                        Text("View All", color = TfcCrimson)
                    }
                }
            }
            items(newPendingOrders.take(3), key = { "home_${it.id}" }) { order ->
                OwnerOrderActionCard(
                    order = order,
                    onAccept = {
                        onSilenceAlarm()
                        onUpdateOrderStatus(order.id, OrderStatus.ACCEPTED, null, null)
                    },
                    onReject = {
                        onSilenceAlarm()
                        rejectingOrder = order
                    },
                    onAdvance = { next ->
                        val payStatus = if (next == OrderStatus.DELIVERED) "Paid" else null
                        onUpdateOrderStatus(order.id, next, null, payStatus)
                    },
                    onOverrideStatus = { status ->
                        onUpdateOrderStatus(order.id, status, null, if (status == OrderStatus.DELIVERED) "Paid" else null)
                    }
                )
            }
        }

        item {
            Button(
                onClick = onAddNewFoodClick,
                colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("owner_add_new_food_cta")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "+ ADD NEW FOOD",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TfcPureWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }

        if (foodItems.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TfcSoftRed),
                    border = BorderStroke(1.dp, TfcCrimson.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Quick Starter Catalog", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Seed starter TFC Indian Rupees (₹) categories, signature buckets & burgers, delivery zones, and TFC20 coupon in one tap, or add dishes manually above.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = onInitializeStarterCatalog,
                            colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("init_starter_catalog_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Initialize TFC Starter Menu (₹)")
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Total Revenue", formatCurrency(totalRevenue), Modifier.weight(1f), highlight = true)
                StatCard("Active Orders", activeOrders.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Menu Items", foodItems.size.toString(), Modifier.weight(1f))
                StatCard("Categories / Team", "$categoriesCount / $managersCount", Modifier.weight(1f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { onNavigateSection(OwnerSection.FOOD_MENU) },
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                ) { Text("Manage Menu") }
                OutlinedButton(
                    onClick = { onNavigateSection(OwnerSection.MANAGERS) },
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                ) { Text("Manage Staff") }
            }
        }
    }

    rejectingOrder?.let { target ->
        RejectOrderReasonDialog(
            orderNumber = target.orderNumber,
            onDismiss = { rejectingOrder = null },
            onConfirmReject = { reason ->
                onUpdateOrderStatus(target.id, OrderStatus.CANCELLED, reason, null)
                rejectingOrder = null
            }
        )
    }
}

@Composable
private fun OwnerOrdersSection(
    ordersState: UiState<List<Order>>,
    onUpdateOrderStatus: (String, OrderStatus, String?, String?) -> Unit,
    onSilenceAlarm: () -> Unit
) {
    var showCompleted by remember { mutableStateOf(false) }
    var rejectingOrder by remember { mutableStateOf<Order?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !showCompleted,
                onClick = { showCompleted = false },
                label = { Text("New & Active") }
            )
            FilterChip(
                selected = showCompleted,
                onClick = { showCompleted = true },
                label = { Text("Completed & Cancelled") }
            )
        }

        when (ordersState) {
            is UiState.Loading -> LoadingView()
            is UiState.Error -> ErrorStateView(ordersState.message)
            is UiState.Success -> {
                val filtered = ordersState.data.filter { order ->
                    val terminal = OrderStatus.fromRaw(order.status).isTerminal
                    if (showCompleted) terminal else !terminal
                }
                if (filtered.isEmpty()) {
                    EmptyStateView(
                        title = if (showCompleted) "No Completed Orders" else "No Incoming Orders",
                        subtitle = "Customer orders save to Firestore and stream here in real time."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = if (showCompleted) "Completed & Cancelled (${filtered.size})" else "Incoming & Active Orders (${filtered.size})",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        items(filtered, key = { it.id }) { order ->
                            OwnerOrderActionCard(
                                order = order,
                                onAccept = {
                                    onSilenceAlarm()
                                    onUpdateOrderStatus(order.id, OrderStatus.ACCEPTED, null, null)
                                },
                                onReject = {
                                    onSilenceAlarm()
                                    rejectingOrder = order
                                },
                                onAdvance = { next ->
                                    val payStatus = if (next == OrderStatus.DELIVERED) "Paid" else null
                                    onUpdateOrderStatus(order.id, next, null, payStatus)
                                },
                                onOverrideStatus = { st ->
                                    val payStatus = if (st == OrderStatus.DELIVERED) "Paid" else null
                                    onUpdateOrderStatus(order.id, st, null, payStatus)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    rejectingOrder?.let { target ->
        RejectOrderReasonDialog(
            orderNumber = target.orderNumber,
            onDismiss = { rejectingOrder = null },
            onConfirmReject = { reason ->
                onUpdateOrderStatus(target.id, OrderStatus.CANCELLED, reason, null)
                rejectingOrder = null
            }
        )
    }
}

@Composable
private fun OwnerOrderActionCard(
    order: Order,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onAdvance: (OrderStatus) -> Unit,
    onOverrideStatus: (OrderStatus) -> Unit
) {
    var showStatusDialog by remember { mutableStateOf(false) }
    val status = OrderStatus.fromRaw(order.status)
    val isPickup = OrderType.fromRaw(order.orderType) == OrderType.PICKUP
    val nextStatus = when (status) {
        OrderStatus.ACCEPTED -> OrderStatus.PREPARING
        OrderStatus.PREPARING -> OrderStatus.READY
        OrderStatus.READY -> if (isPickup) OrderStatus.DELIVERED else OrderStatus.OUT_FOR_DELIVERY
        OrderStatus.OUT_FOR_DELIVERY -> OrderStatus.DELIVERED
        else -> null
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            if (status == OrderStatus.ORDER_PLACED) 1.5.dp else 1.dp,
            if (status == OrderStatus.ORDER_PLACED) TfcCrimson else MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (status == OrderStatus.ORDER_PLACED) TfcSoftRed.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(order.orderNumber, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${order.customerName} • ${if (isPickup) "SELF PICKUP" else "HOME DELIVERY"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OrderStatusBadge(order.status)
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (order.customerPhone.isNotBlank() || order.customerEmail.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = TfcCrimson, modifier = Modifier.size(14.dp))
                            Text(
                                text = listOfNotNull(
                                    order.customerPhone.takeIf { it.isNotBlank() },
                                    order.customerEmail.takeIf { it.isNotBlank() }
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    if (!order.deliveryAddress.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = TfcCrimson, modifier = Modifier.size(14.dp))
                            Text(
                                text = buildString {
                                    append(order.deliveryAddress)
                                    if (!order.landmark.isNullOrBlank()) append(" (${order.landmark})")
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            order.items.forEach { item ->
                val extra = listOfNotNull(
                    item.variantName.takeIf { it.isNotBlank() },
                    item.addonsSummary.takeIf { it.isNotBlank() }?.let { "+ $it" }
                ).joinToString(" • ")
                Text(
                    text = "• ${item.quantity}x ${item.name}" + if (extra.isNotBlank()) " [$extra]" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (!order.cookingInstructions.isNullOrBlank()) {
                Text(
                    text = "Cooking Note: ${order.cookingInstructions}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TfcCrimson
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: ${formatCurrency(order.totalAmount)} • ${order.paymentMethod}",
                    style = MaterialTheme.typography.titleSmall.copy(color = TfcJetBlack, fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = { showStatusDialog = true }) {
                    Text("Change Status", color = TfcCrimson)
                }
            }

            if (status == OrderStatus.ORDER_PLACED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        border = BorderStroke(1.dp, TfcCrimson),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("owner_reject_order_${order.id}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = TfcCrimson, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reject", color = TfcCrimson)
                    }
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = TfcSuccessGreen),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("owner_accept_order_${order.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Accept")
                    }
                }
            } else if (nextStatus != null) {
                Button(
                    onClick = { onAdvance(nextStatus) },
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Mark as ${nextStatus.raw}")
                }
            }
        }
    }

    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Update ${order.orderNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OrderStatus.entries.forEach { st ->
                        OutlinedButton(
                            onClick = {
                                onOverrideStatus(st)
                                showStatusDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(st.raw) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun RejectOrderReasonDialog(
    orderNumber: String,
    onDismiss: () -> Unit,
    onConfirmReject: (String) -> Unit
) {
    var reason by remember(orderNumber) { mutableStateOf("Kitchen at capacity") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject $orderNumber") },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Rejection Reason") },
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReject(reason.ifBlank { "Order rejected by restaurant" }) },
                colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson)
            ) {
                Text("Reject Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun OwnerFoodMenuSection(
    foodItemsState: UiState<List<FoodItem>>,
    onAddNewFood: () -> Unit,
    onEditFood: (FoodItem) -> Unit,
    onToggleStock: (String, Boolean) -> Unit,
    onDeleteFood: (String) -> Unit
) {
    when (foodItemsState) {
        is UiState.Loading -> LoadingView()
        is UiState.Error -> ErrorStateView(foodItemsState.message)
        is UiState.Success -> {
            val items = foodItemsState.data
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Button(
                        onClick = onAddNewFood,
                        colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("menu_add_new_food_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "+ ADD NEW FOOD",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                }
                if (items.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "Your Food Menu is Empty",
                            subtitle = "Tap + ADD NEW FOOD above to add unlimited dishes with photos, ₹ prices, variants, and add-ons."
                        )
                    }
                } else {
                    items(items, key = { it.effectiveId }) { item ->
                        Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FoodPhoto(
                                    imageUrl = item.imageUrl,
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        VegTypeBadge(item.foodType)
                                        Text(item.name, style = MaterialTheme.typography.titleSmall)
                                    }
                                    Text(
                                        "${item.effectiveCategory} • ${formatCurrency(item.effectivePrice)} • ${if (item.isAvailable) "In Stock" else "Out of Stock"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (item.isAvailable) TfcSuccessGreen else TfcCrimson
                                    )
                                    val meta = buildList {
                                        if (item.isPopular) add("Popular")
                                        if (item.variants.isNotEmpty()) add("${item.variants.size} variants")
                                        if (item.addons.isNotEmpty()) add("${item.addons.size} add-ons")
                                    }.joinToString(" • ")
                                    if (meta.isNotBlank()) {
                                        Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Switch(
                                        checked = item.isAvailable,
                                        onCheckedChange = { onToggleStock(item.effectiveId, it) }
                                    )
                                    Row {
                                        IconButton(onClick = { onEditFood(item) }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                                        }
                                        IconButton(onClick = { onDeleteFood(item.effectiveId) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TfcCrimson)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
