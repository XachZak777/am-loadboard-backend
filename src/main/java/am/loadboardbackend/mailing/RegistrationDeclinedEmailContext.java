package am.loadboardbackend.mailing;

import am.loadboardbackend.model.User;

public class RegistrationDeclinedEmailContext extends AbstractEmailContext {

    @Override
    public <T> void init(T source) {
        User user = (User) source;
        setTo(user.getEmail());
        setSubject("Update on Your LoadBoard Registration");
        setTemplateLocation("email/registration-declined");

        String name = user.getEmail().split("@")[0];
        String role = "Carrier";
        String companyName = "";
        if (user.getCarrier() != null) {
            role = "Carrier";
            companyName = user.getCarrier().getLegalName() != null
                    ? user.getCarrier().getLegalName() : "";
        } else if (user.getBroker() != null) {
            role = "Broker";
            companyName = user.getBroker().getLegalName() != null
                    ? user.getBroker().getLegalName() : "";
        }

        put("firstName", name);
        put("email", user.getEmail());
        put("role", role);
        put("companyName", companyName);
    }
}
