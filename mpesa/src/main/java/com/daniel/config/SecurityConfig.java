package com.daniel.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import com.daniel.filter.RequestIdFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    RequestIdFilter requestIdFilter;

    @Bean
    public SecurityFilterChain chain(HttpSecurity http) throws Exception{
      http
          .csrf(c->c.disable())
          .cors(c->{})
          .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(
            a->a.requestMatchers("/swagger-ui/index.html",
              "http://localhost:8080/h2-console"
            )
            .permitAll()
            .anyRequest().permitAll()
          )
          
          .addFilterBefore(requestIdFilter,SecurityContextHolderFilter.class)
          .formLogin(c->c.disable());

          return http.build();

    }
}
