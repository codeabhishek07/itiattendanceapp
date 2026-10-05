package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceStatus
import com.example.data.model.LeaveStatus
import com.example.ui.theme.ItiPrimaryBlue
import com.example.ui.theme.ItiSecondaryBlue
import com.example.ui.theme.StatusAbsentBg
import com.example.ui.theme.StatusAbsentRed
import com.example.ui.theme.StatusLeaveBg
import com.example.ui.theme.StatusLeaveBlue
import com.example.ui.theme.StatusPresentBg
import com.example.ui.theme.StatusPresentGreen
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.theme.StatusWarningBg
import com.example.util.ExportedReportResult
import com.example.util.GeneratedReportData

@Composable
fun StatMetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  containerColor: Color = MaterialTheme.colorScheme.surface,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun AttendanceStatusChip(status: AttendanceStatus, modifier: Modifier = Modifier) {
  val (bg, fg) = when (status) {
    AttendanceStatus.PRESENT -> StatusPresentBg to StatusPresentGreen
    AttendanceStatus.ABSENT -> StatusAbsentBg to StatusAbsentRed
    AttendanceStatus.HALF_DAY -> StatusWarningBg to StatusWarningAmber
    AttendanceStatus.ON_LEAVE -> StatusLeaveBg to StatusLeaveBlue
  }
  Surface(
    color = bg,
    shape = RoundedCornerShape(50),
    modifier = modifier
  ) {
    Text(
      text = status.label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = fg,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    )
  }
}

@Composable
fun LeaveStatusChip(status: LeaveStatus, modifier: Modifier = Modifier) {
  val (bg, fg) = when (status) {
    LeaveStatus.APPROVED -> StatusPresentBg to StatusPresentGreen
    LeaveStatus.REJECTED -> StatusAbsentBg to StatusAbsentRed
    LeaveStatus.PENDING -> StatusWarningBg to StatusWarningAmber
  }
  Surface(
    color = bg,
    shape = RoundedCornerShape(50),
    modifier = modifier
  ) {
    Text(
      text = status.label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = fg,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    )
  }
}

@Composable
fun FeedbackBanner(
  statusMessage: String?,
  errorMessage: String?,
  onDismiss: () -> Unit
) {
  if (statusMessage != null) {
    Card(
      colors = CardDefaults.cardColors(containerColor = StatusPresentBg),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, StatusPresentGreen.copy(alpha = 0.4f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .testTag("status_feedback_banner")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Success",
          tint = StatusPresentGreen,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = statusMessage,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = StatusPresentGreen,
          modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Dismiss notification",
            tint = StatusPresentGreen,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }

  if (errorMessage != null) {
    Card(
      colors = CardDefaults.cardColors(containerColor = StatusAbsentBg),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, StatusAbsentRed.copy(alpha = 0.4f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .testTag("error_feedback_banner")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.ErrorOutline,
          contentDescription = "Error",
          tint = StatusAbsentRed,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = errorMessage,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = StatusAbsentRed,
          modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Dismiss error",
            tint = StatusAbsentRed,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
fun AttendanceCircularGauge(
  percentage: Int,
  thresholdPercent: Int = 75,
  size: Dp = 96.dp
) {
  val gaugeColor = when {
    percentage >= thresholdPercent -> StatusPresentGreen
    percentage >= thresholdPercent - 10 -> StatusWarningAmber
    else -> StatusAbsentRed
  }
  val trackColor = MaterialTheme.colorScheme.surfaceVariant

  Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
    Canvas(modifier = Modifier.size(size)) {
      val strokeWidth = 10.dp.toPx()
      val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
      val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

      drawArc(
        color = trackColor,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )
      drawArc(
        color = gaugeColor,
        startAngle = -90f,
        sweepAngle = (percentage.coerceIn(0, 100) / 100f) * 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "$percentage%",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = gaugeColor
      )
      Text(
        text = "Min $thresholdPercent%",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
fun SimpleHorizontalBarChart(
  title: String,
  subtitle: String,
  bars: List<Triple<String, Int, Color>>,
  maxReference: Int,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(14.dp))

      val safeMax = maxReference.coerceAtLeast(1)
      bars.forEach { (label, count, color) ->
        val fraction = (count.toFloat() / safeMax.toFloat()).coerceIn(0f, 1f)
        Column(modifier = Modifier.padding(vertical = 5.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "$count",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = color
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Canvas(
            modifier = Modifier
              .fillMaxWidth()
              .height(10.dp)
          ) {
            drawRoundRect(
              color = color.copy(alpha = 0.15f),
              size = Size(this.size.width, this.size.height),
              cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            drawRoundRect(
              color = color,
              size = Size(this.size.width * fraction, this.size.height),
              cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
          }
        }
      }
    }
  }
}

@Composable
fun ReportPreviewTableCard(
  reportData: GeneratedReportData,
  exportedResult: ExportedReportResult?,
  onDownloadPdf: () -> Unit,
  onDownloadCsv: () -> Unit,
  onShareExported: (ExportedReportResult) -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = reportData.reportType.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = ItiPrimaryBlue
      )
      Text(
        text = "Govt. ITI Raigarh • Trade: ${reportData.tradeName} (${reportData.sessionYear})",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = reportData.filterSummary,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Summary Pills
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        reportData.summaryMetrics.forEach { (k, v) ->
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp)
          ) {
            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
              Text(
                text = "$k: ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = v,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Download Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = onDownloadPdf,
          colors = ButtonDefaults.buttonColors(containerColor = ItiPrimaryBlue),
          modifier = Modifier
            .weight(1f)
            .testTag("download_pdf_report_button")
        ) {
          Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Download PDF",
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Download PDF")
        }

        OutlinedButton(
          onClick = onDownloadCsv,
          border = BorderStroke(1.dp, ItiSecondaryBlue),
          modifier = Modifier
            .weight(1f)
            .testTag("download_excel_report_button")
        ) {
          Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Download Excel CSV",
            tint = ItiSecondaryBlue,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Download Excel", color = ItiSecondaryBlue)
        }
      }

      if (exportedResult != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          color = StatusPresentBg,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Report Ready: ${exportedResult.fileName}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = StatusPresentGreen
              )
              Text(
                text = exportedResult.savedLocationDescription,
                style = MaterialTheme.typography.bodySmall,
                color = StatusPresentGreen
              )
            }
            OutlinedButton(
              onClick = { onShareExported(exportedResult) },
              modifier = Modifier.testTag("share_exported_report_button")
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share or Open Report",
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Open / Share")
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(8.dp))

      // Scrollable Data Table Preview
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
            .padding(vertical = 8.dp, horizontal = 8.dp)
        ) {
          reportData.headers.forEach { header ->
            Text(
              text = header,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier
                .width(120.dp)
                .padding(end = 8.dp)
            )
          }
        }

        if (reportData.rows.isEmpty()) {
          Text(
            text = "No records match the current filter selection.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
          )
        } else {
          reportData.rows.take(25).forEachIndexed { idx, row ->
            Row(
              modifier = Modifier
                .background(
                  if (idx % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                  else Color.Transparent
                )
                .padding(vertical = 7.dp, horizontal = 8.dp)
            ) {
              row.forEach { cell ->
                Text(
                  text = cell,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier
                    .width(120.dp)
                    .padding(end = 8.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}
