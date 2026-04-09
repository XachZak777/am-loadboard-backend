package am.loadboardbackend.service;

import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserApprovalService {

    private final UserRepository userRepository;

    @Transactional
    public User approveUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setAdminApproved(true);
        user.setAdminApprovedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public User rejectUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setAdminApproved(false);
        user.setAdminApprovedAt(null);
        return userRepository.save(user);
    }
}
