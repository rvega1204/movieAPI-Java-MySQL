package com.rvg.movieapi.controllers;

import com.rvg.movieapi.dto.MoviePageResponse;
import com.rvg.movieapi.dto.MovieRequestDto;
import com.rvg.movieapi.dto.MovieResponseDto;
import com.rvg.movieapi.exceptions.EmptyFileException;
import com.rvg.movieapi.service.MovieService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieControllerTest {

    @Mock
    private MovieService movieService;

    @InjectMocks
    private MovieController movieController;

    // -------------------------------------------------------------------------
    // addMovie()
    // -------------------------------------------------------------------------

    @Test
    void addMovie_whenFileIsValid_returns201WithBody() throws Exception, EmptyFileException {
        MockMultipartFile file = validFile();
        String movieJson = validMovieJson();
        MovieResponseDto expected = buildMovieResponse(1);
        when(movieService.addMovie(any(MovieRequestDto.class), eq(file))).thenReturn(expected);

        ResponseEntity<MovieResponseDto> response = movieController.addMovie(file, movieJson);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void addMovie_whenFileIsEmpty_throwsEmptyFileException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> movieController.addMovie(emptyFile, validMovieJson()))
                .isInstanceOf(EmptyFileException.class);

        verifyNoInteractions(movieService);
    }

    @Test
    void addMovie_delegatesToMovieService() throws Exception, EmptyFileException {
        MockMultipartFile file = validFile();
        when(movieService.addMovie(any(MovieRequestDto.class), eq(file))).thenReturn(buildMovieResponse(1));

        movieController.addMovie(file, validMovieJson());

        verify(movieService).addMovie(any(MovieRequestDto.class), eq(file));
    }

    // -------------------------------------------------------------------------
    // getMovieById()
    // -------------------------------------------------------------------------

    @Test
    void getMovieById_returns200WithMovie() {
        MovieResponseDto expected = buildMovieResponse(1);
        when(movieService.getMovieById(1)).thenReturn(expected);

        ResponseEntity<MovieResponseDto> response = movieController.getMovieById(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void getMovieById_delegatesToMovieService() {
        when(movieService.getMovieById(1)).thenReturn(buildMovieResponse(1));

        movieController.getMovieById(1);

        verify(movieService).getMovieById(1);
    }

    // -------------------------------------------------------------------------
    // getAllMovies()
    // -------------------------------------------------------------------------

    @Test
    void getAllMovies_whenMoviesExist_returns200WithList() {
        List<MovieResponseDto> movies = List.of(buildMovieResponse(1), buildMovieResponse(2));
        when(movieService.getAllMovies()).thenReturn(movies);

        ResponseEntity<List<MovieResponseDto>> response = movieController.getAllMovies();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getAllMovies_whenServiceReturnsNull_returns204() {
        when(movieService.getAllMovies()).thenReturn(null);

        ResponseEntity<List<MovieResponseDto>> response = movieController.getAllMovies();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // -------------------------------------------------------------------------
    // updateMovie()
    // -------------------------------------------------------------------------

    @Test
    void updateMovie_returns200WithUpdatedMovie() throws Exception {
        MockMultipartFile file = validFile();
        MovieResponseDto expected = buildMovieResponse(1);
        when(movieService.updateMovie(eq(1), any(MovieRequestDto.class), eq(file))).thenReturn(expected);

        ResponseEntity<MovieResponseDto> response = movieController.updateMovie(1, file, validMovieJson());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void updateMovie_withNullFile_delegatesToService() throws Exception {
        MovieResponseDto expected = buildMovieResponse(1);
        when(movieService.updateMovie(eq(1), any(MovieRequestDto.class), isNull())).thenReturn(expected);

        ResponseEntity<MovieResponseDto> response = movieController.updateMovie(1, null, validMovieJson());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(movieService).updateMovie(eq(1), any(MovieRequestDto.class), isNull());
    }

    // -------------------------------------------------------------------------
    // deleteMovie()
    // -------------------------------------------------------------------------

    @Test
    void deleteMovie_returns200WithConfirmationMessage() throws Exception {
        when(movieService.deleteMovie(1)).thenReturn("Movie deleted successfully");

        ResponseEntity<String> response = movieController.deleteMovie(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Movie deleted successfully");
    }

    @Test
    void deleteMovie_delegatesToMovieService() throws Exception {
        when(movieService.deleteMovie(1)).thenReturn("Movie deleted successfully");

        movieController.deleteMovie(1);

        verify(movieService).deleteMovie(1);
    }

    // -------------------------------------------------------------------------
    // getMoviesWithPagination()
    // -------------------------------------------------------------------------

    @Test
    void getMoviesWithPagination_returns200WithPageResponse() {
        MoviePageResponse pageResponse = buildPageResponse();
        when(movieService.getAllMoviesWithPagination(0, 5)).thenReturn(pageResponse);

        ResponseEntity<MoviePageResponse> response = movieController.getMoviesWithPagination(0, 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(pageResponse);
    }

    @Test
    void getMoviesWithPagination_delegatesPageAndSizeToService() {
        when(movieService.getAllMoviesWithPagination(2, 10)).thenReturn(buildPageResponse());

        movieController.getMoviesWithPagination(2, 10);

        verify(movieService).getAllMoviesWithPagination(2, 10);
    }

    // -------------------------------------------------------------------------
    // getMoviesWithPaginationAndSorting()
    // -------------------------------------------------------------------------

    @Test
    void getMoviesWithPaginationAndSorting_returns200WithPageResponse() {
        MoviePageResponse pageResponse = buildPageResponse();
        when(movieService.getAllMoviesWithPaginationAndSorting(0, 5, "title", "asc"))
                .thenReturn(pageResponse);

        ResponseEntity<MoviePageResponse> response =
                movieController.getMoviesWithPaginationAndSorting(0, 5, "title", "asc");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(pageResponse);
    }

    @Test
    void getMoviesWithPaginationAndSorting_delegatesAllParamsToService() {
        when(movieService.getAllMoviesWithPaginationAndSorting(1, 20, "releaseYear", "desc"))
                .thenReturn(buildPageResponse());

        movieController.getMoviesWithPaginationAndSorting(1, 20, "releaseYear", "desc");

        verify(movieService).getAllMoviesWithPaginationAndSorting(1, 20, "releaseYear", "desc");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private MockMultipartFile validFile() {
        return new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", "image-content".getBytes());
    }

    private String validMovieJson() {
        return """
                {
                  "title": "Inception",
                  "director": "Christopher Nolan",
                  "studio": "Warner Bros",
                  "releaseYear": 2010,
                  "movieCast": ["Leonardo DiCaprio"]
                }
                """;
    }

    private MovieResponseDto buildMovieResponse(Integer id) {
        return new MovieResponseDto(id, "Inception", "Christopher Nolan",
                "Warner Bros", Set.of("Leonardo DiCaprio"), 2010,
                "inception.jpg", "http://localhost/inception.jpg");
    }

    private MoviePageResponse buildPageResponse() {
        return new MoviePageResponse(List.of(buildMovieResponse(1)), 0, 5, 1, 1, true);
    }
}