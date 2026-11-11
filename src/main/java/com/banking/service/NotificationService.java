package com.banking.service;

import com.banking.dto.AdminDtos;
import com.banking.dto.CommonDtos.PageResponse;
import com.banking.entity.User;
import com.banking.enums.NotificationType;

public interface NotificationService {

    PageResponse<AdminDtos.NotificationResponse> list(int page, int size);

    long unreadCount();

    void markRead(Long id);

    void markAllRead();

    void notify(User user, String title, String message, NotificationType type, String link);
}
