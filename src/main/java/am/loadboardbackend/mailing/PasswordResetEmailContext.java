package am.loadboardbackend.mailing;

import am.loadboardbackend.model.User;

public class PasswordResetEmailContext extends AbstractEmailContext {

    private String token;

    @Override
    public <T> void init(T source) {
        User user = (User) source;
        setTo(user.getEmail());
        setSubject("Reset your LoadBoard password");
        setTemplateLocation("email/password-reset");
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

    public void buildResetUrl(String baseUrl) {
        String url = baseUrl + "/reset-password?token=" + token;
        put("resetUrl", url);
    }
}
