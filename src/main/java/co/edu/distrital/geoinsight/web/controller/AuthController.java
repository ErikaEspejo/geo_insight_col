package co.edu.distrital.geoinsight.web.controller;

import co.edu.distrital.geoinsight.application.auth.AuthenticationService;
import co.edu.distrital.geoinsight.application.auth.RegistrationService;
import co.edu.distrital.geoinsight.application.common.InvalidCredentialsException;
import co.edu.distrital.geoinsight.domain.model.Role;
import co.edu.distrital.geoinsight.domain.model.UserAccount;
import co.edu.distrital.geoinsight.web.dto.AuthResponse;
import co.edu.distrital.geoinsight.web.dto.LoginRequest;
import co.edu.distrital.geoinsight.web.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Autenticación (FR-021..FR-025). Login JSON por sesión; registro siempre con
 * rol USER; /api/auth/me devuelve el usuario autenticado.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;

    public AuthController(AuthenticationService authenticationService, RegistrationService registrationService) {
        this.authenticationService = authenticationService;
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserAccount account = registrationService.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(account));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthenticationService.AuthenticatedUser user = authenticationService.authenticate(request.username(), request.password())
                .orElseThrow(() -> new InvalidCredentialsException("Usuario o contraseña incorrectos"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.username(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return new AuthResponse(user.username(), user.role().name(), user.admin());
    }

    private AuthResponse toResponse(UserAccount account) {
        boolean admin = account.role() == Role.ADMIN;
        return new AuthResponse(account.username(), account.role().name(), admin);
    }

    @GetMapping("/me")
    public AuthResponse me(Authentication authentication) {
        String username = authentication.getName();
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + Role.ADMIN.name()));
        return new AuthResponse(username, admin ? Role.ADMIN.name() : Role.USER.name(), admin);
    }
}
