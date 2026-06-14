package com.scheduler.config;

import com.scheduler.model.User;
import com.scheduler.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // This is the key addition
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Step 1: Enable CORS with our custom configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Step 2: Disable CSRF (we're using Basic Auth, not session-based)
                .csrf(csrf -> csrf.disable())

                // Step 3: Allow H2 console for development (debugging)
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))

                // Step 4: Authorization rules - ORDER MATTERS!
                .authorizeHttpRequests(auth -> auth
                        // Preflight requests (OPTIONS) always allowed - needed for CORS
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                        // H2 console for development
                        .requestMatchers("/h2-console/**").permitAll()

                        // Login endpoint should be accessible
                        .requestMatchers("/login").permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // Step 5: Use Basic Authentication (username:password in request)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    /**
     * CORS Configuration Source
     *
     * This bean configures Cross-Origin Resource Sharing for the application.
     * It tells the Spring Boot backend which frontend origins are allowed to access it.
     *
     * Flow when browser makes cross-origin request:
     * 1. Browser sees origin is different (http://localhost:3000 != http://localhost:8080)
     * 2. Browser sends preflight OPTIONS request
     * 3. Spring checks this configuration
     * 4. If origin allowed, returns CORS headers
     * 5. Browser allows actual request (GET, POST, etc.)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed Origins: Which frontend URLs can access this backend
        // For local development: http://localhost:3000
        // For production: would be https://yourdomain.com
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000"     // Local Node.js frontend
            // "https://yourdomain.com"  // Production frontend (add when deploying)
        ));

        // Allowed Methods: Which HTTP verbs are allowed from browser
        // Note: OPTIONS always works (used for preflight checks)
        configuration.setAllowedMethods(Arrays.asList(
            "GET",      // Fetch data
            "POST",     // Send data, login
            "PUT",      // Update data
            "DELETE",   // Remove data
            "OPTIONS"   // Preflight checks
        ));

        // Allowed Headers: Which request headers the browser can send
        // "*" means allow any header (Content-Type, Authorization, etc.)
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Expose Headers: Which response headers the browser can access
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Total-Count"  // For pagination info
        ));

        // Allow Credentials: Important for Basic Auth and cookies
        // true = Include credentials (username:password) with requests
        // false = Don't include credentials
        // MUST be true if using Basic Auth or sessions
        configuration.setAllowCredentials(true);

        // Max Age: How long preflight result is cached (in seconds)
        // 3600 = 1 hour - browser won't ask for preflight again within 1 hour
        configuration.setMaxAge(3600L);

        // Register this configuration for all path patterns
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);  // "/**" = all routes
        return source;
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository repo) {
        return username -> {
            User user = repo.findByUsername(username).orElseThrow();
            return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                    .password(user.getPassword())
                    .authorities(user.getRole().getName()) // Use role name from Role entity
                    .build();
        };
    }

}