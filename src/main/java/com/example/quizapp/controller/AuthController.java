package com.example.quizapp.controller;

import com.example.quizapp.entity.User;
import com.example.quizapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username, @RequestParam String password, @RequestParam String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role); // Save the selected role (ROLE_PARTICIPANT or ROLE_ADMIN)
        userRepository.save(user);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(username);
        message.setSubject("Welcome to QuizApp!");
        message.setText("Thank you for registering with QuizApp! You are registered as a " + role.replace("ROLE_", "") + ".");
        mailSender.send(message);

        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst().map(Object::toString).orElse("ROLE_PARTICIPANT");
        if (role.equals("ROLE_ADMIN")) {
            return "redirect:/admin/dashboard";
        } else {
            return "redirect:/participant/dashboard";
        }
    }
}