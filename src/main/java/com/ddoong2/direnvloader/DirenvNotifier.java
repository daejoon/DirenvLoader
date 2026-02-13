package com.ddoong2.direnvloader;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

// IDE Notification 헬퍼
public final class DirenvNotifier {

    private static final String GROUP_ID = "Direnv Loader";
    private static final String TITLE = "Direnv Loader";

    private DirenvNotifier() {
    }

    // direnv 미설치 경고 Notification 생성
    static Notification createNotInstalledNotification() {
        return new Notification(
                GROUP_ID,
                TITLE,
                "direnv is not installed. Please install direnv and try again.",
                NotificationType.WARNING
        );
    }

    // .envrc blocked 경고 Notification 생성
    static Notification createBlockedNotification() {
        return new Notification(
                GROUP_ID,
                TITLE,
                ".envrc is not allowed. Enable 'Trust .envrc' or run 'direnv allow' in terminal.",
                NotificationType.WARNING
        );
    }

    // 일반 오류 Notification 생성
    static Notification createErrorNotification(String message) {
        return new Notification(
                GROUP_ID,
                TITLE,
                "Error occurred while running direnv: " + message,
                NotificationType.ERROR
        );
    }

    // 환경변수 로드 성공 Notification 생성
    static Notification createLoadedNotification(int count) {
        return new Notification(
                GROUP_ID,
                TITLE,
                "Loaded " + count + " direnv environment variable(s).",
                NotificationType.INFORMATION
        );
    }

    // 프로젝트에 알림 전송
    public static void notifyLoaded(Project project, int count) {
        createLoadedNotification(count).notify(project);
    }

    public static void notifyNotInstalled(Project project) {
        createNotInstalledNotification().notify(project);
    }

    public static void notifyBlocked(Project project) {
        createBlockedNotification().notify(project);
    }

    public static void notifyError(Project project, String message) {
        createErrorNotification(message).notify(project);
    }
}
