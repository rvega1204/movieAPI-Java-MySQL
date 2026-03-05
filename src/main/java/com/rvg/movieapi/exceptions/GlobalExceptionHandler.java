package com.rvg.movieapi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the application.
 * <p>
 * Intercepts exceptions thrown by any controller and maps them to
 * RFC 9457 {@link ProblemDetail} responses, providing consistent
 * error payloads across all endpoints.
 * <p>
 * Handler priority (most specific to least):
 * <ol>
 *   <li>{@link MovieNotFoundException} → 404 Not Found</li>
 *   <li>{@link FileExistsException} → 400 Bad Request</li>
 *   <li>{@link EmptyFileException} → 400 Bad Request</li>
 *   <li>{@link Exception} → 500 Internal Server Error (catch-all)</li>
 * </ol>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link MovieNotFoundException}.
     * Returned when a requested movie does not exist in the database.
     *
     * @param ex the thrown exception
     * @return 404 Not Found with the exception message as detail
     */
    @ExceptionHandler(MovieNotFoundException.class)
    public ProblemDetail handleMovieNotFoundException(MovieNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles {@link FileExistsException}.
     * Returned when an upload is attempted with a file name that already exists.
     *
     * @param ex the thrown exception
     * @return 400 Bad Request with the exception message as detail
     */
    @ExceptionHandler(FileExistsException.class)
    public ProblemDetail handleFileExistsException(FileExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles {@link EmptyFileException}.
     * Returned when an uploaded file has no content.
     *
     * @param ex the thrown exception
     * @return 400 Bad Request with the exception message as detail
     */
    @ExceptionHandler(EmptyFileException.class)
    public ProblemDetail handleEmptyFileException(EmptyFileException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Catch-all handler for any unhandled exception.
     * Prefixes the message with context to distinguish it from specific errors.
     *
     * @param ex the thrown exception
     * @return 500 Internal Server Error with a prefixed message as detail
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + ex.getMessage());
    }
}
