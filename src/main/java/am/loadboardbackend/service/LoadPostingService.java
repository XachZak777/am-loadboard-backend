package am.loadboardbackend.service;

import am.loadboardbackend.config.AppProperties;
import am.loadboardbackend.dto.load.AdditionalVehicleRequest;
import am.loadboardbackend.dto.load.BidResponse;
import am.loadboardbackend.dto.load.CarrierBidWithLoadDto;
import am.loadboardbackend.dto.load.CreateBidRequest;
import am.loadboardbackend.dto.load.CreateLoadRequest;
import am.loadboardbackend.dto.load.LoadPostingDto;
import am.loadboardbackend.dto.load.UpdateBidRequest;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import am.loadboardbackend.mailing.AssignmentRejectedBrokerEmailContext;
import am.loadboardbackend.mailing.BidApprovedEmailContext;
import am.loadboardbackend.mailing.BidPlacedEmailContext;
import am.loadboardbackend.mailing.BidRejectedEmailContext;
import am.loadboardbackend.mailing.LoadStatusUpdateEmailContext;
import am.loadboardbackend.mailing.NewLoadAlertEmailContext;
import am.loadboardbackend.mailing.PaymentConfirmedEmailContext;
import am.loadboardbackend.model.*;
import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.repository.LoadPostingRepository;
import am.loadboardbackend.repository.CarrierRepository;
import am.loadboardbackend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import am.loadboardbackend.repository.BidRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class LoadPostingService {

    private final LoadPostingRepository loadRepo;
    private final CarrierRepository carrierRepo;
    private final AuthService authService;
    private final BidRepository bidRepo;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AppProperties appProperties;
    private final DistanceCalculatorService distanceCalculator;
    private final ObjectMapper objectMapper;

    public LoadPostingService(
            LoadPostingRepository loadRepo,
            CarrierRepository carrierRepo,
            AuthService authService,
            BidRepository bidRepo,
            UserRepository userRepository,
            EmailService emailService,
            AppProperties appProperties,
            DistanceCalculatorService distanceCalculator,
            ObjectMapper objectMapper
    ) {
        this.loadRepo = loadRepo;
        this.carrierRepo = carrierRepo;
        this.authService = authService;
        this.bidRepo = bidRepo;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.appProperties = appProperties;
        this.distanceCalculator = distanceCalculator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LoadPostingDto createLoad(CreateLoadRequest req) {
        User current = authService.currentUserOrThrow();
        if (current.getBroker() == null && current.getDealer() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers and dealers can post loads");
        }

        LoadPosting load = new LoadPosting();
        if (current.getBroker() != null) {
            load.setBroker(current.getBroker());
        } else {
            load.setDealer(current.getDealer());
        }
        load.setPickupType(req.getPickupType());
        load.setDropType(req.getDropType());

        LoadAddress pickup = new LoadAddress();
        pickup.setStreet(req.getPickupStreet());
        pickup.setCity(req.getPickupCity());
        pickup.setState(req.getPickupState());
        pickup.setZip(req.getPickupZip());
        pickup.setCountry(req.getPickupCountry());
        pickup.setLotNumber(req.getPickupLotNumber());
        pickup.setContactName(req.getPickupContactName());
        pickup.setContactPhone(req.getPickupContactPhone());
        load.setPickupAddress(pickup);

        am.loadboardbackend.model.LoadAddress drop = new am.loadboardbackend.model.LoadAddress();
        drop.setStreet(req.getDropStreet());
        drop.setCity(req.getDropCity());
        drop.setState(req.getDropState());
        drop.setZip(req.getDropZip());
        drop.setCountry(req.getDropCountry());
        drop.setLotNumber(req.getDropLotNumber());
        drop.setContactName(req.getDropContactName());
        drop.setContactPhone(req.getDropContactPhone());
        load.setDropAddress(drop);

        am.loadboardbackend.model.VehicleInfo vehicle = new am.loadboardbackend.model.VehicleInfo();
        vehicle.setMake(req.getVehicleMake());
        vehicle.setModel(req.getVehicleModel());
        vehicle.setYear(req.getVehicleYear());
        vehicle.setVehicleType(req.getVehicleType());
        vehicle.setCondition(req.getVehicleCondition());
        vehicle.setVin(req.getVin());
        vehicle.setTrailerType(req.getTrailerType());
        vehicle.setAdditionalInfo(req.getVehicleAdditionalInfo());
        load.setVehicle(vehicle);
        load.setDescription(req.getDescription());
        load.setPaymentNotes(req.getPaymentNotes());
        load.setWeight(req.getWeight());
        load.setPrice(req.getPrice());
        load.setDistance(distanceCalculator.calculateDistance(
                req.getPickupCity(), req.getPickupState(), req.getPickupZip(),
                req.getDropCity(),   req.getDropState(),   req.getDropZip()));
        load.setPickupDate(req.getPickupDate());
        load.setPickupTime(req.getPickupTime());
        load.setDeliveryDate(req.getDeliveryDate());
        load.setDeliveryTime(req.getDeliveryTime());
        load.setContactName(req.getContactName());
        load.setContactPhone(req.getContactPhone());
        load.setContactEmail(req.getContactEmail());
        load.setOrderId(req.getOrderId());
        load.setPaymentMethod(req.getPaymentMethod());
        load.setPaymentTiming(req.getPaymentTiming());
        load.setAdditionalVehicles(serializeAdditionalVehicles(req.getAdditionalVehicles()));

        LoadPosting saved = loadRepo.save(load);
        notifyMatchingCarriers(saved);
        return toDto(saved);
    }

    @Transactional
    public LoadPostingDto updateLoad(UUID id, CreateLoadRequest req) {
        User current = authService.currentUserOrThrow();
        LoadPosting load = loadRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found"));
        if (current.getBroker() == null && current.getDealer() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers and dealers can edit loads");
        }
        boolean ownedByBroker = current.getBroker() != null && load.getBroker() != null
                && load.getBroker().getId().equals(current.getBroker().getId());
        boolean ownedByDealer = current.getDealer() != null && load.getDealer() != null
                && load.getDealer().getId().equals(current.getDealer().getId());
        if (!ownedByBroker && !ownedByDealer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own loads");
        }

        load.setPickupType(req.getPickupType());
        load.setDropType(req.getDropType());

        am.loadboardbackend.model.LoadAddress pickup2 = load.getPickupAddress() == null
                ? new am.loadboardbackend.model.LoadAddress() : load.getPickupAddress();
        pickup2.setStreet(req.getPickupStreet());
        pickup2.setCity(req.getPickupCity());
        pickup2.setState(req.getPickupState());
        pickup2.setZip(req.getPickupZip());
        pickup2.setCountry(req.getPickupCountry());
        pickup2.setLotNumber(req.getPickupLotNumber());
        pickup2.setContactName(req.getPickupContactName());
        pickup2.setContactPhone(req.getPickupContactPhone());
        load.setPickupAddress(pickup2);

        am.loadboardbackend.model.LoadAddress drop2 = load.getDropAddress() == null
                ? new am.loadboardbackend.model.LoadAddress() : load.getDropAddress();
        drop2.setStreet(req.getDropStreet());
        drop2.setCity(req.getDropCity());
        drop2.setState(req.getDropState());
        drop2.setZip(req.getDropZip());
        drop2.setCountry(req.getDropCountry());
        drop2.setLotNumber(req.getDropLotNumber());
        drop2.setContactName(req.getDropContactName());
        drop2.setContactPhone(req.getDropContactPhone());
        load.setDropAddress(drop2);

        am.loadboardbackend.model.VehicleInfo v2 = load.getVehicle() == null
                ? new am.loadboardbackend.model.VehicleInfo() : load.getVehicle();
        v2.setMake(req.getVehicleMake());
        v2.setModel(req.getVehicleModel());
        v2.setYear(req.getVehicleYear());
        v2.setVehicleType(req.getVehicleType());
        v2.setCondition(req.getVehicleCondition());
        v2.setVin(req.getVin());
        v2.setTrailerType(req.getTrailerType());
        v2.setAdditionalInfo(req.getVehicleAdditionalInfo());
        load.setVehicle(v2);
        load.setDescription(req.getDescription());
        load.setPaymentNotes(req.getPaymentNotes());
        load.setWeight(req.getWeight());
        load.setPrice(req.getPrice());
        load.setDistance(distanceCalculator.calculateDistance(
                req.getPickupCity(), req.getPickupState(), req.getPickupZip(),
                req.getDropCity(),   req.getDropState(),   req.getDropZip()));
        load.setPickupDate(req.getPickupDate());
        load.setPickupTime(req.getPickupTime());
        load.setDeliveryDate(req.getDeliveryDate());
        load.setDeliveryTime(req.getDeliveryTime());
        load.setContactName(req.getContactName());
        load.setContactPhone(req.getContactPhone());
        load.setContactEmail(req.getContactEmail());
        load.setOrderId(req.getOrderId());
        load.setPaymentMethod(req.getPaymentMethod());
        load.setPaymentTiming(req.getPaymentTiming());
        load.setAdditionalVehicles(serializeAdditionalVehicles(req.getAdditionalVehicles()));

        LoadPosting saved = loadRepo.save(load);
        return toDto(saved);
    }

    @Transactional
    public void deleteLoad(UUID id) {
        User current = authService.currentUserOrThrow();
        if (current.getBroker() == null && current.getDealer() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers and dealers can delete loads");
        }
        LoadPosting load = loadRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found"));
        boolean ownedByBroker = current.getBroker() != null && load.getBroker() != null
                && load.getBroker().getId().equals(current.getBroker().getId());
        boolean ownedByDealer = current.getDealer() != null && load.getDealer() != null
                && load.getDealer().getId().equals(current.getDealer().getId());
        if (!ownedByBroker && !ownedByDealer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own loads");
        }
        bidRepo.deleteAllByLoadId(id);
        loadRepo.delete(load);
    }

    public List<LoadPostingDto> listAllForCarrier(UUID carrierId) {
        carrierRepo.findById(carrierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrier not found"));
        return loadRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<LoadPostingDto> publicListAllForCarrier(UUID carrierId) {
        carrierRepo.findById(carrierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrier not found"));
        return loadRepo.findAll().stream()
                .filter(p -> p.getStatus() == null || p.getStatus().name().equals("OPEN"))
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<LoadPostingDto> listAllPublic() {
        return loadRepo.findAll().stream()
                .filter(p -> p.getStatus() == null || p.getStatus() == LoadPosting.LoadStatus.OPEN)
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<LoadPostingDto> listMyBrokerLoads() {
        User current = authService.currentUserOrThrow();
        if (current.getBroker() != null) {
            return loadRepo.findAllByBrokerId(current.getBroker().getId())
                    .stream().map(this::toDto).collect(Collectors.toList());
        }
        if (current.getDealer() != null) {
            return loadRepo.findAllByDealerId(current.getDealer().getId())
                    .stream().map(this::toDto).collect(Collectors.toList());
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers and dealers can access this endpoint");
    }

    public List<CarrierBidWithLoadDto> getMyCarrierBids() {
        User current = authService.currentUserOrThrow();
        if (current.getCarrier() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only carriers can access their bids");
        }
        UUID carrierId = current.getCarrier().getId();

        List<CarrierBidWithLoadDto> fromBids = bidRepo.findAllByCarrierId(carrierId).stream()
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
                            bid.getRequestedPickupDate(),
                            bid.getRequestedPickupTime(),
                            bid.getRequestedDropDate(),
                            bid.getRequestedDropTime(),
                            load.getVehicle() != null ? load.getVehicle().getMake() : null,
                            load.getVehicle() != null ? load.getVehicle().getModel() : null,
                            load.getVehicle() != null ? load.getVehicle().getYear() : null,
                            load.getPickupAddress() != null ? load.getPickupAddress().getCity() : null,
                            load.getPickupAddress() != null ? load.getPickupAddress().getState() : null,
                            load.getPickupAddress() != null ? load.getPickupAddress().getZip() : null,
                            load.getDropAddress() != null ? load.getDropAddress().getCity() : null,
                            load.getDropAddress() != null ? load.getDropAddress().getState() : null,
                            load.getDropAddress() != null ? load.getDropAddress().getZip() : null,
                            load.getPrice(),
                            load.getCreatedAt(),
                            load.getPickupDate(),
                            load.getDeliveryDate(),
                            load.getStatus() != null ? load.getStatus().name() : null,
                            load.getBroker() != null ? load.getBroker().getId() : null,
                            load.getOrderId(),
                            bid.getNotes(),
                            deserializeAdditionalVehicles(load.getAdditionalVehicles())
                    );
                })
                .collect(Collectors.toList());

        // Include loads directly assigned to this carrier (no bid record created for them).
        // Only exclude loads where the carrier has an active (non-rejected/non-cancelled) bid,
        // so that a previously-rejected bid doesn't hide a subsequent direct assignment.
        Set<UUID> bidLoadIds = fromBids.stream()
                .filter(b -> !"REJECTED".equals(b.bidStatus()) && !"CANCELLED".equals(b.bidStatus()))
                .map(CarrierBidWithLoadDto::loadId)
                .collect(Collectors.toSet());
        List<LoadPosting.LoadStatus> activeStatuses = List.of(
                LoadPosting.LoadStatus.ASSIGNED, LoadPosting.LoadStatus.PICKED_UP,
                LoadPosting.LoadStatus.DELIVERED, LoadPosting.LoadStatus.PAID);
        List<CarrierBidWithLoadDto> directAssigned = loadRepo
                .findByAssignedCarrierIdAndStatusIn(carrierId, activeStatuses).stream()
                .filter(load -> !bidLoadIds.contains(load.getId()))
                .map(load -> new CarrierBidWithLoadDto(
                        null,
                        load.getId(),
                        null,
                        false,
                        "APPROVED",
                        load.getCreatedAt(),
                        null,
                        null, null, null, null,
                        load.getVehicle() != null ? load.getVehicle().getMake() : null,
                        load.getVehicle() != null ? load.getVehicle().getModel() : null,
                        load.getVehicle() != null ? load.getVehicle().getYear() : null,
                        load.getPickupAddress() != null ? load.getPickupAddress().getCity() : null,
                        load.getPickupAddress() != null ? load.getPickupAddress().getState() : null,
                        load.getPickupAddress() != null ? load.getPickupAddress().getZip() : null,
                        load.getDropAddress() != null ? load.getDropAddress().getCity() : null,
                        load.getDropAddress() != null ? load.getDropAddress().getState() : null,
                        load.getDropAddress() != null ? load.getDropAddress().getZip() : null,
                        load.getPrice(),
                        load.getCreatedAt(),
                        load.getPickupDate(),
                        load.getDeliveryDate(),
                        load.getStatus() != null ? load.getStatus().name() : null,
                        load.getBroker() != null ? load.getBroker().getId() :
                                (load.getDealer() != null ? load.getDealer().getId() : null),
                        load.getOrderId(),
                        null,
                        deserializeAdditionalVehicles(load.getAdditionalVehicles())
                ))
                .collect(Collectors.toList());

        List<CarrierBidWithLoadDto> combined = new ArrayList<>(fromBids);
        combined.addAll(directAssigned);
        return combined;
    }

    private LoadPostingDto toDto(LoadPosting p) {
        LoadPostingDto dto = new LoadPostingDto();
        dto.setId(p.getId());
        dto.setPickupType(p.getPickupType() != null ? p.getPickupType().name() : null);
        dto.setDropType(p.getDropType() != null ? p.getDropType().name() : null);
        boolean showFullAddress = isAssignedCarrierOrBroker(p);
        if (p.getPickupAddress() != null) {
            dto.setPickupCity(p.getPickupAddress().getCity());
            dto.setPickupState(p.getPickupAddress().getState());
            dto.setPickupZip(p.getPickupAddress().getZip());
            if (showFullAddress) {
                dto.setPickupStreet(p.getPickupAddress().getStreet());
                dto.setPickupCountry(p.getPickupAddress().getCountry());
                dto.setPickupLotNumber(p.getPickupAddress().getLotNumber());
                dto.setPickupContactName(p.getPickupAddress().getContactName());
                dto.setPickupContactPhone(p.getPickupAddress().getContactPhone());
            }
        }
        if (p.getDropAddress() != null) {
            dto.setDropCity(p.getDropAddress().getCity());
            dto.setDropState(p.getDropAddress().getState());
            dto.setDropZip(p.getDropAddress().getZip());
            if (showFullAddress) {
                dto.setDropStreet(p.getDropAddress().getStreet());
                dto.setDropCountry(p.getDropAddress().getCountry());
                dto.setDropLotNumber(p.getDropAddress().getLotNumber());
                dto.setDropContactName(p.getDropAddress().getContactName());
                dto.setDropContactPhone(p.getDropAddress().getContactPhone());
            }
        }
        if (p.getVehicle() != null) {
            dto.setVehicleMake(p.getVehicle().getMake());
            dto.setVehicleModel(p.getVehicle().getModel());
            dto.setVehicleYear(p.getVehicle().getYear());
            dto.setVehicleType(p.getVehicle().getVehicleType());
            dto.setVehicleCondition(p.getVehicle().getCondition());
            dto.setTrailerType(p.getVehicle().getTrailerType());
            if (showFullAddress) dto.setVin(p.getVehicle().getVin());
        }
        dto.setDescription(p.getDescription());
        dto.setPaymentNotes(p.getPaymentNotes());
        dto.setWeight(p.getWeight());
        dto.setPrice(p.getPrice());
        dto.setDistance(p.getDistance());
        dto.setPickupDate(p.getPickupDate());
        dto.setPickupTime(p.getPickupTime());
        dto.setDeliveryDate(p.getDeliveryDate());
        dto.setDeliveryTime(p.getDeliveryTime());
        dto.setCreatedAt(p.getCreatedAt());
        if (p.getBroker() != null) dto.setBrokerId(p.getBroker().getId());
        else if (p.getDealer() != null) dto.setBrokerId(p.getDealer().getId());
        if (p.getAssignedCarrier() != null) dto.setAssignedCarrierId(p.getAssignedCarrier().getId());
        dto.setStatus(p.getStatus() != null ? p.getStatus().name() : null);
        dto.setContactName(p.getContactName());
        dto.setContactPhone(p.getContactPhone());
        dto.setContactEmail(p.getContactEmail());
        dto.setOrderId(p.getOrderId());
        dto.setPaymentMethod(p.getPaymentMethod());
        dto.setPaymentTiming(p.getPaymentTiming());
        var additionalVehicles = deserializeAdditionalVehicles(p.getAdditionalVehicles());
        if (!showFullAddress && additionalVehicles != null) {
            additionalVehicles = additionalVehicles.stream()
                    .map(v -> new AdditionalVehicleRequest(
                            v.vehicleMake(), v.vehicleModel(), v.vehicleYear(),
                            v.vehicleType(), v.vehicleCondition(), null,
                            v.vehicleAdditionalInfo(), v.weight()))
                    .toList();
        }
        dto.setAdditionalVehicles(additionalVehicles);
        return dto;
    }

    private boolean isAssignedCarrierOrBroker(LoadPosting load) {
        return authService.currentUserOptional().map(user -> {
            if (user.getBroker() != null || user.getDealer() != null) return true;
            if (user.getCarrier() != null && load.getAssignedCarrier() != null) {
                return load.getAssignedCarrier().getId().equals(user.getCarrier().getId());
            }
            return false;
        }).orElse(false);
    }

    private String serializeAdditionalVehicles(java.util.List<AdditionalVehicleRequest> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(vehicles);
        } catch (Exception e) {
            log.warn("Failed to serialize additionalVehicles", e);
            return null;
        }
    }

    private java.util.List<AdditionalVehicleRequest> deserializeAdditionalVehicles(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<AdditionalVehicleRequest>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize additionalVehicles", e);
            return null;
        }
    }

    // ── Bidding / booking ─────────────────────────────────────────────────────

    @Transactional
    public BidResponse placeBid(CreateBidRequest req) {
        User current = authService.currentUserOrThrow();
        if (current.getCarrier() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only carriers can bid");
        }
        Carrier carrier = current.getCarrier();

        LoadPosting load = loadRepo.findById(req.loadId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found"));
        if (load.getStatus() != null && load.getStatus().name().equals("ASSIGNED")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Load already assigned");
        }

        Bid bid = new Bid();
        bid.setLoad(load);
        bid.setCarrier(carrier);
        bid.setAmount(req.amount());
        bid.setBookNow(req.bookNow());
        bid.setRequestedPickupDate(req.requestedPickupDate());
        bid.setRequestedPickupTime(req.requestedPickupTime());
        bid.setRequestedDropDate(req.requestedDropDate());
        bid.setRequestedDropTime(req.requestedDropTime());
        bid.setNotes(req.notes());
        bid.setStatus(BidStatus.PENDING);

        Bid saved = bidRepo.save(bid);

        notifyBrokerOfBid(saved, load);

        return toBidResponse(saved);
    }

    @Transactional
    public BidResponse updateBid(UUID bidId, UpdateBidRequest req) {
        User current = authService.currentUserOrThrow();
        if (current.getCarrier() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only carriers can edit bids");
        }
        Bid bid = bidRepo.findById(bidId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bid not found"));
        if (!bid.getCarrier().getId().equals(current.getCarrier().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own bids");
        }
        if (bid.getStatus() != BidStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending bids can be edited");
        }
        bid.setAmount(req.amount());
        bid.setRequestedPickupDate(req.requestedPickupDate());
        bid.setRequestedPickupTime(req.requestedPickupTime());
        bid.setRequestedDropDate(req.requestedDropDate());
        bid.setRequestedDropTime(req.requestedDropTime());
        if (req.notes() != null) bid.setNotes(req.notes());
        return toBidResponse(bidRepo.save(bid));
    }

    public List<BidResponse> listBids(UUID loadId) {
        User current = authService.currentUserOrThrow();
        if (current.getBroker() == null && current.getDealer() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers and dealers can view bids");
        }
        return bidRepo.findAllByLoadId(loadId).stream().map(this::toBidResponse).collect(Collectors.toList());
    }

    @Transactional
    public void approveBid(UUID bidId) {
        User current = authService.currentUserOrThrow();
        if (current.getBroker() == null && current.getDealer() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers and dealers can approve bids");
        }
        Bid bid = bidRepo.findById(bidId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bid not found"));
        LoadPosting load = bid.getLoad();

        load.setAssignedCarrier(bid.getCarrier());
        load.setStatus(LoadPosting.LoadStatus.ASSIGNED);
        loadRepo.save(load);

        bid.setStatus(BidStatus.APPROVED);
        bidRepo.save(bid);

        bidRepo.findAllByLoadId(load.getId()).stream()
                .filter(b -> !b.getId().equals(bid.getId()))
                .forEach(other -> {
                    other.setStatus(BidStatus.REJECTED);
                    bidRepo.save(other);
                    notifyCarrierOfRejection(other, load);
                });

        notifyCarrierOfApproval(bid.getCarrier(), bid, load);
    }

    @Transactional
    public LoadPostingDto directAssignCarrier(UUID loadId, UUID carrierId) {
        User current = authService.currentUserOrThrow();
        if (current.getBroker() == null && current.getDealer() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers and dealers can assign carriers");
        }
        LoadPosting load = loadRepo.findById(loadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found"));
        if (load.getStatus() != null && load.getStatus() != LoadPosting.LoadStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Load must be OPEN to assign a carrier");
        }
        Carrier carrier = carrierRepo.findById(carrierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrier not found"));

        bidRepo.findAllByLoadId(loadId).stream()
                .filter(b -> b.getStatus() == BidStatus.PENDING)
                .forEach(b -> {
                    b.setStatus(BidStatus.REJECTED);
                    bidRepo.save(b);
                    notifyCarrierOfRejection(b, load);
                });

        load.setAssignedCarrier(carrier);
        load.setStatus(LoadPosting.LoadStatus.ASSIGNED);
        loadRepo.save(load);

        notifyCarrierOfApproval(carrier, null, load);

        log.info("Direct assignment: load {} assigned to carrier {}", loadId, carrierId);
        return toDto(load);
    }

    @Transactional
    public LoadPostingDto autoAssignCarrier(UUID loadId) {
        User current = authService.currentUserOrThrow();
        if (current.getBroker() == null && current.getDealer() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers and dealers can auto-assign carriers");
        }
        LoadPosting load = loadRepo.findById(loadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found"));
        if (load.getStatus() != null && load.getStatus() != LoadPosting.LoadStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Load must be OPEN for auto-assignment");
        }
        List<Bid> pending = bidRepo.findAllByLoadId(loadId).stream()
                .filter(b -> b.getStatus() == BidStatus.PENDING)
                .toList();
        if (pending.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending bids available for auto-assignment");
        }
        // Priority: book-now bids first, then lowest amount, then earliest submitted
        Bid best = pending.stream()
                .sorted(Comparator
                        .comparingInt((Bid b) -> b.isBookNow() ? 0 : 1)
                        .thenComparing(Bid::getAmount)
                        .thenComparing(Bid::getCreatedAt))
                .findFirst()
                .orElseThrow();
        log.info("Auto-assigning load {} to carrier {} (bid {})", loadId, best.getCarrier().getId(), best.getId());
        approveBid(best.getId());
        return toDto(loadRepo.findById(loadId).orElseThrow());
    }

    @Transactional
    public void carrierRejectAssignment(UUID loadId) {
        User current = authService.currentUserOrThrow();
        if (current.getCarrier() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only carriers can reject assignments");
        }
        LoadPosting load = loadRepo.findById(loadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found"));
        if (load.getStatus() != LoadPosting.LoadStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Load is not in ASSIGNED status");
        }
        if (load.getAssignedCarrier() == null ||
                !load.getAssignedCarrier().getId().equals(current.getCarrier().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This load is not assigned to you");
        }

        bidRepo.findAllByLoadId(loadId).stream()
                .filter(b -> b.getStatus() == BidStatus.APPROVED || b.getStatus() == BidStatus.CANCELLED)
                .forEach(b -> {
                    b.setStatus(BidStatus.REJECTED);
                    bidRepo.save(b);
                });

        Carrier rejectingCarrier = load.getAssignedCarrier();
        load.setAssignedCarrier(null);
        load.setStatus(LoadPosting.LoadStatus.OPEN);
        loadRepo.save(load);
        log.info("Carrier {} rejected assignment on load {}", current.getCarrier().getId(), loadId);
        notifyBrokerOfCarrierRejection(rejectingCarrier, load);
    }

    @Transactional
    public void cancelBooking(UUID loadId) {
        User current = authService.currentUserOrThrow();
        if (current.getBroker() == null && current.getDealer() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers and dealers can cancel bookings");
        }
        LoadPosting load = loadRepo.findById(loadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found"));

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

    @Transactional
    public void rejectBid(UUID loadId, UUID bidId) {
        User current = authService.currentUserOrThrow();
        if (current.getBroker() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only brokers can reject bids");
        }
        LoadPosting load = loadRepo.findById(loadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found"));
        if (!load.getBroker().getId().equals(current.getBroker().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your load");
        }
        Bid bid = bidRepo.findById(bidId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bid not found"));
        if (bid.getStatus() != BidStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending bids can be rejected");
        }
        bid.setStatus(BidStatus.REJECTED);
        bidRepo.save(bid);
        notifyCarrierOfRejection(bid, load);
    }

    private BidResponse toBidResponse(Bid b) {
        return new BidResponse(
                b.getId(), b.getLoad().getId(), b.getCarrier().getId(),
                b.getAmount(), b.isBookNow(), b.getStatus().name(),
                b.getCreatedAt(), b.getUpdatedAt(),
                b.getRequestedPickupDate(), b.getRequestedPickupTime(),
                b.getRequestedDropDate(), b.getRequestedDropTime(),
                b.getNotes()
        );
    }

    public LoadPostingDto getLoad(UUID id) {
        LoadPosting load = loadRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found"));
        return toDto(load);
    }

    // Covers OPEN too, so legacy/test data where assignedCarrier was set but status
    // was never moved from OPEN can still advance normally.
    private static final java.util.Map<LoadPosting.LoadStatus, LoadPosting.LoadStatus> CARRIER_TRANSITIONS =
            java.util.Map.of(
                    LoadPosting.LoadStatus.OPEN,      LoadPosting.LoadStatus.PICKED_UP,
                    LoadPosting.LoadStatus.ASSIGNED,  LoadPosting.LoadStatus.PICKED_UP,
                    LoadPosting.LoadStatus.PICKED_UP, LoadPosting.LoadStatus.DELIVERED,
                    LoadPosting.LoadStatus.DELIVERED, LoadPosting.LoadStatus.PAID
            );

    @Transactional
    public LoadPostingDto advanceLoadStatus(UUID loadId) {
        User current = authService.currentUserOrThrow();
        if (current.getCarrier() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only carriers can update load status");
        }
        LoadPosting load = loadRepo.findById(loadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Load not found"));
        if (load.getAssignedCarrier() == null ||
                !load.getAssignedCarrier().getId().equals(current.getCarrier().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to this load");
        }
        // Treat null status as OPEN (pre-existing data without explicit status)
        LoadPosting.LoadStatus currentStatus = load.getStatus() != null
                ? load.getStatus()
                : LoadPosting.LoadStatus.OPEN;
        log.info("advanceLoadStatus: loadId={} currentStatus={} carrierId={}",
                loadId, currentStatus, current.getCarrier().getId());
        LoadPosting.LoadStatus nextStatus = CARRIER_TRANSITIONS.get(currentStatus);
        if (nextStatus == null) {
            log.warn("advanceLoadStatus: no transition from {} for loadId={}", currentStatus, loadId);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No further status updates available (current: " + currentStatus + ")");
        }
        load.setStatus(nextStatus);
        LoadPosting saved = loadRepo.save(load);
        if (nextStatus == LoadPosting.LoadStatus.PAID) {
            notifyPaymentConfirmed(saved, current.getCarrier());
        } else {
            notifyBrokerOfStatusUpdate(saved, current.getCarrier(), nextStatus.name());
        }
        return toDto(saved);
    }

    private void notifyBrokerOfStatusUpdate(LoadPosting load, Carrier carrier, String newStatus) {
        try {
            String ownerEmail = null;
            String ownerName = null;
            if (load.getBroker() != null) {
                ownerEmail = userRepository.findByBrokerId(load.getBroker().getId())
                        .map(User::getEmail).orElse(null);
                ownerName = load.getBroker().getCompanyName() != null
                        ? load.getBroker().getCompanyName() : load.getBroker().getLegalName();
            } else if (load.getDealer() != null) {
                ownerEmail = userRepository.findByDealerId(load.getDealer().getId())
                        .map(User::getEmail).orElse(null);
                ownerName = load.getDealer().getCompanyName();
            }
            if (ownerEmail == null) return;
            LoadStatusUpdateEmailContext ctx = new LoadStatusUpdateEmailContext();
            ctx.init(ownerEmail, ownerName, load, carrier, newStatus, appProperties.getMail().getFrom(), appProperties.getFrontend().getBaseUrl());
            emailService.sendEmail(ctx);
        } catch (Exception e) {
            log.error("Failed to send status update email for loadId={}: {}", load.getId(), e.getMessage(), e);
        }
    }

    private void notifyPaymentConfirmed(LoadPosting load, Carrier carrier) {
        try {
            // Email to carrier
            String carrierEmail = userRepository.findByCarrierId(carrier.getId()).map(User::getEmail).orElse(null);
            String carrierName  = carrier.getCompanyName() != null ? carrier.getCompanyName() : carrier.getLegalName();
            if (carrierEmail != null) {
                PaymentConfirmedEmailContext carrierCtx = new PaymentConfirmedEmailContext();
                carrierCtx.initForCarrier(carrierEmail, carrierName, load, appProperties.getMail().getFrom(), appProperties.getFrontend().getBaseUrl());
                emailService.sendEmail(carrierCtx);
            }

            // Email to broker / dealer
            String brokerEmail = null;
            String brokerName  = null;
            if (load.getBroker() != null) {
                brokerEmail = userRepository.findByBrokerId(load.getBroker().getId()).map(User::getEmail).orElse(null);
                brokerName  = load.getBroker().getCompanyName() != null ? load.getBroker().getCompanyName() : load.getBroker().getLegalName();
            } else if (load.getDealer() != null) {
                brokerEmail = userRepository.findByDealerId(load.getDealer().getId()).map(User::getEmail).orElse(null);
                brokerName  = load.getDealer().getCompanyName();
            }
            if (brokerEmail != null) {
                PaymentConfirmedEmailContext brokerCtx = new PaymentConfirmedEmailContext();
                brokerCtx.initForBroker(brokerEmail, brokerName, load, carrier, appProperties.getMail().getFrom(), appProperties.getFrontend().getBaseUrl());
                emailService.sendEmail(brokerCtx);
            }
        } catch (Exception e) {
            log.error("Failed to send payment confirmation emails for loadId={}: {}", load.getId(), e.getMessage(), e);
        }
    }

    private void notifyBrokerOfCarrierRejection(Carrier carrier, LoadPosting load) {
        try {
            String brokerEmail = null;
            String brokerName  = null;
            if (load.getBroker() != null) {
                brokerEmail = userRepository.findByBrokerId(load.getBroker().getId()).map(User::getEmail).orElse(null);
                brokerName  = load.getBroker().getCompanyName() != null ? load.getBroker().getCompanyName() : load.getBroker().getLegalName();
            } else if (load.getDealer() != null) {
                brokerEmail = userRepository.findByDealerId(load.getDealer().getId()).map(User::getEmail).orElse(null);
                brokerName  = load.getDealer().getCompanyName();
            }
            if (brokerEmail == null || carrier == null) return;

            AssignmentRejectedBrokerEmailContext ctx = new AssignmentRejectedBrokerEmailContext();
            ctx.init(brokerEmail, brokerName, carrier, load, appProperties.getMail().getFrom(), appProperties.getFrontend().getBaseUrl());
            emailService.sendEmail(ctx);
        } catch (Exception e) {
            log.error("Failed to send assignment rejection email to broker for loadId={}: {}", load.getId(), e.getMessage(), e);
        }
    }

    private void notifyCarrierOfRejection(Bid bid, LoadPosting load) {
        if (bid.getCarrier() == null) return;
        try {
            String carrierEmail = userRepository.findByCarrierId(bid.getCarrier().getId())
                    .map(User::getEmail)
                    .orElse(null);
            if (carrierEmail == null) return;

            String carrierName = bid.getCarrier().getCompanyName() != null
                    ? bid.getCarrier().getCompanyName()
                    : bid.getCarrier().getLegalName();

            BidRejectedEmailContext ctx = new BidRejectedEmailContext();
            ctx.init(carrierEmail, carrierName, bid, load, appProperties.getMail().getFrom(), appProperties.getFrontend().getBaseUrl());
            emailService.sendEmail(ctx);
        } catch (Exception e) {
            log.error("Failed to send bid rejection email for bidId={}: {}", bid.getId(), e.getMessage(), e);
        }
    }

    private void notifyCarrierOfApproval(Carrier carrier, Bid bid, LoadPosting load) {
        if (carrier == null) return;
        try {
            String carrierEmail = userRepository.findByCarrierId(carrier.getId())
                    .map(User::getEmail)
                    .orElse(null);
            if (carrierEmail == null) return;

            String carrierName = carrier.getCompanyName() != null
                    ? carrier.getCompanyName()
                    : carrier.getLegalName();

            BidApprovedEmailContext ctx = new BidApprovedEmailContext();
            ctx.init(carrierEmail, carrierName, bid, load, appProperties.getMail().getFrom(), appProperties.getFrontend().getBaseUrl());
            emailService.sendEmail(ctx);
        } catch (Exception e) {
            log.error("Failed to send bid approval email for carrierId={}: {}", carrier.getId(), e.getMessage(), e);
        }
    }

    private void notifyBrokerOfBid(Bid bid, LoadPosting load) {
        try {
            String ownerEmail = null;
            String ownerName = null;
            if (load.getBroker() != null) {
                ownerEmail = userRepository.findByBrokerId(load.getBroker().getId())
                        .map(User::getEmail).orElse(null);
                ownerName = load.getBroker().getCompanyName() != null
                        ? load.getBroker().getCompanyName() : load.getBroker().getLegalName();
            } else if (load.getDealer() != null) {
                ownerEmail = userRepository.findByDealerId(load.getDealer().getId())
                        .map(User::getEmail).orElse(null);
                ownerName = load.getDealer().getCompanyName();
            }
            if (ownerEmail == null) return;

            BidPlacedEmailContext ctx = new BidPlacedEmailContext();
            ctx.init(ownerEmail, ownerName, bid, load, appProperties.getMail().getFrom(), appProperties.getFrontend().getBaseUrl());
            emailService.sendEmail(ctx);
        } catch (Exception e) {
            log.error("Failed to send bid notification for loadId={}: {}", load.getId(), e.getMessage(), e);
        }
    }

    // ── Preferred-line matching ──────────────────────────────────────────────

    public List<LoadPostingDto> getPreferredLineLoads() {
        User current = authService.currentUserOrThrow();
        Carrier carrier = current.getCarrier();
        if (carrier == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only carriers can access preferred-line loads");
        }
        String json = carrier.getPreferredLines();
        if (json == null || json.isBlank()) return List.of();

        List<Map<String, String>> lines = parsePreferredLines(json);
        if (lines.isEmpty()) return List.of();

        return loadRepo.findAll().stream()
                .filter(l -> l.getStatus() == null || l.getStatus() == LoadPosting.LoadStatus.OPEN)
                .filter(l -> matchesAnyLine(lines, l))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private void notifyMatchingCarriers(LoadPosting load) {
        String pickupState = load.getPickupAddress() != null ? load.getPickupAddress().getState() : null;
        String dropState   = load.getDropAddress()   != null ? load.getDropAddress().getState()   : null;
        if (pickupState == null || dropState == null) return;

        carrierRepo.findAll().stream()
                .filter(c -> c.getPreferredLines() != null && !c.getPreferredLines().isBlank())
                .filter(c -> matchesAnyLine(parsePreferredLines(c.getPreferredLines()), load))
                .forEach(carrier -> {
                    try {
                        String carrierEmail = userRepository.findByCarrierId(carrier.getId())
                                .map(User::getEmail).orElse(null);
                        if (carrierEmail == null) return;
                        String carrierName = carrier.getCompanyName() != null
                                ? carrier.getCompanyName() : carrier.getLegalName();
                        NewLoadAlertEmailContext ctx = new NewLoadAlertEmailContext();
                        ctx.init(carrierEmail, carrierName, load, appProperties.getMail().getFrom(), appProperties.getFrontend().getBaseUrl());
                        emailService.sendEmail(ctx);
                    } catch (Exception e) {
                        log.error("Failed to send preferred-line alert to carrierId={}: {}",
                                carrier.getId(), e.getMessage(), e);
                    }
                });
    }

    private boolean matchesAnyLine(List<Map<String, String>> lines, LoadPosting load) {
        String pickup = load.getPickupAddress() != null ? load.getPickupAddress().getState() : null;
        String drop   = load.getDropAddress()   != null ? load.getDropAddress().getState()   : null;
        if (pickup == null || drop == null) return false;
        return lines.stream().anyMatch(line ->
                pickup.equalsIgnoreCase(line.get("fromState")) &&
                drop.equalsIgnoreCase(line.get("toState")));
    }

    private List<Map<String, String>> parsePreferredLines(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse preferredLines JSON: {}", e.getMessage());
            return List.of();
        }
    }
}
