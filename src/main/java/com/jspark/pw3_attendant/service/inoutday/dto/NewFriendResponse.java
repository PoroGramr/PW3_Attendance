package com.jspark.pw3_attendant.service.inoutday.dto;

import com.jspark.pw3_attendant.domain.inoutday.NewFriend;
import java.time.LocalDate;
import lombok.Getter;

@Getter
public class NewFriendResponse {
    private Long id;
    private String name;
    private LocalDate birth;
    private String phone;
    private Long studentId;

    public NewFriendResponse(NewFriend newFriend) {
        this.id = newFriend.getId();
        this.name = newFriend.getName();
        this.birth = newFriend.getBirth();
        this.phone = newFriend.getPhone();
        if (newFriend.getStudent() != null) {
            this.studentId = newFriend.getStudent().getId();
        }
    }
}
