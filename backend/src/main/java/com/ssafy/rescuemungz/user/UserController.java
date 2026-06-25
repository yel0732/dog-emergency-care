package com.ssafy.rescuemungz.user;

import com.ssafy.rescuemungz.auth.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @GetMapping("/availability")
    public Map<String, Boolean> availability(@RequestParam(required = false) String loginId,
                                             @RequestParam(required = false) String email) {
        return userService.availability(loginId, email);
    }

    @GetMapping("/me")
    public UserResponse me() {
        return userService.find(AuthUtil.requiredUserId());
    }

    @PutMapping("/me")
    public UserResponse updateMe(@Valid @RequestBody UserUpdateRequest request) {
        return userService.update(AuthUtil.requiredUserId(), request);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe() {
        long userId = AuthUtil.requiredUserId();
        userService.deactivate(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{targetId}/follow")
    public UserFollowResponse follow(@PathVariable long targetId) {
        return userService.follow(AuthUtil.requiredUserId(), targetId);
    }

    @DeleteMapping("/{targetId}/follow")
    public UserFollowResponse unfollow(@PathVariable long targetId) {
        return userService.unfollow(AuthUtil.requiredUserId(), targetId);
    }

    @GetMapping("/{targetId}/follow")
    public UserFollowResponse followStatus(@PathVariable long targetId) {
        return userService.followStatus(AuthUtil.requiredUserId(), targetId);
    }

    @GetMapping("/me/followers")
    public List<UserFollowResponse> followers() {
        return userService.followers(AuthUtil.requiredUserId());
    }

    @GetMapping("/me/following")
    public List<UserFollowResponse> following() {
        return userService.following(AuthUtil.requiredUserId());
    }
}
