package com.jspark.pw3_attendant.service;

import com.jspark.pw3_attendant.domain.ClassRoom;
import com.jspark.pw3_attendant.repository.ClassRoomRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassRoomService {

    private final ClassRoomRepository classRoomRepository;

    public ClassRoom findById(Long classRoomId) {
        return classRoomRepository.findById(classRoomId)
            .orElseThrow(() -> new IllegalArgumentException("반을 찾을 수 없습니다."));
    }

    public List<ClassRoom> findAll() {
        return classRoomRepository.findAll();
    }

    @Transactional
    public ClassRoom save(ClassRoom classRoom) {
        return classRoomRepository.save(classRoom);
    }

    @Transactional
    public void delete(Long classRoomId) {
        classRoomRepository.deleteById(classRoomId);
    }
}

