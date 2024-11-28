package de.rogallab.mobile.ui.people

import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.ResultData
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.as8
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.domain.utilities.logError
import de.rogallab.mobile.domain.utilities.logInfo
import de.rogallab.mobile.ui.base.BaseViewModel
import de.rogallab.mobile.ui.errors.ErrorParams
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PeopleViewModel(
   private val _repository: IPersonRepository,
   private val _validator: PersonValidator
) : BaseViewModel(TAG) {

   private var removedPerson: Person? = null

   // Refresh scenario
   private val reloadTrigger = MutableSharedFlow<Unit>(replay = 1)
   init {
      //fetch()
   }

   // ===============================
   // S T A T E   C H A N G E S
   // ===============================
   // Data Binding PeopleListScreen <=> PersonViewModel
   private val _peopleUiStateFlow = MutableStateFlow(PeopleUiState())
   //val peopleUiStateFlow = _peopleUiStateFlow.asStateFlow()

   // transform intent into an action
   fun onProcessPeopleIntent(intent: PeopleIntent) {
      logInfo(TAG, "onProcessIntent: $intent")
      when (intent) {
         is PeopleIntent.Fetch -> { } //fetch()
      }
   }

   // Basic Scenario: Use the stateIn() operator to convert a Flow from the data source into a State
   //
   // Using stateIn means that we only collect data when the UI is actually observing.
   // This keeps things lightweight and prevents unnecessary data collection
   val peopleUiStateFlow: StateFlow<PeopleUiState> = _repository.getAll()
      .map { resultData ->
         when (resultData) {
            is ResultData.Success -> {
               _peopleUiStateFlow.update { it: PeopleUiState ->
                  it.copy(people = resultData.data.toList())
               }
               logDebug(TAG, "fetch() people: ${_peopleUiStateFlow.value.people.size}")
            }
            is ResultData.Error -> {
               onErrorEvent(ErrorParams(throwable = resultData.throwable, navEvent = null))
            }
         }
         return@map _peopleUiStateFlow.value
      }.stateIn(
         scope = viewModelScope,
         started = SharingStarted.WhileSubscribed(5000),
         initialValue = PeopleUiState()
      )


   // Refreshable Scenario
   // Sometimes, we need a refreshable Flow (swipe to refresh, or a refresh button).
   // In this case, a MutableSharedFlow paired with flatMapLatest can help:
   // MutableSharedFlow: This is our control point for triggering a reload. It only stores
   // the latest event.
   // flatMapLatest: It listens for the latest data and cancels any previous collector when
   // a new one comes in. We only use the most recent collection. No leaks. Just clean data.

   @OptIn(ExperimentalCoroutinesApi::class)
   val peopleUiStateFlowNotUsed: StateFlow<PeopleUiState> = reloadTrigger.flatMapLatest {
      _repository.getAll()
         .map { resultData ->
            when (resultData) {
               is ResultData.Success -> {
                  _peopleUiStateFlow.update { it: PeopleUiState ->
                     it.copy(people = resultData.data.toList())
                  }
                  logDebug(TAG, "fetch() people: ${_peopleUiStateFlow.value.people.size}")
               }
               is ResultData.Error -> {
                  onErrorEvent(ErrorParams(throwable = resultData.throwable, navEvent = null))
               }
            }
            return@map _peopleUiStateFlow.value
         }
   }.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(),
      PeopleUiState()
   )

   fun fetch() {
      viewModelScope.launch {
         reloadTrigger.emit(Unit)
      }
   }

// read all people from repository
// Every time fetch is called, we add another collector. Over time, you end up with a lot
// of collectors all collecting the same data, leading to memory leaks and performance issues.
//   fun fetch() {
//      viewModelScope.launch(exceptionHandler) {
//         _repository.getAll().collect { resultData ->
//            when (resultData) {
//               is ResultData.Success -> {
//                  _peopleUiStateFlow.update { it: PeopleUiState ->
//                     it.copy(people = resultData.data.toList())
//                  }
//                  logDebug(TAG, "fetch() people: ${peopleUiStateFlow.value.people.size}")
//               }
//               is ResultData.Error -> {
//                  onErrorEvent(ErrorParams(throwable = resultData.throwable, navEvent = null))
//               }
//            }
//         }
//      }
//   }

   // Data Binding PersonScreen <=> PersonViewModel
   private val _personUiStateFlow = MutableStateFlow(PersonUiState())
   val personUiStateFlow = _personUiStateFlow.asStateFlow()

   // transform intent into an action
   fun onProcessPersonIntent(intent: PersonIntent) {
      logInfo(TAG, "onProcessIntent: $intent")
      when (intent) {
         is PersonIntent.FirstNameChange -> onFirstNameChange(intent.firstName)
         is PersonIntent.LastNameChange -> onLastNameChange(intent.lastName)
         is PersonIntent.EmailChange -> onEmailChange(intent.email)
         is PersonIntent.PhoneChange -> onPhoneChange(intent.phone)

         is PersonIntent.Clear -> clearState()

         is PersonIntent.FetchById -> fetchById(intent.id)
         is PersonIntent.Create -> create()
         is PersonIntent.Update -> update()
         is PersonIntent.Remove -> remove(intent.person)
      }
   }

   private fun onFirstNameChange(firstName: String) {
      if (firstName == _personUiStateFlow.value.person.firstName) return
      _personUiStateFlow.update { it: PersonUiState ->
         it.copy(person = it.person.copy(firstName = firstName))
      }
   }
   private fun onLastNameChange(lastName: String) {
      if (lastName == _personUiStateFlow.value.person.lastName) return
      _personUiStateFlow.update { it: PersonUiState ->
         it.copy(person = it.person.copy(lastName = lastName))
      }
   }
   private fun onEmailChange(email: String?) {
      if (email == null || email == _personUiStateFlow.value.person.email) return
      _personUiStateFlow.update { it: PersonUiState ->
         it.copy(person = it.person.copy(email = email))
      }
   }
   private fun onPhoneChange(phone: String?) {
      if (phone == null || phone == _personUiStateFlow.value.person.phone) return
      _personUiStateFlow.update { it: PersonUiState ->
         it.copy(person = it.person.copy(phone = phone))
      }
   }

   private fun fetchById(personId: String) {
      logDebug(TAG, "fetchPersonById: $personId")

      viewModelScope.launch(exceptionHandler) {
         when (val resultData = _repository.getById(personId)) {
            is ResultData.Success -> _personUiStateFlow.update { it: PersonUiState ->
               it.copy(person = resultData.data ?: Person())  // new UiState
            }
            is ResultData.Error ->
               onErrorEvent(ErrorParams(throwable = resultData.throwable, navEvent = null))
         }
      }
   }
   private fun create() {
      logDebug(TAG, "createPerson: ${_personUiStateFlow.value.person.id.as8()}")
      viewModelScope.launch(exceptionHandler) {
         when (val resultData = _repository.create(_personUiStateFlow.value.person)) {
            is ResultData.Success -> fetch()
            is ResultData.Error ->
               onErrorEvent(ErrorParams(throwable = resultData.throwable, navEvent = null))
         }
      }
   }
   private fun update() {
      logDebug(TAG, "updatePerson: ${_personUiStateFlow.value.person.id.as8()}")
      viewModelScope.launch(exceptionHandler) {
         when (val resultData = _repository.update(_personUiStateFlow.value.person)) {
            is ResultData.Success -> fetch()
            is ResultData.Error ->
               onErrorEvent(ErrorParams(throwable = resultData.throwable, navEvent = null))
         }
      }
   }
   private fun remove(person: Person) {
      logDebug(TAG, "removePerson: ${person.id.as8()}")
      viewModelScope.launch(exceptionHandler) {
         when (val resultData = _repository.remove(person)) {
            is ResultData.Success -> {
               removedPerson = person
               fetch()
            }
            is ResultData.Error ->
               onErrorEvent(ErrorParams(throwable = resultData.throwable, navEvent = null))
         }
      }
   }

   fun undoRemove() {
      removedPerson?.let { person ->
         logDebug(TAG, "undoRemovePerson: ${person.id.as8()}")
         viewModelScope.launch(exceptionHandler) {
            when (val resultData = _repository.create(person)) {
               is ResultData.Success -> {
                  removedPerson = null
                  fetch()
               }
               is ResultData.Error ->
                  onErrorEvent(ErrorParams(throwable = resultData.throwable, navEvent = null))
            }
         }
      }
   }

   private fun clearState() {
      _personUiStateFlow.update { it.copy(person = Person()) }
   }

   // =========================================
   // V A L I D A T E   I N P U T   F I E L D S
   // =========================================
   // validate all input fields after user finished input into the form
   fun validate(isInput: Boolean): Boolean {
      val person = _personUiStateFlow.value.person

      if(validateAndLogError(_validator.validateFirstName(person.firstName)) &&
         validateAndLogError(_validator.validateLastName(person.lastName)) &&
         validateAndLogError(_validator.validateEmail(person.email)) &&
         validateAndLogError(_validator.validatePhone(person.phone))
      ) {
         // write data to repository
         if (isInput) this.create()
         else         this.update()
         return true
      } else {
         return false
      }
   }

   private fun validateAndLogError(validationResult: Pair<Boolean, String>): Boolean {
      val (error, message) = validationResult
      if (error) {
         onErrorEvent(ErrorParams(message = message, navEvent = null))
         logError(TAG, message)
         return false
      }
      return true
   }

   companion object {
      private const val TAG = "<-PeopleViewModel"
   }
}