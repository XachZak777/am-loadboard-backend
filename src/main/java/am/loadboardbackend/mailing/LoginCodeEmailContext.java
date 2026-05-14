package am.loadboardbackend.mailing;

import am.loadboardbackend.model.User;

public class LoginCodeEmailContext extends AbstractEmailContext {

    @Override
    public <T> void init(T source) {
        User user = (User) source;
        setTo(user.getEmail());
        setSubject("Your LoadBoard sign-in code");
        setTemplateLocation("email/login-code");
        put("email", user.getEmail());
    }

    public void setCode(String code) {
        put("code", code);
    }
}
