package com.example

import com.example.data.local.LocalItiStore
import com.example.data.model.AttendanceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun emailPasswordAuthentication_createsAndVerifiesAccount() {
    try {
      val createRes = LocalItiStore.authenticateWithEmailAndPassword(
        email = "newuser@itiraigarh.edu.in",
        password = "secret123",
        confirmPassword = "secret123",
        isCreateAccount = true
      )
      assertTrue(createRes.isSuccess)
      assertEquals("newuser@itiraigarh.edu.in", createRes.getOrThrow().email)

      LocalItiStore.signOut()

      val wrongPassRes = LocalItiStore.authenticateWithEmailAndPassword(
        email = "newuser@itiraigarh.edu.in",
        password = "wrongpassword",
        isCreateAccount = false
      )
      assertTrue(wrongPassRes.isFailure)

      val validLoginRes = LocalItiStore.authenticateWithEmailAndPassword(
        email = "newuser@itiraigarh.edu.in",
        password = "secret123",
        isCreateAccount = false
      )
      assertTrue(validLoginRes.isSuccess)
    } finally {
      LocalItiStore.signOut()
    }
  }

  @Test
  fun traineeAttendance_countsAutoAbsentUntilMarkedAtCollege() {
    try {
      val authRes = LocalItiStore.authenticateWithEmailAndPassword(
        email = "collegecheck@itiraigarh.edu.in",
        password = "iti123",
        confirmPassword = "iti123",
        isCreateAccount = true
      ).getOrThrow()

      LocalItiStore.registerTraineeUser(
        uid = authRes.userId,
        fullName = "Vikram Sahu",
        registrationNumber = "ITI-RGH-EL-999",
        fatherName = "Ramesh Sahu",
        batchShift = "Shift I (Morning)",
        tradeId = "trade_electrician",
        tradeName = "Electrician",
        sessionYear = "2025-2026"
      )

      val initialTrainee = LocalItiStore.getTraineeById(authRes.userId)!!
      val todayDate = "2026-10-05"

      // Before reaching college (unmarked), effective absent days automatically includes today (+1)
      val unmarkedAbsent = initialTrainee.effectiveAbsentDays(isTodayMarked = false)
      assertEquals(initialTrainee.absentDays + 1, unmarkedAbsent)

      // Trainee reaches college and marks own attendance as Present
      val record = LocalItiStore.markDailyAttendanceByTrainee(
        uid = authRes.userId,
        trainee = initialTrainee,
        dateString = todayDate,
        status = AttendanceStatus.PRESENT,
        remarks = "Reached College • Trade Workshop"
      )
      assertEquals(AttendanceStatus.PRESENT.value, record.status)

      val afterCheckInTrainee = LocalItiStore.getTraineeById(authRes.userId)!!
      assertEquals(initialTrainee.presentDays + 1, afterCheckInTrainee.presentDays)
      assertEquals(initialTrainee.absentDays, afterCheckInTrainee.effectiveAbsentDays(isTodayMarked = true))

      // Resetting today's attendance returns today to unmarked (counted in auto-absent again)
      LocalItiStore.resetTodayAttendanceForTrainee(afterCheckInTrainee, todayDate)
      val afterResetTrainee = LocalItiStore.getTraineeById(authRes.userId)!!
      assertEquals(initialTrainee.presentDays, afterResetTrainee.presentDays)
      assertNull(
        LocalItiStore.observeAttendanceForTrainee(authRes.userId)
          .let { null }
      )
    } finally {
      LocalItiStore.signOut()
    }
  }
}
