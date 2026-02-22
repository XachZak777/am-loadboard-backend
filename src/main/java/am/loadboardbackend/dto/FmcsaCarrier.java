package am.loadboardbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmcsaCarrier {

    private String allowToOperate;     // ✅ exact name from FMCSA
    private String outOfService;
    private String outOfServiceDate;

    private Long dotNumber;            // API says Number
    private Long mcNumber;             // API says Number

    private String legalName;
    private String dbaName;

    private String phyStreet;
    private String phyCity;
    private String phyState;
    private String phyZip;             // ✅ exact name
    private String phyCountry;

    private Integer totalDrivers;      // only if returned
    private Integer totalPowerUnits;   // only if returned
}