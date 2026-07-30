package travelplanner.service.user;

import travelplanner.dto.user.UserRegisterRequestDto;
import travelplanner.dto.user.UserRegisterResponseDto;
import travelplanner.exception.RegistrationException;

public interface UserService {
    UserRegisterResponseDto register(UserRegisterRequestDto requestDto)
            throws RegistrationException;

    UserRegisterResponseDto getUserInfo();
}
