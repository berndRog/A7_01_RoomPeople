package de.rogallab.mobile.ui.people.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.ui.errors.ErrorParams
import de.rogallab.mobile.ui.errors.ErrorState
import de.rogallab.mobile.ui.errors.showError
import de.rogallab.mobile.ui.navigation.NavEvent
import de.rogallab.mobile.ui.navigation.NavScreen
import de.rogallab.mobile.ui.people.PeopleViewModel
import de.rogallab.mobile.ui.people.PersonIntent
import de.rogallab.mobile.ui.people.PersonUiState
import de.rogallab.mobile.ui.people.PersonValidator
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
   viewModel: PeopleViewModel = koinViewModel(),
   validator: PersonValidator = koinInject(),
   isInputScreen: Boolean = true,
   id: String? = null
) {
   // is screen used as InputScreen to create a new person
   // or as DetailScreen to update a person
   val isInputMode: Boolean by rememberSaveable { mutableStateOf(isInputScreen) }

   val screenTitle = if (isInputMode) stringResource(R.string.personInput)
   else stringResource(R.string.personDetail)
   val tag = if (isInputMode) "<-PersonInputScreen"
   else "<-PersonDetailScreen"

   // DetailScreen
   if (!isInputMode) {
      id?.let { it: String ->
         LaunchedEffect(Unit) {
            viewModel.onProcessPersonIntent(PersonIntent.FetchById(it))
         }
      } ?: run {
         viewModel.onErrorEvent(
            ErrorParams(
               message = "No id for person is given",
               navEvent = NavEvent.NavigateBack(NavScreen.PeopleList.route)
            )
         )
      }
   }

   // Observe the PersonUiState
   val personUiState: PersonUiState
      by viewModel.personUiStateFlow.collectAsStateWithLifecycle()

   BackHandler{
      viewModel.onNavigate(NavEvent.NavigateBack(NavScreen.PeopleList.route))
   }

   val windowInsets = WindowInsets.systemBars
      .add(WindowInsets.captionBar)
      .add(WindowInsets.ime)
      .add(WindowInsets.safeGestures)

   val snackbarHostState = remember { SnackbarHostState() }

   Scaffold(
      modifier = Modifier
         .fillMaxSize()
         .padding(windowInsets.asPaddingValues())
         .background(color = MaterialTheme.colorScheme.surface),
      topBar = {
         TopAppBar(
            title = { Text(text = screenTitle) },
            navigationIcon = {
               IconButton(onClick = {
                  logDebug(tag, "Reverse navigation -> PeopleList")
                  if(viewModel.validate(isInputMode) )
                     viewModel.onNavigate(NavEvent.NavigateReverse(NavScreen.PeopleList.route))
               }) {
                  Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                     contentDescription = stringResource(R.string.back))
               }
            }
         )
      },
      snackbarHost = {
         SnackbarHost(hostState = snackbarHostState) { data ->
            Snackbar(
               snackbarData = data,
               actionOnNewLine = true
            )
         }
      }) { paddingValues: PaddingValues ->
      Column(
         modifier = Modifier
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding() // padding for the bottom for the IME
      ) {
         InputName(
            name = personUiState.person.firstName,          // State ↓
            onNameChange = { firstName: String ->           // Event ↑
               viewModel.onProcessPersonIntent(PersonIntent.FirstNameChange(firstName)) },
            label = stringResource(R.string.firstName),            // State ↓
            validateName = validator::validateFirstName     // Event ↑ no state change
         )
         InputName(
            name = personUiState.person.lastName,           // State ↓
            onNameChange = { lastName: String ->            // Event ↑
               viewModel.onProcessPersonIntent(PersonIntent.LastNameChange(lastName)) },
            label = stringResource(R.string.lastName),             // State ↓
            validateName = validator::validateLastName,     // Event ↑ no state change
         )
         InputEmail(
            email = personUiState.person.email ?: "",             // State ↓
            onEmailChange = { email:String ->               // Event ↑
               viewModel.onProcessPersonIntent(PersonIntent.EmailChange(email)) },
            validateEmail = validator::validateEmail        // Event ↑ no state change
         )
         InputPhone(
            phone = personUiState.person.phone ?: "",             // State ↓
            onPhoneChange = { phone:String ->               // Event ↑
               viewModel.onProcessPersonIntent(PersonIntent.PhoneChange(phone)) },
            validatePhone = validator::validatePhone        // Event ↑ no state change
         )

         SelectAndShowImage(
            imageUrl = personUiState.person.imagePath,     // State ↓viewModel.imagePath,                          // State ↓
            onImageUrlChange = { Unit }                    // Event ↑
         )

      } // Column
   } // Scaffold

   val errorState: ErrorState
      by viewModel.errorStateFlow.collectAsStateWithLifecycle()
   LaunchedEffect(errorState.params) {
      errorState.params?.let { params: ErrorParams ->
         // show the error with a snackbar
         showError(snackbarHostState, params,
            viewModel::onNavigate, viewModel::onErrorEventHandled)
      }
   }
}