package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminProfile
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.HalfDaySession
import com.example.data.model.ITI_RAIGARH_TRADE_TEMPLATES
import com.example.data.model.LeaveApplication
import com.example.data.model.LeaveType
import com.example.data.model.Trade
import com.example.data.model.TraineeNotification
import com.example.data.model.TraineeRecord
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.ItiAttendanceRepository
import com.example.util.ExportFormat
import com.example.util.ExportedReportResult
import com.example.util.GeneratedReportData
import com.example.util.ReportExporter
import com.example.util.ReportType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class ItiAppViewModel(
  private val repository: ItiAttendanceRepository,
  private val currentUserId: String
) : ViewModel() {

  private val todayDateStr: String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  private val currentMonthStr: String =
    SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

  private val _selectedDate = MutableStateFlow(todayDateStr)
  val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

  private val _selectedMonth = MutableStateFlow(currentMonthStr)
  val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

  private val _selectedTraineeFilter = MutableStateFlow("ALL")
  val selectedTraineeFilter: StateFlow<String> = _selectedTraineeFilter.asStateFlow()

  private val _selectedReportType = MutableStateFlow(ReportType.DAILY_ATTENDANCE)
  val selectedReportType: StateFlow<ReportType> = _selectedReportType.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  private val _lastExportedReport = MutableStateFlow<ExportedReportResult?>(null)
  val lastExportedReport: StateFlow<ExportedReportResult?> = _lastExportedReport.asStateFlow()

  // 1. Observe Current User Profile
  val userProfileState: StateFlow<UiState<UserProfile?>> =
    repository.observeUserProfile(currentUserId)
      .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
      .catch { error ->
        Log.w(TAG, "Error observing userProfile", error)
        emit(UiState.Error(error.message ?: "Failed to load user profile"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
      )

  // 2. Observe Admin Profile
  val adminProfileState: StateFlow<UiState<AdminProfile?>> =
    repository.observeAdminProfile(currentUserId)
      .map<AdminProfile?, UiState<AdminProfile?>> { UiState.Success(it) }
      .catch { error ->
        Log.w(TAG, "Error observing adminProfile", error)
        emit(UiState.Error(error.message ?: "Failed to load admin profile"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
      )

  // 3. Observe Assigned Trade
  val tradeState: StateFlow<UiState<Trade?>> =
    userProfileState.flatMapLatest { state ->
      val profile = (state as? UiState.Success)?.data
      if (profile == null || profile.tradeId.isBlank()) {
        flowOf<UiState<Trade?>>(UiState.Success(null))
      } else {
        repository.observeTrade(profile.tradeId)
          .map<Trade?, UiState<Trade?>> { UiState.Success(it) }
          .catch { emit(UiState.Success(null)) }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  // 4. Observe Trainee Record for Current User (when in Trainee role)
  val myTraineeRecordState: StateFlow<UiState<TraineeRecord?>> =
    repository.observeTraineeByUid(currentUserId)
      .map<TraineeRecord?, UiState<TraineeRecord?>> { UiState.Success(it) }
      .catch { error ->
        Log.w(TAG, "Error observing trainee record", error)
        emit(UiState.Error(error.message ?: "Failed to load trainee record"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
      )

  // 5. Observe All Trainees in Admin's Assigned Trade
  val tradeTraineesState: StateFlow<UiState<List<TraineeRecord>>> =
    userProfileState.flatMapLatest { state ->
      val profile = (state as? UiState.Success)?.data
      if (profile == null || profile.userRole != UserRole.ADMIN) {
        flowOf<UiState<List<TraineeRecord>>>(UiState.Success(emptyList()))
      } else {
        repository.observeTraineesForTrade(currentUserId, profile.tradeId)
          .map<List<TraineeRecord>, UiState<List<TraineeRecord>>> { UiState.Success(it) }
          .catch { error ->
            Log.w(TAG, "Error observing trade trainees", error)
            emit(UiState.Error(error.message ?: "Failed to load trainees for trade"))
          }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  // 6. Observe Attendance Records (Scoped by Role & Trade)
  val attendanceListState: StateFlow<UiState<List<AttendanceRecord>>> =
    userProfileState.flatMapLatest { state ->
      val profile = (state as? UiState.Success)?.data
      when {
        profile == null -> flowOf(UiState.Success(emptyList()))
        profile.userRole == UserRole.ADMIN -> {
          repository.observeAttendanceForTrade(currentUserId, profile.tradeId)
            .map<List<AttendanceRecord>, UiState<List<AttendanceRecord>>> { UiState.Success(it) }
            .catch { error ->
              Log.w(TAG, "Error observing trade attendance", error)
              emit(UiState.Error(error.message ?: "Failed to load trade attendance"))
            }
        }
        else -> {
          repository.observeAttendanceForTrainee(currentUserId)
            .map<List<AttendanceRecord>, UiState<List<AttendanceRecord>>> { UiState.Success(it) }
            .catch { error ->
              Log.w(TAG, "Error observing trainee attendance", error)
              emit(UiState.Error(error.message ?: "Failed to load attendance"))
            }
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  // 7. Observe Leave Applications (Scoped by Role & Trade)
  val leavesListState: StateFlow<UiState<List<LeaveApplication>>> =
    userProfileState.flatMapLatest { state ->
      val profile = (state as? UiState.Success)?.data
      when {
        profile == null -> flowOf(UiState.Success(emptyList()))
        profile.userRole == UserRole.ADMIN -> {
          repository.observeLeavesForTrade(currentUserId, profile.tradeId)
            .map<List<LeaveApplication>, UiState<List<LeaveApplication>>> { UiState.Success(it) }
            .catch { error ->
              Log.w(TAG, "Error observing trade leaves", error)
              emit(UiState.Error(error.message ?: "Failed to load leave applications"))
            }
        }
        else -> {
          repository.observeLeavesForTrainee(currentUserId)
            .map<List<LeaveApplication>, UiState<List<LeaveApplication>>> { UiState.Success(it) }
            .catch { error ->
              Log.w(TAG, "Error observing trainee leaves", error)
              emit(UiState.Error(error.message ?: "Failed to load leaves"))
            }
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  // 8. Observe Notifications
  val notificationsState: StateFlow<UiState<List<TraineeNotification>>> =
    userProfileState.flatMapLatest { state ->
      val profile = (state as? UiState.Success)?.data
      when {
        profile == null -> flowOf(UiState.Success(emptyList()))
        profile.userRole == UserRole.ADMIN -> {
          repository.observeNotificationsForTrade(currentUserId, profile.tradeId)
            .map<List<TraineeNotification>, UiState<List<TraineeNotification>>> { UiState.Success(it) }
            .catch { error ->
              Log.w(TAG, "Error observing trade notifications", error)
              emit(UiState.Error(error.message ?: "Failed to load notifications"))
            }
        }
        else -> {
          repository.observeNotificationsForTrainee(currentUserId)
            .map<List<TraineeNotification>, UiState<List<TraineeNotification>>> { UiState.Success(it) }
            .catch { error ->
              Log.w(TAG, "Error observing trainee notifications", error)
              emit(UiState.Error(error.message ?: "Failed to load notifications"))
            }
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  // --- FILTER SETTERS ---

  fun setSelectedDate(date: String) {
    _selectedDate.value = date
    if (date.length >= 7) {
      _selectedMonth.value = date.take(7)
    }
  }

  fun setSelectedMonth(month: String) {
    _selectedMonth.value = month
  }

  fun setSelectedTraineeFilter(traineeId: String) {
    _selectedTraineeFilter.value = traineeId
  }

  fun setSelectedReportType(reportType: ReportType) {
    _selectedReportType.value = reportType
  }

  fun clearBanners() {
    _statusMessage.value = null
    _errorMessage.value = null
  }

  fun clearExportedReport() {
    _lastExportedReport.value = null
  }

  // --- ONBOARDING & ROLE ACTIONS ---

  fun completeAdminOnboarding(
    fullName: String,
    employeeCode: String,
    tradeId: String,
    tradeName: String,
    tradeCode: String,
    sessionYear: String,
    seedSampleData: Boolean
  ) {
    if (fullName.trim().length < 2) {
      _errorMessage.value = "Please enter your full name (at least 2 characters)."
      return
    }
    if (employeeCode.trim().length < 2) {
      _errorMessage.value = "Please enter your Employee / Officer ID."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      clearBanners()
      val result = repository.registerTrainingOfficer(
        fullName = fullName,
        employeeCode = employeeCode,
        tradeId = tradeId,
        tradeName = tradeName,
        tradeCode = tradeCode,
        sessionYear = sessionYear
      )
      result.onSuccess { profile ->
        if (seedSampleData) {
          repository.seedSampleRosterForTrade(
            tradeId = profile.tradeId,
            tradeName = profile.tradeName,
            sessionYear = profile.sessionYear,
            officerName = profile.fullName,
            todayDate = todayDateStr
          )
        }
        _statusMessage.value = "Welcome, Training Officer ${profile.fullName}! Assigned Trade: ${profile.tradeName}."
      }.onFailure { err ->
        _errorMessage.value = err.message ?: "Failed to register Training Officer profile."
      }
      _isBusy.value = false
    }
  }

  fun completeTraineeOnboarding(
    fullName: String,
    registrationNumber: String,
    fatherName: String,
    batchShift: String,
    tradeId: String,
    tradeName: String,
    sessionYear: String
  ) {
    if (fullName.trim().length < 2) {
      _errorMessage.value = "Please enter your full name."
      return
    }
    if (registrationNumber.trim().length < 2) {
      _errorMessage.value = "Please enter your ITI Registration / Roll Number."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      clearBanners()
      val result = repository.registerTraineeUser(
        fullName = fullName,
        registrationNumber = registrationNumber,
        fatherName = fatherName.ifBlank { "Guardian" },
        batchShift = batchShift,
        tradeId = tradeId,
        tradeName = tradeName,
        sessionYear = sessionYear
      )
      result.onSuccess { profile ->
        _statusMessage.value = "Welcome, ${profile.fullName}! Enrolled in ${profile.tradeName} (${profile.sessionYear})."
      }.onFailure { err ->
        _errorMessage.value = err.message ?: "Failed to register Trainee profile."
      }
      _isBusy.value = false
    }
  }

  fun switchUserRole(targetRole: UserRole) {
    val currentProfile = (userProfileState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      _isBusy.value = true
      clearBanners()
      repository.switchRoleForCurrentUser(currentProfile, targetRole)
        .onSuccess {
          _statusMessage.value = "Switched to ${if (targetRole == UserRole.ADMIN) "Training Officer (Admin)" else "Trainee"} Portal."
        }
        .onFailure { err ->
          _errorMessage.value = err.message ?: "Could not switch role."
        }
      _isBusy.value = false
    }
  }

  // --- TRAINEE ACTIONS ---

  fun markTodayAttendanceAsTrainee(
    trainee: TraineeRecord,
    status: AttendanceStatus,
    remarks: String
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      clearBanners()
      repository.markDailyAttendanceByTrainee(
        trainee = trainee,
        dateString = todayDateStr,
        status = status,
        remarks = remarks.ifBlank { "Marked via Trainee App" }
      ).onSuccess {
        _statusMessage.value = "Today's attendance ($todayDateStr) marked as ${status.label}!"
      }.onFailure { err ->
        _errorMessage.value = if (err.message?.contains("PERMISSION_DENIED") == true) {
          "Attendance for today ($todayDateStr) has already been recorded. Duplicate attendance is not permitted."
        } else {
          err.message ?: "Could not mark attendance."
        }
      }
      _isBusy.value = false
    }
  }

  fun resetTodayAttendanceAsTrainee(trainee: TraineeRecord) {
    viewModelScope.launch {
      _isBusy.value = true
      clearBanners()
      repository.resetTodayAttendanceForTrainee(trainee, todayDateStr)
        .onSuccess {
          _statusMessage.value = "Today's attendance ($todayDateStr) reset to Unmarked (automatically counted as Absent until marked at college)."
        }
        .onFailure { err ->
          _errorMessage.value = err.message ?: "Could not reset today's attendance."
        }
      _isBusy.value = false
    }
  }

  fun submitLeaveRequest(
    trainee: TraineeRecord,
    leaveType: LeaveType,
    startDate: String,
    endDate: String,
    totalDays: Int,
    halfDaySession: HalfDaySession,
    reason: String
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      clearBanners()
      repository.submitLeaveApplication(
        trainee = trainee,
        leaveType = leaveType,
        startDate = startDate,
        endDate = endDate,
        totalDays = totalDays,
        halfDaySession = halfDaySession,
        reason = reason
      ).onSuccess { app ->
        _statusMessage.value = "${app.parsedLeaveType.label} application submitted for approval!"
      }.onFailure { err ->
        _errorMessage.value = err.message ?: "Failed to submit leave application."
      }
      _isBusy.value = false
    }
  }

  fun markNotificationRead(notificationId: String) {
    viewModelScope.launch {
      repository.markNotificationAsRead(notificationId)
    }
  }

  // --- ADMIN ACTIONS ---

  fun createNewTrainee(
    fullName: String,
    registrationNumber: String,
    fatherName: String,
    batchShift: String
  ) {
    val profile = (userProfileState.value as? UiState.Success)?.data ?: return
    if (fullName.trim().length < 2 || registrationNumber.trim().length < 2) {
      _errorMessage.value = "Trainee Name and Registration Number are required."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      clearBanners()
      repository.createTraineeInTrade(
        tradeId = profile.tradeId,
        tradeName = profile.tradeName,
        sessionYear = profile.sessionYear,
        fullName = fullName,
        registrationNumber = registrationNumber,
        fatherName = fatherName.ifBlank { "Guardian" },
        batchShift = batchShift.ifBlank { "Shift I (Morning)" }
      ).onSuccess { created ->
        _statusMessage.value = "Trainee ${created.fullName} (${created.registrationNumber}) added to ${profile.tradeName}."
      }.onFailure { err ->
        _errorMessage.value = err.message ?: "Failed to create trainee account."
      }
      _isBusy.value = false
    }
  }

  fun updateTraineeDetails(
    traineeId: String,
    fullName: String,
    registrationNumber: String,
    fatherName: String,
    batchShift: String,
    sessionYear: String,
    isActive: Boolean
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      clearBanners()
      repository.updateTraineeAccount(
        traineeId = traineeId,
        fullName = fullName,
        registrationNumber = registrationNumber,
        fatherName = fatherName,
        batchShift = batchShift,
        sessionYear = sessionYear,
        isActive = isActive
      ).onSuccess {
        _statusMessage.value = "Updated trainee account for $fullName."
      }.onFailure { err ->
        _errorMessage.value = err.message ?: "Failed to update trainee account."
      }
      _isBusy.value = false
    }
  }

  fun markTraineeAttendanceByAdmin(
    trainee: TraineeRecord,
    dateString: String,
    newStatus: AttendanceStatus,
    existingRecord: AttendanceRecord?,
    remarks: String = "Marked by Training Officer"
  ) {
    viewModelScope.launch {
      clearBanners()
      repository.markOrUpdateAttendanceByAdmin(
        trainee = trainee,
        dateString = dateString,
        newStatus = newStatus,
        existingRecord = existingRecord,
        remarks = remarks
      ).onSuccess {
        _statusMessage.value = "${trainee.fullName} marked ${newStatus.label} for $dateString."
      }.onFailure { err ->
        _errorMessage.value = err.message ?: "Could not update attendance."
      }
    }
  }

  fun markAllUnmarkedPresentForDate(
    trainees: List<TraineeRecord>,
    attendanceForDate: List<AttendanceRecord>,
    dateString: String
  ) {
    val markedIds = attendanceForDate.map { it.traineeId }.toSet()
    val unmarkedActive = trainees.filter { it.isActive && it.traineeId !in markedIds }
    if (unmarkedActive.isEmpty()) {
      _statusMessage.value = "All active trainees already have attendance recorded for $dateString."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      clearBanners()
      var count = 0
      for (trainee in unmarkedActive) {
        val res = repository.markOrUpdateAttendanceByAdmin(
          trainee = trainee,
          dateString = dateString,
          newStatus = AttendanceStatus.PRESENT,
          existingRecord = null,
          remarks = "Batch Roll Call"
        )
        if (res.isSuccess) count++
      }
      _statusMessage.value = "Marked $count trainees as Present for $dateString."
      _isBusy.value = false
    }
  }

  fun markAllUnmarkedAbsentForDate(
    trainees: List<TraineeRecord>,
    attendanceForDate: List<AttendanceRecord>,
    dateString: String
  ) {
    val markedIds = attendanceForDate.map { it.traineeId }.toSet()
    val unmarkedActive = trainees.filter { it.isActive && it.traineeId !in markedIds }
    if (unmarkedActive.isEmpty()) {
      _statusMessage.value = "All active trainees already have attendance recorded for $dateString."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      clearBanners()
      var count = 0
      for (trainee in unmarkedActive) {
        val res = repository.markOrUpdateAttendanceByAdmin(
          trainee = trainee,
          dateString = dateString,
          newStatus = AttendanceStatus.ABSENT,
          existingRecord = null,
          remarks = "Auto-Marked Absent: Not marked by trainee on college arrival"
        )
        if (res.isSuccess) count++
      }
      _statusMessage.value = "Auto-marked $count unmarked trainees as Absent for $dateString."
      _isBusy.value = false
    }
  }

  fun reviewLeave(
    leave: LeaveApplication,
    trainee: TraineeRecord?,
    approve: Boolean,
    adminRemarks: String
  ) {
    val profile = (userProfileState.value as? UiState.Success)?.data ?: return
    if (trainee == null) {
      _errorMessage.value = "Trainee record not found for this leave application."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      clearBanners()
      repository.reviewLeaveApplication(
        leave = leave,
        trainee = trainee,
        approve = approve,
        adminRemarks = adminRemarks,
        officerName = profile.fullName
      ).onSuccess {
        val actionWord = if (approve) "Approved" else "Rejected"
        _statusMessage.value = "$actionWord ${leave.parsedLeaveType.label} for ${leave.traineeName}."
      }.onFailure { err ->
        _errorMessage.value = err.message ?: "Failed to review leave application."
      }
      _isBusy.value = false
    }
  }

  fun sendLowAttendanceWarning(
    trainee: TraineeRecord,
    customMessage: String
  ) {
    val profile = (userProfileState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      _isBusy.value = true
      clearBanners()
      repository.issueLowAttendanceWarning(
        trainee = trainee,
        warningMessage = customMessage,
        officerName = profile.fullName
      ).onSuccess {
        _statusMessage.value = "Low Attendance Warning issued to ${trainee.fullName} (${trainee.attendancePercentage}%)."
      }.onFailure { err ->
        _errorMessage.value = err.message ?: "Failed to issue low attendance warning."
      }
      _isBusy.value = false
    }
  }

  fun updateTradeSettings(
    tradeName: String,
    tradeCode: String,
    sessionYear: String,
    totalWorkingDays: Int,
    maxCasualLeave: Int,
    maxMedicalLeave: Int,
    halfDayRule: String,
    maxHalfDayLeaves: Int,
    minAttendancePercent: Int
  ) {
    val profile = (userProfileState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      _isBusy.value = true
      clearBanners()
      repository.updateTradeConfiguration(
        tradeId = profile.tradeId,
        tradeName = tradeName,
        tradeCode = tradeCode,
        adminName = profile.fullName,
        sessionYear = sessionYear,
        totalWorkingDays = totalWorkingDays,
        maxCasualLeave = maxCasualLeave,
        maxMedicalLeave = maxMedicalLeave,
        halfDayDeductionRule = halfDayRule,
        maxHalfDayLeaves = maxHalfDayLeaves,
        minAttendancePercent = minAttendancePercent
      ).onSuccess {
        _statusMessage.value = "Updated Trade & Academic Session configuration ($sessionYear)."
      }.onFailure { err ->
        _errorMessage.value = err.message ?: "Failed to update Trade configuration."
      }
      _isBusy.value = false
    }
  }

  fun seedSampleTraineesIfNeeded() {
    val profile = (userProfileState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      _isBusy.value = true
      clearBanners()
      repository.seedSampleRosterForTrade(
        tradeId = profile.tradeId,
        tradeName = profile.tradeName,
        sessionYear = profile.sessionYear,
        officerName = profile.fullName,
        todayDate = todayDateStr
      ).onSuccess {
        _statusMessage.value = "Sample trainees, attendance, and leave requests loaded for ${profile.tradeName}."
      }.onFailure { err ->
        _errorMessage.value = err.message ?: "Could not seed sample trainees."
      }
      _isBusy.value = false
    }
  }

  // --- REPORT GENERATION & DOWNLOAD ---

  fun generateAndDownloadReport(
    context: Context,
    format: ExportFormat,
    reportData: GeneratedReportData
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      clearBanners()
      ReportExporter.exportReport(context, reportData, format)
        .onSuccess { exported ->
          _lastExportedReport.value = exported
          _statusMessage.value = "Saved ${exported.format.label} to ${exported.savedLocationDescription}"
        }
        .onFailure { err ->
          _errorMessage.value = "Report export failed: ${err.message}"
        }
      _isBusy.value = false
    }
  }

  companion object {
    private const val TAG = "ItiAppVM"
    private const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
