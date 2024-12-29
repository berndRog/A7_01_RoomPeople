package de.rogallab.mobile.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.ResultData
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.domain.utilities.logError
import de.rogallab.mobile.domain.utilities.logInfo
import de.rogallab.mobile.domain.utilities.newUuid
import de.rogallab.mobile.ui.IErrorHandler
import de.rogallab.mobile.ui.INavigationHandler
import de.rogallab.mobile.ui.errors.ErrorHandler
import de.rogallab.mobile.ui.errors.ErrorParams
import kotlinx.coroutines.CoroutineExceptionHandler
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
   private val _validator: PersonValidator,
   private val _navigationHandler: INavigationHandler,
   private val _errorHandler: IErrorHandler,
   private val _exceptionHandler: CoroutineExceptionHandler
) : ViewModel(),
   INavigationHandler by _navigationHandler,
   IErrorHandler by _errorHandler {

   //region PeopleListScreen -------------------------------------------------------------------------------
   private val _peopleUiStateFlow = MutableStateFlow(PeopleUiState())
   //val peopleUiStateFlow = _peopleUiStateFlow.asStateFlow()

   // transform intent into an action
   fun onProcessPeopleIntent(intent: PeopleIntent) {
      logInfo(TAG, "onProcessIntent: $intent")
      when (intent) {
         is PeopleIntent.Fetch -> {} //fetch()
      }
   }

   // Basic Scenario: Use the stateIn() operator to convert a Flow from the data source into a State
   val peopleUiStateFlow: StateFlow<PeopleUiState> =
      _repository.selectAll().map { resultData ->
         when (resultData) {
            is ResultData.Success -> _peopleUiStateFlow.update { it: PeopleUiState ->
               it.copy(people = resultData.data.toList())
            }
            is ResultData.Error -> handleErrorEvent(resultData.throwable)
         }
         return@map _peopleUiStateFlow.value
      }.stateIn(
         scope = viewModelScope,
         started = SharingStarted.WhileSubscribed(5000),
         initialValue = PeopleUiState()
      )

   // Refreshable Scenario
   private val reloadTrigger = MutableSharedFlow<Unit>(replay = 1)
   init {
      // fetchTrigger()
   }
   @OptIn(ExperimentalCoroutinesApi::class)
   val peopleUiStateFlowNotUsed: StateFlow<PeopleUiState> = reloadTrigger.flatMapLatest {
      _repository.selectAll()
         .map { resultData ->
            when (resultData) {
               is ResultData.Success -> _peopleUiStateFlow.update { it: PeopleUiState ->
                  it.copy(people = resultData.data.toList())
               }
               is ResultData.Error -> handleErrorEvent(resultData.throwable)
            }
            return@map _peopleUiStateFlow.value
         }
   }.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(),
      PeopleUiState()
   )

   fun fetchTrigger() {
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
   //endregion


   //region PersonScreen --------------------------------------------------------------------------------
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
         is PersonIntent.UndoRemove -> undoRemove()
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
      viewModelScope.launch(_exceptionHandler) {
         when (val resultData = _repository.findById(personId)) {
            is ResultData.Success -> _personUiStateFlow.update { it: PersonUiState ->
               it.copy(person = resultData.data ?: Person(id = newUuid()))  // new UiState
            }
            is ResultData.Error -> handleErrorEvent(resultData.throwable)
         }
      }
   }

   private fun create() {
      logDebug(TAG, "createPerson()")
      viewModelScope.launch(_exceptionHandler) {
         when (val resultData = _repository.insert(_personUiStateFlow.value.person)) {
            is ResultData.Success -> {}
            is ResultData.Error -> handleErrorEvent(resultData.throwable)
         }
      }
   }

   private fun update() {
      logDebug(TAG, "updatePerson()")
      viewModelScope.launch(_exceptionHandler) {
         when (val resultData = _repository.update(_personUiStateFlow.value.person)) {
            is ResultData.Success -> {}
            is ResultData.Error -> handleErrorEvent(resultData.throwable)
         }
      }
   }

   private var removedPerson: Person? = null
   private fun remove(person: Person) {
      logDebug(TAG, "removePerson()")
      viewModelScope.launch(_exceptionHandler) {
         when (val resultData = _repository.remove(person)) {
            is ResultData.Success -> removedPerson = person
            is ResultData.Error -> handleErrorEvent(resultData.throwable)
         }
      }
   }

   private fun undoRemove() {
      removedPerson?.let { person ->
         logDebug(TAG, "undoRemovePerson()")
         viewModelScope.launch(_exceptionHandler) {
            when (val resultData = _repository.insert(person)) {
               is ResultData.Success -> removedPerson = null
               is ResultData.Error -> handleErrorEvent(resultData.throwable)
            }
         }
      }
   }
   private fun clearState() {
      _personUiStateFlow.update { it.copy(person = Person(id = newUuid())) }
   }
   //endregion

   //region Validate input fields ------------------------------------------------------------------------
   // validate all input fields after user finished input into the form
   fun validate(isInput: Boolean): Boolean {
      val person = _personUiStateFlow.value.person

      if (validateAndLogError(_validator.validateFirstName(person.firstName)) &&
         validateAndLogError(_validator.validateLastName(person.lastName)) &&
         validateAndLogError(_validator.validateEmail(person.email)) &&
         validateAndLogError(_validator.validatePhone(person.phone))
      ) {
         // write data to repository
         if (isInput) this.create()
         else this.update()
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
   //endregion

   companion object {
      private const val TAG = "<-PeopleViewModel"
   }
}