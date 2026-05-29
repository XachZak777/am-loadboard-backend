package am.loadboardbackend.dto.dealer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterDealerRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotBlank String companyName,
    @NotBlank String ownerFirstName,
    @NotBlank String ownerLastName,
    @NotBlank String businessPhone,
    @NotBlank String companyAddress,
    @NotBlank String city,
    @NotBlank String state,
    @NotBlank String zipCode,
    String yearEstablished,
    String dealerLicenseNumber,
    String auctionAccessNumber,
    String howDidYouHear,
    String captchaToken
) {}
