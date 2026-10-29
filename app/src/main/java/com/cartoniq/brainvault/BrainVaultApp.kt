package com.cartoniq.brainvault

import android.app.Application
import com.cartoniq.brainvault.ads.AdManager
import com.cartoniq.brainvault.data.database.BrainDatabase
import com.cartoniq.brainvault.data.repository.BrainRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BrainVaultApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database by lazy { BrainDatabase.build(this) }
    val repository by lazy { BrainRepository(database) }
    val adManager by lazy { AdManager(this) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch { repository.ensureProfile() }
        adManager.init()
    }
}
