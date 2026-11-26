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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FoodItem
import com.example.data.model.ManagerAccount
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.UiState
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
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(
    managerRecord: ManagerAccount?,
    isSuperAdmin: Boolean,
    ordersState: UiState<List<Order>>,
    foodItemsState: UiState<List<FoodItem>>,
    onBack: () -> Unit,
    onUpdateOrderStatus: (String, OrderStatus, String?, String?) -> Unit,
    onToggleFoodStock: (String, Boolean) -> Unit
) {
    BackHandler { onBack() }

    val hasAccess = isSuperAdmin || (managerRecord?.active == true)
    val canViewOrders = isSuperAdmin || (managerRecord?.active == true && (managerRecord.canViewOrders || managerRecord.canManageOrders))
    val canManageOrders = isSuperAdmin || (managerRecord?.active == true && managerRecord.canManageOrders)
    val canViewMenu = isSuperAdmin || (managerRecord?.active == true && (managerRecord.canViewMenu || managerRecord.canManageMenu))
    val canManageMenu = isSuperAdmin || (managerRecord?.active == true && managerRecord.canManageMenu)
    val canViewSales = isSuperAdmin || (managerRecord?.active == true && managerRecord.canViewSales)

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Orders", "Menu Stock", "Daily Sales")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TFC Manager Portal")
                        if (!isSuperAdmin && managerRecord != null) {
                            Text(
                                text = "${managerRecord.name} • ${managerRecord.email.ifBlank { managerRecord.phone }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("manager_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!hasAccess) {
            BoxWithConstraints(Modifier.padding(innerPadding)) {
                EmptyStateView(
                    title = "Manager Access Required",
                    subtitle = "Your Google account does not have active Manager permissions. Ask the Restaurant Owner to add your Google email."
                )
            }
            return@Scaffold
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("manager_dashboard_screen")
        ) {
            val isWide = maxWidth >= 680.dp
            Row(modifier = Modifier.fillMaxSize()) {
                if (isWide) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                            label = { Text("Orders") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Fastfood, contentDescription = null) },
                            label = { Text("Menu") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                            label = { Text("Sales") }
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (!isWide) {
                        PrimaryTabRow(selectedTabIndex = selectedTab) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title) }
                                )
                            }
                        }
                    }

                    when (selectedTab) {
                        0 -> ManagerOrdersTab(
                            canViewOrders = canViewOrders,
                            canManageOrders = canManageOrders,
                            ordersState = ordersState,
                            onUpdateOrderStatus = onUpdateOrderStatus
                        )
                        1 -> ManagerMenuTab(
                            canViewMenu = canViewMenu,
                            canManageMenu = canManageMenu,
                            foodItemsState = foodItemsState,
                            onToggleFoodStock = onToggleFoodStock
                        )
                        2 -> ManagerDailySalesTab(
                            canViewSales = canViewSales,
                            ordersState = ordersState
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerOrdersTab(
    canViewOrders: Boolean,
    canManageOrders: Boolean,
    ordersState: UiState<List<Order>>,
    onUpdateOrderStatus: (String, OrderStatus, String?, String?) -> Unit
) {
    if (!canViewOrders) {
        EmptyStateView("Permission Disabled", "The Restaurant Owner has disabled order viewing for your account.")
        return
    }
    var showCompleted by remember { mutableStateOf(false) }
    var rejectingOrderId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !showCompleted,
                onClick = { showCompleted = false },
                label = { Text("Incoming & Active") }
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
                        subtitle = "Orders placed by TFC customers appear here in real time."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { order ->
                            ManagerOrderCard(
                                order = order,
                                canManageOrders = canManageOrders,
                                onAccept = {
                                    onUpdateOrderStatus(order.id, OrderStatus.ACCEPTED, null, null)
                                },
                                onReject = { rejectingOrderId = order.id },
                                onAdvance = { next ->
                                    val payStatus = if (next == OrderStatus.DELIVERED) "Paid" else null
                                    onUpdateOrderStatus(order.id, next, null, payStatus)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (rejectingOrderId != null) {
        var reason by remember { mutableStateOf("Kitchen at capacity") }
        AlertDialog(
            onDismissRequest = { rejectingOrderId = null },
            title = { Text("Reject Order") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Rejection Reason") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = rejectingOrderId ?: return@Button
                        onUpdateOrderStatus(id, OrderStatus.CANCELLED, reason, null)
                        rejectingOrderId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson)
                ) { Text("Reject Order") }
            },
            dismissButton = {
                TextButton(onClick = { rejectingOrderId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ManagerOrderCard(
    order: Order,
    canManageOrders: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onAdvance: (OrderStatus) -> Unit
) {
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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

            // Customer contact & address information
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
            Text(
                text = "Total: ${formatCurrency(order.totalAmount)} (${order.paymentMethod})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            if (canManageOrders) {
                if (status == OrderStatus.ORDER_PLACED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReject,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reject", color = TfcCrimson)
                        }
                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(containerColor = TfcSuccessGreen),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
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
    }
}

@Composable
private fun ManagerMenuTab(
    canViewMenu: Boolean,
    canManageMenu: Boolean,
    foodItemsState: UiState<List<FoodItem>>,
    onToggleFoodStock: (String, Boolean) -> Unit
) {
    if (!canViewMenu) {
        EmptyStateView("Permission Disabled", "The Restaurant Owner has disabled menu access for your account.")
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = TfcSoftRed,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (canManageMenu) {
                    "Toggle dish availability (In Stock / Out of Stock). Creating or editing prices is restricted to the Restaurant Owner."
                } else {
                    "View-only menu mode. Stock updates are disabled for your manager account."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TfcCrimson,
                modifier = Modifier.padding(12.dp)
            )
        }

        when (foodItemsState) {
            is UiState.Loading -> LoadingView()
            is UiState.Error -> ErrorStateView(foodItemsState.message)
            is UiState.Success -> {
                if (foodItemsState.data.isEmpty()) {
                    EmptyStateView(
                        title = "No Menu Items",
                        subtitle = "The Restaurant Owner has not published menu items yet."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(foodItemsState.data, key = { it.effectiveId }) { item ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
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
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            VegTypeBadge(item.foodType)
                                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                                        }
                                        Text(
                                            text = "${item.effectiveCategory} • ${formatCurrency(item.effectivePrice)} • ${if (item.isAvailable) "In Stock" else "Out of Stock"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (item.isAvailable) TfcSuccessGreen else TfcCrimson
                                        )
                                    }
                                    Switch(
                                        checked = item.isAvailable,
                                        enabled = canManageMenu,
                                        onCheckedChange = { onToggleFoodStock(item.effectiveId, it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerDailySalesTab(
    canViewSales: Boolean,
    ordersState: UiState<List<Order>>
) {
    if (!canViewSales) {
        EmptyStateView("Permission Disabled", "You do not have permission to view sales.")
        return
    }
    when (ordersState) {
        is UiState.Loading -> LoadingView()
        is UiState.Error -> ErrorStateView(ordersState.message)
        is UiState.Success -> {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = cal.time
            val todaysOrders = ordersState.data.filter { order ->
                val d = order.createdAt?.toDate()
                d != null && !d.before(startOfToday)
            }
            val validToday = todaysOrders.filter { OrderStatus.fromRaw(it.status) != OrderStatus.CANCELLED }
            val revenueToday = validToday.sumOf { it.totalAmount }
            val deliveredToday = validToday.count { OrderStatus.fromRaw(it.status) == OrderStatus.DELIVERED }
            val activeNow = validToday.count { !OrderStatus.fromRaw(it.status).isTerminal }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Today's Kitchen Summary", style = MaterialTheme.typography.headlineMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("Today's Sales", formatCurrency(revenueToday), Modifier.weight(1f), highlight = true)
                    StatCard("Orders Today", validToday.size.toString(), Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("Active Kitchen", activeNow.toString(), Modifier.weight(1f))
                    StatCard("Delivered", deliveredToday.toString(), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) TfcJetBlack else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (highlight) TfcPureWhite.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = if (highlight) TfcCrimson else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }
    }
}
