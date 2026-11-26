package com.example.ui.screens.staff

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Coupon
import com.example.data.model.DeliveryZone
import com.example.data.model.DiscountType
import com.example.data.model.EmailDocIdHelper
import com.example.data.model.FoodCategory
import com.example.data.model.IndianPhoneValidator
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
import com.example.ui.components.LoadingView
import com.example.ui.components.formatCurrency
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcJetBlack
import com.example.ui.theme.TfcSoftRed
import java.util.Calendar

@Composable
fun OwnerCategoriesSection(
    categoriesState: UiState<List<FoodCategory>>,
    onSaveCategory: (FoodCategory, () -> Unit) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var editingCategory by remember { mutableStateOf<FoodCategory?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Categories (${categories.size})", style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = { editingCategory = null; showCategoryDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Category")
                }
            }
        }
        if (categories.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No Categories Yet",
                    subtitle = "Create categories such as Crispy Buckets, Burgers, Wraps, or Beverages."
                )
            }
        } else {
            items(categories, key = { it.effectiveId }) { cat ->
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Sort: ${cat.sortOrder} • ${if (cat.active) "Active" else "Hidden"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row {
                            IconButton(onClick = { editingCategory = cat; showCategoryDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit category")
                            }
                            IconButton(onClick = { onDeleteCategory(cat.effectiveId) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete category", tint = TfcCrimson)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        var name by remember { mutableStateOf(editingCategory?.name ?: "") }
        var desc by remember { mutableStateOf(editingCategory?.description ?: "") }
        var sortText by remember { mutableStateOf((editingCategory?.sortOrder ?: 1L).toString()) }
        var active by remember { mutableStateOf(editingCategory?.active ?: true) }
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text(if (editingCategory == null) "Add Category" else "Edit Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                    OutlinedTextField(
                        value = sortText,
                        onValueChange = { sortText = it },
                        label = { Text("Sort Order") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCategory(
                            FoodCategory(
                                docId = editingCategory?.effectiveId ?: "",
                                id = editingCategory?.effectiveId ?: "",
                                name = name.trim(),
                                description = desc.trim(),
                                sortOrder = sortText.toLongOrNull() ?: 1L,
                                active = active
                            )
                        ) { showCategoryDialog = false }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun OwnerCouponsSection(
    couponsState: UiState<List<Coupon>>,
    onSaveCoupon: (Coupon, () -> Unit) -> Unit,
    onDeleteCoupon: (String) -> Unit
) {
    var editingCoupon by remember { mutableStateOf<Coupon?>(null) }
    var showCouponDialog by remember { mutableStateOf(false) }
    val coupons = (couponsState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Coupons & Offers (${coupons.size})", style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = { editingCoupon = null; showCouponDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Coupon")
                }
            }
        }
        if (coupons.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No Coupons Active",
                    subtitle = "Create percentage or flat ₹ discount codes for TFC customers."
                )
            }
        } else {
            items(coupons, key = { it.id }) { coupon ->
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${coupon.code} — ${coupon.title}", style = MaterialTheme.typography.titleSmall)
                            val valLabel = if (DiscountType.fromRaw(coupon.discountType) == DiscountType.PERCENT) {
                                "${coupon.discountValue.toInt()}% OFF"
                            } else {
                                "${formatCurrency(coupon.discountValue)} OFF"
                            }
                            Text(
                                "$valLabel • Min ${formatCurrency(coupon.minOrderAmount)} • ${if (coupon.active) "Active" else "Paused"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row {
                            IconButton(onClick = { editingCoupon = coupon; showCouponDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit coupon")
                            }
                            IconButton(onClick = { onDeleteCoupon(coupon.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete coupon", tint = TfcCrimson)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCouponDialog) {
        var code by remember { mutableStateOf(editingCoupon?.code ?: "") }
        var title by remember { mutableStateOf(editingCoupon?.title ?: "") }
        var desc by remember { mutableStateOf(editingCoupon?.description ?: "") }
        var type by remember { mutableStateOf(DiscountType.fromRaw(editingCoupon?.discountType)) }
        var valueText by remember { mutableStateOf((editingCoupon?.discountValue ?: 20.0).toString()) }
        var minOrderText by remember { mutableStateOf((editingCoupon?.minOrderAmount ?: 199.0).toString()) }
        var maxDiscText by remember { mutableStateOf((editingCoupon?.maxDiscount ?: 150.0).toString()) }
        var active by remember { mutableStateOf(editingCoupon?.active ?: true) }
        AlertDialog(
            onDismissRequest = { showCouponDialog = false },
            title = { Text(if (editingCoupon == null) "Add Coupon" else "Edit Coupon") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("Code (e.g. TFC20)") })
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Offer Title") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = type == DiscountType.PERCENT, onClick = { type = DiscountType.PERCENT }, label = { Text("Percent %") })
                        FilterChip(selected = type == DiscountType.FLAT, onClick = { type = DiscountType.FLAT }, label = { Text("Flat ₹") })
                    }
                    OutlinedTextField(value = valueText, onValueChange = { valueText = it }, label = { Text("Discount Value") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = minOrderText, onValueChange = { minOrderText = it }, label = { Text("Min Order (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = maxDiscText, onValueChange = { maxDiscText = it }, label = { Text("Max Discount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Active")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCoupon(
                            Coupon(
                                id = editingCoupon?.id ?: "",
                                code = code.trim(),
                                title = title.trim(),
                                description = desc.trim(),
                                discountType = type.raw,
                                discountValue = valueText.toDoubleOrNull() ?: 10.0,
                                minOrderAmount = minOrderText.toDoubleOrNull() ?: 0.0,
                                maxDiscount = maxDiscText.toDoubleOrNull() ?: 0.0,
                                active = active
                            )
                        ) { showCouponDialog = false }
                    },
                    enabled = code.trim().length >= 2 && title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showCouponDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun OwnerManagersSection(
    managersState: UiState<List<ManagerAccount>>,
    onSaveManager: (ManagerAccount, Boolean, () -> Unit) -> Unit,
    onDeleteManager: (String) -> Unit
) {
    var showManagerDialog by remember { mutableStateOf(false) }
    var editingManager by remember { mutableStateOf<ManagerAccount?>(null) }
    val managers = (managersState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Managers (${managers.size})", style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = { editingManager = null; showManagerDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                    modifier = Modifier.testTag("owner_add_manager_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Manager")
                }
            }
        }
        item {
            Surface(color = TfcSoftRed, shape = RoundedCornerShape(12.dp)) {
                Text(
                    text = "Authorize Managers using their Google Email address. Managers sign in via Manager Login -> Google Sign-In and cannot create owner accounts or change owner permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TfcCrimson,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        if (managers.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No Managers Added",
                    subtitle = "Tap Add Manager to authorize a manager's Google email address and assign permissions."
                )
            }
        } else {
            items(managers, key = { it.id.ifBlank { it.userId } }) { mgr ->
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(mgr.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${mgr.email.ifBlank { mgr.phone }} • ${if (mgr.active) "Active" else "Suspended"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (mgr.phone.isNotBlank() && mgr.email.isNotBlank()) {
                                Text(
                                    mgr.phone,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val permList = listOfNotNull(
                                if (mgr.canViewOrders) "View Orders" else null,
                                if (mgr.canManageOrders) "Accept/Reject/Update Orders" else null,
                                if (mgr.canViewMenu) "View Menu" else null,
                                if (mgr.canManageMenu) "Stock Toggle" else null,
                                if (mgr.canViewSales) "Daily Sales" else null
                            )
                            Text(
                                text = "Permissions: ${permList.joinToString(", ").ifBlank { "None" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TfcCrimson
                            )
                        }
                        Row {
                            IconButton(onClick = { editingManager = mgr; showManagerDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit manager")
                            }
                            IconButton(onClick = { onDeleteManager(mgr.id.ifBlank { mgr.userId }) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove manager", tint = TfcCrimson)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManagerDialog) {
        val isEdit = editingManager != null
        var email by remember { mutableStateOf(editingManager?.email ?: "") }
        var name by remember { mutableStateOf(editingManager?.name ?: "") }
        var tenDigitPhone by remember {
            mutableStateOf(
                editingManager?.phone?.takeIf { it.isNotBlank() }?.let { IndianPhoneValidator.extractTenDigits(it) } ?: ""
            )
        }
        var active by remember { mutableStateOf(editingManager?.active ?: true) }
        var canViewOrders by remember { mutableStateOf(editingManager?.canViewOrders ?: true) }
        var canManageOrders by remember { mutableStateOf(editingManager?.canManageOrders ?: true) }
        var canViewMenu by remember { mutableStateOf(editingManager?.canViewMenu ?: true) }
        var canManageMenu by remember { mutableStateOf(editingManager?.canManageMenu ?: true) }
        var canViewSales by remember { mutableStateOf(editingManager?.canViewSales ?: true) }

        val validEmail = EmailDocIdHelper.isValidEmail(email)
        val validOptionalPhone = tenDigitPhone.isEmpty() || IndianPhoneValidator.isValidTenDigitIndianMobile(tenDigitPhone)

        AlertDialog(
            onDismissRequest = { showManagerDialog = false },
            title = { Text(if (isEdit) "Edit Manager" else "Add Manager (Google Email)") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Manager Full Name *") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        enabled = !isEdit,
                        label = { Text("Manager Google Email *") },
                        placeholder = { Text("manager@gmail.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tenDigitPhone,
                        onValueChange = { tenDigitPhone = it.filter { ch -> ch.isDigit() }.take(10) },
                        label = { Text("🇮🇳 Indian Mobile (+91) (Optional)") },
                        prefix = { Text("+91 ") },
                        placeholder = { Text("9876543210") },
                        isError = !validOptionalPhone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Account Active")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("View Orders & Customer Info")
                        Switch(checked = canViewOrders, onCheckedChange = { canViewOrders = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Accept / Reject / Update Orders")
                        Switch(checked = canManageOrders, onCheckedChange = { canManageOrders = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("View Menu")
                        Switch(checked = canViewMenu, onCheckedChange = { canViewMenu = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Update Item Availability")
                        Switch(checked = canManageMenu, onCheckedChange = { canManageMenu = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("View Daily Sales")
                        Switch(checked = canViewSales, onCheckedChange = { canViewSales = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val normalizedEmail = email.trim().lowercase()
                        val e164 = if (IndianPhoneValidator.isValidTenDigitIndianMobile(tenDigitPhone)) {
                            "+91$tenDigitPhone"
                        } else {
                            ""
                        }
                        val docId = if (isEdit) {
                            editingManager!!.id.ifBlank { editingManager!!.userId }
                        } else {
                            EmailDocIdHelper.fromEmail(normalizedEmail)
                        }
                        onSaveManager(
                            ManagerAccount(
                                id = docId,
                                userId = docId,
                                name = name.trim(),
                                email = normalizedEmail,
                                phone = e164,
                                active = active,
                                canViewOrders = canViewOrders,
                                canManageOrders = canManageOrders,
                                canViewMenu = canViewMenu,
                                canManageMenu = canManageMenu,
                                canViewSales = canViewSales
                            ),
                            isEdit
                        ) { showManagerDialog = false }
                    },
                    enabled = validEmail && validOptionalPhone && name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson)
                ) { Text("Save Manager") }
            },
            dismissButton = { TextButton(onClick = { showManagerDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun OwnerSalesSection(
    ordersState: UiState<List<Order>>,
    usersState: UiState<List<UserProfile>>,
    ticketsState: UiState<List<SupportTicket>>,
    onResolveTicket: (String, String) -> Unit
) {
    val customers = (usersState as? UiState.Success)?.data ?: emptyList()
    val tickets = (ticketsState as? UiState.Success)?.data ?: emptyList()

    when (ordersState) {
        is UiState.Loading -> LoadingView()
        is UiState.Error -> ErrorStateView(ordersState.message)
        is UiState.Success -> {
            val orders = ordersState.data
            val nonCancelled = orders.filter { OrderStatus.fromRaw(it.status) != OrderStatus.CANCELLED }
            val totalRevenue = nonCancelled.sumOf { it.totalAmount }
            val avgOrder = if (nonCancelled.isNotEmpty()) totalRevenue / nonCancelled.size else 0.0
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = cal.time
            val todayRevenue = nonCancelled
                .filter { it.createdAt?.toDate()?.let { d -> !d.before(startOfToday) } == true }
                .sumOf { it.totalAmount }
            val deliveryCount = orders.count { OrderType.fromRaw(it.orderType) == OrderType.DELIVERY }
            val pickupCount = orders.count { OrderType.fromRaw(it.orderType) == OrderType.PICKUP }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("Sales & Business Analytics", style = MaterialTheme.typography.titleLarge) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard("Total Revenue", formatCurrency(totalRevenue), Modifier.weight(1f), highlight = true)
                        StatCard("Today's Sales", formatCurrency(todayRevenue), Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard("Avg Order", formatCurrency(avgOrder), Modifier.weight(1f))
                        StatCard("Delivery / Pickup", "$deliveryCount / $pickupCount", Modifier.weight(1f))
                    }
                }
                item {
                    Text(
                        "Registered Customers (${customers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(customers.take(20), key = { it.effectiveUid }) { user ->
                    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.effectiveName.ifBlank { "TFC User" }, style = MaterialTheme.typography.titleSmall)
                                Text(user.effectivePhone.ifBlank { user.email }, style = MaterialTheme.typography.bodySmall)
                            }
                            Surface(color = TfcSoftRed, shape = RoundedCornerShape(50)) {
                                Text(
                                    text = user.role.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(color = TfcCrimson, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                if (tickets.isNotEmpty()) {
                    item {
                        Text(
                            "Support Tickets (${tickets.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(tickets, key = { it.id }) { ticket ->
                        Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ticket.subject, style = MaterialTheme.typography.titleSmall)
                                    Text(ticket.message, style = MaterialTheme.typography.bodySmall)
                                    Text("${ticket.userEmail} • ${ticket.status}", style = MaterialTheme.typography.labelSmall, color = TfcCrimson)
                                }
                                if (ticket.status == "Open") {
                                    OutlinedButton(onClick = { onResolveTicket(ticket.id, "Resolved") }) {
                                        Text("Resolve")
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

@Composable
fun OwnerSettingsSection(
    settings: RestaurantSettings,
    zonesState: UiState<List<DeliveryZone>>,
    onSaveSettings: (RestaurantSettings) -> Unit,
    onSaveZone: (DeliveryZone, () -> Unit) -> Unit,
    onDeleteZone: (String) -> Unit
) {
    var address by remember(settings) { mutableStateOf(settings.restaurantAddress) }
    var pickupNotes by remember(settings) { mutableStateOf(settings.pickupInstructions) }
    var hours by remember(settings) { mutableStateOf(settings.openingHours) }
    var orderingOpen by remember(settings) { mutableStateOf(settings.orderingOpen) }
    var minOrder by remember(settings) { mutableStateOf(settings.minimumOrderAmount.toString()) }
    var baseFee by remember(settings) { mutableStateOf(settings.baseDeliveryFee.toString()) }
    var pickupMins by remember(settings) { mutableStateOf(settings.estimatedPickupMinutes.toString()) }
    var supportPhone by remember(settings) { mutableStateOf(settings.supportPhone) }
    var supportEmail by remember(settings) { mutableStateOf(settings.supportEmail) }
    var bannerTitle by remember(settings) { mutableStateOf(settings.activeBannerTitle) }
    var bannerSub by remember(settings) { mutableStateOf(settings.activeBannerSubtitle) }

    var showZoneDialog by remember { mutableStateOf(false) }
    var editingZone by remember { mutableStateOf<DeliveryZone?>(null) }
    val zones = (zonesState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Restaurant Settings", style = MaterialTheme.typography.titleLarge) }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TfcSoftRed),
                border = BorderStroke(1.dp, TfcCrimson.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Restaurant Ordering", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (orderingOpen) "Customers can place new orders" else "Ordering paused for customers",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(checked = orderingOpen, onCheckedChange = { orderingOpen = it })
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Restaurant Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pickupNotes, onValueChange = { pickupNotes = it }, label = { Text("Pickup Instructions") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Opening Hours") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minOrder,
                        onValueChange = { minOrder = it },
                        label = { Text("Min Order (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = baseFee,
                        onValueChange = { baseFee = it },
                        label = { Text("Base Delivery (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(value = pickupMins, onValueChange = { pickupMins = it }, label = { Text("Pickup ETA (mins)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = supportPhone, onValueChange = { supportPhone = it }, label = { Text("Customer Support Phone (+91)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = supportEmail, onValueChange = { supportEmail = it }, label = { Text("Customer Support Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bannerTitle, onValueChange = { bannerTitle = it }, label = { Text("Home Banner Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bannerSub, onValueChange = { bannerSub = it }, label = { Text("Home Banner Subtitle") }, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = {
                        onSaveSettings(
                            settings.copy(
                                restaurantAddress = address,
                                pickupInstructions = pickupNotes,
                                openingHours = hours,
                                orderingOpen = orderingOpen,
                                minimumOrderAmount = minOrder.toDoubleOrNull() ?: settings.minimumOrderAmount,
                                baseDeliveryFee = baseFee.toDoubleOrNull() ?: settings.baseDeliveryFee,
                                estimatedPickupMinutes = pickupMins.toLongOrNull() ?: settings.estimatedPickupMinutes,
                                supportPhone = supportPhone,
                                supportEmail = supportEmail,
                                ownerPhone = "",
                                activeBannerTitle = bannerTitle,
                                activeBannerSubtitle = bannerSub
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .minimumInteractiveComponentSize()
                ) { Text("Save Restaurant Settings") }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Delivery Zones (${zones.size})", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = { editingZone = null; showZoneDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TfcJetBlack)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Zone")
                }
            }
        }

        items(zones, key = { it.id }) { zone ->
            Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(zone.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Fee ${formatCurrency(zone.fee)} • ${zone.estimatedMinutes} mins • Min ${formatCurrency(zone.minOrderAmount)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row {
                        IconButton(onClick = { editingZone = zone; showZoneDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit zone")
                        }
                        IconButton(onClick = { onDeleteZone(zone.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete zone", tint = TfcCrimson)
                        }
                    }
                }
            }
        }
    }

    if (showZoneDialog) {
        var name by remember { mutableStateOf(editingZone?.name ?: "") }
        var fee by remember { mutableStateOf((editingZone?.fee ?: 30.0).toString()) }
        var eta by remember { mutableStateOf((editingZone?.estimatedMinutes ?: 30L).toString()) }
        var minOrd by remember { mutableStateOf((editingZone?.minOrderAmount ?: 149.0).toString()) }
        var active by remember { mutableStateOf(editingZone?.active ?: true) }
        AlertDialog(
            onDismissRequest = { showZoneDialog = false },
            title = { Text(if (editingZone == null) "Add Delivery Zone" else "Edit Delivery Zone") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Zone Name") })
                    OutlinedTextField(value = fee, onValueChange = { fee = it }, label = { Text("Delivery Fee (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = eta, onValueChange = { eta = it }, label = { Text("Estimated Minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = minOrd, onValueChange = { minOrd = it }, label = { Text("Min Order (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Active")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveZone(
                            DeliveryZone(
                                id = editingZone?.id ?: "",
                                name = name.trim(),
                                fee = fee.toDoubleOrNull() ?: 0.0,
                                estimatedMinutes = eta.toLongOrNull() ?: 30L,
                                minOrderAmount = minOrd.toDoubleOrNull() ?: 0.0,
                                active = active
                            )
                        ) { showZoneDialog = false }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showZoneDialog = false }) { Text("Cancel") } }
        )
    }
}
