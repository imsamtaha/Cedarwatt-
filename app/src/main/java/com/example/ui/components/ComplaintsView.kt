package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ComplaintDraft
import com.example.data.local.ComplaintStatus
import com.example.data.local.MunicipalityComplaint
import com.example.data.local.ScannedBill
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.CedarOrangePrimary
import com.example.ui.theme.LuxuryCardBorder
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ComplaintsView(
    complaints: List<MunicipalityComplaint>,
    bills: List<ScannedBill>,
    drafts: List<ComplaintDraft> = emptyList(),
    onSelectComplaint: (MunicipalityComplaint) -> Unit,
    onStartNewComplaint: (ScannedBill) -> Unit,
    onResumeDraft: (ComplaintDraft) -> Unit = {},
    onDeleteDraft: (ComplaintDraft) -> Unit = {},
    onOpenComplaintForm: () -> Unit = {},
    onNavigateToScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()
    val totalClaimedUsd = complaints.sumOf { it.overchargeClaimUsd }
    val unfiledOverchargedBills = bills.filter { it.overchargeAmountUsd > 0 && !it.isComplaintFiled }
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Filed Complaints, 1: Saved Drafts (Room DB)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = strings.complaintsDeskTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = strings.complaintsDeskSubtitle,
                    fontSize = 13.sp,
                    color = TextSecondaryDark
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenComplaintForm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_new_complaint_form")
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.newComplaintButton,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = StatusRedContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = StatusRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${complaints.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusRed
                        )
                    }
                }
            }
        }

        // Active Protection & Claimed Summary Card
        LuxuryGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("complaints_summary_card"),
            cornerRadius = 24.dp,
            borderColor = StatusRed.copy(alpha = 0.4f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.totalClaimedRecovery,
                        color = TextSecondaryDark,
                        fontSize = 13.sp
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "LEGAL RECOVERY",
                            color = StatusRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", totalClaimedUsd)} USD",
                            color = StatusRed,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "≈ ${String.format(Locale.US, "%,.0f", totalClaimedUsd * 89500)} LBP",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${complaints.count { it.status == ComplaintStatus.REFUND_ISSUED || it.status == ComplaintStatus.RESOLVED }} Settled",
                            color = StatusGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${complaints.count { it.status != ComplaintStatus.REFUND_ISSUED && it.status != ComplaintStatus.RESOLVED }} In Review",
                            color = SunsetCoral,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Unfiled Eligible Bills Banner
        if (unfiledOverchargedBills.isNotEmpty()) {
            LuxuryGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = StatusAmberContainer,
                borderColor = StatusAmber,
                cornerRadius = 20.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${strings.unfiledAlertTitle} (${unfiledOverchargedBills.size})",
                        color = StatusAmber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.unfiledAlertSubtitle,
                        color = TextPrimaryDark,
                        fontSize = 12.sp
                    )

                    unfiledOverchargedBills.take(2).forEach { bill ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${bill.providerName} (+$${String.format(Locale.US, "%.2f", bill.overchargeAmountUsd)})",
                                color = TextPrimaryDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = { onStartNewComplaint(bill) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StatusRed,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(strings.fileClaimButton, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Sub-Tabs Bar: Filed Complaints vs. Saved Drafts (Room DB)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                label = {
                    Text(
                        text = "${strings.tabLodgedComplaints} (${complaints.size})",
                        fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusRed,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                    containerColor = LuxurySurfaceElevated,
                    labelColor = TextSecondaryDark
                ),
                border = BorderStroke(1.dp, if (selectedSubTab == 0) StatusRed else LuxuryCardBorder),
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_filed_complaints")
            )

            FilterChip(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                label = {
                    Text(
                        text = "${strings.tabSavedDrafts} (${drafts.size})",
                        fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Drafts,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CedarOrangePrimary,
                    selectedLabelColor = Color.Black,
                    selectedLeadingIconColor = Color.Black,
                    containerColor = LuxurySurfaceElevated,
                    labelColor = TextSecondaryDark
                ),
                border = BorderStroke(1.dp, if (selectedSubTab == 1) CedarOrangePrimary else LuxuryCardBorder),
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_saved_drafts")
            )
        }

        if (selectedSubTab == 0) {
            // Filed Complaints Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (drafts.isNotEmpty()) {
                    Surface(
                        color = CedarOrangePrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CedarOrangePrimary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSubTab = 1 }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = CedarOrangePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${drafts.size} Saved Complaint Draft(s)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = "Stored in local Room database for offline editing",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = CedarOrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (complaints.isEmpty()) {
                    LuxuryGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(strings.noClaimsPending, color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
                            Text(strings.complaintsDeskSubtitle, color = TextSecondaryDark, fontSize = 12.sp)
                        }
                    }
                } else {
                    complaints.forEach { complaint ->
                        ComplaintCard(
                            complaint = complaint,
                            onClick = { onSelectComplaint(complaint) }
                        )
                    }
                }
            }
        } else {
            // Saved Drafts (Room Database) Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.tabSavedDrafts,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Surface(
                        color = CedarOrangePrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CedarOrangePrimary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = CedarOrangePrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.offlineDatabaseBadge,
                                color = CedarOrangePrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (drafts.isEmpty()) {
                    LuxuryGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Drafts,
                                contentDescription = null,
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Saved Complaint Drafts",
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start a complaint from any bill and tap 'Save as Draft' to preserve your unfiled legal petitions offline.",
                                color = TextSecondaryDark,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.fillMaxWidth(0.9f)
                            )
                        }
                    }
                } else {
                    drafts.forEach { draft ->
                        ComplaintDraftCard(
                            draft = draft,
                            onResume = { onResumeDraft(draft) },
                            onDelete = { onDeleteDraft(draft) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComplaintDraftCard(
    draft: ComplaintDraft,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val savedDateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US).format(Date(draft.lastSavedTimestamp))

    LuxuryGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("complaint_draft_card_${draft.id}"),
        cornerRadius = 20.dp,
        borderColor = CedarOrangePrimary.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = draft.municipalityName.ifBlank { "Municipality Complaint" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CedarOrangePrimary
                    )
                    Text(
                        text = if (draft.billProviderName.isNotBlank()) "${draft.billProviderName} • ${draft.billMonth}" else "Standalone Draft",
                        fontSize = 12.sp,
                        color = TextPrimaryDark
                    )
                }

                Surface(
                    color = CedarOrangePrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "DRAFT",
                        color = CedarOrangePrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Violation and Claim Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = draft.violationType.take(35) + if (draft.violationType.length > 35) "..." else "",
                    fontSize = 12.sp,
                    color = SunsetCoral,
                    fontWeight = FontWeight.Medium
                )
                if (draft.claimAmountUsd > 0.0) {
                    Text(
                        text = "Claim: $${String.format(Locale.US, "%.2f", draft.claimAmountUsd)} USD",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusRed
                    )
                }
            }

            // Letter Snippet
            if (draft.draftLetterText.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = draft.draftLetterText.take(140) + if (draft.draftLetterText.length > 140) "..." else "",
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        lineHeight = 15.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Saved Date & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved: $savedDateStr",
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Draft",
                            tint = StatusRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CedarOrangePrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("btn_resume_draft_${draft.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.resumeDraftButton,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComplaintCard(
    complaint: MunicipalityComplaint,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val (statusColor, statusBg, statusTitle) = when (complaint.status) {
        ComplaintStatus.DRAFT -> Triple(StatusAmber, StatusAmberContainer, strings.tabSavedDrafts)
        ComplaintStatus.SUBMITTED -> Triple(SunsetCoral, Color(0x26FF6B35), strings.statusSubmitted)
        ComplaintStatus.UNDER_REVIEW -> Triple(StatusAmber, StatusAmberContainer, strings.statusUnderReview)
        ComplaintStatus.MUNICIPALITY_INVESTIGATING -> Triple(CedarOrangePrimary, Color(0x26FF7A00), strings.statusInvestigating)
        ComplaintStatus.REFUND_ISSUED -> Triple(StatusGreen, StatusGreenContainer, strings.statusRefundIssued)
        ComplaintStatus.RESOLVED -> Triple(StatusGreen, StatusGreenContainer, strings.statusResolved)
    }

    LuxuryGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("complaint_card_${complaint.id}"),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${strings.caseNumberLabel} #${complaint.caseNumber}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CedarOrangePrimary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = complaint.providerName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )
                Text(
                    text = complaint.municipalityName + " • " + SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(complaint.filingDate)),
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${strings.claimAmountLabel}: +$${String.format(Locale.US, "%.2f", complaint.overchargeClaimUsd)} USD",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusRed
                )
            }

            Surface(
                color = statusBg,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = statusTitle,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
