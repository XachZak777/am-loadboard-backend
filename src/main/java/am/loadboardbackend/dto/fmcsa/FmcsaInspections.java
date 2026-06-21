package am.loadboardbackend.dto.fmcsa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Grouped inspection stats (vehicle / driver / hazmat / iep).
 * Maps the SAFER {@code united_states_inspections} and {@code us_inspections} objects.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmcsaInspections {
    private FmcsaInspectionStats vehicle;
    private FmcsaInspectionStats driver;
    private FmcsaInspectionStats hazmat;
    private FmcsaInspectionStats iep;
}
