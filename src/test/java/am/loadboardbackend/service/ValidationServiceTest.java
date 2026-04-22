package am.loadboardbackend.service;

import am.loadboardbackend.client.FmcsaClient;
import am.loadboardbackend.dto.carrier.CarrierLookupType;
import am.loadboardbackend.dto.fmcsa.FmcsaCarrier;
import am.loadboardbackend.dto.fmcsa.FmcsaCarrierResponse;
import am.loadboardbackend.dto.fmcsa.FmcsaContent;
import am.loadboardbackend.dto.fmcsa.FmcsaAuthorityResponse;
import am.loadboardbackend.dto.validation.LookupResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidationServiceTest {

    @Test
    public void carrierValidation_returnsLookupResponse() {
        FmcsaClient client = Mockito.mock(FmcsaClient.class);

        FmcsaCarrier carrier = new FmcsaCarrier();
        carrier.setDotNumber("123456");
        carrier.setMcNumber("MC123");
        carrier.setLegalName("ACME Logistics");

        FmcsaContent content = new FmcsaContent();
        content.setCarrier(carrier);

        FmcsaCarrierResponse resp = new FmcsaCarrierResponse();
        resp.setContent(content);

        Mockito.when(client.fetchByMc("MC123")).thenReturn(resp);

    TemporaryValidationStore store = new TemporaryValidationStore();
    var carrierValidationRepo = Mockito.mock(am.loadboardbackend.repository.CarrierValidationRepository.class);
    CarrierValidationService svc = new CarrierValidationService(client, store, carrierValidationRepo);

        LookupResponse out = svc.validateAndCache("MC123", CarrierLookupType.MC);

        assertThat(out).isNotNull();
        assertThat(out.mcNumber()).isEqualTo("MC123");
    }

    @Test
    public void brokerValidation_returnsLookupResponse() {
        FmcsaClient client = Mockito.mock(FmcsaClient.class);

        FmcsaCarrier carrier = new FmcsaCarrier();
        carrier.setDotNumber("654321");
        carrier.setMcNumber("MC456");
        carrier.setLegalName("Broker Inc");

        FmcsaContent content = new FmcsaContent();
        content.setCarrier(carrier);

        FmcsaCarrierResponse resp = new FmcsaCarrierResponse();
        resp.setContent(content);

    FmcsaAuthorityResponse auth = new FmcsaAuthorityResponse();
    FmcsaAuthorityResponse.Authority a = new FmcsaAuthorityResponse.Authority();
    a.setBrokerAuthorityStatus("ACTIVE");
    auth.setAuthority(a);

    Mockito.when(client.fetchByDot("654321")).thenReturn(resp);
    Mockito.when(client.fetchAuthority("654321")).thenReturn(auth);

    TemporaryValidationStore store = new TemporaryValidationStore();
    var brokerValidationRepo = Mockito.mock(am.loadboardbackend.repository.BrokerValidationRepository.class);
    BrokerValidationService svc = new BrokerValidationService(client, store, brokerValidationRepo);

    LookupResponse out = svc.validateAndCache("654321", CarrierLookupType.DOT);

        assertThat(out).isNotNull();
        assertThat(out.dotNumber()).isEqualTo("654321");
        assertThat(out.brokerAuthorityActive()).isTrue();
    }
}
