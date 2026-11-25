package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.remote.gemini.ChatbotRole
import kotlinx.coroutines.flow.Flow

@Dao
interface EnergyBillDao {
    @Query("SELECT * FROM energy_bills ORDER BY scanDate DESC")
    fun getAllBills(): Flow<List<EnergyBill>>

    @Query("SELECT * FROM energy_bills WHERE userId = :userId ORDER BY scanDate DESC")
    fun getAllBillsForUser(userId: String): Flow<List<EnergyBill>>

    @Query("SELECT * FROM energy_bills WHERE id = :id")
    suspend fun getBillById(id: Long): EnergyBill?

    @Query("SELECT * FROM energy_bills ORDER BY scanDate DESC LIMIT 1")
    fun getLatestBill(): Flow<EnergyBill?>

    @Query("SELECT * FROM energy_bills WHERE userId = :userId ORDER BY scanDate DESC LIMIT 1")
    fun getLatestBillForUser(userId: String): Flow<EnergyBill?>

    @Query("SELECT * FROM energy_bills WHERE fairnessStatus = :status ORDER BY scanDate DESC")
    fun getBillsByFairness(status: BillFairnessStatus): Flow<List<EnergyBill>>

    @Query("SELECT * FROM energy_bills WHERE userId = :userId AND fairnessStatus = :status ORDER BY scanDate DESC")
    fun getBillsByFairnessForUser(userId: String, status: BillFairnessStatus): Flow<List<EnergyBill>>

    @Query("SELECT * FROM energy_bills WHERE providerName LIKE '%' || :query || '%' OR billMonth LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY scanDate DESC")
    fun searchBills(query: String): Flow<List<EnergyBill>>

    @Query("SELECT * FROM energy_bills WHERE userId = :userId AND (providerName LIKE '%' || :query || '%' OR billMonth LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%') ORDER BY scanDate DESC")
    fun searchBillsForUser(userId: String, query: String): Flow<List<EnergyBill>>

    @Query("SELECT * FROM energy_bills WHERE overchargeAmountUsd > 0 ORDER BY overchargeAmountUsd DESC")
    fun getOverchargedBills(): Flow<List<EnergyBill>>

    @Query("SELECT * FROM energy_bills WHERE userId = :userId AND overchargeAmountUsd > 0 ORDER BY overchargeAmountUsd DESC")
    fun getOverchargedBillsForUser(userId: String): Flow<List<EnergyBill>>

    @Query("SELECT COUNT(*) FROM energy_bills")
    fun getBillCount(): Flow<Int>

    @Query("SELECT SUM(overchargeAmountUsd) FROM energy_bills")
    fun getTotalOverchargesUsd(): Flow<Double?>

    @Query("SELECT * FROM energy_bills WHERE meterNumber = :meterNumber ORDER BY scanDate DESC")
    fun getBillsByMeter(meterNumber: String): Flow<List<EnergyBill>>

    @Query("SELECT * FROM energy_bills WHERE isOfflineStored = 1 ORDER BY scanDate DESC")
    fun getOfflineStoredBills(): Flow<List<EnergyBill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: EnergyBill): Long

    @Update
    suspend fun updateBill(bill: EnergyBill)

    @Delete
    suspend fun deleteBill(bill: EnergyBill)

    @Query("UPDATE energy_bills SET isComplaintFiled = 1 WHERE id = :billId")
    suspend fun markComplaintFiled(billId: Long)

    @Query("UPDATE energy_bills SET userId = :newUserId WHERE userId = 'guest_default'")
    suspend fun linkGuestBillsToUser(newUserId: String)
}

typealias BillDao = EnergyBillDao

@Dao
interface MunicipalityComplaintDao {
    @Query("SELECT * FROM municipality_complaints ORDER BY filingDate DESC")
    fun getAllComplaints(): Flow<List<MunicipalityComplaint>>

    @Query("SELECT * FROM municipality_complaints WHERE userId = :userId ORDER BY filingDate DESC")
    fun getAllComplaintsForUser(userId: String): Flow<List<MunicipalityComplaint>>

    @Query("SELECT * FROM municipality_complaints WHERE id = :id")
    suspend fun getComplaintById(id: Long): MunicipalityComplaint?

    @Query("SELECT * FROM municipality_complaints WHERE billId = :billId LIMIT 1")
    suspend fun getComplaintForBill(billId: Long): MunicipalityComplaint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: MunicipalityComplaint): Long

    @Update
    suspend fun updateComplaint(complaint: MunicipalityComplaint)

    @Delete
    suspend fun deleteComplaint(complaint: MunicipalityComplaint)

    @Query("UPDATE municipality_complaints SET userId = :newUserId WHERE userId = 'guest_default'")
    suspend fun linkGuestComplaintsToUser(newUserId: String)
}

typealias ComplaintDao = MunicipalityComplaintDao

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE userId = :userId LIMIT 1")
    fun getUserProfile(userId: String): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getDefaultProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)
}


@Dao
interface TariffBenchmarkDao {
    @Query("SELECT * FROM tariff_benchmarks ORDER BY publishedDate DESC")
    fun getAllBenchmarks(): Flow<List<BenchmarkTariff>>

    @Query("SELECT * FROM tariff_benchmarks WHERE isCurrent = 1 LIMIT 1")
    fun getCurrentBenchmark(): Flow<BenchmarkTariff?>

    @Query("SELECT * FROM tariff_benchmarks WHERE id = :id")
    suspend fun getBenchmarkById(id: Long): BenchmarkTariff?

    @Query("SELECT * FROM tariff_benchmarks WHERE monthName = :monthName LIMIT 1")
    suspend fun getBenchmarkByMonth(monthName: String): BenchmarkTariff?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmark(benchmark: BenchmarkTariff): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmarks(benchmarks: List<BenchmarkTariff>)

    @Update
    suspend fun updateBenchmark(benchmark: BenchmarkTariff)

    @Delete
    suspend fun deleteBenchmark(benchmark: BenchmarkTariff)
}

typealias BenchmarkDao = TariffBenchmarkDao

@Dao
interface UsageHistoryDao {
    @Query("SELECT * FROM usage_history ORDER BY timestamp DESC")
    fun getAllUsageHistory(): Flow<List<EnergyUsageRecord>>

    @Query("SELECT * FROM usage_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllUsageHistoryForUser(userId: String): Flow<List<EnergyUsageRecord>>

    @Query("SELECT * FROM usage_history WHERE dateString = :date ORDER BY hourOfDay ASC")
    fun getUsageByDate(date: String): Flow<List<EnergyUsageRecord>>

    @Query("SELECT * FROM usage_history WHERE userId = :userId AND dateString = :date ORDER BY hourOfDay ASC")
    fun getUsageByDateForUser(userId: String, date: String): Flow<List<EnergyUsageRecord>>

    @Query("SELECT * FROM usage_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentUsage(limit: Int = 50): Flow<List<EnergyUsageRecord>>

    @Query("SELECT * FROM usage_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentUsageForUser(userId: String, limit: Int = 50): Flow<List<EnergyUsageRecord>>

    @Query("SELECT * FROM usage_history WHERE billId = :billId ORDER BY timestamp ASC")
    fun getUsageForBill(billId: Long): Flow<List<EnergyUsageRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(record: EnergyUsageRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUsage(records: List<EnergyUsageRecord>)

    @Update
    suspend fun updateUsage(record: EnergyUsageRecord)

    @Delete
    suspend fun deleteUsage(record: EnergyUsageRecord)

    @Query("DELETE FROM usage_history")
    suspend fun clearUsageHistory()

    @Query("SELECT SUM(kwhConsumed) FROM usage_history WHERE primarySource = :source")
    fun getTotalKwhBySource(source: EnergySource): Flow<Double?>

    @Query("SELECT SUM(kwhConsumed) FROM usage_history")
    fun getTotalKwhConsumed(): Flow<Double?>

    @Query("SELECT SUM(solarGeneratedKwh) FROM usage_history")
    fun getTotalSolarGenerated(): Flow<Double?>

    @Query("SELECT AVG(kwhConsumed) FROM usage_history")
    fun getAverageDailyKwh(): Flow<Double?>

    @Query("UPDATE usage_history SET userId = :newUserId WHERE userId = 'guest_default'")
    suspend fun linkGuestUsageToUser(newUserId: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY timestamp ASC")
    fun getAllMessagesForUser(userId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesSnapshot(): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE roleUsed = :role ORDER BY timestamp ASC")
    fun getMessagesByRole(role: ChatbotRole): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE text LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(query: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE userId = :userId AND text LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessagesForUser(userId: String, query: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE text LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun searchMessagesSnapshot(query: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE billContextId = :billId ORDER BY timestamp ASC")
    fun getMessagesForBill(billId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    suspend fun clearAllMessagesForUser(userId: String)

    @Query("SELECT COUNT(*) FROM chat_messages")
    fun getMessageCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getMessageCountSnapshot(): Int

    @Query("UPDATE chat_messages SET userId = :newUserId WHERE userId = 'guest_default'")
    suspend fun linkGuestChatToUser(newUserId: String)
}

typealias ChatDao = ChatMessageDao

@Dao
interface ComplaintDraftDao {
    @Query("SELECT * FROM complaint_drafts ORDER BY lastSavedTimestamp DESC")
    fun getAllDrafts(): Flow<List<ComplaintDraft>>

    @Query("SELECT * FROM complaint_drafts WHERE userId = :userId ORDER BY lastSavedTimestamp DESC")
    fun getAllDraftsForUser(userId: String): Flow<List<ComplaintDraft>>

    @Query("SELECT * FROM complaint_drafts WHERE id = :id")
    suspend fun getDraftById(id: Long): ComplaintDraft?

    @Query("SELECT * FROM complaint_drafts WHERE billId = :billId LIMIT 1")
    fun getDraftForBill(billId: Long): Flow<ComplaintDraft?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: ComplaintDraft): Long

    @Update
    suspend fun updateDraft(draft: ComplaintDraft)

    @Delete
    suspend fun deleteDraft(draft: ComplaintDraft)

    @Query("DELETE FROM complaint_drafts WHERE id = :id")
    suspend fun deleteDraftById(id: Long)

    @Query("SELECT COUNT(*) FROM complaint_drafts")
    fun getDraftCount(): Flow<Int>

    @Query("UPDATE complaint_drafts SET userId = :newUserId WHERE userId = 'guest_default'")
    suspend fun linkGuestDraftsToUser(newUserId: String)
}


typealias DraftDao = ComplaintDraftDao
