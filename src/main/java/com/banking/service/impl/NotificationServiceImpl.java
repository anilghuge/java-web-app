package com.banking.service.impl;

import com.banking.dto.AdminDtos;
import com.banking.dto.CommonDtos.PageResponse;
import com.banking.entity.Notification;
import com.banking.entity.User;
import com.banking.enums.NotificationType;
import com.banking.exception.ApiException;
import com.banking.mapper.AdminMapper;
import com.banking.repository.NotificationRepository;
import com.banking.service.AuthenticatedUserService;
import com.banking.service.NotificationService;
import com.banking.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminDtos.NotificationResponse> list(int page, int size) {
        User user = authenticatedUserService.user();
        Pageable pageable = PaginationUtil.pageable(page, size, "createdAt", "desc");
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        return new PageResponse<>(
                notifications.getContent().stream().map(AdminMapper::toNotification).toList(),
                notifications.getNumber(), notifications.getSize(), notifications.getTotalElements(),
                notifications.getTotalPages(), notifications.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByUserIdAndReadFalse(authenticatedUserService.user().getId());
    }

    @Override
    @Transactional
    public void markRead(Long id) {
        User user = authenticatedUserService.user();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("NOTIFICATION_NOT_FOUND", "Notification not found"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("NOTIFICATION_ACCESS_DENIED", "This notification does not belong to you");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllRead() {
        notificationRepository.markAllRead(authenticatedUserService.user().getId());
    }

    @Override
    @Transactional
    public void notify(User user, String title, String message, NotificationType type, String link) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setLink(link);
        notificationRepository.save(notification);
    }
}
