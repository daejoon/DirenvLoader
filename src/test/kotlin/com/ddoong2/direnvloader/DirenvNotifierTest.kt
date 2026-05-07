package com.ddoong2.direnvloader

import com.intellij.notification.NotificationType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DirenvNotifierTest {

    // direnv 미설치 알림이 WARNING 타입인지 확인
    @Test
    fun notifyNotInstalled() {
        val notification = DirenvNotifier.createNotInstalledNotification()

        assertEquals("Direnv Loader", notification.groupId)
        assertEquals(NotificationType.WARNING, notification.type)
        assertTrue(notification.content.contains("direnv"))
    }

    // .envrc blocked 알림이 WARNING 타입인지 확인
    @Test
    fun notifyBlocked() {
        val notification = DirenvNotifier.createBlockedNotification()

        assertEquals("Direnv Loader", notification.groupId)
        assertEquals(NotificationType.WARNING, notification.type)
        assertTrue(notification.content.contains(".envrc"))
    }

    // 일반 오류 알림이 ERROR 타입이고 메시지를 포함하는지 확인
    @Test
    fun notifyError() {
        val message = "timeout exceeded"
        val notification = DirenvNotifier.createErrorNotification(message)

        assertEquals("Direnv Loader", notification.groupId)
        assertEquals(NotificationType.ERROR, notification.type)
        assertTrue(notification.content.contains(message))
    }

    // 환경변수 로드 성공 알림이 INFORMATION 타입이고 개수를 포함하는지 확인
    @Test
    fun notifyLoaded() {
        val notification = DirenvNotifier.createLoadedNotification(5)

        assertEquals("Direnv Loader", notification.groupId)
        assertEquals(NotificationType.INFORMATION, notification.type)
        assertTrue(notification.content.contains("5"))
    }

    // direnv 미설치 알림이 영문 메시지를 포함하는지 확인
    @Test
    fun notifyNotInstalled_containsEnglishMessage() {
        val notification = DirenvNotifier.createNotInstalledNotification()
        assertTrue(notification.content.contains("not installed"))
    }

    // .envrc blocked 알림이 영문 메시지를 포함하는지 확인
    @Test
    fun notifyBlocked_containsEnglishMessage() {
        val notification = DirenvNotifier.createBlockedNotification()
        assertTrue(notification.content.contains("not allowed"))
    }

    // 오류 알림이 영문 메시지를 포함하는지 확인
    @Test
    fun notifyError_containsEnglishMessage() {
        val notification = DirenvNotifier.createErrorNotification("test error")
        assertTrue(notification.content.contains("Error occurred"))
    }

    // 로드 성공 알림이 영문 메시지를 포함하는지 확인
    @Test
    fun notifyLoaded_containsEnglishMessage() {
        val notification = DirenvNotifier.createLoadedNotification(3)
        assertTrue(notification.content.contains("Loaded"))
        assertTrue(notification.content.contains("3"))
    }
}
