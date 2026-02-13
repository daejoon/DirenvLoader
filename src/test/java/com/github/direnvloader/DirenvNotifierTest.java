package com.github.direnvloader;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class DirenvNotifierTest extends BasePlatformTestCase {

    // direnv 미설치 알림이 WARNING 타입인지 확인
    public void testNotifyNotInstalled() {
        Notification notification = DirenvNotifier.createNotInstalledNotification();

        assertEquals("Direnv Loader", notification.getGroupId());
        assertEquals(NotificationType.WARNING, notification.getType());
        assertTrue(notification.getContent().contains("direnv"));
    }

    // .envrc blocked 알림이 WARNING 타입인지 확인
    public void testNotifyBlocked() {
        Notification notification = DirenvNotifier.createBlockedNotification();

        assertEquals("Direnv Loader", notification.getGroupId());
        assertEquals(NotificationType.WARNING, notification.getType());
        assertTrue(notification.getContent().contains(".envrc"));
    }

    // 일반 오류 알림이 ERROR 타입이고 메시지를 포함하는지 확인
    public void testNotifyError() {
        String message = "timeout exceeded";
        Notification notification = DirenvNotifier.createErrorNotification(message);

        assertEquals("Direnv Loader", notification.getGroupId());
        assertEquals(NotificationType.ERROR, notification.getType());
        assertTrue(notification.getContent().contains(message));
    }
}
