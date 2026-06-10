package com.example.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // screenshot, chat, url, qr, apk, voice, email, assistant
    val inputData: String, // the original email text, URL, file description, etc.
    val riskScore: Int, // 0 to 100
    val riskCategory: String, // Safe, Low Risk, Medium Risk, High Risk, Critical
    val reasoning: String, // Full analysis from Gemini
    val recommendedAction: String // Advice provided to user
)

@Entity(tableName = "reported_scams")
data class ReportedScamItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val reporterEmail: String,
    val scamType: String, // UPI Scam, Phishing, Fake Job, Romance, Impersonation
    val scamTitle: String,
    val scamDetails: String,
    val upvotes: Int = 1,
    val isVerified: Boolean = false
)
