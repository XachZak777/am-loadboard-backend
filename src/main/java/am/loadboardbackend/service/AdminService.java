package am.loadboardbackend.service;

import am.loadboardbackend.dto.load.LoadPostingDto;
import am.loadboardbackend.model.*;
import am.loadboardbackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final CarrierRepository carrierRepository;
    private final BrokerRepository brokerRepository;
    private final LoadPostingRepository loadPostingRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuthService authService;

    // Carrier management
    public List<Carrier> getAllCarriers() {
        log.info("Admin fetching all carriers");
        return carrierRepository.findAll();
    }

    public Carrier getCarrierById(UUID id) {
        return carrierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrier not found"));
    }

    @Transactional
    public void deleteCarrier(UUID carrierId) {
        User admin = authService.currentUserOrThrow();
        Carrier carrier = getCarrierById(carrierId);

        log.info("Admin {} deleting carrier {}", admin.getEmail(), carrierId);

        // Create audit log
        auditLogRepository.save(AuditLog.builder()
                .user(admin)
                .action("DELETE_CARRIER")
                .entityType("CARRIER")
                .entityId(carrierId)
                .details("Deleted carrier: " + carrier.getLegalName())
                .build());

        carrierRepository.deleteById(carrierId);
    }

    // Broker management
    public List<Broker> getAllBrokers() {
        log.info("Admin fetching all brokers");
        return brokerRepository.findAll();
    }

    public Broker getBrokerById(UUID id) {
        return brokerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Broker not found"));
    }

    @Transactional
    public void deleteBroker(UUID brokerId) {
        User admin = authService.currentUserOrThrow();
        Broker broker = getBrokerById(brokerId);

        log.info("Admin {} deleting broker {}", admin.getEmail(), brokerId);

        // Create audit log
        auditLogRepository.save(AuditLog.builder()
                .user(admin)
                .action("DELETE_BROKER")
                .entityType("BROKER")
                .entityId(brokerId)
                .details("Deleted broker: " + broker.getLegalName())
                .build());

        brokerRepository.deleteById(brokerId);
    }

    // User history
    public List<AuditLog> getUserHistory(UUID userId) {
        log.info("Admin fetching history for user {}", userId);
        return auditLogRepository.findByUserId(userId);
    }

    public List<AuditLog> getEntityHistory(UUID entityId) {
        log.info("Admin fetching history for entity {}", entityId);
        return auditLogRepository.findByEntityIdOrderByCreatedAtDesc(entityId);
    }

    // Load management
    public List<LoadPosting> getAllLoads() {
        log.info("Admin fetching all loads");
        return loadPostingRepository.findAll();
    }

    public LoadPosting getLoadById(UUID id) {
        return loadPostingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found"));
    }

    @Transactional
    public LoadPosting createLoad(UUID brokerId, LoadPostingDto dto) {
        User admin = authService.currentUserOrThrow();
        Broker broker = brokerRepository.findById(brokerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Broker not found"));

        log.info("Admin {} creating load for broker {}", admin.getEmail(), brokerId);

        // Create load for specified broker
        LoadPosting load = new LoadPosting();
        load.setBroker(broker);
        load.setStatus(LoadPosting.LoadStatus.OPEN);
        load.setDescription(dto.getDescription());
        load.setWeight(dto.getWeight());
        load.setPrice(dto.getPrice());

        if (dto.getPickupType() != null) {
            load.setPickupType(PickupType.valueOf(dto.getPickupType()));
        }
        if (dto.getDropType() != null) {
            load.setDropType(DropType.valueOf(dto.getDropType()));
        }

    // Create pickup address
    LoadAddress pickupAddress = new LoadAddress();
        pickupAddress.setStreet(dto.getPickupStreet());
        pickupAddress.setCity(dto.getPickupCity());
        pickupAddress.setState(dto.getPickupState());
        pickupAddress.setZip(dto.getPickupZip());
        pickupAddress.setCountry(dto.getPickupCountry());
    pickupAddress.setLotNumber(dto.getPickupLotNumber());
        load.setPickupAddress(pickupAddress);

    // Create drop address
    LoadAddress dropAddress = new LoadAddress();
    dropAddress.setStreet(dto.getDropStreet());
    dropAddress.setCity(dto.getDropCity());
    dropAddress.setState(dto.getDropState());
    dropAddress.setZip(dto.getDropZip());
    dropAddress.setCountry(dto.getDropCountry());
    dropAddress.setLotNumber(dto.getDropLotNumber());
    load.setDropAddress(dropAddress);

    VehicleInfo vehicle = new VehicleInfo();
    vehicle.setMake(dto.getVehicleMake());
    vehicle.setModel(dto.getVehicleModel());
    vehicle.setYear(dto.getVehicleYear());
    load.setVehicle(vehicle);

        LoadPosting saved = loadPostingRepository.save(load);

        auditLogRepository.save(AuditLog.builder()
                .user(admin)
                .action("CREATE_LOAD")
                .entityType("LOAD")
                .entityId(saved.getId())
                .details("Created load from admin panel for broker: " + broker.getLegalName())
                .build());

        return saved;
    }

    @Transactional
    public LoadPosting updateLoad(UUID loadId, LoadPostingDto dto) {
        User admin = authService.currentUserOrThrow();
        LoadPosting load = getLoadById(loadId);

        log.info("Admin {} updating load {}", admin.getEmail(), loadId);

        // Update basic fields
        load.setDescription(dto.getDescription());
        load.setWeight(dto.getWeight());
        load.setPrice(dto.getPrice());

        if (dto.getPickupType() != null) {
            load.setPickupType(PickupType.valueOf(dto.getPickupType()));
        }
        if (dto.getDropType() != null) {
            load.setDropType(DropType.valueOf(dto.getDropType()));
        }

        // Update pickup address
        if (load.getPickupAddress() == null) {
            load.setPickupAddress(new LoadAddress());
        }
        load.getPickupAddress().setStreet(dto.getPickupStreet());
        load.getPickupAddress().setCity(dto.getPickupCity());
        load.getPickupAddress().setState(dto.getPickupState());
        load.getPickupAddress().setZip(dto.getPickupZip());
        load.getPickupAddress().setCountry(dto.getPickupCountry());
        load.getPickupAddress().setLotNumber(dto.getPickupLotNumber());

        // Update drop address
        if (load.getDropAddress() == null) {
            load.setDropAddress(new LoadAddress());
        }
        load.getDropAddress().setStreet(dto.getDropStreet());
        load.getDropAddress().setCity(dto.getDropCity());
        load.getDropAddress().setState(dto.getDropState());
        load.getDropAddress().setZip(dto.getDropZip());
        load.getDropAddress().setCountry(dto.getDropCountry());
        load.getDropAddress().setLotNumber(dto.getDropLotNumber());

        if (load.getVehicle() == null) {
            load.setVehicle(new VehicleInfo());
        }
        load.getVehicle().setMake(dto.getVehicleMake());
        load.getVehicle().setModel(dto.getVehicleModel());
        load.getVehicle().setYear(dto.getVehicleYear());

        LoadPosting updated = loadPostingRepository.save(load);

        auditLogRepository.save(AuditLog.builder()
                .user(admin)
                .action("UPDATE_LOAD")
                .entityType("LOAD")
                .entityId(loadId)
                .details("Updated load details")
                .build());

        return updated;
    }

    @Transactional
    public void deleteLoad(UUID loadId) {
        User admin = authService.currentUserOrThrow();
        getLoadById(loadId); // Verify load exists before deleting

        log.info("Admin {} deleting load {}", admin.getEmail(), loadId);

        auditLogRepository.save(AuditLog.builder()
                .user(admin)
                .action("DELETE_LOAD")
                .entityType("LOAD")
                .entityId(loadId)
                .details("Deleted load")
                .build());

        loadPostingRepository.deleteById(loadId);
    }

    // Audit log
    public void logAdminAction(String action, String entityType, UUID entityId, String details) {
        try {
            User admin = authService.currentUserOrThrow();
            auditLogRepository.save(AuditLog.builder()
                    .user(admin)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .build());
        } catch (Exception e) {
            log.warn("Could not log admin action: {}", e.getMessage());
        }
    }
}
