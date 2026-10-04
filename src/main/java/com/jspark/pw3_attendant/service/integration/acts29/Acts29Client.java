package com.jspark.pw3_attendant.service.integration.acts29;

import java.time.LocalDate;
import java.util.List;

public interface Acts29Client {

    Session open(LocalDate date, String grade, String className);

    interface Session extends AutoCloseable {
        List<Acts29StudentRecord> fetchAttendance();

        void saveAttendance(List<Acts29StudentRecord> records);

        @Override
        void close();
    }
}
