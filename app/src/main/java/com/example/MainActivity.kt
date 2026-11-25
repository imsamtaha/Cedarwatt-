package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CedarWattViewModel
import com.example.ui.components.AiBillScanner
import com.example.ui.components.BillDetailDialog
import com.example.ui.components.ComplaintDetailDialog
import com.example.ui.components.ComplaintsView
import com.example.ui.components.DashboardView
import com.example.ui.components.EnergyCopilot
import com.example.ui.components.ProfileSettingsDialog
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.localization.LocalAppStrings
import com.example.ui.localization.LocaleStrings
import com.example.ui.theme.CedarOrangePrimary
import com.example.ui.theme.CedarWattTheme
import com.example.ui.theme.LuxurySurfaceDark
import com.example.ui.theme.LuxurySurfaceElevated
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CedarWattTheme {
                CedarWattApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CedarWattApp(
    viewModel: CedarWattViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val monthlyTrends by viewModel.monthlyTrends.collectAsStateWithLifecycle()
    val scannerState by viewModel.scannerState.collectAsStateWithLifecycle()
    val timelineSlots by viewModel.timeline.collectAsStateWithLifecycle()
    val savingsTips by viewModel.savingsTips.collectAsStateWithLifecycle()
    val billDetail by viewModel.selectedBillDetail.collectAsStateWithLifecycle()
    val complaintDetail by viewModel.selectedComplaintDetail.collectAsStateWithLifecycle()
    val billForComplaint by viewModel.selectedBillForComplaint.collectAsStateWithLifecycle()
    val complaintDrafts by viewModel.complaintDrafts.collectAsStateWithLifecycle()
    val selectedDraftForEditing by viewModel.selectedDraftForEditing.collectAsStateWithLifecycle()
    val showComplaintForm by viewModel.showComplaintForm.collectAsStateWithLifecycle()
    val billToExplainInChat by viewModel.billToExplainInChat.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val strings = remember(currentLanguage) { LocaleStrings.get(currentLanguage) }

    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val supabaseUrl by viewModel.supabaseUrl.collectAsStateWithLifecycle()
    val supabaseAnonKey by viewModel.supabaseAnonKey.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }


    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalAppLanguage provides currentLanguage,
        LocalLayoutDirection provides currentLanguage.layoutDirection
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBackground),
            containerColor = ObsidianBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(CedarOrangePrimary, SunsetCoral)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strings.appName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = strings.appSubtitle,
                                    fontSize = 11.sp,
                                    color = CedarOrangePrimary
                                )
                            }
                        }
                    },
                    actions = {
                        // Supabase Auth Account Button
                        Surface(
                            onClick = { showAuthDialog = true },
                            color = LuxurySurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (activeUser != null && !activeUser!!.isGuest) StatusGreen.copy(alpha = 0.8f) else CedarOrangePrimary.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("btn_top_auth")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (activeUser != null && !activeUser!!.isGuest) StatusGreen else CedarOrangePrimary
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (activeUser != null && !activeUser!!.isGuest) Icons.Default.CheckCircle else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (activeUser != null && !activeUser!!.isGuest) {
                                        activeUser!!.fullName.split(" ").firstOrNull() ?: "User"
                                    } else {
                                        "Auth"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }
                        }

                        // Quick Language Switcher Button
                        Surface(
                            onClick = { viewModel.toggleLanguage() },
                            color = LuxurySurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CedarOrangePrimary.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("btn_top_language_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Language",
                                    tint = CedarOrangePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = strings.languageToggleLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("btn_top_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = strings.settingsTitle,
                                tint = TextSecondaryDark
                            )
                        }
                    },

                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = LuxurySurfaceDark,
                        titleContentColor = TextPrimaryDark
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_bottom_nav"),
                    containerColor = LuxurySurfaceDark,
                    tonalElevation = 8.dp
                ) {
                    // Tab 0: Dashboard
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = strings.navDashboard
                            )
                        },
                        label = { Text(strings.navDashboard, fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        colors = navigationItemColors(),
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )

                    // Tab 1: AI Scanner
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Default.DocumentScanner else Icons.Outlined.DocumentScanner,
                                contentDescription = strings.navScanBill
                            )
                        },
                        label = { Text(strings.navScanBill, fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        colors = navigationItemColors(),
                        modifier = Modifier.testTag("nav_tab_scanner")
                    )

                    // Tab 2: Copilot
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { viewModel.selectTab(2) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Default.Timeline else Icons.Outlined.Timeline,
                                contentDescription = strings.navCopilot
                            )
                        },
                        label = { Text(strings.navCopilot, fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        colors = navigationItemColors(),
                        modifier = Modifier.testTag("nav_tab_copilot")
                    )

                    // Tab 3: Complaints
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { viewModel.selectTab(3) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 3) Icons.Default.Gavel else Icons.Outlined.Gavel,
                                contentDescription = strings.navComplaints
                            )
                        },
                        label = { Text(strings.navComplaints, fontSize = 11.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                        colors = navigationItemColors(),
                        modifier = Modifier.testTag("nav_tab_complaints")
                    )

                    // Tab 4: Gemini AI Chat
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { viewModel.selectTab(4) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 4) Icons.Default.SmartToy else Icons.Outlined.SmartToy,
                                contentDescription = strings.navChat
                            )
                        },
                        label = { Text(strings.navChat, fontSize = 11.sp, fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) },
                        colors = navigationItemColors(),
                        modifier = Modifier.testTag("nav_tab_chat")
                    )
                }
            }
        ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .background(ObsidianBackground)
        ) {
            Crossfade(targetState = selectedTab, label = "tab_crossfade") { tab ->
                when (tab) {
                    0 -> DashboardView(
                        bills = bills,
                        userProfile = userProfile,
                        benchmarkTariff = viewModel.currentBenchmark,
                        monthlyTrends = monthlyTrends,
                        activeUser = activeUser,
                        onOpenAuth = { showAuthDialog = true },
                        onNavigateToScanner = { viewModel.selectTab(1) },
                        onNavigateToCopilot = { viewModel.selectTab(2) },
                        onNavigateToComplaints = { viewModel.selectTab(3) },
                        onSelectBill = { viewModel.openBillDetail(it) },
                        onOpenSettings = { showSettingsDialog = true },
                        onFileComplaint = { viewModel.startComplaintForBill(it) }
                    )

                    1 -> AiBillScanner(
                        scannerState = scannerState,
                        officialBenchmarkRateUsd = viewModel.currentBenchmark.officialGeneratorKwhUsd,
                        onFieldUpdate = { prov, mth, kwh, amps, rate, fix, fuel ->
                            viewModel.updateScannerField(prov, mth, kwh, amps, rate, fix, fuel)
                        },
                        onLoadPreset = { viewModel.loadPresetBill(it) },
                        onTriggerScan = { viewModel.scanBillWithCloudOcr(null) },
                        onSelectOcrProvider = { viewModel.selectOcrProvider(it) },
                        onTriggerCloudOcr = { uri -> viewModel.scanBillWithCloudOcr(uri) },
                        onResetScan = { viewModel.resetScanner() },
                        onSaveBill = { viewModel.saveCurrentScannedBill() },
                        onFileComplaint = {
                            val state = scannerState
                            val kwh = state.kwhConsumption.toDoubleOrNull() ?: 0.0
                            val rate = state.ratePerKwhUsd.toDoubleOrNull() ?: 0.0
                            val fixed = state.fixedFeeUsd.toDoubleOrNull() ?: 0.0
                            val fuel = state.fuelSurchargeUsd.toDoubleOrNull() ?: 0.0
                            val bill = com.example.data.local.ScannedBill(
                                providerName = state.providerName,
                                billMonth = state.billMonth,
                                kwhConsumption = kwh,
                                amperage = state.amperage,
                                ratePerKwhUsd = rate,
                                ratePerKwhLbp = rate * 89500,
                                officialBenchmarkRateUsd = viewModel.currentBenchmark.officialGeneratorKwhUsd,
                                fixedFeeUsd = fixed,
                                fuelSurchargeUsd = fuel,
                                totalAmountUsd = (kwh * rate) + fixed + fuel,
                                totalAmountLbp = ((kwh * rate) + fixed + fuel) * 89500,
                                overchargeAmountUsd = state.overchargeAmountUsd,
                                fairnessStatus = state.fairnessStatus,
                                aiAuditSummary = state.auditSummary
                            )
                            viewModel.startComplaintForBill(bill)
                        }
                    )
                    2 -> EnergyCopilot(
                        timelineSlots = timelineSlots,
                        savingTips = savingsTips,
                        userProfile = userProfile
                    )
                    3 -> ComplaintsView(
                        complaints = complaints,
                        bills = bills,
                        drafts = complaintDrafts,
                        onResumeDraft = { viewModel.resumeDraft(it) },
                        onDeleteDraft = { viewModel.deleteComplaintDraft(it) },
                        onSelectComplaint = { viewModel.openComplaintDetail(it) },
                        onStartNewComplaint = { viewModel.startComplaintForBill(it) },
                        onOpenComplaintForm = { viewModel.openComplaintForm() },
                        onNavigateToScanner = { viewModel.selectTab(1) }
                    )
                    4 -> {
                        val chatViewModel: com.example.ui.chat.GeminiChatViewModel = viewModel()
                        val messages by chatViewModel.filteredMessages.collectAsStateWithLifecycle()
                        val searchQuery by chatViewModel.searchQuery.collectAsStateWithLifecycle()
                        val selectedRole by chatViewModel.selectedRole.collectAsStateWithLifecycle()
                        val isGenerating by chatViewModel.isGenerating.collectAsStateWithLifecycle()
                        val inputText by chatViewModel.inputText.collectAsStateWithLifecycle()
                        val selectedBillInChat by chatViewModel.selectedBill.collectAsStateWithLifecycle()
                        val isArabic = currentLanguage == AppLanguage.ARABIC

                        // Automatically trigger bill explanation if navigated from Bill Detail
                        LaunchedEffect(billToExplainInChat) {
                            billToExplainInChat?.let { bill ->
                                chatViewModel.explainBill(bill, isArabic = isArabic)
                                viewModel.clearBillToExplain()
                            }
                        }

                        LaunchedEffect(bills, userProfile, selectedBillInChat) {
                            val benchmark = viewModel.currentBenchmark
                            chatViewModel.setLiveContextProvider {
                                buildString {
                                    appendLine("User Location: ${userProfile.location}")
                                    appendLine("Subscription Amperage: ${userProfile.subscribedAmps}A (Max continuous load: ${(userProfile.subscribedAmps * 220 * 0.8).toInt()}W)")
                                    appendLine("Solar PV Capacity: ${userProfile.solarCapacityKwp} kWp | Battery Storage: ${userProfile.batteryCapacityKwh} kWh")
                                    appendLine("Official Ministry Benchmark Tariff (${benchmark.monthName}):")
                                    appendLine("  - Official Tariff Ceiling: $${String.format(java.util.Locale.US, "%.3f", benchmark.officialGeneratorKwhUsd)}/kWh (${String.format(java.util.Locale.US, "%,.0f", benchmark.officialGeneratorKwhLbp)} LBP)")
                                    appendLine("  - Fixed Amperage Allowances: 5A = $${benchmark.fixed5AmpFeeUsd}, 10A = $${benchmark.fixed10AmpFeeUsd}, 15A = $${benchmark.fixed15AmpFeeUsd}")
                                    appendLine("  - Official 20L Diesel Price: $${benchmark.dieselPrice20LUsd}")
                                    val activeBill = selectedBillInChat ?: bills.firstOrNull()
                                    if (activeBill != null) {
                                        appendLine("Active Audited Bill (${activeBill.billMonth}):")
                                        appendLine("  - Provider: ${activeBill.providerName}")
                                        appendLine("  - Consumption: ${activeBill.kwhConsumption} kWh (${activeBill.amperage}A)")
                                        appendLine("  - Billed Unit Rate: $${String.format(java.util.Locale.US, "%.3f", activeBill.ratePerKwhUsd)}/kWh (Overcharge: $${String.format(java.util.Locale.US, "%.2f", activeBill.overchargeAmountUsd)})")
                                        appendLine("  - Fixed Fee: $${String.format(java.util.Locale.US, "%.2f", activeBill.fixedFeeUsd)} | Fuel Surcharge: $${String.format(java.util.Locale.US, "%.2f", activeBill.fuelSurchargeUsd)}")
                                        appendLine("  - Total Amount: $${String.format(java.util.Locale.US, "%.2f", activeBill.totalAmountUsd)} (${String.format(java.util.Locale.US, "%,.0f", activeBill.totalAmountLbp)} LBP)")
                                        appendLine("  - Status: ${activeBill.fairnessStatus.name}")
                                    }
                                }
                            }
                        }

                        com.example.ui.chat.GeminiChatView(
                            messages = messages,
                            selectedRole = selectedRole,
                            isGenerating = isGenerating,
                            inputText = inputText,
                            onRoleSelected = { chatViewModel.selectRole(it) },
                            onInputChanged = { chatViewModel.updateInput(it) },
                            onSendMessage = { chatViewModel.sendMessage(it) },
                            onClearChat = { chatViewModel.clearChat() },
                            onRetry = { chatViewModel.retryLastMessage() },
                            bills = bills,
                            selectedBill = selectedBillInChat,
                            onSelectBill = { chatViewModel.selectBill(it) },
                            onExplainBill = { chatViewModel.explainBill(it, isArabic = isArabic) },
                            onAskCostReduction = { chatViewModel.askCostReductionAdvice(it, isArabic = isArabic) },
                            searchQuery = searchQuery,
                            onSearchQueryChanged = { chatViewModel.setSearchQuery(it) }
                        )
                    }
                }
            }
        }

        // Bill Detail Modal Dialog
        billDetail?.let { bill ->
            BillDetailDialog(
                bill = bill,
                onDismiss = { viewModel.closeBillDetail() },
                onFileComplaint = { viewModel.startComplaintForBill(it) },
                onDelete = { viewModel.deleteBill(it) },
                onAskCopilot = { viewModel.askCopilotToExplainBill(it) }
            )
        }

        // Municipality Complaint Form Dialog
        if (showComplaintForm || billForComplaint != null || selectedDraftForEditing != null) {
            com.example.ui.components.MunicipalityComplaintFormDialog(
                bills = bills,
                initialSelectedBill = billForComplaint,
                userProfile = userProfile,
                draft = selectedDraftForEditing,
                onSaveDraft = { bill, mun, dept, viol, desc, claim, tone, meter, rem, ev, text, isAr, draftId ->
                    viewModel.saveComplaintDraft(
                        bill = bill,
                        municipality = mun,
                        department = dept,
                        violationType = viol,
                        discrepancyDescription = desc,
                        claimAmountUsd = claim,
                        selectedTone = tone,
                        meterNumber = meter,
                        requestedRemedies = rem,
                        attachedEvidence = ev,
                        draftLetterText = text,
                        isArabic = isAr,
                        existingDraftId = draftId
                    )
                },
                onDismiss = { viewModel.closeComplaintForm() },
                onGenerateDocument = { bill, mun, violation, desc, claim, isAr ->
                    viewModel.generateComplaintDocument(bill, mun, violation, desc, claim, isAr)
                },
                onSubmitComplaint = { targetBill, mun, violation, desc, claim, docText ->
                    viewModel.submitComplaintForBill(targetBill, mun, violation, desc, claim, docText)
                }
            )
        }

        // Complaint Detail Modal Dialog
        complaintDetail?.let { complaint ->
            ComplaintDetailDialog(
                complaint = complaint,
                onDismiss = { viewModel.closeComplaintDetail() },
                onAdvanceStatus = { c, st -> viewModel.updateComplaintStatus(c, st) },
                onDelete = { viewModel.deleteComplaint(it) }
            )
        }

        // Profile Settings Modal Dialog
        if (showSettingsDialog) {
            ProfileSettingsDialog(
                userProfile = userProfile,
                onDismiss = { showSettingsDialog = false },
                currentLanguage = currentLanguage,
                onLanguageChange = { viewModel.setLanguage(it) },
                onSave = { name, loc, mun, amps, solar, sKwp, bat, bKwh ->
                    viewModel.saveProfile(name, loc, mun, amps, solar, sKwp, bat, bKwh)
                }
            )
        }

        // Supabase Authentication Modal Dialog
        if (showAuthDialog) {
            com.example.ui.components.AuthDialog(
                viewModel = viewModel,
                authState = authState,
                activeUser = activeUser,
                supabaseUrl = supabaseUrl,
                supabaseAnonKey = supabaseAnonKey,
                onDismiss = { showAuthDialog = false }
            )
        }
    }
}

}

@Composable
fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Color.Black,
    selectedTextColor = CedarOrangePrimary,
    indicatorColor = CedarOrangePrimary,
    unselectedIconColor = TextSecondaryDark,
    unselectedTextColor = TextSecondaryDark
)
