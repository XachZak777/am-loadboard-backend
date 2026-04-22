package am.loadboardbackend.dto.fmcsa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

/**
 * Top-level wrapper returned by the FMCSA REST API.
 *
 * <p>The API is inconsistent about the {@code content} field:
 * <ul>
 *   <li>DOT  endpoint ({@code /carriers/{dot}})              — returns a single JSON object</li>
 *   <li>MC   endpoint ({@code /carriers/docket-number/{mc}}) — returns a JSON array</li>
 * </ul>
 * {@link FmcsaContentDeserializer} normalises both forms into a single {@link FmcsaContent}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmcsaCarrierResponse {

    @JsonDeserialize(using = FmcsaContentDeserializer.class)
    private FmcsaContent content;
}
