# DirenvLoader Plugin Rebranding Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 패키지명을 `com.ddoong2.direnvloader`로 변경하고, 알림 메시지를 영문화하고, direnv 공식 로고 기반 아이콘을 추가한다.

**Architecture:** 3개 독립 작업을 순서대로 진행. 패키지명 변경은 전 파일 원자적 변경, 알림 영문화는 DirenvNotifier + DirenvCommandExecutor 메시지 수정, 아이콘은 SVG 파일 생성 + plugin.xml 설정.

**Tech Stack:** Java 21, IntelliJ Platform Plugin SDK, Gradle Kotlin DSL, JUnit 5

---

### Task 1: 패키지명 변경 - 디렉토리 생성 및 소스 이동

**Files:**
- Rename directory: `src/main/java/com/github/direnvloader/` → `src/main/java/com/ddoong2/direnvloader/`
- Rename directory: `src/test/java/com/github/direnvloader/` → `src/test/java/com/ddoong2/direnvloader/`

**Step 1: 새 디렉토리 생성 및 파일 이동**

```bash
mkdir -p src/main/java/com/ddoong2/direnvloader
mkdir -p src/test/java/com/ddoong2/direnvloader

# 소스 파일 이동
mv src/main/java/com/github/direnvloader/*.java src/main/java/com/ddoong2/direnvloader/
mv src/test/java/com/github/direnvloader/*.java src/test/java/com/ddoong2/direnvloader/

# 빈 디렉토리 삭제
rm -rf src/main/java/com/github
rm -rf src/test/java/com/github
```

**Step 2: 모든 Java 파일의 package 선언 변경**

모든 `.java` 파일에서:
```
package com.github.direnvloader;
```
→
```
package com.ddoong2.direnvloader;
```

대상 파일 (소스 7개):
- `src/main/java/com/ddoong2/direnvloader/DirenvException.java`
- `src/main/java/com/ddoong2/direnvloader/DirenvBlockedException.java`
- `src/main/java/com/ddoong2/direnvloader/DirenvCommandExecutor.java`
- `src/main/java/com/ddoong2/direnvloader/DirenvNotifier.java`
- `src/main/java/com/ddoong2/direnvloader/DirenvRunConfigurationExtension.java`
- `src/main/java/com/ddoong2/direnvloader/DirenvSettings.java`
- `src/main/java/com/ddoong2/direnvloader/DirenvSettingsEditor.java`

대상 파일 (테스트 3개):
- `src/test/java/com/ddoong2/direnvloader/DirenvCommandExecutorTest.java`
- `src/test/java/com/ddoong2/direnvloader/DirenvNotifierTest.java`
- `src/test/java/com/ddoong2/direnvloader/DirenvRunConfigurationExtensionTest.java`

**Step 3: DirenvRunConfigurationExtension.java의 SERIALIZATION_ID 변경**

`src/main/java/com/ddoong2/direnvloader/DirenvRunConfigurationExtension.java:22`:
```java
// 변경 전
private static final String SERIALIZATION_ID = "com.github.direnvloader";
// 변경 후
private static final String SERIALIZATION_ID = "com.ddoong2.direnvloader";
```

**Step 4: plugin.xml 패키지 참조 변경**

`src/main/resources/META-INF/plugin.xml`:
```xml
<!-- 변경 전 -->
<id>com.github.direnvloader</id>
<runConfigurationExtension implementation="com.github.direnvloader.DirenvRunConfigurationExtension"/>

<!-- 변경 후 -->
<id>com.ddoong2.direnvloader</id>
<runConfigurationExtension implementation="com.ddoong2.direnvloader.DirenvRunConfigurationExtension"/>
```

**Step 5: gradle.properties의 pluginGroup 변경**

`gradle.properties:1`:
```properties
# 변경 전
pluginGroup = com.github.direnvloader
# 변경 후
pluginGroup = com.ddoong2.direnvloader
```

**Step 6: 빌드 및 테스트로 검증**

```bash
./gradlew clean test
```
Expected: BUILD SUCCESSFUL, 모든 테스트 통과

**Step 7: 커밋**

```bash
git add -A
git commit -m "refactor: 패키지명 com.github → com.ddoong2로 변경"
```

---

### Task 2: DirenvNotifier 알림 메시지 영문화

**Files:**
- Modify: `src/main/java/com/ddoong2/direnvloader/DirenvNotifier.java:16-53`
- Modify: `src/main/java/com/ddoong2/direnvloader/DirenvCommandExecutor.java:35,47,57,86,92`
- Test: `src/test/java/com/ddoong2/direnvloader/DirenvNotifierTest.java`

**Step 1: DirenvNotifierTest에 영문 메시지 검증 테스트 추가**

`src/test/java/com/ddoong2/direnvloader/DirenvNotifierTest.java`에 각 메서드의 영문 메시지 검증:

```java
// direnv 미설치 알림이 영문 메시지를 포함하는지 확인
@Test
void notifyNotInstalled_containsEnglishMessage() {
    Notification notification = DirenvNotifier.createNotInstalledNotification();
    assertTrue(notification.getContent().contains("not installed"));
}

// .envrc blocked 알림이 영문 메시지를 포함하는지 확인
@Test
void notifyBlocked_containsEnglishMessage() {
    Notification notification = DirenvNotifier.createBlockedNotification();
    assertTrue(notification.getContent().contains("not allowed"));
}

// 오류 알림이 영문 메시지를 포함하는지 확인
@Test
void notifyError_containsEnglishMessage() {
    Notification notification = DirenvNotifier.createErrorNotification("test error");
    assertTrue(notification.getContent().contains("Error occurred"));
}

// 로드 성공 알림이 영문 메시지를 포함하는지 확인
@Test
void notifyLoaded_containsEnglishMessage() {
    Notification notification = DirenvNotifier.createLoadedNotification(3);
    assertTrue(notification.getContent().contains("Loaded"));
    assertTrue(notification.getContent().contains("3"));
}
```

**Step 2: 테스트 실행하여 실패 확인**

```bash
./gradlew test --tests "com.ddoong2.direnvloader.DirenvNotifierTest"
```
Expected: 4개 새 테스트 FAIL (한글 메시지이므로 영문 키워드 없음)

**Step 3: DirenvNotifier.java 한글 → 영어 변경**

`src/main/java/com/ddoong2/direnvloader/DirenvNotifier.java`:

| 라인 | 변경 전 (한글) | 변경 후 (영어) |
|------|--------------|--------------|
| 21 | `"direnv가 설치되어 있지 않습니다. direnv를 설치한 후 다시 시도하세요."` | `"direnv is not installed. Please install direnv and try again."` |
| 31 | `".envrc가 허용되지 않았습니다. 'Trust .envrc' 옵션을 활성화하거나 터미널에서 'direnv allow'를 실행하세요."` | `".envrc is not allowed. Enable 'Trust .envrc' or run 'direnv allow' in terminal."` |
| 41 | `"direnv 실행 중 오류가 발생했습니다: " + message` | `"Error occurred while running direnv: " + message` |
| 51 | `"direnv 환경변수 " + count + "개를 로드했습니다."` | `"Loaded " + count + " direnv environment variable(s)."` |

**Step 4: DirenvCommandExecutor.java 한글 에러 메시지 → 영어 변경**

`src/main/java/com/ddoong2/direnvloader/DirenvCommandExecutor.java`:

| 라인 | 변경 전 | 변경 후 |
|------|--------|--------|
| 35 | `"direnv JSON 파싱 실패: "` | `"Failed to parse direnv JSON: "` |
| 47 | `"direnv export 실패 (exit code "` | `"direnv export failed (exit code "` |
| 57 | `"direnv allow 실패: "` | `"direnv allow failed: "` |
| 86 | `"direnv 명령이 타임아웃되었습니다 ("` | `"direnv command timed out ("` |
| 92 | `"direnv 실행 실패: "` | `"Failed to execute direnv: "` |

**Step 5: 테스트 실행하여 통과 확인**

```bash
./gradlew test --tests "com.ddoong2.direnvloader.DirenvNotifierTest"
```
Expected: 모든 테스트 PASS

**Step 6: 커밋**

```bash
git add src/main/java/com/ddoong2/direnvloader/DirenvNotifier.java \
        src/main/java/com/ddoong2/direnvloader/DirenvCommandExecutor.java \
        src/test/java/com/ddoong2/direnvloader/DirenvNotifierTest.java
git commit -m "feat: 알림 및 에러 메시지를 영어로 변경"
```

---

### Task 3: direnv 플러그인 아이콘 추가

**Files:**
- Create: `src/main/resources/pluginIcon.svg` (40x40, light theme 기본)
- Create: `src/main/resources/pluginIcon_dark.svg` (40x40, dark theme)
- 참고: IntelliJ Platform은 `pluginIcon.svg`를 플러그인 루트 리소스에서 자동 탐지

**Step 1: direnv 공식 로고 확인**

direnv 공식 사이트(https://direnv.net)에서 로고 디자인 확인.
direnv 로고는 녹색 계열의 'd' 심볼 또는 터미널 프롬프트 스타일.

**Step 2: pluginIcon.svg 생성 (light theme)**

`src/main/resources/pluginIcon.svg`:
- 크기: 40x40
- direnv 공식 로고 색상(녹색 #4CAF50 계열) 사용
- 밝은 배경에서 잘 보이도록 설계

**Step 3: pluginIcon_dark.svg 생성 (dark theme)**

`src/main/resources/pluginIcon_dark.svg`:
- 크기: 40x40
- 어두운 배경에서 잘 보이도록 밝은 계열로 조정

**Step 4: 빌드하여 아이콘 인식 확인**

```bash
./gradlew buildPlugin
```
Expected: BUILD SUCCESSFUL

**Step 5: runIde로 시각 확인 (수동)**

```bash
./gradlew runIde
```
IDE Settings → Plugins에서 아이콘 표시 확인

**Step 6: 커밋**

```bash
git add src/main/resources/pluginIcon.svg src/main/resources/pluginIcon_dark.svg
git commit -m "feat: direnv 로고 기반 플러그인 아이콘 추가"
```
