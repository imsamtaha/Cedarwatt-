package com.example.ui.chat

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BillFairnessStatus
import com.example.data.local.ScannedBill
import com.example.data.remote.gemini.ChatMessage
import com.example.data.remote.gemini.ChatbotRole
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.CedarOrangePrimary
import com.example.ui.theme.LuxuryCardBorder
import com.example.ui.theme.LuxurySurfaceDark
import com.example.ui.theme.LuxurySurfaceElevated
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GeminiChatView(
    messages: List<ChatMessage>,
    selectedRole: ChatbotRole,
    isGenerating: Boolean,
    inputText: String,
    onRoleSelected: (ChatbotRole) -> Unit,
    onInputChanged: (String) -> Unit,
    onSendMessage: (String?) -> Unit,
    onClearChat: () -> Unit,
    onRetry: () -> Unit,
    bills: List<ScannedBill> = emptyList(),
    selectedBill: ScannedBill? = null,
    onSelectBill: (ScannedBill?) -> Unit = {},
    onExplainBill: (ScannedBill) -> Unit = {},
    onAskCostReduction: (ScannedBill?) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive or state changes
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .imePadding()
            .testTag("gemini_chat_view")
    ) {
        // Chat Header with Title, Role Subtitle, Room Storage Badge, Search & Clear button
        ChatHeaderSection(
            title = strings.chatTitle,
            subtitle = strings.chatSubtitle,
            searchQuery = searchQuery,
            onSearchQueryChanged = onSearchQueryChanged,
            messageCount = messages.size,
            onClearChat = onClearChat
        )

        // Persona & Model Role Selector (General, Legal, Fast)
        ChatRoleSelectorRow(
            selectedRole = selectedRole,
            onRoleSelected = onRoleSelected
        )

        // Active Bill Context Bar with Explain & Cost Reduction Actions
        ActiveBillContextBar(
            bills = bills,
            selectedBill = selectedBill,
            onSelectBill = onSelectBill,
            onExplainBill = onExplainBill,
            onAskCostReduction = onAskCostReduction
        )

        // Scrollable Message Thread
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
                    .testTag("chat_message_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatMessageItem(
                        message = message,
                        onRetry = onRetry
                    )
                }

                if (isGenerating) {
                    item(key = "generating_indicator") {
                        GeneratingIndicatorItem(
                            role = selectedRole,
                            thinkingText = strings.chatThinking
                        )
                    }
                }
            }
        }

        // Suggested Prompts Carousel
        SuggestedPromptsRow(
            role = selectedRole,
            onPromptSelected = { onSendMessage(it) }
        )

        // Bottom Input Bar
        ChatInputBar(
            inputText = inputText,
            isGenerating = isGenerating,
            onInputChanged = onInputChanged,
            onSend = { onSendMessage(null) },
            placeholder = strings.chatInputPlaceholder
        )
    }
}

@Composable
private fun ChatHeaderSection(
    title: String,
    subtitle: String,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    messageCount: Int,
    onClearChat: () -> Unit
) {
    var isSearchExpanded by remember { mutableStateOf(searchQuery.isNotBlank()) }

    Surface(
        color = LuxurySurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(CedarOrangePrimary, SunsetCoral)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Room Database Local Storage Pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StatusGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = "Room Local Database",
                                    tint = StatusGreen,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Room Offline",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreen
                                )
                            }
                        }
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) onSearchQueryChanged("")
                        },
                        modifier = Modifier.testTag("chat_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Chat History",
                            tint = if (isSearchExpanded || searchQuery.isNotBlank()) CedarOrangePrimary else TextSecondaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier.testTag("chat_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear Chat",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isSearchExpanded || searchQuery.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = {
                            Text(
                                "Search local bills, audits & advice...",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = CedarOrangePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = TextSecondaryDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = LuxurySurfaceElevated,
                            unfocusedContainerColor = LuxurySurfaceElevated,
                            focusedBorderColor = CedarOrangePrimary,
                            unfocusedBorderColor = LuxuryCardBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("chat_search_input")
                    )

                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "Found $messageCount matching record(s) in Room storage",
                            fontSize = 10.sp,
                            color = CedarOrangePrimary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRoleSelectorRow(
    selectedRole: ChatbotRole,
    onRoleSelected: (ChatbotRole) -> Unit
) {
    val strings = LocalAppStrings.current

    Surface(
        color = Color(0xFF131720),
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("chat_role_selector"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ChatbotRole.values()) { role ->
                val isSelected = selectedRole == role
                val (roleTitle, roleIcon, accentColor) = when (role) {
                    ChatbotRole.GENERAL_ADVISOR -> Triple(strings.chatRoleGeneral, Icons.Default.AutoAwesome, CedarOrangePrimary)
                    ChatbotRole.LEGAL_AUDITOR -> Triple(strings.chatRoleLegal, Icons.Default.Gavel, SunsetCoral)
                    ChatbotRole.FAST_CALCULATOR -> Triple(strings.chatRoleFast, Icons.Default.Speed, StatusGreen)
                }

                Surface(
                    onClick = { onRoleSelected(role) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) accentColor.copy(alpha = 0.2f) else LuxurySurfaceElevated,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) accentColor else LuxuryCardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = roleIcon,
                            contentDescription = null,
                            tint = if (isSelected) accentColor else TextSecondaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = roleTitle,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) TextPrimaryDark else TextSecondaryDark
                            )
                            Text(
                                text = role.modelId,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) accentColor else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveBillContextBar(
    bills: List<ScannedBill>,
    selectedBill: ScannedBill?,
    onSelectBill: (ScannedBill?) -> Unit,
    onExplainBill: (ScannedBill) -> Unit,
    onAskCostReduction: (ScannedBill?) -> Unit
) {
    val strings = LocalAppStrings.current
    val isArabic = LocalAppLanguage.current == AppLanguage.ARABIC

    Surface(
        color = Color(0xFF0F131C),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_active_bill_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            if (selectedBill != null) {
                // Active selected bill card with audit highlights and direct action buttons
                val isOvercharged = selectedBill.overchargeAmountUsd > 0.0
                val statusColor = if (isOvercharged) StatusRed else StatusGreen

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LuxurySurfaceDark,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = CedarOrangePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${selectedBill.providerName} • ${selectedBill.billMonth}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }

                            // Amperage & Overcharge Pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = if (isOvercharged) "+$${String.format(Locale.US, "%.1f", selectedBill.overchargeAmountUsd)} Excess" else "Fair Tariff",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            IconButton(
                                onClick = { onSelectBill(null) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Deselect",
                                    tint = TextSecondaryDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Key Figures Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedBill.kwhConsumption.toInt()} kWh • ${selectedBill.amperage}A",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = "•",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = "$${String.format(Locale.US, "%.3f", selectedBill.ratePerKwhUsd)}/kWh",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = "•",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = "Total $${String.format(Locale.US, "%.2f", selectedBill.totalAmountUsd)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CedarOrangePrimary
                            )
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onExplainBill(selectedBill) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("explain_bill_action_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CedarOrangePrimary,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.chatExplainThisBill,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { onAskCostReduction(selectedBill) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("reduce_costs_action_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LuxurySurfaceElevated,
                                    contentColor = TextPrimaryDark
                                ),
                                border = BorderStroke(1.dp, LuxuryCardBorder),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EnergySavingsLeaf,
                                    contentDescription = null,
                                    tint = StatusGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.chatReduceCostsAction,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else if (bills.isNotEmpty()) {
                // Quick Scanned Bills Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.chatSelectBillPrompt,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(bills) { bill ->
                        val isOver = bill.overchargeAmountUsd > 0.0
                        Surface(
                            onClick = { onSelectBill(bill) },
                            shape = RoundedCornerShape(10.dp),
                            color = LuxurySurfaceElevated,
                            border = BorderStroke(1.dp, if (isOver) StatusRed.copy(alpha = 0.5f) else LuxuryCardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = if (isOver) StatusRed else CedarOrangePrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${bill.billMonth} ($${String.format(Locale.US, "%.1f", bill.totalAmountUsd)})",
                                    fontSize = 11.sp,
                                    color = TextPrimaryDark
                                )
                            }
                        }
                    }

                    // Quick general reduce costs chip
                    item {
                        Surface(
                            onClick = { onAskCostReduction(null) },
                            shape = RoundedCornerShape(10.dp),
                            color = StatusGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EnergySavingsLeaf,
                                    contentDescription = null,
                                    tint = StatusGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = strings.chatReduceCostsAction,
                                    fontSize = 11.sp,
                                    color = StatusGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onRetry: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))

    if (message.isUser) {
        // User Message Bubble - End aligned
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                color = CedarOrangePrimary,
                modifier = Modifier
                    .padding(start = 48.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        fontSize = 14.sp,
                        color = Color.Black,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeString,
                        fontSize = 10.sp,
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    } else {
        // Assistant Message Bubble - Start aligned
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                color = if (message.isError) StatusRed.copy(alpha = 0.12f) else LuxurySurfaceDark,
                border = BorderStroke(
                    1.dp,
                    if (message.isError) StatusRed.copy(alpha = 0.5f) else LuxuryCardBorder
                ),
                modifier = Modifier
                    .padding(end = 36.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Bot Header pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(CedarOrangePrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = CedarOrangePrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = message.modelUsed ?: "Gemini",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CedarOrangePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Copy response button
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(message.text))
                                Toast.makeText(context, "Copied response", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy text",
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val annotatedText = remember(message.text, message.isError) {
                        if (message.isError) {
                            AnnotatedString(message.text)
                        } else {
                            renderMarkdownAnnotatedString(
                                text = message.text,
                                textColor = TextPrimaryDark,
                                accentColor = CedarOrangePrimary
                            )
                        }
                    }

                    Text(
                        text = annotatedText,
                        fontSize = 14.sp,
                        color = if (message.isError) StatusRed else TextPrimaryDark,
                        lineHeight = 21.sp
                    )

                    if (message.isError) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusRed.copy(alpha = 0.2f),
                                contentColor = StatusRed
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeString,
                        fontSize = 10.sp,
                        color = TextSecondaryDark,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneratingIndicatorItem(
    role: ChatbotRole,
    thinkingText: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LuxurySurfaceDark,
        border = BorderStroke(1.dp, CedarOrangePrimary.copy(alpha = 0.4f)),
        modifier = Modifier.padding(end = 48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = CedarOrangePrimary,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "$thinkingText (${role.modelId})",
                fontSize = 12.sp,
                color = CedarOrangePrimary.copy(alpha = alpha),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SuggestedPromptsRow(
    role: ChatbotRole,
    onPromptSelected: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val isArabic = LocalAppLanguage.current == AppLanguage.ARABIC

    val suggestions = when (role) {
        ChatbotRole.GENERAL_ADVISOR -> if (isArabic) {
            listOf(
                "هل يمكن تشغيل المكيف على اشتراك 10 أمبير؟",
                "كيف أخفض فاتورة المولد هذا الشهر؟",
                "حساب وفورات الطاقة الشمسية 3.6 ك.و.ذ",
                "أفضل أوقات شحن بطارية الليثيوم"
            )
        } else {
            listOf(
                "Can I run my AC on a 10A generator?",
                "How to lower monthly generator bill?",
                "Estimate solar savings for 3.6 kWp",
                "Optimal hours to run high appliances"
            )
        }
        ChatbotRole.LEGAL_AUDITOR -> if (isArabic) {
            listOf(
                "تدقيق فاتورتي الأخيرة وكشف التسعير الزائد",
                "ما هي حقوقي بموجب القرار 41/2021؟",
                "صياغة كتاب شكوى رسمي إلى البلدية",
                "هل المقطوعة الشهرية دون عداد غير قانونية؟"
            )
        } else {
            listOf(
                "Audit my latest bill for price gouging",
                "What are my rights under Decree 41/2021?",
                "Draft official complaint to Municipality",
                "Is charging a flat rate (مقطوعة) illegal?"
            )
        }
        ChatbotRole.FAST_CALCULATOR -> if (isArabic) {
            listOf(
                "احسب 200 ك.و.س بسعر 0.360$ + رسم 10A",
                "تحويل 120$ إلى ليرة لبنانية على سعر 89,500",
                "ما هو الحد الأقصى للواط على 10 أمبير؟",
                "حساب استهلاك مكيف 1.5 حصان يومياً"
            )
        } else {
            listOf(
                "Calculate 200 kWh at $0.360 + 10A fixed fee",
                "Convert 120 USD to LBP at 89,500",
                "Check 10A breaker limit in Watts",
                "Estimate 1.5 HP AC daily consumption"
            )
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { prompt ->
            Surface(
                onClick = { onPromptSelected(prompt) },
                shape = RoundedCornerShape(16.dp),
                color = LuxurySurfaceElevated,
                border = BorderStroke(1.dp, LuxuryCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = CedarOrangePrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        color = TextPrimaryDark
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    isGenerating: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String
) {
    Surface(
        color = LuxurySurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                placeholder = {
                    Text(placeholder, fontSize = 13.sp, color = TextSecondaryDark)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF131720),
                    unfocusedContainerColor = Color(0xFF131720),
                    focusedBorderColor = CedarOrangePrimary,
                    unfocusedBorderColor = LuxuryCardBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isGenerating,
                shape = CircleShape,
                color = if (inputText.isNotBlank() && !isGenerating) CedarOrangePrimary else LuxurySurfaceElevated,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("chat_send_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = CedarOrangePrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) Color.Black else TextSecondaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun renderMarkdownAnnotatedString(
    text: String,
    textColor: Color,
    accentColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        val pattern = Regex("\\*\\*(.*?)\\*\\*")
        var currentIndex = 0
        val matches = pattern.findAll(text)

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1
            val boldContent = match.groupValues[1]

            if (start > currentIndex) {
                withStyle(SpanStyle(color = textColor)) {
                    append(text.substring(currentIndex, start))
                }
            }

            withStyle(
                SpanStyle(
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(boldContent)
            }

            currentIndex = end
        }

        if (currentIndex < text.length) {
            withStyle(SpanStyle(color = textColor)) {
                append(text.substring(currentIndex))
            }
        }
    }
}
