package am.loadboardbackend.dto.fmcsa;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * Handles the FMCSA REST API inconsistency where {@code content} is:
 * <ul>
 *   <li>A single JSON object  — for the DOT endpoint ({@code /carriers/{dot}})</li>
 *   <li>A JSON array          — for the MC  endpoint ({@code /carriers/docket-number/{mc}})</li>
 * </ul>
 * In both cases we extract the first (and only) {@link FmcsaContent} entry.
 */
public class FmcsaContentDeserializer extends StdDeserializer<FmcsaContent> {

    public FmcsaContentDeserializer() {
        super(FmcsaContent.class);
    }

    @Override
    public FmcsaContent deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
        if (p.currentToken() == JsonToken.START_ARRAY) {
            // MC / docket-number endpoint returns an array — take the first element
            FmcsaContent first = null;
            while (p.nextToken() != JsonToken.END_ARRAY) {
                if (p.currentToken() == JsonToken.START_OBJECT && first == null) {
                    first = ctx.readValue(p, FmcsaContent.class);
                } else {
                    p.skipChildren(); // skip any unexpected extra entries
                }
            }
            return first;
        }

        // DOT endpoint returns a plain object
        return ctx.readValue(p, FmcsaContent.class);
    }
}
