package com.example.data.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Struct to hold structured response
    data class AnalysisResult(
        val riskScore: Int,
        val riskCategory: String, // Safe, Low Risk, Medium Risk, High Risk, Critical
        val reasoning: String,
        val recommendedAction: String
    ) {
        companion object {
            // Safe fallback
            fun fallback(reason: String) = AnalysisResult(
                riskScore = 0,
                riskCategory = "Safe",
                reasoning = reason,
                recommendedAction = "Stay vigilant of suspicious activities."
            )
        }
    }

    /**
     * Helper to encode Bitmap to base64
     */
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Generic query function that returns raw text or parsed JSON
     */
    suspend fun analyzeTextOrImage(
        prompt: String,
        bitmap: Bitmap? = null,
        isJsonResponse: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured or using default placeholder.")
            return@withContext if (isJsonResponse) {
                """{
                    "riskScore": 45,
                    "riskCategory": "Medium Risk",
                    "reasoning": "[Demo Mode] No API Key inserted in Secrets panel. To enable real-time live AI scans, please enter your GEMINI_API_KEY in the Google AI Studio Secrets panel.\n\nSimulated scan detected standard urgency tags in your query.",
                    "recommendedAction": "Configure your GEMINI_API_KEY in the Secrets panel."
                }"""
            } else {
                "[Demo Mode] Please configure your GEMINI_API_KEY in AI Studio's Secrets panel to experience personalized real-time security assistant answering."
            }
        }

        val requestUrl = "$BASE_URL?key=$apiKey"
        
        try {
            // Build the contents block
            val partsArray = JSONArray()
            
            // Add user text prompt
            val textPart = JSONObject().put("text", prompt)
            partsArray.put(textPart)

            // Add image inlineData if present
            if (bitmap != null) {
                val imagePart = JSONObject().put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", bitmap.toBase64())
                })
                partsArray.put(imagePart)
            }

            val contentObj = JSONObject().put("parts", partsArray)
            val contentsArray = JSONArray().put(contentObj)

            // Dynamic payload
            val rootObj = JSONObject().put("contents", contentsArray)

            // Create generationConfig if structured JSON format requested
            if (isJsonResponse) {
                val generationConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                }
                rootObj.put("generationConfig", generationConfig)
            }

            val requestBodyJson = rootObj.toString()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val okRequestBody = requestBodyJson.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(requestUrl)
                .post(okRequestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini API Request failed: Code ${response.code}, Body: $errBody")
                    throw Exception("API Error: ${response.code} $errBody")
                }

                val responseBodyText = response.body?.string()
                    ?: throw Exception("Empty response from AI engine")
                
                val parsedJson = JSONObject(responseBodyText)
                val candidates = parsedJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text")
                        }
                    }
                }
                throw Exception("Response text not found in response block")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying Gemini API", e)
            if (isJsonResponse) {
                """{
                    "riskScore": 0,
                    "riskCategory": "Safe",
                    "reasoning": "Connection or configuration error: ${e.localizedMessage}. Please double check your internet connection.",
                    "recommendedAction": "Verify connection & try again."
                }"""
            } else {
                "Unable to reach security assistant. Error: ${e.localizedMessage}. Check internet connection."
            }
        }
    }

    /**
     * Parses the response into AnalysisResult
     */
    fun parseAnalysisResult(jsonStr: String): AnalysisResult {
        return try {
            val json = JSONObject(jsonStr)
            val riskScore = json.optInt("riskScore", 0)
            val riskCategory = json.optString("riskCategory", "Unknown")
            val reasoning = json.optString("reasoning", "No details provided.")
            val recommendedAction = json.optString("recommendedAction", "No recommendation provided.")
            AnalysisResult(riskScore, riskCategory, reasoning, recommendedAction)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Analysis JSON: $jsonStr", e)
            AnalysisResult.fallback("Failed to parse response details. Raw content:\n$jsonStr")
        }
    }

    /**
     * System guideline prompts for each scanner module to enforce structured JSON output.
     */
    private val screenshotSystemPrompt = """
        You are a Cybersecurity Screenshot Analyzer. Identify if this screenshot contains phishing elements, scam messages, fake bank alerts, crypto scams, or malicious URLs. Extract phone numbers, URLs, payment requests, or banking details.
        You MUST return a JSON object with this exact schema:
        {
          "riskScore": Int (0 to 100, where 0 is perfectly safe and 100 is highly malicious),
          "riskCategory": "Safe" or "Low Risk" or "Medium Risk" or "High Risk" or "Critical",
          "reasoning": "A paragraph explaining exactly what suspicious wording, layout, domain, or phone numbers make it unsafe. Highlight specific parts.",
          "recommendedAction": "Clear advice on what to do next (e.g. Do not click, report and delete immediately)."
        }
    """.trimIndent()

    private val chatSystemPrompt = """
        You are a Cybersecurity Chat Auditor. Diagnose the pasted conversational text (from WhatsApp, Telegram, SMS, or Messenger) for online frauds (OTP scams, fake investments, high-yield crypto claims, romance scams, fake job recruitments, or emergency cash loan baits).
        You MUST return a JSON object with this exact schema:
        {
          "riskScore": Int (0 to 100),
          "riskCategory": "Safe" or "Low Risk" or "Medium Risk" or "High Risk" or "Critical",
          "reasoning": "A highly detailed breakdown explaining specific lines or phrases that signal coercion, extreme urgency, too-good-to-be-true rewards, or pressure tactics.",
          "recommendedAction": "Clear, practical action. (e.g. Block user, report number to authorities, do not share OTP, run security sweep)."
        }
    """.trimIndent()

    private val urlSystemPrompt = """
        You are an AI Threat URL Intelligence Scanner. Investigate the specified domain link for phish attempts, brand impersonation, deceptive directories, or malware indicators. 
        You MUST return a JSON object with this exact schema:
        {
          "riskScore": Int (0 to 100),
          "riskCategory": "Safe" or "Low Risk" or "Medium Risk" or "High Risk" or "Critical",
          "reasoning": "Identify the potential risks of this URL. Mention brand impersonation indicators, typical phishing subdirectory structures, SSL, and domain health warnings.",
          "recommendedAction": "Actionable instructions (e.g., Avoid entering credentials, block link, keep browser upgraded)."
        }
    """.trimIndent()

    private val qrSystemPrompt = """
        You are a QR Code Security Inspector. Analyze this uploaded QR code asset (or image containing a QR code). Check for malicious redirects, UPI auto-payment requests, or scam addresses.
        You MUST return a JSON object with this exact schema:
        {
          "riskScore": Int (0 to 100),
          "riskCategory": "Safe" or "Low Risk" or "Medium Risk" or "High Risk" or "Critical",
          "reasoning": "Detail what risk is associated with the QR code. Spot UPI payment links requesting automatic debit, or phishing URLs.",
          "recommendedAction": "How to proceed safely (e.g., Decline payment request, avoid scans in untrusted portals)."
        }
    """.trimIndent()

    private val apkSystemPrompt = """
        You are an Android APK Malware Auditor. The user specifies details about an APK (name, size, package, requested permissions, trackers, or general properties). Inspect if these permissions resemble spyware, banking trojans, SMS stealers, or Accessibility Service abuse.
        You MUST return a JSON object with this exact schema:
        {
          "riskScore": Int (0 to 100),
          "riskCategory": "Safe" or "Low Risk" or "Medium Risk" or "High Risk" or "Critical",
          "reasoning": "Review permissions like RECEIVE_SMS, READ_SMS, BIND_ACCESSIBILITY_SERVICE, REQUEST_INSTALL_PACKAGES, overlay drawing, and evaluate if this is potentially adware, ransomware, or a banking credential stealer.",
          "recommendedAction": "Actionable security recommendation (e.g. Uninstall immediately, deny overlay rights)."
        }
    """.trimIndent()

    private val voiceSystemPrompt = """
        You are an AI Voice Scam and Deepfake Analyst. You will analyze transcription text representing a voicemail recording or suspicious phone call. Detect high-pressure tactics, synthetic deepfake wording patterns, voice clone indicators, or emergency imposter scenarios.
        You MUST return a JSON object with this exact schema:
        {
          "riskScore": Int (0 to 100),
          "riskCategory": "Safe" or "Low Risk" or "Medium Risk" or "High Risk" or "Critical",
          "reasoning": "Analyze key triggers. For example: requests for instant gift card codes, emotional manipulation, family member kidnap claims, or imposter tech-support instructions.",
          "recommendedAction": "Immediate advice (e.g., Hang up, call the relative back on their real number directly, report voice signature)."
        }
    """.trimIndent()

    private val emailSystemPrompt = """
        You are an Email Phishing and Invoice Fraud Scanner. Analyze the email contents. Look for sender spoofing giveaways, fake billing/invoice scams, high-pressure deadlines, or suspicious attachments.
        You MUST return a JSON object with this exact schema:
        {
          "riskScore": Int (0 to 100),
          "riskCategory": "Safe" or "Low Risk" or "Medium Risk" or "High Risk" or "Critical",
          "reasoning": "Expose urgency triggers, link discrepancies, generic greetings, and false urgency regarding account deactivations or invoice settlements.",
          "recommendedAction": "Safety recommendations (e.g., Do not download attachment, mark sender as spam, verify with physical department)."
        }
    """.trimIndent()

    /**
     * Interface methods
     */
    suspend fun scanScreenshot(bitmap: Bitmap): AnalysisResult {
        val prompt = "$screenshotSystemPrompt\n\nAnalyze this screenshot now."
        val jsonText = analyzeTextOrImage(prompt = prompt, bitmap = bitmap, isJsonResponse = true)
        return parseAnalysisResult(jsonText)
    }

    suspend fun scanChat(chatText: String): AnalysisResult {
        val prompt = "$chatSystemPrompt\n\nChat text to analyze:\n$chatText"
        val jsonText = analyzeTextOrImage(prompt = prompt, bitmap = null, isJsonResponse = true)
        return parseAnalysisResult(jsonText)
    }

    suspend fun scanUrl(url: String): AnalysisResult {
        val prompt = "$urlSystemPrompt\n\nURL link to investigate:\n$url"
        val jsonText = analyzeTextOrImage(prompt = prompt, bitmap = null, isJsonResponse = true)
        return parseAnalysisResult(jsonText)
    }

    suspend fun scanQr(bitmap: Bitmap?, description: String): AnalysisResult {
        val prompt = "$qrSystemPrompt\n\nQR details or description:\n$description"
        val jsonText = analyzeTextOrImage(prompt = prompt, bitmap = bitmap, isJsonResponse = true)
        return parseAnalysisResult(jsonText)
    }

    suspend fun scanApk(apkDetails: String): AnalysisResult {
        val prompt = "$apkSystemPrompt\n\nAPK details, name, or metadata provided by user:\n$apkDetails"
        val jsonText = analyzeTextOrImage(prompt = prompt, bitmap = null, isJsonResponse = true)
        return parseAnalysisResult(jsonText)
    }

    suspend fun scanVoice(voiceTranscript: String): AnalysisResult {
        val prompt = "$voiceSystemPrompt\n\nVoice transcription transcript or details:\n$voiceTranscript"
        val jsonText = analyzeTextOrImage(prompt = prompt, bitmap = null, isJsonResponse = true)
        return parseAnalysisResult(jsonText)
    }

    suspend fun scanEmail(emailText: String): AnalysisResult {
        val prompt = "$emailSystemPrompt\n\nEmail body text to audit:\n$emailText"
        val jsonText = analyzeTextOrImage(prompt = prompt, bitmap = null, isJsonResponse = true)
        return parseAnalysisResult(jsonText)
    }

    suspend fun askSecurityAssistant(question: String, history: List<Pair<String, String>>): String {
        val historyPrompt = StringBuilder()
        history.forEach { (role, content) ->
            historyPrompt.append("${role.uppercase()}: $content\n")
        }
        val prompt = """
            You are "Scam Guardian", a knowledgeable Cyber Defence Security Assistant. Your mission is to protect regular internet users (students, seniors, online shoppers) from dangerous digital traps. Help them understand internet vulnerabilities, tell them if actions are safe, and keep advice friendly, simple, and action-oriented.
            
            Conversation History:
            $historyPrompt
            
            USER CURRENT INQUIRY:
            $question
            
            Provide a clear, formatted explanation. State clearly if the action described by the user is safe or risky right at the beginning. Use bullet points for readability.
        """.trimIndent()
        return analyzeTextOrImage(prompt = prompt, bitmap = null, isJsonResponse = false)
    }
}
