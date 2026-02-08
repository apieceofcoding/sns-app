package com.apiece.springboot_sns_sample.config.auth;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.util.List;

@Configuration
@EnableRedisIndexedHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SecurityConfig implements BeanClassLoaderAware {

    private ClassLoader classLoader;

    @Override
    public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry, AuthSuccessHandler authSuccessHandler) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(form -> form
                        .loginProcessingUrl("/api/v1/login") // POST, Content-Type: application/x-www-form-urlencoded
                        .successHandler(authSuccessHandler)
                        .failureHandler((request, response, ex) -> response.setStatus(HttpStatus.UNAUTHORIZED.value()))
                )
                .logout(logout -> logout
                        .logoutUrl("/api/v1/logout") // POST
                        .logoutSuccessHandler(((request, response, authentication) -> response.setStatus(HttpStatus.OK.value())))
                        .invalidateHttpSession(true)
                )
                .sessionManagement(session -> session
                        .maximumSessions(2)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/users/signup",
                                "/api/v1/login",
                                "/api/v1/sessions"
                        ).permitAll()
                        .anyRequest().authenticated()
                ).exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public SessionRegistry sessionRegistry(FindByIndexNameSessionRepository<?> sessionRepository) {
        return new SpringSessionBackedSessionRegistry<>(sessionRepository);
    }

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        var typeValidatorBuilder = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("org.springframework.session.")
                .allowIfSubType("com.apiece.");

        JsonMapper jsonMapper = JsonMapper.builder()
                .addModules(SecurityJacksonModules.getModules(classLoader, typeValidatorBuilder))
                .build();

        return new JacksonJsonRedisSerializer<>(jsonMapper, Object.class);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
