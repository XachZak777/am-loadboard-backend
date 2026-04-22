package am.loadboardbackend.dto.fmcsa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * The FMCSA REST API returns {@code carrierOperation} as a nested object,
 * e.g. {@code {"carrierOperationCode":"A","carrierOperationDesc":"Interstate"}}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmcsaCarrierOperation {
    private String carrierOperationCode;
    private String carrierOperationDesc;
}
