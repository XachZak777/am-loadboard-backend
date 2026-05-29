package am.loadboardbackend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationCountResponse {
    private int pendingBids;
    private int loadsNeedingAction;
    private int newAssignments;
    private int total;
}
