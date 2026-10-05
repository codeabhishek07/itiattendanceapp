package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceRecord
import com.example.data.model.LeaveApplication
import com.example.data.model.TraineeRecord
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReportType(val id: String, val title: String, val subtitle: String) {
  DAILY_ATTENDANCE(
    "DAILY_ATTENDANCE",
    "Daily Attendance Report",
    "Date-wise attendance register with check-in status and remarks"
  ),
  MONTHLY_ATTENDANCE(
    "MONTHLY_ATTENDANCE",
    "Monthly Attendance Summary",
    "Month-wise attendance log and present/absent breakdown"
  ),
  TRAINEE_WISE_ATTENDANCE(
    "TRAINEE_WISE_ATTENDANCE",
    "Trainee-Wise Attendance Report",
    "Complete attendance percentage and session summary per trainee"
  ),
  TRADE_WISE_ATTENDANCE(
    "TRADE_WISE_ATTENDANCE",
    "Trade-Wise Consolidated Report",
    "Overall trade strength, average attendance %, and session metrics"
  ),
  LEAVE_APPLICATIONS(
    "LEAVE_APPLICATIONS",
    "Leave Applications Register",
    "Approved, rejected, and pending leave applications with reasons"
  ),
  LEAVE_UTILIZATION(
    "LEAVE_UTILIZATION",
    "Leave Utilization & Balance Report",
    "Trainee-wise CL (Max 12), Half-Day, and Medical (Max 36) balances"
  ),
  LOW_ATTENDANCE_LIST(
    "LOW_ATTENDANCE_LIST",
    "Low Attendance Defaulters List",
    "Trainees below minimum required attendance threshold with warning counts"
  )
}

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
  PDF("pdf", "application/pdf", "PDF Document (.pdf)"),
  EXCEL_CSV("csv", "text/csv", "Excel Spreadsheet (.csv)")
}

data class GeneratedReportData(
  val reportType: ReportType,
  val tradeName: String,
  val sessionYear: String,
  val filterSummary: String,
  val generatedAt: String,
  val headers: List<String>,
  val rows: List<List<String>>,
  val summaryMetrics: List<Pair<String, String>>
)

data class ExportedReportResult(
  val fileName: String,
  val savedLocationDescription: String,
  val cacheFile: File,
  val format: ExportFormat,
  val previewData: GeneratedReportData
)

object ReportExporter {

  fun buildReportData(
    reportType: ReportType,
    tradeName: String,
    sessionYear: String,
    selectedDate: String,
    selectedMonth: String,
    selectedTraineeId: String?,
    minAttendanceThreshold: Int,
    trainees: List<TraineeRecord>,
    attendanceList: List<AttendanceRecord>,
    leavesList: List<LeaveApplication>
  ): GeneratedReportData {
    val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
    val filteredTrainees = if (selectedTraineeId.isNullOrBlank() || selectedTraineeId == "ALL") {
      trainees
    } else {
      trainees.filter { it.traineeId == selectedTraineeId }
    }

    val traineeFilterLabel = if (selectedTraineeId.isNullOrBlank() || selectedTraineeId == "ALL") {
      "All Trainees (${filteredTrainees.size})"
    } else {
      filteredTrainees.firstOrNull()?.let { "${it.fullName} (${it.registrationNumber})" } ?: "Selected Trainee"
    }

    return when (reportType) {
      ReportType.DAILY_ATTENDANCE -> {
        val dayRecords = attendanceList.filter {
          it.dateString == selectedDate &&
            (selectedTraineeId.isNullOrBlank() || selectedTraineeId == "ALL" || it.traineeId == selectedTraineeId)
        }
        val presentCount = dayRecords.count { it.status == "PRESENT" }
        val absentCount = dayRecords.count { it.status == "ABSENT" }
        val leaveCount = dayRecords.count { it.status == "ON_LEAVE" || it.status == "HALF_DAY" }

        GeneratedReportData(
          reportType = reportType,
          tradeName = tradeName,
          sessionYear = sessionYear,
          filterSummary = "Date: $selectedDate | Trainee: $traineeFilterLabel",
          generatedAt = nowStr,
          headers = listOf("Reg. No.", "Trainee Name", "Date", "Status", "Time", "Marked By", "Remarks"),
          rows = if (dayRecords.isNotEmpty()) {
            dayRecords.map {
              listOf(
                it.registrationNumber,
                it.traineeName,
                it.dateString,
                it.attendanceStatus.label,
                it.checkInTime,
                it.markedByRole,
                it.remarks.ifEmpty { "-" }
              )
            }
          } else {
            filteredTrainees.map { t ->
              listOf(t.registrationNumber, t.fullName, selectedDate, "Not Marked", "-", "-", "-")
            }
          },
          summaryMetrics = listOf(
            "Selected Date" to selectedDate,
            "Present" to "$presentCount",
            "Absent" to "$absentCount",
            "On Leave / Half-Day" to "$leaveCount"
          )
        )
      }

      ReportType.MONTHLY_ATTENDANCE -> {
        val monthRecords = attendanceList.filter {
          it.monthString == selectedMonth &&
            (selectedTraineeId.isNullOrBlank() || selectedTraineeId == "ALL" || it.traineeId == selectedTraineeId)
        }
        val presentCount = monthRecords.count { it.status == "PRESENT" }
        val absentCount = monthRecords.count { it.status == "ABSENT" }
        val halfOrLeave = monthRecords.count { it.status == "HALF_DAY" || it.status == "ON_LEAVE" }

        GeneratedReportData(
          reportType = reportType,
          tradeName = tradeName,
          sessionYear = sessionYear,
          filterSummary = "Month: $selectedMonth | Trainee: $traineeFilterLabel",
          generatedAt = nowStr,
          headers = listOf("Date", "Reg. No.", "Trainee Name", "Status", "Time", "Remarks"),
          rows = monthRecords.map {
            listOf(
              it.dateString,
              it.registrationNumber,
              it.traineeName,
              it.attendanceStatus.label,
              it.checkInTime,
              it.remarks.ifEmpty { "-" }
            )
          },
          summaryMetrics = listOf(
            "Month" to selectedMonth,
            "Total Entries" to "${monthRecords.size}",
            "Present Entries" to "$presentCount",
            "Absent / Leave" to "$absentCount / $halfOrLeave"
          )
        )
      }

      ReportType.TRAINEE_WISE_ATTENDANCE -> {
        val avgAttendance = if (filteredTrainees.isNotEmpty()) {
          filteredTrainees.map { it.attendancePercentage }.average().toInt()
        } else 100
        GeneratedReportData(
          reportType = reportType,
          tradeName = tradeName,
          sessionYear = sessionYear,
          filterSummary = "Trainee Filter: $traineeFilterLabel",
          generatedAt = nowStr,
          headers = listOf("Reg. No.", "Trainee Name", "Shift", "Present", "Absent", "Half-Day", "Leave", "Attendance %"),
          rows = filteredTrainees.map {
            listOf(
              it.registrationNumber,
              it.fullName,
              it.batchShift,
              "${it.presentDays}",
              "${it.absentDays}",
              "${it.halfDays}",
              "${it.approvedLeaveDays}",
              "${it.attendancePercentage}%"
            )
          },
          summaryMetrics = listOf(
            "Trainees Included" to "${filteredTrainees.size}",
            "Average Attendance" to "$avgAttendance%",
            "Active Trainees" to "${filteredTrainees.count { it.isActive }}",
            "Below $minAttendanceThreshold%" to "${filteredTrainees.count { it.isLowAttendance(minAttendanceThreshold) }}"
          )
        )
      }

      ReportType.TRADE_WISE_ATTENDANCE -> {
        val activeCount = trainees.count { it.isActive }
        val avgPct = if (trainees.isNotEmpty()) trainees.map { it.attendancePercentage }.average().toInt() else 100
        val totalPresentLogs = trainees.sumOf { it.presentDays }
        val totalAbsentLogs = trainees.sumOf { it.absentDays }
        val totalLeaveLogs = trainees.sumOf { it.approvedLeaveDays }

        GeneratedReportData(
          reportType = reportType,
          tradeName = tradeName,
          sessionYear = sessionYear,
          filterSummary = "Trade: $tradeName | Session: $sessionYear",
          generatedAt = nowStr,
          headers = listOf("Trade", "Session", "Reg. No.", "Trainee Name", "Status", "Present", "Absent", "Att. %"),
          rows = trainees.map {
            listOf(
              it.tradeName,
              it.sessionYear,
              it.registrationNumber,
              it.fullName,
              if (it.isActive) "Active" else "Inactive",
              "${it.presentDays}",
              "${it.absentDays}",
              "${it.attendancePercentage}%"
            )
          },
          summaryMetrics = listOf(
            "Trade Name" to tradeName,
            "Active / Total" to "$activeCount / ${trainees.size}",
            "Trade Avg Attendance" to "$avgPct%",
            "Present / Absent / Leave" to "$totalPresentLogs / $totalAbsentLogs / $totalLeaveLogs"
          )
        )
      }

      ReportType.LEAVE_APPLICATIONS -> {
        val filteredLeaves = leavesList.filter {
          (selectedMonth.isBlank() || it.monthString == selectedMonth) &&
            (selectedTraineeId.isNullOrBlank() || selectedTraineeId == "ALL" || it.traineeId == selectedTraineeId)
        }
        GeneratedReportData(
          reportType = reportType,
          tradeName = tradeName,
          sessionYear = sessionYear,
          filterSummary = "Month: $selectedMonth | Trainee: $traineeFilterLabel",
          generatedAt = nowStr,
          headers = listOf("Reg. No.", "Trainee Name", "Leave Type", "From", "To", "Days", "Status", "Reason"),
          rows = filteredLeaves.map {
            listOf(
              it.registrationNumber,
              it.traineeName,
              it.parsedLeaveType.label,
              it.startDate,
              it.endDate,
              if (it.parsedLeaveType.value == "HALF_DAY") "0.5 (${it.parsedHalfDaySession.label})" else "${it.totalDays}",
              it.parsedLeaveStatus.label,
              it.reason
            )
          },
          summaryMetrics = listOf(
            "Total Applications" to "${filteredLeaves.size}",
            "Approved" to "${filteredLeaves.count { it.status == "APPROVED" }}",
            "Pending" to "${filteredLeaves.count { it.status == "PENDING" }}",
            "Rejected" to "${filteredLeaves.count { it.status == "REJECTED" }}"
          )
        )
      }

      ReportType.LEAVE_UTILIZATION -> {
        GeneratedReportData(
          reportType = reportType,
          tradeName = tradeName,
          sessionYear = sessionYear,
          filterSummary = "Trainee: $traineeFilterLabel | Quotas: CL=12, Medical=36",
          generatedAt = nowStr,
          headers = listOf(
            "Reg. No.",
            "Trainee Name",
            "CL Used / 12",
            "CL Remaining",
            "Half-Day Used",
            "Medical Used / 36",
            "Medical Rem."
          ),
          rows = filteredTrainees.map {
            listOf(
              it.registrationNumber,
              it.fullName,
              "${it.effectiveCasualLeaveUsed} / 12",
              "${it.remainingCasualLeave}",
              "${it.halfDayLeaveUsed}",
              "${it.medicalLeaveUsed} / 36",
              "${it.remainingMedicalLeave}"
            )
          },
          summaryMetrics = listOf(
            "Max Casual Leave" to "12 Days",
            "Max Medical Leave" to "36 Days",
            "Total CL Used" to "${filteredTrainees.sumOf { it.casualLeaveUsed }} Days",
            "Total Medical Used" to "${filteredTrainees.sumOf { it.medicalLeaveUsed }} Days"
          )
        )
      }

      ReportType.LOW_ATTENDANCE_LIST -> {
        val defaulters = filteredTrainees.filter { it.isLowAttendance(minAttendanceThreshold) }
        GeneratedReportData(
          reportType = reportType,
          tradeName = tradeName,
          sessionYear = sessionYear,
          filterSummary = "Threshold: Below $minAttendanceThreshold% | Trade: $tradeName",
          generatedAt = nowStr,
          headers = listOf("Reg. No.", "Trainee Name", "Present", "Absent", "Attendance %", "Warnings", "Last Warning"),
          rows = defaulters.map {
            listOf(
              it.registrationNumber,
              it.fullName,
              "${it.presentDays}",
              "${it.absentDays}",
              "${it.attendancePercentage}%",
              "${it.lowAttendanceWarningCount}",
              it.lastWarningMessage.ifEmpty { "None issued" }
            )
          },
          summaryMetrics = listOf(
            "Required Threshold" to "$minAttendanceThreshold%",
            "Defaulters Found" to "${defaulters.size}",
            "Total Trainees" to "${filteredTrainees.size}",
            "Warnings Issued" to "${defaulters.sumOf { it.lowAttendanceWarningCount }}"
          )
        )
      }
    }
  }

  fun exportReport(
    context: Context,
    reportData: GeneratedReportData,
    format: ExportFormat
  ): Result<ExportedReportResult> = runCatching {
    val timestampSlug = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val safeTrade = reportData.tradeName.replace(Regex("[^a-zA-Z0-9]"), "_").take(15)
    val fileName = "ITI_Raigarh_${reportData.reportType.id}_${safeTrade}_$timestampSlug.${format.extension}"

    val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
    val cacheFile = File(reportsDir, fileName)

    when (format) {
      ExportFormat.PDF -> {
        FileOutputStream(cacheFile).use { out ->
          writePdfStream(reportData, out)
        }
      }
      ExportFormat.EXCEL_CSV -> {
        FileOutputStream(cacheFile).use { out ->
          writeCsvStream(reportData, out)
        }
      }
    }

    // Also save to MediaStore.Downloads on Android 10+ (API 29+) for zero-permission Downloads access
    var savedLocation = "App Reports Cache (${cacheFile.name})"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
          put(MediaStore.Downloads.DISPLAY_NAME, fileName)
          put(MediaStore.Downloads.MIME_TYPE, format.mimeType)
          put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ITI_Raigarh_Reports")
        }
        val uri = resolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)
        if (uri != null) {
          resolver.openOutputStream(uri)?.use { out ->
            cacheFile.inputStream().use { input -> input.copyTo(out) }
          }
          savedLocation = "Downloads/ITI_Raigarh_Reports/$fileName"
        }
      } catch (_: Exception) {
        // Fallback to cache file if MediaStore is unavailable in headless/test environment
      }
    }

    ExportedReportResult(
      fileName = fileName,
      savedLocationDescription = savedLocation,
      cacheFile = cacheFile,
      format = format,
      previewData = reportData
    )
  }

  fun shareExportedFile(context: Context, exported: ExportedReportResult) {
    try {
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        exported.cacheFile
      )
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = exported.format.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(
          Intent.EXTRA_SUBJECT,
          "Government ITI Raigarh - ${exported.previewData.reportType.title}"
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(
        Intent.createChooser(shareIntent, "Share / Open ${exported.previewData.reportType.title}")
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    } catch (_: Exception) {
      // Safe no-op if no external handler is installed
    }
  }

  private fun writeCsvStream(data: GeneratedReportData, out: OutputStream) {
    val sb = StringBuilder()
    // UTF-8 BOM for Excel compatibility
    sb.append("\uFEFF")
    sb.appendLine("\"GOVERNMENT INDUSTRIAL TRAINING INSTITUTE (ITI) RAIGARH, CHHATTISGARH\"")
    sb.appendLine("\"${data.reportType.title.uppercase()}\"")
    sb.appendLine("\"Trade: ${data.tradeName}\",\"Academic Session: ${data.sessionYear}\",\"Generated: ${data.generatedAt}\"")
    sb.appendLine("\"${data.filterSummary}\"")
    sb.appendLine()

    // Summary Metrics Row
    sb.appendLine(data.summaryMetrics.joinToString(",") { "\"${it.first}: ${it.second}\"" })
    sb.appendLine()

    // Table Headers
    sb.appendLine(data.headers.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" })

    // Table Rows
    for (row in data.rows) {
      sb.appendLine(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" })
    }

    out.write(sb.toString().toByteArray(Charsets.UTF_8))
    out.flush()
  }

  private fun writePdfStream(data: GeneratedReportData, out: OutputStream) {
    val pdfDocument = PdfDocument()
    val pageWidth = 842 // A4 Landscape width in points
    val pageHeight = 595 // A4 Landscape height in points
    val rowsPerPage = 16
    val chunks = if (data.rows.isEmpty()) listOf(emptyList()) else data.rows.chunked(rowsPerPage)

    val headerBgPaint = Paint().apply {
      color = Color.rgb(13, 71, 161) // #0D47A1
      style = Paint.Style.FILL
    }
    val subHeaderBgPaint = Paint().apply {
      color = Color.rgb(227, 242, 253) // #E3F2FD
      style = Paint.Style.FILL
    }
    val tableHeaderPaint = Paint().apply {
      color = Color.rgb(25, 118, 210)
      style = Paint.Style.FILL
    }
    val zebraPaint = Paint().apply {
      color = Color.rgb(248, 250, 252)
      style = Paint.Style.FILL
    }
    val borderPaint = Paint().apply {
      color = Color.rgb(203, 213, 225)
      style = Paint.Style.STROKE
      strokeWidth = 1f
    }
    val whiteTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      textSize = 15f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val whiteSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(225, 245, 254)
      textSize = 10f
    }
    val darkBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(15, 23, 42)
      textSize = 10f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(30, 41, 59)
      textSize = 9f
    }
    val tableHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      textSize = 9.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    chunks.forEachIndexed { pageIndex, pageRows ->
      val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
      val page = pdfDocument.startPage(pageInfo)
      val canvas: Canvas = page.canvas

      // 1. Blue Institutional Banner
      canvas.drawRect(24f, 20f, (pageWidth - 24).toFloat(), 72f, headerBgPaint)
      canvas.drawText(
        "GOVERNMENT INDUSTRIAL TRAINING INSTITUTE (ITI) RAIGARH, CHHATTISGARH",
        36f,
        42f,
        whiteTitlePaint
      )
      canvas.drawText(
        "${data.reportType.title}  |  Trade: ${data.tradeName}  |  Session: ${data.sessionYear}  |  Generated: ${data.generatedAt}",
        36f,
        60f,
        whiteSubtitlePaint
      )

      // 2. Summary Strip
      canvas.drawRect(24f, 78f, (pageWidth - 24).toFloat(), 110f, subHeaderBgPaint)
      val metricsText = data.summaryMetrics.joinToString("    •    ") { "${it.first}: ${it.second}" }
      canvas.drawText(metricsText, 36f, 94f, darkBoldPaint)
      canvas.drawText(data.filterSummary, 36f, 106f, cellTextPaint)

      // 3. Table Header
      val tableLeft = 24f
      val tableRight = (pageWidth - 24).toFloat()
      val tableWidth = tableRight - tableLeft
      val colCount = data.headers.size.coerceAtLeast(1)
      val colWidth = tableWidth / colCount

      var currentY = 120f
      canvas.drawRect(tableLeft, currentY, tableRight, currentY + 24f, tableHeaderPaint)
      data.headers.forEachIndexed { colIndex, header ->
        val x = tableLeft + colIndex * colWidth + 6f
        canvas.drawText(header.take(22), x, currentY + 16f, tableHeaderTextPaint)
      }
      currentY += 24f

      // 4. Table Rows
      if (pageRows.isEmpty()) {
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + 28f, borderPaint)
        canvas.drawText("No matching records found for the selected filter criteria.", tableLeft + 12f, currentY + 18f, cellTextPaint)
      } else {
        pageRows.forEachIndexed { rowIdx, row ->
          val rowHeight = 22f
          if (rowIdx % 2 == 1) {
            canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, zebraPaint)
          }
          canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, borderPaint)
          row.forEachIndexed { colIndex, cellValue ->
            val x = tableLeft + colIndex * colWidth + 6f
            canvas.drawText(cellValue.take(26), x, currentY + 15f, cellTextPaint)
          }
          currentY += rowHeight
        }
      }

      // 5. Footer Signature & Page Number
      val footerY = (pageHeight - 20).toFloat()
      canvas.drawText(
        "Official Digital Record — Government ITI Raigarh Attendance & Leave Management System",
        24f,
        footerY,
        cellTextPaint
      )
      canvas.drawText(
        "Page ${pageIndex + 1} of ${chunks.size}   |   Training Officer Signature",
        (pageWidth - 260).toFloat(),
        footerY,
        darkBoldPaint
      )

      pdfDocument.finishPage(page)
    }

    pdfDocument.writeTo(out)
    pdfDocument.close()
    out.flush()
  }
}
