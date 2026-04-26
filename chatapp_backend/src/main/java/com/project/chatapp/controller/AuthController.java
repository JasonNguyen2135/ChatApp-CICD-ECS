package com.project.chatapp.controller;

import com.project.chatapp.config.JwtUtils;
import com.project.chatapp.model.User;
import com.project.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> register(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> {
                    String token = jwtUtils.generateToken(user.getEmail(), user.getId());
                    return ResponseEntity.ok((Object) Map.of(
                        "token", token,
                        "userId", user.getId(),
                        "email", user.getEmail()
                    ));
                })
                .orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid credentials")));
    }

    // BỔ SUNG: API Tìm kiếm người dùng theo Email
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam("query") String query) {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getEmail().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
}
