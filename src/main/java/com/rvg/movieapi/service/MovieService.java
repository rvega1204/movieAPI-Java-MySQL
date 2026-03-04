package com.rvg.movieapi.service;

import com.rvg.movieapi.dto.MoviePageResponse;
import com.rvg.movieapi.dto.MovieRequestDto;
import com.rvg.movieapi.dto.MovieResponseDto;
import com.rvg.movieapi.exceptions.FileExistsException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MovieService {

    MovieResponseDto addMovie(MovieRequestDto movieDto, MultipartFile file) throws Exception, FileExistsException;

    MovieResponseDto getMovieById(Integer id);

    List<MovieResponseDto> getAllMovies();

    MovieResponseDto updateMovie(Integer movieId, MovieRequestDto movieDto, MultipartFile file) throws Exception;

    String deleteMovie(Integer movieId) throws Exception;

    MoviePageResponse getAllMoviesWithPagination(Integer pageNumber, Integer pageSize);

    MoviePageResponse getAllMoviesWithPaginationAndSorting(Integer pageNumber, Integer pageSize,
                                                           String sortBy, String dir);
}
