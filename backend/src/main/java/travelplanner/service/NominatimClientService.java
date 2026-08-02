package travelplanner.service;

import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import travelplanner.dto.geocoding.NominatimResponseDto;

@Service
public class NominatimClientService {

    private final RestClient restClient;

    public NominatimClientService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader("User-Agent", "TravelPlannerApp/1.0")
                .build();
    }

    public List<NominatimResponseDto> search(String query) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<NominatimResponseDto>>() {});
    }
}
