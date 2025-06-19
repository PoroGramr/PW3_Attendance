package com.jspark.pw3_attendant.controller;


import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.domain.Pray.Pray;
import com.jspark.pw3_attendant.service.ClassRoom.dto.ClassRoomResponse;
import com.jspark.pw3_attendant.service.Pray.PrayService;
import com.jspark.pw3_attendant.service.Pray.dto.PrayResponse;
import java.time.LocalDate;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pray")
public class PrayController {

    private final PrayService prayService;

    @GetMapping({"/{date}"})
    public PrayResponse pray(@PathVariable LocalDate date){
        Pray pray = prayService.findByDate(date);
        return PrayResponse.from(pray);
    }
}
