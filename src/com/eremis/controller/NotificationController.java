package com.eremis.controller;

import com.eremis.model.Notification;
import com.eremis.service.NotificationService;
import java.util.List;

public class NotificationController {
    private final NotificationService notifService = new NotificationService();
    public List<Notification> getForUser(int userId) { return notifService.getForUser(userId); }
    public int countUnread(int userId)                { return notifService.countUnread(userId); }
    public void markAllRead(int userId)               { notifService.markAllRead(userId); }
}
