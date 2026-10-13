package travelplanner.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private static final String TEST_SECRET = "mySecretKeyForTestingPurposesShouldBeLongEnough123";
    private static final Long TEST_EXPIRATION = 3600000L;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil(TEST_SECRET);

        Field expirationField = JwtUtil.class.getDeclaredField("expiration");
        expirationField.setAccessible(true);
        expirationField.set(jwtUtil, TEST_EXPIRATION);
    }

    @Test
    @DisplayName("Generate Token: Should generate valid token for username")
    void generateToken_ShouldReturnValidToken() {

        String username = "test@example.com";

        String token = jwtUtil.generateToken(username);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtUtil.isValidToken(token));
    }

    @Test
    @DisplayName("Get Username: Should extract correct username from token")
    void getUserName_ShouldReturnCorrectUsername() {

        String username = "user@example.com";
        String token = jwtUtil.generateToken(username);

        String extractedUsername = jwtUtil.getUserName(token);

        assertEquals(username, extractedUsername);
    }

    @Test
    @DisplayName("Is Valid Token: Should return true for valid token")
    void isValidToken_ShouldReturnTrue_WhenTokenIsValid() {

        String token = jwtUtil.generateToken("validUser");

        boolean isValid = jwtUtil.isValidToken(token);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Is Valid Token: Should return false for invalid (malformed) token")
    void isValidToken_ShouldReturnFalse_WhenTokenIsMalformed() {

        String invalidToken = "invalid.token.string";

        boolean isValid = jwtUtil.isValidToken(invalidToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Is Valid Token: Should return false for expired token")
    void isValidToken_ShouldReturnFalse_WhenTokenIsExpired() throws Exception {

        Key key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .setSubject("expiredUser")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000L))
                .setExpiration(new Date(System.currentTimeMillis() - 3600000L))
                .signWith(key)
                .compact();

        boolean isValid = jwtUtil.isValidToken(expiredToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Is Valid Token: Should return false for token signed with different key")
    void isValidToken_ShouldReturnFalse_WhenSignatureIsInvalid() {

        String differentSecret = "anotherSecretKeyForTestingPurposesMustBeLongEnough987654";
        Key differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));

        String foreignToken = Jwts.builder()
                .setSubject("hacker")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION))
                .signWith(differentKey)
                .compact();

        boolean isValid = jwtUtil.isValidToken(foreignToken);

        assertFalse(isValid);
    }
}
