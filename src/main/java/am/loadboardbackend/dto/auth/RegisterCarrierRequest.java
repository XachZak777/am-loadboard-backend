package am.loadboardbackend.dto.auth;

import am.loadboardbackend.dto.carrier.CarrierLookupType;

public record RegisterCarrierRequest(
     String email,
     String password,
     String lookupValue,
     CarrierLookupType lookupType
) {}
