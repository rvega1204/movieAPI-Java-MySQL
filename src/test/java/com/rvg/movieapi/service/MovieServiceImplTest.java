package com.rvg.movieapi.service;

import com.rvg.movieapi.dto.MoviePageResponse;
import com.rvg.movieapi.dto.MovieRequestDto;
import com.rvg.movieapi.dto.MovieResponseDto;
import com.rvg.movieapi.entities.Movie;
import com.rvg.movieapi.exceptions.FileExistsException;
import com.rvg.movieapi.exceptions.MovieNotFoundException;
import com.rvg.movieapi.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceImplTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private FileService fileService;

    @InjectMocks
    private MovieServiceImpl movieService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(movieService, "path", "/uploads/posters");
        ReflectionTestUtils.setField(movieService, "baseUrl", "http://localhost:8080");
    }

    // -------------------------------------------------------------------------
    // addMovie()
    // -------------------------------------------------------------------------

    @Test
    void addMovie_whenFileIsNull_throwsFileExistsException() {
        assertThatThrownBy(() -> movieService.addMovie(buildRequest(), null))
                .isInstanceOf(FileExistsException.class);

        verifyNoInteractions(movieRepository);
    }

    @Test
    void addMovie_whenFileIsEmpty_throwsFileExistsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        assertThatThrownBy(() -> movieService.addMovie(buildRequest(), emptyFile))
                .isInstanceOf(FileExistsException.class);
    }

    @Test
    void addMovie_whenFileIsValid_savesMovieAndReturnsDto() throws Exception {
        MockMultipartFile file = validFile();
        Movie saved = buildMovie(1);
        when(fileService.uploadFile(any(), eq(file))).thenReturn("poster.jpg");
        when(movieRepository.save(any(Movie.class))).thenReturn(saved);

        MovieResponseDto result = movieService.addMovie(buildRequest(), file);

        assertThat(result.getMovieId()).isEqualTo(1);
        assertThat(result.getTitle()).isEqualTo("Inception");
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    void addMovie_posterUrlContainsBaseUrlAndFileName() throws Exception {
        MockMultipartFile file = validFile();
        when(fileService.uploadFile(any(), eq(file))).thenReturn("poster.jpg");
        when(movieRepository.save(any(Movie.class))).thenReturn(buildMovie(1));

        MovieResponseDto result = movieService.addMovie(buildRequest(), file);

        assertThat(result.getPosterUrl()).startsWith("http://localhost:8080");
        assertThat(result.getPosterUrl()).contains("poster.jpg");
    }

    // -------------------------------------------------------------------------
    // getMovieById()
    // -------------------------------------------------------------------------

    @Test
    void getMovieById_whenMovieExists_returnsDto() {
        when(movieRepository.findById(1)).thenReturn(Optional.of(buildMovie(1)));

        MovieResponseDto result = movieService.getMovieById(1);

        assertThat(result.getMovieId()).isEqualTo(1);
        assertThat(result.getTitle()).isEqualTo("Inception");
    }

    @Test
    void getMovieById_whenMovieNotFound_throwsMovieNotFoundException() {
        when(movieRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.getMovieById(99))
                .isInstanceOf(MovieNotFoundException.class)
                .hasMessageContaining("99");
    }

    // -------------------------------------------------------------------------
    // getAllMovies()
    // -------------------------------------------------------------------------

    @Test
    void getAllMovies_returnsMappedDtoList() {
        when(movieRepository.findAll()).thenReturn(List.of(buildMovie(1), buildMovie(2)));

        List<MovieResponseDto> result = movieService.getAllMovies();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllMovies_whenNoMovies_returnsEmptyList() {
        when(movieRepository.findAll()).thenReturn(List.of());

        List<MovieResponseDto> result = movieService.getAllMovies();

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // updateMovie()
    // -------------------------------------------------------------------------

    @Test
    void updateMovie_whenMovieNotFound_throwsMovieNotFoundException() {
        when(movieRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.updateMovie(99, buildRequest(), null))
                .isInstanceOf(MovieNotFoundException.class);
    }

    @Test
    void updateMovie_withNoFile_keepsExistingPoster() throws Exception {
        Movie existing = buildMovie(1);
        when(movieRepository.findById(1)).thenReturn(Optional.of(existing));
        when(movieRepository.save(any(Movie.class))).thenReturn(existing);

        movieService.updateMovie(1, buildRequest(), null);

        verify(fileService, never()).uploadFile(any(), any());
    }

    @Test
    void updateMovie_withNewFile_uploadsAndUpdatesFileName() throws Exception {
        MockMultipartFile file = validFile();
        Movie existing = buildMovie(1);
        when(movieRepository.findById(1)).thenReturn(Optional.of(existing));
        when(fileService.uploadFile(any(), eq(file))).thenReturn("new-poster.jpg");
        when(movieRepository.save(any(Movie.class))).thenReturn(buildMovie(1));

        MovieResponseDto result = movieService.updateMovie(1, buildRequest(), file);

        verify(fileService).uploadFile(any(), eq(file));
        assertThat(result).isNotNull();
    }

    // -------------------------------------------------------------------------
    // deleteMovie()
    // -------------------------------------------------------------------------

    @Test
    void deleteMovie_whenMovieNotFound_throwsMovieNotFoundException() {
        when(movieRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.deleteMovie(99))
                .isInstanceOf(MovieNotFoundException.class);
    }

    @Test
    void deleteMovie_whenMovieExists_deletesAndReturnsMessage() throws Exception {
        Movie movie = buildMovie(1);
        when(movieRepository.findById(1)).thenReturn(Optional.of(movie));

        String result = movieService.deleteMovie(1);

        verify(movieRepository).delete(movie);
        assertThat(result).isEqualTo("Movie deleted successfully");
    }

    // -------------------------------------------------------------------------
    // getAllMoviesWithPagination()
    // -------------------------------------------------------------------------

    @Test
    void getAllMoviesWithPagination_returnsPageResponse() {
        PageImpl<Movie> page = new PageImpl<>(List.of(buildMovie(1)), PageRequest.of(0, 5), 1);
        when(movieRepository.findAll(any(Pageable.class))).thenReturn(page);

        MoviePageResponse result = movieService.getAllMoviesWithPagination(0, 5);

        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(5);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.MovieResponseDto()).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // getAllMoviesWithPaginationAndSorting()
    // -------------------------------------------------------------------------

    @Test
    void getAllMoviesWithPaginationAndSorting_ascDirection_returnsPageResponse() {
        PageImpl<Movie> page = new PageImpl<>(List.of(buildMovie(1)), PageRequest.of(0, 5), 1);
        when(movieRepository.findAll(any(Pageable.class))).thenReturn(page);

        MoviePageResponse result = movieService.getAllMoviesWithPaginationAndSorting(0, 5, "title", "asc");

        assertThat(result.MovieResponseDto()).hasSize(1);
    }

    @Test
    void getAllMoviesWithPaginationAndSorting_descDirection_returnsPageResponse() {
        PageImpl<Movie> page = new PageImpl<>(List.of(buildMovie(1)), PageRequest.of(0, 5), 1);
        when(movieRepository.findAll(any(Pageable.class))).thenReturn(page);

        MoviePageResponse result = movieService.getAllMoviesWithPaginationAndSorting(0, 5, "title", "desc");

        assertThat(result.MovieResponseDto()).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Movie buildMovie(Integer id) {
        return new Movie(id, "Inception", "Christopher Nolan", "Warner Bros",
                Set.of("Leonardo DiCaprio"), 2010, "poster.jpg");
    }

    private MovieRequestDto buildRequest() {
        MovieRequestDto dto = new MovieRequestDto();
        dto.setTitle("Inception");
        dto.setDirector("Christopher Nolan");
        dto.setStudio("Warner Bros");
        dto.setMovieCast(Set.of("Leonardo DiCaprio"));
        dto.setReleaseYear(2010);
        return dto;
    }

    private MockMultipartFile validFile() {
        return new MockMultipartFile("file", "poster.jpg", "image/jpeg", "content".getBytes());
    }
}