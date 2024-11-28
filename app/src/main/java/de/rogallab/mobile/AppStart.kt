package de.rogallab.mobile

import android.app.Application
import de.rogallab.mobile.data.local.database.SeedDatabase
import de.rogallab.mobile.domain.utilities.logInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AppStart : Application() {

   // Define a CoroutineScope with a SupervisorJob for long-running application-wide tasks
   private val _applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

   override fun onCreate() {
      super.onCreate()
      logInfo(TAG, "onCreate()")

      logInfo(TAG, "onCreate(): startKoin{...}")
      startKoin {
         // Log Koin into Android logger
         androidLogger(Level.DEBUG)
         // Reference Android context
         androidContext(this@AppStart)
         // Load modules
         modules(domainModules, dataModules, uiModules)
      }

      val seedDatabase: SeedDatabase by inject()
      _applicationScope.launch() {
         seedDatabase.seedPerson()
      }
   }

   companion object {
      const val ISINFO = true
      const val ISDEBUG = true
      const val ISVERBOSE = true
      const val DATABASENAME = "db_7_01"
      const val DATABASEVERSION = 1

      private const val TAG = "<-AppStart"
   }
}