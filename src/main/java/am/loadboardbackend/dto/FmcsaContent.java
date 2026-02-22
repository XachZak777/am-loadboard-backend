package am.loadboardbackend.dto;

import am.loadboardbackend.dto.fmcsa.FmcsaCarrier;
import lombok.Data;

@Data
public class FmcsaContent {
    private FmcsaCarrier carrier;
}
