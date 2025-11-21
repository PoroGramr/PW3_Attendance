package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.service.Student.StudentService;
import com.jspark.pw3_attendant.service.Student.dto.MonthlyStudentRegistrationResponse;
import com.jspark.pw3_attendant.service.StudentClass.StudentClassService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;

    @MockBean
    private StudentClassService studentClassService;

    @Test
    @DisplayName("연도별 월별 학생 등록 현황 조회")
    void getMonthlyRegistrations() throws Exception {
        // given
        int year = 2024;
        List<MonthlyStudentRegistrationResponse> response = List.of(
                MonthlyStudentRegistrationResponse.builder().month(1).students(List.of()).build()
        );
        given(studentService.findStudentsByYearGroupByMonth(year)).willReturn(response);

        // when & then
        mockMvc.perform(get("/students/registrations/by-year/{year}", year))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value(1));
    }
}
