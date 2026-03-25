package com.example.rollbasedlogin.config;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }
}

// ✅ @Configuration
// This annotation is part of Spring's core and tells the Spring container:

// “Hey Spring, this class is meant for configuration – it contains beans you need to manage.”

// 📌 Think of it like this:
// It's like telling Spring:

// “Please scan this class for beans and settings I want to configure manually.”

// java
// Copy
// Edit
// @Configuration
// public class SecurityConfig {
//     // contains configuration code
// }
// 🧠 Without this, Spring won’t treat the class as a configuration source and won’t read any @Bean methods inside.

// ✅ @Bean
// This is used inside a @Configuration class and tells Spring:

// “Create and manage this object (return value) as a bean in the application context.”

// java
// Copy
// Edit
// @Bean
// public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//    // Spring will manage the returned SecurityFilterChain object as a bean
// }
// 🧠 Basically:

// Spring will execute this method once during startup.

// The returned object (e.g., SecurityFilterChain) will be added to the Spring container (so other parts of your app can use it).

// Summary in Telugu Style 😄
// text
// Copy
// Edit
// @Configuration ante -> "Ee class lo Spring ki kavalsina configuration undi, chusko ra babu"

// @Bean ante -> "Ee method return chesina object ni Spring container lo petti manage cheyyi"
// So when you write:

// java
// Copy
// Edit
// @Configuration
// public class SecurityConfig {
//     @Bean
//     public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//         // configure & return the bean
//     }
// }
// You’re telling Spring:

// “Store this security config bean and use it wherever needed.”