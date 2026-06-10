package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.model.ReportedScamItem
import com.example.data.local.model.ScanHistoryItem
import com.example.data.network.GeminiService
import com.example.ui.theme.*
import com.example.ui.viewmodel.SecurityViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: SecurityViewModel) {
    val scanHistory by viewModel.scanHistory.collectAsState()
    val reportedScams by viewModel.reportedScams.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val lastResult by viewModel.lastResult.collectAsState()
    val assistantChat by viewModel.assistantChat.collectAsState()
    val familyAlerts by viewModel.familyAlerts.collectAsState()

    val currentScore = viewModel.getImmunityScore(scanHistory)
    val badgeTitle = viewModel.getBadgeTitle(currentScore)

    var currentTab by remember { mutableStateOf("guardian") } // guardian, scanners, terminal, community
    var mobileMonitoringEnabled by remember { mutableStateOf(true) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CyberDarkSurface,
                tonalElevation = 10.dp,
                modifier = Modifier.border(0.5.dp, CyberDarkSurfaceVariant, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                NavigationBarItem(
                    selected = currentTab == "guardian",
                    onClick = { currentTab = "guardian" },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Guardian") },
                    label = { Text("Guardian") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberPrimaryGreen,
                        selectedTextColor = CyberPrimaryGreen,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray,
                        indicatorColor = CyberDarkSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "scanners",
                    onClick = { currentTab = "scanners" },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Inspectors") },
                    label = { Text("Inspectors") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberPrimaryGreen,
                        selectedTextColor = CyberPrimaryGreen,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray,
                        indicatorColor = CyberDarkSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "terminal",
                    onClick = { currentTab = "terminal" },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "AI Terminal") },
                    label = { Text("AI Helper") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberPrimaryGreen,
                        selectedTextColor = CyberPrimaryGreen,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray,
                        indicatorColor = CyberDarkSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "community",
                    onClick = { currentTab = "community" },
                    icon = { Icon(Icons.Default.Public, contentDescription = "Fraud Network") },
                    label = { Text("Intelligence") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberPrimaryGreen,
                        selectedTextColor = CyberPrimaryGreen,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray,
                        indicatorColor = CyberDarkSurfaceVariant
                    )
                )
            }
        },
        containerColor = CyberDarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background overlay or subtle ambient lights
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CyberPrimaryGreen.copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            radius = 600f
                        )
                    )
            )

            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    "guardian" -> GuardianTab(
                        score = currentScore,
                        badge = badgeTitle,
                        alerts = familyAlerts,
                        monitoring = mobileMonitoringEnabled,
                        onMonitoringChange = { mobileMonitoringEnabled = it },
                        scanHistory = scanHistory,
                        onDeleteHistory = { viewModel.deleteScanHistoryItem(it) },
                        onClearHistory = { viewModel.clearAllHistory() }
                    )
                    "scanners" -> ScannersTab(
                        viewModel = viewModel,
                        isAnalyzing = isAnalyzing,
                        lastResult = lastResult
                    )
                    "terminal" -> TerminalTab(
                        viewModel = viewModel,
                        assistantChat = assistantChat,
                        isAnalyzing = isAnalyzing
                    )
                    "community" -> CommunityTab(
                        viewModel = viewModel,
                        reportedScams = reportedScams
                    )
                }
            }
        }
    }
}

// --- GUARDIAN DASHBOARD TAB ---
@Composable
fun GuardianTab(
    score: Int,
    badge: String,
    alerts: List<String>,
    monitoring: Boolean,
    onMonitoringChange: (Boolean) -> Unit,
    scanHistory: List<ScanHistoryItem>,
    onDeleteHistory: (Long) -> Unit,
    onClearHistory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Premium Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberDarkSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(CyberDarkSurfaceVariant, CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon),
                            contentDescription = "Shield Guard",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "AI SCAM GUARDIAN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = CyberPrimaryGreen,
                            fontFamily = FontFamily.SansSerif
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(CyberPrimaryGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GLOBAL DEFENSTRY ACTIVE",
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = TextGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Cyber Immunity Circle Meter
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberDarkSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "YOUR SECURITY POSTURE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.size(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient outer glow ring
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    BorderStroke(4.dp, CyberDarkSurfaceVariant),
                                    CircleShape
                                )
                        )
                        // Colored progress ring depending on standard score range
                        val scoreColor = when {
                            score >= 90 -> CyberPrimaryGreen
                            score >= 75 -> CyberSecondaryBlue
                            score >= 50 -> CyberTertiaryAmber
                            else -> CyberCriticalRed
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.92f)
                                .border(
                                    BorderStroke(5.dp, scoreColor),
                                    CircleShape
                                )
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$score%",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = scoreColor,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "HEALTH INDEX",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = TextGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = badge,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Gamified Badge Level",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
            }
        }

        // Active Mobile Background Protection Mode switch
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberDarkSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (monitoring) Icons.Default.Verified else Icons.Default.Warning,
                            contentDescription = "Shield",
                            tint = if (monitoring) CyberPrimaryGreen else CyberTertiaryAmber,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Real-time Mobile Protection",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (monitoring) "Sensing suspicious messages & risky downloads" else "Protection paused. Device vulnerable.",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = monitoring,
                        onCheckedChange = onMonitoringChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberDarkBackground,
                            checkedTrackColor = CyberPrimaryGreen,
                            uncheckedThumbColor = TextGray,
                            uncheckedTrackColor = CyberDarkSurfaceVariant
                        )
                    )
                }
            }
        }

        // Family Protection Live Alert feed
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Family Protection Circle Tracker",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = CyberSecondaryBlue,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, CyberDarkSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        alerts.forEach { alert ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (alert.contains("ALERT")) Icons.Default.Campaign else Icons.Default.Info,
                                    contentDescription = "info",
                                    tint = if (alert.contains("ALERT")) CyberCriticalRed else CyberSecondaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = alert,
                                    color = OnCyberDarkSurface,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Scan History Title and Action Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Security Scan Logs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                if (scanHistory.isNotEmpty()) {
                    Text(
                        text = "Clear Logs",
                        fontSize = 12.sp,
                        color = CyberCriticalRed,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onClearHistory() }
                            .padding(4.dp)
                    )
                }
            }
        }

        if (scanHistory.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(0.5.dp, CyberDarkSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "empty",
                                tint = TextGray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No scan logs recorded yet.",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                            Text(
                                text = "Run inspectors to evaluate real-time items.",
                                fontSize = 10.sp,
                                color = TextGray.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        } else {
            items(scanHistory) { log ->
                HistoryLogCard(item = log, onDelete = { onDeleteHistory(log.id) })
            }
        }
    }
}

@Composable
fun HistoryLogCard(item: ScanHistoryItem, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dateStr = formatter.format(Date(item.timestamp))

    val riskColor = when (item.riskCategory) {
        "Safe" -> CyberSafeEmerald
        "Low Risk" -> CyberPrimaryGreen
        "Medium Risk" -> CyberTertiaryAmber
        "High Risk" -> Color(0xFFFF9100)
        "Critical" -> CyberCriticalRed
        else -> TextGray
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberDarkSurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(riskColor.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.type) {
                                "screenshot" -> Icons.Default.UploadFile
                                "chat" -> Icons.Default.Chat
                                "url" -> Icons.Default.Link
                                "qr" -> Icons.Default.QrCodeScanner
                                "apk" -> Icons.Default.SettingsCell
                                "voice" -> Icons.Default.RecordVoiceOver
                                "email" -> Icons.Default.Mail
                                else -> Icons.Default.Shield
                            },
                            contentDescription = "icon",
                            tint = riskColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.type.replaceFirstChar { it.uppercase() } + " Inspector",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = dateStr,
                            color = TextGray,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(riskColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(1.dp, riskColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.riskCategory,
                            color = riskColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = TextGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = item.inputData,
                color = OnCyberDarkSurface.copy(alpha = 0.8f),
                fontSize = 12.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (expanded) {
                Divider(
                    color = CyberDarkSurfaceVariant,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                Text(
                    text = "AI Threat Analysis Details:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberPrimaryGreen,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = item.reasoning,
                    fontSize = 11.sp,
                    color = OnCyberDarkSurface,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Recommended Safe Measures:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberTertiaryAmber,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.recommendedAction,
                        fontSize = 11.sp,
                        color = Color.White,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Score Deducted: ${item.riskScore}%",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// --- INSPECTORS / SCANNERS TAB ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScannersTab(
    viewModel: SecurityViewModel,
    isAnalyzing: Boolean,
    lastResult: GeminiService.AnalysisResult?
) {
    var activeInspector by remember { mutableStateOf<String?>("screenshot") } // screenshot, chat, url, qr, apk, voice, email

    // Input States
    var chatPastedText by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var qrDescription by remember { mutableStateOf("") }
    var apkInputDetails by remember { mutableStateOf("") }
    var voiceTranscript by remember { mutableStateOf("") }
    var emailRawBody by remember { mutableStateOf("") }

    // Visual media picker launcher
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            selectedImageUri = uri
            if (uri != null) {
                // Decode bitmap
                selectedBitmap = try {
                    if (Build.VERSION.SDK_INT < 28) {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    } else {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Selector Header chips grid
        item {
            Text(
                text = "CHOOSE BULLETPROOF INSPECTOR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CyberSecondaryBlue,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val inspectors = listOf(
                    "screenshot" to "Screenshot",
                    "chat" to "Chat logs",
                    "url" to "URL Link",
                    "qr" to "QR Code",
                    "apk" to "APK Audit",
                    "voice" to "Voice call",
                    "email" to "Email link"
                )

                inspectors.forEach { (type, label) ->
                    val isSelected = activeInspector == type
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) CyberPrimaryGreen else CyberDarkSurface,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) CyberPrimaryGreen else CyberDarkSurfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                activeInspector = type
                                selectedImageUri = null
                                selectedBitmap = null
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) CyberDarkBackground else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Active Inspector form block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberDarkSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (activeInspector) {
                        "screenshot" -> {
                            Text(
                                text = "UPLOAD SUSPICIOUS SCREENSHOT",
                                color = CyberPrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "Take a screenshot of a weird payment requests, bank receipts, cryptocurrency sites, or SMS threads, and inspect it here.",
                                color = TextGray,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Button(
                                onClick = {
                                    singlePhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberDarkSurfaceVariant),
                                modifier = Modifier.align(Alignment.CenterHorizontally).testTag("select_screenshot")
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = "upload", tint = CyberPrimaryGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedImageUri != null) "Select Another Screenshot" else "Attach Screenshot",
                                    color = Color.White
                                )
                            }

                            selectedBitmap?.let { bitmap ->
                                Spacer(modifier = Modifier.height(14.dp))
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Selected screenshot",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, CyberDarkSurfaceVariant, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { viewModel.analyzeScreenshot(bitmap) },
                                    enabled = !isAnalyzing,
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimaryGreen),
                                    modifier = Modifier.fillMaxWidth().testTag("analyze_button")
                                ) {
                                    if (isAnalyzing) {
                                        CircularProgressIndicator(color = CyberDarkBackground, modifier = Modifier.size(20.dp))
                                    } else {
                                        Text("Audit Screenshot Now", color = CyberDarkBackground, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        "chat" -> {
                            Text(
                                text = "SCAM CHAT CONVERSATION AUDIT",
                                color = CyberPrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Paste conversation logs or texts from WhatsApp, Telegram, or Messenger offering job opportunities, rewards, crypto or urgent family rescue cash.",
                                color = TextGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            OutlinedTextField(
                                value = chatPastedText,
                                onValueChange = { chatPastedText = it },
                                placeholder = { Text("Paste conversation thread texts here...", color = TextGray) },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberPrimaryGreen,
                                    unfocusedBorderColor = CyberDarkSurfaceVariant,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.analyzeChat(chatPastedText) },
                                enabled = !isAnalyzing && chatPastedText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimaryGreen),
                                modifier = Modifier.fillMaxWidth().testTag("chat_audit_btn")
                            ) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(color = CyberDarkBackground, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("Analyze Conversational Bait", color = CyberDarkBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        "url" -> {
                            Text(
                                text = "SUSPICIOUS THREAT LINK INTEL",
                                color = CyberPrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Check website URLs, netbanking portals, or cash lottery linkages before typing your logins or passwords.",
                                color = TextGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                placeholder = { Text("e.g. hdfc-rewards-online.com", color = TextGray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberPrimaryGreen,
                                    unfocusedBorderColor = CyberDarkSurfaceVariant,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.analyzeUrl(urlInput) },
                                enabled = !isAnalyzing && urlInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimaryGreen),
                                modifier = Modifier.fillMaxWidth().testTag("scan_url_btn")
                            ) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(color = CyberDarkBackground, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("Audit Threat Domain", color = CyberDarkBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        "qr" -> {
                            Text(
                                text = "UPLOAD UPI / DEBIT QR CODE",
                                color = CyberPrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Scammers send QRs requesting automatic bank debit or containing links redirecting to fake payment pages.",
                                color = TextGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            Button(
                                onClick = {
                                    singlePhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberDarkSurfaceVariant),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "qr", tint = CyberPrimaryGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedImageUri != null) "QR Image Selected" else "Choose QR Image",
                                    color = Color.White
                                )
                            }

                            selectedBitmap?.let { bitmap ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "qr preview",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .align(Alignment.CenterHorizontally)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, CyberDarkSurfaceVariant, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = qrDescription,
                                onValueChange = { qrDescription = it },
                                placeholder = { Text("Add UPI or scan payment details (optional)...", color = TextGray) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberPrimaryGreen,
                                    unfocusedBorderColor = CyberDarkSurfaceVariant,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.analyzeQr(selectedBitmap, qrDescription.ifBlank { "Scanned custom local QR Code asset." }) },
                                enabled = !isAnalyzing,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimaryGreen),
                                modifier = Modifier.fillMaxWidth().testTag("scan_qr_btn")
                            ) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(color = CyberDarkBackground, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("Analyze QR Redirect Route", color = CyberDarkBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        "apk" -> {
                            Text(
                                text = "ANDROID APK PERMISSIONS AUDITOR",
                                color = CyberPrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Enter permissions requested by an untrusted APK (such as Accessibility, SMS Reading, Draw Overlays) to evaluate virus indicators.",
                                color = TextGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            OutlinedTextField(
                                value = apkInputDetails,
                                onValueChange = { apkInputDetails = it },
                                placeholder = { Text("e.g. FlashPlayer.apk requesting BIND_ACCESSIBILITY_SERVICE and RECEIVE_SMS", color = TextGray) },
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberPrimaryGreen,
                                    unfocusedBorderColor = CyberDarkSurfaceVariant,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.analyzeApk(apkInputDetails) },
                                enabled = !isAnalyzing && apkInputDetails.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimaryGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(color = CyberDarkBackground, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("Run Permissions Audit", color = CyberDarkBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        "voice" -> {
                            Text(
                                text = "DEEPFAKE VOICE SCAM DIAGNOSTIC",
                                color = CyberPrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Paste the text transcript or describe a voicemail claim where they demand giftcards/urgency cash/claim to be bank helpline.",
                                color = TextGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            OutlinedTextField(
                                value = voiceTranscript,
                                onValueChange = { voiceTranscript = it },
                                placeholder = { Text("Helpline calling: Your account has been breached. Purchase a $200 Google Play card immediately to secure your money...", color = TextGray) },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberPrimaryGreen,
                                    unfocusedBorderColor = CyberDarkSurfaceVariant,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.analyzeVoice(voiceTranscript) },
                                enabled = !isAnalyzing && voiceTranscript.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimaryGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(color = CyberDarkBackground, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("Diagnose Voice Clone/Tactics", color = CyberDarkBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        "email" -> {
                            Text(
                                text = "EMAIL PHISHING KEY INDICATORS",
                                color = CyberPrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Paste suspicious email body text with links or claims to verify if it represents deceptive invoice fraud.",
                                color = TextGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            OutlinedTextField(
                                value = emailRawBody,
                                onValueChange = { emailRawBody = it },
                                placeholder = { Text("Paste entire raw email body here...", color = TextGray) },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberPrimaryGreen,
                                    unfocusedBorderColor = CyberDarkSurfaceVariant,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.analyzeEmail(emailRawBody) },
                                enabled = !isAnalyzing && emailRawBody.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimaryGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(color = CyberDarkBackground, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("Inspect Email Phishing Trait", color = CyberDarkBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Output Result Card
        item {
            lastResult?.let { result ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "LATEST SCAN THREAT REPORT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberSecondaryBlue,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                val riskColor = when (result.riskCategory) {
                    "Safe" -> CyberSafeEmerald
                    "Low Risk" -> CyberPrimaryGreen
                    "Medium Risk" -> CyberTertiaryAmber
                    "High Risk" -> Color(0xFFFF9100)
                    "Critical" -> CyberCriticalRed
                    else -> Color.White
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(2.dp, riskColor),
                    modifier = Modifier.fillMaxWidth().testTag("scan_result_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = result.riskCategory,
                                    color = riskColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp
                                )
                                Text(
                                    text = "AI CLASSIFIED THREAT LEVEL",
                                    color = TextGray,
                                    fontSize = 10.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${result.riskScore}/100",
                                    color = riskColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "RISK INDEX",
                                    color = TextGray,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Divider(
                            color = CyberDarkSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        Text(
                            text = "AI Threat Reasonings:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberPrimaryGreen,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = result.reasoning,
                            color = OnCyberDarkSurface,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Absolute Action Plan:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTertiaryAmber,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result.recommendedAction,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        // Caution notice for Prototype key insertion
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Security Warning: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                            color = TextGray.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// --- AI SECURITY TERMINAL ASSISTANT ---
@Composable
fun TerminalTab(
    viewModel: SecurityViewModel,
    assistantChat: List<Pair<String, String>>,
    isAnalyzing: Boolean
) {
    var userMessage by remember { mutableStateOf("") }
    val spacingHelper = remember { mutableStateOf(true) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            border = BorderStroke(1.dp, CyberDarkSurfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(CyberPrimaryGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "PERSONAL CYBER SECURITY ASSISTANT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Query about URLs, investment messages, romance baits or fake job listings.",
                        fontSize = 10.sp,
                        color = TextGray
                    )
                }
            }
        }

        // Chat terminal area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            reverseLayout = true
        ) {
            // Display chatting entries in anti-chronological view mapping standard UX
            val itemsToDisplay = assistantChat.reversed()
            items(itemsToDisplay) { (role, body) ->
                val isAssistant = role == "assistant"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isAssistant) Arrangement.Start else Arrangement.End
                ) {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isAssistant) 2.dp else 12.dp,
                            bottomEnd = if (isAssistant) 12.dp else 2.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAssistant) CyberDarkSurface else CyberPrimaryGreen
                        ),
                        border = BorderStroke(1.dp, if (isAssistant) CyberDarkSurfaceVariant else CyberPrimaryGreen),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .testTag("chat_msg_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isAssistant) "GUARDIAN CORE" else "YOU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAssistant) CyberPrimaryGreen else CyberDarkBackground,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = body,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = if (isAssistant) Color.White else CyberDarkBackground
                            )
                        }
                    }
                }
            }
        }

        if (isAnalyzing) {
            LinearProgressIndicator(
                color = CyberPrimaryGreen,
                trackColor = CyberDarkSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .padding(bottom = 8.dp)
            )
        }

        // Message Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                placeholder = { Text("Is this investment genuine?", color = TextGray) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (userMessage.isNotBlank()) {
                        viewModel.sendAssistantMessage(userMessage)
                        userMessage = ""
                        keyboardController?.hide()
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberPrimaryGreen,
                    unfocusedBorderColor = CyberDarkSurfaceVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (userMessage.isNotBlank()) {
                        viewModel.sendAssistantMessage(userMessage)
                        userMessage = ""
                        keyboardController?.hide()
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .background(CyberPrimaryGreen, RoundedCornerShape(12.dp))
                    .border(1.dp, CyberPrimaryGreen, RoundedCornerShape(12.dp))
                    .testTag("chat_send_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "send message",
                    tint = CyberDarkBackground
                )
            }
        }
    }
}

// --- COMMUNITY / FRAUD INTEL TAB ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityTab(
    viewModel: SecurityViewModel,
    reportedScams: List<ReportedScamItem>
) {
    var showReportSheet by remember { mutableStateOf(false) }

    // Report sheet fields
    var reporterEmail by remember { mutableStateOf("") }
    var scamTheme by remember { mutableStateOf("UPI Request Fraud") }
    var scamTitle by remember { mutableStateOf("") }
    var scamDescription by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, CyberDarkSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "FRAUD INTELLIGENCE NETWORK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberSecondaryBlue,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Anonymous crowdsourced database built by cyber analysts. Report phishing URLs or scam caller numbers here to protect other citizens worldwide.",
                            color = TextGray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "LATEST CROWDSOURCED ALERTS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            if (reportedScams.isEmpty()) {
                item {
                    // Seed standard elements showing value
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                        border = BorderStroke(0.5.dp, CyberDarkSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Secure Ledger is empty.",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    viewModel.submitScamReport(
                                        "citizen@defender.net",
                                        "UPI / UPI auto-debit Scams",
                                        "Fake electricity bill auto-debit request",
                                        "WhatsApp text calling to avoid power cut by scanning QR requesting automatic debit of 15000 INR."
                                    )
                                    viewModel.submitScamReport(
                                        "intel@scam.org",
                                        "Phishing Link",
                                        "Fake Netflix account suspension warning",
                                        "Phishing URL linked within email: netflix-renew-subscription.com"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberDarkSurfaceVariant)
                            ) {
                                Text("Load Community Seed logs", color = CyberSecondaryBlue, fontSize = 11.sp)
                            }
                        }
                    }
                }
            } else {
                items(reportedScams) { scam ->
                    ScamReportCard(scam = scam, onUpvote = { viewModel.upvoteReportedScam(scam.id) })
                }
            }
        }

        // High contrast float CTA to post report
        FloatingActionButton(
            onClick = { showReportSheet = true },
            containerColor = CyberPrimaryGreen,
            contentColor = CyberDarkBackground,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("report_scam_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Report Scam")
        }

        if (showReportSheet) {
            ModalBottomSheet(
                onDismissRequest = { showReportSheet = false },
                containerColor = CyberDarkSurface,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "ANONYMOUS CYBER SCAM REPORT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = CyberPrimaryGreen,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = reporterEmail,
                        onValueChange = { reporterEmail = it },
                        placeholder = { Text("Email (optional, private)", color = TextGray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimaryGreen,
                            unfocusedBorderColor = CyberDarkSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    var dropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = scamTheme,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Scam Category", color = TextGray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dropdownExpanded = true },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, "down", tint = CyberPrimaryGreen, modifier = Modifier.clickable { dropdownExpanded = true })
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberPrimaryGreen,
                                unfocusedBorderColor = CyberDarkSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(CyberDarkSurface)
                        ) {
                            val categories = listOf(
                                "UPI Auto-Debit Hack",
                                "Phishing Link",
                                "Romance Scam Wallet",
                                "Job Offer Bait",
                                "Emergency Helpline Impersonator"
                            )
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = Color.White) },
                                    onClick = {
                                        scamTheme = cat
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = scamTitle,
                        onValueChange = { scamTitle = it },
                        placeholder = { Text("Title: e.g. Fake Amazon Refund text calling...", color = TextGray) },
                        modifier = Modifier.fillMaxWidth().testTag("scam_title_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimaryGreen,
                            unfocusedBorderColor = CyberDarkSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = scamDescription,
                        onValueChange = { scamDescription = it },
                        placeholder = { Text("Provide details: phone numbers, links, malicious UPI codes, text details...", color = TextGray) },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("scam_details_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimaryGreen,
                            unfocusedBorderColor = CyberDarkSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Button(
                        onClick = {
                            if (scamTitle.isNotBlank() && scamDescription.isNotBlank()) {
                                viewModel.submitScamReport(
                                    email = reporterEmail,
                                    scamType = scamTheme,
                                    title = scamTitle,
                                    details = scamDescription
                                )
                                showReportSheet = false
                                reporterEmail = ""
                                scamTitle = ""
                                scamDescription = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimaryGreen),
                        modifier = Modifier.fillMaxWidth().testTag("scam_submit_btn")
                    ) {
                        Text(
                            text = "Anonymously Commit to Threat Base",
                            color = CyberDarkBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScamReportCard(scam: ReportedScamItem, onUpvote: () -> Unit) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dateStr = formatter.format(Date(scam.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberDarkSurfaceVariant),
        modifier = Modifier.fillMaxWidth().testTag("scam_report_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(CyberSecondaryBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = scam.scamType,
                        color = CyberSecondaryBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = dateStr,
                    color = TextGray,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = scam.scamTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = scam.scamDetails,
                fontSize = 12.sp,
                color = OnCyberDarkSurface.copy(alpha = 0.8f),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "reporter",
                        tint = CyberTertiaryAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reported Anonymously",
                        fontSize = 10.sp,
                        color = TextGray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onUpvote() }
                        .background(CyberDarkSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("scam_upvote_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Upvote",
                        tint = CyberPrimaryGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Vouch (${scam.upvotes})",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
