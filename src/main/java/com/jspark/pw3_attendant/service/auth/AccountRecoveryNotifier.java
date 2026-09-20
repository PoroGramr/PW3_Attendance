package com.jspark.pw3_attendant.service.auth;

import com.jspark.pw3_attendant.domain.admin.RecoveryChannel;

public interface AccountRecoveryNotifier {

    void send(RecoveryChannel channel, String destination, String subject, String content);
}
