package com.rvg.movieapi.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // -------------------------------------------------------------------------
    // MovieNotFoundException → 404
    // -------------------------------------------------------------------------

    @Test
    void handleMovieNotFoundException_returns404() {
        MovieNotFoundException ex = new MovieNotFoundException("Movie not found with id: 1");

        ProblemDetail result = handler.handleMovieNotFoundException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void handleMovieNotFoundException_containsExceptionMessage() {
        MovieNotFoundException ex = new MovieNotFoundException("Movie not found with id: 1");

        ProblemDetail result = handler.handleMovieNotFoundException(ex);

        assertThat(result.getDetail()).isEqualTo("Movie not found with id: 1");
    }

    // -------------------------------------------------------------------------
    // FileExistsException → 400
    // -------------------------------------------------------------------------

    @Test
    void handleFileExistsException_returns400() {
        FileExistsException ex = new FileExistsException("File already exists: poster.jpg");

        ProblemDetail result = handler.handleFileExistsException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void handleFileExistsException_containsExceptionMessage() {
        FileExistsException ex = new FileExistsException("File already exists: poster.jpg");

        ProblemDetail result = handler.handleFileExistsException(ex);

        assertThat(result.getDetail()).isEqualTo("File already exists: poster.jpg");
    }

    // -------------------------------------------------------------------------
    // EmptyFileException → 400
    // -------------------------------------------------------------------------

    @Test
    void handleEmptyFileException_returns400() {
        EmptyFileException ex = new EmptyFileException("File is empty!");

        ProblemDetail result = handler.handleEmptyFileException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void handleEmptyFileException_containsExceptionMessage() {
        EmptyFileException ex = new EmptyFileException("File is empty!");

        ProblemDetail result = handler.handleEmptyFileException(ex);

        assertThat(result.getDetail()).isEqualTo("File is empty!");
    }

    // -------------------------------------------------------------------------
    // General Exception → 500
    // -------------------------------------------------------------------------

    @Test
    void handleGeneralException_returns500() {
        Exception ex = new Exception("Something went wrong");

        ProblemDetail result = handler.handleGeneralException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    void handleGeneralException_prefixesMessageWithContext() {
        Exception ex = new Exception("Something went wrong");

        ProblemDetail result = handler.handleGeneralException(ex);

        assertThat(result.getDetail())
                .startsWith("An unexpected error occurred:")
                .contains("Something went wrong");
    }

    @Test
    void handleGeneralException_catchesAnyExceptionSubtype() {
        IllegalArgumentException ex = new IllegalArgumentException("bad arg");

        ProblemDetail result = handler.handleGeneralException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getDetail()).contains("bad arg");
    }
}