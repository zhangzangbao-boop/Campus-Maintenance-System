package com.ligong.reportingcenter.dto;

import java.time.LocalDateTime;

public record RatingDto(
    Long ratingId,
    Integer score,
    String comment,
    String studentId,
    String staffId,
    LocalDateTime ratedAt
) {
}

