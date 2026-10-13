package travelplanner.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import travelplanner.entity.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Find By Email: Should return user when email exists")
    void findByEmail_ShouldReturnUser_WhenEmailExists() {

        String email = "repository.test@example.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hash123");
        user.setFullName("Repository Test User");
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        Optional<User> foundUserOpt = userRepository.findByEmail(email);

        assertTrue(foundUserOpt.isPresent());
        assertEquals(email, foundUserOpt.get().getEmail());
        assertEquals("Repository Test User", foundUserOpt.get().getFullName());
    }

    @Test
    @DisplayName("Find By Email: Should return empty Optional when email does not exist")
    void findByEmail_ShouldReturnEmpty_WhenEmailNotExists() {

        String email = "nonexistent@example.com";

        Optional<User> foundUserOpt = userRepository.findByEmail(email);

        assertTrue(foundUserOpt.isEmpty());
    }

    @Test
    @DisplayName("Exists By Email: Should return true when email exists")
    void existsByEmail_ShouldReturnTrue_WhenEmailExists() {

        String email = "exists.test@example.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hash123");
        user.setFullName("Repository Test User");
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        boolean exists = userRepository.existsByEmail(email);

        assertTrue(exists);
    }

    @Test
    @DisplayName("Exists By Email: Should return false when email does not exist")
    void existsByEmail_ShouldReturnFalse_WhenEmailNotExists() {

        String email = "not.exists@example.com";

        boolean exists = userRepository.existsByEmail(email);

        assertFalse(exists);
    }
}
