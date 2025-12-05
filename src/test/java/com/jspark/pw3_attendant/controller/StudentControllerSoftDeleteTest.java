package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.service.Student.StudentService;
import com.jspark.pw3_attendant.service.StudentClass.StudentClassService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
public class StudentControllerSoftDeleteTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;

    @MockBean
    private StudentClassService studentClassService;

    @Test
    @DisplayName("학생을 soft delete 하면 200 OK를 반환한다.")
    void softDeleteStudent() throws Exception {
        // given
        Long studentId = 1L;

        // when & then
        mockMvc.perform(delete("/students/{id}", studentId))
                .andExpect(status().isOk());

        verify(studentService).deleteStudent(studentId);
    }

    @Test
    @DisplayName("삭제된 학생을 조회하면 404 Not Found를 반환해야 한다.")
    void getDeletedStudent() throws Exception {
        // given
        Long studentId = 1L;
        when(studentService.findById(studentId)).thenThrow(new IllegalArgumentException("학생을 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(get("/students/{id}", studentId))
                .andExpect(status().isNotFound());
    }
}
