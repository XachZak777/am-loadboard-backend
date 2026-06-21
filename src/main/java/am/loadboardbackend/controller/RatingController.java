package am.loadboardbackend.controller;

import am.loadboardbackend.dto.rating.RatingsResponse;
import am.loadboardbackend.dto.rating.SubmitRatingRequest;
import am.loadboardbackend.model.User;
import am.loadboardbackend.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @GetMapping("/me")
    public ResponseEntity<RatingsResponse> getMyRatings(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ratingService.getRatingsForMe(user));
    }

    @GetMapping("/{targetType}/{id}")
    public ResponseEntity<RatingsResponse> getRatingsForTarget(
            @PathVariable String targetType,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ratingService.getRatingsForTarget(targetType, id));
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> submitRating(
            @AuthenticationPrincipal User user,
            @RequestBody SubmitRatingRequest request) {
        ratingService.submitRating(user, request);
        return ResponseEntity.ok(Map.of("message", "Rating submitted successfully"));
    }

    @GetMapping("/my-submitted-load-ids")
    public ResponseEntity<List<UUID>> getMySubmittedLoadIds(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ratingService.getMySubmittedLoadIds(user));
    }
}
