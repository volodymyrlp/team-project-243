package travelplanner.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Do Filter: Should authenticate user when token is valid and header is correct")
    void doFilterInternal_ShouldAuthenticateUser_WhenTokenIsValid()
            throws ServletException, IOException {

        String token = "valid.jwt.token";
        String username = "user@example.com";
        UserDetails userDetails = new User(username, "password", Collections.emptyList());

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(jwtUtil.isValidToken(token)).thenReturn(true);
        when(jwtUtil.getUserName(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(userDetails, authentication.getPrincipal());

        verify(filterChain, times(1)).doFilter(request, response);
        verify(userDetailsService, times(1)).loadUserByUsername(username);
    }

    @Test
    @DisplayName("Do Filter: Should not authenticate when Authorization header is missing")
    void doFilterInternal_ShouldNotAuthenticate_WhenHeaderIsMissing()
            throws ServletException, IOException {

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil, never()).isValidToken(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Do Filter: Should not authenticate when "
            + "Authorization header does not start with Bearer")
    void doFilterInternal_ShouldNotAuthenticate_WhenHeaderDoesNotStartWithBearer()
            throws ServletException, IOException {

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic some_token_string");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil, never()).isValidToken(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Do Filter: Should not authenticate when token is invalid")
    void doFilterInternal_ShouldNotAuthenticate_WhenTokenIsInvalid()
            throws ServletException, IOException {

        String token = "invalid.jwt.token";
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(jwtUtil.isValidToken(token)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil, times(1)).isValidToken(token);
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
