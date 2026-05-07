package com.ddoong2.direnvloader

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

// IDE Notification 헬퍼
object DirenvNotifier {

    private const val GROUP_ID = "Direnv Loader"
    private const val TITLE = "Direnv Loader"

    // direnv 미설치 경고 Notification 생성
    @JvmStatic
    internal fun createNotInstalledNotification(): Notification = Notification(
        GROUP_ID,
        TITLE,
        "direnv is not installed. Please install direnv and try again.",
        NotificationType.WARNING,
    )

    // .envrc blocked 경고 Notification 생성
    @JvmStatic
    internal fun createBlockedNotification(): Notification = Notification(
        GROUP_ID,
        TITLE,
        ".envrc is not allowed. Enable 'Trust .envrc' or run 'direnv allow' in terminal.",
        NotificationType.WARNING,
    )

    // 일반 오류 Notification 생성
    @JvmStatic
    internal fun createErrorNotification(message: String): Notification = Notification(
        GROUP_ID,
        TITLE,
        "Error occurred while running direnv: $message",
        NotificationType.ERROR,
    )

    // 환경변수 로드 성공 Notification 생성
    @JvmStatic
    internal fun createLoadedNotification(count: Int): Notification = Notification(
        GROUP_ID,
        TITLE,
        "Loaded $count direnv environment variable(s).",
        NotificationType.INFORMATION,
    )

    @JvmStatic
    fun notifyLoaded(project: Project, count: Int) {
        createLoadedNotification(count).notify(project)
    }

    @JvmStatic
    fun notifyNotInstalled(project: Project) {
        createNotInstalledNotification().notify(project)
    }

    @JvmStatic
    fun notifyBlocked(project: Project) {
        createBlockedNotification().notify(project)
    }

    @JvmStatic
    fun notifyError(project: Project, message: String) {
        createErrorNotification(message).notify(project)
    }
}
