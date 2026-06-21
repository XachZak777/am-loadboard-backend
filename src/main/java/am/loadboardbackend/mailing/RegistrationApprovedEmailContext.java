package am.loadboardbackend.mailing;

import am.loadboardbackend.model.User;

public class RegistrationApprovedEmailContext extends AbstractEmailContext {

    @Override
    public <T> void init(T source) {
        User user = (User) source;
        setTo(user.getEmail());
        setSubject("Your LoadBoard Registration Has Been Approved!");
        setTemplateLocation("email/registration-approved");

        String name = user.getEmail().split("@")[0];
        String role  = "CARRIER";
        String companyName = "";
        if (user.getCarrier() != null) {
            role = "Carrier";
            companyName = user.getCarrier().getCompanyName() != null
                    ? user.getCarrier().getCompanyName()
                    : (user.getCarrier().getLegalName() != null ? user.getCarrier().getLegalName() : "");
        } else if (user.getBroker() != null) {
            role = "Broker";
            companyName = user.getBroker().getCompanyName() != null
                    ? user.getBroker().getCompanyName()
                    : (user.getBroker().getLegalName() != null ? user.getBroker().getLegalName() : "");
        } else if (user.getDealer() != null) {
            role = "Dealer";
            companyName = user.getDealer().getCompanyName() != null
                    ? user.getDealer().getCompanyName() : "";
        }

        put("firstName", name);
        put("email", user.getEmail());
        put("role", role);
        put("companyName", companyName);
    }

    public void buildLoginUrl(String baseUrl) {
        put("loginUrl", baseUrl + "/login");
    }
}
