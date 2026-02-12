package am.loadboardbackend.service;

import am.loadboardbackend.dto.load.CreateLoadRequest;
import am.loadboardbackend.dto.load.LoadResponseDto;
import am.loadboardbackend.model.Broker;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class LoadService {

    public LoadResponseDto create(CreateLoadRequest request, Broker broker) {
        // TODO: implement persistence and real mapping once load entity is defined
        return new LoadResponseDto(
                null,
                request.originAddresses(),
                request.destinationAddresses(),
                request.pickupDate(),
                request.rate(),
                "NEW"
        );
    }

    public List<LoadResponseDto> getByBroker(Broker broker) {
        // TODO: implement real lookup once load storage is available
        return Collections.emptyList();
    }
}

