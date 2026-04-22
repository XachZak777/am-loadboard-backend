package am.loadboardbackend.dto.fmcsa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmcsaContent {
    private FmcsaCarrier carrier;
}
