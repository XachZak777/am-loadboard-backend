package am.loadboardbackend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class DistanceCalculatorService {

    private static final double EARTH_RADIUS_MILES = 3958.8;
    private static final double ROAD_FACTOR        = 1.20;  // straight-line → estimated road miles
    private static final String NOMINATIM_URL      = "https://nominatim.openstreetmap.org/search";

    private final RestTemplate restTemplate;

    public DistanceCalculatorService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(4_000);
        factory.setReadTimeout(6_000);
        RestTemplate rt = new RestTemplate(factory);
        rt.getInterceptors().add((req, body, exec) -> {
            req.getHeaders().set("User-Agent", "HauliusLoadBoard/1.0 (contact@haulius.com)");
            req.getHeaders().set("Accept",     "application/json");
            return exec.execute(req, body);
        });
        this.restTemplate = rt;
    }

    /**
     * Returns the estimated driving distance in miles between two US locations,
     * or null if either location cannot be geocoded.
     */
    public Double calculateDistance(String city1, String state1, String zip1,
                                    String city2, String state2, String zip2) {
        try {
            double[] p1 = geocode(city1, state1, zip1);
            double[] p2 = geocode(city2, state2, zip2);
            if (p1 == null || p2 == null) return null;
            double miles = haversine(p1[0], p1[1], p2[0], p2[1]) * ROAD_FACTOR;
            return Math.round(miles * 10.0) / 10.0;
        } catch (Exception e) {
            log.warn("Distance calculation failed: {}", e.getMessage());
            return null;
        }
    }

    private double[] geocode(String city, String state, String zip) {
        try {
            UriComponentsBuilder ub = UriComponentsBuilder.fromUriString(NOMINATIM_URL)
                    .queryParam("format", "json")
                    .queryParam("limit", 1)
                    .queryParam("country", "US");
            if (zip  != null && !zip.isBlank())   ub.queryParam("postalcode", zip.trim());
            if (city != null && !city.isBlank())  ub.queryParam("city",       city.trim());
            if (state != null && !state.isBlank()) ub.queryParam("state",     state.trim());

            NominatimResult[] results = restTemplate.getForObject(
                    ub.toUriString(), NominatimResult[].class);
            if (results == null || results.length == 0) {
                log.warn("No geocode result for city={} state={} zip={}", city, state, zip);
                return null;
            }
            return new double[]{ Double.parseDouble(results[0].lat), Double.parseDouble(results[0].lon) };
        } catch (Exception e) {
            log.warn("Geocode failed for city={} state={} zip={}: {}", city, state, zip, e.getMessage());
            return null;
        }
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_MILES * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class NominatimResult {
        public String lat;
        public String lon;
    }
}
