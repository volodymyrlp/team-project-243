package travelplanner.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import travelplanner.dto.landing.LandingContentResponse;
import travelplanner.service.LandingService;

@RestController
@RequestMapping("/api/v1/landing")
@RequiredArgsConstructor
public class LandingController {

    private final LandingService landingService;

    @GetMapping("/content")
    public ResponseEntity<LandingContentResponse> getLandingContent() {
        LandingContentResponse content = landingService.getLandingContent();
        return ResponseEntity.ok(content);
    }
}
