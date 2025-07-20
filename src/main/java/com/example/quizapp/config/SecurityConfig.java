package com.example.quizapp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.example.quizapp.entity.User;
import com.example.quizapp.repository.UserRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserRepository userRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/register", "/login", "/css/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/participant/**").hasRole("PARTICIPANT")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(customSuccessHandler())
                .failureHandler(customFailureHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                System.out.println("❌ User not found: " + username);
                throw new UsernameNotFoundException("User not found: " + username);
            }

            String role = user.getRole(); // Should be ROLE_ADMIN or ROLE_PARTICIPANT
            String encodedPassword = user.getPassword();

            System.out.println("✅ Loading user from DB:");
            System.out.println("Username: " + user.getUsername());
            System.out.println("Role: " + role);
            System.out.println("Encoded Password: " + encodedPassword);

            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getUsername())
                    .password(encodedPassword)
                    .roles(role.replace("ROLE_", "")) // set role without ROLE_ prefix
                    .build();
        };
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return (request, response, authentication) -> {
            String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElseThrow(() -> new RuntimeException("No role assigned to user"));

            System.out.println("✅ Login Success:");
            System.out.println("Username: " + authentication.getName());
            System.out.println("Role: " + role);
            System.out.println("Authorities: " + authentication.getAuthorities());

            if (role.equals("ROLE_ADMIN")) {
                System.out.println("➡️ Redirecting to /admin/dashboard");
                response.sendRedirect("/admin/dashboard");
            } else if (role.equals("ROLE_PARTICIPANT")) {
                System.out.println("➡️ Redirecting to /participant/dashboard");
                response.sendRedirect("/participant/dashboard");
            } else {
                System.out.println("⚠️ Unknown role: " + role);
                response.sendRedirect("/login?error=Unknown role");
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler customFailureHandler() {
        return (request, response, exception) -> {
            String username = request.getParameter("username");
            String submittedPassword = request.getParameter("password");

            User user = userRepository.findByUsername(username);
            String encodedPassword = (user != null) ? user.getPassword() : "User not found";

            PasswordEncoder encoder = new BCryptPasswordEncoder();
            boolean passwordMatches = (user != null) && encoder.matches(submittedPassword, encodedPassword);

            System.out.println("❌ Login Failed:");
            System.out.println("Username: " + username);
            System.out.println("Submitted Password: " + submittedPassword);
            System.out.println("Encoded Password: " + encodedPassword);
            System.out.println("Password Match: " + passwordMatches);
            System.out.println("Failure Reason: " + exception.getMessage());

            response.sendRedirect("/login?error=" + exception.getMessage());
        };
    }
}
