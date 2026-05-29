package am.loadboardbackend.mailing;

public class NewRatingEmailContext extends AbstractEmailContext {

    @Override
    public <T> void init(T source) {
        throw new UnsupportedOperationException("Use init(String, String, String, String, String, String, String, String)");
    }

    public void init(String recipientEmail, String recipientName, String ratingType,
                     String fromName, String fromRole, String vehicleSummary,
                     String fromAddress, String dashboardUrl) {
        setTo(recipientEmail);
        setFrom(fromAddress);
        setSubject("You received a new " + ratingType + " rating on Haulius");
        setTemplateLocation("email/new-rating");

        put("recipientName", recipientName != null && !recipientName.isBlank()
                ? recipientName : recipientEmail.split("@")[0]);
        put("ratingType", ratingType);
        put("isPositive", "positive".equals(ratingType));
        put("fromName", fromName != null ? fromName : fromRole);
        put("fromRole", fromRole);
        put("vehicleSummary", vehicleSummary);
        put("dashboardUrl", dashboardUrl);
    }
}
