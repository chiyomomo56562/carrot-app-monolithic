package com.carrot.app.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import com.carrot.app.global.security.CsrfCookieFilter;
import com.carrot.app.global.security.CustomLoginFilter;
import com.carrot.app.global.security.JwtAuthenticationFilter;
import com.carrot.app.global.security.LoginSuccessHandler;
import com.carrot.app.global.security.refreshToken.JwtUtil;

import jakarta.servlet.http.HttpServletResponse;

import com.carrot.app.global.security.CustomLogoutHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtUtil jwtUtil;
	private final AuthenticationConfiguration authenticationConfiguration;
	private final LoginSuccessHandler loginSuccessHandler;
	private final CustomLogoutHandler customLogoutHandler;
	private final MdcLoggingFilter mdcLoggingFilter;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		// CustomLoginFilter 설정
		CustomLoginFilter customLoginFilter = new CustomLoginFilter(
				authenticationManager(authenticationConfiguration));
		// successHandler 설정
		customLoginFilter.setAuthenticationSuccessHandler(loginSuccessHandler);
		// 로그인 처리 URL 설정
		customLoginFilter.setFilterProcessesUrl("/api/auth/login");

		CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
		requestHandler.setCsrfRequestAttributeName("_csrf");

		http
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfTokenRepository())
						.csrfTokenRequestHandler(requestHandler)
						.ignoringRequestMatchers("/api/auth/logout"))
				.formLogin(form -> form.disable())
				.httpBasic(httpBasic -> httpBasic.disable())
				// H2 Console
				.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				.authorizeHttpRequests(auth -> auth
						// 정적 자원
						.requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico")
						.permitAll()
						.requestMatchers("/h2-console/**").permitAll()

						// 공개 페이지 (로그인, 회원가입, 랜딩)
						.requestMatchers("/", "/users/login", "/api/auth/login", "/users/signup",
								"/api/users/signup",
								"/api/users/email-check", "/api/users/nickname-check",
								"/api/users/email-verify",
								"/api/users/refresh-token", "/api/auth/logout")
						.permitAll()

						// product
						.requestMatchers("/products/new").permitAll()

						.requestMatchers("/products", "/products/{productId}").permitAll()
						// category
						.requestMatchers("/api/categories").permitAll()
						// search
						.requestMatchers("/search", "/search/**", "/api/search/**").permitAll()
						// chat
						.requestMatchers("/ws-chat", "/ws-chat/**").permitAll()
						// Swagger UI & API Docs
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
								"/swagger-resources/**")
						.permitAll()
						// prometheus
						.requestMatchers("/actuator/**").permitAll()
						.anyRequest().authenticated())

				// MDC 필터 추가 (가장 먼저 실행)
				.addFilterBefore(mdcLoggingFilter, DisableEncodeUrlFilter.class)

				// JWT 검증 필터 추가 (CSRF 필터보다 먼저 실행하여 로그 확인 및 인증 처리)
				.addFilterBefore(new JwtAuthenticationFilter(jwtUtil), CsrfFilter.class)

				// CSRF 쿠키 갱신을 위한 필터 추가
				.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)

				// 로그인 필터 추가
				.addFilterAt(customLoginFilter, UsernamePasswordAuthenticationFilter.class)

				// 로그아웃 시 관련 쿠키 삭제
				.logout(logout -> logout
						.logoutUrl("/api/auth/logout")
						.addLogoutHandler(customLogoutHandler)
						.deleteCookies("accessToken", "refreshToken")
						.logoutSuccessUrl("/?logout"))

				// 403 Forbidden 상세 로그를 위한 설정
				.exceptionHandling(handler -> handler
						.accessDeniedHandler(accessDeniedHandler()));

		return http.build();
	}

	// 비밀번호 암호화를 위한 빈
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public CsrfTokenRepository csrfTokenRepository() {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieName("XSRF-TOKEN"); // 쿠키 이름
		// repository.setHeaderName("X-CSRF-TOKEN"); // 헤더에서 찾
		repository.setHeaderName("X-XSRF-TOKEN"); // 헤더에서 찾을 이름 (AJAX용)
		// 파라미터 이름은 기본값이 "_csrf"이므로 별도 설정 없어도 폼 전송을 인식함
		return repository;
	}

	@Bean
	public AccessDeniedHandler accessDeniedHandler() {
		return (request, response, accessDeniedException) -> {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, accessDeniedException.getMessage());
		};
	}
}
