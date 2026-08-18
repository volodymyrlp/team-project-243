package travelplanner.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import travelplanner.dto.user.UserResponseDto;
import travelplanner.dto.user.UserUpdatePasswordRequestDto;
import travelplanner.dto.user.UserUpdateRequestDto;
import travelplanner.service.user.UserService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getUserInfo() {
        return ResponseEntity.ok(userService.getUserInfo());
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDto> updateUserInfo(
            @RequestBody @Valid UserUpdateRequestDto requestDto) {
        return ResponseEntity.ok(userService.updateUserInfo(requestDto));
    }

    @PutMapping("/me/password")
    public ResponseEntity<UserResponseDto> updateUserPassword(
            @RequestBody @Valid UserUpdatePasswordRequestDto requestDto) {
        return ResponseEntity.ok(userService.updateUserPassword(requestDto));
    }
}
