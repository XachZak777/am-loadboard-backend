package am.loadboardbackend.dto.load;

public record AdditionalVehicleRequest(
        String vehicleMake,
        String vehicleModel,
        Integer vehicleYear,
        String vehicleType,
        String vehicleCondition,
        String vin,
        String vehicleAdditionalInfo,
        Double weight
) {}
