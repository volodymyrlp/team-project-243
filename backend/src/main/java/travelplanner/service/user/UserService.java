package travelplanner.service.user;

import travelplanner.dto.user.UserRegisterRequestDto;
import travelplanner.dto.user.UserRegisterResponseDto;
import travelplanner.dto.user.UserResponseDto;
import travelplanner.dto.user.UserUpdatePasswordRequestDto;
import travelplanner.dto.user.UserUpdateRequestDto;
import travelplanner.exception.RegistrationException;

public interface UserService {
    UserRegisterResponseDto register(UserRegisterRequestDto requestDto)
            throws RegistrationException;

    UserResponseDto getUserInfo();

    UserResponseDto updateUserInfo(UserUpdateRequestDto requestDto);

    UserResponseDto updateUserPassword(UserUpdatePasswordRequestDto requestDto);
}
