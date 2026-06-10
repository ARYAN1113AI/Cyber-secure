package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.SecurityRepository

class ScamGuardianApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: SecurityRepository by lazy { SecurityRepository(database.securityDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ScamGuardianApplication
            private set
    }
}
