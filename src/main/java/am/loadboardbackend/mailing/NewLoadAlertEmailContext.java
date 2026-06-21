package am.loadboardbackend.mailing;

import am.loadboardbackend.model.LoadPosting;

public class NewLoadAlertEmailContext extends AbstractEmailContext {

    @Override
    public <T> void init(T source) {
        throw new UnsupportedOperationException("Use init(String, String, LoadPosting, String, String)");
    }

    public void init(String carrierEmail, String carrierName, LoadPosting load, String fromAddress, String appBaseUrl) {
        String pickupState = load.getPickupAddress() != null ? load.getPickupAddress().getState() : "—";
        String dropState   = load.getDropAddress()   != null ? load.getDropAddress().getState()   : "—";

        setTo(carrierEmail);
        setFrom(fromAddress);
        setSubject("New Load on Your Preferred Lane — " + pickupState + " → " + dropState);
        setTemplateLocation("email/new-load-alert");

        put("carrierName", carrierName != null && !carrierName.isBlank() ? carrierName : carrierEmail.split("@")[0]);
        put("vehicleSummary", vehicleSummary(load));
        put("pickupCity",  load.getPickupAddress() != null ? load.getPickupAddress().getCity() : "—");
        put("pickupState", pickupState);
        put("dropCity",    load.getDropAddress()   != null ? load.getDropAddress().getCity()   : "—");
        put("dropState",   dropState);
        put("price",       load.getPrice() != null ? "$" + load.getPrice() : "—");
        put("pickupDate",  load.getPickupDate() != null ? load.getPickupDate().toString() : "—");
        put("loadId",      load.getId().toString());
        put("loadBoardUrl", appBaseUrl + "/loads");
    }

    private String vehicleSummary(LoadPosting load) {
        if (load.getVehicle() == null) return "Vehicle";
        Integer year  = load.getVehicle().getYear();
        String  make  = load.getVehicle().getMake()  != null ? load.getVehicle().getMake()  : "";
        String  model = load.getVehicle().getModel() != null ? load.getVehicle().getModel() : "";
        return ((year != null ? year + " " : "") + make + " " + model).trim();
    }
}
