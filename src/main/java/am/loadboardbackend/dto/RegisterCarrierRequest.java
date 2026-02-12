package am.loadboardbackend.dto;

public record RegisterCarrierRequest(
     String email,
     String password,
     String lookupValue,
     CarrierLookupType lookupType
) {}
