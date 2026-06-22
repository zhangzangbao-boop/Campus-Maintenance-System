package com.ligong.reportingcenter.service;

import com.ligong.reportingcenter.domain.entity.Rating;
import com.ligong.reportingcenter.dto.RatingDto;
import com.ligong.reportingcenter.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;

    @Transactional(readOnly = true)
    public List<RatingDto> listAll() {
        return ratingRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long ratingId) {
        ratingRepository.deleteById(ratingId);
    }

    private RatingDto toDto(Rating rating) {
        return new RatingDto(
                rating.getRatingId(),
                rating.getScore(),
                rating.getComment(),
                rating.getStudent() != null ? rating.getStudent().getUserId() : null,
                rating.getStaff() != null ? rating.getStaff().getUserId() : null,
                rating.getRatedAt()
        );
    }
}