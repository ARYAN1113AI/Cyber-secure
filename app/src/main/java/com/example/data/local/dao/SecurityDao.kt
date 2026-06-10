package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.model.ScanHistoryItem
import com.example.data.local.model.ReportedScamItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityDao {
    // Scan History Queries
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScanHistory(): Flow<List<ScanHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanHistory(item: ScanHistoryItem): Long

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteScanHistoryById(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun clearScanHistory()

    // Reported Scams Queries (Community Intelligence Network)
    @Query("SELECT * FROM reported_scams ORDER BY timestamp DESC")
    fun getAllReportedScams(): Flow<List<ReportedScamItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun reportScam(item: ReportedScamItem): Long

    @Query("UPDATE reported_scams SET upvotes = upvotes + 1 WHERE id = :id")
    suspend fun upvoteScam(id: Long)

    @Query("DELETE FROM reported_scams WHERE id = :id")
    suspend fun deleteReportedScamById(id: Long)
}
