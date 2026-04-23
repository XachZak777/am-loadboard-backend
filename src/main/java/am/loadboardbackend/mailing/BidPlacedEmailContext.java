package am.loadboardbackend.mailing;

import am.loadboardbackend.model.Bid;
import am.loadboardbackend.model.LoadPosting;

public class BidPlacedEmailContext extends AbstractEmailContext {

    @Override
    public <T> void init(T source) {
        throw new UnsupportedOperationException("Use init(String, String, Bid, LoadPosting, String)");
    }

    public void init(String brokerEmail, String brokerName, Bid bid, LoadPosting load, String fromAddress) {
        setTo(brokerEmail);
        setFrom(fromAddress);
        setSubject("New Bid on Your Load — " + vehicleSummary(load));
        setTemplateLocation("email/bid-placed");

        put("brokerName", brokerName != null && !brokerName.isBlank() ? brokerName : brokerEmail.split("@")[0]);
        put("vehicleSummary", vehicleSummary(load));
        put("pickupCity", load.getPickupAddress() != null ? load.getPickupAddress().getCity() : "—");
        put("pickupState", load.getPickupAddress() != null ? load.getPickupAddress().getState() : "");
        put("dropCity", load.getDropAddress() != null ? load.getDropAddress().getCity() : "—");
        put("dropState", load.getDropAddress() != null ? load.getDropAddress().getState() : "");
        put("bidAmount", bid.getAmount() != null ? "$" + bid.getAmount() : "—");
        put("bookNow", bid.isBookNow());
        put("carrierName", resolveCarrierName(bid));
        put("loadId", load.getId().toString());
    }

    private String vehicleSummary(LoadPosting load) {
        if (load.getVehicle() == null) return "vehicle";
        Integer year = load.getVehicle().getYear();
        String make = load.getVehicle().getMake() != null ? load.getVehicle().getMake() : "";
        String model = load.getVehicle().getModel() != null ? load.getVehicle().getModel() : "";
        return ((year != null ? year + " " : "") + make + " " + model).trim();
    }

    private String resolveCarrierName(Bid bid) {
        if (bid.getCarrier() == null) return "A carrier";
        if (bid.getCarrier().getCompanyName() != null) return bid.getCarrier().getCompanyName();
        if (bid.getCarrier().getLegalName() != null) return bid.getCarrier().getLegalName();
        return "A carrier";
    }
}
