package onetoone.Exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles errors you throw like: new ResponseStatusException(...)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<onetoone.Exceptions.ApiError> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();
        ApiError err = new ApiError(
                status.value(),
                status.getReasonPhrase(),
                ex.getReason(),
                req.getRequestURI()
        );
        return ResponseEntity.status(status).body(err);
    }

    // Handles @Valid validation errors (email format, blank fields, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Validation error"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

        ApiError err = new ApiError(
                400,
                "Bad Request",
                msg,
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    // Fallback for unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex, HttpServletRequest req) {
        ApiError err = new ApiError(
                500,
                "Internal Server Error",
                "Something went wrong",
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
}