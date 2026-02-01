package am.loadboardbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import tools.jackson.databind.ObjectMapper;

@Data
@AllArgsConstructor
public class CarrierPreviewDto {

    private Long dotNumber;
    private String mcNumber;
    private String legalName;
    private String dbaName;
    private String phyCity;
    private String phyState;

    private String rawFmcsaJson;

    public static CarrierPreviewDto from(FmcsaCarrier carrier,
                                         FmcsaCarrierResponse raw) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return new CarrierPreviewDto(
                    carrier.getDotNumber(),
                    carrier.getMcNumber(),
                    carrier.getLegalName(),
                    carrier.getDbaName(),
                    carrier.getPhyCity(),
                    carrier.getPhyState(),
                    mapper.writeValueAsString(raw)
            );
        } catch (Exception e) {
            throw new RuntimeException("FMCSA_MAPPING_FAILED");
        }
    }
}

