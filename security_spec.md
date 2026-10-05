# Security Specification — ITI Raigarh Attendance

## 1. Data Invariants

1. **Zero Unauthenticated Access**: Every read and write operation across `/admins`, `/users`, `/trades`, `/trainees`, `/attendance`, `/leaves`, and `/notifications` requires `request.auth != null`.
2. **Role-Based Access Control (RBAC) & No Self-Assigned Admin Escalation**:
   - A user cannot create or update `/users/{userId}` with `role == 'ADMIN'` unless `isAdmin()` is true (`exists(/databases/$(database)/documents/admins/$(request.auth.uid))` or verified bootstrapped admin email).
   - Creating `/admins/{adminId}` requires `request.auth.uid == adminId` and the institutional verification code `officerVerificationCode == 'ITI-RAIGARH-OFFICER'`.
3. **Strict Trade-Wise Data Isolation**:
   - A Training Officer (`AdminProfile`) is bound to `assignedTradeId`.
   - Helper `isAdminForTrade(tradeId)` verifies that the caller's `/admins/$(request.auth.uid).data.assignedTradeId == tradeId` (or bootstrapped admin with `email_verified == true`).
   - An Admin of Trade A (`trade_electrician`) CANNOT read, create, or modify trainees, attendance records, leave applications, or notifications belonging to Trade B (`trade_fitter`).
4. **One Attendance Record Per Trainee Per Day**:
   - Document ID in `/attendance/{attendanceId}` must equal `incoming().traineeId + '_' + incoming().dateString`.
   - Duplicate attendance submissions for the same trainee and date fail automatically because `create` cannot overwrite an existing document and trainees have no `update` permission on `/attendance`.
5. **Automatic Leave Quota Enforcement**:
   - Casual Leave (`CL`): `totalDays >= 1 && totalDays <= 12`.
   - Half-Day Leave (`HALF_DAY`): `totalDays == 1` and `halfDaySession in ['FIRST_HALF', 'SECOND_HALF']`.
   - Medical Leave (`MEDICAL`): `totalDays >= 1 && totalDays <= 36`.
   - On creation, all leave requests must start in `status == 'PENDING'`. Only the assigned Trade's Training Officer can transition `status` to `'APPROVED'` or `'REJECTED'`.
6. **Secure List Queries (No Blanket Reads)**:
   - Every `allow list` rule validates `resource.data.traineeUid == request.auth.uid || resource.data.adminId == request.auth.uid` without `get()` or `exists()` lookups.

## 2. The "Dirty Dozen" Adversarial Payloads

1. **Unauthenticated Scrape**: Unauthenticated client queries `/trainees` or `/attendance`. -> Rejected by `isSignedIn()`.
2. **Self-Assigned Admin Escalation**: Regular user creates `/users/{uid}` with `role: "ADMIN"` without an `/admins/{uid}` record. -> Rejected by `(incoming().role == 'TRAINEE' || isAdmin())`.
3. **Cross-Trade Admin Snooping**: Admin of `trade_electrician` attempts to read or update a trainee or leave application in `trade_fitter`. -> Rejected by `isAdminForTrade(tradeId)` and `resource.data.adminId == request.auth.uid`.
4. **Cross-Trainee Data Leak**: Trainee A attempts `get` or `list` on Trainee B's `/attendance` or `/leaves`. -> Rejected by `resource.data.traineeUid == request.auth.uid`.
5. **Duplicate Daily Attendance Injection**: Trainee attempts to create a second attendance document for `2026-10-04` using a random ID `att_999`. -> Rejected by `incoming().attendanceId == incoming().traineeId + '_' + incoming().dateString`.
6. **Trainee Self-Approving Leave**: Trainee creates a leave with `status: "APPROVED"` or updates their pending leave to `"APPROVED"`. -> Rejected by `incoming().status == 'PENDING'` on create and admin-only update gate.
7. **Leave Quota Overflow**: Trainee submits Casual Leave with `totalDays: 15` (exceeding 12) or Medical Leave with `totalDays: 40` (exceeding 36). -> Rejected by `isValidLeaveApplication(incoming())`.
8. **Shadow Field Injection**: Client sends a valid `/trainees/{id}` update plus an undeclared field `"isVerified": true`. -> Rejected by `incoming().keys().hasOnly(...)` and `affectedKeys().hasOnly(...)`.
9. **Immortal Field Mutation**: Client attempts to change `createdAt`, `traineeId`, or `tradeId` during an update. -> Rejected by equality assertions `incoming().createdAt == existing().createdAt`.
10. **Value Poisoning on Update**: Admin updates `presentDays` on `/trainees/{id}` with a string `"100"` or negative number `-5`. -> Rejected because `isValidTraineeRecord(incoming())` wraps the entire `allow update` block.
11. **Unverified Email Spoofing**: Attacker crafts token with `email == 'abhixfactor@gmail.com'` and `email_verified == false`. -> Rejected by `request.auth.token.email_verified == true`.
12. **Blanket Query Attack**: Authenticated trainee runs `db.collection("attendance").get()` without `.whereEqualTo("traineeUid", uid)`. -> Rejected by `allow list` checking `resource.data.traineeUid == request.auth.uid`.
