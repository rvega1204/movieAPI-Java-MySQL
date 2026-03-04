package com.rvg.movieapi.service;

import com.rvg.movieapi.dto.MoviePageResponse;
import com.rvg.movieapi.dto.MovieRequestDto;
import com.rvg.movieapi.dto.MovieResponseDto;
import com.rvg.movieapi.entities.Movie;
import com.rvg.movieapi.exceptions.FileExistsException;
import com.rvg.movieapi.exceptions.MovieNotFoundException;
import com.rvg.movieapi.repository.MovieRepository;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final FileService fileService;

    @Value("${project.poster}")
    private String path;

    @Value("${base.url}")
    private String baseUrl;

    public MovieServiceImpl(MovieRepository movieRepository,
                            FileService fileService) {
        this.movieRepository = movieRepository;
        this.fileService = fileService;
    }

    // ========================
    // ADD MOVIE
    // ========================
    @Override
    public MovieResponseDto addMovie(MovieRequestDto dto,
                                     MultipartFile file) throws Exception, FileExistsException {

        if (file == null || file.isEmpty()) {
            throw new FileExistsException("Poster file is required");
        }

        String uploadedFileName = fileService.uploadFile(path, file);

        Movie movie = new Movie(
                null,
                dto.getTitle(),
                dto.getDirector(),
                dto.getStudio(),
                dto.getMovieCast(),
                dto.getReleaseYear(),
                uploadedFileName
        );

        Movie saved = movieRepository.save(movie);

        return mapToResponse(saved);
    }

    // ========================
    // GET BY ID
    // ========================
    @Override
    public MovieResponseDto getMovieById(Integer id) {

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() ->
                        new MovieNotFoundException("Movie not found with id: " + id));

        return mapToResponse(movie);
    }

    // ========================
    // GET ALL
    // ========================
    @Override
    public List<MovieResponseDto> getAllMovies() {

        return movieRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ========================
    // UPDATE
    // ========================
    @Override
    public MovieResponseDto updateMovie(Integer movieId,
                                        MovieRequestDto dto,
                                        MultipartFile file) throws Exception {

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() ->
                        new MovieNotFoundException("Movie not found"));

        String fileName = movie.getPoster();

        if (file != null && !file.isEmpty()) {
            Files.deleteIfExists(Paths.get(path + File.separator + fileName));
            fileName = fileService.uploadFile(path, file);
        }

        Movie updated = new Movie(
                movie.getMovieId(),
                dto.getTitle(),
                dto.getDirector(),
                dto.getStudio(),
                dto.getMovieCast(),
                dto.getReleaseYear(),
                fileName
        );

        Movie saved = movieRepository.save(updated);

        return mapToResponse(saved);
    }

    // ========================
    // DELETE
    // ========================
    @Override
    public String deleteMovie(Integer movieId) throws Exception {

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() ->
                        new MovieNotFoundException("Movie not found"));

        Files.deleteIfExists(Paths.get(path + File.separator + movie.getPoster()));

        movieRepository.delete(movie);

        return "Movie deleted successfully";
    }

    @Override
    public MoviePageResponse getAllMoviesWithPagination(Integer pageNumber, Integer pageSize) {
        return getMoviePageResponse(pageNumber, pageSize, null);
    }


    @Override
    public MoviePageResponse getAllMoviesWithPaginationAndSorting(Integer pageNumber, Integer pageSize, String sortBy, String dir) {
        Sort sort = dir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        return getMoviePageResponse(pageNumber, pageSize, sort);
    }

    private MoviePageResponse getMoviePageResponse(Integer pageNumber,
                                                   Integer pageSize,
                                                   Sort sort) {
        Pageable pageable;
        if (sort != null) {
            pageable = PageRequest.of(pageNumber, pageSize, sort);
        } else {
            pageable = PageRequest.of(pageNumber, pageSize);
        }

        Page<Movie> moviePages = movieRepository.findAll(pageable);

        List<MovieResponseDto> movieDtos = moviePages.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new MoviePageResponse(
                movieDtos,
                pageNumber,
                pageSize,
                (int) moviePages.getTotalElements(),
                moviePages.getTotalPages(),
                moviePages.isLast()
        );
    }

    // ========================
    // MAPPER
    // ========================
    private MovieResponseDto mapToResponse(Movie movie) {

        String posterUrl = String.format(
                "%s/api/v1/file/%s",
                baseUrl.replaceAll("/$", ""),
                URLEncoder.encode(movie.getPoster(), StandardCharsets.UTF_8)
        );

        return new MovieResponseDto(
                movie.getMovieId(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getStudio(),
                movie.getMovieCast(),
                movie.getReleaseYear(),
                movie.getPoster(),
                posterUrl
        );
    }
}
