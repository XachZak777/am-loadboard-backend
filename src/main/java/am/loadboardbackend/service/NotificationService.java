package am.loadboardbackend.service;

import am.loadboardbackend.dto.NotificationCountResponse;
import am.loadboardbackend.model.BidStatus;
import am.loadboardbackend.model.LoadPosting;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.BidRepository;
import am.loadboardbackend.repository.LoadPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final LoadPostingRepository loadRepo;
    private final BidRepository bidRepo;
    private final AuthService authService;

    public NotificationCountResponse getCount() {
        User user = authService.currentUserOrThrow();

        if (user.getBroker() != null) {
            return getBrokerCount(user);
        } else if (user.getDealer() != null) {
            return getDealerCount(user);
        } else if (user.getCarrier() != null) {
            return getCarrierCount(user);
        }
        return NotificationCountResponse.builder().total(0).build();
    }

    private NotificationCountResponse getBrokerCount(User user) {
        var brokerId = user.getBroker().getId();
        long pendingBids = bidRepo.countByBrokerIdAndStatus(brokerId, BidStatus.PENDING);
        long loadsNeedingAction = loadRepo.countByBrokerIdAndStatusIn(brokerId,
                List.of(LoadPosting.LoadStatus.PICKED_UP, LoadPosting.LoadStatus.DELIVERED));
        int total = (int) (pendingBids + loadsNeedingAction);
        return NotificationCountResponse.builder()
                .pendingBids((int) pendingBids)
                .loadsNeedingAction((int) loadsNeedingAction)
                .total(total)
                .build();
    }

    private NotificationCountResponse getDealerCount(User user) {
        var dealerId = user.getDealer().getId();
        long pendingBids = bidRepo.countByDealerIdAndStatus(dealerId, BidStatus.PENDING);
        long loadsNeedingAction = loadRepo.countByDealerIdAndStatusIn(dealerId,
                List.of(LoadPosting.LoadStatus.PICKED_UP, LoadPosting.LoadStatus.DELIVERED));
        int total = (int) (pendingBids + loadsNeedingAction);
        return NotificationCountResponse.builder()
                .pendingBids((int) pendingBids)
                .loadsNeedingAction((int) loadsNeedingAction)
                .total(total)
                .build();
    }

    private NotificationCountResponse getCarrierCount(User user) {
        var carrierId = user.getCarrier().getId();
        long newAssignments = loadRepo.countByAssignedCarrierIdAndStatus(carrierId, LoadPosting.LoadStatus.ASSIGNED);
        int total = (int) newAssignments;
        return NotificationCountResponse.builder()
                .newAssignments(total)
                .total(total)
                .build();
    }
}
