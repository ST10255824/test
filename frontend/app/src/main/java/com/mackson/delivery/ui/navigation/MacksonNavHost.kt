package com.mackson.delivery.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mackson.delivery.data.model.UserRole
import com.mackson.delivery.data.repository.CartStore
import com.mackson.delivery.ui.auth.AuthViewModel
import com.mackson.delivery.ui.auth.LoginScreen
import com.mackson.delivery.ui.auth.OtpScreen
import com.mackson.delivery.ui.auth.OtpUiState
import com.mackson.delivery.ui.auth.OtpViewModel
import com.mackson.delivery.ui.auth.RegisterScreen
import com.mackson.delivery.ui.customer.BrowseScreen
import com.mackson.delivery.ui.customer.CartScreen
import com.mackson.delivery.ui.customer.ChatScreen
import com.mackson.delivery.ui.customer.CheckoutScreen
import com.mackson.delivery.ui.customer.HistoryScreen
import com.mackson.delivery.ui.customer.HomeScreen
import com.mackson.delivery.ui.customer.ProductDetailScreen
import com.mackson.delivery.ui.customer.ProfileScreen
import com.mackson.delivery.ui.customer.SearchScreen
import com.mackson.delivery.ui.customer.TrackingScreen
import com.mackson.delivery.ui.driver.DriverActiveRunScreen
import com.mackson.delivery.ui.driver.DriverJobsScreen
import com.mackson.delivery.ui.driver.DriverProofScreen
import com.mackson.delivery.ui.picker.PickerManifestScreen
import com.mackson.delivery.ui.picker.PickerOrderDetailScreen
import com.mackson.delivery.ui.picker.PickerScanRoute
import com.mackson.delivery.ui.picker.ScanMode
import com.mackson.delivery.util.viewModelFactory

/**
 * Single flat NavHost carrying all four role graphs (Part 1 section 9.1.7: "consider doing this
 * [the UX journey map] for each application ... and remember to include different user roles").
 * A bottom navigation bar is shown only on each role's top-level tab destinations; detail flows
 * (checkout, tracking, scanning) render full-screen on top of it.
 */
@androidx.camera.core.ExperimentalGetImage
@Composable
fun MacksonNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory { AuthViewModel() })
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Compose Navigation only reads startDestination once, so a login that happens after first
    // composition (the common case) needs an explicit hop to the role hub here.
    LaunchedEffect(authState.signedInUserId, currentRoute) {
        val onAuthScreen = currentRoute == Routes.LOGIN || currentRoute == Routes.REGISTER
        if (authState.signedInUserId != null && onAuthScreen) {
            navController.navigate("role-hub") { popUpTo(Routes.LOGIN) { inclusive = true } }
        }
    }

    // The Part 1 Figma prototype keeps its bottom nav bar visible on Home, Search results, and
    // Cart (not just the true top-level tabs) — only the deeper single-purpose flow screens
    // (product detail, checkout, tracking, chat) hide it, closer to a back-arrow-driven flow.
    val bottomTabs = when {
        currentRoute == null -> null
        currentRoute.startsWith("customer/") && currentRoute !in listOf(
            Routes.CUSTOMER_PRODUCT, Routes.CUSTOMER_CHECKOUT, Routes.CUSTOMER_TRACKING, Routes.CUSTOMER_CHAT
        ) -> customerTabs
        currentRoute == Routes.PICKER_MANIFEST -> pickerTabs
        currentRoute == Routes.DRIVER_JOBS -> driverTabs
        else -> null
    }

    Scaffold(
        bottomBar = {
            bottomTabs?.let { RoleBottomBar(navController, currentRoute, it) }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (authState.signedInUserId != null) "role-hub" else Routes.LOGIN,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            // ---- Auth ---------------------------------------------------------------
            composable(Routes.LOGIN) {
                LoginScreen(
                    uiState = authState,
                    onLogin = authViewModel::login,
                    onGoToRegister = { navController.navigate(Routes.REGISTER) },
                    onGoToOtpLogin = { navController.navigate(Routes.otp("")) },
                    onDismissError = authViewModel::clearError
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(uiState = authState, onRegister = authViewModel::register)
            }
            composable(
                Routes.OTP,
                arguments = listOf(navArgument("phone") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                val otpViewModel: OtpViewModel = viewModel(factory = viewModelFactory { OtpViewModel() })
                val otpState by otpViewModel.uiState.collectAsStateWithLifecycle()
                if (otpState.phase == OtpUiState.Phase.VERIFIED) {
                    navController.navigate("role-hub") { popUpTo(Routes.LOGIN) { inclusive = true } }
                } else {
                    OtpScreen(
                        initialPhone = entry.arguments?.getString("phone").orEmpty(),
                        uiState = otpState,
                        onRequestOtp = otpViewModel::requestOtp,
                        onConfirmCode = otpViewModel::confirmCode
                    )
                }
            }

            composable("role-hub") {
                RoleHubScreen { role ->
                    val destination = when (role) {
                        UserRole.CUSTOMER -> Routes.CUSTOMER_HOME
                        UserRole.PICKER -> Routes.PICKER_MANIFEST
                        UserRole.DRIVER -> Routes.DRIVER_JOBS
                        // Store Manager / Admin has no mobile UI — see admin-web/.
                        UserRole.MANAGER, UserRole.ADMIN -> Routes.CUSTOMER_HOME
                    }
                    navController.navigate(destination)
                }
            }

            // ---- Customer -------------------------------------------------------------
            composable(Routes.CUSTOMER_HOME) {
                HomeScreen { store ->
                    CartStore.setStore(store.storeId)
                    navController.navigate(Routes.customerBrowse(store.storeId))
                }
            }
            composable(
                Routes.CUSTOMER_BROWSE,
                arguments = listOf(navArgument("storeId") { type = NavType.StringType })
            ) { entry ->
                val storeId = entry.arguments?.getString("storeId").orEmpty()
                val cart by CartStore.cart.collectAsStateWithLifecycle()
                BrowseScreen(
                    storeId = storeId,
                    cartItemCount = cart.itemCount,
                    onSearch = { navController.navigate(Routes.customerSearch(storeId)) },
                    onCartClick = { navController.navigate(Routes.CUSTOMER_CART) },
                    onProductClick = { product -> navController.navigate(Routes.customerProduct(storeId, product.productId)) }
                )
            }
            composable(
                Routes.CUSTOMER_SEARCH,
                arguments = listOf(navArgument("storeId") { type = NavType.StringType })
            ) { entry ->
                val storeId = entry.arguments?.getString("storeId").orEmpty()
                SearchScreen(storeId) { product -> navController.navigate(Routes.customerProduct(storeId, product.productId)) }
            }
            composable(
                Routes.CUSTOMER_PRODUCT,
                arguments = listOf(
                    navArgument("storeId") { type = NavType.StringType },
                    navArgument("productId") { type = NavType.StringType }
                )
            ) { entry ->
                val storeId = entry.arguments?.getString("storeId").orEmpty()
                val productId = entry.arguments?.getString("productId").orEmpty()
                ProductDetailScreen(storeId, productId) { navController.navigate(Routes.CUSTOMER_CART) }
            }
            composable(Routes.CUSTOMER_CART) {
                CartScreen(
                    onCheckout = { navController.navigate(Routes.CUSTOMER_CHECKOUT) },
                    onContinueShopping = { navController.popBackStack() }
                )
            }
            composable(Routes.CUSTOMER_CHECKOUT) {
                val cart by CartStore.cart.collectAsStateWithLifecycle()
                CheckoutScreen(storeId = cart.storeId) { orderId ->
                    navController.navigate(Routes.customerTracking(orderId)) {
                        popUpTo(Routes.CUSTOMER_HOME)
                    }
                }
            }
            composable(
                Routes.CUSTOMER_TRACKING,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { entry ->
                val orderId = entry.arguments?.getString("orderId").orEmpty()
                TrackingScreen(orderId) { navController.navigate(Routes.customerChat(orderId)) }
            }
            composable(
                Routes.CUSTOMER_CHAT,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { entry ->
                ChatScreen(entry.arguments?.getString("orderId").orEmpty())
            }
            composable(Routes.CUSTOMER_HISTORY) {
                HistoryScreen { order -> navController.navigate(Routes.customerTracking(order.orderId)) }
            }
            composable(Routes.CUSTOMER_PROFILE) {
                ProfileScreen {
                    navController.navigate(Routes.LOGIN) { popUpTo(0) }
                }
            }

            // ---- Picker -----------------------------------------------------------------
            composable(Routes.PICKER_MANIFEST) {
                PickerManifestScreen { order -> navController.navigate(Routes.pickerOrderDetail(order.orderId)) }
            }
            composable(
                Routes.PICKER_ORDER_DETAIL,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { entry ->
                val orderId = entry.arguments?.getString("orderId").orEmpty()
                PickerOrderDetailScreen(
                    orderId = orderId,
                    onScanToPick = { item -> navController.navigate(Routes.pickerScanForPicking(orderId, item.productId)) },
                    onFlagSubstitution = { item -> navController.navigate(Routes.pickerScanForSubstitution(orderId, item.productId)) },
                    onStaged = { navController.popBackStack() }
                )
            }
            composable(
                Routes.PICKER_SCAN,
                arguments = listOf(
                    navArgument("orderId") { type = NavType.StringType },
                    navArgument("mode") { type = NavType.StringType },
                    navArgument("targetProductId") { type = NavType.StringType }
                )
            ) { entry ->
                val orderId = entry.arguments?.getString("orderId").orEmpty()
                val mode = ScanMode.valueOf(entry.arguments?.getString("mode") ?: "PICK")
                val targetProductId = entry.arguments?.getString("targetProductId").orEmpty()
                PickerScanRoute(orderId, mode, targetProductId) { navController.popBackStack() }
            }

            // ---- Driver -----------------------------------------------------------------
            composable(Routes.DRIVER_JOBS) {
                DriverJobsScreen { orderId -> navController.navigate(Routes.driverActiveRun(orderId)) }
            }
            composable(
                Routes.DRIVER_ACTIVE_RUN,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { entry ->
                val orderId = entry.arguments?.getString("orderId").orEmpty()
                DriverActiveRunScreen(orderId) { navController.navigate(Routes.driverProof(orderId)) }
            }
            composable(
                Routes.DRIVER_PROOF,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { entry ->
                DriverProofScreen(entry.arguments?.getString("orderId").orEmpty()) {
                    navController.navigate(Routes.DRIVER_JOBS) { popUpTo(Routes.DRIVER_JOBS) { inclusive = true } }
                }
            }
        }
    }
}
