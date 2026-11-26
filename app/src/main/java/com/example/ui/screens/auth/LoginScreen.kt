package com.example.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.AuthManager
import com.example.data.firebase.UserRepository
import com.example.data.model.LoginRole
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcJetBlack
import com.example.ui.theme.TfcPureWhite
import com.example.ui.theme.TfcSoftRed
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onAuthSuccess: (LoginRole) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var loadingRole by remember { mutableStateOf<LoginRole?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isLoading = loadingRole != null

    fun launchGoogleLogin(intendedRole: LoginRole) {
        errorMessage = null
        loadingRole = intendedRole
        AuthManager.onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = {
                scope.launch {
                    val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
                    val repo = UserRepository(db)
                    val verified = repo.verifyRoleAccess(intendedRole)
                    loadingRole = null
                    verified.fold(
                        onSuccess = { role -> onAuthSuccess(role) },
                        onFailure = { err ->
                            AuthManager.signOut(credentialManager, scope)
                            errorMessage = err.localizedMessage ?: "Unauthorized for ${intendedRole.label}"
                        }
                    )
                }
            },
            onAuthError = { msg ->
                loadingRole = null
                errorMessage = msg
            },
            onAuthCancelled = {
                loadingRole = null
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TfcJetBlack)
            .testTag("login_screen")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_banner),
            contentDescription = "TFC Restaurant signature platter",
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            TfcJetBlack.copy(alpha = 0.28f),
                            TfcJetBlack.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .padding(top = 104.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TfcCrimson)
                        .border(1.5.dp, TfcPureWhite.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TFC",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TfcPureWhite,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "TFC Restaurant",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = TfcPureWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Sign in securely with Google for Customer, Manager, or Restaurant Owner access.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TfcPureWhite.copy(alpha = 0.8f)
                    )
                )
            }

            Card(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = TfcPureWhite)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Google Sign-In Portals",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TfcJetBlack,
                            fontWeight = FontWeight.Bold
                        )
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = TfcCrimson)
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TfcJetBlack
                                )
                            }
                        }
                    }

                    // 1. CUSTOMER GOOGLE LOGIN
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = TfcSoftRed,
                        border = BorderStroke(1.dp, TfcCrimson.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(TfcCrimson),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = TfcPureWhite, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(
                                        text = "Customer Login",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = TfcJetBlack,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Browse TFC menu, order delivery or pickup & track live",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF52525B)
                                    )
                                }
                            }

                            Button(
                                onClick = { launchGoogleLogin(LoginRole.CUSTOMER) },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = TfcCrimson),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("google_sign_in_button")
                            ) {
                                if (loadingRole == LoginRole.CUSTOMER) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = TfcPureWhite
                                    )
                                } else {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Continue with Google", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }

                    // 2. MANAGER GOOGLE LOGIN
                    RoleLoginCard(
                        title = "Manager Login",
                        subtitle = "Sign in with Owner-authorized Google account to manage orders & stock",
                        icon = Icons.Default.ManageAccounts,
                        accentColor = TfcJetBlack,
                        enabled = !isLoading,
                        isLoading = loadingRole == LoginRole.MANAGER,
                        testTag = "manager_login_button",
                        onClick = { launchGoogleLogin(LoginRole.MANAGER) }
                    )

                    // 3. RESTAURANT OWNER GOOGLE LOGIN
                    RoleLoginCard(
                        title = "Restaurant Owner Login",
                        subtitle = "Pre-authorized Owner Google email for menu, pricing, categories & staff",
                        icon = Icons.Default.AdminPanelSettings,
                        accentColor = TfcCrimson,
                        enabled = !isLoading,
                        isLoading = loadingRole == LoginRole.OWNER,
                        testTag = "owner_login_button",
                        onClick = { launchGoogleLogin(LoginRole.OWNER) }
                    )

                    Surface(
                        color = Color(0xFFF4F4F5),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = TfcCrimson, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Roles are verified strictly on Firebase from your Google account — no OTP or SMS needed.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF52525B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleLoginCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    enabled: Boolean,
    isLoading: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFAFAFA),
        border = BorderStroke(1.dp, Color(0xFFE4E4E7)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = TfcPureWhite, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TfcJetBlack,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF52525B)
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = accentColor
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = title,
                    tint = accentColor
                )
            }
        }
    }
}
