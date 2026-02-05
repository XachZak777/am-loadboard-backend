package am.loadboardbackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class FmcsaCarrierResponse {
    private List<FmcsaContent> content;
}