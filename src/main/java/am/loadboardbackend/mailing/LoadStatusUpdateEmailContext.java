package am.loadboardbackend.mailing;

import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.model.LoadPosting;

public class LoadStatusUpdateEmailContext extends AbstractEmailContext {

    @Override
    public <T> void init(T source) {
        throw new UnsupportedOperationException("Use the named init method");
    }

    public void init(String brokerEmail, String brokerName, LoadPosting load,
                     Carrier carrier, String newStatus, String fromAddress) {
        setTo(brokerEmail);
        setFrom(fromAddress);
        setTemplateLocation("email/load-status-update");

        String vehicleSummary = vehicleSummary(load);
        String carrierName = resolveCarrierName(carrier);
        String statusLabel = labelFor(newStatus);

        setSubject("Load Update: " + vehicleSummary + " — " + statusLabel);

        put("brokerName", brokerName != null && !brokerName.isBlank() ? brokerName : brokerEmail.split("@")[0]);
        put("vehicleSummary", vehicleSummary);
        put("pickupCity", load.getPickupAddress() != null ? load.getPickupAddress().getCity() : "—");
        put("pickupState", load.getPickupAddress() != null ? load.getPickupAddress().getState() : "");
        put("dropCity", load.getDropAddress() != null ? load.getDropAddress().getCity() : "—");
        put("dropState", load.getDropAddress() != null ? load.getDropAddress().getState() : "");
        put("carrierName", carrierName);
        put("statusLabel", statusLabel);
        put("statusDescription", descriptionFor(newStatus, carrierName, vehicleSummary));
        put("newStatus", newStatus);
    }

    private String vehicleSummary(LoadPosting load) {
        if (load.getVehicle() == null) return "vehicle";
        Integer year = load.getVehicle().getYear();
        String make = load.getVehicle().getMake() != null ? load.getVehicle().getMake() : "";
        String model = load.getVehicle().getModel() != null ? load.getVehicle().getModel() : "";
        return ((year != null ? year + " " : "") + make + " " + model).trim();
    }

    private String resolveCarrierName(Carrier carrier) {
        if (carrier == null) return "Your carrier";
        if (carrier.getCompanyName() != null) return carrier.getCompanyName();
        if (carrier.getLegalName() != null) return carrier.getLegalName();
        return "Your carrier";
    }

    private String labelFor(String status) {
        return switch (status) {
            case "PICKED_UP" -> "Vehicle Picked Up";
            case "DELIVERED" -> "Vehicle Delivered";
            case "PAID"      -> "Payment Confirmed";
            default          -> status;
        };
    }

    private String descriptionFor(String status, String carrierName, String vehicle) {
        return switch (status) {
            case "PICKED_UP" -> carrierName + " has confirmed that the " + vehicle + " has been picked up and is on its way.";
            case "DELIVERED" -> carrierName + " has confirmed that the " + vehicle + " has been successfully delivered.";
            case "PAID"      -> carrierName + " has confirmed receipt of payment for the " + vehicle + " shipment.";
            default          -> "The status of your load has been updated to " + status + ".";
        };
    }
}
