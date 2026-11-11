package com.banking.controller;

import com.banking.dto.AdminDtos;
import com.banking.dto.CommonDtos.ApiResponse;
import com.banking.dto.CommonDtos.MessageResponse;
import com.banking.dto.CommonDtos.PageResponse;
import com.banking.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "In-app notification inbox")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List the authenticated user's notifications")
    public ApiResponse<PageResponse<AdminDtos.NotificationResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(notificationService.list(page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get the unread notification count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.ok(Map.of("count", notificationService.unreadCount()));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ApiResponse<MessageResponse> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ApiResponse.ok("Notification marked as read", new MessageResponse("Notification marked as read"));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ApiResponse<MessageResponse> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.ok("All notifications marked as read",
                new MessageResponse("All notifications marked as read"));
    }
}
