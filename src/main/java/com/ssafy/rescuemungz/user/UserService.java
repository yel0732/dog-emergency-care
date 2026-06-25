package com.ssafy.rescuemungz.user;

import com.ssafy.rescuemungz.common.ConflictException;
import com.ssafy.rescuemungz.common.NotFoundException;
import com.ssafy.rescuemungz.common.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class UserService {
    private static final String DEFAULT_PROFILE_IMAGE_URL = "/choco-profile.png";
    private static final int MAX_PROFILE_IMAGE_URL_LENGTH = 1_200_000;
    private static final Pattern DATA_IMAGE_PATTERN = Pattern.compile("^data:image/(png|jpeg|jpg|webp|gif);base64,[A-Za-z0-9+/=\\r\\n]+$");
    private static final Pattern SAFE_IMAGE_URL_PATTERN = Pattern.compile("^(https?://[^\\s]+|/[^\\s]*)$");

    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, FollowMapper followMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.followMapper = followMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        String loginId = request.loginId().trim();
        String email = request.email().trim().toLowerCase();
        String name = request.name().trim();
        String nickname = request.nickname().trim();
        validatePasswordPolicy(request.password(), loginId, email, name, nickname);
        if (userMapper.findByLoginId(loginId) != null) {
            throw new ConflictException("이미 사용 중인 아이디입니다.");
        }
        if (userMapper.findByEmail(email) != null) {
            throw new ConflictException("이미 사용 중인 이메일입니다.");
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User();
        user.setLoginId(loginId);
        user.setPassword(encodedPassword);
        user.setPasswordHash(encodedPassword);
        user.setName(name);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setProfileImageUrl(DEFAULT_PROFILE_IMAGE_URL);
        user.setRole("USER");
        userMapper.insert(user);
        return UserResponse.from(findUser(user.getId()));
    }

    public Map<String, Boolean> availability(String loginId, String email) {
        boolean loginIdAvailable = true;
        boolean emailAvailable = true;
        if (loginId != null && !loginId.isBlank()) {
            loginIdAvailable = userMapper.findByLoginId(loginId.trim()) == null;
        }
        if (email != null && !email.isBlank()) {
            emailAvailable = userMapper.findByEmail(email.trim().toLowerCase(Locale.ROOT)) == null;
        }
        return Map.of("loginIdAvailable", loginIdAvailable, "emailAvailable", emailAvailable);
    }

    public UserResponse find(long id) {
        return UserResponse.from(findUser(id));
    }

    @Transactional
    public UserResponse update(long id, UserUpdateRequest request) {
        User user = findUser(id);
        String email = request.email().trim().toLowerCase();
        String name = request.name().trim();
        String nickname = request.nickname().trim();
        User emailOwner = userMapper.findByEmail(email);
        if (emailOwner != null && !emailOwner.getId().equals(id)) {
            throw new ConflictException("이미 사용 중인 이메일입니다.");
        }
        if (request.password() != null && !request.password().isBlank()) {
            validatePasswordPolicy(request.password(), user.getLoginId(), email, name, nickname);
            String encodedPassword = passwordEncoder.encode(request.password());
            user.setPassword(encodedPassword);
            user.setPasswordHash(encodedPassword);
        }
        user.setName(name);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setProfileImageUrl(normalizeProfileImageUrl(request.profileImageUrl()));
        if (userMapper.update(user) == 0) {
            throw new NotFoundException("회원을 찾을 수 없습니다.");
        }
        return find(id);
    }

    @Transactional
    public void deactivate(long id) {
        if (userMapper.deactivate(id) == 0) {
            throw new NotFoundException("회원을 찾을 수 없습니다.");
        }
    }

    @Transactional
    public UserFollowResponse follow(long followerId, long followingId) {
        assertFollowTarget(followerId, followingId);
        followMapper.follow(followerId, followingId);
        return followStatus(followerId, followingId);
    }

    @Transactional
    public UserFollowResponse unfollow(long followerId, long followingId) {
        assertFollowTarget(followerId, followingId);
        followMapper.unfollow(followerId, followingId);
        return followStatus(followerId, followingId);
    }

    public UserFollowResponse followStatus(long viewerId, long targetId) {
        User target = findUser(targetId);
        return new UserFollowResponse(
                target.getId(),
                target.getNickname(),
                target.getProfileImageUrl(),
                followMapper.countFollowers(targetId),
                followMapper.countFollowing(targetId),
                followMapper.isFollowing(viewerId, targetId),
                null
        );
    }

    public List<UserFollowResponse> followers(long userId) {
        findUser(userId);
        return followMapper.findFollowers(userId);
    }

    public List<UserFollowResponse> following(long userId) {
        findUser(userId);
        return followMapper.findFollowing(userId);
    }

    @Transactional
    public UserResponse login(LoginRequest request) {
        User user = userMapper.findByLoginId(request.loginId().trim());
        if (user == null || !Boolean.TRUE.equals(user.getActive()) || !isPasswordMatch(request.password(), user)) {
            throw new UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return UserResponse.from(user);
    }

    private boolean isPasswordMatch(String rawPassword, User user) {
        String storedPassword = user.getPasswordHash() != null ? user.getPasswordHash() : user.getPassword();
        return isBcryptHash(storedPassword) && passwordEncoder.matches(rawPassword, storedPassword);
    }

    private boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    private String normalizeProfileImageUrl(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_PROFILE_IMAGE_URL;
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_PROFILE_IMAGE_URL_LENGTH) {
            throw new IllegalArgumentException("프로필 이미지는 900KB 이하 파일만 사용할 수 있습니다.");
        }
        if (!SAFE_IMAGE_URL_PATTERN.matcher(trimmed).matches() && !DATA_IMAGE_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("프로필 이미지는 /로 시작하는 경로, http(s) URL, data:image 값만 사용할 수 있습니다.");
        }
        return trimmed;
    }

    private void validatePasswordPolicy(String password, String loginId, String email, String name, String nickname) {
        String lowerPassword = password.toLowerCase(Locale.ROOT);
        String lowerLoginId = safeLower(loginId);
        if (!lowerLoginId.isBlank() && lowerPassword.contains(lowerLoginId)) {
            throw new IllegalArgumentException("비밀번호는 아이디를 포함할 수 없습니다.");
        }
        if (containsMeaningfulPart(lowerPassword, name) || containsMeaningfulPart(lowerPassword, nickname)) {
            throw new IllegalArgumentException("비밀번호는 이름이나 닉네임을 포함할 수 없습니다.");
        }
    }

    private boolean containsMeaningfulPart(String lowerPassword, String value) {
        String normalized = safeLower(value).replace(" ", "");
        return normalized.length() >= 3 && lowerPassword.contains(normalized);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private User findUser(long id) {
        User user = userMapper.findById(id);
        if (user == null || !Boolean.TRUE.equals(user.getActive())) {
            throw new NotFoundException("회원을 찾을 수 없습니다.");
        }
        return user;
    }

    private void assertFollowTarget(long followerId, long followingId) {
        if (followerId == followingId) {
            throw new IllegalArgumentException("자기 자신은 팔로우할 수 없습니다.");
        }
        findUser(followerId);
        findUser(followingId);
    }
}
