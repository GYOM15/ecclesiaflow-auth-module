package com.ecclesiaflow.springsecurity.web.exception.advice;

import com.ecclesiaflow.springsecurity.business.exceptions.MemberNotFoundException;
import com.ecclesiaflow.springsecurity.web.exception.*;
import com.ecclesiaflow.springsecurity.web.exception.model.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.Path;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler - unit tests")
class GlobalExceptionHandlerTest {

    private HttpServletRequest mockRequest(String path) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn(path);
        return req;
    }

    @Test
    @DisplayName("handleMemberNotFound -> 404 NOT FOUND")
    void handleMemberNotFound_shouldReturn404() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api/members/123");

        ResponseEntity<ApiErrorResponse> resp = handler.handleMemberNotFound(
                new MemberNotFoundException("Member not found"), req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().message()).isEqualTo("Member not found");
        assertThat(resp.getBody().path()).isEqualTo("/api/members/123");
    }

    @Test
    @DisplayName("handleInvalidToken -> 401 UNAUTHORIZED")
    void handleInvalidToken_shouldReturn401() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api/auth");

        ResponseEntity<ApiErrorResponse> resp = handler.handleInvalidToken(
                new InvalidTokenException("invalid"), req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().errors()).isNotEmpty();
    }

    @Test
    @DisplayName("handleValidation -> covers ObjectError branch (non-FieldError)")
    void handleValidation_shouldCoverObjectErrorBranch() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api/auth");

        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "authRequest");
        // Add a global ObjectError (not a FieldError) to exercise the else branch
        binding.addError(new ObjectError("authRequest", "global error"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, binding);

        ResponseEntity<ApiErrorResponse> resp = handler.handleValidation(ex, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().errors()).isNotEmpty();
    }

    @Test
    @DisplayName("handleInvalidCredentials(InvalidCredentialsException) -> 401 UNAUTHORIZED")
    void handleInvalidCredentials_custom_shouldReturn401() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api/auth/login");

        ResponseEntity<ApiErrorResponse> resp = handler.handleInvalidCredentials(
                new InvalidCredentialsException("bad creds"), req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().message()).contains("Identifiants invalides");
    }

    @Test
    @DisplayName("handleBadCredentials(BadCredentialsException) -> 401 UNAUTHORIZED")
    void handleBadCredentials_shouldReturn401() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api/auth/login");

        ResponseEntity<ApiErrorResponse> resp = handler.handleBadCredentials(
                new BadCredentialsException("bad creds"), req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().message()).contains("Identifiants invalides");
    }

    @Test
    @DisplayName("handleAuthentication(AuthenticationException) -> 401 UNAUTHORIZED")
    void handleAuthentication_shouldReturn401() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api/auth/any");

        ResponseEntity<ApiErrorResponse> resp = handler.handleAuthentication(
                new AuthenticationCredentialsNotFoundException("missing"), req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().message()).contains("Erreur d'authentification");
    }

    @Test
    @DisplayName("handleInvalidRequest -> 400 BAD REQUEST")
    void handleInvalidRequest_shouldReturn400() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api/auth");

        ResponseEntity<ApiErrorResponse> resp = handler.handleInvalidRequest(
                new InvalidRequestException("Bad payload"), req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().message()).isEqualTo("Bad payload");
    }

    @Test
    @DisplayName("handleJwtProcessing -> 401 UNAUTHORIZED")
    void handleJwtProcessing_shouldReturn401() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api/auth");

        ResponseEntity<ApiErrorResponse> resp = handler.handleJwtProcessing(
                new JwtProcessingException("error"), req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    @DisplayName("handleInvalidJson -> 400 BAD REQUEST")
    void handleInvalidJson_shouldReturn400() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api/auth");

        ResponseEntity<ApiErrorResponse> resp = handler.handleInvalidJson(
                new HttpMessageNotReadableException("bad json"), req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().errors()).isNotEmpty();
    }

    @Test
    @DisplayName("handleValidation -> 400 BAD REQUEST with field errors")
    void handleValidation_shouldReturn400_withErrors() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api/auth");

        // Prepare a binding result with one FieldError
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "authRequest");
        binding.addError(new FieldError("authRequest", "email", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, binding);

        ResponseEntity<ApiErrorResponse> resp = handler.handleValidation(ex, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().errors()).isNotEmpty();
    }

    @Test
    @DisplayName("handleConstraintViolation -> 400 BAD REQUEST")
    void handleConstraintViolation_shouldReturn400_andCoverBranches() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api/auth");

        // Mock violation 1: invalidValue non-null and groups attribute present
        ConstraintViolation<?> v1 = mock(ConstraintViolation.class);
        ConstraintDescriptor<?> cd1 = mock(ConstraintDescriptor.class);
        Annotation ann1 = new Annotation() { @Override public Class<? extends Annotation> annotationType() { return jakarta.validation.constraints.NotNull.class; } };
        Map<String, Object> attrs1 = new HashMap<>();
        attrs1.put("groups", java.util.List.of("G1"));
        Path path1 = mock(Path.class);
        when(path1.toString()).thenReturn("field1");
        when(v1.getMessage()).thenReturn("msg1");
        when(v1.getPropertyPath()).thenReturn(path1);
        when(v1.getInvalidValue()).thenReturn("val1");
        org.mockito.Mockito.doReturn(ann1).when(cd1).getAnnotation();
        when(cd1.getAttributes()).thenReturn(attrs1);
        org.mockito.Mockito.doReturn(cd1).when(v1).getConstraintDescriptor();

        // Mock violation 2: invalidValue null and groups attribute absent
        ConstraintViolation<?> v2 = mock(ConstraintViolation.class);
        ConstraintDescriptor<?> cd2 = mock(ConstraintDescriptor.class);
        Annotation ann2 = new Annotation() { @Override public Class<? extends Annotation> annotationType() { return jakarta.validation.constraints.Size.class; } };
        Map<String, Object> attrs2 = new HashMap<>();
        Path path2 = mock(Path.class);
        when(path2.toString()).thenReturn("field2");
        when(v2.getMessage()).thenReturn("msg2");
        when(v2.getPropertyPath()).thenReturn(path2);
        when(v2.getInvalidValue()).thenReturn(null);
        org.mockito.Mockito.doReturn(ann2).when(cd2).getAnnotation();
        when(cd2.getAttributes()).thenReturn(attrs2);
        org.mockito.Mockito.doReturn(cd2).when(v2).getConstraintDescriptor();

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(v1, v2));
        ResponseEntity<ApiErrorResponse> resp = handler.handleConstraintViolation(ex, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().errors()).isNotEmpty();
    }

    @Test
    @DisplayName("handleMissingRequestHeader -> 400 BAD REQUEST")
    void handleMissingHeader_shouldReturn400() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api/auth");

        org.springframework.core.MethodParameter mp = org.mockito.Mockito.mock(org.springframework.core.MethodParameter.class);
        MissingRequestHeaderException ex = new MissingRequestHeaderException("Authorization", mp);
        ResponseEntity<ApiErrorResponse> resp = handler.handleMissingRequestHeader(ex, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().message()).contains("En-tête requis manquant");
    }

    @Test
    @DisplayName("handleAccessDenied -> 403 FORBIDDEN")
    void handleAccessDenied_shouldReturn403() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api/secure");

        ResponseEntity<ApiErrorResponse> resp = handler.handleAccessDenied(new AccessDeniedException("denied"), req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    @DisplayName("handleIllegalArgument -> 400 BAD REQUEST")
    void handleIllegalArgument_shouldReturn400() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api");

        ResponseEntity<ApiErrorResponse> resp = handler.handleIllegalArgument(new IllegalArgumentException("bad"), req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("handleRuntimeException -> 500 INTERNAL SERVER ERROR")
    void handleRuntime_shouldReturn500() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api");

        ResponseEntity<ApiErrorResponse> resp = handler.handleRuntimeException(new RuntimeException("oops"), req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("handleAll -> 500 INTERNAL SERVER ERROR")
    void handleAll_shouldReturn500() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mockRequest("/api");

        ResponseEntity<ApiErrorResponse> resp = handler.handleAll(new Exception("oops"), req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("handleNotFoundExceptions -> 404 NOT FOUND")
    void handleNoResourceFound_shouldReturn404() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ServletWebRequest webRequest = Mockito.mock(ServletWebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/missing");

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getRequestURI()).thenReturn("/missing");
        
        ResponseEntity<ApiErrorResponse> resp = handler.handleNotFoundExceptions(
                new NoHandlerFoundException("GET", "/missing", new HttpHeaders()), mockRequest);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().path()).isEqualTo("/missing");
    }

    // ====================================================================
    // Tests simples d'instanciation des exceptions du package web.exception
    // ====================================================================

    @Test
    @DisplayName("InvalidCredentialsException conserve message et cause")
    void invalidCredentialsException_creation() {
        Throwable cause = new IllegalArgumentException("root");
        InvalidCredentialsException ex1 = new InvalidCredentialsException("bad creds");
        InvalidCredentialsException ex2 = new InvalidCredentialsException("bad creds", cause);

        assertThat(ex1).hasMessage("bad creds");
        assertThat(ex2).hasMessage("bad creds");
        assertThat(ex2.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("InvalidRequestException conserve le message")
    void invalidRequestException_creation() {
        InvalidRequestException ex = new InvalidRequestException("invalid request");
        assertThat(ex).hasMessage("invalid request");
    }

    @Test
    @DisplayName("InvalidTokenException conserve le message")
    void invalidTokenException_creation() {
        InvalidTokenException ex = new InvalidTokenException("invalid token");
        assertThat(ex).hasMessage("invalid token");
    }

    @Test
    @DisplayName("JwtProcessingException conserve le message")
    void jwtProcessingException_creation() {
        JwtProcessingException ex = new JwtProcessingException("jwt error");
        assertThat(ex).hasMessage("jwt error");
    }
}