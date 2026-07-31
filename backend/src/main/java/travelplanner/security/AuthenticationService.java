package travelplanner.security;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import travelplanner.dto.user.UserLoginRequestDto;
import travelplanner.dto.user.UserLoginResponseDto;
import travelplanner.entity.User;
import travelplanner.exception.EntityNotFoundException;
import travelplanner.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public UserLoginResponseDto authenticate(UserLoginRequestDto userLoginRequestDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userLoginRequestDto.getEmail(), userLoginRequestDto.getPassword()));
        String token = jwtUtil.generateToken(authentication.getName());
        return new UserLoginResponseDto(token);
    }

    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new EntityNotFoundException("User is not authenticated");
        }
        User user = (User) authentication.getPrincipal();
        return userRepository.findById(user.getUserId()).orElseThrow(
                () -> new EntityNotFoundException("Can't find user by id " + user.getUserId())
        );
    }

    public UUID getAuthenticatedUserId() {
        User principal = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.getUserId();
    }
}
