package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ComplaintDraft
import com.example.data.local.ScannedBill
import com.example.data.local.UserProfile
import com.example.ui.theme.LuxuryCardBorder
import com.example.ui.theme.LuxurySurfaceDark

@Composable
fun MunicipalityComplaintFormDialog(
    bills: List<ScannedBill>,
    initialSelectedBill: ScannedBill?,
    userProfile: UserProfile,
    onDismiss: () -> Unit,
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
    onGenerateDocument: (
        bill: ScannedBill,
        municipality: String,
        violationType: String,
        discrepancyDescription: String,
        claimAmountUsd: Double,
        isArabic: Boolean
    ) -> String,
    onSubmitComplaint: (
        bill: ScannedBill,
        municipality: String,
        violationType: String,
        discrepancyDescription: String,
        claimAmountUsd: Double,
        customDocumentText: String
    ) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .imePadding()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, LuxuryCardBorder, RoundedCornerShape(24.dp))
                .testTag("municipality_complaint_form_dialog"),
            color = LuxurySurfaceDark
        ) {
            MunicipalityComplaintForm(
                bills = bills,
                initialSelectedBill = initialSelectedBill,
                userProfile = userProfile,
                draft = draft,
                onSaveDraft = onSaveDraft,
                onDismiss = onDismiss,
                onGenerateDocument = { bill, mun, violation, desc, claim, isAr, phone, dept, tone, remedies, meter ->
                    onGenerateDocument(bill, mun, violation, desc, claim, isAr)
                },
                onSubmitComplaint = onSubmitComplaint
            )
        }
    }
}
