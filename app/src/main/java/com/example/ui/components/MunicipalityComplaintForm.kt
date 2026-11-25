package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BillFairnessStatus
import com.example.data.local.ComplaintDraft
import com.example.data.local.ScannedBill
import com.example.data.local.UserProfile
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.CedarOrangePrimary
import com.example.ui.theme.LuxuryCardBorder
import com.example.ui.theme.LuxurySurfaceDark
import com.example.ui.theme.LuxurySurfaceElevated
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedContainer
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.util.Locale

/**
 * Reusable, comprehensive form component for generating structured energy billing
 * complaint letters addressed to Lebanese municipalities using validated bill data as context.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MunicipalityComplaintForm(
    bills: List<ScannedBill>,
    initialSelectedBill: ScannedBill?,
    userProfile: UserProfile,
    draft: ComplaintDraft? = null,
    onSaveDraft: ((
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
        draftId: Long
    ) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    onGenerateDocument: ((
        bill: ScannedBill,
        municipality: String,
        violationType: String,
        discrepancyDescription: String,
        claimAmountUsd: Double,
        isArabic: Boolean,
        complainantPhone: String,
        department: String,
        tone: String,
        requestedRemedies: List<String>,
        meterNumber: String
    ) -> String)? = null,
    onSubmitComplaint: (
        bill: ScannedBill,
        municipality: String,
        violationType: String,
        discrepancyDescription: String,
        claimAmountUsd: Double,
        customDocumentText: String
    ) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // 1. Bill Selection State
    var selectedBill by remember {
        mutableStateOf(
            if (draft != null) {
                bills.firstOrNull { it.id == draft.billId } ?: initialSelectedBill ?: bills.firstOrNull()
            } else {
                initialSelectedBill
                    ?: bills.firstOrNull { it.overchargeAmountUsd > 0 && !it.isComplaintFiled }
                    ?: bills.firstOrNull()
            }
        )
    }
    var billDropdownExpanded by remember { mutableStateOf(false) }

    // 2. Municipality & Jurisdiction State
    val lebaneseMunicipalities = listOf(
        "Municipality of Beirut (بلدية بيروت)",
        "Municipality of Tripoli (بلدية طرابلس)",
        "Municipality of Saida (بلدية صيدا)",
        "Municipality of Jounieh (بلدية جونية)",
        "Municipality of Zahlé (بلدية زحلة)",
        "Municipality of Byblos / Jbeil (بلدية جبيل)",
        "Municipality of Baabda (بلدية بعبدا)",
        "Municipality of Aley (بلدية عاليه)",
        "Municipality of Sin El Fil (بلدية سن الفيل)",
        "Municipality of Dekwaneh (بلدية الدكوانة)",
        "Municipality of Bourj Hammoud (بلدية برج حمود)",
        "Municipality of Choueifat (بلدية الشويفات)",
        "Municipality of Tyre (بلدية صور)",
        "Municipality of Nabatieh (بلدية النبطية)",
        "Municipality of Batroun (بلدية البترون)"
    )
    var municipalityName by remember {
        mutableStateOf(
            if (draft != null && draft.municipalityName.isNotBlank()) draft.municipalityName
            else userProfile.municipalityName.ifBlank { "Municipality of Beirut" }
        )
    }
    var municipalityDropdownExpanded by remember { mutableStateOf(false) }

    val departments = listOf(
        "Municipal Energy & Generator Oversight Bureau",
        "Consumer Protection & Municipal Police Directorate (الشرطة البلدية)",
        "Office of the Mayor & Municipal Council (ديوان رئاسة المجلس البلدي)",
        "Joint Municipal Arbitration & Complaints Committee"
    )
    var selectedDepartment by remember {
        mutableStateOf(
            if (draft != null && draft.department.isNotBlank()) draft.department
            else departments[0]
        )
    }
    var departmentDropdownExpanded by remember { mutableStateOf(false) }

    // Complainant info
    var complainantName by remember {
        mutableStateOf(
            if (draft != null && draft.complainantName.isNotBlank()) draft.complainantName
            else userProfile.fullName.ifBlank { "Sami Taha" }
        )
    }
    var complainantAddress by remember {
        mutableStateOf(
            if (draft != null && draft.complainantAddress.isNotBlank()) draft.complainantAddress
            else userProfile.location.ifBlank { "Achrafieh, Beirut" }
        )
    }
    var complainantPhone by remember {
        mutableStateOf(
            if (draft != null && draft.complainantPhone.isNotBlank()) draft.complainantPhone
            else "+961 03 123 456"
        )
    }
    var meterNumber by remember {
        mutableStateOf(
            if (draft != null && draft.meterNumber.isNotBlank()) draft.meterNumber
            else "KW-${(1000..9999).random()}"
        )
    }

    // 3. Violation Classification & Legal Escalation Tone
    val violationCategories = listOf(
        "Price Gouging (>25% official tariff limit - القرار الشهري لوزارة الطاقة)",
        "Refusal to Install / Read Energy Meter (Decree 41/2021 إلزامية تركيب العدادات)",
        "Illegal Fixed Amperage Fee (> legal ceiling رسوم مقطوعة غير مصرح بها)",
        "Unjustified Arbitrary Fuel Surcharge (فرض بدلات مازوت وهمية)",
        "Forced Flat Fee (مقطوعة) Without Meter",
        "Arbitrary & Inflated Currency Exchange Rate (التلاعب بسعر صرف الدولار)",
        "Threat of Service Cutoff upon Requesting Tariff (التهديد بقطع التيار)"
    )
    var violationType by remember {
        mutableStateOf(
            if (draft != null && draft.violationType.isNotBlank()) draft.violationType
            else violationCategories[0]
        )
    }
    var violationDropdownExpanded by remember { mutableStateOf(false) }

    val escalationTones = listOf(
        "Official Administrative Petition / عريضة تنظيمية رسمية",
        "Formal Legal Notice & Reclaim / إنذار رسمي واسترداد",
        "Amicable Reconciliation Notice / إخطار تسوية ودية"
    )
    var selectedTone by remember {
        mutableStateOf(
            if (draft != null && draft.selectedTone.isNotBlank()) draft.selectedTone
            else escalationTones[0]
        )
    }
    var toneDropdownExpanded by remember { mutableStateOf(false) }

    // 4. Billing Discrepancies Description & Quick Chips
    var discrepancyDescription by remember {
        mutableStateOf(
            if (draft != null && draft.discrepancyDescription.isNotBlank()) {
                draft.discrepancyDescription
            } else {
                selectedBill?.let {
                    if (it.overchargeAmountUsd > 0) {
                        "Provider billed $${String.format(Locale.US, "%.3f", it.ratePerKwhUsd)}/kWh exceeding the official Ministry ceiling ($${String.format(Locale.US, "%.3f", it.officialBenchmarkRateUsd)}/kWh) by +$${String.format(Locale.US, "%.2f", it.overchargeAmountUsd)} overcharge."
                    } else ""
                } ?: ""
            }
        )
    }

    // 5. Overcharge Claim Amount State
    var claimAmountText by remember {
        mutableStateOf(
            if (draft != null && draft.claimAmountUsd > 0) {
                String.format(Locale.US, "%.2f", draft.claimAmountUsd)
            } else {
                selectedBill?.let { String.format(Locale.US, "%.2f", it.overchargeAmountUsd) } ?: "0.00"
            }
        )
    }

    // 6. Demanded Remedies Multi-Select
    val availableRemedies = listOf(
        "Direct refund / credit of overcharge on upcoming invoice",
        "Immediate dispatch of municipal inspector for counter inspection",
        "Enforce Decree 41/2021 electronic counter installation",
        "Impose statutory municipal fines on violating operator"
    )
    val selectedRemedies = remember {
        mutableStateListOf(
            availableRemedies[0],
            availableRemedies[1],
            availableRemedies[2]
        )
    }

    // 7. Evidence Attachments Checklist
    val evidenceOptions = listOf(
        "Scanned Bill / Receipt Image",
        "Photo of Subscriber Breaker / Meter Counter",
        "Ministry Benchmark Decree Reference (Res 41/2021)"
    )
    val attachedEvidence = remember {
        mutableStateListOf(evidenceOptions[0], evidenceOptions[2])
    }

    // 8. Document Language & Generated Text State
    var isArabicDoc by remember {
        mutableStateOf(draft?.isArabic ?: (strings.appName.contains("AI") && strings.navDashboard == "الرئيسية"))
    }
    var generatedDocumentText by remember {
        mutableStateOf(draft?.draftLetterText?.ifBlank { "" } ?: "")
    }
    var isDocumentManuallyEdited by remember {
        mutableStateOf(draft != null && draft.draftLetterText.isNotBlank())
    }

    // Auto-generate document helper
    fun regenerateDoc() {
        val currentBill = selectedBill ?: return
        val claim = claimAmountText.toDoubleOrNull() ?: currentBill.overchargeAmountUsd

        generatedDocumentText = if (onGenerateDocument != null) {
            onGenerateDocument(
                currentBill,
                municipalityName,
                violationType,
                discrepancyDescription,
                claim,
                isArabicDoc,
                complainantPhone,
                selectedDepartment,
                selectedTone,
                selectedRemedies.toList(),
                meterNumber
            )
        } else {
            // Local fallback generator if external callback not provided
            val claimLbp = claim * 89500.0
            val pct = if (currentBill.officialBenchmarkRateUsd > 0) {
                (((currentBill.ratePerKwhUsd / currentBill.officialBenchmarkRateUsd) - 1.0) * 100).toInt()
            } else 0
            val certCode = "CW-${(1000..9999).random()}"

            if (isArabicDoc) {
                """
كتاب شكوى واعتراض رسمي - مخالفات تسعيرة المولدات الخاصة
إلى: جانب المجلس البلدي لبلدية $municipalityName المحترم
الدائرة المختصة: $selectedDepartment
نسخة إلى: مصلحة حماية المستهلك - وزارة الاقتصاد والتجارة
مقدّم الشكوى: $complainantName (العنوان: $complainantAddress | هاتف: $complainantPhone)

الموضوع: اعتراض رسمي ومطالبة باسترداد مبالغ غير مشروعة عن فاتورة شهر ${currentBill.billMonth}
الصفة الإدارية للملف: $selectedTone
المشكو بحقه: المشغّل / صاحب المولد (${currentBill.providerName})
رقم العداد / الاشتراك: $meterNumber

أولاً: بيانات الاشتراك والتدقيق الحسابي (بيانات معتمدة من تطبيق CedarWatt):
- قوة الاشتراك: ${currentBill.amperage} أمبير
- استهلاك العداد المسجل: ${String.format(Locale.US, "%.2f", currentBill.kwhConsumption)} كيلوواط ساعة (kWh)
- التسعيرة المطبقة من المشغل: $${String.format(Locale.US, "%.3f", currentBill.ratePerKwhUsd)} لكل ك.و.س
- السقف التوجيهي الرسمي لوزارة الطاقة والمياه: $${String.format(Locale.US, "%.3f", currentBill.officialBenchmarkRateUsd)} لكل ك.و.س
- نسبة الزيادة غير المشروعة: +$pct% فوق السقف المعتمد
- الرسوم الثابتة وبدلات الوقود الإضافية: $${String.format(Locale.US, "%.2f", currentBill.fixedFeeUsd + currentBill.fuelSurchargeUsd)} دولار
- إجمالي مبلغ الاسترداد المطالب به: $${String.format(Locale.US, "%.2f", claim)} دولار أميركي (≈ ${String.format(Locale.US, "%,.0f", claimLbp)} ل.ل)

ثانياً: تصنيف المخالفة القانونية الرئيسية:
- $violationType

ثالثاً: بيان المشترك وتفاصيل الفروقات المالية المشكو منها:
${discrepancyDescription.ifBlank { "فرض تسعيرة تفوق السقف الرسمي لوزارة الطاقة والمياه واحتساب مبالغ إضافية دون وجه حق." }}

رابعاً: السند القانوني والنصوص المرعية:
- قرارات وتعاميم وزارة الطاقة والمياه اللبنانية، ولا سيما القرار 41/2021 الملزم بتركيب عدادات الكيلوواط واعتماد التسعيرة الرسمية.
- قانون حماية المستهلك رقم 659/2005 وقرارات وزارة الاقتصاد لمكافحة الاحتكار والأسعار الجائرة.
- صلاحيات البلديات في الرقابة وضبط المخالفات بموجب قانون البلديات اللبناني (المرسوم الاشتراعي رقم 118/1977).

خامساً: المطالب والرجاء:
${selectedRemedies.mapIndexed { i, r -> "${i + 1}. $r" }.joinToString("\n")}

المرفقات المرفقة:
${attachedEvidence.map { "• $it" }.joinToString("\n")}

وتفضلوا بقبول فائق الاحترام والتقدير،
مقدّم الشكوى: $complainantName
رمز التوثيق الإلكتروني: CW-$certCode
                """.trimIndent()
            } else {
                """
FORMAL COMPLAINT - MUNICIPAL ENERGY REGULATORY VIOLATION
To: $municipalityName - $selectedDepartment
Cc: Ministry of Economy & Trade - Consumer Protection Directorate
From: $complainantName (Resident: $complainantAddress | Tel: $complainantPhone)

Subject: Formal Grievance & Refund Claim for Private Generator Billing Overcharges (${currentBill.billMonth})
Document Classification: $selectedTone
Respondent Operator: ${currentBill.providerName}
Subscription Meter ID: $meterNumber

1. SUBSCRIPTION & AUDIT METRICS (CedarWatt Certified Audit):
   - Subscribed Breaker: ${currentBill.amperage} Amperes
   - Metered Consumption: ${String.format(Locale.US, "%.2f", currentBill.kwhConsumption)} kWh
   - Billed Unit Rate: $${String.format(Locale.US, "%.3f", currentBill.ratePerKwhUsd)} USD / kWh
   - Official Ministry Benchmark: $${String.format(Locale.US, "%.3f", currentBill.officialBenchmarkRateUsd)} USD / kWh
   - Rate Discrepancy: +$pct% above legal ceiling
   - Fixed Surcharges & Fees: $${String.format(Locale.US, "%.2f", currentBill.fixedFeeUsd + currentBill.fuelSurchargeUsd)} USD
   - Total Overcharge Claimed: $${String.format(Locale.US, "%.2f", claim)} USD (≈ ${String.format(Locale.US, "%,.0f", claimLbp)} LBP)

2. PRIMARY VIOLATION CLASSIFICATION:
   - $violationType

3. CITIZEN STATEMENT OF BILLING DISCREPANCIES:
   ${discrepancyDescription.ifBlank { "The operator billed rates and surcharges significantly exceeding the official Ministry tariff ceiling." }}

4. STATUTORY & REGULATORY CITATIONS:
   - Ministry of Energy & Water Resolution No. 41/2021 (Mandatory Metering & Regulated kWh Tariff Ceilings).
   - Lebanese Consumer Protection Law No. 659/2005 (Prohibition of Price Manipulation & Coercive Fees).
   - Municipal Regulatory Authority under Decree-Law No. 118/1977.

5. FORMAL DEMANDS:
${selectedRemedies.mapIndexed { i, r -> "   ${i + 1}. $r" }.joinToString("\n")}

ATTACHED EVIDENCE:
${attachedEvidence.map { "   • $it" }.joinToString("\n")}

Respectfully submitted,
$complainantName
Digital Certification Reference: CW-$certCode
                """.trimIndent()
            }
        }
        isDocumentManuallyEdited = false
    }

    // Trigger initial generation
    LaunchedEffect(selectedBill, isArabicDoc, selectedTone, selectedDepartment) {
        if (!isDocumentManuallyEdited && selectedBill != null) {
            regenerateDoc()
        }
    }

    // Bill change handler
    fun onPickBill(bill: ScannedBill) {
        selectedBill = bill
        claimAmountText = String.format(Locale.US, "%.2f", bill.overchargeAmountUsd)
        if (discrepancyDescription.isBlank() || discrepancyDescription.startsWith("Provider billed")) {
            discrepancyDescription = "Provider billed $${String.format(Locale.US, "%.3f", bill.ratePerKwhUsd)}/kWh exceeding the official Ministry ceiling ($${String.format(Locale.US, "%.3f", bill.officialBenchmarkRateUsd)}/kWh) by +$${String.format(Locale.US, "%.2f", bill.overchargeAmountUsd)} overcharge."
        }
        regenerateDoc()
    }

    // Share via Android Intent
    fun shareDocument() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Formal Energy Complaint - $municipalityName")
            putExtra(Intent.EXTRA_TEXT, generatedDocumentText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Complaint Letter via"))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(StatusRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = StatusRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = strings.complaintFormTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = strings.complaintFormSubtitle,
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondaryDark
                    )
                }
            }
        }

        // Saved Draft Indicator Banner
        if (draft != null) {
            Surface(
                color = CedarOrangePrimary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CedarOrangePrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banner_editing_draft")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = CedarOrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.editingDraftBanner,
                            color = CedarOrangePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Draft ID #${draft.id} • ${strings.offlineDatabaseBadge}",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 1: VALIDATED BILL CONTEXT & FINANCIAL METRICS
        // =========================================================================
        LuxuryGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("complaint_section_bill_context"),
            cornerRadius = 18.dp,
            borderColor = CedarOrangePrimary.copy(alpha = 0.4f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = CedarOrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "1. ${strings.selectBillLabel} (Validated Bill Context)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CedarOrangePrimary
                        )
                    }

                    selectedBill?.let { bill ->
                        val (badgeBg, badgeColor, badgeText) = when (bill.fairnessStatus) {
                            BillFairnessStatus.RED -> Triple(StatusRed.copy(alpha = 0.25f), StatusRed, "OVERCHARGED")
                            BillFairnessStatus.AMBER -> Triple(StatusAmber.copy(alpha = 0.25f), StatusAmber, "BORDERLINE")
                            BillFairnessStatus.GREEN -> Triple(StatusGreen.copy(alpha = 0.25f), StatusGreen, "FAIR")
                        }
                        Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (bills.isEmpty()) {
                    Surface(
                        color = LuxurySurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = strings.noBillsAvailablePrompt,
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    // Bill Dropdown Selector
                    ExposedDropdownMenuBox(
                        expanded = billDropdownExpanded,
                        onExpandedChange = { billDropdownExpanded = !billDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedBill?.let {
                                "${it.providerName} • ${it.billMonth} ($${String.format(Locale.US, "%.2f", it.totalAmountUsd)})"
                            } ?: "Select an audited bill...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Audited Energy Bill") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = billDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                                .testTag("complaint_bill_selector"),
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = billDropdownExpanded,
                            onDismissRequest = { billDropdownExpanded = false }
                        ) {
                            bills.forEach { b ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${b.providerName} (${b.billMonth})",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                if (b.overchargeAmountUsd > 0) {
                                                    Text(
                                                        text = "+$${String.format(Locale.US, "%.2f", b.overchargeAmountUsd)}",
                                                        color = StatusRed,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${b.kwhConsumption} kWh • ${b.amperage}A • Charged: $${String.format(Locale.US, "%.3f", b.ratePerKwhUsd)}/kWh (Legal cap: $${String.format(Locale.US, "%.3f", b.officialBenchmarkRateUsd)})",
                                                fontSize = 11.sp,
                                                color = TextSecondaryDark
                                            )
                                        }
                                    },
                                    onClick = {
                                        onPickBill(b)
                                        billDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Validated Bill Context Summary Cards
                    selectedBill?.let { bill ->
                        val discrepancyPct = if (bill.officialBenchmarkRateUsd > 0) {
                            (((bill.ratePerKwhUsd / bill.officialBenchmarkRateUsd) - 1.0) * 100).toInt()
                        } else 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = LuxurySurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Billed Rate", fontSize = 10.sp, color = TextSecondaryDark)
                                    Text(
                                        text = "$${String.format(Locale.US, "%.3f", bill.ratePerKwhUsd)}/kWh",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (discrepancyPct > 0) StatusRed else StatusGreen
                                    )
                                    Text(
                                        text = if (discrepancyPct > 0) "+$discrepancyPct% excess" else "Within legal cap",
                                        fontSize = 10.sp,
                                        color = if (discrepancyPct > 0) StatusRed else StatusGreen
                                    )
                                }
                            }

                            Surface(
                                color = LuxurySurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Ministry Benchmark", fontSize = 10.sp, color = TextSecondaryDark)
                                    Text(
                                        text = "$${String.format(Locale.US, "%.3f", bill.officialBenchmarkRateUsd)}/kWh",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = "${bill.kwhConsumption} kWh • ${bill.amperage}A",
                                        fontSize = 10.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }

                            Surface(
                                color = StatusRedContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Validated Overcharge", fontSize = 10.sp, color = StatusRed)
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", bill.overchargeAmountUsd)} USD",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = StatusRed
                                    )
                                    Text(
                                        text = "≈ ${String.format(Locale.US, "%,.0f", bill.overchargeAmountUsd * 89500)} LBP",
                                        fontSize = 10.sp,
                                        color = StatusRed.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 2: JURISDICTION & COMPLAINANT INFORMATION
        // =========================================================================
        LuxuryGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("complaint_section_jurisdiction"),
            cornerRadius = 18.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = CedarOrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "2. Municipal Jurisdiction & Complainant Details",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CedarOrangePrimary
                    )
                }

                // Municipality Dropdown / Text Field
                ExposedDropdownMenuBox(
                    expanded = municipalityDropdownExpanded,
                    onExpandedChange = { municipalityDropdownExpanded = !municipalityDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = municipalityName,
                        onValueChange = {
                            municipalityName = it
                            regenerateDoc()
                        },
                        label = { Text("Designated Municipality / البلدية المختصة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = municipalityDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                            .fillMaxWidth()
                            .testTag("complaint_municipality_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = municipalityDropdownExpanded,
                        onDismissRequest = { municipalityDropdownExpanded = false }
                    ) {
                        lebaneseMunicipalities.forEach { muni ->
                            DropdownMenuItem(
                                text = { Text(muni, fontSize = 12.sp) },
                                onClick = {
                                    municipalityName = muni.substringBefore(" (")
                                    municipalityDropdownExpanded = false
                                    regenerateDoc()
                                }
                            )
                        }
                    }
                }

                // Department Selector
                ExposedDropdownMenuBox(
                    expanded = departmentDropdownExpanded,
                    onExpandedChange = { departmentDropdownExpanded = !departmentDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedDepartment,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Oversight Bureau / الدائرة المعنية") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = departmentDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = departmentDropdownExpanded,
                        onDismissRequest = { departmentDropdownExpanded = false }
                    ) {
                        departments.forEach { dept ->
                            DropdownMenuItem(
                                text = { Text(dept, fontSize = 12.sp) },
                                onClick = {
                                    selectedDepartment = dept
                                    departmentDropdownExpanded = false
                                    regenerateDoc()
                                }
                            )
                        }
                    }
                }

                // Complainant Details in 2 columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = complainantName,
                        onValueChange = {
                            complainantName = it
                            regenerateDoc()
                        },
                        label = { Text("Complainant Full Name") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors()
                    )

                    OutlinedTextField(
                        value = complainantPhone,
                        onValueChange = {
                            complainantPhone = it
                            regenerateDoc()
                        },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = complainantAddress,
                        onValueChange = {
                            complainantAddress = it
                            regenerateDoc()
                        },
                        label = { Text("Neighborhood / Address") },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors()
                    )

                    OutlinedTextField(
                        value = meterNumber,
                        onValueChange = {
                            meterNumber = it
                            regenerateDoc()
                        },
                        label = { Text("Meter / Sub ID") },
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors()
                    )
                }
            }
        }

        // =========================================================================
        // SECTION 3: STATUTORY VIOLATION & FACTUAL NARRATIVE
        // =========================================================================
        LuxuryGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("complaint_section_violation"),
            cornerRadius = 18.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "3. Infractions & Factual Narrative",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CedarOrangePrimary
                    )
                }

                // Primary Violation Dropdown
                ExposedDropdownMenuBox(
                    expanded = violationDropdownExpanded,
                    onExpandedChange = { violationDropdownExpanded = !violationDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = violationType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Primary Statutory Infraction") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = violationDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                            .testTag("complaint_violation_dropdown"),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = violationDropdownExpanded,
                        onDismissRequest = { violationDropdownExpanded = false }
                    ) {
                        violationCategories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item, fontSize = 12.sp) },
                                onClick = {
                                    violationType = item
                                    violationDropdownExpanded = false
                                    regenerateDoc()
                                }
                            )
                        }
                    }
                }

                // Escalation Tone Selector
                ExposedDropdownMenuBox(
                    expanded = toneDropdownExpanded,
                    onExpandedChange = { toneDropdownExpanded = !toneDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedTone,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Document Tone / Preamble Classification") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toneDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = toneDropdownExpanded,
                        onDismissRequest = { toneDropdownExpanded = false }
                    ) {
                        escalationTones.forEach { toneItem ->
                            DropdownMenuItem(
                                text = { Text(toneItem, fontSize = 12.sp) },
                                onClick = {
                                    selectedTone = toneItem
                                    toneDropdownExpanded = false
                                    regenerateDoc()
                                }
                            )
                        }
                    }
                }

                // Quick Discrepancy Chips
                Text(
                    text = "Quick Discrepancy Statements:",
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
                val quickTags = listOf(
                    strings.quickTagRefusedMeter,
                    strings.quickTagFlatFee,
                    strings.quickTagExceededTariff,
                    strings.quickTagInflatedExchange,
                    "Imposed unapproved fuel fee",
                    "Threatened disconnect"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickTags) { tag ->
                        Surface(
                            onClick = {
                                val current = discrepancyDescription.trim()
                                discrepancyDescription = if (current.isBlank()) tag else "$current; $tag"
                                regenerateDoc()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = LuxurySurfaceElevated,
                            border = BorderStroke(1.dp, LuxuryCardBorder)
                        ) {
                            Text(
                                text = "+ $tag",
                                fontSize = 11.sp,
                                color = TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Factual Statement TextArea
                OutlinedTextField(
                    value = discrepancyDescription,
                    onValueChange = {
                        discrepancyDescription = it
                        regenerateDoc()
                    },
                    label = { Text(strings.describeDiscrepanciesTitle) },
                    placeholder = { Text(strings.describeDiscrepanciesPlaceholder, fontSize = 12.sp, color = TextSecondaryDark) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("complaint_discrepancy_description"),
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = outlinedFieldColors()
                )

                // Overcharge Claim Amount (USD) with dynamic LBP conversion
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = claimAmountText,
                        onValueChange = {
                            claimAmountText = it
                            regenerateDoc()
                        },
                        label = { Text("Overcharge Claim ($ USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("complaint_claim_amount_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors()
                    )

                    val enteredClaim = claimAmountText.toDoubleOrNull() ?: 0.0
                    Surface(
                        color = LuxurySurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LuxuryCardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Equivalent In LBP", fontSize = 10.sp, color = TextSecondaryDark)
                            Text(
                                text = "${String.format(Locale.US, "%,.0f", enteredClaim * 89500)} LBP",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = StatusRed
                            )
                            Text("@ 89,500 LBP/USD", fontSize = 9.sp, color = TextSecondaryDark)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 4: DEMANDED RELIEF & EVIDENCE ATTACHMENTS
        // =========================================================================
        LuxuryGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "4. Demanded Relief & Evidence Dossier",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CedarOrangePrimary
                )

                Text(
                    text = "Requested Municipal Remedies:",
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
                availableRemedies.forEach { remedy ->
                    val isChecked = selectedRemedies.contains(remedy)
                    Surface(
                        onClick = {
                            if (isChecked) selectedRemedies.remove(remedy)
                            else selectedRemedies.add(remedy)
                            regenerateDoc()
                        },
                        color = if (isChecked) CedarOrangePrimary.copy(alpha = 0.15f) else LuxurySurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isChecked) CedarOrangePrimary else LuxuryCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (isChecked) CedarOrangePrimary else TextSecondaryDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = remedy,
                                fontSize = 11.sp,
                                color = if (isChecked) TextPrimaryDark else TextSecondaryDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Evidence Attachments Included in Letter:",
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    evidenceOptions.forEach { ev ->
                        val isAttached = attachedEvidence.contains(ev)
                        FilterChip(
                            selected = isAttached,
                            onClick = {
                                if (isAttached) attachedEvidence.remove(ev)
                                else attachedEvidence.add(ev)
                                regenerateDoc()
                            },
                            label = { Text(ev.take(18) + if (ev.length > 18) "..." else "", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CedarOrangePrimary.copy(alpha = 0.2f),
                                selectedLabelColor = CedarOrangePrimary
                            )
                        )
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 5: AUTO-GENERATED OFFICIAL COMPLAINT DOSSIER & ACTIONS
        // =========================================================================
        LuxuryGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("complaint_section_generated_document"),
            cornerRadius = 18.dp,
            borderColor = StatusRed.copy(alpha = 0.4f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = CedarOrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "5. ${strings.autoGenerateDocTitle}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CedarOrangePrimary
                            )
                        }
                        Text(
                            text = strings.autoGenerateDocSubtitle,
                            fontSize = 10.sp,
                            color = TextSecondaryDark
                        )
                    }

                    // Language switcher button
                    Surface(
                        onClick = {
                            isArabicDoc = !isArabicDoc
                            regenerateDoc()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = CedarOrangePrimary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, CedarOrangePrimary),
                        modifier = Modifier.testTag("btn_toggle_document_language")
                    ) {
                        Text(
                            text = if (isArabicDoc) "العربية" else "English",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CedarOrangePrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Document Container
                Surface(
                    color = Color(0xFF0D0F14),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LuxuryCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabicDoc) "كتاب الشكوى الرسمي (صيغة البلدية المعتمدة)" else "Official Municipal Regulatory Letter",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { regenerateDoc() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Regenerate",
                                        tint = CedarOrangePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(generatedDocumentText))
                                        Toast.makeText(context, strings.copiedToClipboard, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = TextSecondaryDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { shareDocument() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = CedarOrangePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Editable generated complaint text
                        OutlinedTextField(
                            value = generatedDocumentText,
                            onValueChange = {
                                generatedDocumentText = it
                                isDocumentManuallyEdited = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 320.dp)
                                .testTag("complaint_document_preview"),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            )
                        )
                    }
                }

                // Action Buttons Row (Save Draft, Share & Submit)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val billToUse = selectedBill
                            val claim = claimAmountText.toDoubleOrNull() ?: (billToUse?.overchargeAmountUsd ?: 0.0)
                            onSaveDraft?.invoke(
                                billToUse,
                                municipalityName,
                                selectedDepartment,
                                violationType,
                                discrepancyDescription,
                                claim,
                                selectedTone,
                                meterNumber,
                                selectedRemedies.joinToString("; "),
                                attachedEvidence.joinToString("; "),
                                generatedDocumentText,
                                isArabicDoc,
                                draft?.id ?: 0L
                            )
                            Toast.makeText(context, strings.draftSavedNotice, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_save_complaint_draft"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CedarOrangePrimary.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = CedarOrangePrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.saveDraftButton,
                            color = CedarOrangePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { shareDocument() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_share_complaint_letter"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CedarOrangePrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = CedarOrangePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = CedarOrangePrimary, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val billToUse = selectedBill
                            if (billToUse != null) {
                                val claim = claimAmountText.toDoubleOrNull() ?: billToUse.overchargeAmountUsd
                                onSubmitComplaint(
                                    billToUse,
                                    municipalityName,
                                    violationType,
                                    discrepancyDescription,
                                    claim,
                                    generatedDocumentText
                                )
                                Toast.makeText(context, "Complaint successfully lodged with $municipalityName!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Please select an energy bill first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("btn_submit_municipality_complaint"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedBill != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusRed,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.submitComplaintButton,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
