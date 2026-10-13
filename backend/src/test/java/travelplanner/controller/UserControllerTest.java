package travelplanner.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import travelplanner.dto.user.UserResponseDto;
import travelplanner.dto.user.UserUpdatePasswordRequestDto;
import travelplanner.dto.user.UserUpdateRequestDto;
import travelplanner.security.JwtUtil;
import travelplanner.service.user.UserService;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("Get User Info: Should return 200 OK and user details")
    void getUserInfo_ShouldReturnUserResponse_WhenAuthenticated() throws Exception {

        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setEmail("test@example.com");
        responseDto.setFullName("Test User");

        when(userService.getUserInfo()).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"));

        verify(userService, times(1)).getUserInfo();
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("Update User Info: Should return 200 OK when request is valid")
    void updateUserInfo_ShouldReturnUpdatedUser_WhenRequestIsValid() throws Exception {

        UserUpdateRequestDto requestDto = new UserUpdateRequestDto();
        requestDto.setFullName("Updated Name");
        requestDto.setAvatarUrl("avatars/new.png");

        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setEmail("test@example.com");
        responseDto.setFullName("Updated Name");
        responseDto.setAvatarUrl("avatars/new.png");

        when(userService.updateUserInfo(any(UserUpdateRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/v1/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.avatarUrl").value("avatars/new.png"));

        verify(userService, times(1)).updateUserInfo(any(UserUpdateRequestDto.class));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("Update User Password: Should return 200 OK when request is valid")
    void updateUserPassword_ShouldReturnSuccess_WhenRequestIsValid() throws Exception {

        UserUpdatePasswordRequestDto requestDto = new UserUpdatePasswordRequestDto();
        requestDto.setPasswordHash("newSecurePassword123");
        requestDto.setConfirmPassword("newSecurePassword123");

        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setEmail("test@example.com");

        when(userService.updateUserPassword(any(UserUpdatePasswordRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/api/v1/users/me/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService, times(1)).updateUserPassword(any(UserUpdatePasswordRequestDto.class));
    }
}
