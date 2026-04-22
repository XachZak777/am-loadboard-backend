package am.loadboardbackend.service;

import am.loadboardbackend.dto.load.CreateLoadRequest;
import am.loadboardbackend.dto.load.LoadPostingDto;
import am.loadboardbackend.dto.load.CarrierBidWithLoadDto;
import am.loadboardbackend.model.*;
import am.loadboardbackend.repository.LoadPostingRepository;
import am.loadboardbackend.repository.CarrierRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import am.loadboardbackend.repository.BidRepository;
import am.loadboardbackend.dto.load.CreateBidRequest;
import am.loadboardbackend.dto.load.BidResponse;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoadPostingService {

    private final LoadPostingRepository loadRepo;
    private final CarrierRepository carrierRepo;
    private final AuthService authService;
    private final BidRepository bidRepo;

    public LoadPostingService(LoadPostingRepository loadRepo, CarrierRepository carrierRepo, AuthService authService, BidRepository bidRepo) {
        this.loadRepo = loadRepo;
        this.carrierRepo = carrierRepo;
        this.authService = authService;
        this.bidRepo = bidRepo;
    }

    public LoadPostingDto createLoad(CreateLoadRequest req) {
        User current = authService.currentUserOrThrow();
        if (current.getRole() == null || current.getBroker() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers can post loads");
        }
        Broker broker = current.getBroker();

        LoadPosting load = new LoadPosting();
        // set owner broker
        load.setBroker(broker);

        load.setPickupType(req.getPickupType());
        load.setDropType(req.getDropType());

        am.loadboardbackend.model.LoadAddress pickup = new am.loadboardbackend.model.LoadAddress();
        pickup.setStreet(req.getPickupStreet());
        pickup.setCity(req.getPickupCity());
        pickup.setState(req.getPickupState());
        pickup.setZip(req.getPickupZip());
        pickup.setCountry(req.getPickupCountry());
        pickup.setLotNumber(req.getPickupLotNumber());
        load.setPickupAddress(pickup);

        am.loadboardbackend.model.LoadAddress drop = new am.loadboardbackend.model.LoadAddress();
        drop.setStreet(req.getDropStreet());
        drop.setCity(req.getDropCity());
        drop.setState(req.getDropState());
        drop.setZip(req.getDropZip());
        drop.setCountry(req.getDropCountry());
        drop.setLotNumber(req.getDropLotNumber());
        load.setDropAddress(drop);

        am.loadboardbackend.model.VehicleInfo vehicle = new am.loadboardbackend.model.VehicleInfo();
        vehicle.setMake(req.getVehicleMake());
        vehicle.setModel(req.getVehicleModel());
        vehicle.setYear(req.getVehicleYear());
        load.setVehicle(vehicle);
        load.setDescription(req.getDescription());
        load.setWeight(req.getWeight());
        load.setPrice(req.getPrice());
        load.setPickupDate(req.getPickupDate());
        load.setDeliveryDate(req.getDeliveryDate());

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

        load.setPickupType(req.getPickupType());
        load.setDropType(req.getDropType());

        am.loadboardbackend.model.LoadAddress pickup2 = load.getPickupAddress() == null ? new am.loadboardbackend.model.LoadAddress() : load.getPickupAddress();
        pickup2.setStreet(req.getPickupStreet());
        pickup2.setCity(req.getPickupCity());
        pickup2.setState(req.getPickupState());
        pickup2.setZip(req.getPickupZip());
        pickup2.setCountry(req.getPickupCountry());
        pickup2.setLotNumber(req.getPickupLotNumber());
        load.setPickupAddress(pickup2);

        am.loadboardbackend.model.LoadAddress drop2 = load.getDropAddress() == null ? new am.loadboardbackend.model.LoadAddress() : load.getDropAddress();
        drop2.setStreet(req.getDropStreet());
        drop2.setCity(req.getDropCity());
        drop2.setState(req.getDropState());
        drop2.setZip(req.getDropZip());
        drop2.setCountry(req.getDropCountry());
        drop2.setLotNumber(req.getDropLotNumber());
        load.setDropAddress(drop2);

        am.loadboardbackend.model.VehicleInfo v2 = load.getVehicle() == null ? new am.loadboardbackend.model.VehicleInfo() : load.getVehicle();
        v2.setMake(req.getVehicleMake());
        v2.setModel(req.getVehicleModel());
        v2.setYear(req.getVehicleYear());
        load.setVehicle(v2);
        load.setDescription(req.getDescription());
        load.setWeight(req.getWeight());
        load.setPrice(req.getPrice());
        load.setPickupDate(req.getPickupDate());
        load.setDeliveryDate(req.getDeliveryDate());

        LoadPosting saved = loadRepo.save(load);
        return toDto(saved);
    }

    @Transactional
    public void deleteLoad(UUID id) {
        User current = authService.currentUserOrThrow();
        if (current.getBroker() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers can delete loads");
        }
        LoadPosting load = loadRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found"));
        if (!load.getBroker().getId().equals(current.getBroker().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own loads");
        }
        // Remove all bids first to avoid FK constraint violations
        bidRepo.deleteAllByLoadId(id);
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
        return loadRepo.findAll().stream().filter(p -> p.getStatus() == null || p.getStatus().name().equals("OPEN")).map(this::toDto).collect(Collectors.toList());
    }

    public List<LoadPostingDto> listAllPublic() {
        return loadRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<LoadPostingDto> listMyBrokerLoads() {
        User current = authService.currentUserOrThrow();
        if (current.getBroker() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers can access this endpoint");
        }
        return loadRepo.findAllByBrokerId(current.getBroker().getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Returns all bids placed by the currently authenticated carrier,
     * with embedded load info so the frontend needs only one request.
     */
    public List<CarrierBidWithLoadDto> getMyCarrierBids() {
        User current = authService.currentUserOrThrow();
        if (current.getCarrier() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Only carriers can access their bids");
        }
        return bidRepo.findAllByCarrierId(current.getCarrier().getId()).stream()
                .map(bid -> {
                    LoadPosting load = bid.getLoad();
                    return new CarrierBidWithLoadDto(
                            bid.getId(),
                            load.getId(),
                            bid.getAmount(),
                            bid.isBookNow(),
                            bid.getStatus().name(),
                            bid.getCreatedAt(),
                            bid.getUpdatedAt(),
                            load.getVehicle() != null ? load.getVehicle().getMake() : null,
                            load.getVehicle() != null ? load.getVehicle().getModel() : null,
                            load.getVehicle() != null ? load.getVehicle().getYear() : null,
                            load.getPickupAddress() != null ? load.getPickupAddress().getCity() : null,
                            load.getPickupAddress() != null ? load.getPickupAddress().getState() : null,
                            load.getDropAddress() != null ? load.getDropAddress().getCity() : null,
                            load.getDropAddress() != null ? load.getDropAddress().getState() : null,
                            load.getPrice(),
                            load.getCreatedAt(),
                            load.getPickupDate(),
                            load.getDeliveryDate(),
                            load.getStatus() != null ? load.getStatus().name() : null,
                            load.getBroker() != null ? load.getBroker().getId() : null
                    );
                })
                .collect(Collectors.toList());
    }

    private LoadPostingDto toDto(LoadPosting p) {
        LoadPostingDto dto = new LoadPostingDto();
        dto.setId(p.getId());
        dto.setPickupType(p.getPickupType() != null ? p.getPickupType().name() : null);
        dto.setDropType(p.getDropType() != null ? p.getDropType().name() : null);
        if (p.getPickupAddress() != null) {
            dto.setPickupStreet(p.getPickupAddress().getStreet());
            dto.setPickupCity(p.getPickupAddress().getCity());
            dto.setPickupState(p.getPickupAddress().getState());
            dto.setPickupZip(p.getPickupAddress().getZip());
            dto.setPickupCountry(p.getPickupAddress().getCountry());
            dto.setPickupLotNumber(p.getPickupAddress().getLotNumber());
        }
        if (p.getDropAddress() != null) {
            dto.setDropStreet(p.getDropAddress().getStreet());
            dto.setDropCity(p.getDropAddress().getCity());
            dto.setDropState(p.getDropAddress().getState());
            dto.setDropZip(p.getDropAddress().getZip());
            dto.setDropCountry(p.getDropAddress().getCountry());
            dto.setDropLotNumber(p.getDropAddress().getLotNumber());
        }
        if (p.getVehicle() != null) {
            dto.setVehicleMake(p.getVehicle().getMake());
            dto.setVehicleModel(p.getVehicle().getModel());
            dto.setVehicleYear(p.getVehicle().getYear());
        }
        dto.setDescription(p.getDescription());
        dto.setWeight(p.getWeight());
        dto.setPrice(p.getPrice());
        dto.setPickupDate(p.getPickupDate());
        dto.setDeliveryDate(p.getDeliveryDate());
        dto.setCreatedAt(p.getCreatedAt());
        if (p.getBroker() != null) dto.setBrokerId(p.getBroker().getId());
        if (p.getAssignedCarrier() != null) dto.setAssignedCarrierId(p.getAssignedCarrier().getId());
        dto.setStatus(p.getStatus() != null ? p.getStatus().name() : null);
        return dto;
    }

    // Bidding / booking operations
    public BidResponse placeBid(CreateBidRequest req) {
        User current = authService.currentUserOrThrow();
        if (current.getCarrier() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Only carriers can bid");
        }
        Carrier carrier = current.getCarrier();

        LoadPosting load = loadRepo.findById(req.loadId()).orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Load not found"));
        if (load.getStatus() != null && load.getStatus().name().equals("ASSIGNED")) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Load already assigned");
        }

        Bid bid = new Bid();
        bid.setLoad(load);
        bid.setCarrier(carrier);
        bid.setAmount(req.amount());
        bid.setBookNow(req.bookNow());
        bid.setStatus(BidStatus.PENDING);

        Bid saved = bidRepo.save(bid);

        // Do not auto-assign on bookNow — broker must approve. Keep bid record for broker review.
        return toBidResponse(saved);
    }

    public List<BidResponse> listBids(UUID loadId) {
        // only broker who owns loads or admin can list — for simplicity, allow any authenticated broker
        User current = authService.currentUserOrThrow();
        if (current.getBroker() == null) throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Only brokers can view bids");
    return bidRepo.findAllByLoadId(loadId).stream().map(this::toBidResponse).collect(Collectors.toList());
    }

    public void approveBid(UUID bidId) {
        User current = authService.currentUserOrThrow();
        if (current.getBroker() == null) throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Only brokers can approve bids");
        Bid bid = bidRepo.findById(bidId).orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Bid not found"));
        LoadPosting load = bid.getLoad();
        // assign
        load.setAssignedCarrier(bid.getCarrier());
        load.setStatus(LoadPosting.LoadStatus.ASSIGNED);
        loadRepo.save(load);

        // mark this bid approved
        bid.setStatus(BidStatus.APPROVED);
        bidRepo.save(bid);

        // mark other bids for the same load as REJECTED
        bidRepo.findAllByLoadId(load.getId()).stream()
                .filter(b -> !b.getId().equals(bid.getId()))
                .forEach(other -> {
                    other.setStatus(BidStatus.REJECTED);
                    bidRepo.save(other);
                });
    }

    public void cancelBooking(UUID loadId) {
        User current = authService.currentUserOrThrow();
        if (current.getBroker() == null) throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Only brokers can cancel bookings");
        LoadPosting load = loadRepo.findById(loadId).orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Load not found"));
        // unassign and mark open
        // mark any approved bids as cancelled and reopen the load
        bidRepo.findAllByLoadId(load.getId()).stream()
                .filter(b -> b.getStatus() == BidStatus.APPROVED)
                .forEach(approved -> {
                    approved.setStatus(BidStatus.CANCELLED);
                    bidRepo.save(approved);
                });

        load.setAssignedCarrier(null);
        load.setStatus(LoadPosting.LoadStatus.OPEN);
        loadRepo.save(load);
    }

    private BidResponse toBidResponse(Bid b) {
        return new BidResponse(b.getId(), b.getLoad().getId(), b.getCarrier().getId(), b.getAmount(), b.isBookNow(), b.getStatus().name(), b.getCreatedAt(), b.getUpdatedAt());
    }
}
