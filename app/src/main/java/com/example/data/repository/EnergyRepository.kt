package com.example.data.repository

import com.example.data.local.BenchmarkTariff
import com.example.data.local.BillFairnessStatus
import com.example.data.local.ChatMessageDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ComplaintDraft
import com.example.data.local.ComplaintDraftDao
import com.example.data.local.EnergyBill
import com.example.data.local.EnergyBillDao
import com.example.data.local.EnergyUsageRecord
import com.example.data.local.MunicipalityComplaint
import com.example.data.local.MunicipalityComplaintDao
import com.example.data.local.TariffBenchmarkDao
import com.example.data.local.UsageHistoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repository interface defining data access contracts for EnergyBill,
 * MunicipalityComplaint, ComplaintDraft, BenchmarkTariff, EnergyUsageRecord, and ChatMessageEntity entities.
 */
interface EnergyRepository {
    val allBills: Flow<List<EnergyBill>>
    val latestBill: Flow<EnergyBill?>
    val overchargedBills: Flow<List<EnergyBill>>
    val allComplaints: Flow<List<MunicipalityComplaint>>
    val allDrafts: Flow<List<ComplaintDraft>>
    val allBenchmarks: Flow<List<BenchmarkTariff>>
    val currentBenchmark: Flow<BenchmarkTariff?>
    val allUsageHistory: Flow<List<EnergyUsageRecord>>
    val allChatMessages: Flow<List<ChatMessageEntity>>

    suspend fun getBillById(id: Long): EnergyBill?
    fun searchBills(query: String): Flow<List<EnergyBill>>
    fun getBillsByFairness(status: BillFairnessStatus): Flow<List<EnergyBill>>
    suspend fun insertBill(bill: EnergyBill): Long
    suspend fun updateBill(bill: EnergyBill)
    suspend fun deleteBill(bill: EnergyBill)
    suspend fun markComplaintFiled(billId: Long)

    suspend fun getComplaintById(id: Long): MunicipalityComplaint?
    suspend fun getComplaintForBill(billId: Long): MunicipalityComplaint?
    suspend fun insertComplaint(complaint: MunicipalityComplaint): Long
    suspend fun updateComplaint(complaint: MunicipalityComplaint)
    suspend fun deleteComplaint(complaint: MunicipalityComplaint)

    suspend fun getDraftById(id: Long): ComplaintDraft?
    fun getDraftForBill(billId: Long): Flow<ComplaintDraft?>
    suspend fun insertDraft(draft: ComplaintDraft): Long
    suspend fun updateDraft(draft: ComplaintDraft)
    suspend fun deleteDraft(draft: ComplaintDraft)
    suspend fun deleteDraftById(id: Long)

    suspend fun getBenchmarkById(id: Long): BenchmarkTariff?
    suspend fun getBenchmarkByMonth(monthName: String): BenchmarkTariff?
    suspend fun insertBenchmark(benchmark: BenchmarkTariff): Long
    suspend fun insertBenchmarks(benchmarks: List<BenchmarkTariff>)
    suspend fun updateBenchmark(benchmark: BenchmarkTariff)
    suspend fun deleteBenchmark(benchmark: BenchmarkTariff)

    fun getUsageByDate(date: String): Flow<List<EnergyUsageRecord>>
    fun getRecentUsage(limit: Int = 50): Flow<List<EnergyUsageRecord>>
    suspend fun insertUsage(record: EnergyUsageRecord): Long
    suspend fun insertAllUsage(records: List<EnergyUsageRecord>)
    suspend fun deleteUsage(record: EnergyUsageRecord)
    suspend fun clearUsageHistory()

    fun searchChatMessages(query: String): Flow<List<ChatMessageEntity>>
    suspend fun insertChatMessage(message: ChatMessageEntity): Long
    suspend fun insertChatMessages(messages: List<ChatMessageEntity>)
    suspend fun deleteChatMessage(id: String)
    suspend fun clearChatMessages()

    // User-scoped queries
    fun getBillsForUser(userId: String): Flow<List<EnergyBill>>
    fun getLatestBillForUser(userId: String): Flow<EnergyBill?>
    fun getComplaintsForUser(userId: String): Flow<List<MunicipalityComplaint>>
    fun getDraftsForUser(userId: String): Flow<List<ComplaintDraft>>
    fun getUsageHistoryForUser(userId: String): Flow<List<EnergyUsageRecord>>
    fun getChatMessagesForUser(userId: String): Flow<List<ChatMessageEntity>>
    suspend fun linkGuestDataToAccount(userId: String)
}

/**
 * Production implementation of [EnergyRepository] backed by Room DAOs.
 */
class EnergyRepositoryImpl(
    private val energyBillDao: EnergyBillDao,
    private val municipalityComplaintDao: MunicipalityComplaintDao,
    private val tariffBenchmarkDao: TariffBenchmarkDao? = null,
    private val usageHistoryDao: UsageHistoryDao? = null,
    private val chatMessageDao: ChatMessageDao? = null,
    private val complaintDraftDao: ComplaintDraftDao? = null
) : EnergyRepository {

    override val allBills: Flow<List<EnergyBill>> = energyBillDao.getAllBills()
    override val latestBill: Flow<EnergyBill?> = energyBillDao.getLatestBill()
    override val overchargedBills: Flow<List<EnergyBill>> = energyBillDao.getOverchargedBills()
    override val allComplaints: Flow<List<MunicipalityComplaint>> = municipalityComplaintDao.getAllComplaints()
    override val allDrafts: Flow<List<ComplaintDraft>> =
        complaintDraftDao?.getAllDrafts() ?: flowOf(emptyList())
    override val allBenchmarks: Flow<List<BenchmarkTariff>> =
        tariffBenchmarkDao?.getAllBenchmarks() ?: flowOf(emptyList())
    override val currentBenchmark: Flow<BenchmarkTariff?> =
        tariffBenchmarkDao?.getCurrentBenchmark() ?: flowOf(null)
    override val allUsageHistory: Flow<List<EnergyUsageRecord>> =
        usageHistoryDao?.getAllUsageHistory() ?: flowOf(emptyList())
    override val allChatMessages: Flow<List<ChatMessageEntity>> =
        chatMessageDao?.getAllMessages() ?: flowOf(emptyList())

    override suspend fun getBillById(id: Long): EnergyBill? = energyBillDao.getBillById(id)

    override fun searchBills(query: String): Flow<List<EnergyBill>> = energyBillDao.searchBills(query)

    override fun getBillsByFairness(status: BillFairnessStatus): Flow<List<EnergyBill>> =
        energyBillDao.getBillsByFairness(status)

    override suspend fun insertBill(bill: EnergyBill): Long = energyBillDao.insertBill(bill)

    override suspend fun updateBill(bill: EnergyBill) = energyBillDao.updateBill(bill)

    override suspend fun deleteBill(bill: EnergyBill) = energyBillDao.deleteBill(bill)

    override suspend fun markComplaintFiled(billId: Long) = energyBillDao.markComplaintFiled(billId)

    override suspend fun getComplaintById(id: Long): MunicipalityComplaint? =
        municipalityComplaintDao.getComplaintById(id)

    override suspend fun getComplaintForBill(billId: Long): MunicipalityComplaint? =
        municipalityComplaintDao.getComplaintForBill(billId)

    override suspend fun insertComplaint(complaint: MunicipalityComplaint): Long {
        val id = municipalityComplaintDao.insertComplaint(complaint)
        energyBillDao.markComplaintFiled(complaint.billId)
        return id
    }

    override suspend fun updateComplaint(complaint: MunicipalityComplaint) =
        municipalityComplaintDao.updateComplaint(complaint)

    override suspend fun deleteComplaint(complaint: MunicipalityComplaint) =
        municipalityComplaintDao.deleteComplaint(complaint)

    override suspend fun getDraftById(id: Long): ComplaintDraft? =
        complaintDraftDao?.getDraftById(id)

    override fun getDraftForBill(billId: Long): Flow<ComplaintDraft?> =
        complaintDraftDao?.getDraftForBill(billId) ?: flowOf(null)

    override suspend fun insertDraft(draft: ComplaintDraft): Long =
        complaintDraftDao?.insertDraft(draft) ?: 0L

    override suspend fun updateDraft(draft: ComplaintDraft) {
        complaintDraftDao?.updateDraft(draft)
    }

    override suspend fun deleteDraft(draft: ComplaintDraft) {
        complaintDraftDao?.deleteDraft(draft)
    }

    override suspend fun deleteDraftById(id: Long) {
        complaintDraftDao?.deleteDraftById(id)
    }

    override suspend fun getBenchmarkById(id: Long): BenchmarkTariff? =
        tariffBenchmarkDao?.getBenchmarkById(id)

    override suspend fun getBenchmarkByMonth(monthName: String): BenchmarkTariff? =
        tariffBenchmarkDao?.getBenchmarkByMonth(monthName)

    override suspend fun insertBenchmark(benchmark: BenchmarkTariff): Long =
        tariffBenchmarkDao?.insertBenchmark(benchmark) ?: 0L

    override suspend fun insertBenchmarks(benchmarks: List<BenchmarkTariff>) {
        tariffBenchmarkDao?.insertBenchmarks(benchmarks)
    }

    override suspend fun updateBenchmark(benchmark: BenchmarkTariff) {
        tariffBenchmarkDao?.updateBenchmark(benchmark)
    }

    override suspend fun deleteBenchmark(benchmark: BenchmarkTariff) {
        tariffBenchmarkDao?.deleteBenchmark(benchmark)
    }

    override fun getUsageByDate(date: String): Flow<List<EnergyUsageRecord>> =
        usageHistoryDao?.getUsageByDate(date) ?: flowOf(emptyList())

    override fun getRecentUsage(limit: Int): Flow<List<EnergyUsageRecord>> =
        usageHistoryDao?.getRecentUsage(limit) ?: flowOf(emptyList())

    override suspend fun insertUsage(record: EnergyUsageRecord): Long =
        usageHistoryDao?.insertUsage(record) ?: 0L

    override suspend fun insertAllUsage(records: List<EnergyUsageRecord>) {
        usageHistoryDao?.insertAllUsage(records)
    }

    override suspend fun deleteUsage(record: EnergyUsageRecord) {
        usageHistoryDao?.deleteUsage(record)
    }

    override suspend fun clearUsageHistory() {
        usageHistoryDao?.clearUsageHistory()
    }

    override fun searchChatMessages(query: String): Flow<List<ChatMessageEntity>> =
        chatMessageDao?.searchMessages(query) ?: flowOf(emptyList())

    override suspend fun insertChatMessage(message: ChatMessageEntity): Long =
        chatMessageDao?.insertMessage(message) ?: 0L

    override suspend fun insertChatMessages(messages: List<ChatMessageEntity>) {
        chatMessageDao?.insertMessages(messages)
    }

    override suspend fun deleteChatMessage(id: String) {
        chatMessageDao?.deleteMessageById(id)
    }

    override suspend fun clearChatMessages() {
        chatMessageDao?.clearAllMessages()
    }

    override fun getBillsForUser(userId: String): Flow<List<EnergyBill>> =
        energyBillDao.getAllBillsForUser(userId)

    override fun getLatestBillForUser(userId: String): Flow<EnergyBill?> =
        energyBillDao.getLatestBillForUser(userId)

    override fun getComplaintsForUser(userId: String): Flow<List<MunicipalityComplaint>> =
        municipalityComplaintDao.getAllComplaintsForUser(userId)

    override fun getDraftsForUser(userId: String): Flow<List<ComplaintDraft>> =
        complaintDraftDao?.getAllDraftsForUser(userId) ?: flowOf(emptyList())

    override fun getUsageHistoryForUser(userId: String): Flow<List<EnergyUsageRecord>> =
        usageHistoryDao?.getAllUsageHistoryForUser(userId) ?: flowOf(emptyList())

    override fun getChatMessagesForUser(userId: String): Flow<List<ChatMessageEntity>> =
        chatMessageDao?.getAllMessagesForUser(userId) ?: flowOf(emptyList())

    override suspend fun linkGuestDataToAccount(userId: String) {
        if (userId.isNotBlank() && userId != "guest_default") {
            energyBillDao.linkGuestBillsToUser(userId)
            municipalityComplaintDao.linkGuestComplaintsToUser(userId)
            complaintDraftDao?.linkGuestDraftsToUser(userId)
            usageHistoryDao?.linkGuestUsageToUser(userId)
            chatMessageDao?.linkGuestChatToUser(userId)
        }
    }
}

