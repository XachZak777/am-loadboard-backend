package am.loadboardbackend.mailing;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public abstract class AbstractEmailContext {

    private String to;
    private String from;
    private String subject;
    private String templateLocation;
    private final Map<String, Object> context = new HashMap<>();

    public abstract <T> void init(T source);

    public Object put(String key, Object value) {
        return key == null ? null : this.context.put(key, value);
    }

    public Object get(String key) {
        return this.context.get(key);
    }

    public Map<String, Object> getContext() {
        return context;
    }
}
