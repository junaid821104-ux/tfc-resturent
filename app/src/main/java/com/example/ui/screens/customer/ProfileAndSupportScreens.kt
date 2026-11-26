package com.example.ui.screens.customer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import coil.compose.AsyncImage
import com.example.data.model.IndianPhoneValidator
import com.example.data.model.ManagerAccount
import com.example.data.model.RestaurantSettings
import com.example.data.model.SavedAddress
import com.example.data.model.SupportTicket
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcJetBlack
import com.example.ui.theme.TfcPureWhite
import com.example.ui.theme.TfcSoftRed

@Composable
fun ProfileScreen(
    currentUserId: String,
    userProfile: UserProfile?,
    addressesState: UiState<List<SavedAddress>>,
    myManagerRecord: ManagerAccount?,
    isSuperAdmin: Boolean,
    onUpdateProfile: (String, String) -> Unit,
    onSaveAddress: (SavedAddress) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onOpenSupport: () -> Unit,
    onOpenManagerDashboard: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onSignOut: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var showEditProfile by remember { mutableStateOf(false) }
    var showAddAddress by remember { mutableStateOf(false) }

    val savedAddresses = (addressesState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = TfcJetBlack)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val profileImg = userProfile?.effectiveProfileImage.orEmpty()
                            val displayName = userProfile?.effectiveName?.ifBlank { "TFC Guest" } ?: "TFC Guest"
                            val displayPhone = userProfile?.effectivePhone.orEmpty()
                            if (profileImg.isNotBlank()) {
                                AsyncImage(
                                    model = profileImg,
                                    contentDescription = "Profile photo",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(TfcCrimson),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (displayName.firstOrNull() ?: 'T').uppercase(),
                                        style = MaterialTheme.typography.titleLarge.copy(color = TfcPureWhite)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleLarge.copy(color = TfcPureWhite)
                                )
                                val roleLabel = when {
                                    isSuperAdmin -> "Restaurant Owner"
                                    myManagerRecord?.active == true -> "Manager"
                                    else -> "Customer"
                                }
                                Surface(
                                    color = TfcCrimson,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                                ) {
                                    Text(
                                        text = roleLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TfcPureWhite,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                if (!userProfile?.email.isNullOrBlank()) {
                                    Text(
                                        text = userProfile?.email ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TfcPureWhite.copy(alpha = 0.78f))
                                    )
                                }
                                if (displayPhone.isNotBlank()) {
                                    Text(
                                        text = displayPhone,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TfcPureWhite.copy(alpha = 0.78f))
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { showEditProfile = true },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit profile", tint = TfcPureWhite)
                        }
                    }

                    // Copyable User ID for staff provisioning
                    Surface(
                        color = TfcPureWhite.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UID: ${currentUserId.take(18)}...",
                                style = MaterialTheme.typography.labelSmall.copy(color = TfcPureWhite.copy(alpha = 0.8f))
                            )
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("TFC User ID", currentUserId))
                                    onShowMessage("User ID copied")
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TfcCrimson, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copy ID", color = TfcPureWhite, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Role-Gated Staff Portals (NEVER exposed to regular customer accounts)
        if (isSuperAdmin || myManagerRecord?.active == true) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = TfcSoftRed),
                    border = BorderStroke(1.dp, TfcCrimson.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Restaurant Operations Access",
                            style = MaterialTheme.typography.titleMedium.copy(color = TfcCrimson)
                        )
                        if (myManagerRecord?.active == true || isSuperAdmin) {
                            Button(
                                onClick = onOpenManagerDashboard,
                                colors = ButtonDefaults.buttonColors(containerColor = TfcJetBlack),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_manager_dashboard_button")
                            ) {
                                Icon(Icons.Default.ManageAccounts, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Open Manager Dashboard")
                            }
                        }
                        if (isSuperAdmin) {
                            Button(
                                onClick = onOpenAdminDashboard,
                                colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_admin_dashboard_button")
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Open Restaurant Owner Dashboard")
                            }
                        }
                    }
                }
            }
        }

        // Saved Delivery Addresses
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Saved Addresses", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showAddAddress = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add New", color = TfcCrimson)
                }
            }
        }

        if (savedAddresses.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = "No saved delivery addresses yet. Add Home or Work for faster checkout.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(savedAddresses, key = { it.id }) { addr ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = TfcCrimson)
                            Column {
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
                        IconButton(
                            onClick = { onDeleteAddress(addr.id) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete address")
                        }
                    }
                }
            }
        }

        // Customer Support & Sign Out
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onOpenSupport,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("customer_support_button")
                ) {
                    Icon(Icons.Default.HeadsetMic, contentDescription = null, tint = TfcCrimson)
                    Spacer(Modifier.width(8.dp))
                    Text("Customer Support & Help")
                }

                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = TfcCrimson)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", color = TfcCrimson)
                }
            }
        }
    }

    if (showEditProfile) {
        var name by remember(userProfile) { mutableStateOf(userProfile?.effectiveName ?: "") }
        var tenDigits by remember(userProfile) {
            mutableStateOf(
                userProfile?.effectivePhone?.let { IndianPhoneValidator.extractTenDigits(it) } ?: ""
            )
        }
        val validPhone = IndianPhoneValidator.isValidTenDigitIndianMobile(tenDigits)
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 80) name = it },
                        label = { Text("Full Name *") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = userProfile?.email.orEmpty(),
                        onValueChange = {},
                        enabled = false,
                        readOnly = true,
                        label = { Text("Google Email (Read-Only)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tenDigits,
                        onValueChange = { raw -> tenDigits = raw.filter { it.isDigit() }.take(10) },
                        label = { Text("🇮🇳 Indian Mobile (+91) *") },
                        prefix = { Text("+91 ") },
                        placeholder = { Text("9876543210") },
                        isError = tenDigits.isNotEmpty() && !validPhone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.trim().isNotEmpty() && validPhone) {
                            onUpdateProfile(name.trim(), tenDigits)
                            showEditProfile = false
                        }
                    },
                    enabled = name.trim().isNotEmpty() && validPhone,
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddAddress) {
        var label by remember { mutableStateOf("Home") }
        var address by remember { mutableStateOf("") }
        var landmark by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddAddress = false },
            title = { Text("Add Delivery Address") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label (Home / Work)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Full Address") }
                    )
                    OutlinedTextField(
                        value = landmark,
                        onValueChange = { landmark = it },
                        label = { Text("Landmark (Optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (address.trim().length >= 3) {
                            onSaveAddress(
                                SavedAddress(
                                    label = label.ifBlank { "Home" },
                                    fullAddress = address.trim(),
                                    landmark = landmark.trim(),
                                    defaultAddress = savedAddresses.isEmpty()
                                )
                            )
                            showAddAddress = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddAddress = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSupportScreen(
    settings: RestaurantSettings,
    ticketsState: UiState<List<SupportTicket>>,
    onBack: () -> Unit,
    onSubmitTicket: (String, String, String?, () -> Unit) -> Unit
) {
    BackHandler { onBack() }

    var subject by remember { mutableStateOf("") }
    var orderNumber by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val tickets = (ticketsState as? UiState.Success)?.data ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Support") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("support_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("support_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TfcJetBlack),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "TFC Guest Care (India +91)",
                            style = MaterialTheme.typography.titleLarge.copy(color = TfcPureWhite)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = TfcCrimson, modifier = Modifier.size(18.dp))
                            Text(settings.supportPhone, color = TfcPureWhite, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = TfcCrimson, modifier = Modifier.size(18.dp))
                            Text(settings.supportEmail, color = TfcPureWhite, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Text("Send a Support Request", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    placeholder = { Text("Missing item, delivery delay, billing...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("support_subject_input")
                )
                OutlinedTextField(
                    value = orderNumber,
                    onValueChange = { orderNumber = it },
                    label = { Text("Order ID (Optional)") },
                    placeholder = { Text("TFC-20261124-XXXX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("How can we help?") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("support_message_input")
                )
                Button(
                    onClick = {
                        onSubmitTicket(subject, message, orderNumber.ifBlank { null }) {
                            subject = ""
                            orderNumber = ""
                            message = ""
                        }
                    },
                    enabled = subject.trim().length >= 2 && message.trim().length >= 5,
                    colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("submit_support_button")
                ) {
                    Text("Submit Request")
                }

                if (tickets.isNotEmpty()) {
                    Text("Your Recent Tickets", style = MaterialTheme.typography.titleMedium)
                    tickets.forEach { ticket ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(ticket.subject, style = MaterialTheme.typography.titleSmall)
                                    Surface(color = TfcSoftRed, shape = RoundedCornerShape(50)) {
                                        Text(
                                            ticket.status,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TfcCrimson,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text(ticket.message, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
