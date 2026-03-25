package com.example.rollbasedlogin.config;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice   // 🔥 This makes it global for all controllers
public class GlobalExceptionHandler 
{

     // Validation errors (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 400);
        response.put("errors", errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }




    // Handles any exception that you don't handle explicitly
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("error", "Internal Server Error");
        error.put("message", ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}


// ✅ What is @ControllerAdvice?

// @ControllerAdvice is a global controller for your entire Spring Boot application.

// It allows you to write common logic (like exception handling, data binding, or model attributes) in one place instead of repeating it in every controller.

// 🎯 Simple Definition (Developer-friendly)

// @ControllerAdvice is used to handle exceptions globally for all controllers, instead of writing try–catch in each controller.

// 📌 What it solves

// Without @ControllerAdvice:
// You would write try-catch in every controller method.

// With @ControllerAdvice:
// You handle the exception once, globally.



// 📌 How it Works?

// Whenever any controller throws an exception, Spring checks:

// Does the controller have its own @ExceptionHandler?
// → Use it.

// If not, does @ControllerAdvice contain a handler?
// → Yes → handle it globally.

// 🧠 Why is it important in real development?

// Because:

// ✔ Prevents duplicate try/catch
// ✔ Makes error responses consistent
// ✔ Helps maintain code cleanliness
// ✔ Reduces bugs
// ✔ Gives better control over API responses
// 🏆 Interview Answer (Use this)

// @ControllerAdvice in Spring Boot is used to handle exceptions globally.
// Instead of writing exception handling in every controller, we create one class with @ControllerAdvice and Spring applies it across all controllers automatically.
// It helps maintain clean, reusable, and consistent error handling.

// ⚡ Want me to show a complete working example with custom error response JSON?

// Like:

// {
//   "timestamp": "...",
//   "error": "User Not Found",
//   "status": 404,
//   "path": "/api/user/10"
// }


