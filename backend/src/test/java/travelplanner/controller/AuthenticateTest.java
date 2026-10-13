package travelplanner.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import travelplanner.dto.user.UserLoginRequestDto;
import travelplanner.dto.user.UserLoginResponseDto;
import travelplanner.dto.user.UserRegisterRequestDto;
import travelplanner.dto.user.UserRegisterResponseDto;
import travelplanner.exception.CustomGlobalExceptionHandler;
import travelplanner.exception.RegistrationException;
import travelplanner.security.AuthenticationService;
import travelplanner.security.JwtUtil;
import travelplanner.service.user.UserService;

@WebMvcTest(Authenticate.class)
@Import(CustomGlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @DisplayName("Register: Should return 201 Created when request is valid")
    void register_ShouldReturnCreated_WhenRequestIsValid() throws Exception {

        UserRegisterRequestDto requestDto = new UserRegisterRequestDto();
        requestDto.setFullName("Test User");
        requestDto.setEmail("test@example.com");
        requestDto.setPasswordHash("ValidPassword123!");
        requestDto.setConfirmPassword("ValidPassword123!");

        UserRegisterResponseDto responseDto = new UserRegisterResponseDto(
                UUID.randomUUID(),
                "test@example.com",
                "Test User",
                LocalDateTime.now(),
                null
        );

        when(userService.register(any(UserRegisterRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/auth/registration")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"));

        verify(userService, times(1)).register(any(UserRegisterRequestDto.class));
    }

    @Test
    @DisplayName("Register: Should return 400 Bad Request when validation fails")
    void register_ShouldReturnBadRequest_WhenValidationFails() throws Exception {

        UserRegisterRequestDto requestDto = new UserRegisterRequestDto();
        requestDto.setFullName("");
        requestDto.setEmail("invalid-email");
        requestDto.setPasswordHash("short");
        requestDto.setConfirmPassword("short");

        mockMvc.perform(post("/api/v1/auth/registration")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        verify(userService, times(0)).register(any());
    }

    @Test
    @DisplayName("Register: Should return 409 Conflict when email already exists")
    void register_ShouldReturnConflict_WhenEmailExists() throws Exception {
        // Given
        UserRegisterRequestDto requestDto = new UserRegisterRequestDto();
        requestDto.setFullName("Test User");
        requestDto.setEmail("existing@example.com");
        requestDto.setPasswordHash("ValidPassword123!");
        requestDto.setConfirmPassword("ValidPassword123!");

        when(userService.register(any(UserRegisterRequestDto.class)))
                .thenThrow(new RegistrationException("User already exists"));

        mockMvc.perform(post("/api/v1/auth/registration")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict());

        verify(userService, times(1)).register(any(UserRegisterRequestDto.class));
    }

    @Test
    @DisplayName("Login: Should return 200 OK and token when credentials are valid")
    void login_ShouldReturnToken_WhenCredentialsAreValid() throws Exception {

        UserLoginRequestDto requestDto = new UserLoginRequestDto();
        requestDto.setEmail("test@example.com");
        requestDto.setPassword("ValidPassword123!");

        UserLoginResponseDto responseDto = new UserLoginResponseDto("jwt.token.string");

        when(authenticationService.authenticate(any(UserLoginRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.string"));

        verify(authenticationService, times(1)).authenticate(any(UserLoginRequestDto.class));
    }

    @Test
    @DisplayName("Login: Should return 400 Bad Request when login validation fails")
    void login_ShouldReturnBadRequest_WhenValidationFails() throws Exception {

        UserLoginRequestDto requestDto = new UserLoginRequestDto();
        requestDto.setEmail("");
        requestDto.setPassword("ValidPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, times(0)).authenticate(any());
    }
}
