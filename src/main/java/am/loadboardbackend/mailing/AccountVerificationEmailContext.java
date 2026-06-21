package am.loadboardbackend.mailing;

import am.loadboardbackend.model.User;

public class AccountVerificationEmailContext extends AbstractEmailContext {

    private String token;

    @Override
    public <T> void init(T source) {
        User user = (User) source;
        setTo(user.getEmail());
        setSubject("Verify your LoadBoard email address");
        setTemplateLocation("email/email-verification");
        put("firstName", user.getEmail().split("@")[0]);
        put("email", user.getEmail());
    }

    public void setToken(String token) {
        this.token = token;
        put("token", token);
    }

    public String getToken() {
        return token;
    }

    public void buildVerificationUrl(String baseUrl) {
        String url = baseUrl + "/verify-email?token=" + token;
        put("verificationUrl", url);
    }
}
