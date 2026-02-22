package am.loadboardbackend.dto.fmcsa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmcsaCarrier {

    private String allowedToOperate;
    private String statusCode;

    private String dotNumber;
    private String mcNumber;
    private String legalName;
    private String dbaName;

    private String phyStreet;
    private String phyCity;
    private String phyState;
    private String phyZipcode;
    private String phyCountry;

    private Integer totalDrivers;
    private Integer totalPowerUnits;

}

