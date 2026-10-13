package travelplanner.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import travelplanner.dto.user.UserRegisterRequestDto;
import travelplanner.dto.user.UserRegisterResponseDto;
import travelplanner.dto.user.UserResponseDto;
import travelplanner.dto.user.UserUpdatePasswordRequestDto;
import travelplanner.dto.user.UserUpdateRequestDto;
import travelplanner.entity.User;
import travelplanner.exception.RegistrationException;
import travelplanner.mapper.UserMapper;
import travelplanner.repository.UserRepository;
import travelplanner.security.AuthenticationService;
import travelplanner.service.StorageService;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Register: Should successfully register user when email is not taken")
    void register_ShouldSuccessfullyRegisterUser_WhenEmailNotExists() throws RegistrationException {

        UserRegisterRequestDto requestDto = new UserRegisterRequestDto();
        requestDto.setEmail("test@example.com");
        requestDto.setPasswordHash("rawPassword");

        User user = new User();
        user.setEmail("test@example.com");

        User savedUser = new User();
        savedUser.setEmail("test@example.com");
        savedUser.setPasswordHash("encodedPassword");

        UserRegisterResponseDto expectedResponse = new UserRegisterResponseDto(
                UUID.randomUUID(),
                "test@example.com",
                "Test User",
                LocalDateTime.now(),
                null
        );

        when(userRepository.existsByEmail(requestDto.getEmail())).thenReturn(false);
        when(userMapper.toModel(requestDto)).thenReturn(user);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(user)).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(expectedResponse);

        UserRegisterResponseDto actualResponse = userService.register(requestDto);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse.email(), actualResponse.email());
        verify(userRepository, times(1)).existsByEmail(requestDto.getEmail());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Register: Should throw RegistrationException when email already exists")
    void register_ShouldThrowException_WhenEmailAlreadyExists() {

        UserRegisterRequestDto requestDto = new UserRegisterRequestDto();
        requestDto.setEmail("existing@example.com");

        when(userRepository.existsByEmail(requestDto.getEmail())).thenReturn(true);

        RegistrationException exception = assertThrows(
                RegistrationException.class,
                () -> userService.register(requestDto)
        );

        assertEquals("Can't register user, because user with email "
                + "existing@example.com is already exist", exception.getMessage());
        verify(userRepository, times(1)).existsByEmail(requestDto.getEmail());
        verify(userRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("Get User Info: Should return user info with"
            + " presigned avatar URL when avatar exists")
    void getUserInfo_ShouldReturnUserInfoWithAvatar_WhenAvatarUrlIsPresent() {
        
        User user = new User();
        user.setEmail("test@example.com");
        user.setAvatarUrl("avatars/user.png");

        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setEmail("test@example.com");
        responseDto.setAvatarUrl("avatars/user.png");

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(userMapper.toFullUserInfoDto(user)).thenReturn(responseDto);
        when(storageService.generateGetPresignedUrl("avatars/user.png"))
                .thenReturn("https://s3.url/presigned");

        UserResponseDto actualResponse = userService.getUserInfo();

        assertNotNull(actualResponse);
        assertEquals("https://s3.url/presigned", actualResponse.getAvatarUrl());
        verify(authenticationService, times(1)).getAuthenticatedUser();
        verify(storageService, times(1)).generateGetPresignedUrl("avatars/user.png");
    }

    @Test
    @DisplayName("Get User Info: Should return user info without"
            + " presigned URL when avatar is blank")
    void getUserInfo_ShouldReturnUserInfoWithoutAvatar_WhenAvatarUrlIsBlank() {

        User user = new User();
        user.setEmail("test@example.com");
        user.setAvatarUrl("");

        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setEmail("test@example.com");
        responseDto.setAvatarUrl("");

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(userMapper.toFullUserInfoDto(user)).thenReturn(responseDto);

        UserResponseDto actualResponse = userService.getUserInfo();

        assertNotNull(actualResponse);
        assertEquals("", actualResponse.getAvatarUrl());
        verify(storageService, times(0)).generateGetPresignedUrl(any());
    }

    @Test
    @DisplayName("Update User Info: Should update full name and avatar successfully")
    void updateUserInfo_ShouldUpdateSuccessfully() {

        UserUpdateRequestDto requestDto = new UserUpdateRequestDto();
        requestDto.setFullName("New Name");
        requestDto.setAvatarUrl("avatars/new.png");

        User user = new User();
        user.setEmail("test@example.com");

        User savedUser = new User();
        savedUser.setEmail("test@example.com");
        savedUser.setFullName("New Name");
        savedUser.setAvatarUrl("avatars/new.png");

        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setFullName("New Name");
        responseDto.setAvatarUrl("avatars/new.png");

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(userRepository.save(user)).thenReturn(savedUser);
        when(userMapper.toFullUserInfoDto(savedUser)).thenReturn(responseDto);
        when(storageService.generateGetPresignedUrl("avatars/new.png"))
                .thenReturn("https://s3.url/new-presigned");

        UserResponseDto actualResponse = userService.updateUserInfo(requestDto);

        assertNotNull(actualResponse);
        assertEquals("New Name", actualResponse.getFullName());
        assertEquals("https://s3.url/new-presigned", actualResponse.getAvatarUrl());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Update User Password: Should encode and update password successfully")
    void updateUserPassword_ShouldUpdatePasswordSuccessfully() {

        UserUpdatePasswordRequestDto requestDto = new UserUpdatePasswordRequestDto();
        requestDto.setPasswordHash("newRawPassword");

        User user = new User();
        user.setEmail("test@example.com");

        User savedUser = new User();
        savedUser.setEmail("test@example.com");
        savedUser.setPasswordHash("encodedNewPassword");

        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setEmail("test@example.com");

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(passwordEncoder.encode("newRawPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(user)).thenReturn(savedUser);
        when(userMapper.toFullUserInfoDto(savedUser)).thenReturn(responseDto);

        UserResponseDto actualResponse = userService.updateUserPassword(requestDto);

        assertNotNull(actualResponse);
        verify(passwordEncoder, times(1)).encode("newRawPassword");
        verify(userRepository, times(1)).save(user);
    }
}
