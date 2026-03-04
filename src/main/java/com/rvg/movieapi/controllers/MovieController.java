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

@RestController
@RequestMapping("/api/v1/movie")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

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


    @GetMapping("/{movieId}")
    public ResponseEntity<MovieResponseDto> getMovieById(@PathVariable Integer movieId) {
        MovieResponseDto movie = movieService.getMovieById(movieId);
        return ResponseEntity.ok(movie);
    }

    @GetMapping("/all")
    public ResponseEntity<List<MovieResponseDto>> getAllMovies() {
        ResponseEntity<List<MovieResponseDto>> response = ResponseEntity.ok(movieService.getAllMovies());
        if (response.getBody() == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return response;
    }

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

    @DeleteMapping("/delete/{movieId}")
    public ResponseEntity<String> deleteMovie(@PathVariable Integer movieId) throws Exception {
        String response = movieService.deleteMovie(movieId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/allMoviesPage")
    public ResponseEntity<MoviePageResponse> getMoviesWithPagination(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) Integer pageSize
    ) {
        return ResponseEntity.ok(movieService.getAllMoviesWithPagination(pageNumber, pageSize));
    }

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
