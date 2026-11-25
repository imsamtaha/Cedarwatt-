package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.BenchmarkTariff
import com.example.data.local.BillFairnessStatus
import com.example.data.local.ComplaintDraft
import com.example.data.local.ComplaintStatus
import com.example.data.local.EnergySavingTip
import com.example.data.local.EnergySource
import com.example.data.local.EnergyTimelineSlot
import com.example.data.local.EnergyUsageRecord
import com.example.data.local.MonthlyEnergyTrendPoint
import com.example.data.local.MunicipalityComplaint
import com.example.data.local.ScannedBill
import com.example.data.local.UserProfile
import com.example.data.repository.CedarWattRepository
import com.example.data.remote.supabase.AuthResult
import com.example.data.remote.supabase.AuthState
import com.example.data.remote.supabase.AuthUser
import com.example.data.remote.supabase.SupabaseAuthManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import android.net.Uri
import com.example.data.remote.ocr.CloudOcrProvider
import com.example.data.remote.ocr.CloudOcrResult
import com.example.data.remote.ocr.CloudOcrService
import com.example.data.remote.ocr.ParsedBillData
import kotlinx.coroutines.launch
import com.example.ui.localization.AppLanguage
import java.util.Locale
import java.util.UUID

data class ScannerUiState(
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val activeStep: Int = 0, // 0: Camera / Upload, 1: Analyzing OCR, 2: Validated Result
    val providerName: String = "Achrafieh Power Co.",
    val billMonth: String = "August 2026",
    val kwhConsumption: String = "285",
    val amperage: Int = 10,
    val ratePerKwhUsd: String = "0.52",
    val fixedFeeUsd: String = "18.00",
    val fuelSurchargeUsd: String = "12.50",
    val fairnessStatus: BillFairnessStatus = BillFairnessStatus.RED,
    val overchargeAmountUsd: Double = 45.60,
    val totalAmountUsd: Double = 178.70,
    val auditSummary: String = "CRITICAL OVERCHARGE: Billed rate is 44% above Ministry ceiling ($0.360/kWh). Eligible for municipal complaint.",
    val generatedLetter: String = "",
    val isSaved: Boolean = false,
    val meterNumber: String = "KW-8842-BEIRUT",
    val invoiceNumber: String = "INV-2026-0881",
    val dueDate: String = "2026-09-10",
    val scannedImageUri: String? = null,
    val ocrRawText: String = "ACHRAFIEH ELECTRIC POWER CO.\nAugust 2026\n285 kWh @ $0.520/kWh\nFixed: $18.00 • Fuel Surcharge: $12.50\nTotal: $178.70",
    val confidenceScore: Double = 0.98,
    val generatorSubstation: String = "Achrafieh Sector 4 Substation B",
    val selectedOcrProvider: CloudOcrProvider = CloudOcrProvider.GOOGLE_CLOUD_VISION,
    val ocrProcessingStage: String = "Ready",
    val ocrErrorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CedarWattViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CedarWattRepository
    val supabaseAuthManager = SupabaseAuthManager(application, viewModelScope)

    val authState: StateFlow<AuthState> = supabaseAuthManager.authState
    val activeUser: StateFlow<AuthUser?> = supabaseAuthManager.activeUser
    val supabaseUrl: StateFlow<String> = supabaseAuthManager.supabaseUrl
    val supabaseAnonKey: StateFlow<String> = supabaseAuthManager.supabaseAnonKey

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = CedarWattRepository(database)

        viewModelScope.launch {
            activeUser.collect { user ->
                if (user != null && !user.isGuest) {
                    repository.linkGuestDataToAccount(user.id)
                }
            }
        }
    }

    val bills: StateFlow<List<ScannedBill>> = activeUser.flatMapLatest { user ->
        val uid = user?.id ?: "guest_default"
        repository.getBillsForUser(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val complaints: StateFlow<List<MunicipalityComplaint>> = activeUser.flatMapLatest { user ->
        val uid = user?.id ?: "guest_default"
        repository.getComplaintsForUser(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val complaintDrafts: StateFlow<List<ComplaintDraft>> = activeUser.flatMapLatest { user ->
        val uid = user?.id ?: "guest_default"
        repository.getDraftsForUser(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = activeUser.flatMapLatest { user ->
        val uid = user?.id ?: "guest_default"
        repository.getUserProfile(uid).map { profile ->
            profile ?: UserProfile(
                userId = uid,
                fullName = user?.fullName ?: "Sami Taha",
                location = user?.location ?: "Achrafieh, Beirut",
                municipalityName = user?.municipality ?: "Municipality of Beirut"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val currentBenchmark: BenchmarkTariff = repository.currentBenchmarkTariff

    val tariffBenchmarks: StateFlow<List<BenchmarkTariff>> = repository.allBenchmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usageHistory: StateFlow<List<EnergyUsageRecord>> = activeUser.flatMapLatest { user ->
        val uid = user?.id ?: "guest_default"
        repository.getUsageHistoryForUser(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val monthlyTrends: StateFlow<List<MonthlyEnergyTrendPoint>> = combine(
        bills,
        userProfile
    ) { billList, profile ->
        repository.getMonthlyTrendData(billList, currentBenchmark, profile)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        repository.getMonthlyTrendData(emptyList(), currentBenchmark, null)
    )

    private val _appLanguage = MutableStateFlow(
        if (Locale.getDefault().language == "ar") AppLanguage.ARABIC else AppLanguage.ARABIC
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Dashboard, 1: Scanner, 2: Copilot, 3: Complaints
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _scannerState = MutableStateFlow(ScannerUiState())
    val scannerState: StateFlow<ScannerUiState> = _scannerState.asStateFlow()

    private val _timeline = MutableStateFlow<List<EnergyTimelineSlot>>(emptyList())
    val timeline: StateFlow<List<EnergyTimelineSlot>> = _timeline.asStateFlow()

    private val _savingsTips = MutableStateFlow<List<EnergySavingTip>>(emptyList())
    val savingsTips: StateFlow<List<EnergySavingTip>> = _savingsTips.asStateFlow()

    private val _selectedBillForComplaint = MutableStateFlow<ScannedBill?>(null)
    val selectedBillForComplaint: StateFlow<ScannedBill?> = _selectedBillForComplaint.asStateFlow()

    private val _selectedDraftForEditing = MutableStateFlow<ComplaintDraft?>(null)
    val selectedDraftForEditing: StateFlow<ComplaintDraft?> = _selectedDraftForEditing.asStateFlow()

    private val _showComplaintForm = MutableStateFlow(false)
    val showComplaintForm: StateFlow<Boolean> = _showComplaintForm.asStateFlow()

    private val _selectedBillDetail = MutableStateFlow<ScannedBill?>(null)
    val selectedBillDetail: StateFlow<ScannedBill?> = _selectedBillDetail.asStateFlow()

    private val _selectedComplaintDetail = MutableStateFlow<MunicipalityComplaint?>(null)
    val selectedComplaintDetail: StateFlow<MunicipalityComplaint?> = _selectedComplaintDetail.asStateFlow()

    private val _billToExplainInChat = MutableStateFlow<ScannedBill?>(null)
    val billToExplainInChat: StateFlow<ScannedBill?> = _billToExplainInChat.asStateFlow()

    init {
        refreshCopilotData()
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun askCopilotToExplainBill(bill: ScannedBill) {
        _selectedBillDetail.value = null
        _billToExplainInChat.value = bill
        _selectedTab.value = 4 // Navigate to Gemini AI Chat
    }

    fun clearBillToExplain() {
        _billToExplainInChat.value = null
    }

    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
    }

    fun toggleLanguage() {
        _appLanguage.value = if (_appLanguage.value == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
    }

    fun openBillDetail(bill: ScannedBill) {
        _selectedBillDetail.value = bill
    }

    fun closeBillDetail() {
        _selectedBillDetail.value = null
    }

    fun openComplaintDetail(complaint: MunicipalityComplaint) {
        _selectedComplaintDetail.value = complaint
    }

    fun closeComplaintDetail() {
        _selectedComplaintDetail.value = null
    }

    fun startComplaintForBill(bill: ScannedBill) {
        _selectedBillForComplaint.value = bill
        _selectedDraftForEditing.value = null
        _showComplaintForm.value = true
    }

    fun openComplaintForm(bill: ScannedBill? = null) {
        _selectedBillForComplaint.value = bill
        _selectedDraftForEditing.value = null
        _showComplaintForm.value = true
    }

    fun resumeDraft(draft: ComplaintDraft) {
        viewModelScope.launch {
            val associatedBill = draft.billId?.let { repository.getBillById(it) }
            _selectedBillForComplaint.value = associatedBill
            _selectedDraftForEditing.value = draft
            _showComplaintForm.value = true
        }
    }

    fun closeComplaintForm() {
        _showComplaintForm.value = false
        _selectedBillForComplaint.value = null
        _selectedDraftForEditing.value = null
    }

    fun closeComplaintDialog() {
        _showComplaintForm.value = false
        _selectedBillForComplaint.value = null
        _selectedDraftForEditing.value = null
    }

    fun saveComplaintDraft(
        bill: ScannedBill?,
        municipality: String,
        department: String,
        violationType: String,
        discrepancyDescription: String,
        claimAmountUsd: Double,
        selectedTone: String,
        meterNumber: String,
        requestedRemedies: String,
        attachedEvidence: String,
        draftLetterText: String,
        isArabic: Boolean,
        existingDraftId: Long = 0L
    ) {
        viewModelScope.launch {
            val currentUserId = activeUser.value?.id ?: "guest_default"
            val profile = userProfile.value
            val draft = ComplaintDraft(
                id = existingDraftId,
                userId = currentUserId,
                billId = bill?.id,
                billProviderName = bill?.providerName ?: "",
                billMonth = bill?.billMonth ?: "",
                billKwh = bill?.kwhConsumption ?: 0.0,
                billAmperage = bill?.amperage ?: 10,
                billChargedRateUsd = bill?.ratePerKwhUsd ?: 0.0,
                billOfficialBenchmarkUsd = bill?.officialBenchmarkRateUsd ?: currentBenchmark.officialGeneratorKwhUsd,
                municipalityName = municipality,
                department = department,
                complainantName = profile.fullName,
                complainantAddress = profile.location,
                complainantPhone = "+961 70 123 456",
                violationType = violationType,
                discrepancyDescription = discrepancyDescription,
                claimAmountUsd = claimAmountUsd,
                selectedTone = selectedTone,
                meterNumber = meterNumber,
                requestedRemedies = requestedRemedies,
                attachedEvidence = attachedEvidence,
                draftLetterText = draftLetterText,
                isArabic = isArabic,
                lastSavedTimestamp = System.currentTimeMillis()
            )
            repository.saveComplaintDraft(draft)
            _selectedDraftForEditing.value = null
            _showComplaintForm.value = false
        }
    }

    fun deleteComplaintDraft(draft: ComplaintDraft) {
        viewModelScope.launch {
            repository.deleteComplaintDraft(draft)
            if (_selectedDraftForEditing.value?.id == draft.id) {
                _selectedDraftForEditing.value = null
            }
        }
    }

    fun logQuickEnergyUsage(
        source: EnergySource,
        kwh: Double,
        costUsd: Double,
        peakDemandKw: Double = 0.0,
        generatorMinutes: Int = 0,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val currentUserId = activeUser.value?.id ?: "guest_default"
            repository.logQuickUsageEntry(
                source = source,
                kwh = kwh,
                costUsd = costUsd,
                peakDemandKw = peakDemandKw,
                generatorMinutes = generatorMinutes,
                notes = notes,
                userId = currentUserId
            )
        }
    }


    fun generateComplaintDocument(
        bill: ScannedBill,
        municipality: String,
        violationType: String,
        discrepancyDescription: String,
        claimAmountUsd: Double,
        isArabic: Boolean,
        complainantPhone: String = "",
        department: String = "Municipal Energy & Generator Oversight Bureau",
        tone: String = "Official Administrative Petition",
        requestedRemedies: List<String> = emptyList(),
        meterNumber: String = ""
    ): String {
        val profile = userProfile.value
        return repository.generateComplaintLetter(
            userName = profile.fullName,
            userLocation = profile.location,
            municipality = municipality,
            bill = bill,
            violationType = violationType,
            discrepancyDescription = discrepancyDescription,
            customClaimAmountUsd = claimAmountUsd,
            isArabic = isArabic,
            complainantPhone = complainantPhone,
            department = department,
            tone = tone,
            requestedRemedies = requestedRemedies,
            meterNumber = meterNumber
        )
    }

    fun refreshCopilotData() {
        val profile = userProfile.value
        _timeline.value = repository.get24HourTimeline(profile)
        _savingsTips.value = repository.getSmartSavingsTips(profile)
    }

    fun updateScannerField(
        provider: String? = null,
        month: String? = null,
        kwh: String? = null,
        amps: Int? = null,
        rateUsd: String? = null,
        fixedUsd: String? = null,
        fuelUsd: String? = null
    ) {
        _scannerState.update { current ->
            val newProvider = provider ?: current.providerName
            val newMonth = month ?: current.billMonth
            val newKwh = kwh ?: current.kwhConsumption
            val newAmps = amps ?: current.amperage
            val newRate = rateUsd ?: current.ratePerKwhUsd
            val newFixed = fixedUsd ?: current.fixedFeeUsd
            val newFuel = fuelUsd ?: current.fuelSurchargeUsd

            val kwhVal = newKwh.toDoubleOrNull() ?: 0.0
            val rateVal = newRate.toDoubleOrNull() ?: 0.0
            val fixedVal = newFixed.toDoubleOrNull() ?: 0.0
            val fuelVal = newFuel.toDoubleOrNull() ?: 0.0

            val (status, overcharge, summary) = repository.calculateBillAudit(
                kwhVal, newAmps, rateVal, fixedVal, fuelVal
            )
            val total = (kwhVal * rateVal) + fixedVal + fuelVal

            current.copy(
                providerName = newProvider,
                billMonth = newMonth,
                kwhConsumption = newKwh,
                amperage = newAmps,
                ratePerKwhUsd = newRate,
                fixedFeeUsd = newFixed,
                fuelSurchargeUsd = newFuel,
                fairnessStatus = status,
                overchargeAmountUsd = overcharge,
                totalAmountUsd = total,
                auditSummary = summary,
                isSaved = false
            )
        }
    }

    fun loadPresetBill(presetType: String) {
        when (presetType) {
            "RED" -> {
                updateScannerField(
                    provider = "Achrafieh Central Diesel Co.",
                    month = "August 2026",
                    kwh = "320",
                    amps = 10,
                    rateUsd = "0.54",
                    fixedUsd = "20.00",
                    fuelUsd = "15.00"
                )
            }
            "AMBER" -> {
                updateScannerField(
                    provider = "Metn Coastal Power Coop",
                    month = "August 2026",
                    kwh = "240",
                    amps = 10,
                    rateUsd = "0.40",
                    fixedUsd = "12.00",
                    fuelUsd = "6.00"
                )
            }
            "GREEN" -> {
                updateScannerField(
                    provider = "Beirut Green Energy Solar Microgrid",
                    month = "August 2026",
                    kwh = "210",
                    amps = 10,
                    rateUsd = "0.35",
                    fixedUsd = "10.00",
                    fuelUsd = "0.00"
                )
            }
        }
    }

    private val cloudOcrService = CloudOcrService()

    fun selectOcrProvider(provider: CloudOcrProvider) {
        _scannerState.update { it.copy(selectedOcrProvider = provider) }
    }

    fun scanBillWithCloudOcr(imageUri: Uri? = null, customApiKey: String? = null) {
        viewModelScope.launch {
            val provider = _scannerState.value.selectedOcrProvider
            _scannerState.update {
                it.copy(
                    isScanning = true,
                    activeStep = 1,
                    scanProgress = 0.15f,
                    scannedImageUri = imageUri?.toString(),
                    ocrProcessingStage = "Connecting to ${provider.displayName}...",
                    ocrErrorMessage = null
                )
            }
            delay(300)
            _scannerState.update {
                it.copy(
                    scanProgress = 0.40f,
                    ocrProcessingStage = "Extracting document text blocks & bounding geometry..."
                )
            }

            val result: CloudOcrResult = if (imageUri != null) {
                cloudOcrService.processBillImageFromUri(
                    context = getApplication(),
                    uri = imageUri,
                    provider = provider,
                    customApiKey = customApiKey
                )
            } else {
                cloudOcrService.processBillImage(
                    imageBytes = byteArrayOf(),
                    provider = provider,
                    customApiKey = customApiKey
                )
            }

            _scannerState.update {
                it.copy(
                    scanProgress = 0.75f,
                    ocrProcessingStage = "Auditing charges against Ministry of Energy statutory price ceiling..."
                )
            }
            delay(300)

            val parsed = result.parsedData
            val (status, overcharge, summary) = repository.calculateBillAudit(
                kwh = parsed.kwhConsumption,
                amps = parsed.amperage,
                chargedRateUsd = parsed.ratePerKwhUsd,
                fixedFeeUsd = parsed.fixedFeeUsd,
                fuelSurchargeUsd = parsed.fuelSurchargeUsd
            )

            val totalUsd = (parsed.kwhConsumption * parsed.ratePerKwhUsd) + parsed.fixedFeeUsd + parsed.fuelSurchargeUsd

            _scannerState.update { current ->
                current.copy(
                    isScanning = false,
                    scanProgress = 1.0f,
                    activeStep = 2,
                    ocrProcessingStage = "OCR Complete",
                    providerName = parsed.providerName,
                    billMonth = parsed.billMonth,
                    kwhConsumption = if (parsed.kwhConsumption % 1.0 == 0.0) parsed.kwhConsumption.toInt().toString() else parsed.kwhConsumption.toString(),
                    amperage = parsed.amperage,
                    ratePerKwhUsd = String.format(Locale.US, "%.3f", parsed.ratePerKwhUsd),
                    fixedFeeUsd = String.format(Locale.US, "%.2f", parsed.fixedFeeUsd),
                    fuelSurchargeUsd = String.format(Locale.US, "%.2f", parsed.fuelSurchargeUsd),
                    totalAmountUsd = totalUsd,
                    fairnessStatus = status,
                    overchargeAmountUsd = overcharge,
                    auditSummary = summary,
                    meterNumber = parsed.meterNumber,
                    invoiceNumber = parsed.invoiceNumber,
                    dueDate = parsed.dueDate,
                    generatorSubstation = parsed.substation,
                    confidenceScore = parsed.confidenceScore,
                    ocrRawText = result.rawText,
                    ocrErrorMessage = result.errorMessage,
                    isSaved = false
                )
            }
        }
    }

    fun triggerMockOcrScan() {
        scanBillWithCloudOcr(null)
    }

    fun resetScanner() {
        _scannerState.update {
            it.copy(
                activeStep = 0,
                isScanning = false,
                scanProgress = 0f,
                isSaved = false
            )
        }
    }

    fun saveCurrentScannedBill() {
        viewModelScope.launch {
            val currentUserId = activeUser.value?.id ?: "guest_default"
            val state = _scannerState.value
            val kwh = state.kwhConsumption.toDoubleOrNull() ?: 0.0
            val rate = state.ratePerKwhUsd.toDoubleOrNull() ?: 0.0
            val fixed = state.fixedFeeUsd.toDoubleOrNull() ?: 0.0
            val fuel = state.fuelSurchargeUsd.toDoubleOrNull() ?: 0.0
            val totalUsd = (kwh * rate) + fixed + fuel
            val totalLbp = totalUsd * 89500.0

            val bill = ScannedBill(
                userId = currentUserId,
                providerName = state.providerName,
                billMonth = state.billMonth,
                scanDate = System.currentTimeMillis(),
                kwhConsumption = kwh,
                amperage = state.amperage,
                ratePerKwhUsd = rate,
                ratePerKwhLbp = rate * 89500.0,
                officialBenchmarkRateUsd = currentBenchmark.officialGeneratorKwhUsd,
                fixedFeeUsd = fixed,
                fuelSurchargeUsd = fuel,
                totalAmountUsd = totalUsd,
                totalAmountLbp = totalLbp,
                overchargeAmountUsd = state.overchargeAmountUsd,
                fairnessStatus = state.fairnessStatus,
                aiAuditSummary = state.auditSummary,
                isComplaintFiled = false,
                notes = "Scanned via CedarWatt OCR Engine",
                meterNumber = state.meterNumber,
                invoiceNumber = state.invoiceNumber,
                dueDate = state.dueDate,
                scannedImageUri = state.scannedImageUri,
                ocrRawText = state.ocrRawText,
                confidenceScore = state.confidenceScore,
                generatorSubstation = state.generatorSubstation,
                isOfflineStored = true
            )
            repository.insertBill(bill)
            _scannerState.update { it.copy(isSaved = true) }
        }
    }

    fun submitComplaintForBill(
        bill: ScannedBill,
        municipality: String,
        violationType: String,
        discrepancyDescription: String = "",
        customClaimAmountUsd: Double = bill.overchargeAmountUsd,
        customDocumentText: String? = null
    ) {
        viewModelScope.launch {
            val currentUserId = activeUser.value?.id ?: "guest_default"
            val profile = userProfile.value
            val isAr = _appLanguage.value == AppLanguage.ARABIC
            val letter = customDocumentText ?: repository.generateComplaintLetter(
                userName = profile.fullName,
                userLocation = profile.location,
                municipality = municipality,
                bill = bill,
                violationType = violationType,
                discrepancyDescription = discrepancyDescription,
                customClaimAmountUsd = customClaimAmountUsd,
                isArabic = isAr
            )
            val caseNum = "CW-2026-" + UUID.randomUUID().toString().take(4).uppercase()
            val claimAmount = if (customClaimAmountUsd > 0) customClaimAmountUsd else bill.overchargeAmountUsd

            val tracking = if (discrepancyDescription.isNotBlank()) {
                "Filed with $municipality. Case #$caseNum. Notes: ${discrepancyDescription.take(80)}"
            } else {
                "Official complaint filed with $municipality. Case #$caseNum assigned."
            }

            val complaint = MunicipalityComplaint(
                userId = currentUserId,
                billId = bill.id,
                caseNumber = caseNum,
                municipalityName = municipality,
                providerName = bill.providerName,
                violationType = violationType,
                overchargeClaimUsd = claimAmount,
                filingDate = System.currentTimeMillis(),
                status = ComplaintStatus.SUBMITTED,
                complaintLetterText = letter,
                trackingNotes = tracking
            )
            repository.insertComplaint(complaint)

            // If there was an active draft or saved draft for this bill, clear it
            _selectedDraftForEditing.value?.let { activeDraft ->
                repository.deleteComplaintDraft(activeDraft)
            }
            _selectedDraftForEditing.value = null
            _selectedBillForComplaint.value = null
            _showComplaintForm.value = false
            _selectedTab.value = 3 // Switch to complaints view
        }
    }

    fun updateComplaintStatus(complaint: MunicipalityComplaint, newStatus: ComplaintStatus) {
        viewModelScope.launch {
            repository.updateComplaint(complaint.copy(status = newStatus))
        }
    }

    fun deleteBill(bill: ScannedBill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            if (_selectedBillDetail.value?.id == bill.id) {
                _selectedBillDetail.value = null
            }
        }
    }

    fun deleteComplaint(complaint: MunicipalityComplaint) {
        viewModelScope.launch {
            repository.deleteComplaint(complaint)
            if (_selectedComplaintDetail.value?.id == complaint.id) {
                _selectedComplaintDetail.value = null
            }
        }
    }

    fun saveProfile(
        name: String,
        location: String,
        municipality: String,
        amps: Int,
        hasSolar: Boolean,
        solarKwp: Double,
        hasBattery: Boolean,
        batteryKwh: Double
    ) {
        viewModelScope.launch {
            val currentUserId = activeUser.value?.id ?: "guest_default"
            val newProfile = UserProfile(
                userId = currentUserId,
                fullName = name,
                location = location,
                municipalityName = municipality,
                subscribedAmps = amps,
                hasSolar = hasSolar,
                solarCapacityKwp = solarKwp,
                hasBattery = hasBattery,
                batteryCapacityKwh = batteryKwh,
                currencyPreference = "USD"
            )
            repository.saveUserProfile(newProfile)
            supabaseAuthManager.updateProfile(name, activeUser.value?.phone ?: "", location, municipality)
            refreshCopilotData()
        }
    }

    // --- SUPABASE AUTHENTICATION ACTIONS ---

    fun signUp(
        email: String,
        pass: String,
        fullName: String,
        phone: String = "",
        location: String = "Achrafieh, Beirut",
        municipality: String = "Municipality of Beirut",
        onResult: (AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = supabaseAuthManager.signUp(email, pass, fullName, phone, location, municipality)
            if (result is AuthResult.Success) {
                repository.linkGuestDataToAccount(result.user.id)
                repository.saveUserProfile(
                    UserProfile(
                        userId = result.user.id,
                        fullName = result.user.fullName,
                        location = result.user.location,
                        municipalityName = result.user.municipality
                    )
                )
            }
            onResult(result)
        }
    }

    fun signIn(
        email: String,
        pass: String,
        onResult: (AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = supabaseAuthManager.signIn(email, pass)
            if (result is AuthResult.Success) {
                repository.linkGuestDataToAccount(result.user.id)
            }
            onResult(result)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            supabaseAuthManager.signOut()
        }
    }

    fun continueAsGuest() {
        val user = supabaseAuthManager.continueAsGuest()
        viewModelScope.launch {
            repository.linkGuestDataToAccount(user.id)
        }
    }

    fun recoverPassword(email: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val res = supabaseAuthManager.recoverPassword(email)
            onResult(res)
        }
    }

    fun updateSupabaseCredentials(url: String, key: String) {
        supabaseAuthManager.updateSupabaseCredentials(url, key)
    }

    fun resetSupabaseCredentials() {
        supabaseAuthManager.resetToDefaultCredentials()
    }
}

