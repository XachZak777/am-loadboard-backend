package am.loadboardbackend.service;

import am.loadboardbackend.dto.carrier.CarrierPreviewDto;
import am.loadboardbackend.dto.carrier.CarrierResponseDto;
import am.loadboardbackend.mapper.CarrierMapper;
import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.repository.CarrierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarrierService {

    private final CarrierRepository carrierRepository;
    private final CarrierMapper carrierMapper;

    public CarrierResponseDto getMyCarrier(UUID carrierId) {
        Carrier carrier = carrierRepository.findById(carrierId)
                .orElseThrow(() -> new RuntimeException("Carrier not found"));

        return carrierMapper.toResponse(carrier);
    }

    public List<CarrierPreviewDto> getCarrierPreviews(List<Carrier> carriers) {
        return carriers.stream()
                .map(carrierMapper::toPreview)
                .toList();
    }

    public Carrier getEntity(UUID carrierId) {
        return carrierRepository.findById(carrierId)
                .orElseThrow(() -> new RuntimeException("Carrier not found"));
    }
}
