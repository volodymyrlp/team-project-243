package travelplanner.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import travelplanner.dto.user.UserLoginRequestDto;
import travelplanner.dto.user.UserLoginResponseDto;
import travelplanner.dto.user.UserRegisterRequestDto;
import travelplanner.dto.user.UserRegisterResponseDto;
import travelplanner.exception.RegistrationException;
import travelplanner.security.AuthenticationService;
import travelplanner.service.user.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class Authenticate {
    private final UserService userService;
    private final AuthenticationService authenticationService;

    @PostMapping("/registration")
    public ResponseEntity<UserRegisterResponseDto> register(
            @RequestBody @Valid UserRegisterRequestDto requestDto) throws RegistrationException {
        UserRegisterResponseDto response = userService.register(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(
            @RequestBody @Valid UserLoginRequestDto request) {
        UserLoginResponseDto response = authenticationService.authenticate(request);
        return ResponseEntity.ok(response);
    }
}
