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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.LeaveApplication
import com.example.data.model.LeaveStatus
import com.example.data.model.LeaveType
import com.example.data.model.Trade
import com.example.data.model.TraineeRecord
import com.example.data.model.UserProfile
import com.example.ui.components.AttendanceCircularGauge
import com.example.ui.components.AttendanceStatusChip
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.LeaveStatusChip
import com.example.ui.components.ReportPreviewTableCard
import com.example.ui.components.SimpleHorizontalBarChart
import com.example.ui.components.StatMetricCard
import com.example.ui.theme.ItiPrimaryBlue
import com.example.ui.theme.ItiSecondaryBlue
import com.example.ui.theme.StatusAbsentBg
import com.example.ui.theme.StatusAbsentRed
import com.example.ui.theme.StatusLeaveBlue
import com.example.ui.theme.StatusPresentGreen
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.theme.StatusWarningBg
import com.example.util.ExportFormat
import com.example.util.ExportedReportResult
import com.example.util.GeneratedReportData
import com.example.util.ReportExporter
import com.example.util.ReportType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(
  userProfile: UserProfile,
  trade: Trade?,
  trainees: List<TraineeRecord>,
  attendanceList: List<AttendanceRecord>,
  leavesList: List<LeaveApplication>,
  selectedDate: String,
  selectedMonth: String,
  selectedTraineeFilter: String,
  selectedReportType: ReportType,
  lastExportedReport: ExportedReportResult?,
  isBusy: Boolean,
  statusMessage: String?,
  errorMessage: String?,
  onDismissBanner: () -> Unit,
  onSelectDate: (String) -> Unit,
  onSelectMonth: (String) -> Unit,
  onSelectTraineeFilter: (String) -> Unit,
  onSelectReportType: (ReportType) -> Unit,
  onCreateTrainee: (String, String, String, String) -> Unit,
  onUpdateTrainee: (String, String, String, String, String, Boolean) -> Unit,
  onMarkTraineeAttendance: (TraineeRecord, String, AttendanceStatus, AttendanceRecord?) -> Unit,
  onMarkAllPresentForDate: (List<TraineeRecord>, List<AttendanceRecord>, String) -> Unit,
  onReviewLeave: (LeaveApplication, TraineeRecord?, Boolean, String) -> Unit,
  onIssueWarning: (TraineeRecord, String) -> Unit,
  onUpdateTradeSettings: (String, String, String, Int, Int, Int, String, Int, Int) -> Unit,
  onSeedSampleTrainees: () -> Unit,
  onDownloadReport: (ExportFormat, GeneratedReportData) -> Unit,
  onSwitchToTraineeMode: () -> Unit,
  onSignOut: () -> Unit
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  var showAddTraineeDialog by rememberSaveable { mutableStateOf(false) }
  var editingTrainee by remember { mutableStateOf<TraineeRecord?>(null) }
  var inspectingTrainee by remember { mutableStateOf<TraineeRecord?>(null) }
  var warningTargetTrainee by remember { mutableStateOf<TraineeRecord?>(null) }
  var showTradeConfigDialog by rememberSaveable { mutableStateOf(false) }

  if (selectedTab != 0) {
    BackHandler { selectedTab = 0 }
  }

  val pendingLeavesCount = leavesList.count { it.status == LeaveStatus.PENDING.value }
  val minAttendanceThreshold = trade?.minAttendancePercent ?: 75

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
              text = "ITI Raigarh • Training Officer",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "Assigned Trade: ${userProfile.tradeName} • Session ${trade?.sessionYear ?: userProfile.sessionYear}",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.9f)
            )
          }
        },
        actions = {
          IconButton(
            onClick = { showTradeConfigDialog = true },
            modifier = Modifier.testTag("trade_settings_button")
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Configure Trade & Session Year"
            )
          }
          IconButton(
            onClick = onSwitchToTraineeMode,
            modifier = Modifier.testTag("switch_role_button")
          ) {
            Icon(
              imageVector = Icons.Default.SwapHoriz,
              contentDescription = "Switch to Trainee View"
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
    floatingActionButton = {
      if (selectedTab == 1) {
        FloatingActionButton(
          onClick = { showAddTraineeDialog = true },
          containerColor = ItiPrimaryBlue,
          contentColor = Color.White,
          modifier = Modifier.testTag("add_trainee_fab")
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = "Add New Trainee")
        }
      }
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
          modifier = Modifier.testTag("admin_tab_dashboard")
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = { Icon(Icons.Default.Groups, contentDescription = "Trainees") },
          label = { Text("Trainees") },
          modifier = Modifier.testTag("admin_tab_trainees")
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.Default.FactCheck, contentDescription = "Roll Call") },
          label = { Text("Roll Call") },
          modifier = Modifier.testTag("admin_tab_attendance")
        )
        NavigationBarItem(
          selected = selectedTab == 3,
          onClick = { selectedTab = 3 },
          icon = {
            BadgedBox(
              badge = {
                if (pendingLeavesCount > 0) {
                  Badge { Text("$pendingLeavesCount") }
                }
              }
            ) {
              Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = "Leaves")
            }
          },
          label = { Text("Leaves") },
          modifier = Modifier.testTag("admin_tab_leaves")
        )
        NavigationBarItem(
          selected = selectedTab == 4,
          onClick = { selectedTab = 4 },
          icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Reports") },
          label = { Text("Reports") },
          modifier = Modifier.testTag("admin_tab_reports")
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
          .widthIn(max = 680.dp)
      ) {
        FeedbackBanner(
          statusMessage = statusMessage,
          errorMessage = errorMessage,
          onDismiss = onDismissBanner
        )

        when (selectedTab) {
          0 -> AdminDashboardTab(
            userProfile = userProfile,
            trade = trade,
            trainees = trainees,
            attendanceList = attendanceList,
            leavesList = leavesList,
            selectedDate = selectedDate,
            minAttendanceThreshold = minAttendanceThreshold,
            onIssueWarningClick = { warningTargetTrainee = it },
            onNavigateToLeaves = { selectedTab = 3 },
            onNavigateToRollCall = { selectedTab = 2 },
            onNavigateToReports = { selectedTab = 4 },
            onOpenTradeConfig = { showTradeConfigDialog = true },
            onSeedSampleTrainees = onSeedSampleTrainees
          )

          1 -> AdminTraineesTab(
            userProfile = userProfile,
            trainees = trainees,
            minAttendanceThreshold = minAttendanceThreshold,
            onAddTraineeClick = { showAddTraineeDialog = true },
            onEditTraineeClick = { editingTrainee = it },
            onInspectTraineeClick = { inspectingTrainee = it },
            onIssueWarningClick = { warningTargetTrainee = it }
          )

          2 -> AdminAttendanceRollCallTab(
            trainees = trainees,
            attendanceList = attendanceList,
            selectedDate = selectedDate,
            selectedMonth = selectedMonth,
            onSelectDate = onSelectDate,
            onSelectMonth = onSelectMonth,
            onMarkTraineeAttendance = onMarkTraineeAttendance,
            onMarkAllPresent = onMarkAllPresentForDate
          )

          3 -> AdminLeavesReviewTab(
            trainees = trainees,
            leavesList = leavesList,
            isBusy = isBusy,
            onReviewLeave = onReviewLeave
          )

          4 -> AdminReportsTab(
            userProfile = userProfile,
            trade = trade,
            trainees = trainees,
            attendanceList = attendanceList,
            leavesList = leavesList,
            selectedDate = selectedDate,
            selectedMonth = selectedMonth,
            selectedTraineeFilter = selectedTraineeFilter,
            selectedReportType = selectedReportType,
            minAttendanceThreshold = minAttendanceThreshold,
            lastExportedReport = lastExportedReport,
            onSelectDate = onSelectDate,
            onSelectMonth = onSelectMonth,
            onSelectTraineeFilter = onSelectTraineeFilter,
            onSelectReportType = onSelectReportType,
            onDownloadReport = onDownloadReport
          )
        }
      }
    }
  }

  // --- DIALOGS ---

  if (showAddTraineeDialog) {
    AddOrEditTraineeDialog(
      existing = null,
      tradeName = userProfile.tradeName,
      defaultSession = trade?.sessionYear ?: userProfile.sessionYear,
      onDismiss = { showAddTraineeDialog = false },
      onSaveNew = { name, regNo, father, shift ->
        onCreateTrainee(name, regNo, father, shift)
        showAddTraineeDialog = false
      },
      onSaveUpdate = { _, _, _, _, _, _, _ -> }
    )
  }

  editingTrainee?.let { target ->
    AddOrEditTraineeDialog(
      existing = target,
      tradeName = userProfile.tradeName,
      defaultSession = target.sessionYear,
      onDismiss = { editingTrainee = null },
      onSaveNew = { _, _, _, _ -> },
      onSaveUpdate = { id, name, regNo, father, shift, session, active ->
        onUpdateTrainee(id, name, regNo, father, shift, active)
        editingTrainee = null
      }
    )
  }

  inspectingTrainee?.let { target ->
    IndividualTraineeInspectorDialog(
      trainee = target,
      attendanceList = attendanceList.filter { it.traineeId == target.traineeId },
      leavesList = leavesList.filter { it.traineeId == target.traineeId },
      onIssueWarning = {
        inspectingTrainee = null
        warningTargetTrainee = target
      },
      onDismiss = { inspectingTrainee = null }
    )
  }

  warningTargetTrainee?.let { target ->
    IssueWarningDialog(
      trainee = target,
      minAttendanceThreshold = minAttendanceThreshold,
      onDismiss = { warningTargetTrainee = null },
      onConfirmSend = { msg ->
        onIssueWarning(target, msg)
        warningTargetTrainee = null
      }
    )
  }

  if (showTradeConfigDialog) {
    TradeAndSessionConfigDialog(
      userProfile = userProfile,
      trade = trade,
      onDismiss = { showTradeConfigDialog = false },
      onSave = { tName, tCode, sYear, workDays, maxCl, maxMed, hdRule, maxHd, minPct ->
        onUpdateTradeSettings(tName, tCode, sYear, workDays, maxCl, maxMed, hdRule, maxHd, minPct)
        showTradeConfigDialog = false
      }
    )
  }
}

@Composable
private fun AdminDashboardTab(
  userProfile: UserProfile,
  trade: Trade?,
  trainees: List<TraineeRecord>,
  attendanceList: List<AttendanceRecord>,
  leavesList: List<LeaveApplication>,
  selectedDate: String,
  minAttendanceThreshold: Int,
  onIssueWarningClick: (TraineeRecord) -> Unit,
  onNavigateToLeaves: () -> Unit,
  onNavigateToRollCall: () -> Unit,
  onNavigateToReports: () -> Unit,
  onOpenTradeConfig: () -> Unit,
  onSeedSampleTrainees: () -> Unit
) {
  val activeTrainees = trainees.filter { it.isActive }
  val todayRecords = attendanceList.filter { it.dateString == selectedDate }
  val presentToday = todayRecords.count { it.status == AttendanceStatus.PRESENT.value }
  val unmarkedToday = (activeTrainees.size - todayRecords.size).coerceAtLeast(0)
  val absentToday = todayRecords.count { it.status == AttendanceStatus.ABSENT.value } + unmarkedToday
  val halfOrLeaveToday = todayRecords.count {
    it.status == AttendanceStatus.HALF_DAY.value || it.status == AttendanceStatus.ON_LEAVE.value
  }
  val avgAttendancePct = if (activeTrainees.isNotEmpty()) {
    activeTrainees.map { it.attendancePercentage }.average().toInt()
  } else 100
  val pendingLeaves = leavesList.filter { it.status == LeaveStatus.PENDING.value }
  val lowAttendanceTrainees = activeTrainees.filter { it.isLowAttendance(minAttendanceThreshold) }

  val totalClUsed = trainees.sumOf { it.casualLeaveUsed }
  val totalHalfUsed = trainees.sumOf { it.halfDayLeaveUsed }
  val totalMedicalUsed = trainees.sumOf { it.medicalLeaveUsed }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    // Trade Isolation Header Banner
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, ItiPrimaryBlue.copy(alpha = 0.3f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Engineering,
                contentDescription = "Assigned Trade",
                tint = ItiPrimaryBlue,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Trade: ${userProfile.tradeName} (${trade?.tradeCode ?: "NCVT"})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Training Officer: ${userProfile.fullName} • Session: ${trade?.sessionYear ?: userProfile.sessionYear} • Trade-Isolated Access",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
          }
          OutlinedButton(onClick = onOpenTradeConfig) {
            Text("Session Config")
          }
        }
      }
    }

    if (trainees.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, ItiPrimaryBlue.copy(alpha = 0.35f))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "No Trainees Enrolled in ${userProfile.tradeName} Yet",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Add trainees manually in the Trainees tab or load a sample ITI Raigarh batch to explore statistics, leave approvals, and reports.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = onSeedSampleTrainees,
              colors = ButtonDefaults.buttonColors(containerColor = ItiPrimaryBlue),
              modifier = Modifier.testTag("seed_sample_roster_button")
            ) {
              Text("Load Sample ${userProfile.tradeName} Batch")
            }
          }
        }
      }
    }

    // 1. Admin KPI Statistics Grid (6 core metrics requested by user)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StatMetricCard(
            title = "Total Trainees",
            value = "${trainees.size}",
            subtitle = "Active: ${activeTrainees.size} in ${userProfile.tradeName}",
            icon = Icons.Default.Groups,
            accentColor = ItiPrimaryBlue,
            modifier = Modifier.weight(1f)
          )
          StatMetricCard(
            title = "Present Today",
            value = "$presentToday",
            subtitle = "Date: $selectedDate",
            icon = Icons.Default.CheckCircle,
            accentColor = StatusPresentGreen,
            modifier = Modifier.weight(1f)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StatMetricCard(
            title = "Absent Today",
            value = "$absentToday",
            subtitle = "Auto-Absent: $unmarkedToday • Leave/Half: $halfOrLeaveToday",
            icon = Icons.Default.PersonOff,
            accentColor = StatusAbsentRed,
            modifier = Modifier.weight(1f)
          )
          StatMetricCard(
            title = "Avg Attendance %",
            value = "$avgAttendancePct%",
            subtitle = "Min Required: $minAttendanceThreshold%",
            icon = Icons.Default.Analytics,
            accentColor = ItiSecondaryBlue,
            modifier = Modifier.weight(1f)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StatMetricCard(
            title = "Pending Leaves",
            value = "${pendingLeaves.size}",
            subtitle = "Total Requests: ${leavesList.size}",
            icon = Icons.Default.PendingActions,
            accentColor = StatusWarningAmber,
            modifier = Modifier.weight(1f)
          )
          StatMetricCard(
            title = "Low Attendance",
            value = "${lowAttendanceTrainees.size}",
            subtitle = "Below $minAttendanceThreshold% threshold",
            icon = Icons.Default.WarningAmber,
            accentColor = StatusAbsentRed,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // Quick Action Row
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onNavigateToRollCall,
          colors = ButtonDefaults.buttonColors(containerColor = ItiPrimaryBlue),
          modifier = Modifier.weight(1f)
        ) {
          Text("Daily Roll Call")
        }
        OutlinedButton(
          onClick = onNavigateToLeaves,
          modifier = Modifier.weight(1f)
        ) {
          Text("Approve Leaves (${pendingLeaves.size})")
        }
        OutlinedButton(
          onClick = onNavigateToReports,
          modifier = Modifier.weight(1f)
        ) {
          Text("PDF/Excel")
        }
      }
    }

    // 2. Simple Attendance & Leave Graphs
    item {
      SimpleHorizontalBarChart(
        title = "Today's Attendance Graph ($selectedDate)",
        subtitle = "Distribution across ${activeTrainees.size} active trainees in ${userProfile.tradeName}",
        bars = listOf(
          Triple("Present Today", presentToday, StatusPresentGreen),
          Triple("Absent Today", absentToday, StatusAbsentRed),
          Triple("On Leave / Half-Day", halfOrLeaveToday, StatusLeaveBlue),
          Triple(
            "Unmarked",
            (activeTrainees.size - todayRecords.size).coerceAtLeast(0),
            StatusWarningAmber
          )
        ),
        maxReference = activeTrainees.size.coerceAtLeast(1)
      )
    }

    item {
      SimpleHorizontalBarChart(
        title = "Trade Leave Utilization Graph",
        subtitle = "Total leave days utilized across ${userProfile.tradeName} trainees",
        bars = listOf(
          Triple("Casual Leave (CL - Max 12/trainee)", totalClUsed, ItiPrimaryBlue),
          Triple("Half-Day Leave Sessions", totalHalfUsed, StatusWarningAmber),
          Triple("Medical Leave (Max 36/trainee)", totalMedicalUsed, ItiSecondaryBlue)
        ),
        maxReference = (totalClUsed + totalHalfUsed + totalMedicalUsed).coerceAtLeast(12)
      )
    }

    // 3. Low Attendance Trainees & One-Tap Warning Issuer
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Low Attendance Defaulters",
                tint = StatusAbsentRed
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Low Attendance Trainees (<$minAttendanceThreshold%)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
            Text(
              text = "${lowAttendanceTrainees.size} Trainees",
              style = MaterialTheme.typography.labelMedium,
              color = StatusAbsentRed,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(8.dp))

          if (lowAttendanceTrainees.isEmpty()) {
            Text(
              text = "All active trainees in ${userProfile.tradeName} currently meet the $minAttendanceThreshold% attendance requirement.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          } else {
            lowAttendanceTrainees.forEach { t ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${t.fullName} (${t.registrationNumber})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                  )
                  Text(
                    text = "Attendance: ${t.attendancePercentage}% • Present: ${t.presentDays} • Absent: ${t.absentDays} • Warnings: ${t.lowAttendanceWarningCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusAbsentRed
                  )
                }
                OutlinedButton(
                  onClick = { onIssueWarningClick(t) },
                  border = BorderStroke(1.dp, StatusAbsentRed),
                  modifier = Modifier.testTag("issue_warning_button_${t.traineeId}")
                ) {
                  Text("Issue Warning", color = StatusAbsentRed, style = MaterialTheme.typography.labelSmall)
                }
              }
              HorizontalDivider()
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
private fun AdminTraineesTab(
  userProfile: UserProfile,
  trainees: List<TraineeRecord>,
  minAttendanceThreshold: Int,
  onAddTraineeClick: () -> Unit,
  onEditTraineeClick: (TraineeRecord) -> Unit,
  onInspectTraineeClick: (TraineeRecord) -> Unit,
  onIssueWarningClick: (TraineeRecord) -> Unit
) {
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var filterMode by rememberSaveable { mutableStateOf("ALL") }

  val filteredTrainees = trainees.filter { t ->
    val matchesSearch = searchQuery.isBlank() ||
      t.fullName.contains(searchQuery, ignoreCase = true) ||
      t.registrationNumber.contains(searchQuery, ignoreCase = true)
    val matchesMode = when (filterMode) {
      "ACTIVE" -> t.isActive
      "INACTIVE" -> !t.isActive
      "LOW" -> t.isLowAttendance(minAttendanceThreshold)
      else -> true
    }
    matchesSearch && matchesMode
  }

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${userProfile.tradeName} Trainees (${filteredTrainees.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Create, edit, deactivate trainee accounts & inspect individual records",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Button(
              onClick = onAddTraineeClick,
              colors = ButtonDefaults.buttonColors(containerColor = ItiPrimaryBlue),
              modifier = Modifier.testTag("add_trainee_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = "Add Trainee", modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("New Trainee")
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by Trainee Name or Registration No.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(8.dp))
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
              "ALL" to "All (${trainees.size})",
              "ACTIVE" to "Active (${trainees.count { it.isActive }})",
              "LOW" to "Low Attendance (<$minAttendanceThreshold%)",
              "INACTIVE" to "Deactivated (${trainees.count { !it.isActive }})"
            ).forEach { (key, label) ->
              FilterChip(
                selected = filterMode == key,
                onClick = { filterMode = key },
                label = { Text(label) }
              )
            }
          }
        }
      }
    }

    items(filteredTrainees, key = { it.traineeId }) { trainee ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("trainee_item_card_${trainee.traineeId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (trainee.isActive) MaterialTheme.colorScheme.surface
          else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
          1.dp,
          if (trainee.isLowAttendance(minAttendanceThreshold)) StatusAbsentRed.copy(alpha = 0.5f)
          else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = trainee.fullName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                  color = if (trainee.isActive) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.errorContainer,
                  shape = RoundedCornerShape(50)
                ) {
                  Text(
                    text = if (trainee.isActive) "Active" else "Deactivated",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (trainee.isActive) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                  )
                }
              }
              Text(
                text = "Reg. No: ${trainee.registrationNumber} • ${trainee.batchShift} • Father: ${trainee.fatherName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Surface(
              color = if (trainee.attendancePercentage >= minAttendanceThreshold) {
                StatusPresentGreen.copy(alpha = 0.14f)
              } else {
                StatusAbsentRed.copy(alpha = 0.14f)
              },
              shape = RoundedCornerShape(10.dp)
            ) {
              Text(
                text = "${trainee.attendancePercentage}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (trainee.attendancePercentage >= minAttendanceThreshold) StatusPresentGreen else StatusAbsentRed,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Present: ${trainee.presentDays}d • Absent: ${trainee.absentDays}d • Half-Day: ${trainee.halfDays} • Leave: ${trainee.approvedLeaveDays}d  |  CL Left: ${trainee.remainingCasualLeave}/12 • Med Left: ${trainee.remainingMedicalLeave}/36",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
          )

          if (trainee.lowAttendanceWarningCount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Warnings Issued: ${trainee.lowAttendanceWarningCount} (${trainee.lastWarningMessage})",
              style = MaterialTheme.typography.labelSmall,
              color = StatusWarningAmber
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { onInspectTraineeClick(trainee) },
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Visibility, contentDescription = "View History", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("History", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
              onClick = { onEditTraineeClick(trainee) },
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Edit Trainee", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (trainee.isActive) "Edit / Deactivate" else "Activate", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
              onClick = { onIssueWarningClick(trainee) },
              border = BorderStroke(1.dp, StatusWarningAmber)
            ) {
              Icon(
                Icons.Default.WarningAmber,
                contentDescription = "Issue Low Attendance Warning",
                tint = StatusWarningAmber,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Warn", color = StatusWarningAmber, style = MaterialTheme.typography.labelMedium)
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(72.dp)) }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminAttendanceRollCallTab(
  trainees: List<TraineeRecord>,
  attendanceList: List<AttendanceRecord>,
  selectedDate: String,
  selectedMonth: String,
  onSelectDate: (String) -> Unit,
  onSelectMonth: (String) -> Unit,
  onMarkTraineeAttendance: (TraineeRecord, String, AttendanceStatus, AttendanceRecord?) -> Unit,
  onMarkAllPresent: (List<TraineeRecord>, List<AttendanceRecord>, String) -> Unit
) {
  var viewMode by rememberSaveable { mutableStateOf("DAILY") }
  val activeTrainees = trainees.filter { it.isActive }
  val dateAttendanceMap = remember(attendanceList, selectedDate) {
    attendanceList.filter { it.dateString == selectedDate }.associateBy { it.traineeId }
  }
  val monthAttendanceList = remember(attendanceList, selectedMonth) {
    attendanceList.filter { it.monthString == selectedMonth }
  }

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
              text = "Daily & Monthly Attendance",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(
                selected = viewMode == "DAILY",
                onClick = { viewMode = "DAILY" },
                label = { Text("Daily Roll Call") }
              )
              FilterChip(
                selected = viewMode == "MONTHLY",
                onClick = { viewMode = "MONTHLY" },
                label = { Text("Monthly Summary") }
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          if (viewMode == "DAILY") {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = selectedDate,
                onValueChange = onSelectDate,
                label = { Text("Roll Call Date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              Button(
                onClick = {
                  onMarkAllPresent(
                    activeTrainees,
                    dateAttendanceMap.values.toList(),
                    selectedDate
                  )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ItiPrimaryBlue),
                modifier = Modifier.testTag("mark_all_present_button")
              ) {
                Text("Mark Rest Present")
              }
            }
          } else {
            OutlinedTextField(
              value = selectedMonth,
              onValueChange = onSelectMonth,
              label = { Text("Select Month (YYYY-MM)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }
    }

    if (viewMode == "DAILY") {
      items(activeTrainees, key = { it.traineeId }) { trainee ->
        val existingRecord = dateAttendanceMap[trainee.traineeId]
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
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = trainee.fullName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "${trainee.registrationNumber} • Overall: ${trainee.attendancePercentage}%",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              if (existingRecord != null) {
                AttendanceStatusChip(status = existingRecord.attendanceStatus)
              } else {
                Surface(
                  color = StatusAbsentBg,
                  shape = RoundedCornerShape(50)
                ) {
                  Text(
                    text = "Not Marked • Auto-Absent",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = StatusAbsentRed,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              AttendanceStatus.entries.forEach { statusOption ->
                val isSelected = existingRecord?.attendanceStatus == statusOption
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    onMarkTraineeAttendance(trainee, selectedDate, statusOption, existingRecord)
                  },
                  label = { Text(statusOption.label) }
                )
              }
            }
          }
        }
      }
    } else {
      // Monthly Attendance Summary per Trainee
      items(trainees, key = { it.traineeId }) { trainee ->
        val tMonthRecords = monthAttendanceList.filter { it.traineeId == trainee.traineeId }
        val pCount = tMonthRecords.count { it.status == AttendanceStatus.PRESENT.value }
        val aCount = tMonthRecords.count { it.status == AttendanceStatus.ABSENT.value }
        val hdCount = tMonthRecords.count { it.status == AttendanceStatus.HALF_DAY.value }
        val lCount = tMonthRecords.count { it.status == AttendanceStatus.ON_LEAVE.value }
        val mTotal = tMonthRecords.size
        val mPct = if (mTotal > 0) (((pCount + hdCount * 0.5) / mTotal) * 100).toInt() else trainee.attendancePercentage

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
                text = "${trainee.fullName} (${trainee.registrationNumber})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Month ($selectedMonth): Present $pCount • Absent $aCount • Half-Day $hdCount • Leave $lCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "Session Totals: P=${trainee.presentDays}, A=${trainee.absentDays}, L=${trainee.approvedLeaveDays}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
              )
            }
            Text(
              text = "$mPct%",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = if (mPct >= 75) StatusPresentGreen else StatusAbsentRed
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(20.dp)) }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminLeavesReviewTab(
  trainees: List<TraineeRecord>,
  leavesList: List<LeaveApplication>,
  isBusy: Boolean,
  onReviewLeave: (LeaveApplication, TraineeRecord?, Boolean, String) -> Unit
) {
  var statusFilter by rememberSaveable { mutableStateOf("PENDING") }
  val traineeMap = remember(trainees) { trainees.associateBy { it.traineeId } }
  val filteredLeaves = leavesList.filter {
    statusFilter == "ALL" || it.status == statusFilter
  }

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Leave Applications & Balances",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Approved leaves automatically update trainee leave balances and record ON_LEAVE / HALF_DAY in attendance.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
              "PENDING" to "Pending (${leavesList.count { it.status == "PENDING" }})",
              "APPROVED" to "Approved (${leavesList.count { it.status == "APPROVED" }})",
              "REJECTED" to "Rejected (${leavesList.count { it.status == "REJECTED" }})",
              "ALL" to "All (${leavesList.size})"
            ).forEach { (key, label) ->
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

    if (filteredLeaves.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Text(
            text = "No leave applications in this category.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp)
          )
        }
      }
    } else {
      items(filteredLeaves, key = { it.leaveId }) { leave ->
        val trainee = traineeMap[leave.traineeId]
        var officerRemarks by rememberSaveable(leave.leaveId) { mutableStateOf(leave.adminRemarks) }

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${leave.traineeName} (${leave.registrationNumber})",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "${leave.parsedLeaveType.label} • ${
                    if (leave.parsedLeaveType == LeaveType.HALF_DAY) {
                      "${leave.startDate} (${leave.parsedHalfDaySession.label})"
                    } else {
                      "${leave.startDate} to ${leave.endDate} (${leave.totalDays} Days)"
                    }
                  }",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                  color = ItiPrimaryBlue
                )
              }
              LeaveStatusChip(status = leave.parsedLeaveStatus)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Reason: ${leave.reason}",
              style = MaterialTheme.typography.bodyMedium
            )

            if (trainee != null) {
              Spacer(modifier = Modifier.height(6.dp))
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Trainee Leave Balance: CL Remaining ${trainee.remainingCasualLeave}/12d • Medical Remaining ${trainee.remainingMedicalLeave}/36d • Attendance: ${trainee.attendancePercentage}%",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(8.dp)
                )
              }
            }

            if (leave.parsedLeaveStatus == LeaveStatus.PENDING) {
              Spacer(modifier = Modifier.height(10.dp))
              OutlinedTextField(
                value = officerRemarks,
                onValueChange = { officerRemarks = it },
                label = { Text("Training Officer Remarks") },
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
                    onReviewLeave(
                      leave,
                      trainee,
                      true,
                      officerRemarks.ifBlank { "Approved as per ITI leave rules" }
                    )
                  },
                  enabled = !isBusy && trainee != null,
                  colors = ButtonDefaults.buttonColors(containerColor = StatusPresentGreen),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("approve_leave_button_${leave.leaveId}")
                ) {
                  Icon(Icons.Default.Check, contentDescription = "Approve Leave", modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Approve")
                }

                OutlinedButton(
                  onClick = {
                    onReviewLeave(
                      leave,
                      trainee,
                      false,
                      officerRemarks.ifBlank { "Rejected due to workshop schedule" }
                    )
                  },
                  enabled = !isBusy && trainee != null,
                  border = BorderStroke(1.dp, StatusAbsentRed),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("reject_leave_button_${leave.leaveId}")
                ) {
                  Icon(Icons.Default.Close, contentDescription = "Reject Leave", tint = StatusAbsentRed, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Reject", color = StatusAbsentRed)
                }
              }
            } else if (leave.adminRemarks.isNotBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Reviewed by ${leave.reviewedBy}: ${leave.adminRemarks}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
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
private fun AdminReportsTab(
  userProfile: UserProfile,
  trade: Trade?,
  trainees: List<TraineeRecord>,
  attendanceList: List<AttendanceRecord>,
  leavesList: List<LeaveApplication>,
  selectedDate: String,
  selectedMonth: String,
  selectedTraineeFilter: String,
  selectedReportType: ReportType,
  minAttendanceThreshold: Int,
  lastExportedReport: ExportedReportResult?,
  onSelectDate: (String) -> Unit,
  onSelectMonth: (String) -> Unit,
  onSelectTraineeFilter: (String) -> Unit,
  onSelectReportType: (ReportType) -> Unit,
  onDownloadReport: (ExportFormat, GeneratedReportData) -> Unit
) {
  val context = LocalContext.current
  val reportData = remember(
    selectedReportType,
    userProfile.tradeName,
    trade?.sessionYear,
    selectedDate,
    selectedMonth,
    selectedTraineeFilter,
    minAttendanceThreshold,
    trainees,
    attendanceList,
    leavesList
  ) {
    ReportExporter.buildReportData(
      reportType = selectedReportType,
      tradeName = userProfile.tradeName,
      sessionYear = trade?.sessionYear ?: userProfile.sessionYear,
      selectedDate = selectedDate,
      selectedMonth = selectedMonth,
      selectedTraineeId = selectedTraineeFilter,
      minAttendanceThreshold = minAttendanceThreshold,
      trainees = trainees,
      attendanceList = attendanceList,
      leavesList = leavesList
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    // 1. Report Category & Filter Controls Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Generate & Download PDF / Excel Reports",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Filter by Report Type, Date, Month, and Trainee for Trade: ${userProfile.tradeName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))

          // 7 Report Types
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            ReportType.entries.forEach { type ->
              FilterChip(
                selected = selectedReportType == type,
                onClick = { onSelectReportType(type) },
                label = { Text(type.title) }
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedTextField(
              value = selectedDate,
              onValueChange = onSelectDate,
              label = { Text("Date Filter (YYYY-MM-DD)") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = selectedMonth,
              onValueChange = onSelectMonth,
              label = { Text("Month Filter (YYYY-MM)") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Filter by Trainee:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = selectedTraineeFilter == "ALL",
              onClick = { onSelectTraineeFilter("ALL") },
              label = { Text("All Trainees (${trainees.size})") }
            )
            trainees.forEach { t ->
              FilterChip(
                selected = selectedTraineeFilter == t.traineeId,
                onClick = { onSelectTraineeFilter(t.traineeId) },
                label = { Text("${t.fullName} (${t.registrationNumber})") }
              )
            }
          }
        }
      }
    }

    // 2. Report Preview & Download Card
    item {
      ReportPreviewTableCard(
        reportData = reportData,
        exportedResult = lastExportedReport,
        onDownloadPdf = { onDownloadReport(ExportFormat.PDF, reportData) },
        onDownloadCsv = { onDownloadReport(ExportFormat.EXCEL_CSV, reportData) },
        onShareExported = { exported -> ReportExporter.shareExportedFile(context, exported) }
      )
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

@Composable
private fun AddOrEditTraineeDialog(
  existing: TraineeRecord?,
  tradeName: String,
  defaultSession: String,
  onDismiss: () -> Unit,
  onSaveNew: (String, String, String, String) -> Unit,
  onSaveUpdate: (String, String, String, String, String, String, Boolean) -> Unit
) {
  var fullName by rememberSaveable { mutableStateOf(existing?.fullName ?: "") }
  var regNo by rememberSaveable {
    mutableStateOf(existing?.registrationNumber ?: "RGH-${tradeName.take(3).uppercase()}-25-")
  }
  var fatherName by rememberSaveable { mutableStateOf(existing?.fatherName ?: "") }
  var shift by rememberSaveable { mutableStateOf(existing?.batchShift ?: "Shift I (Morning)") }
  var sessionYear by rememberSaveable { mutableStateOf(existing?.sessionYear ?: defaultSession) }
  var isActive by rememberSaveable { mutableStateOf(existing?.isActive ?: true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (existing == null) "Add Trainee to $tradeName" else "Edit Trainee Account",
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text("Trainee Full Name") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dialog_trainee_name_input")
        )
        OutlinedTextField(
          value = regNo,
          onValueChange = { regNo = it },
          label = { Text("Registration / Roll Number") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dialog_trainee_reg_input")
        )
        OutlinedTextField(
          value = fatherName,
          onValueChange = { fatherName = it },
          label = { Text("Father / Guardian Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = shift,
          onValueChange = { shift = it },
          label = { Text("Batch / Shift") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        if (existing != null) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
              checked = isActive,
              onCheckedChange = { isActive = it }
            )
            Text("Account Active (Uncheck to deactivate trainee)")
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (existing == null) {
            onSaveNew(fullName, regNo, fatherName, shift)
          } else {
            onSaveUpdate(
              existing.traineeId,
              fullName,
              regNo,
              fatherName,
              shift,
              sessionYear,
              isActive
            )
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = ItiPrimaryBlue),
        modifier = Modifier.testTag("save_trainee_dialog_button")
      ) {
        Text(if (existing == null) "Create Trainee" else "Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
private fun IndividualTraineeInspectorDialog(
  trainee: TraineeRecord,
  attendanceList: List<AttendanceRecord>,
  leavesList: List<LeaveApplication>,
  onIssueWarning: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text(text = trainee.fullName, fontWeight = FontWeight.Bold)
        Text(
          text = "${trainee.registrationNumber} • ${trainee.tradeName} (${trainee.sessionYear})",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          AttendanceCircularGauge(
            percentage = trainee.attendancePercentage,
            size = 76.dp
          )
          Column {
            Text(
              text = "Present: ${trainee.presentDays}d | Absent: ${trainee.absentDays}d",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "Half-Day: ${trainee.halfDays} | Leave: ${trainee.approvedLeaveDays}d",
              style = MaterialTheme.typography.bodySmall
            )
            Text(
              text = "CL Left: ${trainee.remainingCasualLeave}/12d | Med Left: ${trainee.remainingMedicalLeave}/36d",
              style = MaterialTheme.typography.labelSmall,
              color = ItiPrimaryBlue
            )
          }
        }

        HorizontalDivider()
        Text(
          text = "Recent Attendance (${attendanceList.size} entries)",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold
        )
        if (attendanceList.isEmpty()) {
          Text("No attendance records.", style = MaterialTheme.typography.bodySmall)
        } else {
          attendanceList.take(4).forEach { att ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("${att.dateString} (${att.checkInTime})", style = MaterialTheme.typography.bodySmall)
              Text(
                att.attendanceStatus.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        HorizontalDivider()
        Text(
          text = "Leave History (${leavesList.size} requests)",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold
        )
        if (leavesList.isEmpty()) {
          Text("No leave applications.", style = MaterialTheme.typography.bodySmall)
        } else {
          leavesList.take(3).forEach { lv ->
            Text(
              text = "• ${lv.parsedLeaveType.label}: ${lv.startDate} to ${lv.endDate} — ${lv.parsedLeaveStatus.label}",
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    },
    confirmButton = {
      OutlinedButton(
        onClick = onIssueWarning,
        border = BorderStroke(1.dp, StatusWarningAmber)
      ) {
        Text("Issue Low Attendance Warning", color = StatusWarningAmber)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

@Composable
private fun IssueWarningDialog(
  trainee: TraineeRecord,
  minAttendanceThreshold: Int,
  onDismiss: () -> Unit,
  onConfirmSend: (String) -> Unit
) {
  var message by rememberSaveable {
    mutableStateOf(
      "Official Warning: Your attendance is currently ${trainee.attendancePercentage}%, below the mandatory $minAttendanceThreshold% threshold at Govt. ITI Raigarh. Please attend classes regularly."
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Issue Low Attendance Warning", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Trainee: ${trainee.fullName} (${trainee.registrationNumber}) • Current Attendance: ${trainee.attendancePercentage}%",
          style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
          value = message,
          onValueChange = { message = it },
          label = { Text("Warning Notice Text") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirmSend(message) },
        colors = ButtonDefaults.buttonColors(containerColor = StatusAbsentRed),
        modifier = Modifier.testTag("confirm_send_warning_button")
      ) {
        Text("Send Official Warning")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
private fun TradeAndSessionConfigDialog(
  userProfile: UserProfile,
  trade: Trade?,
  onDismiss: () -> Unit,
  onSave: (String, String, String, Int, Int, Int, String, Int, Int) -> Unit
) {
  var tradeName by rememberSaveable { mutableStateOf(trade?.tradeName ?: userProfile.tradeName) }
  var tradeCode by rememberSaveable { mutableStateOf(trade?.tradeCode ?: "ITI-RGH-TRD") }
  var sessionYear by rememberSaveable { mutableStateOf(trade?.sessionYear ?: userProfile.sessionYear) }
  var workingDays by rememberSaveable { mutableStateOf((trade?.totalWorkingDays ?: 240).toString()) }
  var minAttendance by rememberSaveable { mutableStateOf((trade?.minAttendancePercent ?: 75).toString()) }
  var halfDayRule by rememberSaveable { mutableStateOf(trade?.halfDayDeductionRule ?: "DEDUCT_HALF_CL") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Trade & Session Year Configuration", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = tradeName,
          onValueChange = { tradeName = it },
          label = { Text("Assigned Trade Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = tradeCode,
            onValueChange = { tradeCode = it },
            label = { Text("Trade Code") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = sessionYear,
            onValueChange = { sessionYear = it },
            label = { Text("Academic Session") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = workingDays,
            onValueChange = { workingDays = it },
            label = { Text("Working Days") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = minAttendance,
            onValueChange = { minAttendance = it },
            label = { Text("Min Att. %") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }
        Text(
          text = "Leave Rules: Casual Leave (CL) Max = 12 Days • Medical Leave Max = 36 Days • Half-Day Leave = 0.5 CL Deduction",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            tradeName,
            tradeCode,
            sessionYear,
            workingDays.toIntOrNull() ?: 240,
            12,
            36,
            halfDayRule,
            12,
            minAttendance.toIntOrNull() ?: 75
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = ItiPrimaryBlue)
      ) {
        Text("Save Configuration")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
