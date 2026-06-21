package am.loadboardbackend.controller;

import am.loadboardbackend.dto.admin.AdminUserDto;
import am.loadboardbackend.dto.load.LoadPostingDto;
import am.loadboardbackend.model.AuditLog;
import am.loadboardbackend.model.Broker;
import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.model.LoadPosting;
import am.loadboardbackend.model.User;
import am.loadboardbackend.service.AdminService;
import am.loadboardbackend.service.AdminUserService;
import am.loadboardbackend.service.UserApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService       adminService;
    private final AdminUserService   adminUserService;
    private final UserApprovalService userApprovalService;

    // ── Unified user list ────────────────────────────────────────────────────

    /**
     * GET /api/admin/users
     * Returns all carriers and brokers with their profile fields and uploaded documents.
     * This is the primary endpoint for the admin registration-review panel.
     */
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    /** GET /api/admin/users/approved — only users with adminApproved = true. */
    @GetMapping("/users/approved")
    public ResponseEntity<List<AdminUserDto>> getApprovedUsers() {
        return ResponseEntity.ok(adminUserService.getApprovedUsers());
    }

    /** GET /api/admin/users/pending — users not yet reviewed (adminApproved=false AND declined=false). */
    @GetMapping("/users/pending")
    public ResponseEntity<List<AdminUserDto>> getPendingUsers() {
        return ResponseEntity.ok(adminUserService.getPendingUsers());
    }

    /** GET /api/admin/users/rejected — users whose registration was actively declined. */
    @GetMapping("/users/rejected")
    public ResponseEntity<List<AdminUserDto>> getRejectedUsers() {
        return ResponseEntity.ok(adminUserService.getRejectedUsers());
    }

    /**
     * GET /api/admin/users/{id}
     * Full detail of one user (for the admin review drawer/page).
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDto> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    /**
     * DELETE /api/admin/users/{id}
     * Hard-delete a user and their linked carrier/broker record + documents.
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ── Carrier endpoints ────────────────────────────────────────────────────

    @GetMapping("/carriers")
    public ResponseEntity<List<Carrier>> getAllCarriers() {
        return ResponseEntity.ok(adminService.getAllCarriers());
    }

    @GetMapping("/carriers/{id}")
    public ResponseEntity<Carrier> getCarrier(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getCarrierById(id));
    }

    @DeleteMapping("/carriers/{id}")
    public ResponseEntity<Void> deleteCarrier(@PathVariable UUID id) {
        adminService.deleteCarrier(id);
        return ResponseEntity.noContent().build();
    }

    /** POST /api/admin/carriers/{id}/approve — approve the carrier's user account. */
    @PostMapping("/carriers/{id}/approve")
    public ResponseEntity<Map<String, String>> approveCarrier(@PathVariable UUID id) {
        userApprovalService.approveByCarrierId(id);
        return ResponseEntity.ok(Map.of("message", "Carrier approved"));
    }

    /** POST /api/admin/carriers/{id}/decline — decline the carrier's registration. */
    @PostMapping("/carriers/{id}/decline")
    public ResponseEntity<Map<String, String>> declineCarrier(@PathVariable UUID id) {
        userApprovalService.declineByCarrierId(id);
        return ResponseEntity.ok(Map.of("message", "Carrier declined"));
    }

    /** POST /api/admin/carriers/{id}/revoke — revoke approval, moving carrier back to Pending. */
    @PostMapping("/carriers/{id}/revoke")
    public ResponseEntity<Map<String, String>> revokeCarrier(@PathVariable UUID id) {
        userApprovalService.revokeByCarrierId(id);
        return ResponseEntity.ok(Map.of("message", "Carrier approval revoked"));
    }

    // ── Broker endpoints ─────────────────────────────────────────────────────

    @GetMapping("/brokers")
    public ResponseEntity<List<Broker>> getAllBrokers() {
        return ResponseEntity.ok(adminService.getAllBrokers());
    }

    @GetMapping("/brokers/{id}")
    public ResponseEntity<Broker> getBroker(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getBrokerById(id));
    }

    @DeleteMapping("/brokers/{id}")
    public ResponseEntity<Void> deleteBroker(@PathVariable UUID id) {
        adminService.deleteBroker(id);
        return ResponseEntity.noContent().build();
    }

    /** POST /api/admin/brokers/{id}/approve — approve the broker's user account. */
    @PostMapping("/brokers/{id}/approve")
    public ResponseEntity<Map<String, String>> approveBroker(@PathVariable UUID id) {
        userApprovalService.approveByBrokerId(id);
        return ResponseEntity.ok(Map.of("message", "Broker approved"));
    }

    /** POST /api/admin/brokers/{id}/decline — decline the broker's registration. */
    @PostMapping("/brokers/{id}/decline")
    public ResponseEntity<Map<String, String>> declineBroker(@PathVariable UUID id) {
        userApprovalService.declineByBrokerId(id);
        return ResponseEntity.ok(Map.of("message", "Broker declined"));
    }

    /** POST /api/admin/brokers/{id}/revoke — revoke approval, moving broker back to Pending. */
    @PostMapping("/brokers/{id}/revoke")
    public ResponseEntity<Map<String, String>> revokeBroker(@PathVariable UUID id) {
        userApprovalService.revokeByBrokerId(id);
        return ResponseEntity.ok(Map.of("message", "Broker approval revoked"));
    }

    // ── Legacy user-level approval (by user id) ──────────────────────────────

    @PostMapping("/users/{id}/approve")
    public ResponseEntity<User> approveUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userApprovalService.approveUser(id));
    }

    @PostMapping("/users/{id}/reject")
    public ResponseEntity<User> rejectUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userApprovalService.rejectUser(id));
    }

    // ── User history / audit log ─────────────────────────────────────────────

    @GetMapping("/users/{id}/history")
    public ResponseEntity<List<AuditLog>> getUserHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getUserHistory(id));
    }

    @GetMapping("/entities/{id}/history")
    public ResponseEntity<List<AuditLog>> getEntityHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getEntityHistory(id));
    }

    // ── Load management ──────────────────────────────────────────────────────

    @GetMapping("/loads")
    public ResponseEntity<List<LoadPosting>> getAllLoads() {
        return ResponseEntity.ok(adminService.getAllLoads());
    }

    @GetMapping("/loads/{id}")
    public ResponseEntity<LoadPosting> getLoad(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getLoadById(id));
    }

    @PostMapping("/loads/{brokerId}")
    public ResponseEntity<LoadPosting> createLoad(
            @PathVariable UUID brokerId,
            @RequestBody LoadPostingDto dto) {
        LoadPosting load = adminService.createLoad(brokerId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(load);
    }

    @PutMapping("/loads/{id}")
    public ResponseEntity<LoadPosting> updateLoad(
            @PathVariable UUID id,
            @RequestBody LoadPostingDto dto) {
        LoadPosting load = adminService.updateLoad(id, dto);
        return ResponseEntity.ok(load);
    }

    @DeleteMapping("/loads/{id}")
    public ResponseEntity<Void> deleteLoad(@PathVariable UUID id) {
        adminService.deleteLoad(id);
        return ResponseEntity.noContent().build();
    }
}
