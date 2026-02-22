package am.loadboardbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
public class CarrierPreviewDto {

    private Long dotNumber;
    private Long mcNumber;
    private String legalName;
    private String dbaName;
    private String phyCity;
    private String phyState;

    public CarrierPreviewDto(Long dotNumber, Long mcNumber, String legalName, String dbaName, String phyCity, String phyState) {
        this.dotNumber = dotNumber;
        this.mcNumber = mcNumber;
        this.legalName = legalName;
        this.dbaName = dbaName;
        this.phyCity = phyCity;
        this.phyState = phyState;
    }

    public Long getDotNumber() { return dotNumber; }
    public Long getMcNumber() { return mcNumber; }
    public String getLegalName() { return legalName; }
    public String getDbaName() { return dbaName; }
    public String getPhyCity() { return phyCity; }
    public String getPhyState() { return phyState; }

}

