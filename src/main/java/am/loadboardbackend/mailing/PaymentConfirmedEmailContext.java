package am.loadboardbackend.mailing;

import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.model.LoadPosting;

public class PaymentConfirmedEmailContext extends AbstractEmailContext {

    @Override
    public <T> void init(T source) {
        throw new UnsupportedOperationException("Use initForCarrier or initForBroker");
    }

    public void initForCarrier(String carrierEmail, String carrierName, LoadPosting load,
                                String fromAddress, String appBaseUrl) {
        setTo(carrierEmail);
        setFrom(fromAddress);
        setSubject("Payment Confirmed — " + vehicleSummary(load));
        setTemplateLocation("email/payment-confirmed");
        populate(carrierEmail, carrierName, "carrier", load, appBaseUrl + "/carrier/completed");
        put("carrierName", null);
    }

    public void initForBroker(String brokerEmail, String brokerName, LoadPosting load,
                               Carrier carrier, String fromAddress, String appBaseUrl) {
        setTo(brokerEmail);
        setFrom(fromAddress);
        setSubject("Payment Confirmed — " + vehicleSummary(load));
        setTemplateLocation("email/payment-confirmed");
        populate(brokerEmail, brokerName, "broker", load, appBaseUrl + "/broker/dashboard");
        put("carrierName", resolveCarrierName(carrier));
    }

    private void populate(String email, String name, String role, LoadPosting load, String dashboardUrl) {
        put("recipientName",  name != null && !name.isBlank() ? name : email.split("@")[0]);
        put("recipientRole",  role);
        put("vehicleSummary", vehicleSummary(load));
        put("pickupCity",  load.getPickupAddress() != null ? load.getPickupAddress().getCity()  : "—");
        put("pickupState", load.getPickupAddress() != null ? load.getPickupAddress().getState() : "");
        put("dropCity",    load.getDropAddress()   != null ? load.getDropAddress().getCity()    : "—");
        put("dropState",   load.getDropAddress()   != null ? load.getDropAddress().getState()   : "");
        put("price",       load.getPrice() != null ? "$" + load.getPrice().intValue() : "—");
        put("orderId",     load.getOrderId());
        put("dashboardUrl", dashboardUrl);
    }

    private String vehicleSummary(LoadPosting load) {
        if (load.getVehicle() == null) return "Vehicle";
        Integer year  = load.getVehicle().getYear();
        String  make  = load.getVehicle().getMake()  != null ? load.getVehicle().getMake()  : "";
        String  model = load.getVehicle().getModel() != null ? load.getVehicle().getModel() : "";
        return ((year != null ? year + " " : "") + make + " " + model).trim();
    }

    private String resolveCarrierName(Carrier carrier) {
        if (carrier == null) return "your carrier";
        if (carrier.getCompanyName() != null) return carrier.getCompanyName();
        if (carrier.getLegalName()   != null) return carrier.getLegalName();
        return "your carrier";
    }
}
