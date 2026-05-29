package am.loadboardbackend.mailing;

import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.model.LoadPosting;

public class AssignmentRejectedBrokerEmailContext extends AbstractEmailContext {

    @Override
    public <T> void init(T source) {
        throw new UnsupportedOperationException("Use init(String, String, Carrier, LoadPosting, String, String)");
    }

    public void init(String brokerEmail, String brokerName, Carrier carrier,
                     LoadPosting load, String fromAddress, String appBaseUrl) {
        setTo(brokerEmail);
        setFrom(fromAddress);
        setSubject("Assignment Rejected — " + vehicleSummary(load));
        setTemplateLocation("email/assignment-rejected-broker");

        String carrierName = carrier.getCompanyName() != null && !carrier.getCompanyName().isBlank()
                ? carrier.getCompanyName()
                : carrier.getLegalName();

        put("brokerName",      brokerName != null && !brokerName.isBlank() ? brokerName : brokerEmail.split("@")[0]);
        put("carrierName",     carrierName != null ? carrierName : "The carrier");
        put("vehicleSummary",  vehicleSummary(load));
        put("pickupCity",      load.getPickupAddress() != null ? load.getPickupAddress().getCity()  : "—");
        put("pickupState",     load.getPickupAddress() != null ? load.getPickupAddress().getState() : "");
        put("dropCity",        load.getDropAddress()   != null ? load.getDropAddress().getCity()    : "—");
        put("dropState",       load.getDropAddress()   != null ? load.getDropAddress().getState()   : "");
        put("orderId",         load.getOrderId());
        put("loadId",          load.getId().toString());
        put("dashboardUrl",    appBaseUrl + "/broker/dashboard");
    }

    private String vehicleSummary(LoadPosting load) {
        if (load.getVehicle() == null) return "Vehicle";
        Integer year  = load.getVehicle().getYear();
        String  make  = load.getVehicle().getMake()  != null ? load.getVehicle().getMake()  : "";
        String  model = load.getVehicle().getModel() != null ? load.getVehicle().getModel() : "";
        return ((year != null ? year + " " : "") + make + " " + model).trim();
    }
}
