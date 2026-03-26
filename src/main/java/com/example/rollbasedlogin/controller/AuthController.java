package com.example.rollbasedlogin.controller;



import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.example.rollbasedlogin.model.User;
import com.example.rollbasedlogin.model.LoginRequest;
import com.example.rollbasedlogin.repository.UserRepository;
import com.example.rollbasedlogin.util.JwtUtil;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")  // 🔥 This is important!
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) 
    {
        try {
            System.out.println("Registration attempt for: " + user.getEmail());
            System.out.println("User data: " + user);
            
            if (this.userRepo.findByEmail(user.getEmail()).isPresent()) {
                 return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email already exists"));
            }

            // Validate required fields
            if (user.getUsername() == null || user.getUsername().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Username is required"));
            }
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Password is required"));
            }
            if (user.getRole() == null || user.getRole().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Role is required"));
            }

            // Set default phone number if not provided
            if (user.getPhoneNumber() == null || user.getPhoneNumber().trim().isEmpty()) {
                user.setPhoneNumber("+0000000000"); // Default placeholder
            }

            user.setPassword(this.passwordEncoder.encode(user.getPassword()));
            this.userRepo.save(user);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        System.out.println("Login attempt for: " + request.getEmail());
        
        Optional<User> userOpt = this.userRepo.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            System.out.println("User not found for email: " + request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email"));
        }

        User user = userOpt.get();
        System.out.println("User found: " + user.getEmail() + ", Role: " + user.getRole());
        
        if (!this.passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            System.out.println("Invalid password for user: " + request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid password"));
        }

        String token = this.jwtUtil.generateToken(user.getEmail(), user.getRole());
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("role", user.getRole());
        response.put("email", user.getEmail());

        System.out.println("Login successful for: " + user.getEmail() + ", Token: " + token);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
}
