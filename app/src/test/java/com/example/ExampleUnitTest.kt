package com.example

import com.example.data.local.BillFairnessStatus
import com.example.data.local.ComplaintStatus
import com.example.data.local.DatabaseConverters
import com.example.data.local.EnergyBill
import com.example.data.local.MunicipalityComplaint
import com.example.data.local.ScannedBill
import com.example.data.local.UserProfile
import com.example.data.remote.gemini.ChatbotRole
import com.example.data.repository.CedarWattRepository
import com.example.ui.chat.GeminiChatViewModel
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CedarWatt, Room Database entities, and Gemini Chatbot roles.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun roomEntities_creationAndDefaults() {
    val bill = EnergyBill(
      id = 101L,
      providerName = "Beirut Power Corp",
      billMonth = "August 2026",
      kwhConsumption = 250.0,
      amperage = 10,
      ratePerKwhUsd = 0.50,
      ratePerKwhLbp = 44750.0,
      officialBenchmarkRateUsd = 0.36,
      fixedFeeUsd = 15.0,
      fuelSurchargeUsd = 10.0,
      totalAmountUsd = 150.0,
      totalAmountLbp = 13425000.0,
      overchargeAmountUsd = 35.0,
      fairnessStatus = BillFairnessStatus.RED,
      aiAuditSummary = "Critical Overcharge"
    )

    assertEquals(101L, bill.id)
    assertEquals(250.0, bill.kwhConsumption, 0.001)
    assertEquals(BillFairnessStatus.RED, bill.fairnessStatus)
    assertFalse(bill.isComplaintFiled)

    val complaint = MunicipalityComplaint(
      id = 501L,
      billId = bill.id,
      caseNumber = "CW-2026-9999",
      municipalityName = "Municipality of Beirut",
      providerName = bill.providerName,
      violationType = "Price Gouging (>25% official tariff limit)",
      overchargeClaimUsd = 35.0,
      complaintLetterText = "Sample complaint text"
    )

    assertEquals(501L, complaint.id)
    assertEquals(bill.id, complaint.billId)
    assertEquals(ComplaintStatus.SUBMITTED, complaint.status)
    assertEquals(35.0, complaint.overchargeClaimUsd, 0.001)
  }

  @Test
  fun databaseConverters_convertEnumTypesCorrectly() {
    val converters = DatabaseConverters()

    assertEquals("RED", converters.fromFairnessStatus(BillFairnessStatus.RED))
    assertEquals(BillFairnessStatus.RED, converters.toFairnessStatus("RED"))
    assertEquals(BillFairnessStatus.GREEN, converters.toFairnessStatus("INVALID_VALUE"))

    assertEquals("UNDER_REVIEW", converters.fromComplaintStatus(ComplaintStatus.UNDER_REVIEW))
    assertEquals(ComplaintStatus.UNDER_REVIEW, converters.toComplaintStatus("UNDER_REVIEW"))
    assertEquals(ComplaintStatus.SUBMITTED, converters.toComplaintStatus("INVALID_VALUE"))

    assertEquals("SOLAR", converters.fromEnergySource(com.example.data.local.EnergySource.SOLAR))
    assertEquals(com.example.data.local.EnergySource.SOLAR, converters.toEnergySource("SOLAR"))
    assertEquals(com.example.data.local.EnergySource.DIESEL_GENERATOR, converters.toEnergySource("INVALID_VALUE"))
  }

  @Test
  fun roomEntities_benchmarkTariffAndUsageHistory() {
    val benchmark = com.example.data.local.BenchmarkTariff(
      id = 1L,
      monthName = "August 2026",
      officialGeneratorKwhUsd = 0.360,
      officialGeneratorKwhLbp = 32220.0,
      edlGridOffPeakUsd = 0.100,
      edlGridPeakUsd = 0.270,
      dieselPrice20LUsd = 16.80,
      fixed5AmpFeeUsd = 5.00,
      fixed10AmpFeeUsd = 10.00,
      fixed15AmpFeeUsd = 15.00,
      isCurrent = true
    )

    assertEquals(1L, benchmark.id)
    assertEquals("August 2026", benchmark.monthName)
    assertEquals(0.360, benchmark.officialGeneratorKwhUsd, 0.001)
    assertTrue(benchmark.isCurrent)

    val usageRecord = com.example.data.local.EnergyUsageRecord(
      id = 10L,
      timestamp = 1757100000000L,
      dateString = "2026-09-06",
      hourOfDay = 13,
      primarySource = com.example.data.local.EnergySource.SOLAR,
      kwhConsumed = 3.1,
      solarGeneratedKwh = 3.5,
      loadDemandKw = 2.8,
      costUsd = 0.0,
      costLbp = 0.0,
      isGridAvailable = false,
      notes = "Peak solar generation running appliances"
    )

    assertEquals(10L, usageRecord.id)
    assertEquals("2026-09-06", usageRecord.dateString)
    assertEquals(13, usageRecord.hourOfDay)
    assertEquals(com.example.data.local.EnergySource.SOLAR, usageRecord.primarySource)
    assertEquals(3.1, usageRecord.kwhConsumed, 0.001)
    assertEquals(3.5, usageRecord.solarGeneratedKwh, 0.001)
  }

  @Test
  fun complaintDocumentAutoGeneration_generatesStructuredDossier() {
    val repository = CedarWattRepository()
    val bill = ScannedBill(
      id = 1L,
      providerName = "Achrafieh Power Generators",
      billMonth = "August 2026",
      kwhConsumption = 200.0,
      amperage = 10,
      ratePerKwhUsd = 0.50,
      ratePerKwhLbp = 44750.0,
      officialBenchmarkRateUsd = 0.36,
      fixedFeeUsd = 12.0,
      fuelSurchargeUsd = 8.0,
      totalAmountUsd = 120.0,
      totalAmountLbp = 10740000.0,
      overchargeAmountUsd = 28.0,
      fairnessStatus = BillFairnessStatus.RED,
      aiAuditSummary = "Excessive tariff"
    )

    // 1. English Generation
    val englishDoc = repository.generateComplaintLetter(
      userName = "Sami Taha",
      userLocation = "Achrafieh, Beirut",
      municipality = "Municipality of Beirut",
      bill = bill,
      violationType = "Price Gouging (>25% official tariff limit)",
      discrepancyDescription = "Operator refused counter reading and demanded cash flat fee.",
      customClaimAmountUsd = 28.0,
      isArabic = false
    )

    assertTrue(englishDoc.contains("FORMAL COMPLAINT"))
    assertTrue(englishDoc.contains("Achrafieh Power Generators"))
    assertTrue(englishDoc.contains("28.00 USD"))
    assertTrue(englishDoc.contains("Resolution No. 41/2021"))
    assertTrue(englishDoc.contains("Operator refused counter reading"))

    // 2. Arabic Generation
    val arabicDoc = repository.generateComplaintLetter(
      userName = "سامي طه",
      userLocation = "الأشرفية، بيروت",
      municipality = "بلدية بيروت",
      bill = bill,
      violationType = "تسعيرة أعلى من سقف الوزارة",
      discrepancyDescription = "رفض صاحب المولد اعتماد قراءة العداد وفرض مقطوعة غير قانونية.",
      customClaimAmountUsd = 28.0,
      isArabic = true
    )

    assertTrue(arabicDoc.contains("كتاب شكوى واعتراض رسمي"))
    assertTrue(arabicDoc.contains("الأشرفية، بيروت"))
    assertTrue(arabicDoc.contains("القرار 41/2021"))
    assertTrue(arabicDoc.contains("قانون حماية المستهلك رقم 659/2005"))
    assertTrue(arabicDoc.contains("رفض صاحب المولد اعتماد قراءة العداد"))
  }

  @Test
  fun complaintForm_richStructuredContextGeneration() {
    val repository = CedarWattRepository()
    val bill = ScannedBill(
      id = 42L,
      providerName = "Ras Beirut Energy Syndicate",
      billMonth = "September 2026",
      kwhConsumption = 340.0,
      amperage = 15,
      ratePerKwhUsd = 0.52,
      ratePerKwhLbp = 46540.0,
      officialBenchmarkRateUsd = 0.36,
      fixedFeeUsd = 18.0,
      fuelSurchargeUsd = 12.0,
      totalAmountUsd = 206.8,
      totalAmountLbp = 18508600.0,
      overchargeAmountUsd = 54.4,
      fairnessStatus = BillFairnessStatus.RED,
      aiAuditSummary = "Excessive markup"
    )

    val letter = repository.generateComplaintLetter(
      userName = "Sami Taha",
      userLocation = "Hamra, Beirut",
      municipality = "Municipality of Beirut",
      bill = bill,
      violationType = "Price Gouging (>25% official tariff limit)",
      discrepancyDescription = "Operator billed $0.52/kWh and added unauthorized fuel surcharge.",
      customClaimAmountUsd = 54.40,
      isArabic = false,
      complainantPhone = "+961 03 123 456",
      department = "Municipal Energy & Generator Oversight Bureau",
      tone = "Formal Legal Notice & Reclaim",
      requestedRemedies = listOf(
        "Immediate refund or credit of $54.40 USD",
        "Dispatch municipal inspection patrol to calibrate subscriber meter #KW-9021",
        "Enforce compliance with Decree 41/2021"
      ),
      meterNumber = "KW-9021"
    )

    assertTrue(letter.contains("FORMAL COMPLAINT - MUNICIPAL ENERGY REGULATORY VIOLATION"))
    assertTrue(letter.contains("Municipality of Beirut - Municipal Energy & Generator Oversight Bureau"))
    assertTrue(letter.contains("Formal Legal Notice & Reclaim"))
    assertTrue(letter.contains("Ras Beirut Energy Syndicate"))
    assertTrue(letter.contains("Subscription Meter ID: KW-9021"))
    assertTrue(letter.contains("54.40 USD"))
    assertTrue(letter.contains("Tel: +961 03 123 456"))
    assertTrue(letter.contains("Dispatch municipal inspection patrol to calibrate subscriber meter #KW-9021"))
  }

  @Test
  fun chatbotRoles_haveCorrectModelsAndInstructions() {
    // Verify each role is mapped to the requested models
    assertEquals("gemini-3.5-flash", ChatbotRole.GENERAL_ADVISOR.modelId)
    assertEquals("gemini-3.1-pro-preview", ChatbotRole.LEGAL_AUDITOR.modelId)
    assertEquals("gemini-3.1-flash-lite-preview", ChatbotRole.FAST_CALCULATOR.modelId)

    // Verify system instructions are populated
    assertTrue(ChatbotRole.GENERAL_ADVISOR.systemInstruction.isNotBlank())
    assertTrue(ChatbotRole.LEGAL_AUDITOR.systemInstruction.isNotBlank())
    assertTrue(ChatbotRole.FAST_CALCULATOR.systemInstruction.isNotBlank())
  }

  @Test
  fun chatViewModel_roleSwitchingUpdatesWelcomeMessage() {
    val viewModel = GeminiChatViewModel()
    assertEquals(ChatbotRole.GENERAL_ADVISOR, viewModel.selectedRole.value)
    assertTrue(viewModel.messages.value.isNotEmpty())
    assertFalse(viewModel.messages.value.first().isUser)

    // Switch to Legal Auditor
    viewModel.selectRole(ChatbotRole.LEGAL_AUDITOR)
    assertEquals(ChatbotRole.LEGAL_AUDITOR, viewModel.selectedRole.value)
    assertTrue(viewModel.messages.value.first().text.contains("Legal & Regulatory Auditor"))

    // Switch to Fast Estimator
    viewModel.selectRole(ChatbotRole.FAST_CALCULATOR)
    assertEquals(ChatbotRole.FAST_CALCULATOR, viewModel.selectedRole.value)
    assertTrue(viewModel.messages.value.first().text.contains("Fast Estimator"))
  }

  @Test
  fun monthlyTrends_generatesChronologicalTrendPointsAndBenchmarks() {
    val repo = CedarWattRepository(null)
    val benchmark = com.example.data.local.BenchmarkTariff(
      id = 1L,
      monthName = "August 2026",
      officialGeneratorKwhUsd = 0.360,
      officialGeneratorKwhLbp = 32220.0,
      edlGridOffPeakUsd = 0.100,
      edlGridPeakUsd = 0.270,
      dieselPrice20LUsd = 16.80,
      fixed5AmpFeeUsd = 5.00,
      fixed10AmpFeeUsd = 10.00,
      fixed15AmpFeeUsd = 15.00,
      isCurrent = true
    )
    val profile = UserProfile(subscribedAmps = 10)

    val sampleBill = EnergyBill(
      id = 42L,
      providerName = "Hamra Microgrid",
      billMonth = "August 2026",
      kwhConsumption = 280.0,
      amperage = 10,
      ratePerKwhUsd = 0.500,
      ratePerKwhLbp = 44750.0,
      officialBenchmarkRateUsd = 0.360,
      fixedFeeUsd = 15.0,
      fuelSurchargeUsd = 0.0,
      totalAmountUsd = 155.0,
      totalAmountLbp = 13872500.0,
      overchargeAmountUsd = 39.20,
      fairnessStatus = BillFairnessStatus.RED,
      aiAuditSummary = "Overcharge"
    )

    val trends = repo.getMonthlyTrendData(listOf(sampleBill), benchmark, profile)

    // Verify 6 months historical points are returned
    assertEquals(6, trends.size)

    // Check August point mapped to the scanned bill
    val augPoint = trends.firstOrNull { it.monthLabel == "Aug" || it.fullMonthName.contains("August") }
    assertNotNull(augPoint)
    assertTrue(augPoint!!.isFromScannedBill)
    assertEquals(42L, augPoint.associatedBillId)
    assertEquals(280.0, augPoint.kwhConsumption, 0.01)
    assertEquals(0.500, augPoint.billedRateUsd, 0.001)
    assertEquals(0.360, augPoint.benchmarkRateUsd, 0.001)
    assertTrue("Variance should be > 0", augPoint.variancePct > 30)

    // Check household consumption brackets
    val (low10, typ10, high10) = repo.getHouseholdConsumptionBenchmark(10)
    assertTrue(low10 < typ10 && typ10 < high10)
    assertEquals(310.0, typ10, 0.1)

    val (low5, typ5, high5) = repo.getHouseholdConsumptionBenchmark(5)
    assertTrue(typ5 < typ10)
  }

  @Test
  fun supabaseAuthModels_andUserScoping() {
    val authUser = com.example.data.remote.supabase.AuthUser(
      id = "user_abc123",
      email = "resident@cedarwatt.ai",
      fullName = "Karim Khoury",
      phone = "+961 70 888 999",
      location = "Achrafieh, Beirut",
      municipality = "Municipality of Beirut",
      token = "jwt_token_sample",
      isGuest = false
    )

    assertEquals("user_abc123", authUser.id)
    assertEquals("resident@cedarwatt.ai", authUser.email)
    assertEquals("Karim Khoury", authUser.fullName)
    assertFalse(authUser.isGuest)

    val authState = com.example.data.remote.supabase.AuthState.Authenticated(authUser)
    assertEquals(authUser, authState.user)

    // Test entity userId linking
    val bill = EnergyBill(
      id = 99L,
      userId = authUser.id,
      providerName = "Achrafieh Power",
      billMonth = "September 2026",
      kwhConsumption = 300.0,
      amperage = 10,
      ratePerKwhUsd = 0.45,
      ratePerKwhLbp = 40275.0,
      officialBenchmarkRateUsd = 0.36,
      fixedFeeUsd = 10.0,
      fuelSurchargeUsd = 0.0,
      totalAmountUsd = 145.0,
      totalAmountLbp = 12977500.0,
      overchargeAmountUsd = 27.0,
      fairnessStatus = BillFairnessStatus.RED,
      aiAuditSummary = "Overcharged"
    )

    assertEquals(authUser.id, bill.userId)

    val draft = com.example.data.local.ComplaintDraft(
      id = 1L,
      userId = authUser.id,
      billId = bill.id,
      billProviderName = bill.providerName,
      claimAmountUsd = 27.0
    )
    assertEquals(authUser.id, draft.userId)

    val profile = UserProfile(
      userId = authUser.id,
      fullName = authUser.fullName,
      location = authUser.location,
      municipalityName = authUser.municipality
    )
    assertEquals(authUser.id, profile.userId)
    assertEquals("Karim Khoury", profile.fullName)
  }
}


