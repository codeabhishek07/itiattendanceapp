package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.HalfDaySession
import com.example.data.model.LeaveApplication
import com.example.data.model.LeaveStatus
import com.example.data.model.LeaveType
import com.example.data.model.NotificationType
import com.example.data.model.TraineeNotification
import com.example.data.model.TraineeRecord
import com.example.data.model.UserProfile
import com.example.ui.components.AttendanceCircularGauge
import com.example.ui.components.AttendanceStatusChip
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.LeaveStatusChip
import com.example.ui.components.StatMetricCard
import com.example.ui.theme.ItiPrimaryBlue
import com.example.ui.theme.ItiSecondaryBlue
import com.example.ui.theme.StatusAbsentBg
import com.example.ui.theme.StatusAbsentRed
import com.example.ui.theme.StatusLeaveBlue
import com.example.ui.theme.StatusPresentBg
import com.example.ui.theme.StatusPresentGreen
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.theme.StatusWarningBg
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraineePortalScreen(
  userProfile: UserProfile,
  traineeRecord: TraineeRecord?,
  attendanceList: List<AttendanceRecord>,
  leavesList: List<LeaveApplication>,
  notificationsList: List<TraineeNotification>,
  selectedMonth: String,
  isBusy: Boolean,
  statusMessage: String?,
  errorMessage: String?,
  onDismissBanner: () -> Unit,
  onSelectMonth: (String) -> Unit,
  onMarkTodayAttendance: (TraineeRecord, AttendanceStatus, String) -> Unit,
  onResetTodayAttendance: (TraineeRecord) -> Unit = {},
  onSubmitLeave: (TraineeRecord, LeaveType, String, String, Int, HalfDaySession, String) -> Unit,
  onMarkNotificationRead: (String) -> Unit,
  onSwitchToAdminMode: () -> Unit,
  onSignOut: () -> Unit
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
  val unreadCount = notificationsList.count { !it.isRead }

  if (selectedTab != 0) {
    BackHandler { selectedTab = 0 }
  }

  val activeTrainee = traineeRecord ?: TraineeRecord(
    traineeId = userProfile.userId,
    traineeUid = userProfile.userId,
    adminId = userProfile.userId,
    tradeId = userProfile.tradeId,
    tradeName = userProfile.tradeName,
    sessionYear = userProfile.sessionYear,
    fullName = userProfile.fullName,
    registrationNumber = userProfile.registrationNumber,
    fatherName = "Guardian",
    batchShift = "Shift I (Morning)",
    isActive = userProfile.isActive
  )

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = ItiPrimaryBlue,
          titleContentColor = Color.White,
          actionIconContentColor = Color.White
        ),
        title = {
          Column {
            Text(
              text = "ITI Raigarh • Trainee Portal",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "${activeTrainee.fullName} • ${activeTrainee.tradeName} (${activeTrainee.sessionYear})",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.88f)
            )
          }
        },
        actions = {
          IconButton(
            onClick = onSwitchToAdminMode,
            modifier = Modifier.testTag("switch_role_button")
          ) {
            Icon(
              imageVector = Icons.Default.SwapHoriz,
              contentDescription = "Switch to Training Officer Mode"
            )
          }
          IconButton(
            onClick = onSignOut,
            modifier = Modifier.testTag("sign_out_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Sign Out"
            )
          }
        }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
      ) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
          label = { Text("Dashboard") },
          modifier = Modifier.testTag("trainee_tab_dashboard")
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Attendance") },
          label = { Text("Attendance") },
          modifier = Modifier.testTag("trainee_tab_attendance")
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = "Leaves") },
          label = { Text("Leaves") },
          modifier = Modifier.testTag("trainee_tab_leaves")
        )
        NavigationBarItem(
          selected = selectedTab == 3,
          onClick = { selectedTab = 3 },
          icon = {
            BadgedBox(
              badge = {
                if (unreadCount > 0) {
                  Badge { Text("$unreadCount") }
                }
              }
            ) {
              Icon(Icons.Default.Notifications, contentDescription = "Updates")
            }
          },
          label = { Text("Updates") },
          modifier = Modifier.testTag("trainee_tab_notifications")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background),
      contentAlignment = Alignment.TopCenter
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 640.dp)
      ) {
        FeedbackBanner(
          statusMessage = statusMessage,
          errorMessage = errorMessage,
          onDismiss = onDismissBanner
        )

        when (selectedTab) {
          0 -> TraineeDashboardTab(
            trainee = activeTrainee,
            todayDate = todayDate,
            attendanceList = attendanceList,
            leavesList = leavesList,
            isBusy = isBusy,
            onMarkTodayAttendance = onMarkTodayAttendance,
            onResetTodayAttendance = onResetTodayAttendance,
            onNavigateToLeaves = { selectedTab = 2 },
            onNavigateToAttendance = { selectedTab = 1 }
          )

          1 -> TraineeAttendanceTab(
            trainee = activeTrainee,
            todayDate = todayDate,
            attendanceList = attendanceList,
            selectedMonth = selectedMonth,
            isBusy = isBusy,
            onSelectMonth = onSelectMonth,
            onMarkTodayAttendance = onMarkTodayAttendance,
            onResetTodayAttendance = onResetTodayAttendance
          )

          2 -> TraineeLeavesTab(
            trainee = activeTrainee,
            todayDate = todayDate,
            leavesList = leavesList,
            isBusy = isBusy,
            onSubmitLeave = onSubmitLeave
          )

          3 -> TraineeNotificationsTab(
            trainee = activeTrainee,
            notifications = notificationsList,
            onMarkRead = onMarkNotificationRead
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollegeArrivalCheckInCard(
  trainee: TraineeRecord,
  todayDate: String,
  todayAttendance: AttendanceRecord?,
  isBusy: Boolean,
  onMarkTodayAttendance: (TraineeRecord, AttendanceStatus, String) -> Unit,
  onResetTodayAttendance: (TraineeRecord) -> Unit
) {
  var selectedLocation by rememberSaveable { mutableStateOf("Reached College • Trade Workshop") }
  var remarksInput by rememberSaveable { mutableStateOf("") }
  val isMarkedPresentOrHalf = todayAttendance != null && todayAttendance.attendanceStatus != AttendanceStatus.ABSENT

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(
      width = if (isMarkedPresentOrHalf) 1.dp else 1.5.dp,
      color = if (isMarkedPresentOrHalf) StatusPresentGreen.copy(alpha = 0.55f) else StatusAbsentRed.copy(alpha = 0.65f)
    )
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Mark Attendance When Reaching College",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Date: $todayDate • ${trainee.batchShift}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        if (isMarkedPresentOrHalf && todayAttendance != null) {
          AttendanceStatusChip(status = todayAttendance.attendanceStatus)
        } else {
          Surface(
            color = StatusAbsentBg,
            shape = RoundedCornerShape(50)
          ) {
            Text(
              text = "Not Marked • Auto-Absent",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = StatusAbsentRed,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (isMarkedPresentOrHalf && todayAttendance != null) {
        Surface(
          color = StatusPresentBg,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Marked",
              tint = StatusPresentGreen
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "College Attendance Marked (${todayAttendance.attendanceStatus.label} at ${todayAttendance.checkInTime})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = StatusPresentGreen
              )
              Text(
                text = "${todayAttendance.remarks.ifBlank { "Marked at College" }} • Removed from today's Auto-Absent count.",
                style = MaterialTheme.typography.bodySmall,
                color = StatusPresentGreen
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
          onClick = { onResetTodayAttendance(trainee) },
          enabled = !isBusy,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("reset_today_attendance_button")
        ) {
          Text(
            text = "Reset Today to Unmarked (Test Auto-Absent Rule)",
            style = MaterialTheme.typography.labelMedium
          )
        }
      } else {
        // Prominent Auto-Absent Warning Banner
        Surface(
          color = StatusAbsentBg,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.WarningAmber,
              contentDescription = "Auto Absent Rule",
              tint = StatusAbsentRed,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Currently Counted in ABSENT (Not Marked Yet)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = StatusAbsentRed
              )
              Text(
                text = "Mark your attendance as soon as you reach Govt. ITI Raigarh. If not marked, today is automatically counted as Absent.",
                style = MaterialTheme.typography.bodySmall,
                color = StatusAbsentRed
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Select College Arrival Location:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(
            "Reached College • Trade Workshop",
            "Reached College • Theory Class",
            "Reached College • Practical Lab"
          ).forEach { loc ->
            FilterChip(
              selected = selectedLocation == loc,
              onClick = { selectedLocation = loc },
              label = { Text(loc, style = MaterialTheme.typography.labelSmall) }
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = remarksInput,
          onValueChange = { remarksInput = it },
          label = { Text("Optional Note (e.g., Morning Shift Arrival)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              val combinedRemarks = if (remarksInput.isBlank()) {
                selectedLocation
              } else {
                "$selectedLocation (${remarksInput.trim()})"
              }
              onMarkTodayAttendance(
                trainee,
                AttendanceStatus.PRESENT,
                combinedRemarks
              )
            },
            enabled = !isBusy && trainee.isActive,
            colors = ButtonDefaults.buttonColors(containerColor = ItiPrimaryBlue),
            modifier = Modifier
              .weight(1.3f)
              .height(48.dp)
              .testTag("mark_present_button")
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Mark Present", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reached College • Mark Present", fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = {
              val combinedRemarks = if (remarksInput.isBlank()) {
                "$selectedLocation (Half-Day)"
              } else {
                "$selectedLocation (${remarksInput.trim()})"
              }
              onMarkTodayAttendance(
                trainee,
                AttendanceStatus.HALF_DAY,
                combinedRemarks
              )
            },
            enabled = !isBusy && trainee.isActive,
            border = BorderStroke(1.dp, ItiSecondaryBlue),
            modifier = Modifier
              .weight(0.7f)
              .height(48.dp)
              .testTag("mark_half_day_button")
          ) {
            Icon(Icons.Default.AccessTime, contentDescription = "Mark Half-Day", tint = ItiSecondaryBlue, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Half-Day", color = ItiSecondaryBlue)
          }
        }
      }
    }
  }
}

@Composable
private fun TraineeDashboardTab(
  trainee: TraineeRecord,
  todayDate: String,
  attendanceList: List<AttendanceRecord>,
  leavesList: List<LeaveApplication>,
  isBusy: Boolean,
  onMarkTodayAttendance: (TraineeRecord, AttendanceStatus, String) -> Unit,
  onResetTodayAttendance: (TraineeRecord) -> Unit,
  onNavigateToLeaves: () -> Unit,
  onNavigateToAttendance: () -> Unit
) {
  val todayAttendance = attendanceList.firstOrNull { it.dateString == todayDate }
  val isTodayMarkedPresentOrHalf = todayAttendance != null && todayAttendance.attendanceStatus != AttendanceStatus.ABSENT
  val effectiveAbsentCount = trainee.effectiveAbsentDays(isTodayMarkedPresentOrHalf)
  val effectiveAttendancePct = trainee.effectiveAttendancePercentage(isTodayMarkedPresentOrHalf)
  val pendingLeaves = leavesList.filter { it.status == LeaveStatus.PENDING.value }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    // Low Attendance Warning Alert Banner if applicable
    if (trainee.lowAttendanceWarningCount > 0 || trainee.isLowAttendance(75)) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = StatusWarningBg),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, StatusWarningAmber.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.WarningAmber,
              contentDescription = "Low Attendance Warning",
              tint = StatusWarningAmber,
              modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Low Attendance Alert (${trainee.attendancePercentage}%)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = StatusWarningAmber
              )
              Text(
                text = trainee.lastWarningMessage.ifEmpty {
                  "Minimum 75% attendance is required for NCVT examination eligibility at Govt. ITI Raigarh."
                },
                style = MaterialTheme.typography.bodySmall,
                color = StatusWarningAmber
              )
            }
          }
        }
      }
    }

    // 1. Today's Attendance Marking Card (Mark on College Arrival or Auto-Counted in Absent)
    item {
      CollegeArrivalCheckInCard(
        trainee = trainee,
        todayDate = todayDate,
        todayAttendance = todayAttendance,
        isBusy = isBusy,
        onMarkTodayAttendance = onMarkTodayAttendance,
        onResetTodayAttendance = onResetTodayAttendance
      )
    }

    // 2. Attendance % & Present/Absent Summary Card (Reflects Auto-Absent if today is unmarked)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AttendanceCircularGauge(
              percentage = effectiveAttendancePct,
              thresholdPercent = 75
            )
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Overall Attendance Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Reg. No: ${trainee.registrationNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                MiniCounterItem("Present", "${trainee.presentDays}", StatusPresentGreen)
                MiniCounterItem("Absent", "$effectiveAbsentCount", StatusAbsentRed)
                MiniCounterItem("Half-Day", "${trainee.halfDays}", StatusWarningAmber)
                MiniCounterItem("Leave", "${trainee.approvedLeaveDays}", StatusLeaveBlue)
              }
            }
          }
          if (!isTodayMarkedPresentOrHalf) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              color = StatusAbsentBg,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "* Today ($todayDate) is automatically counted in Absent ($effectiveAbsentCount total absent) until you mark your attendance upon reaching college.",
                style = MaterialTheme.typography.labelSmall,
                color = StatusAbsentRed,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
        }
      }
    }

    // 3. Leave Balance Card (CL Max 12, Half-Day, Medical Max 36)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Leave Balance (${trainee.sessionYear})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = onNavigateToLeaves) {
              Text("Apply Leave")
            }
          }
          Spacer(modifier = Modifier.height(10.dp))

          LeaveQuotaProgressRow(
            title = "Casual Leave (CL)",
            usedLabel = "${trainee.effectiveCasualLeaveUsed} / 12 Days Used",
            remainingLabel = "${trainee.remainingCasualLeave} Days Left",
            progress = (trainee.effectiveCasualLeaveUsed.toFloat() / 12f).coerceIn(0f, 1f),
            color = ItiPrimaryBlue
          )
          Spacer(modifier = Modifier.height(10.dp))
          LeaveQuotaProgressRow(
            title = "Medical Leave",
            usedLabel = "${trainee.medicalLeaveUsed} / 36 Days Used",
            remainingLabel = "${trainee.remainingMedicalLeave} Days Left",
            progress = (trainee.medicalLeaveUsed.toFloat() / 36f).coerceIn(0f, 1f),
            color = ItiSecondaryBlue
          )
          Spacer(modifier = Modifier.height(10.dp))
          LeaveQuotaProgressRow(
            title = "Half-Day Leave",
            usedLabel = "${trainee.halfDayLeaveUsed} / 12 Sessions Used",
            remainingLabel = "${trainee.remainingHalfDayLeaves} Left",
            progress = (trainee.halfDayLeaveUsed.toFloat() / 12f).coerceIn(0f, 1f),
            color = StatusWarningAmber
          )
        }
      }
    }

    // 4. Pending Leave Requests & Recent History
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Pending Leave Requests (${pendingLeaves.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))
          if (pendingLeaves.isEmpty()) {
            Text(
              text = "No pending leave applications.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          } else {
            pendingLeaves.take(3).forEach { lv ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${lv.parsedLeaveType.label} (${lv.startDate} to ${lv.endDate})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                  )
                  Text(
                    text = lv.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                LeaveStatusChip(status = lv.parsedLeaveStatus)
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider()
          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Recent Attendance History",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = onNavigateToAttendance) {
              Text("View All")
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          if (!isTodayMarkedPresentOrHalf && todayAttendance == null) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "$todayDate (Today)",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = StatusAbsentRed
                )
                Text(
                  text = "AUTO-ABSENT • Not marked by trainee upon reaching college yet",
                  style = MaterialTheme.typography.bodySmall,
                  color = StatusAbsentRed
                )
              }
              AttendanceStatusChip(status = AttendanceStatus.ABSENT)
            }
          }
          attendanceList.take(5).forEach { att ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = att.dateString,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "${att.checkInTime} • ${att.remarks.ifEmpty { "Standard Roll Call" }}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              AttendanceStatusChip(status = att.attendanceStatus)
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(20.dp)) }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TraineeAttendanceTab(
  trainee: TraineeRecord,
  todayDate: String,
  attendanceList: List<AttendanceRecord>,
  selectedMonth: String,
  isBusy: Boolean,
  onSelectMonth: (String) -> Unit,
  onMarkTodayAttendance: (TraineeRecord, AttendanceStatus, String) -> Unit,
  onResetTodayAttendance: (TraineeRecord) -> Unit
) {
  val todayAttendance = attendanceList.firstOrNull { it.dateString == todayDate }
  val isTodayMarkedPresentOrHalf = todayAttendance != null && todayAttendance.attendanceStatus != AttendanceStatus.ABSENT
  val availableMonths = remember(attendanceList, selectedMonth) {
    (attendanceList.map { it.monthString } + listOf(selectedMonth, "2026-10", "2026-09", "2026-08"))
      .filter { it.isNotBlank() }
      .distinct()
      .sortedDescending()
  }

  val monthRecords = attendanceList.filter { it.monthString == selectedMonth }
  val monthPresent = monthRecords.count { it.status == AttendanceStatus.PRESENT.value }
  val rawMonthAbsent = monthRecords.count { it.status == AttendanceStatus.ABSENT.value }
  val monthAbsent = if (selectedMonth == todayDate.take(7) && todayAttendance == null) {
    rawMonthAbsent + 1
  } else {
    rawMonthAbsent
  }
  val monthHalf = monthRecords.count { it.status == AttendanceStatus.HALF_DAY.value }
  val monthLeave = monthRecords.count { it.status == AttendanceStatus.ON_LEAVE.value }
  val monthTotal = monthPresent + monthAbsent + monthHalf + monthLeave
  val monthPercentage = if (monthTotal > 0) {
    (((monthPresent + monthHalf * 0.5) / monthTotal) * 100).toInt()
  } else trainee.effectiveAttendancePercentage(isTodayMarkedPresentOrHalf)

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    item {
      CollegeArrivalCheckInCard(
        trainee = trainee,
        todayDate = todayDate,
        todayAttendance = todayAttendance,
        isBusy = isBusy,
        onMarkTodayAttendance = onMarkTodayAttendance,
        onResetTodayAttendance = onResetTodayAttendance
      )
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Monthly Attendance Register",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Select month to inspect daily attendance and monthly percentage",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            availableMonths.forEach { m ->
              FilterChip(
                selected = m == selectedMonth,
                onClick = { onSelectMonth(m) },
                label = { Text(m) }
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            StatMetricCard(
              title = "Month % ($selectedMonth)",
              value = "$monthPercentage%",
              subtitle = "Overall: ${trainee.effectiveAttendancePercentage(isTodayMarkedPresentOrHalf)}%",
              icon = Icons.Default.CalendarMonth,
              accentColor = ItiPrimaryBlue,
              modifier = Modifier.weight(1f)
            )
            StatMetricCard(
              title = "Present / Absent",
              value = "$monthPresent / $monthAbsent",
              subtitle = "Half: $monthHalf • Leave: $monthLeave",
              icon = Icons.Default.History,
              accentColor = StatusPresentGreen,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    if (selectedMonth == todayDate.take(7) && todayAttendance == null) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = StatusAbsentBg.copy(alpha = 0.55f)),
          border = BorderStroke(1.dp, StatusAbsentRed.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "$todayDate (Today)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = StatusAbsentRed
              )
              Text(
                text = "Status: Auto-Counted in Absent (Not marked at college yet)",
                style = MaterialTheme.typography.bodySmall,
                color = StatusAbsentRed
              )
              Text(
                text = "Tap 'Reached College • Mark Present' above when you reach campus.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            AttendanceStatusChip(status = AttendanceStatus.ABSENT)
          }
        }
      }
    }

    if (monthRecords.isEmpty() && !(selectedMonth == todayDate.take(7) && todayAttendance == null)) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Text(
            text = "No attendance records found for $selectedMonth.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp)
          )
        }
      }
    } else {
      items(monthRecords, key = { it.attendanceId }) { record ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = record.dateString,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Time: ${record.checkInTime} • Marked by: ${record.markedByRole}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              if (record.remarks.isNotBlank()) {
                Text(
                  text = record.remarks,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.secondary
                )
              }
            }
            AttendanceStatusChip(status = record.attendanceStatus)
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(20.dp)) }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TraineeLeavesTab(
  trainee: TraineeRecord,
  todayDate: String,
  leavesList: List<LeaveApplication>,
  isBusy: Boolean,
  onSubmitLeave: (TraineeRecord, LeaveType, String, String, Int, HalfDaySession, String) -> Unit
) {
  var selectedLeaveType by rememberSaveable { mutableStateOf(LeaveType.CL) }
  var startDate by rememberSaveable { mutableStateOf(todayDate) }
  var endDate by rememberSaveable { mutableStateOf(todayDate) }
  var halfDaySession by rememberSaveable { mutableStateOf(HalfDaySession.FIRST_HALF) }
  var reason by rememberSaveable { mutableStateOf("") }
  var statusFilter by rememberSaveable { mutableStateOf("ALL") }

  val calculatedDays = remember(selectedLeaveType, startDate, endDate) {
    if (selectedLeaveType == LeaveType.HALF_DAY) {
      1
    } else {
      calculateInclusiveDays(startDate, endDate)
    }
  }

  val filteredHistory = leavesList.filter {
    statusFilter == "ALL" || it.status == statusFilter
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    // 1. Apply for Leave Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Apply for Leave",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ItiPrimaryBlue
          )
          Text(
            text = "Balances: CL ${trainee.remainingCasualLeave}/12 days • Medical ${trainee.remainingMedicalLeave}/36 days • Half-Day ${trainee.remainingHalfDayLeaves}/12",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(12.dp))

          // Leave Type Selector
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            LeaveType.entries.forEach { type ->
              FilterChip(
                selected = selectedLeaveType == type,
                onClick = {
                  selectedLeaveType = type
                  if (type == LeaveType.HALF_DAY) {
                    endDate = startDate
                  }
                },
                label = { Text(type.label) },
                modifier = Modifier.testTag("leave_type_${type.value.lowercase()}")
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          if (selectedLeaveType == LeaveType.HALF_DAY) {
            OutlinedTextField(
              value = startDate,
              onValueChange = {
                startDate = it
                endDate = it
              },
              label = { Text("Half-Day Date (YYYY-MM-DD)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(
                selected = halfDaySession == HalfDaySession.FIRST_HALF,
                onClick = { halfDaySession = HalfDaySession.FIRST_HALF },
                label = { Text("First Half (Morning)") }
              )
              FilterChip(
                selected = halfDaySession == HalfDaySession.SECOND_HALF,
                onClick = { halfDaySession = HalfDaySession.SECOND_HALF },
                label = { Text("Second Half (Afternoon)") }
              )
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                label = { Text("From Date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = endDate,
                onValueChange = { endDate = it },
                label = { Text("To Date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            // Quick multi-day duration pills
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "Quick Duration:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              listOf(1, 2, 3, 5).forEach { days ->
                FilterChip(
                  selected = calculatedDays == days,
                  onClick = { endDate = addDaysToDate(startDate, days - 1) },
                  label = { Text("${days}d") }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Reason for Leave Application") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("leave_reason_input")
          )

          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = {
              onSubmitLeave(
                trainee,
                selectedLeaveType,
                startDate,
                if (selectedLeaveType == LeaveType.HALF_DAY) startDate else endDate,
                calculatedDays,
                if (selectedLeaveType == LeaveType.HALF_DAY) halfDaySession else HalfDaySession.NONE,
                reason
              )
              reason = ""
            },
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = ItiPrimaryBlue),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("submit_leave_button")
          ) {
            Text(
              if (selectedLeaveType == LeaveType.HALF_DAY) {
                "Submit Half-Day Leave (${halfDaySession.label})"
              } else {
                "Submit ${selectedLeaveType.label} ($calculatedDays Days)"
              }
            )
          }
        }
      }
    }

    // 2. Complete Leave History Filter & List
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Leave Applications History (${filteredHistory.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL" to "All", "PENDING" to "Pending", "APPROVED" to "Approved", "REJECTED" to "Rejected")
              .forEach { (key, label) ->
                FilterChip(
                  selected = statusFilter == key,
                  onClick = { statusFilter = key },
                  label = { Text(label) }
                )
              }
          }
        }
      }
    }

    if (filteredHistory.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Text(
            text = "No leave applications found for this filter.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp)
          )
        }
      }
    } else {
      items(filteredHistory, key = { it.leaveId }) { leave ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = leave.parsedLeaveType.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ItiPrimaryBlue
              )
              LeaveStatusChip(status = leave.parsedLeaveStatus)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (leave.parsedLeaveType == LeaveType.HALF_DAY) {
                "Date: ${leave.startDate} • ${leave.parsedHalfDaySession.label} (0.5 Day)"
              } else {
                "From: ${leave.startDate} to ${leave.endDate} (${leave.totalDays} Days)"
              },
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Reason: ${leave.reason}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (leave.adminRemarks.isNotBlank() || leave.reviewedBy.isNotBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Training Officer (${leave.reviewedBy.ifEmpty { "TO" }}): ${leave.adminRemarks}",
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(8.dp)
                )
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(20.dp)) }
  }
}

@Composable
private fun TraineeNotificationsTab(
  trainee: TraineeRecord,
  notifications: List<TraineeNotification>,
  onMarkRead: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Leave Status Updates & Official Warnings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Warnings Issued: ${trainee.lowAttendanceWarningCount} • Unread Updates: ${notifications.count { !it.isRead }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    if (notifications.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Text(
            text = "No notifications or warnings yet. When your Training Officer approves or rejects a leave application or issues an attendance notice, it will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp)
          )
        }
      }
    } else {
      items(notifications, key = { it.notificationId }) { notif ->
        val isWarning = notif.notificationType == NotificationType.LOW_ATTENDANCE_WARNING
        val cardBg = when {
          isWarning -> StatusWarningBg.copy(alpha = 0.6f)
          !notif.isRead -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
          else -> MaterialTheme.colorScheme.surface
        }

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = cardBg),
          border = BorderStroke(
            1.dp,
            if (isWarning) StatusWarningAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
          )
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = notif.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isWarning) StatusWarningAmber else ItiPrimaryBlue
              )
              if (!notif.isRead) {
                OutlinedButton(
                  onClick = { onMarkRead(notif.notificationId) }
                ) {
                  Text("Mark Read", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = notif.message,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(20.dp)) }
  }
}

@Composable
private fun MiniCounterItem(label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = color
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun LeaveQuotaProgressRow(
  title: String,
  usedLabel: String,
  remainingLabel: String,
  progress: Float,
  color: Color
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = "$usedLabel ($remainingLabel)",
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.Bold
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
      progress = { progress },
      color = color,
      trackColor = color.copy(alpha = 0.15f),
      modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
    )
  }
}

private fun calculateInclusiveDays(startDate: String, endDate: String): Int {
  val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  return try {
    val start = sdf.parse(startDate) ?: return 1
    val end = sdf.parse(endDate) ?: return 1
    val diffMs = end.time - start.time
    val days = (diffMs / (1000L * 60 * 60 * 24)).toInt() + 1
    days.coerceIn(1, 36)
  } catch (e: Exception) {
    1
  }
}

private fun addDaysToDate(startDate: String, addDays: Int): String {
  val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  return try {
    val start = sdf.parse(startDate) ?: return startDate
    val cal = Calendar.getInstance().apply {
      time = start
      add(Calendar.DAY_OF_MONTH, addDays)
    }
    sdf.format(cal.time)
  } catch (e: Exception) {
    startDate
  }
}
