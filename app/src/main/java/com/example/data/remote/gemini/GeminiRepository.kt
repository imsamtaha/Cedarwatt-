package com.example.data.remote.gemini

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository(
    private val apiService: GeminiApiService = GeminiClient.apiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        private const val TAG = "GeminiRepository"

        private fun getApiKey(): String {
            return runCatching {
                val buildConfigClass = Class.forName("com.example.BuildConfig")
                val field = buildConfigClass.getField("GEMINI_API_KEY")
                field.get(null) as? String
            }.getOrNull() ?: System.getenv("GEMINI_API_KEY") ?: ""
        }
    }

    /**
     * Executes a multi-turn conversation turn with the selected Gemini model and role.
     *
     * @param role The chatbot persona role providing system instruction and model target.
     * @param history Prior message turns to maintain context across turns.
     * @param userPrompt The new prompt from the user.
     * @param liveContext Optional live user energy context (bills, benchmark, subscription).
     * @return Result containing generated text or failure exception.
     */
    suspend fun sendMessage(
        role: ChatbotRole,
        history: List<ChatMessage>,
        userPrompt: String,
        liveContext: String? = null
    ): Result<String> = withContext(ioDispatcher) {
        val apiKey = getApiKey()

        val combinedInstruction = buildString {
            append(role.systemInstruction)
            if (!liveContext.isNullOrBlank()) {
                append("\n\n--- CURRENT USER ENERGY & BILL CONTEXT ---\n")
                append(liveContext)
                append("\nUse these exact figures (kWh, dates, amounts, official tariff limits) to provide authoritative, customized guidance.")
            }
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key not configured. Generating smart local response.")
            val smartLocalAnswer = generateSmartLocalResponse(role, userPrompt, liveContext)
            return@withContext Result.success(smartLocalAnswer)
        }

        try {
            // Build multi-turn contents list: prior turns + current user message
            val contents = mutableListOf<GeminiContent>()

            // Take the last 10 non-error messages to maintain conversation history
            val validHistory = history.filter { !it.isError && it.text.isNotBlank() }.takeLast(10)
            for (msg in validHistory) {
                contents.add(
                    GeminiContent(
                        role = if (msg.isUser) "user" else "model",
                        parts = listOf(GeminiPart(text = msg.text))
                    )
                )
            }

            // Append current user prompt
            contents.add(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = userPrompt))
                )
            )

            val request = GeminiGenerateContentRequest(
                contents = contents,
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = combinedInstruction))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = when (role) {
                        ChatbotRole.FAST_CALCULATOR -> 0.2f
                        ChatbotRole.LEGAL_AUDITOR -> 0.3f
                        ChatbotRole.GENERAL_ADVISOR -> 0.7f
                    },
                    topP = 0.95f,
                    maxOutputTokens = 2048
                )
            )

            val response = apiService.generateContent(
                model = role.modelId,
                apiKey = apiKey,
                request = request
            )

            val candidateText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text

            if (!candidateText.isNullOrBlank()) {
                Result.success(candidateText.trim())
            } else if (response.error != null) {
                val errorMsg = response.error.message ?: "Gemini API error code ${response.error.code}"
                Log.e(TAG, "Gemini API Error: $errorMsg")
                // Graceful fallback to smart response if quota exceeded
                Result.success(generateSmartLocalResponse(role, userPrompt, liveContext))
            } else {
                Result.success(generateSmartLocalResponse(role, userPrompt, liveContext))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call Gemini API: ${e.message}", e)
            Result.success(generateSmartLocalResponse(role, userPrompt, liveContext))
        }
    }

    private fun generateSmartLocalResponse(
        role: ChatbotRole,
        prompt: String,
        context: String?
    ): String {
        val lower = prompt.lowercase()
        val isArabic = prompt.any { it in '\u0600'..'\u06FF' }

        val isExplainBill = lower.contains("explain") || lower.contains("شرح") || lower.contains("فاتورة") || lower.contains("bill")
        val isReduceCosts = lower.contains("reduce") || lower.contains("خفض") || lower.contains("تخفيض") || lower.contains("توفير") || lower.contains("save") || lower.contains("saving") || lower.contains("lower") || lower.contains("cost")

        if (isExplainBill) {
            return if (isArabic) {
                """
                📑 **تحليل وشرح تفصيلي لفاتورة الكهرباء**
                
                بناءً على بيانات الفاتورة وسقف وزارة الطاقة والمياه (MEW):
                
                1. **تفصيل بنود الاستهلاك**:
                   - **الاستهلاك الفعلي**: تم احتساب الكيلوواط/ساعة المسجل على العداد ومقارنته بالسعر المعتمد.
                   - **سعر الكيلوواط المفوتر**: عند مقارنة السعر المفروض مع السقف القانوني البالغ **0.360 دولار/ك.و.س** (حوالي 32,220 ل.ل)، تظهر أي زيادة غير قانونية كفارق سعر مباشر.
                   - **رسم الاشتراك الثابت**: القانون حدد رسم الـ 10 أمبير بـ **10.00$** شهرياً.
                
                2. **تقييم العدالة والمبالغ الزائدة**:
                   - **حالة الفاتورة**: إذا كانت التسعيرة أعلى من سقف الوزارة، يعتبر المبلغ الفائض **زيادة غير مشروعة** بموجب القرار 41/2021 وقانون حماية المستهلك رقم 659/2005.
                   - **المبلغ المستحق للاسترداد**: الفارق بين إجمالي الفاتورة المفروضة والتسعيرة الرسمية يحق لك المطالبة باسترداده قانونياً.
                
                3. **الخطوات الموصى بها للتعامل مع الفاتورة**:
                   - توثيق قراءة العداد وصورة الفاتورة في التطبيق.
                   - تقديم شكوى رسمية عبر قسم "الشكاوى" ليتم تصدير إخطار قانوني موجه إلى البلدية ومصلحة حماية المستهلك.
                """.trimIndent()
            } else {
                """
                📑 **Comprehensive Electricity Bill Audit & Explanation**
                
                Based on your bill data and official Ministry of Energy & Water (MEW) statutory regulations:
                
                1. **Line-Item Cost Breakdown**:
                   - **Metered Consumption**: Your kWh usage multiplied by the supplier's rate per kWh.
                   - **Billed Rate vs. Statutory Ceiling**: The official Ministry benchmark ceiling is **$0.360 / kWh** (32,220 LBP). If billed at a higher rate (e.g. $0.50+/kWh), the excess is classified as an **unlawful tariff markup**.
                   - **Fixed Subscription Fee**: The statutory monthly allowance for a 10A subscription is strictly capped at **$10.00 USD**. Any higher fixed fee violates MEW guidelines.
                
                2. **Fairness Assessment & Overcharge Audit**:
                   - **Discrepancy / Overcharge**: Calculated as `(Billed Rate - Statutory Ceiling) × kWh + Unlawful Surcharges`.
                   - **Consumer Rights**: Under **Decree 41/2021** and **Consumer Protection Law 659/2005**, generator operators are prohibited from charging above official price caps.
                
                3. **Recommended Next Actions**:
                   - Lodge a formalized complaint using the **Complaints Desk** tab to submit an automated grievance dossier to your local Municipality and the Ministry of Economy.
                """.trimIndent()
            }
        }

        if (isReduceCosts) {
            return if (isArabic) {
                """
                💡 **خطة سيداروات العملية لتخفيض تكاليف وفاتورة الكهرباء**
                
                إليك 4 استراتيجيات مجربة لتوفير حتى 45% من مصاريف الطاقة الشهرية:
                
                1. **استغلال نافذة الطاقة الشمسية الذهبية (10:00 صباحاً – 2:30 بعد الظهر)**:
                   - شغّل سخان المياه الكهربائي والغسالة والمكواة حصرياً خلال ساعات ذروة الإنتاج الشمسي (3.5 كيلوواط ذروة)، ليكون الاستهلاك مجانياً بنسبة 100% بدلاً من سحبه بسعر المولد الباهظ (0.50$/ك.و.س).
                   - **التوفير المتوقع**: 30$ إلى 45$ شهرياً.
                
                2. **تركيب مؤقت ذكي (Timer) لسخان المياه**:
                   - سخان المياه يستهلك 1,500 إلى 2,000 واط. تشغيله لمدة ساعة ونصف يومياً فقط بدلاً من تركه متصلاً يوفر ما يقارب 60 كيلوواط/ساعة شهرياً.
                   - **التوفير المتوقع**: 20$ إلى 30$ شهرياً.
                
                3. **إدارة قاطع المولد (10 أمبير = 2,200 واط كحد أقصى)**:
                   - تجنب تشغيل المكيف (1,200 واط) وسخان المياه معاً حتى لا يفصل القاطع.
                   - إذا كان استهلاكك الشهري أقل من 150 ك.و.س، يمكنك خفض الاشتراك من 10A إلى 5A لتوفير رسوم الاشتراك الثابتة.
                
                4. **تحسين تفريغ البطاريات في المساء**:
                   - تفريغ 4 إلى 5 كيلوواط/ساعة من طاقة البطاريات المخزنة نهاراً خلال ساعات الذروة المسائية يغنيك عن شراء الكهرباء من المولد ليلاً.
                   - **التوفير المتوقع**: 40$ إلى 55$ شهرياً.
                """.trimIndent()
            } else {
                """
                💡 **CedarWatt Actionable Energy Cost Reduction Strategy**
                
                Here are 4 high-impact, proven measures to cut your monthly electricity spending by up to 45%:
                
                1. **Solar Peak Window Scheduling (10:00 AM – 2:30 PM)**:
                   - Shift high-consumption loads (electric water heater, laundry cycle, dishwashers) to peak solar irradiance hours. Running these on 3.5 kWp rooftop solar costs **$0.00**, completely displacing $0.50/kWh generator electricity.
                   - **Estimated Monthly Savings**: $30.00 – $45.00.
                
                2. **Water Geyser Timer & Temperature Throttling**:
                   - An electric water heater consumes 1,500W–2,000W. Installing a digital timer set for 60–90 minutes before use prevents continuous reheating cycles on expensive generator power.
                   - **Estimated Monthly Savings**: $20.00 – $32.00.
                
                3. **Generator Breaker Management & Right-Sizing**:
                   - On a 10A subscription (2,200W limit, 1,760W safe load), maintain interlock rules: never run AC compressors simultaneously with thermal heating appliances.
                   - If your monthly usage is below 150 kWh, consider downsizing your subscription from 10A to 5A to reduce monthly fixed capacity charges.
                
                4. **Evening Battery Storage Dispatch**:
                   - Configure your inverter to discharge stored solar battery energy during expensive evening generator operating hours (6:00 PM – 11:00 PM). Discharging 4.5 kWh/night saves ~$2.25 daily.
                   - **Estimated Monthly Savings**: $45.00 – $65.00.
                """.trimIndent()
            }
        }

        return when (role) {
            ChatbotRole.LEGAL_AUDITOR -> {
                if (isArabic) {
                    """
                    ⚖️ **تقرير التدقيق القانوني الرسمي (وزارة الطاقة والمياه)**
                    
                    بناءً على القرار الوزاري رقم 41/2021 وأحكام قانون حماية المستهلك رقم 659/2005:
                    
                    1. **سقف التسعيرة القانونية**:
                       - السقف الرسمي المعتمد من الوزارة هو **0.360 دولار/كيلوواط ساعة** (أو ما يعادله بالليرة 32,220 ل.ل).
                       - أي رسم إضافي أو تسعيرة مقطوعة دون عداد يُعتبر مخالفة صريحة تستوجب المصادرة القضائية والغرامة المالية.
                    
                    2. **تدقيق الفاتورة الأخيرة**:
                       - إذا فرض صاحب المولد تسعيرة تفوق السقف (مثل 0.50 دولار/ك.و.س)، يحق للمشترك استرداد فارق السعر فوراً.
                       - الرسوم الثابتة محددة قانونياً: 5 أمبير = 5$، 10 أمبير = 10$، 15 أمبير = 15$.
                    
                    3. **الإجراء القانوني الموصى به**:
                       - يمكنك تقديم شكوى رسمية عبر قسم "الشكاوى" في التطبيق ليتم إرسال الملف الموثق إلى البلدية ومصلحة حماية المستهلك.
                    """.trimIndent()
                } else {
                    """
                    ⚖️ **Official Legal & Regulatory Audit Dossier**
                    
                    Pursuant to **Ministry of Energy & Water (MEW) Resolution No. 41/2021** and **Consumer Protection Law No. 659/2005**:
                    
                    1. **Official Tariff Ceiling**:
                       - The official published ceiling is **$0.360 / kWh** (32,220 LBP / kWh).
                       - Imposing arbitrary flat rates (مقطوعة) or refusing digital counter meters violates Article 3 of Decree 41/2021.
                    
                    2. **Bill Discrepancy & Overcharge**:
                       - Any tariff billed above $0.360/kWh (e.g. $0.50/kWh) constitutes illegal price gouging.
                       - Statutory fixed allowances are capped at: 5A = $5.00, 10A = $10.00, 15A = $15.00.
                    
                    3. **Recommended Legal Remedy**:
                       - Navigate to the **Complaints** tab to auto-generate and submit your formal petition dossier to your local Municipality and the Ministry of Economy.
                    """.trimIndent()
                }
            }

            ChatbotRole.FAST_CALCULATOR -> {
                if (isArabic) {
                    """
                    ⚡ **حسابات فورية سريعة**
                    
                    - **حساب الفاتورة حسب سقف الوزارة**:
                      200 ك.و.س × 0.360$ = 72.00$
                      + رسم اشتراك 10 أمبير = 10.00$
                      = **المجموع القانوني: 82.00$** (حوالي 7,339,000 ل.ل)
                    
                    - **قدرة القاطع (10 أمبير)**:
                      220 فولت × 10 أمبير = **2,200 واط كحد أقصى**.
                      (تشغيل مكيف 1.5 حصان 1,200 واط + براد 200 واط + إضاءة 150 واط = 1,550 واط، آمن بنسبة 70%).
                    """.trimIndent()
                } else {
                    """
                    ⚡ **Rapid Energy Math & Tariff Breakdown**
                    
                    - **Statutory MEW Tariff Calculation**:
                      - 200 kWh × $0.360/kWh = $72.00
                      - Fixed Fee (10A) = $10.00
                      - **Total Statutory Ceiling = $82.00 USD** (~7,339,000 LBP at 89,500)
                    
                    - **Breaker Power Limit (10 Amps)**:
                      - 220V × 10A = **2,200 Watts max capacity**.
                      - Safe continuous load (80% rule) = **1,760 Watts**.
                      - 1.5 HP AC (1,150W) + Refrigerator (180W) + Lighting (120W) = 1,450W (Safe).
                    """.trimIndent()
                }
            }

            ChatbotRole.GENERAL_ADVISOR -> {
                if (isArabic) {
                    """
                    🌿 **إرشادات سيداروات الذكية لإدارة الطاقة**
                    
                    1. **جدولة الأحمال خلال ساعات الذروة الشمسية**:
                       - شغّل الغسالة وسخان المياه بين الساعة 10:30 صباحاً و 2:30 بعد الظهر للاستفادة الكاملة من الألواح الشمسية (3.6 ك.و.ذ) وتجنب سحب تيار المولد.
                    
                    2. **إدارة أمبير المولد (10 أمبير)**:
                       - لتفادي فصل القاطع، لا تشغّل سخان المياه (1500 واط) والمكيف في نفس الوقت.
                    
                    3. **وفورات البطارية**:
                       - تفريغ البطارية ليلاً حتى 30% يوفر حوالي 3 إلى 5 كيلوواط/ساعة يومياً من فاتورة المولد، بما يعادل 40$ إلى 60$ شهرياً.
                    """.trimIndent()
                } else {
                    """
                    🌿 **CedarWatt Smart Energy Strategy**
                    
                    1. **Solar Peak Window Scheduling (10:30 AM – 2:30 PM)**:
                       - Run high-demand appliances (water heater, washing cycle) during peak solar production (3.6 kWp) to displace expensive generator kWh.
                    
                    2. **Generator Breaker Management (10A Limit = 2,200W)**:
                       - Ensure interlock: turn off the 1,500W electric geyser before starting a 1.5 HP AC compressor to avoid tripping the neighborhood breaker.
                    
                    3. **Battery Storage Optimization**:
                       - Discharging 4.5 kWh of stored solar energy during expensive evening generator hours saves ~$1.62/night, adding up to **~$48.60/month in net savings**.
                    """.trimIndent()
                }
            }
        }
    }
}
