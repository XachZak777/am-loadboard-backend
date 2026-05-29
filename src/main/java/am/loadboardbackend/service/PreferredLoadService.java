package am.loadboardbackend.service;

import am.loadboardbackend.dto.load.LoadPostingDto;
import am.loadboardbackend.model.CarrierPreferredLoad;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.CarrierPreferredLoadRepository;
import am.loadboardbackend.repository.LoadPostingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PreferredLoadService {

    private final CarrierPreferredLoadRepository preferredRepo;
    private final LoadPostingRepository loadRepo;
    private final LoadPostingService loadPostingService;

    @Transactional
    public void add(User user, UUID loadId) {
        UUID carrierId = requireCarrierId(user);
        if (preferredRepo.existsByCarrierIdAndLoadId(carrierId, loadId)) return;
        if (!loadRepo.existsById(loadId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found");
        }
        CarrierPreferredLoad entry = new CarrierPreferredLoad();
        entry.setCarrierId(carrierId);
        entry.setLoadId(loadId);
        preferredRepo.save(entry);
    }

    @Transactional
    public void remove(User user, UUID loadId) {
        UUID carrierId = requireCarrierId(user);
        preferredRepo.deleteByCarrierIdAndLoadId(carrierId, loadId);
    }

    public List<LoadPostingDto> list(User user) {
        UUID carrierId = requireCarrierId(user);
        return preferredRepo.findAllByCarrierIdOrderBySavedAtDesc(carrierId).stream()
                .map(entry -> {
                    try { return loadPostingService.getLoad(entry.getLoadId()); }
                    catch (Exception e) { return null; }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public List<UUID> savedLoadIds(User user) {
        UUID carrierId = requireCarrierId(user);
        return preferredRepo.findAllByCarrierIdOrderBySavedAtDesc(carrierId).stream()
                .map(CarrierPreferredLoad::getLoadId)
                .toList();
    }

    private UUID requireCarrierId(User user) {
        if (user.getCarrier() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Carrier profile required");
        }
        return user.getCarrier().getId();
    }
}
