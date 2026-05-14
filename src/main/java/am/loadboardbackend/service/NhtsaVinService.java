package am.loadboardbackend.service;

import am.loadboardbackend.dto.vin.VinDecodeResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class NhtsaVinService {

    private static final String NHTSA_URL =
            "https://vpic.nhtsa.dot.gov/api/vehicles/DecodeVinValues/%s?format=json";

    private final RestTemplate restTemplate;

    public NhtsaVinService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(8_000);
        RestTemplate rt = new RestTemplate(factory);
        rt.getInterceptors().add((req, body, exec) -> {
            req.getHeaders().set("User-Agent", "HauliusLoadBoard/1.0 (contact@haulius.com)");
            req.getHeaders().set("Accept",     "application/json");
            return exec.execute(req, body);
        });
        this.restTemplate = rt;
    }

    public VinDecodeResult decode(String vin) {
        VinDecodeResult result = new VinDecodeResult();
        result.setVin(vin);
        try {
            String url = String.format(NHTSA_URL, vin.trim().toUpperCase());
            NhtsaResponse response = restTemplate.getForObject(url, NhtsaResponse.class);

            if (response == null || response.results == null || response.results.isEmpty()) {
                result.setSuccess(false);
                result.setErrorText("No data returned for this VIN.");
                return result;
            }

            NhtsaResult r = response.results.get(0);

            // ErrorCode "0" means clean decode; anything else indicates an issue
            if (r.errorCode != null && !r.errorCode.trim().equals("0")) {
                result.setSuccess(false);
                result.setErrorText(r.errorText != null ? r.errorText : "Invalid VIN.");
                return result;
            }

            // Identity
            result.setMake(titleCase(r.make));
            result.setModel(r.model);
            result.setYear(parseIntNullable(r.modelYear));
            result.setManufacturer(titleCase(nullIfBlank(r.manufacturer)));

            // Classification
            result.setVehicleType(mapVehicleType(r.vehicleType, r.bodyClass));
            result.setBodyClass(nullIfBlank(r.bodyClass));
            result.setTrim(nullIfBlank(r.trim));

            // Engine
            result.setEngineHp(nullIfBlank(r.engineHp));
            result.setCylinders(nullIfBlank(r.cylinders));
            result.setDisplacementL(nullIfBlank(r.displacementL));
            result.setEngineConfiguration(nullIfBlank(r.engineConfiguration));
            result.setEngineModel(nullIfBlank(r.engineModel));
            result.setTurbo(nullIfBlank(r.turbo));

            // Drivetrain & Transmission
            result.setDriveType(nullIfBlank(r.driveType));
            result.setTransmissionStyle(nullIfBlank(r.transmissionStyle));
            result.setTransmissionSpeeds(nullIfBlank(r.transmissionSpeeds));

            // Fuel
            result.setFuelType(nullIfBlank(r.fuelType));

            // Dimensions & Weight
            result.setDoors(parseIntNullable(r.doors));
            result.setSeats(parseIntNullable(r.seats));
            result.setWheelbase(nullIfBlank(r.wheelbase));
            result.setWheels(parseIntNullable(r.wheels));
            result.setGvwr(nullIfBlank(r.gvwr));

            // Origin
            result.setPlantCountry(titleCase(nullIfBlank(r.plantCountry)));
            result.setPlantState(titleCase(nullIfBlank(r.plantState)));
            result.setPlantCity(titleCase(nullIfBlank(r.plantCity)));

            // Market
            result.setBasePrice(nullIfBlank(r.basePrice));
            result.setSteeringLocation(nullIfBlank(r.steeringLocation));

            result.setSuccess(true);
        } catch (Exception e) {
            log.warn("NHTSA VIN decode failed for {}: {}", vin, e.getMessage());
            result.setSuccess(false);
            result.setErrorText("Could not reach the NHTSA database. Please try again.");
        }
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String mapVehicleType(String nhtsaType, String bodyClass) {
        String body    = bodyClass  != null ? bodyClass.toLowerCase()  : "";
        String vehicle = nhtsaType  != null ? nhtsaType.toLowerCase()  : "";

        if (body.contains("pickup") || vehicle.contains("truck"))               return "truck";
        if (body.contains("suv") || body.contains("sport utility")
                || vehicle.contains("multipurpose"))                             return "suv";
        if (body.contains("van") || body.contains("minivan"))                   return "van";
        if (body.contains("motorcycle") || vehicle.contains("motorcycle"))      return "motorcycle";
        if (body.contains("rv") || body.contains("recreational"))               return "rv";
        if (body.contains("bus"))                                                return null;
        if (vehicle.contains("passenger car")
                || body.contains("coupe")    || body.contains("sedan")
                || body.contains("hatchback") || body.contains("wagon")
                || body.contains("convertible") || body.contains("roadster"))   return "sedan";
        return null;
    }

    private Integer parseIntNullable(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank() || s.equalsIgnoreCase("not applicable")) ? null : s.trim();
    }

    private String titleCase(String s) {
        if (s == null || s.isBlank()) return null;
        String lower = s.trim().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    // ── NHTSA response model ──────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class NhtsaResponse {
        @JsonProperty("Results") public List<NhtsaResult> results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class NhtsaResult {
        // Identity
        @JsonProperty("Make")                 public String make;
        @JsonProperty("Model")                public String model;
        @JsonProperty("ModelYear")            public String modelYear;
        @JsonProperty("Manufacturer")         public String manufacturer;

        // Classification
        @JsonProperty("VehicleType")          public String vehicleType;
        @JsonProperty("BodyClass")            public String bodyClass;
        @JsonProperty("Trim")                 public String trim;

        // Engine
        @JsonProperty("EngineHP")             public String engineHp;
        @JsonProperty("EngineCylinders")      public String cylinders;
        @JsonProperty("DisplacementL")        public String displacementL;
        @JsonProperty("EngineConfiguration")  public String engineConfiguration;
        @JsonProperty("EngineModel")          public String engineModel;
        @JsonProperty("Turbo")                public String turbo;

        // Drivetrain & Transmission
        @JsonProperty("DriveType")            public String driveType;
        @JsonProperty("TransmissionStyle")    public String transmissionStyle;
        @JsonProperty("TransmissionSpeeds")   public String transmissionSpeeds;

        // Fuel
        @JsonProperty("FuelTypePrimary")      public String fuelType;

        // Dimensions & Weight
        @JsonProperty("Doors")                public String doors;
        @JsonProperty("Seats")                public String seats;
        @JsonProperty("WheelBaseShort")       public String wheelbase;
        @JsonProperty("Wheels")               public String wheels;
        @JsonProperty("GVWR")                 public String gvwr;

        // Origin
        @JsonProperty("PlantCountry")         public String plantCountry;
        @JsonProperty("PlantState")           public String plantState;
        @JsonProperty("PlantCity")            public String plantCity;

        // Market
        @JsonProperty("BasePrice")            public String basePrice;
        @JsonProperty("SteeringLocation")     public String steeringLocation;

        // Result metadata
        @JsonProperty("ErrorCode")            public String errorCode;
        @JsonProperty("ErrorText")            public String errorText;
    }
}
