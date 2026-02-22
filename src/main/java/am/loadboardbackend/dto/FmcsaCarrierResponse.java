package am.loadboardbackend.dto;

import am.loadboardbackend.dto.fmcsa.FmcsaContent;
import lombok.Data;

import java.util.List;

@Data
public class FmcsaCarrierResponse {
    private List<FmcsaContent> content;
}