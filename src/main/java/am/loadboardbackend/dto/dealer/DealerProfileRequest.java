package am.loadboardbackend.dto.dealer;

public record DealerProfileRequest(
        String companyName,
        String ownerFirstName,
        String ownerLastName,
        String businessPhone,
        String companyAddress,
        String city,
        String state,
        String zipCode,
        String yearEstablished,
        String dealerLicenseNumber,
        String auctionAccessNumber,
        String howDidYouHear
) {}
