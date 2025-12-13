package com.jspark.pw3_attendant.repository.message_log;

import com.jspark.pw3_attendant.domain.message_log.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {
}
