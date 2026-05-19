package am.loadboardbackend.service;

import am.loadboardbackend.dto.rating.RatingDto;
import am.loadboardbackend.dto.rating.RatingTagStat;
import am.loadboardbackend.dto.rating.RatingsResponse;
import am.loadboardbackend.dto.rating.SubmitRatingRequest;
import am.loadboardbackend.model.Broker;
import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.model.LoadPosting;
import am.loadboardbackend.model.Rating;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.BrokerRepository;
import am.loadboardbackend.repository.CarrierRepository;
import am.loadboardbackend.repository.LoadPostingRepository;
import am.loadboardbackend.repository.RatingRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    private final RatingRepository ratingRepository;
    private final LoadPostingRepository loadRepository;
    private final BrokerRepository brokerRepository;
    private final CarrierRepository carrierRepository;
    private final ObjectMapper objectMapper;

    public RatingsResponse getRatingsForMe(User user) {
        UUID targetId = resolveOwnProfileId(user);
        String targetType = user.getBroker() != null ? "broker" : "carrier";
        return buildResponse(targetId, targetType);
    }

    public RatingsResponse getRatingsForTarget(String targetType, UUID targetId) {
        if (!targetType.equals("broker") && !targetType.equals("carrier")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetType must be 'broker' or 'carrier'");
        }
        return buildResponse(targetId, targetType);
    }

    private static final Set<String> VALID_TAGS = Set.of(
            "communication", "payment", "accuracy",
            "on_time", "safe_delivery", "professional"
    );

    @Transactional
    public void submitRating(User submitter, SubmitRatingRequest req) {
        String targetType = req.targetType();
        if (!targetType.equals("broker") && !targetType.equals("carrier")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetType must be 'broker' or 'carrier'");
        }
        if (!req.type().equals("positive") && !req.type().equals("negative")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be 'positive' or 'negative'");
        }

        // Rule 4: validate tags
        if (req.tags() != null) {
            for (String tag : req.tags()) {
                if (!VALID_TAGS.contains(tag)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid tag '" + tag + "'. Allowed: communication, payment, accuracy");
                }
            }
        }

        // Rule 5: comment max 500 chars
        if (req.comment() != null && req.comment().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment must be 500 characters or fewer");
        }

        if (targetType.equals("broker") && submitter.getBroker() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brokers cannot rate other brokers");
        }
        if (targetType.equals("carrier") && submitter.getCarrier() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Carriers cannot rate other carriers");
        }
        if (submitter.getBroker() == null && submitter.getCarrier() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers and carriers can submit ratings");
        }

        LoadPosting load = loadRepository.findById(req.loadId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found"));

        // Rule 1: load must be delivered or later
        if (load.getStatus() != LoadPosting.LoadStatus.DELIVERED
                && load.getStatus() != LoadPosting.LoadStatus.PAID
                && load.getStatus() != LoadPosting.LoadStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ratings can only be submitted after the load has been delivered");
        }

        UUID submitterId = resolveOwnProfileId(submitter);

        // Rule 2: submitter must be a participant — the broker who posted or the assigned carrier
        boolean isBrokerParticipant = submitter.getBroker() != null
                && load.getBroker() != null
                && submitter.getBroker().getId().equals(load.getBroker().getId())
                || submitter.getDealer() != null
                && load.getDealer() != null
                && submitter.getDealer().getId().equals(load.getDealer().getId());
        boolean isCarrierParticipant = submitter.getCarrier() != null
                && load.getAssignedCarrier() != null
                && submitter.getCarrier().getId().equals(load.getAssignedCarrier().getId());
        if (!isBrokerParticipant && !isCarrierParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You were not a participant in this load");
        }

        // Rule 3: no duplicate ratings for the same load + submitter
        if (ratingRepository.existsByLoadIdAndSubmitterId(req.loadId(), submitterId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already rated this load");
        }

        Rating rating = new Rating();
        rating.setTargetId(req.targetId());
        rating.setTargetType(targetType);
        rating.setLoadId(req.loadId());
        rating.setSubmitterId(submitterId);
        rating.setType(req.type());
        rating.setComment(req.comment());

        if (req.tags() != null && !req.tags().isEmpty()) {
            try {
                rating.setTags(objectMapper.writeValueAsString(req.tags()));
            } catch (Exception e) {
                log.warn("Failed to serialize tags", e);
            }
        }

        ratingRepository.save(rating);
    }

    public List<UUID> getMySubmittedLoadIds(User submitter) {
        UUID submitterId = resolveOwnProfileId(submitter);
        return ratingRepository.findAllBySubmitterId(submitterId)
                .stream()
                .map(Rating::getLoadId)
                .toList();
    }

    public long computeRatingScore(UUID targetId, String targetType) {
        List<Rating> ratings = ratingRepository.findAllByTargetIdAndTargetType(targetId, targetType);
        if (ratings.isEmpty()) return -1;
        long positive = ratings.stream().filter(r -> "positive".equals(r.getType())).count();
        return Math.round((double) positive / ratings.size() * 100);
    }

    private RatingsResponse buildResponse(UUID targetId, String targetType) {
        List<Rating> ratings = ratingRepository.findAllByTargetIdAndTargetType(targetId, targetType);

        long positiveCount = ratings.stream().filter(r -> "positive".equals(r.getType())).count();
        long negativeCount = ratings.stream().filter(r -> "negative".equals(r.getType())).count();
        long total = ratings.size();

        Map<String, Long> tagCounts = new LinkedHashMap<>();
        for (Rating r : ratings) {
            for (String tag : parseTags(r.getTags())) {
                tagCounts.merge(tag, 1L, Long::sum);
            }
        }

        List<RatingTagStat> tagStats = tagCounts.entrySet().stream()
                .map(e -> new RatingTagStat(e.getKey(), e.getValue(), total))
                .toList();

        List<RatingDto> dtos = ratings.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(r -> toDto(r, targetType))
                .toList();

        return new RatingsResponse(positiveCount, negativeCount, tagStats, dtos);
    }

    private RatingDto toDto(Rating rating, String targetType) {
        String fromName = null;
        String fromRole;
        String submitterType = targetType.equals("broker") ? "carrier" : "broker";
        UUID submitterId = rating.getSubmitterId();

        if ("broker".equals(submitterType)) {
            fromRole = "Broker";
            Broker broker = brokerRepository.findById(submitterId).orElse(null);
            if (broker != null) {
                fromName = broker.getCompanyName() != null ? broker.getCompanyName() : broker.getLegalName();
            }
        } else {
            fromRole = "Carrier";
            Carrier carrier = carrierRepository.findById(submitterId).orElse(null);
            if (carrier != null) {
                fromName = carrier.getCompanyName() != null ? carrier.getCompanyName() : carrier.getLegalName();
            }
        }

        String loadTitle = null;
        LoadPosting load = loadRepository.findById(rating.getLoadId()).orElse(null);
        if (load != null && load.getVehicle() != null) {
            var v = load.getVehicle();
            loadTitle = String.join(" ",
                    v.getYear() != null ? String.valueOf(v.getYear()) : "",
                    v.getMake() != null ? v.getMake() : "",
                    v.getModel() != null ? v.getModel() : ""
            ).trim();
        }

        return new RatingDto(
                rating.getId(),
                rating.getType(),
                fromName,
                fromRole,
                loadTitle,
                parseTags(rating.getTags()),
                rating.getComment(),
                rating.getCreatedAt(),
                rating.getLoadId()
        );
    }

    private List<String> parseTags(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private UUID resolveOwnProfileId(User user) {
        if (user.getBroker() != null) return user.getBroker().getId();
        if (user.getCarrier() != null) return user.getCarrier().getId();
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User has no broker or carrier profile");
    }
}
