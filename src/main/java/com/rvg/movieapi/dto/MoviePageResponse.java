package com.rvg.movieapi.dto;

import java.util.List;

public record MoviePageResponse(List<MovieResponseDto> MovieResponseDto, Integer pageNumber, Integer pageSize, int totalElements, int totalPages, boolean isLast) {
}
