package com.jspark.pw3_attendant.service.promotion.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromotionRequestDto {
    private int fromYear;
    private int toYear;
    private List<PromotionDto> promotions;
}
