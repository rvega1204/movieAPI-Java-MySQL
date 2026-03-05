package com.rvg.movieapi.service;

import com.rvg.movieapi.dto.MoviePageResponse;
import com.rvg.movieapi.dto.MovieRequestDto;
import com.rvg.movieapi.dto.MovieResponseDto;
import com.rvg.movieapi.entities.Movie;
import com.rvg.movieapi.exceptions.FileExistsException;
import com.rvg.movieapi.exceptions.MovieNotFoundException;
import com.rvg.movieapi.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Service implementation for movie management operations.
 * <p>
 * Handles the full lifecycle of a movie: creation, retrieval, update,
 * deletion, and paginated listing. File operations (poster upload/delete)
 * are delegated to {@link FileService}.
 * <p>
 * The poster URL is constructed by combining {@code base.url} with the
 * file endpoint, encoding the file name to handle special characters.
 */
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

    /**
     * Creates a new movie with the given data and poster file.
     * <p>
     * Uploads the poster file via {@link FileService}, persists the movie entity,
     * and returns the mapped response DTO.
     *
     * @param dto  the movie data
     * @param file the poster image file (required, must not be empty)
     * @return the saved movie as a {@link MovieResponseDto}
     * @throws FileExistsException if the file is null or empty
     * @throws Exception           if the file upload fails
     */
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

    /**
     * Retrieves a movie by its ID.
     *
     * @param id the movie ID
     * @return the matching movie as a {@link MovieResponseDto}
     * @throws MovieNotFoundException if no movie exists with the given ID
     */
    @Override
    public MovieResponseDto getMovieById(Integer id) {

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() ->
                        new MovieNotFoundException("Movie not found with id: " + id));

        return mapToResponse(movie);
    }

    /**
     * Retrieves all movies.
     *
     * @return list of all movies as {@link MovieResponseDto}
     */
    @Override
    public List<MovieResponseDto> getAllMovies() {

        return movieRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Updates an existing movie by ID.
     * <p>
     * If a new poster file is provided, the existing poster is deleted from disk
     * and the new one is uploaded. If no file is provided, the existing poster
     * file name is preserved.
     *
     * @param movieId the ID of the movie to update
     * @param dto     the updated movie data
     * @param file    the new poster file (optional)
     * @return the updated movie as a {@link MovieResponseDto}
     * @throws MovieNotFoundException if no movie exists with the given ID
     * @throws Exception              if file operations fail
     */
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

    /**
     * Deletes a movie by ID and removes its poster from disk.
     *
     * @param movieId the ID of the movie to delete
     * @return a confirmation message
     * @throws MovieNotFoundException if no movie exists with the given ID
     * @throws Exception              if the file deletion fails
     */
    @Override
    public String deleteMovie(Integer movieId) throws Exception {

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() ->
                        new MovieNotFoundException("Movie not found"));

        Files.deleteIfExists(Paths.get(path + File.separator + movie.getPoster()));

        movieRepository.delete(movie);

        return "Movie deleted successfully";
    }

    /**
     * Retrieves a paginated list of movies without sorting.
     *
     * @param pageNumber zero-based page index
     * @param pageSize   number of items per page
     * @return paginated movie data as {@link MoviePageResponse}
     */
    @Override
    public MoviePageResponse getAllMoviesWithPagination(Integer pageNumber, Integer pageSize) {
        return getMoviePageResponse(pageNumber, pageSize, null);
    }

    /**
     * Retrieves a paginated and sorted list of movies.
     *
     * @param pageNumber zero-based page index
     * @param pageSize   number of items per page
     * @param sortBy     the field to sort by
     * @param dir        sort direction — {@code asc} or {@code desc} (case-insensitive)
     * @return sorted and paginated movie data as {@link MoviePageResponse}
     */
    @Override
    public MoviePageResponse getAllMoviesWithPaginationAndSorting(Integer pageNumber, Integer pageSize, String sortBy, String dir) {
        Sort sort = dir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        return getMoviePageResponse(pageNumber, pageSize, sort);
    }

    /**
     * Builds a {@link MoviePageResponse} from the given pagination and optional sort parameters.
     *
     * @param pageNumber zero-based page index
     * @param pageSize   number of items per page
     * @param sort       sort configuration, or {@code null} for unsorted
     * @return a {@link MoviePageResponse} with content and pagination metadata
     */
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

    /**
     * Maps a {@link Movie} entity to a {@link MovieResponseDto}.
     * <p>
     * Constructs the poster URL by encoding the file name and combining it
     * with the base URL. Trailing slashes on the base URL are stripped.
     *
     * @param movie the entity to map
     * @return the mapped response DTO
     */
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
