package com.jspark.pw3_attendant.service.Pray.dto;

import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.domain.Pray.Pray;
import com.jspark.pw3_attendant.service.ClassRoom.dto.ClassRoomResponse;
import jakarta.persistence.Column;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PrayResponse {
    private LocalDate date;
    private String prayer;
    private String prayContent;
    private String recitation;
    private String declaration;

    public static PrayResponse from(Pray pray) {
        return new PrayResponse(
            pray.getDate(),
            pray.getPrayer(),
            pray.getPrayContent(),
            pray.getRecitation(),
            pray.getDeclaration()
        );
    }


}
