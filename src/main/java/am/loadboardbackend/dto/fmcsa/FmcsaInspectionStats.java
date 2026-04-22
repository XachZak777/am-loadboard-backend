package am.loadboardbackend.dto.fmcsa;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Inspection stats for a single category (vehicle / driver / hazmat / iep).
 * Accepts both SAFER snake_case and FMCSA REST API camelCase field names.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmcsaInspectionStats {

    @JsonAlias({"inspections"})
    private String inspections;

    @JsonAlias({"out_of_service", "outOfService"})
    private String outOfService;

    @JsonAlias({"out_of_service_percent", "outOfServicePercent"})
    private String outOfServicePercent;

    @JsonAlias({"national_average", "nationalAverage"})
    private String nationalAverage;
}
