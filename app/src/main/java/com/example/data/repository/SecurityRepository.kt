package com.example.data.repository

import com.example.data.local.dao.SecurityDao
import com.example.data.local.model.ScanHistoryItem
import com.example.data.local.model.ReportedScamItem
import kotlinx.coroutines.flow.Flow

class SecurityRepository(private val securityDao: SecurityDao) {
    val scanHistory: Flow<List<ScanHistoryItem>> = securityDao.getAllScanHistory()
    val reportedScams: Flow<List<ReportedScamItem>> = securityDao.getAllReportedScams()

    suspend fun insertScanHistory(item: ScanHistoryItem): Long {
        return securityDao.insertScanHistory(item)
    }

    suspend fun deleteScanHistoryById(id: Long) {
        securityDao.deleteScanHistoryById(id)
    }

    suspend fun clearScanHistory() {
        securityDao.clearScanHistory()
    }

    suspend fun reportScam(item: ReportedScamItem): Long {
        return securityDao.reportScam(item)
    }

    suspend fun upvoteScam(id: Long) {
        securityDao.upvoteScam(id)
    }

    suspend fun deleteReportedScamById(id: Long) {
        securityDao.deleteReportedScamById(id)
    }
}
