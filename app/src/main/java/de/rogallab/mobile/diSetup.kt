package de.rogallab.mobile

import androidx.room.Room
import de.rogallab.mobile.data.local.IPersonDao
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.local.database.SeedDatabase
import de.rogallab.mobile.data.local.seed.Seed
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.utilities.logError
import de.rogallab.mobile.domain.utilities.logInfo
import de.rogallab.mobile.ui.people.PeopleViewModel
import de.rogallab.mobile.ui.people.PersonValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val uiModules: Module = module {
   val tag = "<-uiModules"

   logInfo(tag, "single    -> PersonValidator")
   single<PersonValidator> { PersonValidator(androidContext()) }

   logInfo(tag, "viewModel -> PeopleViewModel")
   viewModel<PeopleViewModel> {
      PeopleViewModel(
         _repository = get<IPersonRepository>(),
         _validator = get<PersonValidator>(),
         _exceptionHandler = get<CoroutineExceptionHandler>()
      )
   }
}

val domainModules: Module = module {
   val tag = "<-domainModules"

   logInfo(tag, "single    -> CoroutineExceptionHandler")
   single<CoroutineExceptionHandler> {
      CoroutineExceptionHandler { _, exception ->
         logError(tag, "Coroutine exception: ${exception.localizedMessage}")
      }
   }
   logInfo(tag, "single    -> named(DispatcherIO)")
   single<CoroutineDispatcher>(named("DispatcherIO")) { Dispatchers.IO }
   logInfo(tag, "single    -> named(DispatcherMain)")
   single<CoroutineDispatcher>(named("DispatcherMain")) { Dispatchers.Main }
}

val dataModules = module {
   val tag = "<-dataModules"

   logInfo(tag, "single    -> Seed")
   logInfo(tag, "single    -> Seed")
   single<Seed> {
      Seed(
         androidContext(),
         androidContext().resources
      )  .createPerson(true)
   }

   logInfo(tag, "single    -> SeedDatabase")
   single<SeedDatabase> {
      SeedDatabase(
         get<AppDatabase>(),
         get<IPersonDao>(),
         get<Seed>(),
         Dispatchers.IO,
      )
   }

   logInfo(tag, "single    -> AppDatabase")
   single {
      Room.databaseBuilder(
         androidContext(),
         AppDatabase::class.java,
         AppStart.DATABASE_NAME
      ).build()
   }

   logInfo(tag, "single    -> IPersonDao")
   single<IPersonDao> { get<AppDatabase>().createPersonDao() }

   // Provide IPersonRepository, injecting the `viewModelScope`
   logInfo(tag, "single    -> PersonRepository: IPersonRepository")
   single<IPersonRepository> {
      PersonRepository(
         get<IPersonDao>(),
         Dispatchers.IO
      )
   }
}
