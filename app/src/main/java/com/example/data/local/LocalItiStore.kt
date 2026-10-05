package com.example.data.local

import android.content.Context
import com.example.data.model.AdminProfile
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.HalfDaySession
import com.example.data.model.LeaveApplication
import com.example.data.model.LeaveStatus
import com.example.data.model.LeaveType
import com.example.data.model.NotificationType
import com.example.data.model.Trade
import com.example.data.model.TraineeNotification
import com.example.data.model.TraineeRecord
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.firebase.Timestamp
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

data class LocalAuthAccount(
  val userId: String,
  val email: String,
  val passwordHash: String,
  val displayName: String,
  val createdAtSeconds: Long = System.currentTimeMillis() / 1000
)

object LocalItiStore {

  private const val STORAGE_FILE_NAME = "iti_raigarh_local_store_v2.json"

  @Volatile
  private var appContext: Context? = null

  @Volatile
  private var isInitialized = false

  private val _currentAccount = MutableStateFlow<LocalAuthAccount?>(null)
  val currentAccount: StateFlow<LocalAuthAccount?> = _currentAccount.asStateFlow()

  val currentUserId: String?
    get() = _currentAccount.value?.userId

  private val accountsMap = MutableStateFlow<Map<String, LocalAuthAccount>>(emptyMap())
  private val adminsMap = MutableStateFlow<Map<String, AdminProfile>>(emptyMap())
  private val usersMap = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
  private val tradesMap = MutableStateFlow<Map<String, Trade>>(emptyMap())
  private val traineesMap = MutableStateFlow<Map<String, TraineeRecord>>(emptyMap())
  private val attendanceMap = MutableStateFlow<Map<String, AttendanceRecord>>(emptyMap())
  private val leavesMap = MutableStateFlow<Map<String, LeaveApplication>>(emptyMap())
  private val notificationsMap = MutableStateFlow<Map<String, TraineeNotification>>(emptyMap())

  @Synchronized
  fun init(context: Context) {
    if (isInitialized) return
    appContext = context.applicationContext
    loadFromDisk()
    if (accountsMap.value.isEmpty()) {
      seedDefaultDemoAccounts()
      saveToDisk()
    }
    isInitialized = true
  }

  private fun hashPassword(email: String, password: String): String {
    val normalized = "${email.trim().lowercase(Locale.US)}:iti_raigarh_salt:$password"
    val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }

  private fun cleanId(raw: String): String =
    raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifEmpty {
      "id_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    }

  /**
   * Unified Email + Password authentication:
   * - If [isCreateAccount] is true: creates a new account (or signs in if matching credentials).
   * - If [isCreateAccount] is false: signs in if account exists, or automatically creates the account
   *   if the email is new so the user can just enter an email ID and password to get started.
   */
  @Synchronized
  fun authenticateWithEmailAndPassword(
    email: String,
    password: String,
    confirmPassword: String? = null,
    isCreateAccount: Boolean = false
  ): Result<LocalAuthAccount> = runCatching {
    val trimmedEmail = email.trim().lowercase(Locale.US)
    require(trimmedEmail.isNotBlank() && trimmedEmail.contains("@") && trimmedEmail.contains(".")) {
      "Please enter a valid Email ID (for example: name@example.com)."
    }
    require(password.length >= 4) {
      "Password must be at least 4 characters long."
    }
    if (isCreateAccount && confirmPassword != null) {
      require(password == confirmPassword) {
        "Passwords do not match. Please re-enter the same password."
      }
    }

    val existing = accountsMap.value[trimmedEmail]
    val computedHash = hashPassword(trimmedEmail, password)

    if (existing != null) {
      if (existing.passwordHash != computedHash) {
        throw IllegalArgumentException("Incorrect password for $trimmedEmail. Please check your password and try again.")
      }
      _currentAccount.value = existing
      saveToDisk()
      return@runCatching existing
    }

    // Create new account for this Email ID
    val emailPrefix = trimmedEmail.substringBefore("@")
      .replace(Regex("[^a-zA-Z0-9]"), " ")
      .split(" ")
      .filter { it.isNotBlank() }
      .joinToString(" ") { part ->
        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
      }
      .ifBlank { "ITI User" }

    val uid = cleanId("usr_${trimmedEmail.replace("@", "_").replace(".", "_")}")
    val newAccount = LocalAuthAccount(
      userId = uid,
      email = trimmedEmail,
      passwordHash = computedHash,
      displayName = emailPrefix
    )

    accountsMap.value = accountsMap.value + (trimmedEmail to newAccount)
    _currentAccount.value = newAccount
    saveToDisk()
    newAccount
  }

  @Synchronized
  fun signOut() {
    _currentAccount.value = null
    saveToDisk()
  }

  // --- REACTIVE OBSERVATION FLOWS ---

  fun observeUserProfile(userId: String): Flow<UserProfile?> =
    usersMap.map { it[userId] }

  fun observeAdminProfile(adminId: String): Flow<AdminProfile?> =
    adminsMap.map { it[adminId] }

  fun observeTrade(tradeId: String): Flow<Trade?> =
    tradesMap.map { it[tradeId] }

  fun observeTraineesForTrade(adminId: String, tradeId: String): Flow<List<TraineeRecord>> =
    traineesMap.map { map ->
      map.values.filter { it.tradeId == tradeId || it.adminId == adminId }
        .sortedBy { it.registrationNumber }
    }

  fun observeTraineeByUid(traineeUid: String): Flow<TraineeRecord?> =
    traineesMap.map { map ->
      map[traineeUid] ?: map.values.firstOrNull { it.traineeUid == traineeUid }
    }

  fun observeAttendanceForTrade(adminId: String, tradeId: String): Flow<List<AttendanceRecord>> =
    attendanceMap.map { map ->
      map.values.filter { it.tradeId == tradeId || it.adminId == adminId }
        .sortedByDescending { it.dateString }
    }

  fun observeAttendanceForTrainee(traineeUid: String): Flow<List<AttendanceRecord>> =
    attendanceMap.map { map ->
      map.values.filter { it.traineeUid == traineeUid || it.traineeId == traineeUid }
        .sortedByDescending { it.dateString }
    }

  fun observeLeavesForTrade(adminId: String, tradeId: String): Flow<List<LeaveApplication>> =
    leavesMap.map { map ->
      map.values.filter { it.tradeId == tradeId || it.adminId == adminId }
        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
    }

  fun observeLeavesForTrainee(traineeUid: String): Flow<List<LeaveApplication>> =
    leavesMap.map { map ->
      map.values.filter { it.traineeUid == traineeUid || it.traineeId == traineeUid }
        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
    }

  fun observeNotificationsForTrainee(traineeUid: String): Flow<List<TraineeNotification>> =
    notificationsMap.map { map ->
      map.values.filter { it.traineeUid == traineeUid || it.traineeId == traineeUid }
        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
    }

  fun observeNotificationsForTrade(adminId: String, tradeId: String): Flow<List<TraineeNotification>> =
    notificationsMap.map { map ->
      map.values.filter { it.tradeId == tradeId || it.adminId == adminId }
        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
    }

  // --- WRITE OPERATIONS ---

  @Synchronized
  fun registerTrainingOfficer(
    uid: String,
    fullName: String,
    employeeCode: String,
    tradeId: String,
    tradeName: String,
    tradeCode: String,
    sessionYear: String,
    officerVerificationCode: String = "ITI-RAIGARH-OFFICER"
  ): UserProfile {
    val now = Timestamp.now()
    // Find if a shared trade for this base tradeId or tradeName already exists, or create a canonical one
    val existingTrade = tradesMap.value.values.firstOrNull {
      it.tradeId == tradeId || it.tradeName.equals(tradeName.trim(), ignoreCase = true)
    }
    val safeTradeId = existingTrade?.tradeId ?: cleanId("${tradeId}_$uid")

    val adminProfile = AdminProfile(
      adminId = uid,
      fullName = fullName.trim(),
      assignedTradeId = safeTradeId,
      assignedTradeName = tradeName.trim(),
      sessionYear = sessionYear.trim(),
      officerVerificationCode = officerVerificationCode.trim(),
      createdAt = now,
      updatedAt = now
    )
    adminsMap.value = adminsMap.value + (uid to adminProfile)

    val userProfile = UserProfile(
      userId = uid,
      fullName = fullName.trim(),
      registrationNumber = employeeCode.trim(),
      role = UserRole.ADMIN.value,
      tradeId = safeTradeId,
      tradeName = tradeName.trim(),
      sessionYear = sessionYear.trim(),
      isActive = true,
      createdAt = now,
      updatedAt = now
    )
    usersMap.value = usersMap.value + (uid to userProfile)

    val trade = Trade(
      tradeId = safeTradeId,
      tradeName = tradeName.trim(),
      tradeCode = tradeCode.trim(),
      adminId = uid,
      adminName = fullName.trim(),
      sessionYear = sessionYear.trim(),
      totalWorkingDays = existingTrade?.totalWorkingDays ?: 240,
      maxCasualLeave = existingTrade?.maxCasualLeave ?: 12,
      maxMedicalLeave = existingTrade?.maxMedicalLeave ?: 36,
      halfDayDeductionRule = existingTrade?.halfDayDeductionRule ?: "DEDUCT_HALF_CL",
      maxHalfDayLeaves = existingTrade?.maxHalfDayLeaves ?: 12,
      minAttendancePercent = existingTrade?.minAttendancePercent ?: 75,
      createdAt = existingTrade?.createdAt ?: now,
      updatedAt = now
    )
    tradesMap.value = tradesMap.value + (safeTradeId to trade)

    if (!traineesMap.value.containsKey(uid)) {
      val selfTrainee = TraineeRecord(
        traineeId = uid,
        traineeUid = uid,
        adminId = uid,
        tradeId = safeTradeId,
        tradeName = tradeName.trim(),
        sessionYear = sessionYear.trim(),
        fullName = fullName.trim(),
        registrationNumber = employeeCode.trim(),
        fatherName = "Self / Demo Trainee",
        batchShift = "Shift I (Morning)",
        isActive = true,
        presentDays = 18,
        absentDays = 1,
        halfDays = 1,
        approvedLeaveDays = 1,
        casualLeaveUsed = 1,
        medicalLeaveUsed = 0,
        halfDayLeaveUsed = 1,
        createdAt = now,
        updatedAt = now
      )
      traineesMap.value = traineesMap.value + (uid to selfTrainee)
    }

    saveToDisk()
    return userProfile
  }

  @Synchronized
  fun registerTraineeUser(
    uid: String,
    fullName: String,
    registrationNumber: String,
    fatherName: String,
    batchShift: String,
    tradeId: String,
    tradeName: String,
    sessionYear: String,
    adminId: String = ""
  ): UserProfile {
    val now = Timestamp.now()
    // Link to an existing Trade created by an Admin if one matches tradeName or tradeId
    val matchingTrade = tradesMap.value.values.firstOrNull {
      it.tradeId == tradeId || it.tradeName.equals(tradeName.trim(), ignoreCase = true)
    }
    val safeTradeId = matchingTrade?.tradeId ?: cleanId(tradeId)
    val effectiveAdminId = when {
      adminId.isNotBlank() -> cleanId(adminId)
      matchingTrade != null -> matchingTrade.adminId
      else -> uid
    }

    // Check if an Admin already created a TraineeRecord with the same registration number
    val existingByReg = traineesMap.value.values.firstOrNull {
      it.registrationNumber.equals(registrationNumber.trim(), ignoreCase = true)
    }

    val userProfile = UserProfile(
      userId = uid,
      fullName = fullName.trim(),
      registrationNumber = registrationNumber.trim(),
      role = UserRole.TRAINEE.value,
      tradeId = safeTradeId,
      tradeName = tradeName.trim(),
      sessionYear = sessionYear.trim(),
      isActive = true,
      createdAt = now,
      updatedAt = now
    )
    usersMap.value = usersMap.value + (uid to userProfile)

    if (existingByReg != null) {
      val linked = existingByReg.copy(
        traineeUid = uid,
        fullName = fullName.trim(),
        fatherName = fatherName.trim().ifEmpty { existingByReg.fatherName },
        batchShift = batchShift.trim().ifEmpty { existingByReg.batchShift },
        updatedAt = now
      )
      traineesMap.value = traineesMap.value + (existingByReg.traineeId to linked)
    } else {
      val traineeRecord = TraineeRecord(
        traineeId = uid,
        traineeUid = uid,
        adminId = effectiveAdminId,
        tradeId = safeTradeId,
        tradeName = tradeName.trim(),
        sessionYear = sessionYear.trim(),
        fullName = fullName.trim(),
        registrationNumber = registrationNumber.trim(),
        fatherName = fatherName.trim().ifEmpty { "Guardian" },
        batchShift = batchShift.trim().ifEmpty { "Shift I (Morning)" },
        isActive = true,
        presentDays = 0,
        absentDays = 0,
        halfDays = 0,
        approvedLeaveDays = 0,
        casualLeaveUsed = 0,
        medicalLeaveUsed = 0,
        halfDayLeaveUsed = 0,
        lowAttendanceWarningCount = 0,
        lastWarningMessage = "",
        createdAt = now,
        updatedAt = now
      )
      traineesMap.value = traineesMap.value + (uid to traineeRecord)
    }

    saveToDisk()
    return userProfile
  }

  @Synchronized
  fun switchRoleForCurrentUser(
    uid: String,
    currentProfile: UserProfile,
    targetRole: UserRole
  ) {
    val now = Timestamp.now()
    if (targetRole == UserRole.ADMIN) {
      if (!adminsMap.value.containsKey(uid)) {
        val adminProfile = AdminProfile(
          adminId = uid,
          fullName = currentProfile.fullName,
          assignedTradeId = currentProfile.tradeId,
          assignedTradeName = currentProfile.tradeName,
          sessionYear = currentProfile.sessionYear,
          officerVerificationCode = "ITI-RAIGARH-OFFICER",
          createdAt = now,
          updatedAt = now
        )
        adminsMap.value = adminsMap.value + (uid to adminProfile)
      }
      if (!tradesMap.value.containsKey(currentProfile.tradeId)) {
        val trade = Trade(
          tradeId = currentProfile.tradeId,
          tradeName = currentProfile.tradeName,
          tradeCode = "ITI-RGH-${currentProfile.tradeName.take(4).uppercase()}",
          adminId = uid,
          adminName = currentProfile.fullName,
          sessionYear = currentProfile.sessionYear,
          createdAt = now,
          updatedAt = now
        )
        tradesMap.value = tradesMap.value + (currentProfile.tradeId to trade)
      }
    }

    val updatedProfile = currentProfile.copy(
      role = targetRole.value,
      updatedAt = now
    )
    usersMap.value = usersMap.value + (uid to updatedProfile)
    saveToDisk()
  }

  @Synchronized
  fun updateTradeConfiguration(
    uid: String,
    tradeId: String,
    tradeName: String,
    tradeCode: String,
    adminName: String,
    sessionYear: String,
    totalWorkingDays: Int,
    maxCasualLeave: Int,
    maxMedicalLeave: Int,
    halfDayDeductionRule: String,
    maxHalfDayLeaves: Int,
    minAttendancePercent: Int
  ) {
    val now = Timestamp.now()
    val existing = tradesMap.value[tradeId]
    val updated = Trade(
      tradeId = tradeId,
      tradeName = tradeName.trim(),
      tradeCode = tradeCode.trim(),
      adminId = existing?.adminId ?: uid,
      adminName = adminName.trim(),
      sessionYear = sessionYear.trim(),
      totalWorkingDays = totalWorkingDays.coerceIn(1, 366),
      maxCasualLeave = maxCasualLeave.coerceIn(1, 12),
      maxMedicalLeave = maxMedicalLeave.coerceIn(1, 36),
      halfDayDeductionRule = halfDayDeductionRule,
      maxHalfDayLeaves = maxHalfDayLeaves.coerceIn(1, 24),
      minAttendancePercent = minAttendancePercent.coerceIn(1, 100),
      createdAt = existing?.createdAt ?: now,
      updatedAt = now
    )
    tradesMap.value = tradesMap.value + (tradeId to updated)

    adminsMap.value[uid]?.let { admin ->
      adminsMap.value = adminsMap.value + (
        uid to admin.copy(
          assignedTradeName = tradeName.trim(),
          sessionYear = sessionYear.trim(),
          updatedAt = now
        )
      )
    }
    saveToDisk()
  }

  @Synchronized
  fun createTraineeInTrade(
    adminUid: String,
    tradeId: String,
    tradeName: String,
    sessionYear: String,
    fullName: String,
    registrationNumber: String,
    fatherName: String,
    batchShift: String,
    customTraineeId: String? = null
  ): TraineeRecord {
    val now = Timestamp.now()
    val rawId = customTraineeId ?: "trn_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val traineeId = cleanId(rawId)

    val trainee = TraineeRecord(
      traineeId = traineeId,
      traineeUid = traineeId,
      adminId = adminUid,
      tradeId = tradeId,
      tradeName = tradeName,
      sessionYear = sessionYear,
      fullName = fullName.trim(),
      registrationNumber = registrationNumber.trim(),
      fatherName = fatherName.trim(),
      batchShift = batchShift.trim(),
      isActive = true,
      presentDays = 0,
      absentDays = 0,
      halfDays = 0,
      approvedLeaveDays = 0,
      casualLeaveUsed = 0,
      medicalLeaveUsed = 0,
      halfDayLeaveUsed = 0,
      lowAttendanceWarningCount = 0,
      lastWarningMessage = "",
      createdAt = now,
      updatedAt = now
    )
    traineesMap.value = traineesMap.value + (traineeId to trainee)
    saveToDisk()
    return trainee
  }

  @Synchronized
  fun updateTraineeAccount(
    traineeId: String,
    fullName: String,
    registrationNumber: String,
    fatherName: String,
    batchShift: String,
    sessionYear: String,
    isActive: Boolean
  ) {
    val existing = traineesMap.value[traineeId]
      ?: throw IllegalStateException("Trainee not found: $traineeId")
    val updated = existing.copy(
      fullName = fullName.trim(),
      registrationNumber = registrationNumber.trim(),
      fatherName = fatherName.trim(),
      batchShift = batchShift.trim(),
      sessionYear = sessionYear.trim(),
      isActive = isActive,
      updatedAt = Timestamp.now()
    )
    traineesMap.value = traineesMap.value + (traineeId to updated)
    saveToDisk()
  }

  fun getTraineeById(traineeId: String): TraineeRecord {
    return traineesMap.value[traineeId]
      ?: traineesMap.value.values.firstOrNull { it.traineeUid == traineeId }
      ?: throw IllegalStateException("Trainee not found: $traineeId")
  }

  @Synchronized
  fun markDailyAttendanceByTrainee(
    uid: String,
    trainee: TraineeRecord,
    dateString: String,
    status: AttendanceStatus,
    remarks: String
  ): AttendanceRecord {
    if (!trainee.isActive) {
      throw IllegalStateException("Your trainee account is currently deactivated. Contact your Training Officer.")
    }
    if (status != AttendanceStatus.PRESENT && status != AttendanceStatus.HALF_DAY) {
      throw IllegalArgumentException("Trainees can only mark Present or Half-Day attendance.")
    }
    val attendanceId = "${trainee.traineeId}_$dateString"
    val existing = attendanceMap.value[attendanceId]
    if (existing != null && existing.attendanceStatus != AttendanceStatus.ABSENT) {
      throw IllegalStateException("Attendance for $dateString is already marked! Duplicate attendance is not allowed.")
    }

    val now = Timestamp.now()
    val monthString = dateString.take(7)
    val checkInTime = SimpleDateFormat("hh:mm a", Locale.US).format(Date())

    val record = AttendanceRecord(
      attendanceId = attendanceId,
      traineeId = trainee.traineeId,
      traineeUid = uid,
      traineeName = trainee.fullName,
      registrationNumber = trainee.registrationNumber,
      tradeId = trainee.tradeId,
      tradeName = trainee.tradeName,
      adminId = trainee.adminId,
      dateString = dateString,
      monthString = monthString,
      sessionYear = trainee.sessionYear,
      status = status.value,
      markedByRole = UserRole.TRAINEE.value,
      remarks = remarks.trim().take(200),
      checkInTime = checkInTime,
      createdAt = existing?.createdAt ?: now,
      updatedAt = now
    )
    attendanceMap.value = attendanceMap.value + (attendanceId to record)

    val wasAutoAbsent = existing?.attendanceStatus == AttendanceStatus.ABSENT
    val updatedAbsent = if (wasAutoAbsent) (trainee.absentDays - 1).coerceAtLeast(0) else trainee.absentDays
    val newPresent = if (status == AttendanceStatus.PRESENT) trainee.presentDays + 1 else trainee.presentDays
    val newHalf = if (status == AttendanceStatus.HALF_DAY) trainee.halfDays + 1 else trainee.halfDays
    val updatedTrainee = trainee.copy(
      presentDays = newPresent.coerceAtMost(366),
      absentDays = updatedAbsent,
      halfDays = newHalf.coerceAtMost(366),
      updatedAt = now
    )
    traineesMap.value = traineesMap.value + (trainee.traineeId to updatedTrainee)
    saveToDisk()
    return record
  }

  @Synchronized
  fun resetTodayAttendanceForTrainee(
    trainee: TraineeRecord,
    dateString: String
  ) {
    val attendanceId = "${trainee.traineeId}_$dateString"
    val existing = attendanceMap.value[attendanceId] ?: return
    attendanceMap.value = attendanceMap.value - attendanceId

    var present = trainee.presentDays
    var absent = trainee.absentDays
    var half = trainee.halfDays
    when (existing.attendanceStatus) {
      AttendanceStatus.PRESENT -> present = (present - 1).coerceAtLeast(0)
      AttendanceStatus.ABSENT -> absent = (absent - 1).coerceAtLeast(0)
      AttendanceStatus.HALF_DAY -> half = (half - 1).coerceAtLeast(0)
      else -> Unit
    }
    val updatedTrainee = trainee.copy(
      presentDays = present,
      absentDays = absent,
      halfDays = half,
      updatedAt = Timestamp.now()
    )
    traineesMap.value = traineesMap.value + (trainee.traineeId to updatedTrainee)
    saveToDisk()
  }

  @Synchronized
  fun markOrUpdateAttendanceByAdmin(
    adminUid: String,
    trainee: TraineeRecord,
    dateString: String,
    newStatus: AttendanceStatus,
    existingRecord: AttendanceRecord?,
    remarks: String
  ): AttendanceRecord {
    val now = Timestamp.now()
    val attendanceId = "${trainee.traineeId}_$dateString"
    val monthString = dateString.take(7)
    val checkInTime = SimpleDateFormat("hh:mm a", Locale.US).format(Date())

    val record = AttendanceRecord(
      attendanceId = attendanceId,
      traineeId = trainee.traineeId,
      traineeUid = trainee.traineeUid,
      traineeName = trainee.fullName,
      registrationNumber = trainee.registrationNumber,
      tradeId = trainee.tradeId,
      tradeName = trainee.tradeName,
      adminId = adminUid,
      dateString = dateString,
      monthString = monthString,
      sessionYear = trainee.sessionYear,
      status = newStatus.value,
      markedByRole = UserRole.ADMIN.value,
      remarks = remarks.trim().take(200),
      checkInTime = checkInTime,
      createdAt = existingRecord?.createdAt ?: now,
      updatedAt = now
    )
    attendanceMap.value = attendanceMap.value + (attendanceId to record)

    var present = trainee.presentDays
    var absent = trainee.absentDays
    var half = trainee.halfDays
    var onLeave = trainee.approvedLeaveDays

    when (existingRecord?.attendanceStatus) {
      AttendanceStatus.PRESENT -> present = (present - 1).coerceAtLeast(0)
      AttendanceStatus.ABSENT -> absent = (absent - 1).coerceAtLeast(0)
      AttendanceStatus.HALF_DAY -> half = (half - 1).coerceAtLeast(0)
      AttendanceStatus.ON_LEAVE -> onLeave = (onLeave - 1).coerceAtLeast(0)
      null -> Unit
    }

    when (newStatus) {
      AttendanceStatus.PRESENT -> present = (present + 1).coerceAtMost(366)
      AttendanceStatus.ABSENT -> absent = (absent + 1).coerceAtMost(366)
      AttendanceStatus.HALF_DAY -> half = (half + 1).coerceAtMost(366)
      AttendanceStatus.ON_LEAVE -> onLeave = (onLeave + 1).coerceAtMost(366)
    }

    val updatedTrainee = trainee.copy(
      presentDays = present,
      absentDays = absent,
      halfDays = half,
      approvedLeaveDays = onLeave,
      updatedAt = now
    )
    traineesMap.value = traineesMap.value + (trainee.traineeId to updatedTrainee)
    saveToDisk()
    return record
  }

  @Synchronized
  fun submitLeaveApplication(
    trainee: TraineeRecord,
    leaveType: LeaveType,
    startDate: String,
    endDate: String,
    totalDays: Int,
    halfDaySession: HalfDaySession,
    reason: String
  ): LeaveApplication {
    if (!trainee.isActive) {
      throw IllegalStateException("Inactive trainee account cannot apply for leave.")
    }
    val trimmedReason = reason.trim()
    require(trimmedReason.length >= 3) { "Please provide a valid reason (at least 3 characters)." }

    when (leaveType) {
      LeaveType.CL -> {
        require(totalDays in 1..12) { "Casual Leave (CL) request must be between 1 and 12 days." }
        val projectedCl = trainee.effectiveCasualLeaveUsed + totalDays.toDouble()
        require(projectedCl <= 12.0) {
          "Casual Leave limit exceeded! Remaining CL balance is ${trainee.remainingCasualLeave} days (Max 12 days/year)."
        }
      }
      LeaveType.HALF_DAY -> {
        require(totalDays == 1) { "Half-Day Leave is applied for 1 session (0.5 day) at a time." }
        require(halfDaySession == HalfDaySession.FIRST_HALF || halfDaySession == HalfDaySession.SECOND_HALF) {
          "Please select First Half or Second Half for Half-Day Leave."
        }
        require(trainee.halfDayLeaveUsed + 1 <= 12) {
          "Half-Day Leave quota exceeded! Remaining Half-Day leaves: ${trainee.remainingHalfDayLeaves}."
        }
        require(trainee.effectiveCasualLeaveUsed + 0.5 <= 12.0) {
          "Insufficient Casual Leave balance (${trainee.remainingCasualLeave} days) for Half-Day Leave."
        }
      }
      LeaveType.MEDICAL -> {
        require(totalDays in 1..36) { "Medical Leave request must be between 1 and 36 days." }
        require(trainee.medicalLeaveUsed + totalDays <= 36) {
          "Medical Leave limit exceeded! Remaining Medical Leave balance is ${trainee.remainingMedicalLeave} days (Max 36 days)."
        }
      }
    }

    val now = Timestamp.now()
    val leaveId = cleanId("lv_${UUID.randomUUID().toString().replace("-", "").take(12)}")
    val effectiveEndDate = if (leaveType == LeaveType.HALF_DAY) startDate else endDate
    val effectiveHalfDaySession = if (leaveType == LeaveType.HALF_DAY) halfDaySession.value else HalfDaySession.NONE.value

    val application = LeaveApplication(
      leaveId = leaveId,
      traineeId = trainee.traineeId,
      traineeUid = trainee.traineeUid,
      traineeName = trainee.fullName,
      registrationNumber = trainee.registrationNumber,
      tradeId = trainee.tradeId,
      tradeName = trainee.tradeName,
      adminId = trainee.adminId,
      sessionYear = trainee.sessionYear,
      leaveType = leaveType.value,
      startDate = startDate,
      endDate = effectiveEndDate,
      monthString = startDate.take(7),
      totalDays = if (leaveType == LeaveType.HALF_DAY) 1 else totalDays,
      halfDaySession = effectiveHalfDaySession,
      reason = trimmedReason.take(300),
      status = LeaveStatus.PENDING.value,
      adminRemarks = "",
      reviewedBy = "",
      createdAt = now,
      updatedAt = now
    )

    leavesMap.value = leavesMap.value + (leaveId to application)
    saveToDisk()
    return application
  }

  @Synchronized
  fun reviewLeaveApplication(
    adminUid: String,
    leave: LeaveApplication,
    trainee: TraineeRecord,
    approve: Boolean,
    adminRemarks: String,
    officerName: String
  ) {
    val now = Timestamp.now()
    val newStatus = if (approve) LeaveStatus.APPROVED else LeaveStatus.REJECTED
    val remarks = adminRemarks.trim().ifEmpty {
      if (approve) "Approved by Training Officer" else "Rejected by Training Officer"
    }.take(300)

    val updatedLeave = leave.copy(
      status = newStatus.value,
      adminRemarks = remarks,
      reviewedBy = officerName.trim().take(100),
      updatedAt = now
    )
    leavesMap.value = leavesMap.value + (leave.leaveId to updatedLeave)

    if (approve) {
      var clUsed = trainee.casualLeaveUsed
      var medUsed = trainee.medicalLeaveUsed
      var halfUsed = trainee.halfDayLeaveUsed
      var approvedLeaveDays = trainee.approvedLeaveDays
      var halfDaysCount = trainee.halfDays

      when (leave.parsedLeaveType) {
        LeaveType.CL -> {
          clUsed = (clUsed + leave.totalDays).coerceAtMost(12)
          approvedLeaveDays = (approvedLeaveDays + leave.totalDays).coerceAtMost(366)
        }
        LeaveType.MEDICAL -> {
          medUsed = (medUsed + leave.totalDays).coerceAtMost(36)
          approvedLeaveDays = (approvedLeaveDays + leave.totalDays).coerceAtMost(366)
        }
        LeaveType.HALF_DAY -> {
          halfUsed = (halfUsed + 1).coerceAtMost(24)
          halfDaysCount = (halfDaysCount + 1).coerceAtMost(366)
        }
      }

      val updatedTrainee = trainee.copy(
        casualLeaveUsed = clUsed,
        medicalLeaveUsed = medUsed,
        halfDayLeaveUsed = halfUsed,
        approvedLeaveDays = approvedLeaveDays,
        halfDays = halfDaysCount,
        updatedAt = now
      )
      traineesMap.value = traineesMap.value + (trainee.traineeId to updatedTrainee)

      val datesToRecord = generateDateRange(leave.startDate, leave.endDate, leave.totalDays)
      val attStatus = if (leave.parsedLeaveType == LeaveType.HALF_DAY) {
        AttendanceStatus.HALF_DAY
      } else {
        AttendanceStatus.ON_LEAVE
      }

      val updatedAttendance = attendanceMap.value.toMutableMap()
      for (dateStr in datesToRecord) {
        val attId = "${trainee.traineeId}_$dateStr"
        val existingAtt = updatedAttendance[attId]
        updatedAttendance[attId] = AttendanceRecord(
          attendanceId = attId,
          traineeId = trainee.traineeId,
          traineeUid = trainee.traineeUid,
          traineeName = trainee.fullName,
          registrationNumber = trainee.registrationNumber,
          tradeId = trainee.tradeId,
          tradeName = trainee.tradeName,
          adminId = adminUid,
          dateString = dateStr,
          monthString = dateStr.take(7),
          sessionYear = trainee.sessionYear,
          status = attStatus.value,
          markedByRole = UserRole.ADMIN.value,
          remarks = "Approved ${leave.parsedLeaveType.label}: ${leave.reason}".take(200),
          checkInTime = "LEAVE",
          createdAt = existingAtt?.createdAt ?: now,
          updatedAt = now
        )
      }
      attendanceMap.value = updatedAttendance
    }

    val notifId = cleanId("ntf_${UUID.randomUUID().toString().replace("-", "").take(12)}")
    val notifType = if (approve) NotificationType.LEAVE_APPROVED else NotificationType.LEAVE_REJECTED
    val notifTitle = if (approve) {
      "${leave.parsedLeaveType.label} Approved"
    } else {
      "${leave.parsedLeaveType.label} Rejected"
    }
    val notifMessage =
      "Your ${leave.parsedLeaveType.label} application (${leave.startDate} to ${leave.endDate}) was ${newStatus.label.lowercase()} by $officerName. Remarks: $remarks"

    val notification = TraineeNotification(
      notificationId = notifId,
      traineeId = trainee.traineeId,
      traineeUid = trainee.traineeUid,
      traineeName = trainee.fullName,
      tradeId = trainee.tradeId,
      adminId = adminUid,
      type = notifType.value,
      title = notifTitle.take(120),
      message = notifMessage.take(400),
      isRead = false,
      createdAt = now,
      updatedAt = now
    )
    notificationsMap.value = notificationsMap.value + (notifId to notification)
    saveToDisk()
  }

  @Synchronized
  fun issueLowAttendanceWarning(
    adminUid: String,
    trainee: TraineeRecord,
    warningMessage: String,
    officerName: String
  ) {
    val now = Timestamp.now()
    val msg = warningMessage.trim().ifEmpty {
      "Official Warning: Your current attendance is ${trainee.attendancePercentage}%, which is below the mandatory 75% NCVT requirement at Govt. ITI Raigarh."
    }.take(300)

    val newCount = (trainee.lowAttendanceWarningCount + 1).coerceAtMost(100)
    val updatedTrainee = trainee.copy(
      lowAttendanceWarningCount = newCount,
      lastWarningMessage = msg,
      updatedAt = now
    )
    traineesMap.value = traineesMap.value + (trainee.traineeId to updatedTrainee)

    val notifId = cleanId("warn_${UUID.randomUUID().toString().replace("-", "").take(12)}")
    val notification = TraineeNotification(
      notificationId = notifId,
      traineeId = trainee.traineeId,
      traineeUid = trainee.traineeUid,
      traineeName = trainee.fullName,
      tradeId = trainee.tradeId,
      adminId = adminUid,
      type = NotificationType.LOW_ATTENDANCE_WARNING.value,
      title = "Low Attendance Warning (#$newCount)",
      message = "$msg — Issued by Training Officer $officerName",
      isRead = false,
      createdAt = now,
      updatedAt = now
    )
    notificationsMap.value = notificationsMap.value + (notifId to notification)
    saveToDisk()
  }

  @Synchronized
  fun markNotificationAsRead(notificationId: String) {
    val existing = notificationsMap.value[notificationId] ?: return
    notificationsMap.value = notificationsMap.value + (
      notificationId to existing.copy(isRead = true, updatedAt = Timestamp.now())
    )
    saveToDisk()
  }

  @Synchronized
  fun seedSampleRosterForTrade(
    adminUid: String,
    tradeId: String,
    tradeName: String,
    sessionYear: String,
    todayDate: String
  ) {
    val now = Timestamp.now()
    val codePrefix = tradeName.take(3).uppercase()
    val sampleTrainees = listOf(
      Triple("Aarav Patel", "RGH-$codePrefix-25-001", "Ramesh Patel"),
      Triple("Priya Sahu", "RGH-$codePrefix-25-002", "Dinesh Sahu"),
      Triple("Vikram Rathore", "RGH-$codePrefix-25-003", "Suresh Rathore"),
      Triple("Neha Dewangan", "RGH-$codePrefix-25-004", "Kishore Dewangan"),
      Triple("Rohan Verma", "RGH-$codePrefix-25-005", "Manoj Verma")
    )

    val newTrainees = traineesMap.value.toMutableMap()
    val newAttendance = attendanceMap.value.toMutableMap()
    val newLeaves = leavesMap.value.toMutableMap()

    sampleTrainees.forEachIndexed { index, (name, regNo, father) ->
      val tId = if (index == 0 && tradeName.equals("Electrician", ignoreCase = true)) {
        "usr_trainee_itiraigarh_edu_in"
      } else {
        cleanId("trn_${tradeId}_${index + 1}")
      }
      val isLow = index == 2 || index == 4
      val present = if (isLow) 11 else 19
      val absent = if (isLow) 8 else 1
      val clUsed = if (index == 1) 2 else 0
      val medUsed = if (index == 2) 4 else 0

      val trainee = TraineeRecord(
        traineeId = tId,
        traineeUid = tId,
        adminId = adminUid,
        tradeId = tradeId,
        tradeName = tradeName,
        sessionYear = sessionYear,
        fullName = name,
        registrationNumber = regNo,
        fatherName = father,
        batchShift = if (index % 2 == 0) "Shift I (Morning)" else "Shift II (Afternoon)",
        isActive = true,
        presentDays = present,
        absentDays = absent,
        halfDays = if (index == 3) 1 else 0,
        approvedLeaveDays = clUsed + medUsed,
        casualLeaveUsed = clUsed,
        medicalLeaveUsed = medUsed,
        halfDayLeaveUsed = if (index == 3) 1 else 0,
        lowAttendanceWarningCount = if (isLow) 1 else 0,
        lastWarningMessage = if (isLow) "Attendance below 75% threshold. Please attend classes regularly." else "",
        createdAt = now,
        updatedAt = now
      )
      newTrainees[tId] = trainee

      // Leave index == 0 (Demo Trainee) unmarked for today so trainee can mark attendance upon reaching college!
      // While unmarked, today is automatically counted in Absent.
      if (index == 0) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val baseDate = try { sdf.parse(todayDate) ?: Date() } catch (_: Exception) { Date() }
        val cal = Calendar.getInstance()

        // Seed past 3 days for Demo Trainee including 1 Auto-Absent day
        listOf(
          Triple(-1, AttendanceStatus.ABSENT, "Auto-Marked Absent: Not marked by trainee on college arrival"),
          Triple(-2, AttendanceStatus.PRESENT, "Marked by Trainee on College Arrival • Trade Workshop"),
          Triple(-3, AttendanceStatus.PRESENT, "Marked by Trainee on College Arrival • Theory Class")
        ).forEach { (dayOffset, pastStatus, note) ->
          cal.time = baseDate
          cal.add(Calendar.DAY_OF_MONTH, dayOffset)
          val pastDateStr = sdf.format(cal.time)
          val pastAttId = "${tId}_$pastDateStr"
          newAttendance[pastAttId] = AttendanceRecord(
            attendanceId = pastAttId,
            traineeId = tId,
            traineeUid = tId,
            traineeName = name,
            registrationNumber = regNo,
            tradeId = tradeId,
            tradeName = tradeName,
            adminId = adminUid,
            dateString = pastDateStr,
            monthString = pastDateStr.take(7),
            sessionYear = sessionYear,
            status = pastStatus.value,
            markedByRole = if (pastStatus == AttendanceStatus.ABSENT) UserRole.ADMIN.value else UserRole.TRAINEE.value,
            remarks = note,
            checkInTime = if (pastStatus == AttendanceStatus.ABSENT) "AUTO-ABSENT" else "09:10 AM",
            createdAt = now,
            updatedAt = now
          )
        }
      } else if (index in 1..3) {
        val status = when (index) {
          1 -> AttendanceStatus.PRESENT
          2 -> AttendanceStatus.ABSENT
          else -> AttendanceStatus.HALF_DAY
        }
        val attId = "${tId}_$todayDate"
        newAttendance[attId] = AttendanceRecord(
          attendanceId = attId,
          traineeId = tId,
          traineeUid = tId,
          traineeName = name,
          registrationNumber = regNo,
          tradeId = tradeId,
          tradeName = tradeName,
          adminId = adminUid,
          dateString = todayDate,
          monthString = todayDate.take(7),
          sessionYear = sessionYear,
          status = status.value,
          markedByRole = if (status == AttendanceStatus.PRESENT) UserRole.TRAINEE.value else UserRole.ADMIN.value,
          remarks = if (status == AttendanceStatus.ABSENT) {
            "Auto-Marked Absent: Not marked by trainee on college arrival"
          } else {
            "Marked by Trainee on College Arrival"
          },
          checkInTime = if (status == AttendanceStatus.ABSENT) "AUTO-ABSENT" else "09:05 AM",
          createdAt = now,
          updatedAt = now
        )
      }

      if (index == 1 || index == 2) {
        val lvId = cleanId("lv_${tradeId}_${index + 1}")
        val lvType = if (index == 1) LeaveType.CL else LeaveType.MEDICAL
        val days = if (index == 1) 2 else 4
        newLeaves[lvId] = LeaveApplication(
          leaveId = lvId,
          traineeId = tId,
          traineeUid = tId,
          traineeName = name,
          registrationNumber = regNo,
          tradeId = tradeId,
          tradeName = tradeName,
          adminId = adminUid,
          sessionYear = sessionYear,
          leaveType = lvType.value,
          startDate = todayDate,
          endDate = todayDate,
          monthString = todayDate.take(7),
          totalDays = days,
          halfDaySession = HalfDaySession.NONE.value,
          reason = if (index == 1) "Family ceremony in Raigarh district" else "Viral fever medical rest advised by District Hospital Raigarh",
          status = LeaveStatus.PENDING.value,
          adminRemarks = "",
          reviewedBy = "",
          createdAt = now,
          updatedAt = now
        )
      }
    }

    traineesMap.value = newTrainees
    attendanceMap.value = newAttendance
    leavesMap.value = newLeaves
    saveToDisk()
  }

  private fun seedDefaultDemoAccounts() {
    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val adminEmail = "officer@itiraigarh.edu.in"
    val adminUid = "usr_officer_itiraigarh_edu_in"
    val adminAccount = LocalAuthAccount(
      userId = adminUid,
      email = adminEmail,
      passwordHash = hashPassword(adminEmail, "iti123"),
      displayName = "R. K. Sharma"
    )

    val traineeEmail = "trainee@itiraigarh.edu.in"
    val traineeUid = "usr_trainee_itiraigarh_edu_in"
    val traineeAccount = LocalAuthAccount(
      userId = traineeUid,
      email = traineeEmail,
      passwordHash = hashPassword(traineeEmail, "iti123"),
      displayName = "Aarav Patel"
    )

    accountsMap.value = mapOf(
      adminEmail to adminAccount,
      traineeEmail to traineeAccount
    )

    // Pre-configure Admin (Electrician Trade) + sample roster
    val adminProfile = registerTrainingOfficer(
      uid = adminUid,
      fullName = "R. K. Sharma",
      employeeCode = "TO-RGH-EL-01",
      tradeId = "trade_electrician",
      tradeName = "Electrician",
      tradeCode = "ITI-RGH-EL-01",
      sessionYear = "2025-2026"
    )
    seedSampleRosterForTrade(
      adminUid = adminUid,
      tradeId = adminProfile.tradeId,
      tradeName = adminProfile.tradeName,
      sessionYear = adminProfile.sessionYear,
      todayDate = todayDate
    )

    // Pre-configure Trainee (Aarav Patel in Electrician Trade)
    val now = Timestamp.now()
    val traineeUserProfile = UserProfile(
      userId = traineeUid,
      fullName = "Aarav Patel",
      registrationNumber = "RGH-ELE-25-001",
      role = UserRole.TRAINEE.value,
      tradeId = adminProfile.tradeId,
      tradeName = adminProfile.tradeName,
      sessionYear = "2025-2026",
      isActive = true,
      createdAt = now,
      updatedAt = now
    )
    usersMap.value = usersMap.value + (traineeUid to traineeUserProfile)
  }

  private fun generateDateRange(startDate: String, endDate: String, maxDays: Int): List<String> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return try {
      val start = sdf.parse(startDate) ?: return listOf(startDate)
      val end = sdf.parse(endDate) ?: return listOf(startDate)
      val cal = Calendar.getInstance().apply { time = start }
      val result = mutableListOf<String>()
      var count = 0
      while (!cal.time.after(end) && count < maxDays.coerceAtMost(36)) {
        result.add(sdf.format(cal.time))
        cal.add(Calendar.DAY_OF_MONTH, 1)
        count++
      }
      if (result.isEmpty()) listOf(startDate) else result
    } catch (e: Exception) {
      listOf(startDate)
    }
  }

  // --- DISK PERSISTENCE ---

  private fun saveToDisk() {
    val ctx = appContext ?: return
    try {
      val root = JSONObject()
      root.put("currentEmail", _currentAccount.value?.email ?: "")

      val accArr = JSONArray()
      accountsMap.value.values.forEach { acc ->
        accArr.put(
          JSONObject().apply {
            put("userId", acc.userId)
            put("email", acc.email)
            put("passwordHash", acc.passwordHash)
            put("displayName", acc.displayName)
            put("createdAtSeconds", acc.createdAtSeconds)
          }
        )
      }
      root.put("accounts", accArr)

      val adminsArr = JSONArray()
      adminsMap.value.values.forEach { a ->
        adminsArr.put(
          JSONObject().apply {
            put("adminId", a.adminId)
            put("fullName", a.fullName)
            put("assignedTradeId", a.assignedTradeId)
            put("assignedTradeName", a.assignedTradeName)
            put("sessionYear", a.sessionYear)
            put("officerVerificationCode", a.officerVerificationCode)
          }
        )
      }
      root.put("admins", adminsArr)

      val usersArr = JSONArray()
      usersMap.value.values.forEach { u ->
        usersArr.put(
          JSONObject().apply {
            put("userId", u.userId)
            put("fullName", u.fullName)
            put("registrationNumber", u.registrationNumber)
            put("role", u.role)
            put("tradeId", u.tradeId)
            put("tradeName", u.tradeName)
            put("sessionYear", u.sessionYear)
            put("isActive", u.isActive)
          }
        )
      }
      root.put("users", usersArr)

      val tradesArr = JSONArray()
      tradesMap.value.values.forEach { t ->
        tradesArr.put(
          JSONObject().apply {
            put("tradeId", t.tradeId)
            put("tradeName", t.tradeName)
            put("tradeCode", t.tradeCode)
            put("adminId", t.adminId)
            put("adminName", t.adminName)
            put("sessionYear", t.sessionYear)
            put("totalWorkingDays", t.totalWorkingDays)
            put("maxCasualLeave", t.maxCasualLeave)
            put("maxMedicalLeave", t.maxMedicalLeave)
            put("halfDayDeductionRule", t.halfDayDeductionRule)
            put("maxHalfDayLeaves", t.maxHalfDayLeaves)
            put("minAttendancePercent", t.minAttendancePercent)
          }
        )
      }
      root.put("trades", tradesArr)

      val traineesArr = JSONArray()
      traineesMap.value.values.forEach { tr ->
        traineesArr.put(
          JSONObject().apply {
            put("traineeId", tr.traineeId)
            put("traineeUid", tr.traineeUid)
            put("adminId", tr.adminId)
            put("tradeId", tr.tradeId)
            put("tradeName", tr.tradeName)
            put("sessionYear", tr.sessionYear)
            put("fullName", tr.fullName)
            put("registrationNumber", tr.registrationNumber)
            put("fatherName", tr.fatherName)
            put("batchShift", tr.batchShift)
            put("isActive", tr.isActive)
            put("presentDays", tr.presentDays)
            put("absentDays", tr.absentDays)
            put("halfDays", tr.halfDays)
            put("approvedLeaveDays", tr.approvedLeaveDays)
            put("casualLeaveUsed", tr.casualLeaveUsed)
            put("medicalLeaveUsed", tr.medicalLeaveUsed)
            put("halfDayLeaveUsed", tr.halfDayLeaveUsed)
            put("lowAttendanceWarningCount", tr.lowAttendanceWarningCount)
            put("lastWarningMessage", tr.lastWarningMessage)
          }
        )
      }
      root.put("trainees", traineesArr)

      val attArr = JSONArray()
      attendanceMap.value.values.forEach { at ->
        attArr.put(
          JSONObject().apply {
            put("attendanceId", at.attendanceId)
            put("traineeId", at.traineeId)
            put("traineeUid", at.traineeUid)
            put("traineeName", at.traineeName)
            put("registrationNumber", at.registrationNumber)
            put("tradeId", at.tradeId)
            put("tradeName", at.tradeName)
            put("adminId", at.adminId)
            put("dateString", at.dateString)
            put("monthString", at.monthString)
            put("sessionYear", at.sessionYear)
            put("status", at.status)
            put("markedByRole", at.markedByRole)
            put("remarks", at.remarks)
            put("checkInTime", at.checkInTime)
          }
        )
      }
      root.put("attendance", attArr)

      val leavesArr = JSONArray()
      leavesMap.value.values.forEach { lv ->
        leavesArr.put(
          JSONObject().apply {
            put("leaveId", lv.leaveId)
            put("traineeId", lv.traineeId)
            put("traineeUid", lv.traineeUid)
            put("traineeName", lv.traineeName)
            put("registrationNumber", lv.registrationNumber)
            put("tradeId", lv.tradeId)
            put("tradeName", lv.tradeName)
            put("adminId", lv.adminId)
            put("sessionYear", lv.sessionYear)
            put("leaveType", lv.leaveType)
            put("startDate", lv.startDate)
            put("endDate", lv.endDate)
            put("monthString", lv.monthString)
            put("totalDays", lv.totalDays)
            put("halfDaySession", lv.halfDaySession)
            put("reason", lv.reason)
            put("status", lv.status)
            put("adminRemarks", lv.adminRemarks)
            put("reviewedBy", lv.reviewedBy)
          }
        )
      }
      root.put("leaves", leavesArr)

      val notifArr = JSONArray()
      notificationsMap.value.values.forEach { nf ->
        notifArr.put(
          JSONObject().apply {
            put("notificationId", nf.notificationId)
            put("traineeId", nf.traineeId)
            put("traineeUid", nf.traineeUid)
            put("traineeName", nf.traineeName)
            put("tradeId", nf.tradeId)
            put("adminId", nf.adminId)
            put("type", nf.type)
            put("title", nf.title)
            put("message", nf.message)
            put("isRead", nf.isRead)
          }
        )
      }
      root.put("notifications", notifArr)

      File(ctx.filesDir, STORAGE_FILE_NAME).writeText(root.toString())
    } catch (_: Exception) {
    }
  }

  private fun loadFromDisk() {
    val ctx = appContext ?: return
    val file = File(ctx.filesDir, STORAGE_FILE_NAME)
    if (!file.exists()) return
    try {
      val root = JSONObject(file.readText())
      val now = Timestamp.now()

      val loadedAccounts = mutableMapOf<String, LocalAuthAccount>()
      val accArr = root.optJSONArray("accounts") ?: JSONArray()
      for (i in 0 until accArr.length()) {
        val o = accArr.getJSONObject(i)
        val acc = LocalAuthAccount(
          userId = o.optString("userId"),
          email = o.optString("email"),
          passwordHash = o.optString("passwordHash"),
          displayName = o.optString("displayName"),
          createdAtSeconds = o.optLong("createdAtSeconds")
        )
        if (acc.email.isNotBlank()) loadedAccounts[acc.email] = acc
      }
      accountsMap.value = loadedAccounts

      val loadedAdmins = mutableMapOf<String, AdminProfile>()
      val adminsArr = root.optJSONArray("admins") ?: JSONArray()
      for (i in 0 until adminsArr.length()) {
        val o = adminsArr.getJSONObject(i)
        val a = AdminProfile(
          adminId = o.optString("adminId"),
          fullName = o.optString("fullName"),
          assignedTradeId = o.optString("assignedTradeId"),
          assignedTradeName = o.optString("assignedTradeName"),
          sessionYear = o.optString("sessionYear"),
          officerVerificationCode = o.optString("officerVerificationCode", "ITI-RAIGARH-OFFICER"),
          createdAt = now,
          updatedAt = now
        )
        if (a.adminId.isNotBlank()) loadedAdmins[a.adminId] = a
      }
      adminsMap.value = loadedAdmins

      val loadedUsers = mutableMapOf<String, UserProfile>()
      val usersArr = root.optJSONArray("users") ?: JSONArray()
      for (i in 0 until usersArr.length()) {
        val o = usersArr.getJSONObject(i)
        val u = UserProfile(
          userId = o.optString("userId"),
          fullName = o.optString("fullName"),
          registrationNumber = o.optString("registrationNumber"),
          role = o.optString("role"),
          tradeId = o.optString("tradeId"),
          tradeName = o.optString("tradeName"),
          sessionYear = o.optString("sessionYear"),
          isActive = o.optBoolean("isActive", true),
          createdAt = now,
          updatedAt = now
        )
        if (u.userId.isNotBlank()) loadedUsers[u.userId] = u
      }
      usersMap.value = loadedUsers

      val loadedTrades = mutableMapOf<String, Trade>()
      val tradesArr = root.optJSONArray("trades") ?: JSONArray()
      for (i in 0 until tradesArr.length()) {
        val o = tradesArr.getJSONObject(i)
        val t = Trade(
          tradeId = o.optString("tradeId"),
          tradeName = o.optString("tradeName"),
          tradeCode = o.optString("tradeCode"),
          adminId = o.optString("adminId"),
          adminName = o.optString("adminName"),
          sessionYear = o.optString("sessionYear"),
          totalWorkingDays = o.optInt("totalWorkingDays", 240),
          maxCasualLeave = o.optInt("maxCasualLeave", 12),
          maxMedicalLeave = o.optInt("maxMedicalLeave", 36),
          halfDayDeductionRule = o.optString("halfDayDeductionRule", "DEDUCT_HALF_CL"),
          maxHalfDayLeaves = o.optInt("maxHalfDayLeaves", 12),
          minAttendancePercent = o.optInt("minAttendancePercent", 75),
          createdAt = now,
          updatedAt = now
        )
        if (t.tradeId.isNotBlank()) loadedTrades[t.tradeId] = t
      }
      tradesMap.value = loadedTrades

      val loadedTrainees = mutableMapOf<String, TraineeRecord>()
      val traineesArr = root.optJSONArray("trainees") ?: JSONArray()
      for (i in 0 until traineesArr.length()) {
        val o = traineesArr.getJSONObject(i)
        val tr = TraineeRecord(
          traineeId = o.optString("traineeId"),
          traineeUid = o.optString("traineeUid"),
          adminId = o.optString("adminId"),
          tradeId = o.optString("tradeId"),
          tradeName = o.optString("tradeName"),
          sessionYear = o.optString("sessionYear"),
          fullName = o.optString("fullName"),
          registrationNumber = o.optString("registrationNumber"),
          fatherName = o.optString("fatherName"),
          batchShift = o.optString("batchShift"),
          isActive = o.optBoolean("isActive", true),
          presentDays = o.optInt("presentDays", 0),
          absentDays = o.optInt("absentDays", 0),
          halfDays = o.optInt("halfDays", 0),
          approvedLeaveDays = o.optInt("approvedLeaveDays", 0),
          casualLeaveUsed = o.optInt("casualLeaveUsed", 0),
          medicalLeaveUsed = o.optInt("medicalLeaveUsed", 0),
          halfDayLeaveUsed = o.optInt("halfDayLeaveUsed", 0),
          lowAttendanceWarningCount = o.optInt("lowAttendanceWarningCount", 0),
          lastWarningMessage = o.optString("lastWarningMessage", ""),
          createdAt = now,
          updatedAt = now
        )
        if (tr.traineeId.isNotBlank()) loadedTrainees[tr.traineeId] = tr
      }
      traineesMap.value = loadedTrainees

      val loadedAttendance = mutableMapOf<String, AttendanceRecord>()
      val attArr = root.optJSONArray("attendance") ?: JSONArray()
      for (i in 0 until attArr.length()) {
        val o = attArr.getJSONObject(i)
        val at = AttendanceRecord(
          attendanceId = o.optString("attendanceId"),
          traineeId = o.optString("traineeId"),
          traineeUid = o.optString("traineeUid"),
          traineeName = o.optString("traineeName"),
          registrationNumber = o.optString("registrationNumber"),
          tradeId = o.optString("tradeId"),
          tradeName = o.optString("tradeName"),
          adminId = o.optString("adminId"),
          dateString = o.optString("dateString"),
          monthString = o.optString("monthString"),
          sessionYear = o.optString("sessionYear"),
          status = o.optString("status"),
          markedByRole = o.optString("markedByRole"),
          remarks = o.optString("remarks"),
          checkInTime = o.optString("checkInTime"),
          createdAt = now,
          updatedAt = now
        )
        if (at.attendanceId.isNotBlank()) loadedAttendance[at.attendanceId] = at
      }
      attendanceMap.value = loadedAttendance

      val loadedLeaves = mutableMapOf<String, LeaveApplication>()
      val leavesArr = root.optJSONArray("leaves") ?: JSONArray()
      for (i in 0 until leavesArr.length()) {
        val o = leavesArr.getJSONObject(i)
        val lv = LeaveApplication(
          leaveId = o.optString("leaveId"),
          traineeId = o.optString("traineeId"),
          traineeUid = o.optString("traineeUid"),
          traineeName = o.optString("traineeName"),
          registrationNumber = o.optString("registrationNumber"),
          tradeId = o.optString("tradeId"),
          tradeName = o.optString("tradeName"),
          adminId = o.optString("adminId"),
          sessionYear = o.optString("sessionYear"),
          leaveType = o.optString("leaveType"),
          startDate = o.optString("startDate"),
          endDate = o.optString("endDate"),
          monthString = o.optString("monthString"),
          totalDays = o.optInt("totalDays", 1),
          halfDaySession = o.optString("halfDaySession", "NONE"),
          reason = o.optString("reason"),
          status = o.optString("status"),
          adminRemarks = o.optString("adminRemarks"),
          reviewedBy = o.optString("reviewedBy"),
          createdAt = now,
          updatedAt = now
        )
        if (lv.leaveId.isNotBlank()) loadedLeaves[lv.leaveId] = lv
      }
      leavesMap.value = loadedLeaves

      val loadedNotifs = mutableMapOf<String, TraineeNotification>()
      val notifArr = root.optJSONArray("notifications") ?: JSONArray()
      for (i in 0 until notifArr.length()) {
        val o = notifArr.getJSONObject(i)
        val nf = TraineeNotification(
          notificationId = o.optString("notificationId"),
          traineeId = o.optString("traineeId"),
          traineeUid = o.optString("traineeUid"),
          traineeName = o.optString("traineeName"),
          tradeId = o.optString("tradeId"),
          adminId = o.optString("adminId"),
          type = o.optString("type"),
          title = o.optString("title"),
          message = o.optString("message"),
          isRead = o.optBoolean("isRead", false),
          createdAt = now,
          updatedAt = now
        )
        if (nf.notificationId.isNotBlank()) loadedNotifs[nf.notificationId] = nf
      }
      notificationsMap.value = loadedNotifs

      val currentEmail = root.optString("currentEmail", "")
      if (currentEmail.isNotBlank()) {
        _currentAccount.value = loadedAccounts[currentEmail]
      }
    } catch (_: Exception) {
    }
  }
}
