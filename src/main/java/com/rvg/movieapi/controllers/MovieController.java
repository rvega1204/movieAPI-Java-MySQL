package com.rvg.movieapi.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rvg.movieapi.dto.MoviePageResponse;
import com.rvg.movieapi.dto.MovieRequestDto;
import com.rvg.movieapi.dto.MovieResponseDto;
import com.rvg.movieapi.exceptions.EmptyFileException;
import com.rvg.movieapi.service.MovieService;
import com.rvg.movieapi.utils.AppConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for movie management operations.
 * <p>
 * Exposes endpoints under {@code /api/v1/movie} for creating, retrieving,
 * updating, deleting, and paginating movies. File upload is handled as
 * multipart form data alongside a JSON movie payload.
 * <p>
 * Adding movies requires {@code ADMIN} authority. All other endpoints
 * are accessible to any authenticated user.
 */
@RestController
@RequestMapping("/api/v1/movie")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    /**
     * Creates a new movie with an associated poster file.
     * <p>
     * Accepts a multipart request containing the poster file and a JSON
     * string representing the movie data. Throws {@link EmptyFileException}
     * if the uploaded file has no content.
     *
     * @param file      the poster image file
     * @param movieJson JSON string representation of {@link MovieRequestDto}
     * @return 201 Created with the saved {@link MovieResponseDto}
     * @throws Exception          if JSON deserialization or file storage fails
     * @throws EmptyFileException if the uploaded file is empty
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(value = "/add-movie", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MovieResponseDto> addMovie(
            @RequestPart("file") MultipartFile file,
            @RequestPart("movie") String movieJson
    ) throws Exception, EmptyFileException {

        if (file.isEmpty()) {
            throw new EmptyFileException("File is empty! Please send another file!");
        }

        MovieRequestDto request =
                new ObjectMapper().readValue(movieJson, MovieRequestDto.class);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(movieService.addMovie(request, file));
    }

    /**
     * Retrieves a single movie by its ID.
     *
     * @param movieId the ID of the movie to retrieve
     * @return 200 OK with the matching {@link MovieResponseDto}
     */
    @GetMapping("/{movieId}")
    public ResponseEntity<MovieResponseDto> getMovieById(@PathVariable Integer movieId) {
        MovieResponseDto movie = movieService.getMovieById(movieId);
        return ResponseEntity.ok(movie);
    }

    /**
     * Retrieves all movies.
     * <p>
     * Returns 204 No Content if the service returns a null body,
     * otherwise returns the full list with 200 OK.
     *
     * @return 200 OK with the list of all movies, or 204 No Content if none
     */
    @GetMapping("/all")
    public ResponseEntity<List<MovieResponseDto>> getAllMovies() {
        ResponseEntity<List<MovieResponseDto>> response = ResponseEntity.ok(movieService.getAllMovies());
        if (response.getBody() == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return response;
    }

    /**
     * Updates an existing movie by ID.
     * <p>
     * Accepts a multipart request. The poster file is optional — if omitted,
     * the existing file is preserved by the service layer.
     *
     * @param movieId   the ID of the movie to update
     * @param file      the new poster file (optional)
     * @param movieJson JSON string representation of the updated {@link MovieRequestDto}
     * @return 200 OK with the updated {@link MovieResponseDto}
     * @throws Exception if JSON deserialization or file storage fails
     */
    @PutMapping(value = "/update/{movieId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MovieResponseDto> updateMovie(
            @PathVariable Integer movieId,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart("movie") String movieJson
    ) throws Exception {

        MovieRequestDto request =
                new ObjectMapper().readValue(movieJson, MovieRequestDto.class);

        return ResponseEntity.ok(
                movieService.updateMovie(movieId, request, file)
        );
    }

    /**
     * Deletes a movie by ID.
     *
     * @param movieId the ID of the movie to delete
     * @return 200 OK with a confirmation message from the service
     * @throws Exception if deletion fails
     */
    @DeleteMapping("/delete/{movieId}")
    public ResponseEntity<String> deleteMovie(@PathVariable Integer movieId) throws Exception {
        String response = movieService.deleteMovie(movieId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of movies.
     *
     * @param pageNumber the zero-based page index (default: {@link AppConstants#DEFAULT_PAGE_NUMBER})
     * @param pageSize   the number of items per page (default: {@link AppConstants#DEFAULT_PAGE_SIZE})
     * @return 200 OK with a {@link MoviePageResponse} containing the page data
     */
    @GetMapping("/allMoviesPage")
    public ResponseEntity<MoviePageResponse> getMoviesWithPagination(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) Integer pageSize
    ) {
        return ResponseEntity.ok(movieService.getAllMoviesWithPagination(pageNumber, pageSize));
    }

    /**
     * Retrieves a paginated and sorted list of movies.
     *
     * @param pageNumber the zero-based page index (default: {@link AppConstants#DEFAULT_PAGE_NUMBER})
     * @param pageSize   the number of items per page (default: {@link AppConstants#DEFAULT_PAGE_SIZE})
     * @param sortBy     the field name to sort by (default: {@link AppConstants#DEFAULT_SORT_BY})
     * @param dir        the sort direction — {@code asc} or {@code desc} (default: {@link AppConstants#DEFAULT_SORT_DIR})
     * @return 200 OK with a sorted {@link MoviePageResponse}
     */
    @GetMapping("/allMoviesPageSort")
    public ResponseEntity<MoviePageResponse> getMoviesWithPaginationAndSorting(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIR, required = false) String dir
    ) {
        return ResponseEntity.ok(movieService.getAllMoviesWithPaginationAndSorting(pageNumber, pageSize, sortBy, dir));
    }

}
