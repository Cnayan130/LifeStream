package top.principlecreativity.lifestream.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.payload.ApiResponse;
import top.principlecreativity.lifestream.payload.ChangePasswordRequest;
import top.principlecreativity.lifestream.payload.UserIdentityAvailability;
import top.principlecreativity.lifestream.payload.UserProfile;
import top.principlecreativity.lifestream.security.CurrentUser;
import top.principlecreativity.lifestream.security.UserPrincipal;
import top.principlecreativity.lifestream.service.UserService;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/user/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserProfile> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());

        UserProfile userProfile = new UserProfile(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getBio(),
                user.getAvatarUrl()
        );

        return ResponseEntity.ok(userProfile);
    }

    @GetMapping("/user/checkUsernameAvailability")
    public ResponseEntity<UserIdentityAvailability> checkUsernameAvailability(@RequestParam String username) {
        Boolean isAvailable = !userService.existsByUsername(username);
        return ResponseEntity.ok(new UserIdentityAvailability(isAvailable));
    }

    @GetMapping("/user/checkEmailAvailability")
    public ResponseEntity<UserIdentityAvailability> checkEmailAvailability(@RequestParam String email) {
        Boolean isAvailable = !userService.existsByEmail(email);
        return ResponseEntity.ok(new UserIdentityAvailability(isAvailable));
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable String username) {
        User user = userService.getUserByUsername(username);

        UserProfile userProfile = new UserProfile(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getBio(),
                user.getAvatarUrl()
        );

        return ResponseEntity.ok(userProfile);
    }

    @PutMapping("/user/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserProfile> updateUser(@CurrentUser UserPrincipal currentUser,
                                                  @Valid @RequestBody UserProfile userProfile) {
        User user = new User();
        user.setUsername(userProfile.getUsername());
        user.setEmail(userProfile.getEmail());
        user.setBio(userProfile.getBio());
        user.setAvatarUrl(userProfile.getAvatarUrl());

        User updatedUser = userService.updateUser(currentUser.getId(), user);

        UserProfile updatedProfile = new UserProfile(
                updatedUser.getId(),
                updatedUser.getUsername(),
                updatedUser.getEmail(),
                updatedUser.getCreatedAt(),
                updatedUser.getBio(),
                updatedUser.getAvatarUrl()
        );

        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/user/password")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse> changePassword(@CurrentUser UserPrincipal currentUser,
                                                      @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser.getId(), request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(new ApiResponse(true, "Password changed successfully"));
    }
}