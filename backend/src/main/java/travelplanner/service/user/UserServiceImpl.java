package travelplanner.service.user;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;

    @Override
    public UserRegisterResponseDto register(UserRegisterRequestDto requestDto)
            throws RegistrationException {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new RegistrationException("Can't register user, because user with email "
                    + requestDto.getEmail() + " is already exist");
        }
        User user = userMapper.toModel(requestDto);
        user.setCreatedAt(LocalDateTime.now());
        user.setPasswordHash(passwordEncoder.encode(requestDto.getPasswordHash()));
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserResponseDto getUserInfo() {
        User user = authenticationService.getAuthenticatedUser();
        return userMapper.toFullUserInfoDto(user);
    }

    @Override
    public UserResponseDto updateUserInfo(UserUpdateRequestDto requestDto) {
        User user = authenticationService.getAuthenticatedUser();
        user.setFullName(requestDto.getFullName());
        if (requestDto.getAvatarUrl() != null) {
            user.setAvatarUrl(requestDto.getAvatarUrl());
        }
        return userMapper.toFullUserInfoDto(userRepository.save(user));
    }

    @Override
    public UserResponseDto updateUserPassword(UserUpdatePasswordRequestDto requestDto) {
        User user = authenticationService.getAuthenticatedUser();
        user.setPasswordHash(passwordEncoder.encode(requestDto.getPasswordHash()));
        return userMapper.toFullUserInfoDto(userRepository.save(user));
    }
}
