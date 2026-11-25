package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.remote.gemini.ChatMessage
import com.example.data.remote.gemini.ChatbotRole

enum class BillFairnessStatus {
    GREEN,   // Fair or below benchmark rate
    AMBER,   // Borderline / Moderate surcharge (10-25% over benchmark)
    RED      // Illegal / Overcharged (>25% over official benchmark)
}

enum class ComplaintStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    MUNICIPALITY_INVESTIGATING,
    REFUND_ISSUED,
    RESOLVED
}

enum class EnergySource {
    SOLAR,
    EDL_GRID,
    DIESEL_GENERATOR,
    BATTERY
}

enum class TipCategory {
    SOLAR_OPTIMIZATION,
    APPLIANCE_TIMING,
    GENERATOR_CUTOFF,
    TARIFF_SWITCH
}

@Entity(
    tableName = "energy_bills",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["scanDate"]),
        Index(value = ["providerName"]),
        Index(value = ["fairnessStatus"]),
        Index(value = ["billMonth"]),
        Index(value = ["meterNumber"]),
        Index(value = ["invoiceNumber"])
    ]
)
data class EnergyBill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "guest_default",
    val providerName: String,
    val billMonth: String,
    val scanDate: Long = System.currentTimeMillis(),
    val kwhConsumption: Double,
    val amperage: Int,
    val ratePerKwhUsd: Double,
    val ratePerKwhLbp: Double,
    val officialBenchmarkRateUsd: Double,
    val fixedFeeUsd: Double,
    val fuelSurchargeUsd: Double,
    val totalAmountUsd: Double,
    val totalAmountLbp: Double,
    val overchargeAmountUsd: Double,
    val fairnessStatus: BillFairnessStatus,
    val aiAuditSummary: String,
    val isComplaintFiled: Boolean = false,
    val notes: String = "",
    // Scanned Bill Metadata stored in Room Database for offline access
    val meterNumber: String = "KW-8842-BEIRUT",
    val invoiceNumber: String = "INV-2026-0881",
    val dueDate: String = "2026-09-10",
    val scannedImageUri: String? = null,
    val ocrRawText: String = "",
    val confidenceScore: Double = 0.96,
    val generatorSubstation: String = "Achrafieh Sub-Station 4B",
    val isOfflineStored: Boolean = true
)

typealias ScannedBill = EnergyBill

@Entity(
    tableName = "complaint_drafts",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["lastSavedTimestamp"]),
        Index(value = ["billId"])
    ]
)
data class ComplaintDraft(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "guest_default",
    val billId: Long? = null,
    val billProviderName: String = "",
    val billMonth: String = "",
    val billKwh: Double = 0.0,
    val billAmperage: Int = 10,
    val billChargedRateUsd: Double = 0.0,
    val billOfficialBenchmarkUsd: Double = 0.0,
    val municipalityName: String = "Municipality of Beirut",
    val department: String = "Municipal Energy & Generator Oversight Bureau",
    val complainantName: String = "Sami Taha",
    val complainantAddress: String = "Achrafieh, Beirut",
    val complainantPhone: String = "",
    val violationType: String = "Price Gouging (>25% official tariff limit)",
    val discrepancyDescription: String = "",
    val claimAmountUsd: Double = 0.0,
    val selectedTone: String = "Official Administrative Petition",
    val meterNumber: String = "",
    val requestedRemedies: String = "",
    val attachedEvidence: String = "",
    val draftLetterText: String = "",
    val isArabic: Boolean = false,
    val lastSavedTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "municipality_complaints",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["filingDate"])
    ]
)
data class MunicipalityComplaint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "guest_default",
    val billId: Long,
    val caseNumber: String,
    val municipalityName: String,
    val providerName: String,
    val violationType: String,
    val overchargeClaimUsd: Double,
    val filingDate: Long = System.currentTimeMillis(),
    val status: ComplaintStatus = ComplaintStatus.SUBMITTED,
    val complaintLetterText: String,
    val trackingNotes: String = "Complaint lodged with municipal energy oversight bureau."
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val userId: String = "guest_default",
    val fullName: String = "Sami Taha",
    val location: String = "Achrafieh, Beirut",
    val municipalityName: String = "Municipality of Beirut",
    val subscribedAmps: Int = 10,
    val hasSolar: Boolean = true,
    val solarCapacityKwp: Double = 3.5,
    val hasBattery: Boolean = true,
    val batteryCapacityKwh: Double = 5.0,
    val currencyPreference: String = "USD"
)


data class EnergyTimelineSlot(
    val hour: Int,
    val timeLabel: String,
    val primarySource: EnergySource,
    val gridAvailable: Boolean,
    val solarOutputKw: Double,
    val loadDemandKw: Double,
    val costRatePerKwhUsd: Double,
    val recommendedAction: String
)

data class EnergySavingTip(
    val id: String,
    val title: String,
    val description: String,
    val estimatedMonthlySavingsUsd: Double,
    val category: TipCategory,
    val difficulty: String,
    val impactScore: Int
)

@Entity(tableName = "tariff_benchmarks")
data class BenchmarkTariff(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val monthName: String,
    val officialGeneratorKwhUsd: Double,
    val officialGeneratorKwhLbp: Double,
    val edlGridOffPeakUsd: Double,
    val edlGridPeakUsd: Double,
    val dieselPrice20LUsd: Double,
    val fixed5AmpFeeUsd: Double,
    val fixed10AmpFeeUsd: Double,
    val fixed15AmpFeeUsd: Double,
    val publishedDate: Long = System.currentTimeMillis(),
    val circularReference: String = "Resolution No. 41/2021",
    val isCurrent: Boolean = false
)

typealias TariffBenchmark = BenchmarkTariff

@Entity(
    tableName = "usage_history",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["timestamp"]),
        Index(value = ["dateString"]),
        Index(value = ["primarySource"])
    ]
)
data class EnergyUsageRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "guest_default",
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String,
    val hourOfDay: Int = 12,
    val primarySource: EnergySource = EnergySource.DIESEL_GENERATOR,
    val kwhConsumed: Double,
    val solarGeneratedKwh: Double = 0.0,
    val loadDemandKw: Double = 0.0,
    val costUsd: Double = 0.0,
    val costLbp: Double = 0.0,
    val isGridAvailable: Boolean = false,
    val billId: Long? = null,
    val notes: String = "",
    val peakDemandKw: Double = 0.0,
    val generatorRunMinutes: Int = 0,
    val isOfflineRecord: Boolean = true
)

typealias UsageHistoryRecord = EnergyUsageRecord

@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["timestamp"]),
        Index(value = ["roleUsed"]),
        Index(value = ["billContextId"])
    ]
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String = "guest_default",
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val roleUsed: ChatbotRole? = null,
    val modelUsed: String? = null,
    val isError: Boolean = false,
    val billContextId: Long? = null,
    val sessionTag: String = "general"
)

fun ChatMessageEntity.toChatMessage(): ChatMessage = ChatMessage(
    id = id,
    isUser = isUser,
    text = text,
    timestamp = timestamp,
    roleUsed = roleUsed,
    modelUsed = modelUsed,
    isError = isError
)

fun ChatMessage.toEntity(
    billContextId: Long? = null,
    sessionTag: String = "general",
    userId: String = "guest_default"
): ChatMessageEntity = ChatMessageEntity(
    id = id,
    userId = userId,
    isUser = isUser,
    text = text,
    timestamp = timestamp,
    roleUsed = roleUsed,
    modelUsed = modelUsed,
    isError = isError,
    billContextId = billContextId,
    sessionTag = sessionTag
)


class DatabaseConverters {
    @TypeConverter
    fun fromFairnessStatus(value: BillFairnessStatus): String = value.name

    @TypeConverter
    fun toFairnessStatus(value: String): BillFairnessStatus =
        try { BillFairnessStatus.valueOf(value) } catch (e: Exception) { BillFairnessStatus.GREEN }

    @TypeConverter
    fun fromComplaintStatus(value: ComplaintStatus): String = value.name

    @TypeConverter
    fun toComplaintStatus(value: String): ComplaintStatus =
        try { ComplaintStatus.valueOf(value) } catch (e: Exception) { ComplaintStatus.SUBMITTED }

    @TypeConverter
    fun fromEnergySource(value: EnergySource): String = value.name

    @TypeConverter
    fun toEnergySource(value: String): EnergySource =
        try { EnergySource.valueOf(value) } catch (e: Exception) { EnergySource.DIESEL_GENERATOR }

    @TypeConverter
    fun fromChatbotRole(value: ChatbotRole?): String? = value?.name

    @TypeConverter
    fun toChatbotRole(value: String?): ChatbotRole? =
        value?.let { try { ChatbotRole.valueOf(it) } catch (e: Exception) { null } }
}

data class MonthlyEnergyTrendPoint(
    val monthLabel: String,
    val fullMonthName: String,
    val kwhConsumption: Double,
    val billedRateUsd: Double,
    val benchmarkRateUsd: Double,
    val edlGridRateUsd: Double = 0.10,
    val totalBilledUsd: Double,
    val fairCostUsd: Double,
    val overchargeUsd: Double,
    val variancePct: Int,
    val providerName: String,
    val amperage: Int,
    val isFromScannedBill: Boolean = true,
    val associatedBillId: Long? = null
)
