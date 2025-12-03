package com.ligong.reportingcenter.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketRatingRequest(
    @NotBlank(message = "学生ID不能为空")
    @Size(max = 50, message = "学生ID长度不能超过50")
    String studentId,

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分不能小于1")
    @Max(value = 5, message = "评分不能大于5")
    Integer score,

    @Size(max = 1000, message = "评价内容过长")
    String comment
) {
}

