package com.github.direnvloader;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirenvNotifierTest {

    // direnv 미설치 알림이 WARNING 타입인지 확인
    @Test
    void notifyNotInstalled() {
        Notification notification = DirenvNotifier.createNotInstalledNotification();

        assertEquals("Direnv Loader", notification.getGroupId());
        assertEquals(NotificationType.WARNING, notification.getType());
        assertTrue(notification.getContent().contains("direnv"));
    }

    // .envrc blocked 알림이 WARNING 타입인지 확인
    @Test
    void notifyBlocked() {
        Notification notification = DirenvNotifier.createBlockedNotification();

        assertEquals("Direnv Loader", notification.getGroupId());
        assertEquals(NotificationType.WARNING, notification.getType());
        assertTrue(notification.getContent().contains(".envrc"));
    }

    // 일반 오류 알림이 ERROR 타입이고 메시지를 포함하는지 확인
    @Test
    void notifyError() {
        String message = "timeout exceeded";
        Notification notification = DirenvNotifier.createErrorNotification(message);

        assertEquals("Direnv Loader", notification.getGroupId());
        assertEquals(NotificationType.ERROR, notification.getType());
        assertTrue(notification.getContent().contains(message));
    }

    // 환경변수 로드 성공 알림이 INFORMATION 타입이고 개수를 포함하는지 확인
    @Test
    void notifyLoaded() {
        Notification notification = DirenvNotifier.createLoadedNotification(5);

        assertEquals("Direnv Loader", notification.getGroupId());
        assertEquals(NotificationType.INFORMATION, notification.getType());
        assertTrue(notification.getContent().contains("5"));
    }
}
