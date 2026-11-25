package com.example.ui.chat

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageDao
import com.example.data.local.ScannedBill
import com.example.data.local.toChatMessage
import com.example.data.local.toEntity
import com.example.data.remote.gemini.ChatMessage
import com.example.data.remote.gemini.ChatbotRole
import com.example.data.remote.gemini.GeminiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class GeminiChatViewModel(
    private val repository: GeminiRepository = GeminiRepository(),
    private val chatDao: ChatMessageDao? = null
) : ViewModel() {

    constructor(application: Application) : this(
        repository = GeminiRepository(),
        chatDao = AppDatabase.getDatabase(
            application,
            CoroutineScope(Dispatchers.IO)
        ).chatMessageDao()
    )

    private val _selectedRole = MutableStateFlow(ChatbotRole.GENERAL_ADVISOR)
    val selectedRole: StateFlow<ChatbotRole> = _selectedRole.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(createWelcomeMessage(ChatbotRole.GENERAL_ADVISOR)))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredMessages: StateFlow<List<ChatMessage>> = combine(_messages, _searchQuery) { msgList, query ->
        if (query.isBlank()) {
            msgList
        } else {
            msgList.filter { msg ->
                msg.text.contains(query, ignoreCase = true) ||
                (msg.roleUsed?.name?.contains(query, ignoreCase = true) == true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _messages.value
    )

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedBill = MutableStateFlow<ScannedBill?>(null)
    val selectedBill: StateFlow<ScannedBill?> = _selectedBill.asStateFlow()

    private val _errorNotice = MutableStateFlow<String?>(null)
    val errorNotice: StateFlow<String?> = _errorNotice.asStateFlow()

    private var liveContextProvider: (() -> String)? = null

    init {
        // Load persisted chat history from Room database for offline access & quick retrieval
        if (chatDao != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val stored = chatDao.getAllMessagesSnapshot()
                    if (stored.isNotEmpty()) {
                        _messages.value = stored.map { it.toChatMessage() }
                    } else {
                        // Persist initial welcome message into Room
                        chatDao.insertMessage(createWelcomeMessage(ChatbotRole.GENERAL_ADVISOR).toEntity())
                    }
                } catch (e: Exception) {
                    // Fallback to in-memory defaults
                }
            }
        }
    }

    fun setLiveContextProvider(provider: () -> String) {
        liveContextProvider = provider
    }

    fun selectBill(bill: ScannedBill?) {
        _selectedBill.value = bill
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun explainBill(bill: ScannedBill, isArabic: Boolean = false) {
        selectBill(bill)
        val prompt = if (isArabic) {
            "يرجى شرح فاتورة الكهرباء لشهر ${bill.billMonth} من ${bill.providerName} بالتفصيل (${bill.kwhConsumption.toInt()} ك.و.س بسعر ${String.format(Locale.US, "%.3f", bill.ratePerKwhUsd)}$، الإجمالي ${String.format(Locale.US, "%.2f", bill.totalAmountUsd)}$). قارنها مع السقف الرسمي لوزارة الطاقة، ووضّح الرسوم الثابتة، وأي مبالغ زائدة أو غير قانونية."
        } else {
            "Please explain my ${bill.billMonth} electricity bill from ${bill.providerName} (${bill.kwhConsumption.toInt()} kWh, $${String.format(Locale.US, "%.3f", bill.ratePerKwhUsd)}/kWh, total $${String.format(Locale.US, "%.2f", bill.totalAmountUsd)}). Compare it with the official Ministry ceiling, explain each line item, and detail any overcharge."
        }
        sendMessage(prompt)
    }

    fun askCostReductionAdvice(bill: ScannedBill? = null, isArabic: Boolean = false) {
        if (bill != null) selectBill(bill)
        val prompt = if (isArabic) {
            "كيف يمكنني تخفيض تكاليف الطاقة وخفض فاتورة المولد الكهربائي؟ أعطني خطة عملية واضحة لجدولة الأجهزة المنزلية الثقيلة، والاستفادة من الطاقة الشمسية والبطاريات، وضبط أمبير الاشتراك لتوفير المصاريف."
        } else {
            "How can I reduce my energy costs and lower my electricity bill? Provide a prioritized, actionable plan for heavy appliance scheduling, solar & battery optimization, and generator amperage management."
        }
        sendMessage(prompt)
    }

    fun selectRole(newRole: ChatbotRole) {
        if (_selectedRole.value != newRole) {
            _selectedRole.value = newRole
            // If current chat is just the initial welcome, replace it with new role's welcome
            if (_messages.value.size <= 1 && !_messages.value.any { it.isUser }) {
                val newWelcome = createWelcomeMessage(newRole)
                _messages.value = listOf(newWelcome)
                chatDao?.let { dao ->
                    viewModelScope.launch(Dispatchers.IO) {
                        dao.clearAllMessages()
                        dao.insertMessage(newWelcome.toEntity())
                    }
                }
            }
        }
    }

    fun updateInput(text: String) {
        _inputText.value = text
    }

    fun sendMessage(customPrompt: String? = null) {
        val prompt = (customPrompt ?: _inputText.value).trim()
        if (prompt.isBlank() || _isGenerating.value) return

        _inputText.value = ""
        _errorNotice.value = null

        val currentRole = _selectedRole.value
        val billId = _selectedBill.value?.id
        val userMessage = ChatMessage(
            isUser = true,
            text = prompt,
            roleUsed = currentRole,
            modelUsed = currentRole.modelId
        )

        val updatedList = _messages.value + userMessage
        _messages.value = updatedList
        _isGenerating.value = true

        // Persist user message to Room database for offline access & quick retrieval
        chatDao?.let { dao ->
            viewModelScope.launch(Dispatchers.IO) {
                dao.insertMessage(userMessage.toEntity(billContextId = billId))
            }
        }

        val liveContext = liveContextProvider?.invoke()

        viewModelScope.launch {
            val result = repository.sendMessage(
                role = currentRole,
                history = updatedList,
                userPrompt = prompt,
                liveContext = liveContext
            )

            result.onSuccess { responseText ->
                val botMessage = ChatMessage(
                    isUser = false,
                    text = responseText,
                    roleUsed = currentRole,
                    modelUsed = currentRole.modelId
                )
                _messages.value = _messages.value + botMessage
                _isGenerating.value = false

                // Persist bot message to Room database
                chatDao?.let { dao ->
                    launch(Dispatchers.IO) {
                        dao.insertMessage(botMessage.toEntity(billContextId = billId))
                    }
                }
            }.onFailure { exception ->
                // Check if we can generate a smart offline response using cached Room data
                val activeBill = _selectedBill.value
                val offlineResponse = generateOfflineHeuristicResponse(prompt, currentRole, activeBill, liveContext)

                val botMessage = if (offlineResponse != null) {
                    ChatMessage(
                        isUser = false,
                        text = offlineResponse,
                        roleUsed = currentRole,
                        modelUsed = "Room Local Offline Engine",
                        isError = false
                    )
                } else {
                    val errorMsg = exception.localizedMessage ?: "Failed to generate response."
                    _errorNotice.value = errorMsg
                    ChatMessage(
                        isUser = false,
                        text = "⚠️ Offline or network unavailable: $errorMsg\n\nAll past conversations and audited bills remain accessible offline via local Room Database.",
                        roleUsed = currentRole,
                        modelUsed = currentRole.modelId,
                        isError = true
                    )
                }

                _messages.value = _messages.value + botMessage
                _isGenerating.value = false

                // Persist response to Room database
                chatDao?.let { dao ->
                    launch(Dispatchers.IO) {
                        dao.insertMessage(botMessage.toEntity(billContextId = billId))
                    }
                }
            }
        }
    }

    private fun generateOfflineHeuristicResponse(
        prompt: String,
        role: ChatbotRole,
        activeBill: ScannedBill?,
        liveContext: String?
    ): String? {
        val lower = prompt.lowercase(Locale.ROOT)
        return when {
            activeBill != null && (lower.contains("bill") || lower.contains("audit") || lower.contains("explain") || lower.contains("فاتورة") || lower.contains("شرح")) -> {
                """
                📱 [Room Offline Local Cache]: Detailed Bill Audit for ${activeBill.billMonth} (${activeBill.providerName})

                • Consumption: ${activeBill.kwhConsumption.toInt()} kWh (${activeBill.amperage}A breaker)
                • Provider Unit Rate: $${String.format(Locale.US, "%.3f", activeBill.ratePerKwhUsd)}/kWh (${String.format(Locale.US, "%,.0f", activeBill.ratePerKwhLbp)} LBP)
                • Ministry Ceiling: $${String.format(Locale.US, "%.3f", activeBill.officialBenchmarkRateUsd)}/kWh
                • Fixed Fees: $${String.format(Locale.US, "%.2f", activeBill.fixedFeeUsd)} | Fuel Surcharge: $${String.format(Locale.US, "%.2f", activeBill.fuelSurchargeUsd)}
                • Total Charged: $${String.format(Locale.US, "%.2f", activeBill.totalAmountUsd)} (${String.format(Locale.US, "%,.0f", activeBill.totalAmountLbp)} LBP)
                • Fairness Status: ${activeBill.fairnessStatus.name} (Overcharge: $${String.format(Locale.US, "%.2f", activeBill.overchargeAmountUsd)} USD)

                Audit Summary: ${activeBill.aiAuditSummary}
                Note: Retrieved locally from Room Database. Reconnect to internet for interactive online AI queries.
                """.trimIndent()
            }
            lower.contains("solar") || lower.contains("battery") || lower.contains("شمس") || lower.contains("بطارية") -> {
                """
                ☀️ [Room Offline Advisory]: Solar & Battery Quick Schedule

                1. Midday Peak (11:00 AM - 2:30 PM): Run electric water heaters and washing cycles on direct solar generation.
                2. Late Afternoon (3:00 PM - 5:30 PM): Divert remaining solar output to top off battery reserves to 95%+.
                3. Generator Peak Hours (6:00 PM - 10:00 PM): Rely on battery inverter to avoid high generator unit rates ($0.360/kWh).
                4. Overnight: Set inverter low-voltage cutoff at 25% DOD for longevity.
                """.trimIndent()
            }
            lower.contains("law") || lower.contains("decree") || lower.contains("complaint") || lower.contains("قانون") || lower.contains("شكوى") -> {
                """
                ⚖️ [Room Offline Regulatory Database]: Lebanese Generator Mandates

                • Decree 41/2021: Mandatory installation of electronic kWh meters by generator operators at provider expense.
                • Monthly Tariff Ceiling: Fixed at end of each month by Ministry of Energy and Water based on global 20L diesel indices.
                • Municipal Oversight: Municipalities are authorized to seize non-compliant generator assets and enforce refund of illegal surcharges.
                """.trimIndent()
            }
            else -> null
        }
    }

    fun clearChat() {
        val welcome = createWelcomeMessage(_selectedRole.value)
        _messages.value = listOf(welcome)
        _errorNotice.value = null
        _inputText.value = ""
        _searchQuery.value = ""

        chatDao?.let { dao ->
            viewModelScope.launch(Dispatchers.IO) {
                dao.clearAllMessages()
                dao.insertMessage(welcome.toEntity())
            }
        }
    }

    fun deleteMessage(messageId: String) {
        _messages.value = _messages.value.filter { it.id != messageId }
        chatDao?.let { dao ->
            viewModelScope.launch(Dispatchers.IO) {
                dao.deleteMessageById(messageId)
            }
        }
    }

    fun retryLastMessage() {
        val lastUserMessage = _messages.value.findLast { it.isUser }
        if (lastUserMessage != null && !_isGenerating.value) {
            // Remove the last error response if present
            val trimmed = _messages.value.filter { !(it == _messages.value.lastOrNull() && it.isError) }
            _messages.value = trimmed
            sendMessage(lastUserMessage.text)
        }
    }

    private companion object {
        fun createWelcomeMessage(role: ChatbotRole): ChatMessage {
            val welcomeText = when (role) {
                ChatbotRole.GENERAL_ADVISOR ->
                    "👋 Welcome! I am your CedarWatt Energy Copilot. All your scanned bills, audits, and chat history are saved in the local Room Database on this device for offline access and quick retrieval. How can I assist you today?"

                ChatbotRole.LEGAL_AUDITOR ->
                    "⚖️ CedarWatt Legal & Regulatory Auditor active. I specialize in Lebanese Ministry of Energy (MEW) monthly tariff decrees, counter meter mandates (Decree 41/2021), and building official municipal overcharge complaint petitions. All audit records are preserved locally in Room Database."

                ChatbotRole.FAST_CALCULATOR ->
                    "⚡ Fast Estimator ready! Enter your kWh numbers, amperage (5A, 10A, 15A), or USD/LBP amounts for instant calculations and quick tariff verification."
            }

            return ChatMessage(
                isUser = false,
                text = welcomeText,
                roleUsed = role,
                modelUsed = role.modelId
            )
        }
    }
}
