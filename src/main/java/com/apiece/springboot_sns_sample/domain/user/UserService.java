package com.apiece.springboot_sns_sample.domain.user;

import com.apiece.springboot_sns_sample.domain.media.MediaService;
import com.apiece.springboot_sns_sample.domain.media.MediaType;
import com.apiece.springboot_sns_sample.domain.media.PresignedUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MediaService mediaService;

    public User signup(String username, String password) {
        String encodedPassword = passwordEncoder.encode(password);
        User user = User.create(username, encodedPassword);
        return userRepository.save(user);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public String getProfileImageUrl(User user) {
        if (user.getProfileMediaId() == null) {
            return null;
        }
        return mediaService.getPresignedUrl(user.getProfileMediaId());
    }

    public PresignedUrl initProfileImage(Long fileSize, User user) {
        return mediaService.initMedia(MediaType.IMAGE, fileSize, user, "profiles");
    }

    public User updateProfileImage(Long mediaId, User user) {
        mediaService.mediaUploaded(mediaId, List.of(), user);

        user.updateProfileMediaId(mediaId);
        return userRepository.save(user);
    }
}
