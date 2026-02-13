package com.github.direnvloader;

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
                "direnv가 설치되어 있지 않습니다. direnv를 설치한 후 다시 시도하세요.",
                NotificationType.WARNING
        );
    }

    // .envrc blocked 경고 Notification 생성
    static Notification createBlockedNotification() {
        return new Notification(
                GROUP_ID,
                TITLE,
                ".envrc가 허용되지 않았습니다. 'Trust .envrc' 옵션을 활성화하거나 터미널에서 'direnv allow'를 실행하세요.",
                NotificationType.WARNING
        );
    }

    // 일반 오류 Notification 생성
    static Notification createErrorNotification(String message) {
        return new Notification(
                GROUP_ID,
                TITLE,
                "direnv 실행 중 오류가 발생했습니다: " + message,
                NotificationType.ERROR
        );
    }

    // 프로젝트에 알림 전송
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
