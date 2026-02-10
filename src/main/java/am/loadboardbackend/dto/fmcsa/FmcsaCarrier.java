package am.loadboardbackend.dto.fmcsa;

import lombok.Data;

@Data
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

