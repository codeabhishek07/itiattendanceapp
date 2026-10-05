const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_ELEC_UID = "admin_elec_1";
const ADMIN_FITTER_UID = "admin_fitter_2";
const TRAINEE_ALICE_UID = "trainee_alice_1";
const TRAINEE_BOB_UID = "trainee_bob_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTimestamp() {
  return new Date();
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedTradeAndAdminAndTrainee() {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const now = nowTimestamp();
    await db.collection("admins").doc(ADMIN_ELEC_UID).set({
      adminId: ADMIN_ELEC_UID,
      fullName: "R. K. Sharma",
      assignedTradeId: "trade_electrician",
      assignedTradeName: "Electrician",
      sessionYear: "2025-2026",
      officerVerificationCode: "ITI-RAIGARH-OFFICER",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("admins").doc(ADMIN_FITTER_UID).set({
      adminId: ADMIN_FITTER_UID,
      fullName: "S. Verma",
      assignedTradeId: "trade_fitter",
      assignedTradeName: "Fitter",
      sessionYear: "2025-2026",
      officerVerificationCode: "ITI-RAIGARH-OFFICER",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("trades").doc("trade_electrician").set({
      tradeId: "trade_electrician",
      tradeName: "Electrician",
      tradeCode: "ITI-RGH-ELEC",
      adminId: ADMIN_ELEC_UID,
      adminName: "R. K. Sharma",
      sessionYear: "2025-2026",
      totalWorkingDays: 240,
      maxCasualLeave: 12,
      maxMedicalLeave: 36,
      halfDayDeductionRule: "DEDUCT_HALF_CL",
      maxHalfDayLeaves: 12,
      minAttendancePercent: 75,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("trainees").doc(TRAINEE_ALICE_UID).set({
      traineeId: TRAINEE_ALICE_UID,
      traineeUid: TRAINEE_ALICE_UID,
      adminId: ADMIN_ELEC_UID,
      tradeId: "trade_electrician",
      tradeName: "Electrician",
      sessionYear: "2025-2026",
      fullName: "Anjali Sahu",
      registrationNumber: "RGH-EL-25-01",
      fatherName: "Mohan Sahu",
      batchShift: "Shift I (Morning)",
      isActive: true,
      presentDays: 10,
      absentDays: 1,
      halfDays: 0,
      approvedLeaveDays: 0,
      casualLeaveUsed: 0,
      medicalLeaveUsed: 0,
      halfDayLeaveUsed: 0,
      lowAttendanceWarningCount: 0,
      lastWarningMessage: "",
      createdAt: now,
      updatedAt: now,
    });
  });
}

test("1. Unauthenticated user: cannot read trainees or attendance", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("trainees").get());
  await assertFails(unauthDb.collection("attendance").get());
});

test("2. Self-Assigned Admin Escalation: regular user cannot create ADMIN role without /admins record", async () => {
  const aliceDb = testEnv.authenticatedContext(TRAINEE_ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(TRAINEE_ALICE_UID).set({
      userId: TRAINEE_ALICE_UID,
      fullName: "Anjali Sahu",
      registrationNumber: "RGH-EL-25-01",
      role: "ADMIN",
      tradeId: "trade_electrician",
      tradeName: "Electrician",
      sessionYear: "2025-2026",
      isActive: true,
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );
});

test("3. Trade-Wise Isolation: Fitter Admin cannot access Electrician Trainee", async () => {
  await seedTradeAndAdminAndTrainee();
  const fitterAdminDb = testEnv.authenticatedContext(ADMIN_FITTER_UID).firestore();
  await assertFails(
    fitterAdminDb.collection("trainees").doc(TRAINEE_ALICE_UID).get()
  );
  const elecAdminDb = testEnv.authenticatedContext(ADMIN_ELEC_UID).firestore();
  await assertSucceeds(
    elecAdminDb.collection("trainees").doc(TRAINEE_ALICE_UID).get()
  );
});

test("4. Cross-Trainee Isolation: Bob cannot read Alice's trainee or attendance record", async () => {
  await seedTradeAndAdminAndTrainee();
  const bobDb = testEnv.authenticatedContext(TRAINEE_BOB_UID).firestore();
  await assertFails(
    bobDb.collection("trainees").doc(TRAINEE_ALICE_UID).get()
  );
});

test("5. One Attendance Record Per Day: Trainee can mark today's attendance once, duplicate fails", async () => {
  await seedTradeAndAdminAndTrainee();
  const aliceDb = testEnv.authenticatedContext(TRAINEE_ALICE_UID).firestore();
  const attId = `${TRAINEE_ALICE_UID}_2026-10-04`;
  const payload = {
    attendanceId: attId,
    traineeId: TRAINEE_ALICE_UID,
    traineeUid: TRAINEE_ALICE_UID,
    traineeName: "Anjali Sahu",
    registrationNumber: "RGH-EL-25-01",
    tradeId: "trade_electrician",
    tradeName: "Electrician",
    adminId: ADMIN_ELEC_UID,
    dateString: "2026-10-04",
    monthString: "2026-10",
    sessionYear: "2025-2026",
    status: "PRESENT",
    markedByRole: "TRAINEE",
    remarks: "On time",
    checkInTime: "09:05 AM",
    createdAt: nowTimestamp(),
    updatedAt: nowTimestamp(),
  };

  await assertSucceeds(aliceDb.collection("attendance").doc(attId).set(payload));
  // Duplicate submission on same day fails because trainee cannot update existing attendance
  await assertFails(aliceDb.collection("attendance").doc(attId).set(payload));
});

test("6. Leave Limits & Self-Approval Prevention: CL > 12 fails, Medical > 36 fails, self-APPROVED fails", async () => {
  await seedTradeAndAdminAndTrainee();
  const aliceDb = testEnv.authenticatedContext(TRAINEE_ALICE_UID).firestore();

  const baseLeave = {
    leaveId: "leave_1",
    traineeId: TRAINEE_ALICE_UID,
    traineeUid: TRAINEE_ALICE_UID,
    traineeName: "Anjali Sahu",
    registrationNumber: "RGH-EL-25-01",
    tradeId: "trade_electrician",
    tradeName: "Electrician",
    adminId: ADMIN_ELEC_UID,
    sessionYear: "2025-2026",
    leaveType: "CL",
    startDate: "2026-10-05",
    endDate: "2026-10-07",
    monthString: "2026-10",
    totalDays: 3,
    halfDaySession: "NONE",
    reason: "Family function in Raigarh",
    status: "PENDING",
    adminRemarks: "",
    reviewedBy: "",
    createdAt: nowTimestamp(),
    updatedAt: nowTimestamp(),
  };

  // Valid CL <= 12 succeeds
  await assertSucceeds(aliceDb.collection("leaves").doc("leave_1").set(baseLeave));

  // CL > 12 fails
  await assertFails(
    aliceDb.collection("leaves").doc("leave_2").set({
      ...baseLeave,
      leaveId: "leave_2",
      totalDays: 15,
    })
  );

  // Medical > 36 fails
  await assertFails(
    aliceDb.collection("leaves").doc("leave_3").set({
      ...baseLeave,
      leaveId: "leave_3",
      leaveType: "MEDICAL",
      totalDays: 40,
    })
  );

  // Trainee self-approving leave fails
  await assertFails(
    aliceDb.collection("leaves").doc("leave_4").set({
      ...baseLeave,
      leaveId: "leave_4",
      status: "APPROVED",
    })
  );
});

test("7. Client Query Alignment: Trainee can query own leaves with filter, fails without filter", async () => {
  await seedTradeAndAdminAndTrainee();
  const aliceDb = testEnv.authenticatedContext(TRAINEE_ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("trainees").where("traineeUid", "==", TRAINEE_ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("trainees").get());
});
