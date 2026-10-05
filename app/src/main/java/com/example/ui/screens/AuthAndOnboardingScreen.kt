package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.LocalItiStore
import com.example.data.model.ITI_RAIGARH_TRADE_TEMPLATES
import com.example.data.model.UserRole
import com.example.ui.components.FeedbackBanner
import com.example.ui.theme.ItiPrimaryBlue
import com.example.ui.theme.ItiPrimaryBlueLight
import com.example.ui.theme.StatusAbsentBg
import com.example.ui.theme.StatusAbsentRed

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit
) {
  var isCreateAccountMode by rememberSaveable { mutableStateOf(false) }
  var emailInput by rememberSaveable { mutableStateOf("") }
  var passwordInput by rememberSaveable { mutableStateOf("") }
  var confirmPasswordInput by rememberSaveable { mutableStateOf("") }
  var showPassword by rememberSaveable { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var authError by remember { mutableStateOf<String?>(null) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 560.dp)
        .verticalScroll(rememberScrollState())
        .padding(bottom = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Hero Banner with Blue Institutional Gradient Overlay
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(215.dp)
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_iti_hero_banner),
          contentDescription = "Government ITI Raigarh Campus Banner",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  ItiPrimaryBlue.copy(alpha = 0.55f),
                  ItiPrimaryBlue.copy(alpha = 0.92f)
                )
              )
            )
        )
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(20.dp)
        ) {
          Surface(
            color = Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(50)
          ) {
            Text(
              text = "GOVT. OF CHHATTISGARH • SKILL DEVELOPMENT",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Government ITI Raigarh",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
          )
          Text(
            text = "Daily Attendance & Leave Management System",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.92f)
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Simple Email ID & Password Login / Create Account Card
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isCreateAccountMode) Icons.Default.PersonAdd else Icons.Default.VerifiedUser,
              contentDescription = "Portal Login",
              tint = ItiPrimaryBlue,
              modifier = Modifier.size(28.dp)
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = if (isCreateAccountMode) "Create Portal Password" else "Official Portal Login",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Enter your Email ID and Password to access the Training Officer (Admin) or Trainee portal.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Mode Switch Tabs: Login vs Create Password
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
              .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              color = if (!isCreateAccountMode) ItiPrimaryBlue else Color.Transparent,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .clickable {
                  isCreateAccountMode = false
                  authError = null
                }
                .testTag("tab_login_mode")
            ) {
              Text(
                text = "Login",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (!isCreateAccountMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp)
              )
            }

            Surface(
              color = if (isCreateAccountMode) ItiPrimaryBlue else Color.Transparent,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .clickable {
                  isCreateAccountMode = true
                  authError = null
                }
                .testTag("tab_create_password_mode")
            ) {
              Text(
                text = "Create Password",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isCreateAccountMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Email ID Field
          OutlinedTextField(
            value = emailInput,
            onValueChange = {
              emailInput = it
              authError = null
            },
            label = { Text("Email ID") },
            placeholder = { Text("example@itiraigarh.edu.in") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Email ID",
                tint = ItiPrimaryBlue
              )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Email,
              imeAction = ImeAction.Next
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("email_input")
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Password Field
          OutlinedTextField(
            value = passwordInput,
            onValueChange = {
              passwordInput = it
              authError = null
            },
            label = { Text(if (isCreateAccountMode) "Create Password" else "Password") },
            placeholder = { Text("Enter password (min 4 chars)") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Password",
                tint = ItiPrimaryBlue
              )
            },
            trailingIcon = {
              IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                  imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (showPassword) "Hide password" else "Show password"
                )
              }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Password,
              imeAction = if (isCreateAccountMode) ImeAction.Next else ImeAction.Done
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("password_input")
          )

          if (isCreateAccountMode) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
              value = confirmPasswordInput,
              onValueChange = {
                confirmPasswordInput = it
                authError = null
              },
              label = { Text("Confirm Password") },
              placeholder = { Text("Re-enter password") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = "Confirm Password",
                  tint = ItiPrimaryBlue
                )
              },
              visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("confirm_password_input")
            )
          }

          if (authError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
              color = StatusAbsentBg,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = authError ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = StatusAbsentRed,
                modifier = Modifier.padding(12.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          Button(
            onClick = {
              isLoading = true
              authError = null
              val result = LocalItiStore.authenticateWithEmailAndPassword(
                email = emailInput,
                password = passwordInput,
                confirmPassword = if (isCreateAccountMode) confirmPasswordInput else null,
                isCreateAccount = isCreateAccountMode
              )
              isLoading = false
              result.onSuccess {
                onAuthSuccess()
              }.onFailure { err ->
                authError = err.message ?: "Authentication failed. Please check your Email ID and Password."
              }
            },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = ItiPrimaryBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("email_sign_in_button")
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
              )
            } else {
              Icon(
                imageVector = if (isCreateAccountMode) Icons.Default.PersonAdd else Icons.Default.Lock,
                contentDescription = "Submit Login",
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = if (isCreateAccountMode) "Create Password & Continue" else "Login with Email & Password",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "Quick Demo Credentials (Tap to Auto-Fill & Login)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            AssistChip(
              onClick = {
                isCreateAccountMode = false
                emailInput = "officer@itiraigarh.edu.in"
                passwordInput = "iti123"
                authError = null
                LocalItiStore.authenticateWithEmailAndPassword(
                  email = emailInput,
                  password = passwordInput
                ).onSuccess { onAuthSuccess() }
              },
              label = {
                Text(
                  text = "Admin: officer@itiraigarh.edu.in",
                  style = MaterialTheme.typography.labelSmall
                )
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.AdminPanelSettings,
                  contentDescription = "Demo Admin",
                  tint = ItiPrimaryBlue,
                  modifier = Modifier.size(16.dp)
                )
              },
              modifier = Modifier
                .weight(1f)
                .testTag("demo_admin_login_chip")
            )

            AssistChip(
              onClick = {
                isCreateAccountMode = false
                emailInput = "trainee@itiraigarh.edu.in"
                passwordInput = "iti123"
                authError = null
                LocalItiStore.authenticateWithEmailAndPassword(
                  email = emailInput,
                  password = passwordInput
                ).onSuccess { onAuthSuccess() }
              },
              label = {
                Text(
                  text = "Trainee: trainee@itiraigarh.edu.in",
                  style = MaterialTheme.typography.labelSmall
                )
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.School,
                  contentDescription = "Demo Trainee",
                  tint = ItiPrimaryBlueLight,
                  modifier = Modifier.size(16.dp)
                )
              },
              modifier = Modifier
                .weight(1f)
                .testTag("demo_trainee_login_chip")
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Role Preview & Policy Highlights Card
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Government ITI Raigarh Attendance Norms",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = ItiPrimaryBlue
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "• Enter any Email ID & Password to register as a Training Officer or Trainee.\n" +
              "• One attendance record per trainee per day (duplicate prevention).\n" +
              "• Casual Leave (CL): Max 12 days • Medical Leave: Max 36 days • Half-Day Leave.\n" +
              "• Strict Trade-wise data isolation for Training Officers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingRoleSetupScreen(
  defaultDisplayName: String,
  isBusy: Boolean,
  statusMessage: String?,
  errorMessage: String?,
  onDismissBanner: () -> Unit,
  onCompleteAdminSetup: (
    fullName: String,
    employeeCode: String,
    tradeId: String,
    tradeName: String,
    tradeCode: String,
    sessionYear: String,
    seedSampleData: Boolean
  ) -> Unit,
  onCompleteTraineeSetup: (
    fullName: String,
    registrationNumber: String,
    fatherName: String,
    batchShift: String,
    tradeId: String,
    tradeName: String,
    sessionYear: String
  ) -> Unit,
  onSignOut: () -> Unit
) {
  var selectedRole by rememberSaveable { mutableStateOf(UserRole.ADMIN) }
  var selectedTradeIndex by rememberSaveable { mutableStateOf(0) }
  val selectedTemplate = ITI_RAIGARH_TRADE_TEMPLATES.getOrElse(selectedTradeIndex) {
    ITI_RAIGARH_TRADE_TEMPLATES.first()
  }

  var fullName by rememberSaveable {
    mutableStateOf(defaultDisplayName.ifBlank { "R. K. Sharma" })
  }
  var idNumber by rememberSaveable {
    mutableStateOf("TO-RGH-2025-01")
  }
  var fatherName by rememberSaveable {
    mutableStateOf("Mohan Lal Sahu")
  }
  var batchShift by rememberSaveable {
    mutableStateOf("Shift I (Morning)")
  }
  var sessionYear by rememberSaveable {
    mutableStateOf("2025-2026")
  }
  var seedSampleData by rememberSaveable {
    mutableStateOf(true)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 600.dp)
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      Card(
        colors = CardDefaults.cardColors(containerColor = ItiPrimaryBlue),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Government ITI Raigarh, C.G.",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "Select your Role and Assigned Trade to initialize your portal",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.9f)
            )
          }
          OutlinedButton(
            onClick = onSignOut,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
          ) {
            Text("Sign Out")
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      FeedbackBanner(
        statusMessage = statusMessage,
        errorMessage = errorMessage,
        onDismiss = onDismissBanner
      )

      // Step 1: Select User Role
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "1. Select Portal Role",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "You can also switch between Training Officer and Trainee view later from the top bar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            RoleOptionCard(
              title = "Admin – Training Officer",
              subtitle = "Manage assigned Trade, trainees, leave approvals, warnings & reports",
              icon = Icons.Default.AdminPanelSettings,
              selected = selectedRole == UserRole.ADMIN,
              onClick = {
                selectedRole = UserRole.ADMIN
                if (idNumber.startsWith("RGH-")) idNumber = "TO-RGH-2025-01"
              },
              modifier = Modifier
                .weight(1f)
                .testTag("select_role_admin")
            )

            RoleOptionCard(
              title = "Trainee",
              subtitle = "Mark daily attendance, apply for CL/Medical/Half-Day leave & view %",
              icon = Icons.Default.School,
              selected = selectedRole == UserRole.TRAINEE,
              onClick = {
                selectedRole = UserRole.TRAINEE
                if (idNumber.startsWith("TO-")) idNumber = "RGH-EL-25-001"
              },
              modifier = Modifier
                .weight(1f)
                .testTag("select_role_trainee")
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Step 2: Select Assigned Trade
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Engineering,
              contentDescription = "Select Trade",
              tint = ItiPrimaryBlue
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "2. Select Assigned ITI Trade",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Training Officers can access only trainees and records belonging to their assigned Trade.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            ITI_RAIGARH_TRADE_TEMPLATES.forEachIndexed { idx, trade ->
              FilterChip(
                selected = selectedTradeIndex == idx,
                onClick = { selectedTradeIndex = idx },
                label = {
                  Text("${trade.tradeName} (${trade.durationLabel})")
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Step 3: Profile & Session Details
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Badge,
              contentDescription = "Profile Details",
              tint = ItiPrimaryBlue
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "3. ${if (selectedRole == UserRole.ADMIN) "Training Officer" else "Trainee"} Details",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("onboarding_name_input")
          )
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedTextField(
              value = idNumber,
              onValueChange = { idNumber = it },
              label = {
                Text(
                  if (selectedRole == UserRole.ADMIN) "Officer / Employee ID"
                  else "NCVT Roll / Reg. No."
                )
              },
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("onboarding_reg_input")
            )

            OutlinedTextField(
              value = sessionYear,
              onValueChange = { sessionYear = it },
              label = { Text("Academic Session") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          if (selectedRole == UserRole.TRAINEE) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = fatherName,
                onValueChange = { fatherName = it },
                label = { Text("Father / Guardian Name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = batchShift,
                onValueChange = { batchShift = it },
                label = { Text("Shift / Unit") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
            }
          } else {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { seedSampleData = !seedSampleData }
            ) {
              Checkbox(
                checked = seedSampleData,
                onCheckedChange = { seedSampleData = it }
              )
              Text(
                text = "Pre-load sample ITI Raigarh trainees, daily attendance & leave requests for ${selectedTemplate.tradeName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = {
              if (selectedRole == UserRole.ADMIN) {
                onCompleteAdminSetup(
                  fullName,
                  idNumber,
                  selectedTemplate.tradeId,
                  selectedTemplate.tradeName,
                  selectedTemplate.tradeCode,
                  sessionYear,
                  seedSampleData
                )
              } else {
                onCompleteTraineeSetup(
                  fullName,
                  idNumber,
                  fatherName,
                  batchShift,
                  selectedTemplate.tradeId,
                  selectedTemplate.tradeName,
                  sessionYear
                )
              }
            },
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = ItiPrimaryBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("complete_onboarding_button")
          ) {
            if (isBusy) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
              )
            } else {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Complete Setup",
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (selectedRole == UserRole.ADMIN) {
                  "Launch Training Officer Portal (${selectedTemplate.tradeName})"
                } else {
                  "Launch Trainee Portal (${selectedTemplate.tradeName})"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RoleOptionCard(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val borderColor = if (selected) ItiPrimaryBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
  val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
  else MaterialTheme.colorScheme.surface

  Card(
    modifier = modifier.clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = bgColor),
    border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = if (selected) ItiPrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(26.dp)
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
