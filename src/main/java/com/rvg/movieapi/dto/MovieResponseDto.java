package com.rvg.movieapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class MovieResponseDto {

    private Integer movieId;
    private String title;
    private String director;
    private String studio;
    private Set<String> movieCast;
    private Integer releaseYear;
    private String poster;
    private String posterUrl;
}
