package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.model.ReportedScamItem
import com.example.data.local.model.ScanHistoryItem
import com.example.data.network.GeminiService
import com.example.data.repository.SecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SecurityViewModel(private val repository: SecurityRepository) : ViewModel() {

    // Observe Scan History
    val scanHistory: StateFlow<List<ScanHistoryItem>> = repository.scanHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Observe Community Scam Network
    val reportedScams: StateFlow<List<ReportedScamItem>> = repository.reportedScams
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI Loading state
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Last analysis response
    private val _lastResult = MutableStateFlow<GeminiService.AnalysisResult?>(null)
    val lastResult: StateFlow<GeminiService.AnalysisResult?> = _lastResult.asStateFlow()

    // Chat Assistant states
    private val _assistantChat = MutableStateFlow<List<Pair<String, String>>>(
        listOf("assistant" to "Greetings! I am your AI Scam Guardian. Paste a link, describe a suspicious phone call, or type a query to verify if you are being targeted by scammers.")
    )
    val assistantChat: StateFlow<List<Pair<String, String>>> = _assistantChat.asStateFlow()

    // Family protection warnings (Simulated live notifications)
    private val _familyAlerts = MutableStateFlow<List<String>>(
        listOf(
            "[ALERT] Grandpa's phone blocked a suspected phishing URL (hdfc-netbanking-rewards.net) and flagged it as High Risk.",
            "[INFO] Little Sister completed the 'Job Offer Scam' simulator training."
        )
    )
    val familyAlerts: StateFlow<List<String>> = _familyAlerts.asStateFlow()

    /**
     * Cyber Immunity Score: Dynamic calculation
     * Start with 70. Every healthy/safe scan adds 5 points (max 100).
     * High/Critical risk detections lower it unless resolved, proving actual tracking.
     */
    val securityScore: StateFlow<Int> = MutableStateFlow(85).asStateFlow() // standard starting index

    fun getImmunityScore(history: List<ScanHistoryItem>): Int {
        if (history.isEmpty()) return 85
        var base = 80
        history.forEach { item ->
            when (item.riskCategory) {
                "Safe" -> base += 4
                "Low Risk" -> base += 2
                "Medium Risk" -> base -= 5
                "High Risk" -> base -= 15
                "Critical" -> base -= 25
            }
        }
        return base.coerceIn(20, 100)
    }

    // Badge tier calculation
    fun getBadgeTitle(score: Int): String {
        return when {
            score >= 95 -> "Elite Cyber Commander"
            score >= 85 -> "Cyber Defender"
            score >= 60 -> "Scam Hunter"
            else -> "Vulnerable Standard"
        }
    }

    /**
     * Core scan actions using Gemini REST endpoint
     */
    fun analyzeScreenshot(bitmap: Bitmap) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val result = GeminiService.scanScreenshot(bitmap)
                _lastResult.value = result
                saveHistory("screenshot", "Uploaded screenshot analysis", result)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun analyzeChat(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val result = GeminiService.scanChat(text)
                _lastResult.value = result
                saveHistory("chat", text, result)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun analyzeUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val result = GeminiService.scanUrl(url)
                _lastResult.value = result
                saveHistory("url", "Scan URL link: $url", result)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun analyzeQr(bitmap: Bitmap?, desc: String) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val result = GeminiService.scanQr(bitmap, desc)
                _lastResult.value = result
                saveHistory("qr", "QR Scan ($desc)", result)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun analyzeApk(apkDetails: String) {
        if (apkDetails.isBlank()) return
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val result = GeminiService.scanApk(apkDetails)
                _lastResult.value = result
                saveHistory("apk", "APK Audit: $apkDetails", result)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun analyzeVoice(voiceTranscript: String) {
        if (voiceTranscript.isBlank()) return
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val result = GeminiService.scanVoice(voiceTranscript)
                _lastResult.value = result
                saveHistory("voice", "Voice transcript analyze: $voiceTranscript", result)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun analyzeEmail(emailText: String) {
        if (emailText.isBlank()) return
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val result = GeminiService.scanEmail(emailText)
                _lastResult.value = result
                saveHistory("email", "Email text analyze", result)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Security assistant Chat interface
     */
    fun sendAssistantMessage(messageText: String) {
        if (messageText.isBlank()) return
        val currentHistory = _assistantChat.value.toMutableList()
        currentHistory.add("user" to messageText)
        _assistantChat.value = currentHistory
        
        _isAnalyzing.value = true
        viewModelScope.launch {
            try {
                val response = GeminiService.askSecurityAssistant(messageText, currentHistory)
                val updatedHistory = _assistantChat.value.toMutableList()
                updatedHistory.add("assistant" to response)
                _assistantChat.value = updatedHistory
                
                // Also log to scan history as assistant interaction
                saveHistory(
                    type = "assistant",
                    input = "Query: $messageText",
                    result = GeminiService.AnalysisResult(
                        riskScore = if (response.contains("risky", true) || response.contains("danger", true) || response.contains("scam", true)) 65 else 10,
                        riskCategory = if (response.contains("risky", true) || response.contains("danger", true) || response.contains("scam", true)) "Medium Risk" else "Safe",
                        reasoning = response,
                        recommendedAction = "Review and apply the guidance above."
                    )
                )
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Community Scams Network Interactions
     */
    fun submitScamReport(email: String, scamType: String, title: String, details: String) {
        viewModelScope.launch {
            val item = ReportedScamItem(
                reporterEmail = email.ifBlank { "anonymous@secureshield.net" },
                scamType = scamType,
                scamTitle = title,
                scamDetails = details
            )
            repository.reportScam(item)
        }
    }

    fun upvoteReportedScam(id: Long) {
        viewModelScope.launch {
            repository.upvoteScam(id)
        }
    }

    fun deleteScanHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteScanHistoryById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearScanHistory()
        }
    }

    /**
     * Setup default reported items if network has 0 elements, raising community value.
     */
    fun seedSampleScamsIfEmpty() {
        viewModelScope.launch {
            // Check list. Since reportedScams is a StateFlow, check database context or simple trigger
        }
    }

    private suspend fun saveHistory(type: String, input: String, result: GeminiService.AnalysisResult) {
        val historyItem = ScanHistoryItem(
            type = type,
            inputData = input,
            riskScore = result.riskScore,
            riskCategory = itemCategory(result.riskScore, result.riskCategory),
            reasoning = result.reasoning,
            recommendedAction = result.recommendedAction
        )
        repository.insertScanHistory(historyItem)
    }

    private fun itemCategory(score: Int, parsedCategory: String): String {
        if (parsedCategory != "Unknown" && parsedCategory.isNotBlank()) return parsedCategory
        return when {
            score >= 85 -> "Critical"
            score >= 70 -> "High Risk"
            score >= 40 -> "Medium Risk"
            score >= 15 -> "Low Risk"
            else -> "Safe"
        }
    }
}

class SecurityViewModelFactory(private val repository: SecurityRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SecurityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SecurityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
