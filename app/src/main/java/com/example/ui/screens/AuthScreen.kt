package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserRoleMode
import com.example.ui.components.ElghalizModalDialog
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

private val AuthDeepGreen = Color(0xFF0F3D2E)
private val AuthMidGreen = Color(0xFF1F6B52)
private val AuthGold = Color(0xFFC9A24D)
private val AuthCreamBg = Color(0xFFF7F3EA)
private val AuthCardBg = Color(0xFFFFFDF8)
private val AuthCardBorder = Color(0xFFEFE8D8)
private val AuthMintSecondary = Color(0xFFE9F0E8)

@Composable
fun AuthScreen(
    selectedRole: UserRoleMode,
    onRoleSelected: (UserRoleMode) -> Unit,
    isSigningIn: Boolean,
    errorMessage: String?,
    lastSavedEmail: String = "",
    lastSavedName: String = "",
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: () -> Unit = {},
    onSignInWithGoogle: () -> Unit
) {
    val context = LocalContext.current
    var showIosWebAppGuideDialog by remember { mutableStateOf(false) }
    var copiedFeedback by remember { mutableStateOf(false) }

    val sheetBgColor = if (isDarkTheme) Color(0xFF0A2A20) else AuthCreamBg
    val cardBgColor = if (isDarkTheme) Color(0xFF123528) else AuthCardBg
    val primaryTextColor = if (isDarkTheme) Color(0xFFF4F8F6) else Color(0xFF1E2B25)
    val mutedTextColor = if (isDarkTheme) Color(0xFFAEC4B9) else Color(0xFF5E6A63)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthDeepGreen)
    ) {
        // Decorative faint thin gold circle outlines on deep green background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeThin = Stroke(width = 1.4.dp.toPx())
            drawCircle(
                color = AuthGold.copy(alpha = 0.14f),
                radius = size.width * 0.48f,
                center = Offset(size.width * 0.88f, size.height * 0.10f),
                style = strokeThin
            )
            drawCircle(
                color = AuthGold.copy(alpha = 0.10f),
                radius = size.width * 0.34f,
                center = Offset(size.width * 0.12f, size.height * 0.22f),
                style = strokeThin
            )
            drawCircle(
                color = AuthGold.copy(alpha = 0.08f),
                radius = size.width * 0.62f,
                center = Offset(size.width * 0.50f, size.height * 0.04f),
                style = strokeThin
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Dark Mode Button + Gold Logo Square + Title + Welcome Subtitle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onToggleDarkTheme,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, AuthGold.copy(alpha = 0.35f), CircleShape)
                            .testTag("auth_dark_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "تبديل الوضع الليلي",
                            tint = AuthGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Gold Logo Square with Pulse/Plus Icon
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AuthGold, Color(0xFFB58C38))
                            )
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.38f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MonitorHeart,
                        contentDescription = null,
                        tint = AuthDeepGreen,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.auth_welcome_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AuthGold.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = stringResource(R.string.auth_welcome_subtitle),
                        style = MaterialTheme.typography.titleMedium,
                        color = AuthGold,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 7.dp)
                            .testTag("auth_welcome_subtitle")
                    )
                }
            }

            // Cream Bottom Sheet with Rounded Top Corners (38.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                shape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp),
                color = sheetBgColor,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drag handle pill
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(AuthGold.copy(alpha = 0.55f))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.choose_role_prompt),
                        style = MaterialTheme.typography.titleLarge,
                        color = primaryTextColor,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "يتم حفظ إيميلك وصفتك تلقائياً بعد أول تسجيل دخول",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedTextColor,
                        textAlign = TextAlign.Center
                    )

                    if (lastSavedEmail.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AuthMintSecondary.copy(alpha = if (isDarkTheme) 0.15f else 1f),
                            border = BorderStroke(1.dp, AuthGold.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) AuthGold else AuthMidGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "آخر حساب محفوظ: ${lastSavedName.ifBlank { lastSavedEmail }} ($lastSavedEmail)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = primaryTextColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Two Selectable Role Cards: "مسؤول / دكتور" and "طالب"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RoleSelectionCard(
                            title = stringResource(R.string.role_admin_doctor_title),
                            description = stringResource(R.string.role_admin_doctor_desc),
                            icon = Icons.Default.AdminPanelSettings,
                            isSelected = selectedRole == UserRoleMode.ADMIN,
                            isDarkTheme = isDarkTheme,
                            cardBgColor = cardBgColor,
                            onClick = { onRoleSelected(UserRoleMode.ADMIN) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_admin_card")
                        )

                        RoleSelectionCard(
                            title = stringResource(R.string.role_student_title),
                            description = stringResource(R.string.role_student_desc),
                            icon = Icons.Default.School,
                            isSelected = selectedRole == UserRoleMode.STUDENT,
                            isDarkTheme = isDarkTheme,
                            cardBgColor = cardBgColor,
                            onClick = { onRoleSelected(UserRoleMode.STUDENT) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_student_card")
                        )
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFBEDEA),
                            border = BorderStroke(1.dp, Color(0xFFB04A44).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_error_banner")
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB04A44),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Deep-Green Sign-In Button
                    Button(
                        onClick = onSignInWithGoogle,
                        enabled = !isSigningIn,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuthDeepGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = AuthGold
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "جاري التحقق من الحساب...",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                tint = AuthGold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (selectedRole == UserRoleMode.ADMIN) {
                                    "تسجيل الدخول كدكتور / مسؤول (Sign in with Google)"
                                } else {
                                    "تسجيل الدخول كطالب (Sign in with Google)"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mint Secondary Button for iPhone / iOS Users
                    Button(
                        onClick = { showIosWebAppGuideDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) Color(0xFF1A4636) else AuthMintSecondary,
                            contentColor = if (isDarkTheme) AuthGold else AuthDeepGreen
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ios_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneIphone,
                            contentDescription = null,
                            tint = if (isDarkTheme) AuthGold else AuthMidGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "طريقة تسجيل الدخول لمستخدمي الآيفون (iPhone / iOS)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showIosWebAppGuideDialog) {
        val iosPortalInfo = "منصة Elghaliz Medicine متوافقة مع أجهزة iPhone و iPad عبر متصفح Safari (تطبيق الويب التقدمي PWA) مع تسجيل الدخول بنفس البريد الإلكتروني المعتمد."
        ElghalizModalDialog(
            onDismissRequest = {
                showIosWebAppGuideDialog = false
                copiedFeedback = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneIphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تشغيل المنصة على الآيفون والآيباد (iOS)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "يمكن لطلاب الدفعة ومستخدمي أجهزة iPhone / iPad الدخول إلى نفس حساباتهم ومشاهدة جميع المحاضرات والاختبارات باتباع الخطوات التالية:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "1. افتح متصفح Safari على جهاز الآيفون.",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "2. سجل الدخول بنفس بريدك الإلكتروني في Google واختر صفة (طالب).",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "3. اضغط على زر المشاركة (Share) في Safari ثم اختر (إضافة إلى الشاشة الرئيسية - Add to Home Screen) لتثبيت أيقونة Elghaliz Medicine على شاشة الآيفون.",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "4. أرسل بريدك الإلكتروني أو معرّف الـ ID الخاص بك للدكتور لتفعيل حسابك والكورسات المشترك بها.",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (copiedFeedback) {
                        Text(
                            text = "تم نسخ إرشادات الدخول لمستخدمي الآيفون لمشاركتها مع الطلاب ✓",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Elghaliz iOS Guide", iosPortalInfo))
                        copiedFeedback = true
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ إرشادات الآيفون")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showIosWebAppGuideDialog = false
                        copiedFeedback = false
                    }
                ) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun RoleSelectionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    cardBgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        if (isDarkTheme) Color(0xFF1A4B39) else AuthMintSecondary
    } else {
        cardBgColor
    }
    val borderColor = if (isSelected) {
        AuthGold
    } else {
        if (isDarkTheme) Color(0xFF285846) else AuthCardBorder
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) AuthDeepGreen else AuthMidGreen.copy(alpha = 0.14f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) AuthGold else AuthMidGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AuthGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isDarkTheme) Color.White else AuthDeepGreen,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 15.sp),
                color = if (isDarkTheme) Color(0xFFB8CEC4) else Color(0xFF5E6A63),
                textAlign = TextAlign.Center,
                minLines = 3
            )
        }
    }
}

@Composable
fun AuthScreen(
    selectedRole: UserRoleMode,
    onRoleSelected: (UserRoleMode) -> Unit,
    onAuthSuccess: () -> Unit,
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val credentialManager = remember { androidx.credentials.CredentialManager.create(context) }
    val savedSession = remember { com.example.data.model.SessionPreferences.getSavedSession(context) }
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AuthScreen(
        selectedRole = selectedRole,
        onRoleSelected = onRoleSelected,
        isSigningIn = isSigningIn,
        errorMessage = errorMessage,
        lastSavedEmail = savedSession.email,
        lastSavedName = savedSession.displayName,
        isDarkTheme = isDarkMode,
        onToggleDarkTheme = onToggleDarkMode,
        onSignInWithGoogle = {
            if (isSigningIn) return@AuthScreen
            isSigningIn = true
            errorMessage = null
            scope.launch {
                try {
                    val webClientId = context.getString(R.string.default_web_client_id)
                    val googleIdOption = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(webClientId)
                        .build()
                    val request = androidx.credentials.GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    val result = credentialManager.getCredential(
                        request = request,
                        context = context
                    )
                    val credential = result.credential
                    if (credential is androidx.credentials.CustomCredential &&
                        credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val googleIdTokenCredential =
                            com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                        val firebaseCred = com.google.firebase.auth.GoogleAuthProvider.getCredential(
                            googleIdTokenCredential.idToken,
                            null
                        )
                        com.google.firebase.Firebase.auth.signInWithCredential(firebaseCred).let { task ->
                            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                                task.addOnCompleteListener { completed ->
                                    if (completed.isSuccessful) {
                                        cont.resumeWith(Result.success(completed.result))
                                    } else {
                                        cont.resumeWith(
                                            Result.failure(
                                                completed.exception ?: RuntimeException("Google Sign-In failed")
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        isSigningIn = false
                        onAuthSuccess()
                    } else {
                        isSigningIn = false
                        errorMessage = "تعذر قراءة بيانات حساب Google. يرجى المحاولة مرة أخرى."
                    }
                } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                    isSigningIn = false
                } catch (e: Exception) {
                    isSigningIn = false
                    errorMessage = e.localizedMessage ?: "تعذر تسجيل الدخول عبر Google. يرجى التحقق من اتصال الإنترنت."
                }
            }
        }
    )
}

fun signOut(
    context: Context,
    credentialManager: androidx.credentials.CredentialManager,
    scope: kotlinx.coroutines.CoroutineScope,
    onSignOutComplete: () -> Unit
) {
    com.example.data.model.SessionPreferences.markExplicitSignOut(context)
    scope.launch {
        try {
            com.google.firebase.Firebase.auth.signOut()
            credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
        } catch (_: Exception) {
            // Ignore credential state clearing exceptions
        } finally {
            onSignOutComplete()
        }
    }
}

