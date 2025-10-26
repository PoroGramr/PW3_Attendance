package com.jspark.pw3_attendant.service.inoutday;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.inoutday.NewFriend;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.repository.inoutday.NewFriendRepository;
import com.jspark.pw3_attendant.service.inoutday.dto.NewFriendRequest;
import com.jspark.pw3_attendant.service.inoutday.dto.NewFriendResponse;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewFriendService {

    private final NewFriendRepository newFriendRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public NewFriendResponse save(NewFriendRequest request) {
        Student student = null;
        if (request.getStudentId() != null) {
            student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 학생입니다."));
        }

        NewFriend newFriend = NewFriend.builder()
            .name(request.getName())
            .birth(request.getBirth())
            .phone(request.getPhone())
            .student(student)
            .build();

        NewFriend savedNewFriend = newFriendRepository.save(newFriend);
        return new NewFriendResponse(savedNewFriend);
    }

    @Transactional
    public NewFriendResponse update(Long id, NewFriendRequest request) {
        NewFriend newFriend = newFriendRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 새친구입니다."));

        Student student = null;
        if (request.getStudentId() != null) {
            student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 학생입니다."));
        }

        newFriend.update(request.getName(), request.getBirth(), request.getPhone(), student);
        return new NewFriendResponse(newFriend);
    }

    @Transactional
    public void delete(Long id) {
        newFriendRepository.deleteById(id);
    }

    public List<NewFriendResponse> findAll() {
        return newFriendRepository.findAll().stream()
            .map(NewFriendResponse::new)
            .collect(Collectors.toList());
    }
}
