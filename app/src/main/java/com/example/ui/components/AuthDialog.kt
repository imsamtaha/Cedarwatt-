package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.supabase.AuthResult
import com.example.data.remote.supabase.AuthState
import com.example.data.remote.supabase.AuthUser
import com.example.ui.CedarWattViewModel
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.theme.CedarOrangePrimary
import com.example.ui.theme.LuxuryCardBorder
import com.example.ui.theme.LuxurySurfaceDark
import com.example.ui.theme.LuxurySurfaceElevated
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDialog(
    viewModel: CedarWattViewModel,
    authState: AuthState,
    activeUser: AuthUser?,
    supabaseUrl: String,
    supabaseAnonKey: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val isArabic = language == AppLanguage.ARABIC
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(if (activeUser != null) 3 else 0) }

    // Sign In states
    var signInEmail by remember { mutableStateOf("") }
    var signInPassword by remember { mutableStateOf("") }
    var showSignInPassword by remember { mutableStateOf(false) }

    // Sign Up states
    var signUpFullName by remember { mutableStateOf("") }
    var signUpEmail by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var signUpConfirmPassword by remember { mutableStateOf("") }
    var signUpPhone by remember { mutableStateOf("") }
    var signUpLocation by remember { mutableStateOf("Achrafieh, Beirut") }
    var signUpMunicipality by remember { mutableStateOf("Municipality of Beirut") }
    var showSignUpPassword by remember { mutableStateOf(false) }
    var municipalityExpanded by remember { mutableStateOf(false) }

    // Supabase config states
    var customUrl by remember { mutableStateOf(supabaseUrl) }
    var customKey by remember { mutableStateOf(supabaseAnonKey) }
    var isTestingConnection by remember { mutableStateOf(false) }

    // Feedback
    var statusBannerMessage by remember { mutableStateOf<String?>(null) }
    var statusBannerIsError by remember { mutableStateOf(false) }

    val municipalities = listOf(
        "Municipality of Beirut",
        "Municipality of Metn (Jdeideh / Bauchrieh)",
        "Municipality of Keserwan (Jounieh)",
        "Municipality of Tripoli",
        "Municipality of Saida",
        "Municipality of Zahle",
        "Municipality of Baabda",
        "Municipality of Chouf",
        "Municipality of Aley",
        "Municipality of Tyre",
        "Municipality of Byblos (Jbeil)"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
                .imePadding()
                .testTag("auth_dialog_surface"),
            shape = RoundedCornerShape(24.dp),
            color = LuxurySurfaceDark,
            border = BorderStroke(1.dp, LuxuryCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(CedarOrangePrimary, SunsetCoral)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (activeUser != null) Icons.Default.Person else Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (activeUser != null) {
                                    if (isArabic) "حسابك الشخصي" else "CedarWatt Account"
                                } else {
                                    if (isArabic) "تسجيل الدخول / إنشاء حساب" else "Supabase Authentication"
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = if (isArabic) "ربط بيانات فواتير الطاقة والشكاوى بحسابك" else "Securely link bills, audits & complaints",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_auth_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Banner
                statusBannerMessage?.let { banner ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (statusBannerIsError) StatusRed.copy(alpha = 0.15f) else StatusGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (statusBannerIsError) StatusRed else StatusGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (statusBannerIsError) Icons.Default.Close else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (statusBannerIsError) StatusRed else StatusGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = banner,
                                fontSize = 12.sp,
                                color = TextPrimaryDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Supabase Connection badge
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = LuxurySurfaceElevated,
                    border = BorderStroke(1.dp, CedarOrangePrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Supabase Auth GoTrue Ready",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondaryDark
                            )
                        }
                        TextButton(
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.testTag("btn_auth_view_supabase_config")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = CedarOrangePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "إعدادات الربط" else "Config",
                                fontSize = 11.sp,
                                color = CedarOrangePrimary
                            )
                        }
                    }
                }

                // Tabs
                val tabTitles = if (activeUser != null) {
                    listOf(
                        if (isArabic) "تسجيل الدخول" else "Sign In",
                        if (isArabic) "إنشاء حساب" else "Sign Up",
                        if (isArabic) "إعدادات Supabase" else "Supabase Settings",
                        if (isArabic) "الملف الشخصي" else "Profile"
                    )
                } else {
                    listOf(
                        if (isArabic) "تسجيل الدخول" else "Sign In",
                        if (isArabic) "إنشاء حساب" else "Sign Up",
                        if (isArabic) "إعدادات Supabase" else "Supabase Settings"
                    )
                }

                TabRow(
                    selectedTabIndex = selectedTab.coerceAtMost(tabTitles.size - 1),
                    containerColor = LuxurySurfaceDark,
                    contentColor = CedarOrangePrimary,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = CedarOrangePrimary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                statusBannerMessage = null
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) CedarOrangePrimary else TextSecondaryDark
                                )
                            },
                            modifier = Modifier.testTag("auth_tab_$index")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Loading indicator if authenticating
                if (authState is AuthState.Loading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = CedarOrangePrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = authState.message,
                            fontSize = 13.sp,
                            color = TextSecondaryDark
                        )
                    }
                } else {
                    when (selectedTab) {
                        0 -> {
                            // SIGN IN TAB
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = signInEmail,
                                    onValueChange = { signInEmail = it },
                                    label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Email,
                                            contentDescription = null,
                                            tint = CedarOrangePrimary
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    colors = authTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_signin_email")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = signInPassword,
                                    onValueChange = { signInPassword = it },
                                    label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = CedarOrangePrimary
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showSignInPassword = !showSignInPassword }) {
                                            Icon(
                                                imageVector = if (showSignInPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle password visibility",
                                                tint = TextSecondaryDark
                                            )
                                        }
                                    },
                                    visualTransformation = if (showSignInPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    colors = authTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_signin_password")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            if (signInEmail.isBlank()) {
                                                statusBannerIsError = true
                                                statusBannerMessage = if (isArabic) "أدخل بريدك الإلكتروني أولاً لاستعادة كلمة المرور" else "Enter your email first to recover password"
                                            } else {
                                                viewModel.recoverPassword(signInEmail) { result ->
                                                    result.fold(
                                                        onSuccess = { msg ->
                                                            statusBannerIsError = false
                                                            statusBannerMessage = msg
                                                        },
                                                        onFailure = { err ->
                                                            statusBannerIsError = true
                                                            statusBannerMessage = err.localizedMessage
                                                        }
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("btn_forgot_password")
                                    ) {
                                        Text(
                                            text = if (isArabic) "نسيت كلمة المرور؟" else "Forgot password?",
                                            fontSize = 11.sp,
                                            color = CedarOrangePrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        viewModel.signIn(signInEmail, signInPassword) { result ->
                                            when (result) {
                                                is AuthResult.Success -> {
                                                    statusBannerIsError = false
                                                    statusBannerMessage = result.message
                                                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                }
                                                is AuthResult.NeedsEmailConfirmation -> {
                                                    statusBannerIsError = false
                                                    statusBannerMessage = result.message
                                                }
                                                is AuthResult.Error -> {
                                                    statusBannerIsError = true
                                                    statusBannerMessage = result.errorMessage
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("btn_action_signin"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CedarOrangePrimary,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isArabic) "تسجيل الدخول عبر Supabase" else "Sign In with Supabase",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = {
                                        viewModel.continueAsGuest()
                                        Toast.makeText(
                                            context,
                                            if (isArabic) "تم الدخول كضيف محلي" else "Exploring in Local Guest Mode",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_continue_as_guest"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, LuxuryCardBorder)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = TextSecondaryDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isArabic) "المتابعة كضيف / تجربة محلية" else "Continue as Guest / Sandbox Mode",
                                        fontSize = 13.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        }

                        1 -> {
                            // SIGN UP TAB
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = signUpFullName,
                                    onValueChange = { signUpFullName = it },
                                    label = { Text(if (isArabic) "الاسم الكامل" else "Full Name") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = CedarOrangePrimary)
                                    },
                                    singleLine = true,
                                    colors = authTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_signup_name")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = signUpEmail,
                                    onValueChange = { signUpEmail = it },
                                    label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = CedarOrangePrimary)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    colors = authTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_signup_email")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = signUpPassword,
                                    onValueChange = { signUpPassword = it },
                                    label = { Text(if (isArabic) "كلمة المرور (٦ أحرف على الأقل)" else "Password (min 6 chars)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = CedarOrangePrimary)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showSignUpPassword = !showSignUpPassword }) {
                                            Icon(
                                                imageVector = if (showSignUpPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = TextSecondaryDark
                                            )
                                        }
                                    },
                                    visualTransformation = if (showSignUpPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    colors = authTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_signup_password")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = signUpPhone,
                                    onValueChange = { signUpPhone = it },
                                    label = { Text(if (isArabic) "رقم الهاتف (اختياري)" else "Phone Number (Optional)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = CedarOrangePrimary)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    colors = authTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_signup_phone")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = signUpLocation,
                                    onValueChange = { signUpLocation = it },
                                    label = { Text(if (isArabic) "المنطقة / الحي" else "Neighborhood / District") },
                                    leadingIcon = {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = CedarOrangePrimary)
                                    },
                                    singleLine = true,
                                    colors = authTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_signup_location")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                ExposedDropdownMenuBox(
                                    expanded = municipalityExpanded,
                                    onExpandedChange = { municipalityExpanded = !municipalityExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = signUpMunicipality,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(if (isArabic) "البلدية المسؤولة" else "Municipality") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = municipalityExpanded) },
                                        colors = authTextFieldColors(),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                            .testTag("dropdown_signup_municipality")
                                    )
                                    ExposedDropdownMenu(
                                        expanded = municipalityExpanded,
                                        onDismissRequest = { municipalityExpanded = false },
                                        modifier = Modifier.background(LuxurySurfaceElevated)
                                    ) {
                                        municipalities.forEach { mun ->
                                            DropdownMenuItem(
                                                text = { Text(mun, color = TextPrimaryDark, fontSize = 13.sp) },
                                                onClick = {
                                                    signUpMunicipality = mun
                                                    municipalityExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (signUpPassword.length < 6) {
                                            statusBannerIsError = true
                                            statusBannerMessage = if (isArabic) "كلمة المرور يجب أن تتكون من ٦ أحرف على الأقل" else "Password must be at least 6 characters"
                                            return@Button
                                        }
                                        viewModel.signUp(
                                            email = signUpEmail,
                                            pass = signUpPassword,
                                            fullName = signUpFullName,
                                            phone = signUpPhone,
                                            location = signUpLocation,
                                            municipality = signUpMunicipality
                                        ) { result ->
                                            when (result) {
                                                is AuthResult.Success -> {
                                                    statusBannerIsError = false
                                                    statusBannerMessage = result.message
                                                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                }
                                                is AuthResult.NeedsEmailConfirmation -> {
                                                    statusBannerIsError = false
                                                    statusBannerMessage = result.message
                                                }
                                                is AuthResult.Error -> {
                                                    statusBannerIsError = true
                                                    statusBannerMessage = result.errorMessage
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("btn_action_signup"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CedarOrangePrimary,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isArabic) "إنشاء حساب وربط البيانات" else "Create Account & Link Data",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        2 -> {
                            // SUPABASE SETTINGS TAB
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (isArabic) "إعدادات الاتصال بمشروع Supabase" else "Supabase GoTrue Project Connection",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isArabic)
                                        "يمكنك استخدام مشروع CedarWatt الافتراضي أو إدخال عنوان مشروع Supabase والمفتاح الخاص بك."
                                    else
                                        "You can connect to your own Supabase project or use the default CedarWatt cloud endpoint.",
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = customUrl,
                                    onValueChange = { customUrl = it },
                                    label = { Text("Supabase Project URL") },
                                    placeholder = { Text("https://xyzcompany.supabase.co") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Cloud, contentDescription = null, tint = CedarOrangePrimary)
                                    },
                                    singleLine = true,
                                    colors = authTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_supabase_url")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = customKey,
                                    onValueChange = { customKey = it },
                                    label = { Text("Supabase Public Anon Key") },
                                    placeholder = { Text("eyJhbGciOiJIUzI1NiIsIn...") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = CedarOrangePrimary)
                                    },
                                    singleLine = false,
                                    maxLines = 3,
                                    colors = authTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_supabase_key")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.resetSupabaseCredentials()
                                            customUrl = viewModel.supabaseUrl.value
                                            customKey = viewModel.supabaseAnonKey.value
                                            statusBannerIsError = false
                                            statusBannerMessage = if (isArabic) "تمت استعادة الإعدادات الافتراضية" else "Restored default project keys"
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("btn_reset_supabase_keys"),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, LuxuryCardBorder)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isArabic) "الافتراضي" else "Reset", fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Button(
                                        onClick = {
                                            viewModel.updateSupabaseCredentials(customUrl, customKey)
                                            statusBannerIsError = false
                                            statusBannerMessage = if (isArabic) "تم حفظ إعدادات Supabase بنجاح" else "Supabase credentials updated successfully"
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("btn_save_supabase_keys"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CedarOrangePrimary,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isArabic) "حفظ وتحديث" else "Save & Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        3 -> {
                            // PROFILE TAB (WHEN LOGGED IN)
                            if (activeUser != null) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = LuxurySurfaceElevated,
                                        border = BorderStroke(1.dp, CedarOrangePrimary.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape)
                                                        .background(CedarOrangePrimary),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = activeUser.fullName.take(2).uppercase(),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 18.sp,
                                                        color = Color.Black
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = activeUser.fullName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = TextPrimaryDark
                                                    )
                                                    Text(
                                                        text = activeUser.email,
                                                        fontSize = 12.sp,
                                                        color = CedarOrangePrimary
                                                    )
                                                    Text(
                                                        text = if (activeUser.isGuest) "Mode: Guest / Sandbox" else "Status: Verified Supabase Account",
                                                        fontSize = 10.sp,
                                                        color = if (activeUser.isGuest) StatusAmber else StatusGreen
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = TextSecondaryDark,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${activeUser.location} • ${activeUser.municipality}",
                                                    fontSize = 12.sp,
                                                    color = TextSecondaryDark
                                                )
                                            }

                                            if (activeUser.phone.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Phone,
                                                        contentDescription = null,
                                                        tint = TextSecondaryDark,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = activeUser.phone,
                                                        fontSize = 12.sp,
                                                        color = TextSecondaryDark
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Link,
                                                    contentDescription = null,
                                                    tint = StatusGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "ID: ${activeUser.id}",
                                                    fontSize = 11.sp,
                                                    color = TextSecondaryDark
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = StatusGreen.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = StatusGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = if (isArabic)
                                                    "جميع فواتيرك، تقارير التدقيق، ومسودات الشكاوى مربوطة بشكل مشفّر بحسابك الحالي."
                                                else
                                                    "All your energy bills, audit reports, and complaints are securely linked to your account.",
                                                fontSize = 12.sp,
                                                color = TextPrimaryDark
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            viewModel.signOut()
                                            Toast.makeText(
                                                context,
                                                if (isArabic) "تم تسجيل الخروج بنجاح" else "Signed out",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            selectedTab = 0
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("btn_action_signout"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = StatusRed.copy(alpha = 0.2f),
                                            contentColor = StatusRed
                                        ),
                                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Logout,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isArabic) "تسجيل الخروج من الحساب" else "Sign Out of Account",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CedarOrangePrimary,
    unfocusedBorderColor = LuxuryCardBorder,
    focusedLabelColor = CedarOrangePrimary,
    unfocusedLabelColor = TextSecondaryDark,
    focusedTextColor = TextPrimaryDark,
    unfocusedTextColor = TextPrimaryDark,
    cursorColor = CedarOrangePrimary
)
