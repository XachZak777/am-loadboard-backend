package am.loadboardbackend.dto.fmcsa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Crash stats (tow / fatal / injury / total).
 * Maps both SAFER and FMCSA REST API crash fields.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmcsaCrashes {
    private Integer tow;
    private Integer fatal;
    private Integer injury;
    private Integer total;
}
