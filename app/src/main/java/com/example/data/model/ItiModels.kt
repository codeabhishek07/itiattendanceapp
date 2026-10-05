package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import kotlin.math.roundToInt

enum class UserRole(val value: String) {
  ADMIN("ADMIN"),
  TRAINEE("TRAINEE");

  companion object {
    fun fromString(value: String?): UserRole =
      entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: TRAINEE
  }
}

enum class AttendanceStatus(val value: String, val label: String) {
  PRESENT("PRESENT", "Present"),
  ABSENT("ABSENT", "Absent"),
  ON_LEAVE("ON_LEAVE", "On Leave"),
  HALF_DAY("HALF_DAY", "Half-Day");

  companion object {
    fun fromString(value: String?): AttendanceStatus =
      entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: PRESENT
  }
}

enum class LeaveType(val value: String, val label: String, val maxDays: Int) {
  CL("CL", "Casual Leave (CL)", 12),
  HALF_DAY("HALF_DAY", "Half-Day Leave", 12),
  MEDICAL("MEDICAL", "Medical Leave", 36);

  companion object {
    fun fromString(value: String?): LeaveType =
      entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: CL
  }
}

enum class HalfDaySession(val value: String, val label: String) {
  NONE("NONE", "Full Day"),
  FIRST_HALF("FIRST_HALF", "First Half (Morning)"),
  SECOND_HALF("SECOND_HALF", "Second Half (Afternoon)");

  companion object {
    fun fromString(value: String?): HalfDaySession =
      entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: NONE
  }
}

enum class LeaveStatus(val value: String, val label: String) {
  PENDING("PENDING", "Pending"),
  APPROVED("APPROVED", "Approved"),
  REJECTED("REJECTED", "Rejected");

  companion object {
    fun fromString(value: String?): LeaveStatus =
      entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: PENDING
  }
}

enum class NotificationType(val value: String, val label: String) {
  LEAVE_APPROVED("LEAVE_APPROVED", "Leave Approved"),
  LEAVE_REJECTED("LEAVE_REJECTED", "Leave Rejected"),
  LOW_ATTENDANCE_WARNING("LOW_ATTENDANCE_WARNING", "Low Attendance Warning"),
  GENERAL_NOTICE("GENERAL_NOTICE", "Trade Notice");

  companion object {
    fun fromString(value: String?): NotificationType =
      entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: GENERAL_NOTICE
  }
}

data class PredefinedTradeTemplate(
  val tradeId: String = "",
  val tradeName: String = "",
  val tradeCode: String = "",
  val durationLabel: String = ""
)

val ITI_RAIGARH_TRADE_TEMPLATES = listOf(
  PredefinedTradeTemplate("trade_electrician", "Electrician", "ITI-RGH-ELEC", "2 Years (NCVT)"),
  PredefinedTradeTemplate("trade_fitter", "Fitter", "ITI-RGH-FIT", "2 Years (NCVT)"),
  PredefinedTradeTemplate("trade_copa", "COPA (Computer Operator)", "ITI-RGH-COPA", "1 Year (NCVT)"),
  PredefinedTradeTemplate("trade_welder", "Welder", "ITI-RGH-WELD", "1 Year (NCVT)"),
  PredefinedTradeTemplate("trade_diesel", "Mechanic Diesel", "ITI-RGH-DSL", "1 Year (NCVT)"),
  PredefinedTradeTemplate("trade_turner", "Turner", "ITI-RGH-TURN", "2 Years (NCVT)")
)

data class AdminProfile(
  val adminId: String = "",
  val fullName: String = "",
  val assignedTradeId: String = "",
  val assignedTradeName: String = "",
  val sessionYear: String = "2025-2026",
  val officerVerificationCode: String = "ITI-RAIGARH-OFFICER",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  fun toCreateMap(): Map<String, Any> = mapOf(
    "adminId" to adminId,
    "fullName" to fullName,
    "assignedTradeId" to assignedTradeId,
    "assignedTradeName" to assignedTradeName,
    "sessionYear" to sessionYear,
    "officerVerificationCode" to officerVerificationCode,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  companion object {
    fun fromSnapshot(doc: DocumentSnapshot): AdminProfile? {
      if (!doc.exists()) return null
      return AdminProfile(
        adminId = doc.getString("adminId") ?: doc.id,
        fullName = doc.getString("fullName") ?: "",
        assignedTradeId = doc.getString("assignedTradeId") ?: "",
        assignedTradeName = doc.getString("assignedTradeName") ?: "",
        sessionYear = doc.getString("sessionYear") ?: "2025-2026",
        officerVerificationCode = doc.getString("officerVerificationCode") ?: "ITI-RAIGARH-OFFICER",
        createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
        updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      )
    }
  }
}

data class UserProfile(
  val userId: String = "",
  val fullName: String = "",
  val registrationNumber: String = "",
  val role: String = UserRole.TRAINEE.value,
  val tradeId: String = "trade_electrician",
  val tradeName: String = "Electrician",
  val sessionYear: String = "2025-2026",
  val isActive: Boolean = true,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  val userRole: UserRole
    get() = UserRole.fromString(role)

  fun toCreateMap(): Map<String, Any> = mapOf(
    "userId" to userId,
    "fullName" to fullName,
    "registrationNumber" to registrationNumber,
    "role" to role,
    "tradeId" to tradeId,
    "tradeName" to tradeName,
    "sessionYear" to sessionYear,
    "isActive" to isActive,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  companion object {
    fun fromSnapshot(doc: DocumentSnapshot): UserProfile? {
      if (!doc.exists()) return null
      return UserProfile(
        userId = doc.getString("userId") ?: doc.id,
        fullName = doc.getString("fullName") ?: "",
        registrationNumber = doc.getString("registrationNumber") ?: "",
        role = doc.getString("role") ?: UserRole.TRAINEE.value,
        tradeId = doc.getString("tradeId") ?: "trade_electrician",
        tradeName = doc.getString("tradeName") ?: "Electrician",
        sessionYear = doc.getString("sessionYear") ?: "2025-2026",
        isActive = doc.getBoolean("isActive") ?: true,
        createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
        updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      )
    }
  }
}

data class Trade(
  val tradeId: String = "trade_electrician",
  val tradeName: String = "Electrician",
  val tradeCode: String = "ITI-RGH-ELEC",
  val adminId: String = "",
  val adminName: String = "",
  val sessionYear: String = "2025-2026",
  val totalWorkingDays: Int = 240,
  val maxCasualLeave: Int = 12,
  val maxMedicalLeave: Int = 36,
  val halfDayDeductionRule: String = "DEDUCT_HALF_CL",
  val maxHalfDayLeaves: Int = 12,
  val minAttendancePercent: Int = 75,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  fun toCreateMap(): Map<String, Any> = mapOf(
    "tradeId" to tradeId,
    "tradeName" to tradeName,
    "tradeCode" to tradeCode,
    "adminId" to adminId,
    "adminName" to adminName,
    "sessionYear" to sessionYear,
    "totalWorkingDays" to totalWorkingDays,
    "maxCasualLeave" to maxCasualLeave,
    "maxMedicalLeave" to maxMedicalLeave,
    "halfDayDeductionRule" to halfDayDeductionRule,
    "maxHalfDayLeaves" to maxHalfDayLeaves,
    "minAttendancePercent" to minAttendancePercent,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  companion object {
    fun fromSnapshot(doc: DocumentSnapshot): Trade? {
      if (!doc.exists()) return null
      return Trade(
        tradeId = doc.getString("tradeId") ?: doc.id,
        tradeName = doc.getString("tradeName") ?: "Electrician",
        tradeCode = doc.getString("tradeCode") ?: "ITI-RGH-ELEC",
        adminId = doc.getString("adminId") ?: "",
        adminName = doc.getString("adminName") ?: "",
        sessionYear = doc.getString("sessionYear") ?: "2025-2026",
        totalWorkingDays = (doc.getLong("totalWorkingDays") ?: 240L).toInt(),
        maxCasualLeave = (doc.getLong("maxCasualLeave") ?: 12L).toInt(),
        maxMedicalLeave = (doc.getLong("maxMedicalLeave") ?: 36L).toInt(),
        halfDayDeductionRule = doc.getString("halfDayDeductionRule") ?: "DEDUCT_HALF_CL",
        maxHalfDayLeaves = (doc.getLong("maxHalfDayLeaves") ?: 12L).toInt(),
        minAttendancePercent = (doc.getLong("minAttendancePercent") ?: 75L).toInt(),
        createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
        updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      )
    }
  }
}

data class TraineeRecord(
  val traineeId: String = "",
  val traineeUid: String = "",
  val adminId: String = "",
  val tradeId: String = "trade_electrician",
  val tradeName: String = "Electrician",
  val sessionYear: String = "2025-2026",
  val fullName: String = "",
  val registrationNumber: String = "",
  val fatherName: String = "",
  val batchShift: String = "Shift I (Morning)",
  val isActive: Boolean = true,
  val presentDays: Int = 0,
  val absentDays: Int = 0,
  val halfDays: Int = 0,
  val approvedLeaveDays: Int = 0,
  val casualLeaveUsed: Int = 0,
  val medicalLeaveUsed: Int = 0,
  val halfDayLeaveUsed: Int = 0,
  val lowAttendanceWarningCount: Int = 0,
  val lastWarningMessage: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  val totalRecordedDays: Int
    get() = presentDays + absentDays + halfDays + approvedLeaveDays

  val effectivePresentUnits: Double
    get() = presentDays.toDouble() + (halfDays.toDouble() * 0.5)

  val attendancePercentage: Int
    get() {
      val total = totalRecordedDays
      if (total <= 0) return 0
      return ((effectivePresentUnits / total.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
    }

  /**
   * If today's attendance has not been marked by the trainee upon reaching college,
   * today is automatically counted as 1 Absent day.
   */
  fun effectiveAbsentDays(isTodayMarked: Boolean): Int =
    if (isTodayMarked) absentDays else absentDays + 1

  fun effectiveAttendancePercentage(isTodayMarked: Boolean): Int {
    val total = if (isTodayMarked) totalRecordedDays else totalRecordedDays + 1
    if (total <= 0) return 0
    return ((effectivePresentUnits / total.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
  }

  val effectiveCasualLeaveUsed: Double
    get() = casualLeaveUsed.toDouble() + (halfDayLeaveUsed.toDouble() * 0.5)

  val remainingCasualLeave: Double
    get() = (12.0 - effectiveCasualLeaveUsed).coerceAtLeast(0.0)

  val remainingMedicalLeave: Int
    get() = (36 - medicalLeaveUsed).coerceAtLeast(0)

  val remainingHalfDayLeaves: Int
    get() = (12 - halfDayLeaveUsed).coerceAtLeast(0)

  fun isLowAttendance(thresholdPercent: Int = 75): Boolean =
    totalRecordedDays > 0 && attendancePercentage < thresholdPercent

  fun toCreateMap(): Map<String, Any> = mapOf(
    "traineeId" to traineeId,
    "traineeUid" to traineeUid,
    "adminId" to adminId,
    "tradeId" to tradeId,
    "tradeName" to tradeName,
    "sessionYear" to sessionYear,
    "fullName" to fullName,
    "registrationNumber" to registrationNumber,
    "fatherName" to fatherName,
    "batchShift" to batchShift,
    "isActive" to isActive,
    "presentDays" to presentDays,
    "absentDays" to absentDays,
    "halfDays" to halfDays,
    "approvedLeaveDays" to approvedLeaveDays,
    "casualLeaveUsed" to casualLeaveUsed,
    "medicalLeaveUsed" to medicalLeaveUsed,
    "halfDayLeaveUsed" to halfDayLeaveUsed,
    "lowAttendanceWarningCount" to lowAttendanceWarningCount,
    "lastWarningMessage" to lastWarningMessage,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  companion object {
    fun fromSnapshot(doc: DocumentSnapshot): TraineeRecord? {
      if (!doc.exists()) return null
      return TraineeRecord(
        traineeId = doc.getString("traineeId") ?: doc.id,
        traineeUid = doc.getString("traineeUid") ?: "",
        adminId = doc.getString("adminId") ?: "",
        tradeId = doc.getString("tradeId") ?: "trade_electrician",
        tradeName = doc.getString("tradeName") ?: "Electrician",
        sessionYear = doc.getString("sessionYear") ?: "2025-2026",
        fullName = doc.getString("fullName") ?: "",
        registrationNumber = doc.getString("registrationNumber") ?: "",
        fatherName = doc.getString("fatherName") ?: "",
        batchShift = doc.getString("batchShift") ?: "Shift I (Morning)",
        isActive = doc.getBoolean("isActive") ?: true,
        presentDays = (doc.getLong("presentDays") ?: 0L).toInt(),
        absentDays = (doc.getLong("absentDays") ?: 0L).toInt(),
        halfDays = (doc.getLong("halfDays") ?: 0L).toInt(),
        approvedLeaveDays = (doc.getLong("approvedLeaveDays") ?: 0L).toInt(),
        casualLeaveUsed = (doc.getLong("casualLeaveUsed") ?: 0L).toInt(),
        medicalLeaveUsed = (doc.getLong("medicalLeaveUsed") ?: 0L).toInt(),
        halfDayLeaveUsed = (doc.getLong("halfDayLeaveUsed") ?: 0L).toInt(),
        lowAttendanceWarningCount = (doc.getLong("lowAttendanceWarningCount") ?: 0L).toInt(),
        lastWarningMessage = doc.getString("lastWarningMessage") ?: "",
        createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
        updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      )
    }
  }
}

data class AttendanceRecord(
  val attendanceId: String = "",
  val traineeId: String = "",
  val traineeUid: String = "",
  val traineeName: String = "",
  val registrationNumber: String = "",
  val tradeId: String = "",
  val tradeName: String = "",
  val adminId: String = "",
  val dateString: String = "",
  val monthString: String = "",
  val sessionYear: String = "2025-2026",
  val status: String = AttendanceStatus.PRESENT.value,
  val markedByRole: String = UserRole.TRAINEE.value,
  val remarks: String = "",
  val checkInTime: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  val attendanceStatus: AttendanceStatus
    get() = AttendanceStatus.fromString(status)

  fun toCreateMap(): Map<String, Any> = mapOf(
    "attendanceId" to attendanceId,
    "traineeId" to traineeId,
    "traineeUid" to traineeUid,
    "traineeName" to traineeName,
    "registrationNumber" to registrationNumber,
    "tradeId" to tradeId,
    "tradeName" to tradeName,
    "adminId" to adminId,
    "dateString" to dateString,
    "monthString" to monthString,
    "sessionYear" to sessionYear,
    "status" to status,
    "markedByRole" to markedByRole,
    "remarks" to remarks,
    "checkInTime" to checkInTime,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  companion object {
    fun fromSnapshot(doc: DocumentSnapshot): AttendanceRecord? {
      if (!doc.exists()) return null
      return AttendanceRecord(
        attendanceId = doc.getString("attendanceId") ?: doc.id,
        traineeId = doc.getString("traineeId") ?: "",
        traineeUid = doc.getString("traineeUid") ?: "",
        traineeName = doc.getString("traineeName") ?: "",
        registrationNumber = doc.getString("registrationNumber") ?: "",
        tradeId = doc.getString("tradeId") ?: "",
        tradeName = doc.getString("tradeName") ?: "",
        adminId = doc.getString("adminId") ?: "",
        dateString = doc.getString("dateString") ?: "",
        monthString = doc.getString("monthString") ?: "",
        sessionYear = doc.getString("sessionYear") ?: "2025-2026",
        status = doc.getString("status") ?: AttendanceStatus.PRESENT.value,
        markedByRole = doc.getString("markedByRole") ?: UserRole.TRAINEE.value,
        remarks = doc.getString("remarks") ?: "",
        checkInTime = doc.getString("checkInTime") ?: "",
        createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
        updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      )
    }
  }
}

data class LeaveApplication(
  val leaveId: String = "",
  val traineeId: String = "",
  val traineeUid: String = "",
  val traineeName: String = "",
  val registrationNumber: String = "",
  val tradeId: String = "",
  val tradeName: String = "",
  val adminId: String = "",
  val sessionYear: String = "2025-2026",
  val leaveType: String = LeaveType.CL.value,
  val startDate: String = "",
  val endDate: String = "",
  val monthString: String = "",
  val totalDays: Int = 1,
  val halfDaySession: String = HalfDaySession.NONE.value,
  val reason: String = "",
  val status: String = LeaveStatus.PENDING.value,
  val adminRemarks: String = "",
  val reviewedBy: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  val parsedLeaveType: LeaveType
    get() = LeaveType.fromString(leaveType)

  val parsedLeaveStatus: LeaveStatus
    get() = LeaveStatus.fromString(status)

  val parsedHalfDaySession: HalfDaySession
    get() = HalfDaySession.fromString(halfDaySession)

  fun toCreateMap(): Map<String, Any> = mapOf(
    "leaveId" to leaveId,
    "traineeId" to traineeId,
    "traineeUid" to traineeUid,
    "traineeName" to traineeName,
    "registrationNumber" to registrationNumber,
    "tradeId" to tradeId,
    "tradeName" to tradeName,
    "adminId" to adminId,
    "sessionYear" to sessionYear,
    "leaveType" to leaveType,
    "startDate" to startDate,
    "endDate" to endDate,
    "monthString" to monthString,
    "totalDays" to totalDays,
    "halfDaySession" to halfDaySession,
    "reason" to reason,
    "status" to status,
    "adminRemarks" to adminRemarks,
    "reviewedBy" to reviewedBy,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  companion object {
    fun fromSnapshot(doc: DocumentSnapshot): LeaveApplication? {
      if (!doc.exists()) return null
      return LeaveApplication(
        leaveId = doc.getString("leaveId") ?: doc.id,
        traineeId = doc.getString("traineeId") ?: "",
        traineeUid = doc.getString("traineeUid") ?: "",
        traineeName = doc.getString("traineeName") ?: "",
        registrationNumber = doc.getString("registrationNumber") ?: "",
        tradeId = doc.getString("tradeId") ?: "",
        tradeName = doc.getString("tradeName") ?: "",
        adminId = doc.getString("adminId") ?: "",
        sessionYear = doc.getString("sessionYear") ?: "2025-2026",
        leaveType = doc.getString("leaveType") ?: LeaveType.CL.value,
        startDate = doc.getString("startDate") ?: "",
        endDate = doc.getString("endDate") ?: "",
        monthString = doc.getString("monthString") ?: "",
        totalDays = (doc.getLong("totalDays") ?: 1L).toInt(),
        halfDaySession = doc.getString("halfDaySession") ?: HalfDaySession.NONE.value,
        reason = doc.getString("reason") ?: "",
        status = doc.getString("status") ?: LeaveStatus.PENDING.value,
        adminRemarks = doc.getString("adminRemarks") ?: "",
        reviewedBy = doc.getString("reviewedBy") ?: "",
        createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
        updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      )
    }
  }
}

data class TraineeNotification(
  val notificationId: String = "",
  val traineeId: String = "",
  val traineeUid: String = "",
  val traineeName: String = "",
  val tradeId: String = "",
  val adminId: String = "",
  val type: String = NotificationType.GENERAL_NOTICE.value,
  val title: String = "",
  val message: String = "",
  val isRead: Boolean = false,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  val notificationType: NotificationType
    get() = NotificationType.fromString(type)

  fun toCreateMap(): Map<String, Any> = mapOf(
    "notificationId" to notificationId,
    "traineeId" to traineeId,
    "traineeUid" to traineeUid,
    "traineeName" to traineeName,
    "tradeId" to tradeId,
    "adminId" to adminId,
    "type" to type,
    "title" to title,
    "message" to message,
    "isRead" to isRead,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  companion object {
    fun fromSnapshot(doc: DocumentSnapshot): TraineeNotification? {
      if (!doc.exists()) return null
      return TraineeNotification(
        notificationId = doc.getString("notificationId") ?: doc.id,
        traineeId = doc.getString("traineeId") ?: "",
        traineeUid = doc.getString("traineeUid") ?: "",
        traineeName = doc.getString("traineeName") ?: "",
        tradeId = doc.getString("tradeId") ?: "",
        adminId = doc.getString("adminId") ?: "",
        type = doc.getString("type") ?: NotificationType.GENERAL_NOTICE.value,
        title = doc.getString("title") ?: "",
        message = doc.getString("message") ?: "",
        isRead = doc.getBoolean("isRead") ?: false,
        createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
        updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      )
    }
  }
}
