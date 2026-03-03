package am.loadboardbackend.service;

import am.loadboardbackend.dto.CreateLoadRequest;
import am.loadboardbackend.dto.LoadPostingDto;
import am.loadboardbackend.model.Broker;
import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.model.LoadPosting;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.LoadPostingRepository;
import am.loadboardbackend.repository.BrokerRepository;
import am.loadboardbackend.repository.CarrierRepository;
import am.loadboardbackend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LoadPostingService {

    private final LoadPostingRepository loadRepo;
    private final BrokerRepository brokerRepo;
    private final CarrierRepository carrierRepo;
    private final UserRepository userRepo;
    private final AuthService authService;

    public LoadPostingService(LoadPostingRepository loadRepo, BrokerRepository brokerRepo, CarrierRepository carrierRepo, UserRepository userRepo, AuthService authService) {
        this.loadRepo = loadRepo;
        this.brokerRepo = brokerRepo;
        this.carrierRepo = carrierRepo;
        this.userRepo = userRepo;
        this.authService = authService;
    }

    public LoadPostingDto createLoad(CreateLoadRequest req) {
        User current = authService.currentUserOrThrow();
        if (current.getRole() == null || current.getBroker() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers can post loads");
        }
        Broker broker = current.getBroker();

        LoadPosting load = new LoadPosting();
        // For now we link load to broker's preferred carrier if exists; to keep model simple we'll link to broker as carrier reference not required
        // Instead link to broker's broker id by creating a carrier placeholder - but better to require carrier selection. Simpler: use broker's mcNumber to find a carrier and link if exists
        Optional<Carrier> maybeCarrier = carrierRepo.findByMcNumber(broker.getMcNumber());
        maybeCarrier.ifPresent(load::setCarrier);

        load.setPickupCity(req.getPickupCity());
        load.setPickupState(req.getPickupState());
        load.setDeliveryCity(req.getDeliveryCity());
        load.setDeliveryState(req.getDeliveryState());
        load.setDescription(req.getDescription());
        load.setWeight(req.getWeight());
        load.setPrice(req.getPrice());

        LoadPosting saved = loadRepo.save(load);
        return toDto(saved);
    }

    public LoadPostingDto updateLoad(UUID id, CreateLoadRequest req) {
        User current = authService.currentUserOrThrow();
        LoadPosting load = loadRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found"));
        // ownership: only broker who posted (we don't store broker on model) -> skip strict ownership for now
        if (current.getBroker() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers can edit loads");
        }

        load.setPickupCity(req.getPickupCity());
        load.setPickupState(req.getPickupState());
        load.setDeliveryCity(req.getDeliveryCity());
        load.setDeliveryState(req.getDeliveryState());
        load.setDescription(req.getDescription());
        load.setWeight(req.getWeight());
        load.setPrice(req.getPrice());

        LoadPosting saved = loadRepo.save(load);
        return toDto(saved);
    }

    public void deleteLoad(UUID id) {
        User current = authService.currentUserOrThrow();
        if (current.getBroker() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers can delete loads");
        }
        LoadPosting load = loadRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found"));
        loadRepo.delete(load);
    }

    public List<LoadPostingDto> listAllForCarrier(UUID carrierId) {
        Carrier carrier = carrierRepo.findById(carrierId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrier not found"));
        if (carrier.getSubscriptionActive() == null || !carrier.getSubscriptionActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Carrier subscription not active");
        }
        return loadRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<LoadPostingDto> publicListAllForCarrier(UUID carrierId) {
        // utility to check without exception
        Carrier carrier = carrierRepo.findById(carrierId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrier not found"));
        if (carrier.getSubscriptionActive() == null || !carrier.getSubscriptionActive()) {
            return List.of();
        }
        return loadRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<LoadPostingDto> listAllPublic() {
        return loadRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    private LoadPostingDto toDto(LoadPosting p) {
        LoadPostingDto dto = new LoadPostingDto();
        dto.setId(p.getId());
        dto.setPickupCity(p.getPickupCity());
        dto.setPickupState(p.getPickupState());
        dto.setDeliveryCity(p.getDeliveryCity());
        dto.setDeliveryState(p.getDeliveryState());
        dto.setDescription(p.getDescription());
        dto.setWeight(p.getWeight());
        dto.setPrice(p.getPrice());
        dto.setCreatedAt(p.getCreatedAt());
        if (p.getCarrier() != null) dto.setCarrierId(p.getCarrier().getId());
        return dto;
    }
}
