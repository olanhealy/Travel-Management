package com.tms.usermanagement.controller;

import com.tms.usermanagement.model.User;
import com.tms.usermanagement.repository.UserRepository;
import com.tms.usermanagement.service.UserRegistrationService;
import com.tms.usermanagement.service.UserLoginService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRegistrationService userRegistrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLoginService userLoginService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        if (user.getPassword() == null || !user.getPassword().equals(user.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("Password and Confirm Password must match.");
        }
        try {
            userRegistrationService.registerUser(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPassword()
            );
            return ResponseEntity.ok("User registered successfully. Please check your email for verification.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login/google")
    public ResponseEntity<Map<String, String>> loginWithGoogle(@RequestBody Map<String, String> googleTokenMap) {
        try {
            String googleToken = googleTokenMap.get("googleToken");
            User user = userLoginService.loginWithGoogle(googleToken);
            String token = generateToken(user);
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("role", user.getRole().getRoleName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Google login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody User loginRequest) {
        try {
            User user = userLoginService.loginWithEmailPassword(loginRequest.getEmail(), loginRequest.getPassword());
            String token = generateToken(user);
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("role", user.getRole().getRoleName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey("your-secret-key")
                    .parseClaimsJws(token)
                    .getBody();
            String email = claims.getSubject();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setEmailVerified(true);
                userRepository.save(user);
                return ResponseEntity.ok("Email verified successfully!");
            } else {
                return ResponseEntity.badRequest().body("Invalid verification token.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Email verification failed: " + e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            if (request.getOldPassword() == null || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Current password is incorrect.");
            }
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("New password cannot be empty.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok("Password changed successfully.");
    }


    private String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().getRoleName())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(SignatureAlgorithm.HS512, "your-secret-key")
                .compact();
    }

    public static class ChangePasswordRequest {
        private String oldPassword;  
        private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }
        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }
        public String getNewPassword() {
            return newPassword;
        }
        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
