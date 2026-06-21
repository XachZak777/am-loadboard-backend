package am.loadboardbackend.mailing;

import am.loadboardbackend.model.User;

public class LoginCodeEmailContext extends AbstractEmailContext {

    @Override
    public <T> void init(T source) {
        throw new UnsupportedOperationException("Use init(User, String, String)");
    }

    public void init(User user, String fromAddress, String appBaseUrl) {
        setTo(user.getEmail());
        setFrom(fromAddress);
        setSubject("Your LoadBoard sign-in code");
        setTemplateLocation("email/login-code");
        put("email", user.getEmail());
        put("loginUrl", appBaseUrl + "/login");
    }

    public void setCode(String code) {
        put("code", code);
    }
}
