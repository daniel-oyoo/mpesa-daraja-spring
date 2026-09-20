package com.daniel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import com.daniel.filter.RequestIdFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final RequestIdFilter requestIdFilter;
    private final String apiUsername;
    private final String apiPassword;

    public SecurityConfig(
      RequestIdFilter requestIdFilter,
      @Value("${security.api-username}") String apiUsername,
      @Value("${security.api-password}") String apiPassword) {
  this.requestIdFilter = requestIdFilter;
  this.apiUsername = apiUsername;
  this.apiPassword = apiPassword;
    }

    @Bean
    public SecurityFilterChain chain(HttpSecurity http) throws Exception {
  http
    .csrf(csrf -> csrf.disable())
    .cors(Customizer.withDefaults())
    .sessionManagement(session -> session.sessionCreationPolicy(
      SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(authorize -> authorize
      .requestMatchers(
        "/",
        "/index.html",
        "/app.js",
        "/style.css",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/api/v1/mpesa/callback")
      .permitAll()
      .anyRequest().authenticated())
    .httpBasic(Customizer.withDefaults())
    .headers(headers -> headers
      .contentTypeOptions(Customizer.withDefaults())
      .frameOptions(frame -> frame.deny()))
    .addFilterBefore(requestIdFilter, SecurityContextHolderFilter.class)
    .formLogin(form -> form.disable());

  return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
  return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager users(PasswordEncoder passwordEncoder) {
  UserDetails apiUser = User.withUsername(apiUsername)
    .password(passwordEncoder.encode(apiPassword))
    .roles("API")
    .build();
  return new InMemoryUserDetailsManager(apiUser);
    }
}
