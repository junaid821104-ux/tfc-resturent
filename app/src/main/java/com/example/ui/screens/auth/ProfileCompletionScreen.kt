package com.example.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.IndianPhoneValidator
import com.example.data.model.LoginRole
import com.example.data.model.UserProfile
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcJetBlack
import com.example.ui.theme.TfcPureWhite
import com.example.ui.theme.TfcSoftRed
import com.example.ui.theme.TfcSuccessGreen

@Composable
fun ProfileCompletionScreen(
    userProfile: UserProfile?,
    fallbackName: String,
    fallbackEmail: String,
    fallbackPhotoUrl: String,
    role: LoginRole,
    isSaving: Boolean,
    onSaveProfile: (fullName: String, tenDigitIndianMobile: String) -> Unit,
    onSignOut: () -> Unit
) {
    val initialName = (userProfile?.effectiveName?.takeIf { it.isNotBlank() } ?: fallbackName).trim()
    val readOnlyEmail = (userProfile?.email?.takeIf { it.isNotBlank() } ?: fallbackEmail).trim()
    val photoUrl = (userProfile?.effectiveProfileImage?.takeIf { it.isNotBlank() } ?: fallbackPhotoUrl).trim()
    val initialDigits = userProfile?.effectivePhone
        ?.takeIf { it.isNotBlank() }
        ?.let { IndianPhoneValidator.extractTenDigits(it) }
        .orEmpty()

    var fullName by remember(initialName) { mutableStateOf(initialName) }
    var tenDigitMobile by remember(initialDigits) { mutableStateOf(initialDigits) }

    val cleanDigits = tenDigitMobile.filter { it.isDigit() }.take(10)
    val isValidMobile = IndianPhoneValidator.isValidTenDigitIndianMobile(cleanDigits)
    val isValidName = fullName.trim().length in 1..80
    val showMobileHintError = cleanDigits.isNotEmpty() && !isValidMobile

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TfcJetBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("profile_completion_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Google profile photo",
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .border(2.dp, TfcCrimson, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(TfcCrimson)
                            .border(2.dp, TfcPureWhite.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (fullName.firstOrNull() ?: readOnlyEmail.firstOrNull() ?: 'T').uppercase(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = TfcPureWhite,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }

                Surface(
                    color = TfcCrimson,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${role.label.uppercase()} ACCOUNT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TfcPureWhite,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Complete Your TFC Profile",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TfcPureWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    text = "Add your 10-digit Indian mobile number so our kitchen & delivery team can reach you.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TfcPureWhite.copy(alpha = 0.78f)
                    )
                )
            }

            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = TfcPureWhite)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { if (it.length <= 80) fullName = it },
                        label = { Text("Full Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TfcCrimson) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )

                    OutlinedTextField(
                        value = readOnlyEmail,
                        onValueChange = {},
                        enabled = false,
                        readOnly = true,
                        label = { Text("Google Email (Read-Only)") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.Lock, contentDescription = "Verified read-only email") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_email_readonly")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Indian Mobile Number *",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = TfcJetBlack,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = TfcSoftRed,
                                border = BorderStroke(1.dp, TfcCrimson.copy(alpha = 0.4f)),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🇮🇳", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "+91",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = TfcCrimson,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = cleanDigits,
                                onValueChange = { raw ->
                                    tenDigitMobile = raw.filter { it.isDigit() }.take(10)
                                },
                                placeholder = { Text("9876543210") },
                                leadingIcon = {
                                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = TfcCrimson)
                                },
                                isError = showMobileHintError,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_mobile_input")
                            )
                        }
                        if (showMobileHintError) {
                            Text(
                                text = "Enter a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TfcCrimson
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = TfcSuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Manual entry only — saved directly with no OTP or SMS verification.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF52525B)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onSaveProfile(fullName.trim(), cleanDigits) },
                        enabled = !isSaving && isValidName && isValidMobile,
                        colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("complete_profile_save_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = TfcPureWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Save & Continue",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = TfcPureWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    TextButton(
                        onClick = onSignOut,
                        enabled = !isSaving,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .minimumInteractiveComponentSize()
                            .testTag("complete_profile_signout_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = TfcCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Use a different Google account", color = TfcCrimson)
                    }
                }
            }
        }
    }
}
