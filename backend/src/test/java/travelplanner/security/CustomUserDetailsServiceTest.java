package travelplanner.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import travelplanner.entity.User;
import travelplanner.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Load User By Username: Should return UserDetails when user exists")
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {

        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hashedPassword");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails actualUserDetails = userDetailsService.loadUserByUsername(email);

        assertNotNull(actualUserDetails);
        assertEquals(email, actualUserDetails.getUsername());
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("Load User By Username: Should throw "
            + "UsernameNotFoundException when user does not exist")
    void loadUserByUsername_ShouldThrowException_WhenUserNotFound() {

        String email = "notfound@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(email)
        );

        assertEquals("Can't find user by email " + email, exception.getMessage());
        verify(userRepository, times(1)).findByEmail(email);
    }
}
