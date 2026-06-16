package com.finsplit.finsplit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError e : ex.getBindingResult().getFieldErrors())
            fieldErrors.put(e.getField(), e.getDefaultMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status",    400,
                "errors",    fieldErrors
        ));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(
            UserAlreadyExistsException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCreds(
            BadCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            UnauthorizedException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArg(
            IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleFileTooLarge(
            MaxUploadSizeExceededException ex) {
        return error(HttpStatus.BAD_REQUEST, "File size must be under 5 MB");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again.");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status",    status.value(),
                "message",   message
        ));
    }
}


//package com.finsplit.finsplit.exception;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.validation.FieldError;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.*;
//import java.time.LocalDateTime;
//import java.util.*;
//
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
//        Map<String, String> fieldErrors = new HashMap<>();
//        for (FieldError e : ex.getBindingResult().getFieldErrors())
//            fieldErrors.put(e.getField(), e.getDefaultMessage());
//        return ResponseEntity.badRequest().body(Map.of(
//                "timestamp", LocalDateTime.now().toString(),
//                "status", 400,
//                "errors", fieldErrors
//        ));
//    }
//
//    @ExceptionHandler(UserAlreadyExistsException.class)
//    public ResponseEntity<Map<String, Object>> handleDuplicate(UserAlreadyExistsException ex) {
//        return error(HttpStatus.CONFLICT, ex.getMessage());
//    }
//
//    @ExceptionHandler(BadCredentialsException.class)
//    public ResponseEntity<Map<String, Object>> handleBadCreds(BadCredentialsException ex) {
//        return error(HttpStatus.UNAUTHORIZED, "Invalid email or password");
//    }
//
//    @ExceptionHandler(ResourceNotFoundException.class)
//    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
//        return error(HttpStatus.NOT_FOUND, ex.getMessage());
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
//        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");
//    }
//
//    @ExceptionHandler(UnauthorizedException.class)
//    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
//        return error(HttpStatus.FORBIDDEN, ex.getMessage());
//    }
//
//    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
//        return ResponseEntity.status(status).body(Map.of(
//                "timestamp", LocalDateTime.now().toString(),
//                "status", status.value(),
//                "message", message
//        ));
//    }
//}
