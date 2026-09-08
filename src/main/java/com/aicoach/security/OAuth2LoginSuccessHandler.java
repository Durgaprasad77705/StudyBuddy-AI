package com.aicoach.security;

import com.aicoach.entity.Profile;
import com.aicoach.entity.User;
import com.aicoach.entity.UserGamification;
import com.aicoach.repository.ProfileRepository;
import com.aicoach.repository.UserGamificationRepository;
import com.aicoach.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserGamificationRepository gamificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.frontend.base-url:http://localhost:8099}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oauth = (OAuth2User) authentication.getPrincipal();
        String email = first(oauth, "email");
        String name = first(oauth, "name");
        String provider = request.getRequestURI().toLowerCase().contains("github") ? "GITHUB" : "GOOGLE";

        if (email == null || email.isBlank()) {
            String login = first(oauth, "login");
            if (login != null && !login.isBlank()) email = login + "@github.oauth";
        }
        if (email == null || email.isBlank()) {
            response.sendRedirect(frontendBaseUrl + "/?oauthError=" + enc("No email was returned by the provider"));
            return;
        }

        final String normalizedEmail = email.toLowerCase();
        final String finalName = name;
        User user = userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            User u = User.builder()
                    .name(finalName == null || finalName.isBlank() ? normalizedEmail.split("@")[0] : finalName)
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .authProvider(provider)
                    .build();
            u = userRepository.save(u);
            profileRepository.save(Profile.builder().user(u).build());
            gamificationRepository.save(UserGamification.builder().user(u).build());
            return u;
        });

        if (user.getAuthProvider() == null || "LOCAL".equalsIgnoreCase(user.getAuthProvider())) {
            user.setAuthProvider(provider);
            userRepository.save(user);
        }

        String token = jwtService.generateToken(user.getEmail(), user.getId());
        String target = frontendBaseUrl + "/?oauthToken=" + enc(token)
                + "&oauthName=" + enc(user.getName())
                + "&oauthEmail=" + enc(user.getEmail()) + "#/dashboard";
        response.sendRedirect(target);
    }

    private static String first(OAuth2User u, String key) {
        Object v = u.getAttributes().get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
