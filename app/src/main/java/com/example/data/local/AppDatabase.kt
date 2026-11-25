package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.remote.gemini.ChatbotRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        EnergyBill::class,
        MunicipalityComplaint::class,
        UserProfile::class,
        BenchmarkTariff::class,
        EnergyUsageRecord::class,
        ChatMessageEntity::class,
        ComplaintDraft::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun energyBillDao(): EnergyBillDao
    abstract fun municipalityComplaintDao(): MunicipalityComplaintDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun tariffBenchmarkDao(): TariffBenchmarkDao
    abstract fun usageHistoryDao(): UsageHistoryDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun complaintDraftDao(): ComplaintDraftDao

    fun billDao(): BillDao = energyBillDao()
    fun complaintDao(): ComplaintDao = municipalityComplaintDao()
    fun benchmarkDao(): BenchmarkDao = tariffBenchmarkDao()
    fun usageDao(): UsageHistoryDao = usageHistoryDao()
    fun chatDao(): ChatDao = chatMessageDao()
    fun draftDao(): ComplaintDraftDao = complaintDraftDao()

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cedarwatt_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(
                            database.billDao(),
                            database.complaintDao(),
                            database.userProfileDao(),
                            database.benchmarkDao(),
                            database.usageDao(),
                            database.chatDao(),
                            database.draftDao()
                        )
                    }
                }
            }
        }

        private suspend fun populateInitialData(
            billDao: BillDao,
            complaintDao: ComplaintDao,
            userProfileDao: UserProfileDao,
            benchmarkDao: BenchmarkDao,
            usageDao: UsageHistoryDao,
            chatDao: ChatDao,
            draftDao: ComplaintDraftDao
        ) {
            // Default user profile
            userProfileDao.saveUserProfile(
                UserProfile(
                    userId = "guest_default",
                    fullName = "Sami Taha",
                    location = "Achrafieh, Beirut",
                    municipalityName = "Municipality of Beirut",
                    subscribedAmps = 10,
                    hasSolar = true,
                    solarCapacityKwp = 3.6,
                    hasBattery = true,
                    batteryCapacityKwh = 5.12,
                    currencyPreference = "USD"
                )
            )

            // Seed initial realistic bills with complete scanned metadata
            val bill1 = ScannedBill(
                providerName = "Achrafieh Electric Power Co.",
                billMonth = "August 2026",
                scanDate = System.currentTimeMillis() - 86400000L * 2,
                kwhConsumption = 285.0,
                amperage = 10,
                ratePerKwhUsd = 0.52,
                ratePerKwhLbp = 46540.0,
                officialBenchmarkRateUsd = 0.36,
                fixedFeeUsd = 18.0,
                fuelSurchargeUsd = 12.50,
                totalAmountUsd = 178.70,
                totalAmountLbp = 16000000.0,
                overchargeAmountUsd = 45.60,
                fairnessStatus = BillFairnessStatus.RED,
                aiAuditSummary = "CRITICAL: Rate is 44.4% above official Ministry of Energy ceiling ($0.36/kWh). Amperage flat fee inflated by $6.00.",
                isComplaintFiled = true,
                notes = "Meter was verified by building committee.",
                meterNumber = "KW-8842-BEIRUT",
                invoiceNumber = "INV-2026-0881",
                dueDate = "2026-09-10",
                generatorSubstation = "Achrafieh Sector 4 Substation B",
                confidenceScore = 0.98,
                ocrRawText = "ACHRAFIEH ELECTRIC POWER CO.\nPeriod: August 2026\nMeter No: KW-8842-BEIRUT\nConsumption: 285 kWh\nRate: $0.520/kWh\nFixed Base Fee: $18.00\nFuel Surcharge: $12.50\nTOTAL: $178.70 USD / 16,000,000 LBP",
                isOfflineStored = true
            )
            val bill1Id = billDao.insertBill(bill1)

            val bill2 = ScannedBill(
                providerName = "Metn Coastal Generators",
                billMonth = "July 2026",
                scanDate = System.currentTimeMillis() - 86400000L * 32,
                kwhConsumption = 310.0,
                amperage = 10,
                ratePerKwhUsd = 0.39,
                ratePerKwhLbp = 34900.0,
                officialBenchmarkRateUsd = 0.35,
                fixedFeeUsd = 12.0,
                fuelSurchargeUsd = 5.00,
                totalAmountUsd = 137.90,
                totalAmountLbp = 12340000.0,
                overchargeAmountUsd = 12.40,
                fairnessStatus = BillFairnessStatus.AMBER,
                aiAuditSummary = "MODERATE: Rate is 11.4% above benchmark. Within acceptable municipal buffer, but surcharge is unitemized.",
                isComplaintFiled = false,
                notes = "Pre-solar installation bill.",
                meterNumber = "KW-METN-4412",
                invoiceNumber = "INV-2026-0715",
                dueDate = "2026-08-08",
                generatorSubstation = "Metn Coastal Substation Zone 2",
                confidenceScore = 0.95,
                ocrRawText = "METN COASTAL GENERATORS\nPeriod: July 2026\nMeter: KW-METN-4412\nConsumption: 310 kWh\nRate: $0.390\nTotal Due: $137.90",
                isOfflineStored = true
            )
            val bill2Id = billDao.insertBill(bill2)

            val bill3 = ScannedBill(
                providerName = "Green Cedar Grid & Microgrid",
                billMonth = "June 2026",
                scanDate = System.currentTimeMillis() - 86400000L * 62,
                kwhConsumption = 195.0,
                amperage = 10,
                ratePerKwhUsd = 0.34,
                ratePerKwhLbp = 30430.0,
                officialBenchmarkRateUsd = 0.34,
                fixedFeeUsd = 10.0,
                fuelSurchargeUsd = 0.0,
                totalAmountUsd = 76.30,
                totalAmountLbp = 6830000.0,
                overchargeAmountUsd = 0.0,
                fairnessStatus = BillFairnessStatus.GREEN,
                aiAuditSummary = "FAIR & COMPLIANT: Bill matches official Ministry published tariff accurately.",
                isComplaintFiled = false,
                notes = "Excellent rate transparency.",
                meterNumber = "KW-GC-1090",
                invoiceNumber = "INV-2026-0610",
                dueDate = "2026-07-05",
                generatorSubstation = "Beirut Microgrid Substation 1",
                confidenceScore = 0.99,
                ocrRawText = "GREEN CEDAR GRID & MICROGRID\nPeriod: June 2026\nMeter: KW-GC-1090\nConsumption: 195 kWh\nRate: $0.340/kWh\nCompliant with Ministry Decree 41/2021",
                isOfflineStored = true
            )
            billDao.insertBill(bill3)

            // Seed initial complaint
            complaintDao.insertComplaint(
                MunicipalityComplaint(
                    billId = bill1Id,
                    caseNumber = "CW-2026-8842",
                    municipalityName = "Municipality of Beirut - Energy Compliance Dept",
                    providerName = "Achrafieh Electric Power Co.",
                    violationType = "Price Gouging (>25% official tariff limit)",
                    overchargeClaimUsd = 45.60,
                    filingDate = System.currentTimeMillis() - 86400000L * 2,
                    status = ComplaintStatus.MUNICIPALITY_INVESTIGATING,
                    complaintLetterText = """
                        TO: Municipality of Beirut - Department of Energy & Utility Oversight
                        FROM: Sami Taha (Achrafieh Sector)
                        RE: Formal Overcharge & Price-Gouging Complaint regarding August 2026 Generator Bill

                        Pursuant to Ministry of Energy and Water Decision No. 84/2026 establishing the official maximum tariff ceiling of $0.360/kWh for private diesel generator subscriptions:

                        1. VIOLATION DETAILS:
                           - Licensed Operator: Achrafieh Electric Power Co.
                           - Metered Consumption: 285.00 kWh (10 Amps)
                           - Billed Unit Rate: $0.520 / kWh (46,540 LBP/kWh)
                           - Ministry Legal Ceiling: $0.360 / kWh (32,220 LBP/kWh)
                           - Overcharge Rate Discrepancy: +44.4%
                           - Total Illegal Overcharge: $45.60 USD (approx. 4,081,200 LBP)

                        2. REQUESTED ADMINISTRATIVE ACTION:
                           - Compel the aforementioned provider to adjust the balance and issue a direct credit of $45.60 on the subsequent billing cycle.
                           - Dispatch a municipal inspector to calibrate the sub-station meter.

                        Generated automatically and certified via CedarWatt AI Energy Auditor.
                    """.trimIndent(),
                    trackingNotes = "Inspector assigned. Operator notified to present metering logs within 5 business days."
                )
            )

            // Seed initial saved complaint draft for offline access
            draftDao.insertDraft(
                ComplaintDraft(
                    billId = bill2Id,
                    billProviderName = "Metn Coastal Generators",
                    billMonth = "July 2026",
                    billKwh = 310.0,
                    billAmperage = 10,
                    billChargedRateUsd = 0.39,
                    billOfficialBenchmarkUsd = 0.35,
                    municipalityName = "Municipality of Metn - Jdeideh",
                    department = "Energy & Municipal Price Control Bureau",
                    complainantName = "Sami Taha",
                    complainantAddress = "Metn Coastal Sector",
                    complainantPhone = "+961 70 123 456",
                    violationType = "Unitemized Fuel Surcharge & Rate Markup",
                    discrepancyDescription = "Operator levied an extra $5.00 fuel surcharge not authorized by the official municipal gazette. Billed rate was $0.390/kWh vs legal cap of $0.350/kWh.",
                    claimAmountUsd = 12.40,
                    selectedTone = "Official Administrative Petition",
                    meterNumber = "KW-METN-4412",
                    requestedRemedies = "Mandatory credit of $12.40 on next cycle; Inspection of substation meter counter",
                    attachedEvidence = "Scanned Bill / Receipt Image, Ministry Benchmark Decree Reference (Res 41/2021)",
                    draftLetterText = """
                        FORMAL COMPLAINT DRAFT (OFFLINE ROOM DATABASE ARCHIVE)
                        To: Municipality of Metn - Jdeideh - Energy & Municipal Price Control Bureau
                        From: Sami Taha (Tel: +961 70 123 456)
                        Re: Draft Grievance for July 2026 Generator Bill Overcharge ($12.40 Claim)
                        Respondent Operator: Metn Coastal Generators (Meter: KW-METN-4412)
                        
                        This draft petition is stored offline in the local Room database. The resident may resume and file when ready.
                    """.trimIndent(),
                    isArabic = false,
                    lastSavedTimestamp = System.currentTimeMillis() - 86400000L
                )
            )

            // Seed official Ministry tariff benchmarks
            benchmarkDao.insertBenchmarks(
                listOf(
                    BenchmarkTariff(
                        monthName = "August 2026",
                        officialGeneratorKwhUsd = 0.360,
                        officialGeneratorKwhLbp = 32220.0,
                        edlGridOffPeakUsd = 0.100,
                        edlGridPeakUsd = 0.270,
                        dieselPrice20LUsd = 16.80,
                        fixed5AmpFeeUsd = 5.00,
                        fixed10AmpFeeUsd = 10.00,
                        fixed15AmpFeeUsd = 15.00,
                        publishedDate = System.currentTimeMillis() - 86400000L * 5,
                        circularReference = "Ministry Decision 84/2026 (Coastal & Mid-elevation)",
                        isCurrent = true
                    ),
                    BenchmarkTariff(
                        monthName = "July 2026",
                        officialGeneratorKwhUsd = 0.350,
                        officialGeneratorKwhLbp = 31325.0,
                        edlGridOffPeakUsd = 0.100,
                        edlGridPeakUsd = 0.270,
                        dieselPrice20LUsd = 16.40,
                        fixed5AmpFeeUsd = 5.00,
                        fixed10AmpFeeUsd = 10.00,
                        fixed15AmpFeeUsd = 15.00,
                        publishedDate = System.currentTimeMillis() - 86400000L * 35,
                        circularReference = "Ministry Decision 71/2026",
                        isCurrent = false
                    ),
                    BenchmarkTariff(
                        monthName = "June 2026",
                        officialGeneratorKwhUsd = 0.340,
                        officialGeneratorKwhLbp = 30430.0,
                        edlGridOffPeakUsd = 0.100,
                        edlGridPeakUsd = 0.270,
                        dieselPrice20LUsd = 15.90,
                        fixed5AmpFeeUsd = 4.50,
                        fixed10AmpFeeUsd = 9.00,
                        fixed15AmpFeeUsd = 14.00,
                        publishedDate = System.currentTimeMillis() - 86400000L * 65,
                        circularReference = "Ministry Decision 59/2026",
                        isCurrent = false
                    )
                )
            )

            // Seed realistic usage history records representing typical Lebanese 24-hour cycle
            val baseTime = System.currentTimeMillis()
            val usageRecords = listOf(
                EnergyUsageRecord(
                    timestamp = baseTime - 3600000L * 18,
                    dateString = "2026-09-06",
                    hourOfDay = 6,
                    primarySource = EnergySource.EDL_GRID,
                    kwhConsumed = 1.2,
                    solarGeneratedKwh = 0.1,
                    loadDemandKw = 1.3,
                    costUsd = 0.12,
                    costLbp = 10740.0,
                    isGridAvailable = true,
                    notes = "Early morning EDL grid supply slot"
                ),
                EnergyUsageRecord(
                    timestamp = baseTime - 3600000L * 14,
                    dateString = "2026-09-06",
                    hourOfDay = 10,
                    primarySource = EnergySource.SOLAR,
                    kwhConsumed = 2.4,
                    solarGeneratedKwh = 3.2,
                    loadDemandKw = 2.1,
                    costUsd = 0.0,
                    costLbp = 0.0,
                    isGridAvailable = false,
                    notes = "Solar PV covering total home load plus battery charging"
                ),
                EnergyUsageRecord(
                    timestamp = baseTime - 3600000L * 11,
                    dateString = "2026-09-06",
                    hourOfDay = 13,
                    primarySource = EnergySource.SOLAR,
                    kwhConsumed = 3.1,
                    solarGeneratedKwh = 3.5,
                    loadDemandKw = 2.8,
                    costUsd = 0.0,
                    costLbp = 0.0,
                    isGridAvailable = false,
                    notes = "Midday peak solar generation, water heater running"
                ),
                EnergyUsageRecord(
                    timestamp = baseTime - 3600000L * 7,
                    dateString = "2026-09-06",
                    hourOfDay = 17,
                    primarySource = EnergySource.BATTERY,
                    kwhConsumed = 1.8,
                    solarGeneratedKwh = 0.6,
                    loadDemandKw = 1.8,
                    costUsd = 0.0,
                    costLbp = 0.0,
                    isGridAvailable = false,
                    notes = "Late afternoon solar tapering, battery discharging smoothly"
                ),
                EnergyUsageRecord(
                    timestamp = baseTime - 3600000L * 4,
                    dateString = "2026-09-06",
                    hourOfDay = 20,
                    primarySource = EnergySource.DIESEL_GENERATOR,
                    kwhConsumed = 2.9,
                    solarGeneratedKwh = 0.0,
                    loadDemandKw = 2.9,
                    costUsd = 1.04,
                    costLbp = 93440.0,
                    isGridAvailable = false,
                    notes = "Evening EDL blackout; private neighborhood generator engaged"
                )
            )
            usageDao.insertAllUsage(usageRecords)

            // Seed initial chat history for instant offline access and quick retrieval
            val initialChatMessages = listOf(
                ChatMessageEntity(
                    id = "msg_seed_01",
                    isUser = false,
                    text = "👋 Welcome to CedarWatt Copilot! All your scanned energy bills, audits, and chat history are saved in the local Room Database on this device for instant offline access and quick retrieval.",
                    timestamp = System.currentTimeMillis() - 86400000L * 2,
                    roleUsed = ChatbotRole.GENERAL_ADVISOR,
                    modelUsed = ChatbotRole.GENERAL_ADVISOR.modelId
                ),
                ChatMessageEntity(
                    id = "msg_seed_02",
                    isUser = true,
                    text = "Why was my August 2026 bill from Achrafieh Electric Power Co. audited with a RED overcharge flag?",
                    timestamp = System.currentTimeMillis() - 86400000L * 2 + 3000L,
                    roleUsed = ChatbotRole.LEGAL_AUDITOR,
                    modelUsed = ChatbotRole.LEGAL_AUDITOR.modelId,
                    billContextId = bill1Id
                ),
                ChatMessageEntity(
                    id = "msg_seed_03",
                    isUser = false,
                    text = "⚖️ **Audit Breakdown for August 2026 (Achrafieh Electric Power Co.):**\n\n1. **Tariff Ceiling Breach**: Your provider billed at **$0.520/kWh** (46,540 LBP/kWh), whereas Ministry of Energy Decree 84/2026 fixed the August maximum rate at **$0.360/kWh**.\n2. **Unit Overcharge**: You were charged **+$0.160/kWh** (+44.4% above legal ceiling).\n3. **Amperage Fixed Fee**: You were billed $18.00 instead of the legal $10.00 allowance for a 10A subscription.\n4. **Total Illegal Overcharge**: **$45.60 USD** (~4,081,200 LBP).\n\nA formal municipal complaint petition has been drafted and submitted under Case #CW-2026-8842.",
                    timestamp = System.currentTimeMillis() - 86400000L * 2 + 5000L,
                    roleUsed = ChatbotRole.LEGAL_AUDITOR,
                    modelUsed = ChatbotRole.LEGAL_AUDITOR.modelId,
                    billContextId = bill1Id
                )
            )
            chatDao.insertMessages(initialChatMessages)
        }
    }
}
