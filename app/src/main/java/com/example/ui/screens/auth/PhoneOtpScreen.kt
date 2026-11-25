package com.example.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.firebase.AuthManager
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcPureWhite
import com.example.ui.theme.TfcSoftRed
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneOtpScreen(
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var phoneInput by remember { mutableStateOf("+1") }
    var otpInput by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resendSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(resendSeconds) {
        if (resendSeconds > 0) {
            delay(1000)
            resendSeconds -= 1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mobile OTP Login") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("phone_otp_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to login")
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (verificationId == null) "Enter your mobile number" else "Verify 6-digit OTP",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = if (verificationId == null) {
                        "Customers and Restaurant Managers can sign in with Firebase Phone OTP (E.164 format, e.g. +15551234567)."
                    } else {
                        "Enter the 6-digit verification code sent to $phoneInput."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (errorMessage != null) {
                    Surface(
                        color = TfcSoftRed,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = TfcCrimson)
                            Text(errorMessage ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (verificationId == null) {
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Mobile Number") },
                        placeholder = { Text("+15551234567") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_number_input")
                    )

                    Button(
                        onClick = {
                            val trimmed = phoneInput.trim()
                            if (!trimmed.startsWith("+") || trimmed.length < 8) {
                                errorMessage = "Enter a valid phone number with country code (e.g. +15551234567)"
                                return@Button
                            }
                            if (activity == null) {
                                errorMessage = "Unable to access Activity for phone verification"
                                return@Button
                            }
                            errorMessage = null
                            isLoading = true
                            AuthManager.sendPhoneOtp(
                                activity = activity,
                                phoneNumberE164 = trimmed,
                                resendToken = resendToken,
                                onCodeSent = { verId, token ->
                                    isLoading = false
                                    verificationId = verId
                                    resendToken = token
                                    resendSeconds = 60
                                },
                                onAutoVerified = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                }
                            )
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("send_otp_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = TfcPureWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send Verification Code", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { if (it.length <= 6) otpInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("6-Digit OTP Code") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_code_input")
                    )

                    Button(
                        onClick = {
                            val verId = verificationId ?: return@Button
                            if (otpInput.length < 6) {
                                errorMessage = "Enter the 6-digit OTP code"
                                return@Button
                            }
                            errorMessage = null
                            isLoading = true
                            scope.launch {
                                val res = AuthManager.verifyPhoneOtpCode(verId, otpInput)
                                isLoading = false
                                res.fold(
                                    onSuccess = { onAuthSuccess() },
                                    onFailure = { e -> errorMessage = e.localizedMessage ?: "Invalid OTP code" }
                                )
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("verify_otp_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = TfcPureWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Verify & Sign In", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                verificationId = null
                                otpInput = ""
                                errorMessage = null
                            }
                        ) {
                            Text("Change Number")
                        }
                        OutlinedButton(
                            enabled = resendSeconds == 0 && !isLoading && activity != null,
                            onClick = {
                                if (activity != null) {
                                    isLoading = true
                                    AuthManager.sendPhoneOtp(
                                        activity = activity,
                                        phoneNumberE164 = phoneInput.trim(),
                                        resendToken = resendToken,
                                        onCodeSent = { verId, token ->
                                            isLoading = false
                                            verificationId = verId
                                            resendToken = token
                                            resendSeconds = 60
                                        },
                                        onAutoVerified = {
                                            isLoading = false
                                            onAuthSuccess()
                                        },
                                        onError = { msg ->
                                            isLoading = false
                                            errorMessage = msg
                                        }
                                    )
                                }
                            }
                        ) {
                            Text(
                                text = if (resendSeconds > 0) "Resend in ${resendSeconds}s" else "Resend OTP",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Use Google Sign-In instead")
                }
            }
        }
    }
}
