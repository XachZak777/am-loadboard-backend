package am.loadboardbackend.controller;

import am.loadboardbackend.dto.NotificationCountResponse;
import am.loadboardbackend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/count")
    public ResponseEntity<NotificationCountResponse> getCount() {
        return ResponseEntity.ok(notificationService.getCount());
    }
}
