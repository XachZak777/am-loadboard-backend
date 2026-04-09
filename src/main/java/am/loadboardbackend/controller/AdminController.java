package am.loadboardbackend.controller;

import am.loadboardbackend.dto.load.LoadPostingDto;
import am.loadboardbackend.model.AuditLog;
import am.loadboardbackend.model.Broker;
import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.model.LoadPosting;
import am.loadboardbackend.model.User;
import am.loadboardbackend.service.AdminService;
import am.loadboardbackend.service.UserApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final UserApprovalService userApprovalService;

    // Carrier endpoints
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

    // Broker endpoints
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

    // User approval endpoints
    @PostMapping("/users/{id}/approve")
    public ResponseEntity<User> approveUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userApprovalService.approveUser(id));
    }

    @PostMapping("/users/{id}/reject")
    public ResponseEntity<User> rejectUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userApprovalService.rejectUser(id));
    }

    // User history endpoints
    @GetMapping("/users/{id}/history")
    public ResponseEntity<List<AuditLog>> getUserHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getUserHistory(id));
    }

    @GetMapping("/entities/{id}/history")
    public ResponseEntity<List<AuditLog>> getEntityHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getEntityHistory(id));
    }

    // Load management endpoints
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
