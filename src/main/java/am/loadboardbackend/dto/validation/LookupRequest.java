package am.loadboardbackend.dto.validation;

import am.loadboardbackend.dto.carrier.CarrierLookupType;

public record LookupRequest(
        String lookupValue,
        CarrierLookupType lookupType
) {}
