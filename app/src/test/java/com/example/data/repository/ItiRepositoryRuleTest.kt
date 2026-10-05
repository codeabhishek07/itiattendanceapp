package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AttendanceStatus
import com.example.data.model.HalfDaySession
import com.example.data.model.LeaveType
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ItiRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun registerTrainingOfficer_andCreateTrainee_succeedsForAssignedTrade() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    signInTestUser("officer_elec_$suffix@itiraigarh.edu.in")
    val repo = ItiAttendanceRepository(firestore)

    val adminResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.registerTrainingOfficer(
        fullName = "R. K. Sharma",
        employeeCode = "TO-EL-$suffix",
        tradeId = "trade_elec_$suffix",
        tradeName = "Electrician",
        tradeCode = "ITI-RGH-EL",
        sessionYear = "2025-2026"
      )
    }
    assertTrue("Admin registration should succeed: ${adminResult.exceptionOrNull()}", adminResult.isSuccess)
    val adminUser = adminResult.getOrThrow()

    val traineeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createTraineeInTrade(
        tradeId = adminUser.tradeId,
        tradeName = adminUser.tradeName,
        sessionYear = adminUser.sessionYear,
        fullName = "Kiran Sahu",
        registrationNumber = "RGH-EL-$suffix",
        fatherName = "Mohan Sahu",
        batchShift = "Shift I (Morning)"
      )
    }
    assertTrue("Trainee creation should succeed: ${traineeResult.exceptionOrNull()}", traineeResult.isSuccess)
    val createdTrainee = traineeResult.getOrThrow()

    val traineesList = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeTraineesForTrade(adminUser.userId, adminUser.tradeId)
        .first { list -> list.any { it.traineeId == createdTrainee.traineeId } }
    }
    assertTrue(traineesList.any { it.traineeId == createdTrainee.traineeId })
  }

  @Test
  fun crossTradeIsolation_otherTradeAdminCannotReadTrainee() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    signInTestUser("admin_elec_$suffix@itiraigarh.edu.in")
    val elecRepo = ItiAttendanceRepository(firestore)

    val elecAdmin = withTimeout(DEFAULT_TIMEOUT_MS) {
      elecRepo.registerTrainingOfficer(
        fullName = "Elec Officer",
        employeeCode = "TO-EL-$suffix",
        tradeId = "trade_elec_$suffix",
        tradeName = "Electrician",
        tradeCode = "ITI-RGH-EL",
        sessionYear = "2025-2026"
      ).getOrThrow()
    }

    val elecTrainee = withTimeout(DEFAULT_TIMEOUT_MS) {
      elecRepo.createTraineeInTrade(
        tradeId = elecAdmin.tradeId,
        tradeName = elecAdmin.tradeName,
        sessionYear = elecAdmin.sessionYear,
        fullName = "Elec Trainee",
        registrationNumber = "RGH-EL-01",
        fatherName = "Father Name",
        batchShift = "Shift I"
      ).getOrThrow()
    }

    // Sign in as Fitter Admin
    signInTestUser("admin_fitter_$suffix@itiraigarh.edu.in")
    val fitterRepo = ItiAttendanceRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      fitterRepo.registerTrainingOfficer(
        fullName = "Fitter Officer",
        employeeCode = "TO-FIT-$suffix",
        tradeId = "trade_fit_$suffix",
        tradeName = "Fitter",
        tradeCode = "ITI-RGH-FIT",
        sessionYear = "2025-2026"
      ).getOrThrow()
    }

    // Fitter Admin attempting to fetch Electrician Trainee must fail with PERMISSION_DENIED
    val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      fitterRepo.getTraineeById(elecTrainee.traineeId)
    }
    assertTrue("Cross-trade access must fail", crossReadResult.isFailure)
    val ex = crossReadResult.exceptionOrNull() as? FirebaseFirestoreException
    assertNotNull("Expected FirebaseFirestoreException", ex)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
  }

  @Test
  fun traineeAttendanceAndLeaveWorkflow_enforcesOnePerDayAndLeaveLimits() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    signInTestUser("trainee_$suffix@itiraigarh.edu.in")
    val repo = ItiAttendanceRepository(firestore)

    val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.registerTraineeUser(
        fullName = "Deepak पटेल",
        registrationNumber = "RGH-COPA-$suffix",
        fatherName = "S. Patel",
        batchShift = "Shift I (Morning)",
        tradeId = "trade_copa_$suffix",
        tradeName = "COPA",
        sessionYear = "2025-2026"
      ).getOrThrow()
    }

    val trainee = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.getTraineeById(profile.userId).getOrThrow()
    }

    // 1. Mark attendance once for 2026-10-04 -> succeeds
    val firstMark = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.markDailyAttendanceByTrainee(trainee, "2026-10-04", AttendanceStatus.PRESENT)
    }
    assertTrue("First attendance mark should succeed: ${firstMark.exceptionOrNull()}", firstMark.isSuccess)

    // 2. Duplicate attendance mark for same day -> fails with PERMISSION_DENIED
    val duplicateMark = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.markDailyAttendanceByTrainee(trainee, "2026-10-04", AttendanceStatus.PRESENT)
    }
    assertTrue("Duplicate attendance on same day must fail", duplicateMark.isFailure)

    // 3. Apply for valid Casual Leave (3 days <= 12) -> succeeds
    val validLeave = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.submitLeaveApplication(
        trainee = trainee,
        leaveType = LeaveType.CL,
        startDate = "2026-10-10",
        endDate = "2026-10-12",
        totalDays = 3,
        halfDaySession = HalfDaySession.NONE,
        reason = "Sister wedding in Raigarh"
      )
    }
    assertTrue("Valid CL application should succeed: ${validLeave.exceptionOrNull()}", validLeave.isSuccess)
  }

  @Test
  fun unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = ItiAttendanceRepository(firestore)
    val oneShotResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.getTraineeById("some_uid")
    }
    assertTrue("Unauthenticated one-shot read must fail", oneShotResult.isFailure)
    val ex = oneShotResult.exceptionOrNull() as? FirebaseFirestoreException
    assertNotNull("Expected FirebaseFirestoreException", ex)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeAttendanceForTrainee("some_uid").first()
      }
      fail("Expected exception for unauthenticated flow query")
    } catch (t: Throwable) {
      var current: Throwable? = t
      var firestoreEx: FirebaseFirestoreException? = null
      while (current != null) {
        if (current is FirebaseFirestoreException) {
          firestoreEx = current
          break
        }
        current = current.cause
      }
      assertNotNull("Expected FirebaseFirestoreException in causal chain, got $t", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
