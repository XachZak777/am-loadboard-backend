package am.loadboardbackend.dto.fmcsa;

import lombok.Data;

@Data
public class FmcsaCarrierResponse {
    /** The FMCSA REST API wraps the single carrier inside {@code content}, not an array. */
    private FmcsaContent content;
}