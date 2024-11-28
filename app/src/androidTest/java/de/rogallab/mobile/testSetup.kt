package de.rogallab.mobile

import androidx.room.Room
import de.rogallab.mobile.data.local.IPersonDao
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.local.database.SeedDatabase
import de.rogallab.mobile.data.local.seed.Seed
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.utilities.logInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataTestModules = module {
   val tag = "<-dataTestModules"

   single<TestCoroutineScheduler> {
      TestCoroutineScheduler().apply {  this.advanceUntilIdle() }
   }
   single<TestDispatcher> {
      StandardTestDispatcher( get<TestCoroutineScheduler>())
   }

   single<CoroutineScope> {
      CoroutineScope(SupervisorJob() + get<TestDispatcher>())
   }

   logInfo(tag, "single    -> Seed")
   single<Seed> {
      Seed(
         androidContext(),
         androidContext().resources
      ).createPerson(false)
   }

   logInfo(tag, "single    -> SeedDatabase")
   single<SeedDatabase> {
      SeedDatabase(
         _database = get<AppDatabase>(),
         _personDao = get<IPersonDao>(),
         _seed = get<Seed>(),
         _coroutineDispatcher = get<TestDispatcher>(),
      )
   }

   logInfo(tag, "single    -> AppDatabase")
   single<AppDatabase> {
      Room.inMemoryDatabaseBuilder(
         androidContext(),
         AppDatabase::class.java
      ).build()
//    Room.databaseBuilder(
//       context = androidContext(),
//       klass = AppDatabase::class.java,
//       name = AppStart.DATABASENAME+"_Test"
//    ).build()
   }

   logInfo(tag, "single    -> IPersonDao")
   single<IPersonDao> { get<AppDatabase>().createPersonDao() }

   // Provide IPersonRepository
   logInfo(tag, "single    -> PersonRepository: IPersonRepository")
   single<IPersonRepository> {
      PersonRepository(
         _personDao = get<IPersonDao>(),
         _coroutineDispatcher = get<TestDispatcher>()
      )
   }
}