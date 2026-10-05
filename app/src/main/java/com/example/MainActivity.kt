package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.LocalItiStore
import com.example.data.model.UserRole
import com.example.data.repository.ItiAttendanceRepository
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.screens.AdminPortalScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.OnboardingRoleSetupScreen
import com.example.ui.screens.TraineePortalScreen
import com.example.ui.theme.ItiPrimaryBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ItiAppViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    LocalItiStore.init(applicationContext)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        AppNavigation()
      }
    }
  }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val context = LocalContext.current
  remember(context) {
    LocalItiStore.init(context.applicationContext)
    true
  }

  val localAccount by LocalItiStore.currentAccount.collectAsStateWithLifecycle()
  val firebaseUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

  val activeUserId = localAccount?.userId ?: firebaseUser?.uid
  val activeDisplayName = localAccount?.displayName ?: firebaseUser?.displayName ?: ""

  if (activeUserId == null) {
    AuthScreen(
      onAuthSuccess = { /* LocalItiStore.currentAccount updates automatically */ }
    )
  } else {
    AuthenticatedAppContent(
      currentUserId = activeUserId,
      defaultDisplayName = activeDisplayName
    )
  }
}

@Composable
private fun AuthenticatedAppContent(
  currentUserId: String,
  defaultDisplayName: String
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: ItiAppViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        ItiAppViewModel(ItiAttendanceRepository(db), currentUserId)
      }
    }
  )

  val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val tradeState by viewModel.tradeState.collectAsStateWithLifecycle()
  val myTraineeRecordState by viewModel.myTraineeRecordState.collectAsStateWithLifecycle()
  val tradeTraineesState by viewModel.tradeTraineesState.collectAsStateWithLifecycle()
  val attendanceListState by viewModel.attendanceListState.collectAsStateWithLifecycle()
  val leavesListState by viewModel.leavesListState.collectAsStateWithLifecycle()
  val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()

  val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
  val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
  val selectedTraineeFilter by viewModel.selectedTraineeFilter.collectAsStateWithLifecycle()
  val selectedReportType by viewModel.selectedReportType.collectAsStateWithLifecycle()
  val lastExportedReport by viewModel.lastExportedReport.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
  val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
  val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

  val handleSignOut: () -> Unit = {
    LocalItiStore.signOut()
    if (Firebase.auth.currentUser != null) {
      GoogleAuthHelper.signOut(
        credentialManager = credentialManager,
        onSignOutComplete = {},
        scope = scope
      )
    }
  }

  when (val profileState = userProfileState) {
    is UiState.Loading -> {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = ItiPrimaryBlue)
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Loading Government ITI Raigarh Portal...",
            style = MaterialTheme.typography.bodyMedium
          )
        }
      }
    }

    is UiState.Error -> {
      OnboardingRoleSetupScreen(
        defaultDisplayName = defaultDisplayName,
        isBusy = isBusy,
        statusMessage = statusMessage,
        errorMessage = profileState.message,
        onDismissBanner = viewModel::clearBanners,
        onCompleteAdminSetup = viewModel::completeAdminOnboarding,
        onCompleteTraineeSetup = viewModel::completeTraineeOnboarding,
        onSignOut = handleSignOut
      )
    }

    is UiState.Success -> {
      val profile = profileState.data
      if (profile == null) {
        OnboardingRoleSetupScreen(
          defaultDisplayName = defaultDisplayName,
          isBusy = isBusy,
          statusMessage = statusMessage,
          errorMessage = errorMessage,
          onDismissBanner = viewModel::clearBanners,
          onCompleteAdminSetup = viewModel::completeAdminOnboarding,
          onCompleteTraineeSetup = viewModel::completeTraineeOnboarding,
          onSignOut = handleSignOut
        )
      } else {
        val trade = (tradeState as? UiState.Success)?.data
        val attendanceList = (attendanceListState as? UiState.Success)?.data ?: emptyList()
        val leavesList = (leavesListState as? UiState.Success)?.data ?: emptyList()
        val notificationsList = (notificationsState as? UiState.Success)?.data ?: emptyList()

        if (profile.userRole == UserRole.ADMIN) {
          val tradeTrainees = (tradeTraineesState as? UiState.Success)?.data ?: emptyList()
          AdminPortalScreen(
            userProfile = profile,
            trade = trade,
            trainees = tradeTrainees,
            attendanceList = attendanceList,
            leavesList = leavesList,
            selectedDate = selectedDate,
            selectedMonth = selectedMonth,
            selectedTraineeFilter = selectedTraineeFilter,
            selectedReportType = selectedReportType,
            lastExportedReport = lastExportedReport,
            isBusy = isBusy,
            statusMessage = statusMessage,
            errorMessage = errorMessage,
            onDismissBanner = viewModel::clearBanners,
            onSelectDate = viewModel::setSelectedDate,
            onSelectMonth = viewModel::setSelectedMonth,
            onSelectTraineeFilter = viewModel::setSelectedTraineeFilter,
            onSelectReportType = viewModel::setSelectedReportType,
            onCreateTrainee = viewModel::createNewTrainee,
            onUpdateTrainee = { id, name, reg, father, shift, active ->
              viewModel.updateTraineeDetails(id, name, reg, father, shift, profile.sessionYear, active)
            },
            onMarkTraineeAttendance = { trainee, date, status, existing ->
              viewModel.markTraineeAttendanceByAdmin(trainee, date, status, existing)
            },
            onMarkAllPresentForDate = viewModel::markAllUnmarkedPresentForDate,
            onReviewLeave = viewModel::reviewLeave,
            onIssueWarning = viewModel::sendLowAttendanceWarning,
            onUpdateTradeSettings = viewModel::updateTradeSettings,
            onSeedSampleTrainees = viewModel::seedSampleTraineesIfNeeded,
            onDownloadReport = { format, reportData ->
              viewModel.generateAndDownloadReport(context, format, reportData)
            },
            onSwitchToTraineeMode = { viewModel.switchUserRole(UserRole.TRAINEE) },
            onSignOut = handleSignOut
          )
        } else {
          val myTrainee = (myTraineeRecordState as? UiState.Success)?.data
          TraineePortalScreen(
            userProfile = profile,
            traineeRecord = myTrainee,
            attendanceList = attendanceList,
            leavesList = leavesList,
            notificationsList = notificationsList,
            selectedMonth = selectedMonth,
            isBusy = isBusy,
            statusMessage = statusMessage,
            errorMessage = errorMessage,
            onDismissBanner = viewModel::clearBanners,
            onSelectMonth = viewModel::setSelectedMonth,
            onMarkTodayAttendance = viewModel::markTodayAttendanceAsTrainee,
            onSubmitLeave = viewModel::submitLeaveRequest,
            onMarkNotificationRead = viewModel::markNotificationRead,
            onSwitchToAdminMode = { viewModel.switchUserRole(UserRole.ADMIN) },
            onSignOut = handleSignOut
          )
        }
      }
    }
  }
}
