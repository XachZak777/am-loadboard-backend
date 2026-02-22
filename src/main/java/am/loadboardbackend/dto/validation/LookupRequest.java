package am.loadboardbackend.dto.validation;

import am.loadboardbackend.dto.CarrierLookupType;

public record LookupRequest(
        String lookupValue,
        CarrierLookupType lookupType
) {}
