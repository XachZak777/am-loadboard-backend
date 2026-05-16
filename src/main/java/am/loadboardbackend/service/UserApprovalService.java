package am.loadboardbackend.service;

import am.loadboardbackend.config.AppProperties;
import am.loadboardbackend.mailing.RegistrationApprovedEmailContext;
import am.loadboardbackend.mailing.RegistrationDeclinedEmailContext;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserApprovalService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AppProperties appProperties;

    @Transactional
    public User approveUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setAdminApproved(true);
        user.setAdminApprovedAt(LocalDateTime.now());
        user.setDeclined(false);
        user.setDeclinedAt(null);
        User saved = userRepository.save(user);
        sendApprovalEmail(saved);
        return saved;
    }

    @Transactional
    public User rejectUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setAdminApproved(false);
        user.setAdminApprovedAt(null);
        user.setDeclined(true);
        user.setDeclinedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        sendDeclinedEmail(saved);
        return saved;
    }

    /** Approve the user whose carrier.id == carrierId. */
    @Transactional
    public User approveByCarrierId(UUID carrierId) {
        User user = userRepository.findByCarrierId(carrierId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User with carrier id " + carrierId + " not found"));
        user.setAdminApproved(true);
        user.setAdminApprovedAt(LocalDateTime.now());
        user.setDeclined(false);
        user.setDeclinedAt(null);
        User saved = userRepository.save(user);
        sendApprovalEmail(saved);
        return saved;
    }

    /** Approve the user whose broker.id == brokerId. */
    @Transactional
    public User approveByBrokerId(UUID brokerId) {
        User user = userRepository.findByBrokerId(brokerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User with broker id " + brokerId + " not found"));
        user.setAdminApproved(true);
        user.setAdminApprovedAt(LocalDateTime.now());
        user.setDeclined(false);
        user.setDeclinedAt(null);
        User saved = userRepository.save(user);
        sendApprovalEmail(saved);
        return saved;
    }

    /** Decline (reject) the user whose carrier.id == carrierId. */
    @Transactional
    public User declineByCarrierId(UUID carrierId) {
        User user = userRepository.findByCarrierId(carrierId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User with carrier id " + carrierId + " not found"));
        user.setAdminApproved(false);
        user.setAdminApprovedAt(null);
        user.setDeclined(true);
        user.setDeclinedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        sendDeclinedEmail(saved);
        return saved;
    }

    /** Decline (reject) the user whose broker.id == brokerId. */
    @Transactional
    public User declineByBrokerId(UUID brokerId) {
        User user = userRepository.findByBrokerId(brokerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User with broker id " + brokerId + " not found"));
        user.setAdminApproved(false);
        user.setAdminApprovedAt(null);
        user.setDeclined(true);
        user.setDeclinedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        sendDeclinedEmail(saved);
        return saved;
    }

    /**
     * Revoke approval for the carrier's user — moves them back to Pending
     * (adminApproved=false, declined=false). No email is sent.
     */
    @Transactional
    public User revokeByCarrierId(UUID carrierId) {
        User user = userRepository.findByCarrierId(carrierId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User with carrier id " + carrierId + " not found"));
        user.setAdminApproved(false);
        user.setAdminApprovedAt(null);
        user.setDeclined(false);
        user.setDeclinedAt(null);
        log.info("Revoked approval for carrierId={} userId={}", carrierId, user.getId());
        return userRepository.save(user);
    }

    /**
     * Revoke approval for the broker's user — moves them back to Pending
     * (adminApproved=false, declined=false). No email is sent.
     */
    @Transactional
    public User revokeByBrokerId(UUID brokerId) {
        User user = userRepository.findByBrokerId(brokerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User with broker id " + brokerId + " not found"));
        user.setAdminApproved(false);
        user.setAdminApprovedAt(null);
        user.setDeclined(false);
        user.setDeclinedAt(null);
        log.info("Revoked approval for brokerId={} userId={}", brokerId, user.getId());
        return userRepository.save(user);
    }

    // ── email helpers ─────────────────────────────────────────────────────────

    private void sendApprovalEmail(User user) {
        try {
            RegistrationApprovedEmailContext ctx = new RegistrationApprovedEmailContext();
            ctx.init(user);
            ctx.setFrom(appProperties.getMail().getFrom());
            ctx.buildLoginUrl(appProperties.getFrontend().getBaseUrl());
            emailService.sendEmail(ctx);
            log.info("Approval email queued for userId={} email={}", user.getId(), user.getEmail());
        } catch (Exception e) {
            log.error("Failed to queue approval email for userId={}: {}", user.getId(), e.getMessage(), e);
        }
    }

    private void sendDeclinedEmail(User user) {
        try {
            RegistrationDeclinedEmailContext ctx = new RegistrationDeclinedEmailContext();
            ctx.init(user);
            ctx.setFrom(appProperties.getMail().getFrom());
            emailService.sendEmail(ctx);
            log.info("Declined email queued for userId={} email={}", user.getId(), user.getEmail());
        } catch (Exception e) {
            log.error("Failed to queue declined email for userId={}: {}", user.getId(), e.getMessage(), e);
        }
    }
}

