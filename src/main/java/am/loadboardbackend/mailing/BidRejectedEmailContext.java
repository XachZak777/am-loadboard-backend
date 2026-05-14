package am.loadboardbackend.mailing;

import am.loadboardbackend.model.Bid;
import am.loadboardbackend.model.LoadPosting;

public class BidRejectedEmailContext extends AbstractEmailContext {

    @Override
    public <T> void init(T source) {
        throw new UnsupportedOperationException("Use init(String, String, Bid, LoadPosting, String, String)");
    }

    public void init(String carrierEmail, String carrierName, Bid bid, LoadPosting load,
                     String fromAddress, String appBaseUrl) {
        setTo(carrierEmail);
        setFrom(fromAddress);
        setSubject("Bid Update — " + vehicleSummary(load));
        setTemplateLocation("email/bid-rejected");

        put("carrierName",  carrierName != null && !carrierName.isBlank() ? carrierName : carrierEmail.split("@")[0]);
        put("vehicleSummary", vehicleSummary(load));
        put("pickupCity",  load.getPickupAddress() != null ? load.getPickupAddress().getCity()  : "—");
        put("pickupState", load.getPickupAddress() != null ? load.getPickupAddress().getState() : "");
        put("dropCity",    load.getDropAddress()   != null ? load.getDropAddress().getCity()    : "—");
        put("dropState",   load.getDropAddress()   != null ? load.getDropAddress().getState()   : "");
        put("bidAmount",   bid.getAmount() != null ? "$" + bid.getAmount() : "—");
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
