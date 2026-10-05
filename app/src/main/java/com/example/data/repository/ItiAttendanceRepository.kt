package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.LocalItiStore
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
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ItiAttendanceRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth
    get() = Firebase.auth

  private val useLocalStore: Boolean
    get() = auth.currentUser == null && LocalItiStore.currentUserId != null

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: LocalItiStore.currentUserId
      ?: throw IllegalStateException("User must be signed in before accessing attendance data.")
  }

  private fun cleanId(raw: String): String =
    raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifEmpty {
      "id_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    }

  // --- REAL-TIME OBSERVATION FLOWS ---

  fun observeUserProfile(userId: String): Flow<UserProfile?> {
    if (useLocalStore) return LocalItiStore.observeUserProfile(userId)
    val path = "users/$userId"
    return db.collection("users").document(userId)
      .snapshots()
      .map { snapshot -> UserProfile.fromSnapshot(snapshot) }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
        throw error
      }
  }

  fun observeAdminProfile(adminId: String): Flow<AdminProfile?> {
    if (useLocalStore) return LocalItiStore.observeAdminProfile(adminId)
    val path = "admins/$adminId"
    return db.collection("admins").document(adminId)
      .snapshots()
      .map { snapshot -> AdminProfile.fromSnapshot(snapshot) }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
        throw error
      }
  }

  fun observeTrade(tradeId: String): Flow<Trade?> {
    if (useLocalStore) return LocalItiStore.observeTrade(tradeId)
    val path = "trades/$tradeId"
    return db.collection("trades").document(tradeId)
      .snapshots()
      .map { snapshot -> Trade.fromSnapshot(snapshot) }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
        throw error
      }
  }

  fun observeTraineesForTrade(adminId: String, tradeId: String): Flow<List<TraineeRecord>> {
    if (useLocalStore) return LocalItiStore.observeTraineesForTrade(adminId, tradeId)
    val path = "trainees"
    return db.collection("trainees")
      .whereEqualTo("adminId", adminId)
      .whereEqualTo("tradeId", tradeId)
      .snapshots()
      .map { querySnapshot ->
        querySnapshot.documents.mapNotNull { TraineeRecord.fromSnapshot(it) }
          .sortedBy { it.registrationNumber }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeTraineeByUid(traineeUid: String): Flow<TraineeRecord?> {
    if (useLocalStore) return LocalItiStore.observeTraineeByUid(traineeUid)
    val path = "trainees"
    return db.collection("trainees")
      .whereEqualTo("traineeUid", traineeUid)
      .snapshots()
      .map { querySnapshot ->
        querySnapshot.documents.mapNotNull { TraineeRecord.fromSnapshot(it) }.firstOrNull()
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeAttendanceForTrade(adminId: String, tradeId: String): Flow<List<AttendanceRecord>> {
    if (useLocalStore) return LocalItiStore.observeAttendanceForTrade(adminId, tradeId)
    val path = "attendance"
    return db.collection("attendance")
      .whereEqualTo("adminId", adminId)
      .whereEqualTo("tradeId", tradeId)
      .snapshots()
      .map { querySnapshot ->
        querySnapshot.documents.mapNotNull { AttendanceRecord.fromSnapshot(it) }
          .sortedByDescending { it.dateString }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeAttendanceForTrainee(traineeUid: String): Flow<List<AttendanceRecord>> {
    if (useLocalStore) return LocalItiStore.observeAttendanceForTrainee(traineeUid)
    val path = "attendance"
    return db.collection("attendance")
      .whereEqualTo("traineeUid", traineeUid)
      .snapshots()
      .map { querySnapshot ->
        querySnapshot.documents.mapNotNull { AttendanceRecord.fromSnapshot(it) }
          .sortedByDescending { it.dateString }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeLeavesForTrade(adminId: String, tradeId: String): Flow<List<LeaveApplication>> {
    if (useLocalStore) return LocalItiStore.observeLeavesForTrade(adminId, tradeId)
    val path = "leaves"
    return db.collection("leaves")
      .whereEqualTo("adminId", adminId)
      .whereEqualTo("tradeId", tradeId)
      .snapshots()
      .map { querySnapshot ->
        querySnapshot.documents.mapNotNull { LeaveApplication.fromSnapshot(it) }
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeLeavesForTrainee(traineeUid: String): Flow<List<LeaveApplication>> {
    if (useLocalStore) return LocalItiStore.observeLeavesForTrainee(traineeUid)
    val path = "leaves"
    return db.collection("leaves")
      .whereEqualTo("traineeUid", traineeUid)
      .snapshots()
      .map { querySnapshot ->
        querySnapshot.documents.mapNotNull { LeaveApplication.fromSnapshot(it) }
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeNotificationsForTrainee(traineeUid: String): Flow<List<TraineeNotification>> {
    if (useLocalStore) return LocalItiStore.observeNotificationsForTrainee(traineeUid)
    val path = "notifications"
    return db.collection("notifications")
      .whereEqualTo("traineeUid", traineeUid)
      .snapshots()
      .map { querySnapshot ->
        querySnapshot.documents.mapNotNull { TraineeNotification.fromSnapshot(it) }
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeNotificationsForTrade(adminId: String, tradeId: String): Flow<List<TraineeNotification>> {
    if (useLocalStore) return LocalItiStore.observeNotificationsForTrade(adminId, tradeId)
    val path = "notifications"
    return db.collection("notifications")
      .whereEqualTo("adminId", adminId)
      .whereEqualTo("tradeId", tradeId)
      .snapshots()
      .map { querySnapshot ->
        querySnapshot.documents.mapNotNull { TraineeNotification.fromSnapshot(it) }
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  // --- ONBOARDING & ROLE REGISTRATION ---

  suspend fun registerTrainingOfficer(
    fullName: String,
    employeeCode: String,
    tradeId: String,
    tradeName: String,
    tradeCode: String,
    sessionYear: String,
    officerVerificationCode: String = "ITI-RAIGARH-OFFICER"
  ): Result<UserProfile> = runCatching {
    val uid = cleanId(requireUserId())
    if (useLocalStore) {
      return@runCatching LocalItiStore.registerTrainingOfficer(
        uid = uid,
        fullName = fullName,
        employeeCode = employeeCode,
        tradeId = tradeId,
        tradeName = tradeName,
        tradeCode = tradeCode,
        sessionYear = sessionYear,
        officerVerificationCode = officerVerificationCode
      )
    }
    val safeTradeId = cleanId("${tradeId}_$uid")

    val adminProfile = AdminProfile(
      adminId = uid,
      fullName = fullName.trim(),
      assignedTradeId = safeTradeId,
      assignedTradeName = tradeName.trim(),
      sessionYear = sessionYear.trim(),
      officerVerificationCode = officerVerificationCode.trim()
    )
    try {
      db.collection("admins").document(uid).set(adminProfile.toCreateMap()).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "admins/$uid")
      throw e
    }

    val userProfile = UserProfile(
      userId = uid,
      fullName = fullName.trim(),
      registrationNumber = employeeCode.trim(),
      role = UserRole.ADMIN.value,
      tradeId = safeTradeId,
      tradeName = tradeName.trim(),
      sessionYear = sessionYear.trim(),
      isActive = true
    )
    try {
      db.collection("users").document(uid).set(userProfile.toCreateMap()).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "users/$uid")
      throw e
    }

    val trade = Trade(
      tradeId = safeTradeId,
      tradeName = tradeName.trim(),
      tradeCode = tradeCode.trim(),
      adminId = uid,
      adminName = fullName.trim(),
      sessionYear = sessionYear.trim(),
      totalWorkingDays = 240,
      maxCasualLeave = 12,
      maxMedicalLeave = 36,
      halfDayDeductionRule = "DEDUCT_HALF_CL",
      maxHalfDayLeaves = 12,
      minAttendancePercent = 75
    )
    try {
      db.collection("trades").document(safeTradeId).set(trade.toCreateMap()).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "trades/$safeTradeId")
      throw e
    }

    // Also create a linked TraineeRecord for the user so they can preview Trainee portal seamlessly
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
      halfDayLeaveUsed = 1
    )
    try {
      db.collection("trainees").document(uid).set(selfTrainee.toCreateMap()).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "trainees/$uid")
      throw e
    }

    userProfile
  }

  suspend fun registerTraineeUser(
    fullName: String,
    registrationNumber: String,
    fatherName: String,
    batchShift: String,
    tradeId: String,
    tradeName: String,
    sessionYear: String,
    adminId: String = ""
  ): Result<UserProfile> = runCatching {
    val uid = cleanId(requireUserId())
    if (useLocalStore) {
      return@runCatching LocalItiStore.registerTraineeUser(
        uid = uid,
        fullName = fullName,
        registrationNumber = registrationNumber,
        fatherName = fatherName,
        batchShift = batchShift,
        tradeId = tradeId,
        tradeName = tradeName,
        sessionYear = sessionYear,
        adminId = adminId
      )
    }
    val safeTradeId = cleanId(tradeId)
    val effectiveAdminId = if (adminId.isNotBlank()) cleanId(adminId) else uid

    val userProfile = UserProfile(
      userId = uid,
      fullName = fullName.trim(),
      registrationNumber = registrationNumber.trim(),
      role = UserRole.TRAINEE.value,
      tradeId = safeTradeId,
      tradeName = tradeName.trim(),
      sessionYear = sessionYear.trim(),
      isActive = true
    )
    try {
      db.collection("users").document(uid).set(userProfile.toCreateMap()).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "users/$uid")
      throw e
    }

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
      lastWarningMessage = ""
    )
    try {
      db.collection("trainees").document(uid).set(traineeRecord.toCreateMap()).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "trainees/$uid")
      throw e
    }

    userProfile
  }

  suspend fun switchRoleForCurrentUser(
    currentProfile: UserProfile,
    targetRole: UserRole
  ): Result<Unit> = runCatching {
    val uid = cleanId(requireUserId())
    if (useLocalStore) {
      LocalItiStore.switchRoleForCurrentUser(uid, currentProfile, targetRole)
      return@runCatching
    }
    if (targetRole == UserRole.ADMIN) {
      val adminDocRef = db.collection("admins").document(uid)
      val adminSnapshot = try {
        adminDocRef.get().await()
      } catch (e: Exception) {
        null
      }
      if (adminSnapshot == null || !adminSnapshot.exists()) {
        val adminProfile = AdminProfile(
          adminId = uid,
          fullName = currentProfile.fullName,
          assignedTradeId = currentProfile.tradeId,
          assignedTradeName = currentProfile.tradeName,
          sessionYear = currentProfile.sessionYear,
          officerVerificationCode = "ITI-RAIGARH-OFFICER"
        )
        try {
          adminDocRef.set(adminProfile.toCreateMap()).await()
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.CREATE, "admins/$uid")
          throw e
        }
      }

      val tradeDocRef = db.collection("trades").document(currentProfile.tradeId)
      val tradeSnap = try {
        tradeDocRef.get().await()
      } catch (e: Exception) {
        null
      }
      if (tradeSnap == null || !tradeSnap.exists()) {
        val trade = Trade(
          tradeId = currentProfile.tradeId,
          tradeName = currentProfile.tradeName,
          tradeCode = "ITI-RGH-${currentProfile.tradeName.take(4).uppercase()}",
          adminId = uid,
          adminName = currentProfile.fullName,
          sessionYear = currentProfile.sessionYear
        )
        try {
          tradeDocRef.set(trade.toCreateMap()).await()
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.CREATE, "trades/${currentProfile.tradeId}")
          throw e
        }
      }
    }

    try {
      db.collection("users").document(uid).update(
        mapOf(
          "role" to targetRole.value,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
      throw e
    }
  }

  // --- ADMIN: TRADE & ACADEMIC SESSION CONFIGURATION ---

  suspend fun updateTradeConfiguration(
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
  ): Result<Unit> = runCatching {
    val uid = cleanId(requireUserId())
    if (useLocalStore) {
      LocalItiStore.updateTradeConfiguration(
        uid = uid,
        tradeId = tradeId,
        tradeName = tradeName,
        tradeCode = tradeCode,
        adminName = adminName,
        sessionYear = sessionYear,
        totalWorkingDays = totalWorkingDays,
        maxCasualLeave = maxCasualLeave,
        maxMedicalLeave = maxMedicalLeave,
        halfDayDeductionRule = halfDayDeductionRule,
        maxHalfDayLeaves = maxHalfDayLeaves,
        minAttendancePercent = minAttendancePercent
      )
      return@runCatching
    }
    val updates = mapOf(
      "tradeName" to tradeName.trim(),
      "tradeCode" to tradeCode.trim(),
      "adminName" to adminName.trim(),
      "sessionYear" to sessionYear.trim(),
      "totalWorkingDays" to totalWorkingDays.coerceIn(1, 366),
      "maxCasualLeave" to maxCasualLeave.coerceIn(1, 12),
      "maxMedicalLeave" to maxMedicalLeave.coerceIn(1, 36),
      "halfDayDeductionRule" to halfDayDeductionRule,
      "maxHalfDayLeaves" to maxHalfDayLeaves.coerceIn(1, 24),
      "minAttendancePercent" to minAttendancePercent.coerceIn(1, 100),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      db.collection("trades").document(tradeId).update(updates).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "trades/$tradeId")
      throw e
    }

    try {
      db.collection("admins").document(uid).update(
        mapOf(
          "assignedTradeName" to tradeName.trim(),
          "sessionYear" to sessionYear.trim(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "admins/$uid")
    }
  }

  // --- ADMIN: TRAINEE ACCOUNT MANAGEMENT (CREATE / EDIT / DEACTIVATE) ---

  suspend fun createTraineeInTrade(
    tradeId: String,
    tradeName: String,
    sessionYear: String,
    fullName: String,
    registrationNumber: String,
    fatherName: String,
    batchShift: String,
    customTraineeId: String? = null
  ): Result<TraineeRecord> = runCatching {
    val adminUid = cleanId(requireUserId())
    if (useLocalStore) {
      return@runCatching LocalItiStore.createTraineeInTrade(
        adminUid = adminUid,
        tradeId = tradeId,
        tradeName = tradeName,
        sessionYear = sessionYear,
        fullName = fullName,
        registrationNumber = registrationNumber,
        fatherName = fatherName,
        batchShift = batchShift,
        customTraineeId = customTraineeId
      )
    }
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
      lastWarningMessage = ""
    )

    try {
      db.collection("trainees").document(traineeId).set(trainee.toCreateMap()).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "trainees/$traineeId")
      throw e
    }

    trainee
  }

  suspend fun updateTraineeAccount(
    traineeId: String,
    fullName: String,
    registrationNumber: String,
    fatherName: String,
    batchShift: String,
    sessionYear: String,
    isActive: Boolean
  ): Result<Unit> = runCatching {
    requireUserId()
    if (useLocalStore) {
      LocalItiStore.updateTraineeAccount(
        traineeId = traineeId,
        fullName = fullName,
        registrationNumber = registrationNumber,
        fatherName = fatherName,
        batchShift = batchShift,
        sessionYear = sessionYear,
        isActive = isActive
      )
      return@runCatching
    }
    val updates = mapOf(
      "fullName" to fullName.trim(),
      "registrationNumber" to registrationNumber.trim(),
      "fatherName" to fatherName.trim(),
      "batchShift" to batchShift.trim(),
      "sessionYear" to sessionYear.trim(),
      "isActive" to isActive,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      db.collection("trainees").document(traineeId).update(updates).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "trainees/$traineeId")
      throw e
    }
  }

  suspend fun getTraineeById(traineeId: String): Result<TraineeRecord> = runCatching {
    if (useLocalStore) {
      return@runCatching LocalItiStore.getTraineeById(traineeId)
    }
    val path = "trainees/$traineeId"
    val doc = try {
      db.collection("trainees").document(traineeId).get().await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      throw e
    }
    TraineeRecord.fromSnapshot(doc)
      ?: throw IllegalStateException("Trainee not found: $traineeId")
  }

  // --- ATTENDANCE MARKING (1 RECORD PER TRAINEE PER DAY) ---

  suspend fun markDailyAttendanceByTrainee(
    trainee: TraineeRecord,
    dateString: String,
    status: AttendanceStatus = AttendanceStatus.PRESENT,
    remarks: String = "Marked via Trainee Portal"
  ): Result<AttendanceRecord> = runCatching {
    val uid = cleanId(requireUserId())
    if (useLocalStore) {
      return@runCatching LocalItiStore.markDailyAttendanceByTrainee(
        uid = uid,
        trainee = trainee,
        dateString = dateString,
        status = status,
        remarks = remarks
      )
    }
    if (!trainee.isActive) {
      throw IllegalStateException("Your trainee account is currently deactivated. Contact your Training Officer.")
    }
    if (status != AttendanceStatus.PRESENT && status != AttendanceStatus.HALF_DAY) {
      throw IllegalArgumentException("Trainees can only mark Present or Half-Day attendance.")
    }

    val attendanceId = "${trainee.traineeId}_$dateString"
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
      checkInTime = checkInTime
    )

    try {
      db.collection("attendance").document(attendanceId).set(record.toCreateMap()).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "attendance/$attendanceId")
      throw e
    }

    // Update trainee counters
    val newPresent = if (status == AttendanceStatus.PRESENT) trainee.presentDays + 1 else trainee.presentDays
    val newHalf = if (status == AttendanceStatus.HALF_DAY) trainee.halfDays + 1 else trainee.halfDays
    try {
      db.collection("trainees").document(trainee.traineeId).update(
        mapOf(
          "presentDays" to newPresent.coerceAtMost(366),
          "halfDays" to newHalf.coerceAtMost(366),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "trainees/${trainee.traineeId}")
    }

    record
  }

  suspend fun resetTodayAttendanceForTrainee(
    trainee: TraineeRecord,
    dateString: String
  ): Result<Unit> = runCatching {
    requireUserId()
    if (useLocalStore) {
      LocalItiStore.resetTodayAttendanceForTrainee(trainee, dateString)
    }
  }

  suspend fun markOrUpdateAttendanceByAdmin(
    trainee: TraineeRecord,
    dateString: String,
    newStatus: AttendanceStatus,
    existingRecord: AttendanceRecord?,
    remarks: String = "Recorded by Training Officer"
  ): Result<AttendanceRecord> = runCatching {
    val adminUid = cleanId(requireUserId())
    if (useLocalStore) {
      return@runCatching LocalItiStore.markOrUpdateAttendanceByAdmin(
        adminUid = adminUid,
        trainee = trainee,
        dateString = dateString,
        newStatus = newStatus,
        existingRecord = existingRecord,
        remarks = remarks
      )
    }
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
      checkInTime = checkInTime
    )

    if (existingRecord == null) {
      try {
        db.collection("attendance").document(attendanceId).set(record.toCreateMap()).await()
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "attendance/$attendanceId")
        throw e
      }
    } else {
      try {
        db.collection("attendance").document(attendanceId).update(
          mapOf(
            "status" to newStatus.value,
            "markedByRole" to UserRole.ADMIN.value,
            "remarks" to remarks.trim().take(200),
            "checkInTime" to checkInTime,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "attendance/$attendanceId")
        throw e
      }
    }

    // Adjust counters on TraineeRecord
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

    try {
      db.collection("trainees").document(trainee.traineeId).update(
        mapOf(
          "presentDays" to present,
          "absentDays" to absent,
          "halfDays" to half,
          "approvedLeaveDays" to onLeave,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "trainees/${trainee.traineeId}")
    }

    record
  }

  // --- LEAVE APPLICATION & APPROVAL WORKFLOW ---

  suspend fun submitLeaveApplication(
    trainee: TraineeRecord,
    leaveType: LeaveType,
    startDate: String,
    endDate: String,
    totalDays: Int,
    halfDaySession: HalfDaySession,
    reason: String
  ): Result<LeaveApplication> = runCatching {
    requireUserId()
    if (useLocalStore) {
      return@runCatching LocalItiStore.submitLeaveApplication(
        trainee = trainee,
        leaveType = leaveType,
        startDate = startDate,
        endDate = endDate,
        totalDays = totalDays,
        halfDaySession = halfDaySession,
        reason = reason
      )
    }
    if (!trainee.isActive) {
      throw IllegalStateException("Inactive trainee account cannot apply for leave.")
    }
    val trimmedReason = reason.trim()
    require(trimmedReason.length >= 3) { "Please provide a valid reason (at least 3 characters)." }

    // Enforce Leave Limits Automatically
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
      reviewedBy = ""
    )

    try {
      db.collection("leaves").document(leaveId).set(application.toCreateMap()).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "leaves/$leaveId")
      throw e
    }

    application
  }

  suspend fun reviewLeaveApplication(
    leave: LeaveApplication,
    trainee: TraineeRecord,
    approve: Boolean,
    adminRemarks: String,
    officerName: String
  ): Result<Unit> = runCatching {
    val adminUid = cleanId(requireUserId())
    if (useLocalStore) {
      LocalItiStore.reviewLeaveApplication(
        adminUid = adminUid,
        leave = leave,
        trainee = trainee,
        approve = approve,
        adminRemarks = adminRemarks,
        officerName = officerName
      )
      return@runCatching
    }
    val newStatus = if (approve) LeaveStatus.APPROVED else LeaveStatus.REJECTED
    val remarks = adminRemarks.trim().ifEmpty {
      if (approve) "Approved by Training Officer" else "Rejected by Training Officer"
    }.take(300)

    // 1. Update LeaveApplication status
    try {
      db.collection("leaves").document(leave.leaveId).update(
        mapOf(
          "status" to newStatus.value,
          "adminRemarks" to remarks,
          "reviewedBy" to officerName.trim().take(100),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "leaves/${leave.leaveId}")
      throw e
    }

    // 2. If Approved, update Trainee leave balance & record leave-approved days in Attendance
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

      try {
        db.collection("trainees").document(trainee.traineeId).update(
          mapOf(
            "casualLeaveUsed" to clUsed,
            "medicalLeaveUsed" to medUsed,
            "halfDayLeaveUsed" to halfUsed,
            "approvedLeaveDays" to approvedLeaveDays,
            "halfDays" to halfDaysCount,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "trainees/${trainee.traineeId}")
      }

      // Record leave-approved days in the attendance system
      val datesToRecord = generateDateRange(leave.startDate, leave.endDate, leave.totalDays)
      val attStatus = if (leave.parsedLeaveType == LeaveType.HALF_DAY) {
        AttendanceStatus.HALF_DAY
      } else {
        AttendanceStatus.ON_LEAVE
      }

      for (dateStr in datesToRecord) {
        val attId = "${trainee.traineeId}_$dateStr"
        val attRecord = AttendanceRecord(
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
          checkInTime = "LEAVE"
        )
        try {
          db.collection("attendance").document(attId).set(attRecord.toCreateMap()).await()
        } catch (e: Exception) {
          // If attendance doc already exists for that day, update it as Admin
          try {
            db.collection("attendance").document(attId).update(
              mapOf(
                "status" to attStatus.value,
                "markedByRole" to UserRole.ADMIN.value,
                "remarks" to "Approved ${leave.parsedLeaveType.label}".take(200),
                "checkInTime" to "LEAVE",
                "updatedAt" to FieldValue.serverTimestamp()
              )
            ).await()
          } catch (inner: Exception) {
            handleFirestoreError(inner, OperationType.UPDATE, "attendance/$attId")
          }
        }
      }
    }

    // 3. Send Notification to Trainee
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
      isRead = false
    )
    try {
      db.collection("notifications").document(notifId).set(notification.toCreateMap()).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "notifications/$notifId")
    }
  }

  // --- ADMIN: LOW ATTENDANCE WARNINGS ---

  suspend fun issueLowAttendanceWarning(
    trainee: TraineeRecord,
    warningMessage: String,
    officerName: String
  ): Result<Unit> = runCatching {
    val adminUid = cleanId(requireUserId())
    if (useLocalStore) {
      LocalItiStore.issueLowAttendanceWarning(
        adminUid = adminUid,
        trainee = trainee,
        warningMessage = warningMessage,
        officerName = officerName
      )
      return@runCatching
    }
    val msg = warningMessage.trim().ifEmpty {
      "Official Warning: Your current attendance is ${trainee.attendancePercentage}%, which is below the mandatory 75% NCVT requirement at Govt. ITI Raigarh."
    }.take(300)

    val newCount = (trainee.lowAttendanceWarningCount + 1).coerceAtMost(100)
    try {
      db.collection("trainees").document(trainee.traineeId).update(
        mapOf(
          "lowAttendanceWarningCount" to newCount,
          "lastWarningMessage" to msg,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "trainees/${trainee.traineeId}")
      throw e
    }

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
      isRead = false
    )
    try {
      db.collection("notifications").document(notifId).set(notification.toCreateMap()).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "notifications/$notifId")
      throw e
    }
  }

  suspend fun markNotificationAsRead(notificationId: String): Result<Unit> = runCatching {
    requireUserId()
    if (useLocalStore) {
      LocalItiStore.markNotificationAsRead(notificationId)
      return@runCatching
    }
    try {
      db.collection("notifications").document(notificationId).update(
        mapOf(
          "isRead" to true,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "notifications/$notificationId")
      throw e
    }
  }

  // --- SEED INITIAL TRADE ROSTER FOR NEW ADMIN ---

  suspend fun seedSampleRosterForTrade(
    tradeId: String,
    tradeName: String,
    sessionYear: String,
    officerName: String,
    todayDate: String
  ): Result<Unit> = runCatching {
    val adminUid = cleanId(requireUserId())
    if (useLocalStore) {
      LocalItiStore.seedSampleRosterForTrade(
        adminUid = adminUid,
        tradeId = tradeId,
        tradeName = tradeName,
        sessionYear = sessionYear,
        todayDate = todayDate
      )
      return@runCatching
    }
    val codePrefix = tradeName.take(3).uppercase()
    val sampleTrainees = listOf(
      Triple("Aarav Patel", "RGH-$codePrefix-25-001", "Ramesh Patel"),
      Triple("Priya Sahu", "RGH-$codePrefix-25-002", "Dinesh Sahu"),
      Triple("Vikram Rathore", "RGH-$codePrefix-25-003", "Suresh Rathore"),
      Triple("Neha Dewangan", "RGH-$codePrefix-25-004", "Kishore Dewangan"),
      Triple("Rohan Verma", "RGH-$codePrefix-25-005", "Manoj Verma")
    )

    sampleTrainees.forEachIndexed { index, (name, regNo, father) ->
      val tId = cleanId("trn_${tradeId}_${index + 1}")
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
        lastWarningMessage = if (isLow) "Attendance below 75% threshold. Please attend classes regularly." else ""
      )

      try {
        db.collection("trainees").document(tId).set(trainee.toCreateMap()).await()
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "trainees/$tId")
      }

      // Mark today's attendance for first 4 trainees
      if (index < 4) {
        val status = when (index) {
          0, 1 -> AttendanceStatus.PRESENT
          2 -> AttendanceStatus.ABSENT
          else -> AttendanceStatus.HALF_DAY
        }
        val attId = "${tId}_$todayDate"
        val att = AttendanceRecord(
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
          markedByRole = UserRole.ADMIN.value,
          remarks = "Morning Roll Call",
          checkInTime = "09:05 AM"
        )
        try {
          db.collection("attendance").document(attId).set(att.toCreateMap()).await()
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.CREATE, "attendance/$attId")
        }
      }

      // Seed 2 pending leave requests so Admin can immediately test Approve / Reject
      if (index == 1 || index == 2) {
        val lvId = cleanId("lv_${tradeId}_${index + 1}")
        val lvType = if (index == 1) LeaveType.CL else LeaveType.MEDICAL
        val days = if (index == 1) 2 else 4
        val leave = LeaveApplication(
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
          reviewedBy = ""
        )
        try {
          db.collection("leaves").document(lvId).set(leave.toCreateMap()).await()
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.CREATE, "leaves/$lvId")
        }
      }
    }
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
}
