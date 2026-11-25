package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.BenchmarkTariff
import com.example.data.local.BillFairnessStatus
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ComplaintDraft
import com.example.data.local.ComplaintStatus
import com.example.data.local.EnergySavingTip
import com.example.data.local.EnergySource
import com.example.data.local.EnergyTimelineSlot
import com.example.data.local.EnergyBill
import com.example.data.local.EnergyUsageRecord
import com.example.data.local.MonthlyEnergyTrendPoint
import com.example.data.local.MunicipalityComplaint
import com.example.data.local.ScannedBill
import com.example.data.local.TipCategory
import com.example.data.local.UserProfile
import com.example.data.local.toChatMessage
import com.example.data.local.toEntity
import com.example.data.remote.gemini.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CedarWattRepository(
    private val database: AppDatabase? = null,
    private val energyRepository: EnergyRepository = if (database != null) {
        EnergyRepositoryImpl(
            database.energyBillDao(),
            database.municipalityComplaintDao(),
            database.tariffBenchmarkDao(),
            database.usageHistoryDao(),
            database.chatMessageDao(),
            database.complaintDraftDao()
        )
    } else {
        object : EnergyRepository {
            override val allBills: Flow<List<EnergyBill>> = flowOf(emptyList())
            override val latestBill: Flow<EnergyBill?> = flowOf(null)
            override val overchargedBills: Flow<List<EnergyBill>> = flowOf(emptyList())
            override val allComplaints: Flow<List<MunicipalityComplaint>> = flowOf(emptyList())
            override val allDrafts: Flow<List<ComplaintDraft>> = flowOf(emptyList())
            override val allBenchmarks: Flow<List<BenchmarkTariff>> = flowOf(emptyList())
            override val currentBenchmark: Flow<BenchmarkTariff?> = flowOf(null)
            override val allUsageHistory: Flow<List<EnergyUsageRecord>> = flowOf(emptyList())
            override val allChatMessages: Flow<List<ChatMessageEntity>> = flowOf(emptyList())

            override suspend fun getBillById(id: Long): EnergyBill? = null
            override fun searchBills(query: String): Flow<List<EnergyBill>> = flowOf(emptyList())
            override fun getBillsByFairness(status: BillFairnessStatus): Flow<List<EnergyBill>> = flowOf(emptyList())
            override suspend fun insertBill(bill: EnergyBill): Long = 0L
            override suspend fun updateBill(bill: EnergyBill) {}
            override suspend fun deleteBill(bill: EnergyBill) {}
            override suspend fun markComplaintFiled(billId: Long) {}
            override suspend fun getComplaintById(id: Long): MunicipalityComplaint? = null
            override suspend fun getComplaintForBill(billId: Long): MunicipalityComplaint? = null
            override suspend fun insertComplaint(complaint: MunicipalityComplaint): Long = 0L
            override suspend fun updateComplaint(complaint: MunicipalityComplaint) {}
            override suspend fun deleteComplaint(complaint: MunicipalityComplaint) {}

            override suspend fun getDraftById(id: Long): ComplaintDraft? = null
            override fun getDraftForBill(billId: Long): Flow<ComplaintDraft?> = flowOf(null)
            override suspend fun insertDraft(draft: ComplaintDraft): Long = 0L
            override suspend fun updateDraft(draft: ComplaintDraft) {}
            override suspend fun deleteDraft(draft: ComplaintDraft) {}
            override suspend fun deleteDraftById(id: Long) {}

            override suspend fun getBenchmarkById(id: Long): BenchmarkTariff? = null
            override suspend fun getBenchmarkByMonth(monthName: String): BenchmarkTariff? = null
            override suspend fun insertBenchmark(benchmark: BenchmarkTariff): Long = 0L
            override suspend fun insertBenchmarks(benchmarks: List<BenchmarkTariff>) {}
            override suspend fun updateBenchmark(benchmark: BenchmarkTariff) {}
            override suspend fun deleteBenchmark(benchmark: BenchmarkTariff) {}

            override fun getUsageByDate(date: String): Flow<List<EnergyUsageRecord>> = flowOf(emptyList())
            override fun getRecentUsage(limit: Int): Flow<List<EnergyUsageRecord>> = flowOf(emptyList())
            override suspend fun insertUsage(record: EnergyUsageRecord): Long = 0L
            override suspend fun insertAllUsage(records: List<EnergyUsageRecord>) {}
            override suspend fun deleteUsage(record: EnergyUsageRecord) {}
            override suspend fun clearUsageHistory() {}

            override fun searchChatMessages(query: String): Flow<List<ChatMessageEntity>> = flowOf(emptyList())
            override suspend fun insertChatMessage(message: ChatMessageEntity): Long = 0L
            override suspend fun insertChatMessages(messages: List<ChatMessageEntity>) {}
            override suspend fun deleteChatMessage(id: String) {}
            override suspend fun clearChatMessages() {}

            override fun getBillsForUser(userId: String): Flow<List<EnergyBill>> = flowOf(emptyList())
            override fun getLatestBillForUser(userId: String): Flow<EnergyBill?> = flowOf(null)
            override fun getComplaintsForUser(userId: String): Flow<List<MunicipalityComplaint>> = flowOf(emptyList())
            override fun getDraftsForUser(userId: String): Flow<List<ComplaintDraft>> = flowOf(emptyList())
            override fun getUsageHistoryForUser(userId: String): Flow<List<EnergyUsageRecord>> = flowOf(emptyList())
            override fun getChatMessagesForUser(userId: String): Flow<List<ChatMessageEntity>> = flowOf(emptyList())
            override suspend fun linkGuestDataToAccount(userId: String) {}
        }
    }
) : EnergyRepository by energyRepository {

    val userProfile: Flow<UserProfile?> = database?.userProfileDao()?.getDefaultProfile() ?: flowOf(null)

    fun getUserProfile(userId: String): Flow<UserProfile?> =
        database?.userProfileDao()?.getUserProfile(userId) ?: flowOf(null)


    val allComplaintDrafts: Flow<List<ComplaintDraft>> = allDrafts

    val chatMessagesDomain: Flow<List<ChatMessage>> = allChatMessages.map { list ->
        list.map { it.toChatMessage() }
    }

    fun searchStoredChatHistory(query: String): Flow<List<ChatMessage>> =
        searchChatMessages(query).map { list -> list.map { it.toChatMessage() } }

    suspend fun saveChatMessage(message: ChatMessage, billId: Long? = null, userId: String = "guest_default") {
        insertChatMessage(message.toEntity(billContextId = billId, userId = userId))
    }

    suspend fun clearChatMessagesHistory() {
        clearChatMessages()
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        database?.userProfileDao()?.saveUserProfile(profile)
    }

    suspend fun saveComplaintDraft(draft: ComplaintDraft): Long = insertDraft(draft)

    suspend fun deleteComplaintDraft(draft: ComplaintDraft) = deleteDraft(draft)

    suspend fun deleteComplaintDraftById(id: Long) = deleteDraftById(id)

    suspend fun getComplaintDraftById(id: Long): ComplaintDraft? = getDraftById(id)

    suspend fun logQuickUsageEntry(
        source: EnergySource,
        kwh: Double,
        costUsd: Double,
        peakDemandKw: Double = 0.0,
        generatorMinutes: Int = 0,
        notes: String = "",
        userId: String = "guest_default"
    ): Long {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val now = Date()
        val record = EnergyUsageRecord(
            userId = userId,
            timestamp = now.time,
            dateString = dateFormat.format(now),
            hourOfDay = SimpleDateFormat("HH", Locale.US).format(now).toIntOrNull() ?: 12,
            primarySource = source,
            kwhConsumed = kwh,
            solarGeneratedKwh = if (source == EnergySource.SOLAR) kwh else 0.0,
            loadDemandKw = peakDemandKw,
            costUsd = costUsd,
            costLbp = costUsd * 89500.0,
            isGridAvailable = source == EnergySource.EDL_GRID,
            notes = notes,
            peakDemandKw = peakDemandKw,
            generatorRunMinutes = generatorMinutes,
            isOfflineRecord = true
        )
        return insertUsage(record)
    }

    // Current official benchmark rates
    val currentBenchmarkTariff = BenchmarkTariff(
        monthName = "August 2026",
        officialGeneratorKwhUsd = 0.360,
        officialGeneratorKwhLbp = 32220.0,
        edlGridOffPeakUsd = 0.100,
        edlGridPeakUsd = 0.270,
        dieselPrice20LUsd = 16.80,
        fixed5AmpFeeUsd = 5.00,
        fixed10AmpFeeUsd = 10.00,
        fixed15AmpFeeUsd = 15.00
    )

    fun calculateBillAudit(
        kwh: Double,
        amps: Int,
        chargedRateUsd: Double,
        fixedFeeUsd: Double,
        fuelSurchargeUsd: Double
    ): Triple<BillFairnessStatus, Double, String> {
        val benchmarkRate = currentBenchmarkTariff.officialGeneratorKwhUsd
        val legalFixedFee = when {
            amps <= 5 -> currentBenchmarkTariff.fixed5AmpFeeUsd
            amps <= 10 -> currentBenchmarkTariff.fixed10AmpFeeUsd
            else -> currentBenchmarkTariff.fixed15AmpFeeUsd
        }

        val fairTotal = (kwh * benchmarkRate) + legalFixedFee
        val actualTotal = (kwh * chargedRateUsd) + fixedFeeUsd + fuelSurchargeUsd
        val overcharge = (actualTotal - fairTotal).coerceAtLeast(0.0)

        val rateRatio = if (benchmarkRate > 0) chargedRateUsd / benchmarkRate else 1.0

        val (status, summary) = when {
            rateRatio > 1.25 || overcharge > 25.0 -> {
                val percentOver = ((rateRatio - 1.0) * 100).toInt()
                BillFairnessStatus.RED to "CRITICAL OVERCHARGE: Billed rate is $percentOver% above Ministry ceiling ($${String.format(Locale.US, "%.3f", benchmarkRate)}/kWh). Estimated unjustified cost: $${String.format(Locale.US, "%.2f", overcharge)} USD. Eligible for municipal complaint."
            }
            rateRatio > 1.08 || overcharge > 8.0 -> {
                val percentOver = ((rateRatio - 1.0) * 100).toInt()
                BillFairnessStatus.AMBER to "MODERATE WARNING: Rate is $percentOver% above official benchmark. Minor fuel surcharge discrepancy ($${String.format(Locale.US, "%.2f", fuelSurchargeUsd)})."
            }
            else -> {
                BillFairnessStatus.GREEN to "FAIR TARIFF: Unit rate matches official energy price decree ($${String.format(Locale.US, "%.3f", benchmarkRate)}/kWh) within regulatory tolerance."
            }
        }

        return Triple(status, overcharge, summary)
    }

    fun generateComplaintLetter(
        userName: String,
        userLocation: String,
        municipality: String,
        bill: ScannedBill,
        violationType: String = "Price Gouging (>25% official tariff limit)",
        discrepancyDescription: String = "",
        customClaimAmountUsd: Double = bill.overchargeAmountUsd,
        isArabic: Boolean = false,
        complainantPhone: String = "",
        department: String = "Municipal Energy & Generator Oversight Bureau",
        tone: String = "Official Administrative Petition",
        requestedRemedies: List<String> = emptyList(),
        meterNumber: String = ""
    ): String {
        val claimAmount = if (customClaimAmountUsd > 0) customClaimAmountUsd else bill.overchargeAmountUsd
        val claimAmountLbp = claimAmount * 89500.0
        val discrepancyPct = if (bill.officialBenchmarkRateUsd > 0) {
            (((bill.ratePerKwhUsd / bill.officialBenchmarkRateUsd) - 1.0) * 100).toInt()
        } else 0
        val certCode = UUID.randomUUID().toString().take(8).uppercase()
        val meterDisplay = if (meterNumber.isNotBlank()) meterNumber else "Meter #KW-${(1000..9999).random()}"

        if (isArabic) {
            val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale("ar")).format(Date())
            val desc = if (discrepancyDescription.isNotBlank()) discrepancyDescription.trim() else "قيام صاحب المولد بفرض تسعيرة تفوق السقف الرسمي لوزارة الطاقة والمياه واحتساب مبالغ إضافية دون وجه حق وخلافاً للأنظمة."
            val phoneLine = if (complainantPhone.isNotBlank()) " | هاتف: $complainantPhone" else ""

            val remediesText = if (requestedRemedies.isNotEmpty()) {
                requestedRemedies.mapIndexed { idx, rem -> "${idx + 1}. $rem" }.joinToString("\n")
            } else {
                """
                1. إلزام المشكو بحقه بإعادة مبلغ الزيادة غير القانونية وقدره $${String.format(Locale.US, "%.2f", claimAmount)} دولار (أو ما يعادله بالليرة: ${String.format(Locale.US, "%,.0f", claimAmountLbp)} ل.ل) فوراً أو قيده كرصيد دائن.
                2. إيفاد مفتشي ومراقبي البلدية للتحقق من عداد المشترك وفرض الغرامات المنصوص عليها قانوناً.
                3. إلزام المشغل بالامتثال التام للتسعيرة التوجيهية الشهرية الصادرة عن وزارة الطاقة والمياه.
                """.trimIndent()
            }

            return """
كتاب شكوى واعتراض رسمي - مخالفات تسعيرة المولدات الخاصة
التاريخ: $dateStr
إلى: جانب المجلس البلدي لبلدية $municipality المحترم
الدائرة المختصة: $department
نسخة إلى: مصلحة حماية المستهلك - وزارة الاقتصاد والتجارة
مقدّم الشكوى: $userName (العنوان السكني: $userLocation$phoneLine)

الموضوع: اعتراض رسمي ومطالبة باسترداد مبالغ غير مشروعة عن فاتورة شهر ${bill.billMonth}
الصفة الإدارية للملف: $tone
المشكو بحقه: المشغّل / صاحب المولد (${bill.providerName})
رقم العداد / الاشتراك: $meterDisplay

أولاً: بيانات الاشتراك والتدقيق الحسابي (بيانات معتمدة من تطبيق CedarWatt):
- قوة الاشتراك: ${bill.amperage} أمبير
- استهلاك العداد المسجل: ${String.format(Locale.US, "%.2f", bill.kwhConsumption)} كيلوواط ساعة (kWh)
- التسعيرة المطبقة من المشغل: $${String.format(Locale.US, "%.3f", bill.ratePerKwhUsd)} لكل ك.و.س (${String.format(Locale.US, "%,.0f", bill.ratePerKwhLbp)} ل.ل)
- السقف التوجيهي الرسمي لوزارة الطاقة والمياه: $${String.format(Locale.US, "%.3f", bill.officialBenchmarkRateUsd)} لكل ك.و.س
- نسبة الزيادة غير المشروعة: +$discrepancyPct% فوق السقف المعتمد
- الرسوم الثابتة وبدلات الوقود الإضافية: $${String.format(Locale.US, "%.2f", bill.fixedFeeUsd + bill.fuelSurchargeUsd)} دولار
- إجمالي مبلغ الاسترداد المطالب به: $${String.format(Locale.US, "%.2f", claimAmount)} دولار أميركي (≈ ${String.format(Locale.US, "%,.0f", claimAmountLbp)} ل.ل)

ثانياً: تصنيف المخالفة القانونية الرئيسية:
- $violationType

ثالثاً: بيان المشترك وتفاصيل الفروقات المالية المشكو منها:
$desc

رابعاً: السند القانوني والنصوص المرعية:
- قرارات وتعاميم وزارة الطاقة والمياه اللبنانية، ولا سيما القرار 41/2021 الملزم بتركيب عدادات الكيلوواط واعتماد التسعيرة الرسمية.
- قانون حماية المستهلك رقم 659/2005 وقرارات وزارة الاقتصاد لمكافحة الاحتكار والأسعار الجائرة.
- صلاحيات البلديات في الرقابة وضبط المخالفات بموجب قانون البلديات اللبناني (المرسوم الاشتراعي رقم 118/1977).

خامساً: المطالب والرجاء:
$remediesText

وتفضلوا بقبول فائق الاحترام والتقدير،
مقدّم الشكوى: $userName
رمز التوثيق الإلكتروني: CW-$certCode
            """.trimIndent()
        } else {
            val dateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date())
            val desc = if (discrepancyDescription.isNotBlank()) discrepancyDescription.trim() else "The operator billed rates and surcharges significantly exceeding the official Ministry of Energy & Water tariff ceilings."
            val phoneLine = if (complainantPhone.isNotBlank()) " | Tel: $complainantPhone" else ""

            val remediesText = if (requestedRemedies.isNotEmpty()) {
                requestedRemedies.mapIndexed { idx, rem -> "${idx + 1}. $rem" }.joinToString("\n")
            } else {
                """
                1. Mandatory credit or refund of $${String.format(Locale.US, "%.2f", claimAmount)} USD (≈ ${String.format(Locale.US, "%,.0f", claimAmountLbp)} LBP) credited on the upcoming billing cycle.
                2. Immediate municipal inspection of the subscriber meter and operator distribution hub.
                3. Enforcement of full adherence to the official Ministry kWh ceiling.
                """.trimIndent()
            }

            return """
FORMAL COMPLAINT - MUNICIPAL ENERGY REGULATORY VIOLATION
Date: $dateStr
To: $municipality - $department
Cc: Ministry of Economy & Trade - Consumer Protection Directorate
From: $userName (Resident: $userLocation$phoneLine)

Subject: Formal Grievance & Refund Claim for Private Generator Billing Overcharges (${bill.billMonth})
Document Classification: $tone
Respondent Operator: ${bill.providerName}
Subscription Meter ID: $meterDisplay

1. SUBSCRIPTION & AUDIT METRICS (CedarWatt Certified Audit):
   - Subscribed Breaker: ${bill.amperage} Amperes
   - Metered Consumption: ${String.format(Locale.US, "%.2f", bill.kwhConsumption)} kWh
   - Billed Unit Rate: $${String.format(Locale.US, "%.3f", bill.ratePerKwhUsd)} USD / kWh (${String.format(Locale.US, "%,.0f", bill.ratePerKwhLbp)} LBP)
   - Official Ministry Benchmark: $${String.format(Locale.US, "%.3f", bill.officialBenchmarkRateUsd)} USD / kWh
   - Rate Discrepancy: +$discrepancyPct% above legal ceiling
   - Fixed Surcharges & Fees: $${String.format(Locale.US, "%.2f", bill.fixedFeeUsd + bill.fuelSurchargeUsd)} USD
   - Total Overcharge Claimed: $${String.format(Locale.US, "%.2f", claimAmount)} USD (≈ ${String.format(Locale.US, "%,.0f", claimAmountLbp)} LBP)

2. PRIMARY VIOLATION CLASSIFICATION:
   - $violationType

3. CITIZEN STATEMENT OF BILLING DISCREPANCIES:
   $desc

4. STATUTORY & REGULATORY CITATIONS:
   - Ministry of Energy & Water Resolution No. 41/2021 (Mandatory Metering & Regulated kWh Tariff Ceilings).
   - Lebanese Consumer Protection Law No. 659/2005 (Prohibition of Price Manipulation & Coercive Fees).
   - Municipal Regulatory Authority under Decree-Law No. 118/1977.

5. FORMAL DEMANDS:
$remediesText

Respectfully submitted,
$userName
Digital Certification Reference: CW-$certCode
            """.trimIndent()
        }
    }

    fun get24HourTimeline(profile: UserProfile?): List<EnergyTimelineSlot> {
        val hasSolar = profile?.hasSolar ?: true
        val solarCap = profile?.solarCapacityKwp ?: 3.5

        return listOf(
            EnergyTimelineSlot(0, "12 AM - 4 AM", EnergySource.BATTERY, false, 0.0, 0.45, 0.02, "Run night essentials on battery inverter."),
            EnergyTimelineSlot(4, "4 AM - 6 AM", EnergySource.DIESEL_GENERATOR, false, 0.0, 0.60, 0.36, "Generator startup period. Keep AC at 24°C."),
            EnergyTimelineSlot(6, "6 AM - 9 AM", EnergySource.EDL_GRID, true, 0.4, 1.20, 0.10, "EDL grid window. Excellent time to recharge batteries."),
            EnergyTimelineSlot(9, "9 AM - 12 PM", EnergySource.SOLAR, false, solarCap * 0.75, 1.80, 0.00, "Solar ramp up! Turn on water heater / dishwasher."),
            EnergyTimelineSlot(12, "12 PM - 3 PM", EnergySource.SOLAR, false, solarCap * 0.95, 2.40, 0.00, "PEAK SOLAR GENERATION. 100% free green energy window."),
            EnergyTimelineSlot(15, "3 PM - 6 PM", EnergySource.SOLAR, false, solarCap * 0.50, 1.40, 0.00, "Afternoon solar decay. Top off battery reserves."),
            EnergyTimelineSlot(18, "6 PM - 10 PM", EnergySource.DIESEL_GENERATOR, false, 0.0, 2.10, 0.36, "PEAK GENERATOR EXPENSE ($0.36/kWh). Shift heavy loads!"),
            EnergyTimelineSlot(22, "10 PM - 12 AM", EnergySource.BATTERY, false, 0.0, 0.80, 0.02, "Switch to battery storage to avoid high night tariffs.")
        )
    }

    fun getSmartSavingsTips(profile: UserProfile?): List<EnergySavingTip> {
        val hasSolar = profile?.hasSolar ?: true
        return listOf(
            EnergySavingTip(
                id = "tip_solar_water_heater",
                title = "Shift Water Heater to 11:30 AM Solar Peak",
                description = "Running your 1500W water heater during solar hours instead of the 7:00 PM generator window eliminates 45 kWh of generator usage monthly.",
                estimatedMonthlySavingsUsd = 16.20,
                category = TipCategory.SOLAR_OPTIMIZATION,
                difficulty = "Easy",
                impactScore = 94
            ),
            EnergySavingTip(
                id = "tip_inverter_night_cutoff",
                title = "Night Inverter Deep Discharge Buffer",
                description = "Configure battery inverter cutoff to 25% DOD so backup power carries through late night without triggering generator cutover.",
                estimatedMonthlySavingsUsd = 22.50,
                category = TipCategory.GENERATOR_CUTOFF,
                difficulty = "Medium",
                impactScore = 88
            ),
            EnergySavingTip(
                id = "tip_aircon_eco_inverter",
                title = "AC Pre-Cooling During 1 PM Solar Window",
                description = "Cool living areas down to 21°C using free midday solar; then maintain at 24°C in the evening to reduce generator compressor strain.",
                estimatedMonthlySavingsUsd = 18.90,
                category = TipCategory.APPLIANCE_TIMING,
                difficulty = "Easy",
                impactScore = 82
            ),
            EnergySavingTip(
                id = "tip_amp_downgrade",
                title = "Audit Amperage Breaker (15A to 10A)",
                description = "With your 3.6kW solar array shaving peak draws, dropping from 15A to 10A generator subscription saves $5/mo in base fees.",
                estimatedMonthlySavingsUsd = 5.00,
                category = TipCategory.TARIFF_SWITCH,
                difficulty = "Medium",
                impactScore = 70
            )
        )
    }

    /**
     * Aggregates Room database bills into chronological monthly trend points,
     * supplemented with realistic Lebanese municipal context for complete historical benchmarking.
     */
    fun getMonthlyTrendData(
        bills: List<EnergyBill>,
        benchmark: BenchmarkTariff,
        userProfile: UserProfile?
    ): List<MonthlyEnergyTrendPoint> {
        val userAmps = userProfile?.subscribedAmps ?: 10
        val baseProvider = bills.firstOrNull()?.providerName ?: "Beirut Power Generators"

        // Baseline 6-month historical trend (April to September 2026) with official Ministry ceilings
        val baselineMonths = listOf(
            Triple("Apr", "April 2026", 0.340),
            Triple("May", "May 2026", 0.345),
            Triple("Jun", "June 2026", 0.350),
            Triple("Jul", "July 2026", 0.355),
            Triple("Aug", "August 2026", 0.360),
            Triple("Sep", "September 2026", benchmark.officialGeneratorKwhUsd.coerceAtLeast(0.365))
        )

        // Map bills by lowercase normalized month string (e.g. "august 2026", "august", "aug")
        val billsByMonth = bills.associateBy { b ->
            b.billMonth.trim().lowercase(Locale.US)
        }

        return baselineMonths.mapIndexed { idx, (shortLabel, fullMonth, officialCap) ->
            // Check if user has an actual scanned bill for this month
            val matchingBill = billsByMonth[fullMonth.lowercase(Locale.US)]
                ?: billsByMonth[shortLabel.lowercase(Locale.US)]
                ?: bills.getOrNull(idx)

            if (matchingBill != null) {
                val rate = matchingBill.ratePerKwhUsd
                val cap = if (matchingBill.officialBenchmarkRateUsd > 0) matchingBill.officialBenchmarkRateUsd else officialCap
                val variance = if (cap > 0) (((rate / cap) - 1.0) * 100).toInt() else 0
                val fairCost = matchingBill.kwhConsumption * cap
                val overcharge = if (matchingBill.overchargeAmountUsd > 0) matchingBill.overchargeAmountUsd else (matchingBill.totalAmountUsd - fairCost).coerceAtLeast(0.0)

                MonthlyEnergyTrendPoint(
                    monthLabel = shortLabel,
                    fullMonthName = matchingBill.billMonth.ifBlank { fullMonth },
                    kwhConsumption = matchingBill.kwhConsumption,
                    billedRateUsd = rate,
                    benchmarkRateUsd = cap,
                    edlGridRateUsd = benchmark.edlGridOffPeakUsd,
                    totalBilledUsd = matchingBill.totalAmountUsd,
                    fairCostUsd = fairCost,
                    overchargeUsd = overcharge,
                    variancePct = variance,
                    providerName = matchingBill.providerName,
                    amperage = matchingBill.amperage,
                    isFromScannedBill = true,
                    associatedBillId = matchingBill.id
                )
            } else {
                // Realistic historical projection based on seasonal AC load in Lebanon
                // Spring (Apr-May: ~180-220 kWh), Summer peak (Jun-Aug: ~260-340 kWh), Autumn (Sep: ~240 kWh)
                val projectedKwh = when (shortLabel) {
                    "Apr" -> 190.0
                    "May" -> 210.0
                    "Jun" -> 275.0
                    "Jul" -> 320.0
                    "Aug" -> 310.0
                    else -> 245.0
                }
                // Simulated typical private operator tariff in Beirut (+15% to +35% over Ministry cap)
                val simulatedMarkupPct = when (shortLabel) {
                    "Apr" -> 10
                    "May" -> 15
                    "Jun" -> 25
                    "Jul" -> 35
                    "Aug" -> 32
                    else -> 28
                }
                val billedRate = officialCap * (1.0 + (simulatedMarkupPct / 100.0))
                val totalBilled = (projectedKwh * billedRate) + (if (userAmps >= 10) 12.0 else 6.0)
                val fairCost = projectedKwh * officialCap
                val overcharge = (totalBilled - fairCost).coerceAtLeast(0.0)

                MonthlyEnergyTrendPoint(
                    monthLabel = shortLabel,
                    fullMonthName = fullMonth,
                    kwhConsumption = projectedKwh,
                    billedRateUsd = billedRate,
                    benchmarkRateUsd = officialCap,
                    edlGridRateUsd = benchmark.edlGridOffPeakUsd,
                    totalBilledUsd = totalBilled,
                    fairCostUsd = fairCost,
                    overchargeUsd = overcharge,
                    variancePct = simulatedMarkupPct,
                    providerName = baseProvider,
                    amperage = userAmps,
                    isFromScannedBill = false,
                    associatedBillId = null
                )
            }
        }
    }

    /**
     * Standard household monthly consumption brackets in Lebanon based on generator amperage.
     * Returns: Triple(Efficient / Low kWh, Typical kWh, High / AC Peak kWh)
     */
    fun getHouseholdConsumptionBenchmark(amperage: Int): Triple<Double, Double, Double> {
        return when {
            amperage <= 5 -> Triple(100.0, 180.0, 260.0)
            amperage <= 10 -> Triple(200.0, 310.0, 450.0)
            amperage <= 15 -> Triple(320.0, 480.0, 680.0)
            else -> Triple(450.0, 650.0, 950.0)
        }
    }
}

/**
 * Typealias for repository matching alternate naming "cidarwatt".
 */
typealias CidarWattRepository = CedarWattRepository
