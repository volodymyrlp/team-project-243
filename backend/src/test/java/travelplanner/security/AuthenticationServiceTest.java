package travelplanner.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import travelplanner.dto.user.UserLoginRequestDto;
import travelplanner.dto.user.UserLoginResponseDto;
import travelplanner.entity.User;
import travelplanner.exception.EntityNotFoundException;
import travelplanner.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authenticate: Should return login response with token when credentials are valid")
    void authenticate_ShouldReturnToken_WhenCredentialsAreValid() {

        UserLoginRequestDto requestDto = new UserLoginRequestDto();
        requestDto.setEmail("test@example.com");
        requestDto.setPassword("password123");

        Authentication authentication = mock(Authentication.class);
        String expectedToken = "jwt.token.string";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(jwtUtil.generateToken("test@example.com")).thenReturn(expectedToken);

        UserLoginResponseDto responseDto = authenticationService.authenticate(requestDto);

        assertNotNull(responseDto);
        assertEquals(expectedToken, responseDto.token());
        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, times(1)).generateToken("test@example.com");
    }

    @Test
    @DisplayName("Get Authenticated User: Should return user from database when authenticated")
    void getAuthenticatedUser_ShouldReturnUser_WhenAuthenticated() {

        UUID userId = UUID.randomUUID();
        User principalUser = new User();
        principalUser.setUserId(userId);

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principalUser);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        User dbUser = new User();
        dbUser.setUserId(userId);
        dbUser.setEmail("test@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(dbUser));

        User actualUser = authenticationService.getAuthenticatedUser();

        assertNotNull(actualUser);
        assertEquals(userId, actualUser.getUserId());
        assertEquals("test@example.com", actualUser.getEmail());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Get Authenticated User: Should throw "
            + "EntityNotFoundException when authentication is missing")
    void getAuthenticatedUser_ShouldThrowException_WhenAuthenticationIsNull() {

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> authenticationService.getAuthenticatedUser()
        );

        assertEquals("User is not authenticated", exception.getMessage());
    }

    @Test
    @DisplayName("Get Authenticated User: Should throw "
            + "EntityNotFoundException when user not found in DB")
    void getAuthenticatedUser_ShouldThrowException_WhenUserNotFoundInDb() {

        UUID userId = UUID.randomUUID();
        User principalUser = new User();
        principalUser.setUserId(userId);

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principalUser);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> authenticationService.getAuthenticatedUser()
        );

        assertEquals("Can't find user by id " + userId, exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Get Authenticated User ID: Should return correct UUID from principal")
    void getAuthenticatedUserId_ShouldReturnCorrectId() {

        UUID expectedId = UUID.randomUUID();
        User principalUser = new User();
        principalUser.setUserId(expectedId);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principalUser);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UUID actualId = authenticationService.getAuthenticatedUserId();

        assertEquals(expectedId, actualId);
    }
}
