package com.example.ui.screens.customer

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.firebase.AuthManager
import com.example.data.firebase.MenuRepository
import com.example.data.firebase.OrderRepository
import com.example.data.firebase.UserRepository
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.PhoneOtpScreen
import com.example.ui.screens.auth.SplashScreen
import com.example.ui.screens.staff.AdminDashboardScreen
import com.example.ui.screens.staff.ManagerDashboardScreen
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcPureWhite
import com.example.ui.theme.TfcSoftRed
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.StaffViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val ORDERS = "orders"
    const val CART = "cart"
    const val PROFILE = "profile"
    const val CATEGORIES = "categories/{categoryId}"
    const val FOOD_DETAIL = "food_detail/{itemId}"
    const val CHECKOUT = "checkout"
    const val ORDER_CONFIRMATION = "order_confirmation/{orderId}/{orderNumber}/{eta}/{total}"
    const val ORDER_TRACKING = "order_tracking/{orderId}"
    const val SUPPORT = "support"
    const val MANAGER_DASHBOARD = "manager_dashboard"
    const val ADMIN_DASHBOARD = "admin_dashboard"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomTabs = listOf(
    BottomNavTab(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavTab(Routes.SEARCH, "Search", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavTab(Routes.ORDERS, "My Orders", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    BottomNavTab(Routes.CART, "Cart", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
    BottomNavTab(Routes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun TfcAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showSplash by remember { mutableStateOf(true) }
    var showPhoneOtp by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                showPhoneOtp = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        AuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
            onUnauthenticated = { }
        )
    }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    val user = currentUser
    if (user == null) {
        if (showPhoneOtp) {
            PhoneOtpScreen(
                onBack = { showPhoneOtp = false },
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
        } else {
            LoginScreen(
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onNavigateToPhoneOtp = { showPhoneOtp = true }
            )
        }
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedTfcShell(
                currentUserId = uid,
                onSignOut = {
                    AuthManager.signOut(credentialManager, scope) {
                        currentUser = null
                    }
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedTfcShell(
    currentUserId: String,
    onSignOut: () -> Unit
) {
    val customerViewModel: CustomerViewModel = viewModel(
        key = "customer_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                CustomerViewModel(
                    menuRepository = MenuRepository(db),
                    orderRepository = OrderRepository(db),
                    userRepository = UserRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val staffViewModel: StaffViewModel = viewModel(
        key = "staff_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                StaffViewModel(
                    menuRepository = MenuRepository(db),
                    orderRepository = OrderRepository(db),
                    userRepository = UserRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(customerViewModel, staffViewModel) {
        merge(customerViewModel.events, staffViewModel.events).collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val categoriesState by customerViewModel.categories.collectAsStateWithLifecycle()
    val foodItemsState by customerViewModel.foodItems.collectAsStateWithLifecycle()
    val couponsState by customerViewModel.coupons.collectAsStateWithLifecycle()
    val zonesState by customerViewModel.deliveryZones.collectAsStateWithLifecycle()
    val settings by customerViewModel.settings.collectAsStateWithLifecycle()
    val userProfile by customerViewModel.userProfile.collectAsStateWithLifecycle()
    val addressesState by customerViewModel.addresses.collectAsStateWithLifecycle()
    val myOrdersState by customerViewModel.myOrders.collectAsStateWithLifecycle()
    val ticketsState by customerViewModel.supportTickets.collectAsStateWithLifecycle()
    val myManagerRecord by customerViewModel.myManagerRecord.collectAsStateWithLifecycle()
    val isSuperAdmin by customerViewModel.isSuperAdmin.collectAsStateWithLifecycle()
    val checkoutDraft by customerViewModel.checkoutDraft.collectAsStateWithLifecycle()
    val cartSummary by customerViewModel.cartSummary.collectAsStateWithLifecycle()
    val isPlacingOrder by customerViewModel.isPlacingOrder.collectAsStateWithLifecycle()

    val allOrdersState by staffViewModel.allOrders.collectAsStateWithLifecycle()
    val allManagersState by staffViewModel.allManagers.collectAsStateWithLifecycle()
    val allUsersState by staffViewModel.allUsers.collectAsStateWithLifecycle()
    val allTicketsState by staffViewModel.allSupportTickets.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topLevelRoutes = remember { setOf(Routes.HOME, Routes.SEARCH, Routes.ORDERS, Routes.CART, Routes.PROFILE) }
    val showBottomBar = currentRoute in topLevelRoutes

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = showBottomBar && maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar && !useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    if (tab.route == Routes.CART && cartSummary.itemCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = TfcCrimson, contentColor = TfcPureWhite) {
                                                    Text(cartSummary.itemCount.toString())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TfcCrimson,
                                    selectedTextColor = TfcCrimson,
                                    indicatorColor = TfcSoftRed
                                ),
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        bottomTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    if (tab.route == Routes.CART && cartSummary.itemCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = TfcCrimson, contentColor = TfcPureWhite) {
                                                    Text(cartSummary.itemCount.toString())
                                                }
                                            }
                                        ) {
                                            Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = TfcCrimson,
                                    selectedTextColor = TfcCrimson,
                                    indicatorColor = TfcSoftRed
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME
                    ) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                settings = settings,
                                categoriesState = categoriesState,
                                foodItemsState = foodItemsState,
                                couponsState = couponsState,
                                addressesState = addressesState,
                                checkoutDraft = checkoutDraft,
                                cartSummary = cartSummary,
                                canOpenAdminOrManager = isSuperAdmin || (myManagerRecord?.active == true),
                                onOpenSearch = {
                                    navController.navigate(Routes.SEARCH) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onOpenCategory = { catId ->
                                    navController.navigate("categories/${Uri.encode(catId)}")
                                },
                                onOpenFoodDetail = { itemId ->
                                    navController.navigate("food_detail/${Uri.encode(itemId)}")
                                },
                                onOpenAddressCheckout = {
                                    navController.navigate(Routes.CHECKOUT)
                                },
                                onOpenStaffSetup = {
                                    if (isSuperAdmin) {
                                        navController.navigate(Routes.ADMIN_DASHBOARD)
                                    } else {
                                        navController.navigate(Routes.MANAGER_DASHBOARD)
                                    }
                                },
                                onAddToCart = { customerViewModel.addToCart(it) },
                                onIncrement = { customerViewModel.incrementItem(it) },
                                onDecrement = { customerViewModel.decrementItem(it) },
                                onApplyCoupon = { customerViewModel.applyCouponCode(it) }
                            )
                        }

                        composable(Routes.SEARCH) {
                            SearchScreen(
                                categoriesState = categoriesState,
                                foodItemsState = foodItemsState,
                                cartSummary = cartSummary,
                                onOpenFoodDetail = { itemId ->
                                    navController.navigate("food_detail/${Uri.encode(itemId)}")
                                },
                                onAddToCart = { customerViewModel.addToCart(it) },
                                onIncrement = { customerViewModel.incrementItem(it) },
                                onDecrement = { customerViewModel.decrementItem(it) }
                            )
                        }

                        composable(Routes.ORDERS) {
                            OrdersScreen(
                                ordersState = myOrdersState,
                                onOpenTracking = { orderId ->
                                    navController.navigate("order_tracking/${Uri.encode(orderId)}")
                                },
                                onReorder = { order ->
                                    customerViewModel.reorderFromOrder(order)
                                    navController.navigate(Routes.CART) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onBrowseMenu = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.HOME) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Routes.CART) {
                            CartScreen(
                                cartSummary = cartSummary,
                                checkoutDraft = checkoutDraft,
                                settings = settings,
                                couponsState = couponsState,
                                onIncrement = { customerViewModel.incrementItem(it) },
                                onDecrement = { customerViewModel.decrementItem(it) },
                                onRemove = { customerViewModel.removeFromCart(it) },
                                onCookingInstructionsChange = { customerViewModel.updateCookingInstructions(it) },
                                onApplyCoupon = { customerViewModel.applyCouponCode(it) },
                                onRemoveCoupon = { customerViewModel.removeCoupon() },
                                onBrowseMenu = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.HOME) { inclusive = true }
                                    }
                                },
                                onProceedToCheckout = { navController.navigate(Routes.CHECKOUT) }
                            )
                        }

                        composable(Routes.PROFILE) {
                            ProfileScreen(
                                currentUserId = currentUserId,
                                userProfile = userProfile,
                                addressesState = addressesState,
                                myManagerRecord = myManagerRecord,
                                isSuperAdmin = isSuperAdmin,
                                onUpdateProfile = { name, phone -> customerViewModel.updateProfile(name, phone) },
                                onSaveAddress = { customerViewModel.saveAddress(it) },
                                onDeleteAddress = { customerViewModel.deleteAddress(it) },
                                onOpenSupport = { navController.navigate(Routes.SUPPORT) },
                                onOpenManagerDashboard = { navController.navigate(Routes.MANAGER_DASHBOARD) },
                                onOpenAdminDashboard = { navController.navigate(Routes.ADMIN_DASHBOARD) },
                                onSignOut = onSignOut,
                                onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                        }

                        composable(
                            route = Routes.CATEGORIES,
                            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
                        ) { entry ->
                            val categoryId = entry.arguments?.getString("categoryId") ?: "ALL"
                            CategoryMenuScreen(
                                initialCategoryId = categoryId,
                                categoriesState = categoriesState,
                                foodItemsState = foodItemsState,
                                cartSummary = cartSummary,
                                onBack = { navController.popBackStack() },
                                onOpenFoodDetail = { itemId ->
                                    navController.navigate("food_detail/${Uri.encode(itemId)}")
                                },
                                onAddToCart = { customerViewModel.addToCart(it) },
                                onIncrement = { customerViewModel.incrementItem(it) },
                                onDecrement = { customerViewModel.decrementItem(it) }
                            )
                        }

                        composable(
                            route = Routes.FOOD_DETAIL,
                            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
                        ) { entry ->
                            val itemId = entry.arguments?.getString("itemId") ?: ""
                            FoodDetailScreen(
                                itemId = itemId,
                                foodItemsState = foodItemsState,
                                onBack = { navController.popBackStack() },
                                onAddToCart = { item, qty -> customerViewModel.addToCart(item, qty) }
                            )
                        }

                        composable(Routes.CHECKOUT) {
                            CheckoutScreen(
                                cartSummary = cartSummary,
                                checkoutDraft = checkoutDraft,
                                settings = settings,
                                addressesState = addressesState,
                                zonesState = zonesState,
                                isPlacingOrder = isPlacingOrder,
                                onBack = { navController.popBackStack() },
                                onSetOrderType = { customerViewModel.setOrderType(it) },
                                onSelectSavedAddress = { customerViewModel.selectSavedAddress(it) },
                                onSetManualAddressMode = { customerViewModel.setManualAddressMode(it) },
                                onUpdateManualAddress = { addr, lm, lat, lng ->
                                    customerViewModel.updateManualAddress(addr, lm, lat, lng)
                                },
                                onSaveNewAddress = { customerViewModel.saveAddress(it, selectAfterSave = true) },
                                onSelectZone = { customerViewModel.selectDeliveryZone(it) },
                                onSelectPaymentMethod = { customerViewModel.selectPaymentMethod(it) },
                                onPlaceOrder = {
                                    customerViewModel.placeOrder { placed ->
                                        val encodedId = Uri.encode(placed.id)
                                        val encodedNum = Uri.encode(placed.orderNumber)
                                        navController.navigate("order_confirmation/$encodedId/$encodedNum/${placed.estimatedMinutes}/${placed.totalAmount}") {
                                            popUpTo(Routes.HOME)
                                        }
                                    }
                                }
                            )
                        }

                        composable(
                            route = Routes.ORDER_CONFIRMATION,
                            arguments = listOf(
                                navArgument("orderId") { type = NavType.StringType },
                                navArgument("orderNumber") { type = NavType.StringType },
                                navArgument("eta") { type = NavType.LongType },
                                navArgument("total") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val orderId = entry.arguments?.getString("orderId") ?: ""
                            val orderNum = entry.arguments?.getString("orderNumber") ?: ""
                            val eta = entry.arguments?.getLong("eta") ?: 25L
                            val total = entry.arguments?.getString("total")?.toDoubleOrNull() ?: 0.0
                            OrderConfirmationScreen(
                                orderNumber = orderNum,
                                estimatedMinutes = eta,
                                totalAmount = total,
                                onTrackOrder = {
                                    navController.navigate("order_tracking/${Uri.encode(orderId)}") {
                                        popUpTo(Routes.HOME)
                                    }
                                },
                                onBackHome = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.HOME) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            route = Routes.ORDER_TRACKING,
                            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                        ) { entry ->
                            val orderId = entry.arguments?.getString("orderId") ?: ""
                            val orderFlow = remember(orderId) { customerViewModel.observeSingleOrder(orderId) }
                            OrderTrackingScreen(
                                orderFlow = orderFlow,
                                onBack = { navController.popBackStack() },
                                onCancelOrder = { customerViewModel.cancelOrder(it) }
                            )
                        }

                        composable(Routes.SUPPORT) {
                            CustomerSupportScreen(
                                settings = settings,
                                ticketsState = ticketsState,
                                onBack = { navController.popBackStack() },
                                onSubmitTicket = { sub, msg, ord, done ->
                                    customerViewModel.submitSupportTicket(sub, msg, ord, done)
                                }
                            )
                        }

                        composable(Routes.MANAGER_DASHBOARD) {
                            ManagerDashboardScreen(
                                managerRecord = myManagerRecord,
                                isSuperAdmin = isSuperAdmin,
                                ordersState = allOrdersState,
                                categoriesState = categoriesState,
                                foodItemsState = foodItemsState,
                                onBack = { navController.popBackStack() },
                                onUpdateOrderStatus = { id, status, reason, pay ->
                                    staffViewModel.updateOrderStatus(id, status, reason, pay)
                                },
                                onToggleFoodStock = { id, avail ->
                                    staffViewModel.toggleFoodAvailability(id, avail)
                                },
                                onSaveFoodItem = { item, done ->
                                    staffViewModel.saveFoodItem(item, done)
                                }
                            )
                        }

                        composable(Routes.ADMIN_DASHBOARD) {
                            AdminDashboardScreen(
                                isSuperAdmin = isSuperAdmin,
                                ordersState = allOrdersState,
                                categoriesState = categoriesState,
                                foodItemsState = foodItemsState,
                                couponsState = couponsState,
                                zonesState = zonesState,
                                settings = settings,
                                managersState = allManagersState,
                                usersState = allUsersState,
                                ticketsState = allTicketsState,
                                onBack = { navController.popBackStack() },
                                onInitializeStarterCatalog = { staffViewModel.initializeStarterCatalog() },
                                onUpdateOrderStatus = { id, status ->
                                    staffViewModel.updateOrderStatus(id, status)
                                },
                                onSaveCategory = { cat, done -> staffViewModel.saveCategory(cat, done) },
                                onDeleteCategory = { staffViewModel.deleteCategory(it) },
                                onSaveFoodItem = { item, done -> staffViewModel.saveFoodItem(item, done) },
                                onDeleteFoodItem = { staffViewModel.deleteFoodItem(it) },
                                onSaveCoupon = { coupon, done -> staffViewModel.saveCoupon(coupon, done) },
                                onDeleteCoupon = { staffViewModel.deleteCoupon(it) },
                                onSaveDeliveryZone = { zone, done -> staffViewModel.saveDeliveryZone(zone, done) },
                                onDeleteDeliveryZone = { staffViewModel.deleteDeliveryZone(it) },
                                onSaveSettings = { staffViewModel.saveRestaurantSettings(it) },
                                onSaveManager = { mgr, isEdit, done -> staffViewModel.saveManager(mgr, isEdit, done) },
                                onDeleteManager = { staffViewModel.deleteManager(it) },
                                onResolveTicket = { id, st -> staffViewModel.resolveSupportTicket(id, st) }
                            )
                        }
                    }
                }
            }
        }
    }
}
