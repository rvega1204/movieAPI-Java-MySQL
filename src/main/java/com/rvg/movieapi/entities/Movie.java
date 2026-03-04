package com.rvg.movieapi.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Movie {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer movieId;

    @Column(length=200, nullable = false)
    @NotBlank(message = "Title is mandatory")
    private String title;

    @Column(length=200, nullable = false)
    @NotBlank(message = "Director is mandatory")
    private String director;

    @Column(length=200, nullable = false)
    @NotBlank(message = "Studio is mandatory")
    private String studio;

    @ElementCollection
    @CollectionTable(name = "movie_cast")
    private Set<String> movieCast;

    @Column(nullable = false)
    @NotNull
    private Integer releaseYear;

    @Column(nullable = false)
    @NotBlank(message = "Poster is mandatory")
    private String poster;
}
