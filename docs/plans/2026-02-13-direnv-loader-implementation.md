# Direnv Loader Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** IntelliJ IDEA Run Configuration 실행 시 direnv 환경변수를 자동 주입하는 플러그인을 구현한다.

**Architecture:** `RunConfigurationExtensionBase`를 상속하여 Java/Kotlin Application Run Configuration에 체크박스 UI를 추가하고, 실행 직전 `patchCommandLine()`에서 `direnv export json` 결과를 환경변수로 병합한다.

**Tech Stack:** Java 21, Gradle Kotlin DSL, IntelliJ Platform Gradle Plugin 2.x, IntelliJ Platform SDK 2024.1+, JUnit 5

---

### Task 1: Gradle 빌드 환경 구성

**Files:**
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `build.gradle.kts`

**Step 1: settings.gradle.kts 생성**

```kotlin
rootProject.name = "DirenvLoader"
```

**Step 2: gradle.properties 생성**

```properties
pluginGroup = com.github.direnvloader
pluginName = Direnv Loader
pluginVersion = 0.1.0

platformType = IC
platformVersion = 2024.1.7
sinceBuildVersion = 241
untilBuildVersion = 253.*

javaVersion = 21

org.gradle.jvmargs = -Xmx2048m
```

**Step 3: build.gradle.kts 생성**

```kotlin
plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        val type = providers.gradleProperty("platformType")
        val version = providers.gradleProperty("platformVersion")
        create(type, version)

        bundledPlugin("com.intellij.java")

        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        ideaVersion {
            sinceBuild = providers.gradleProperty("sinceBuildVersion")
            untilBuild = providers.gradleProperty("untilBuildVersion")
        }
    }
}

tasks {
    withType<JavaCompile> {
        val javaVersion = providers.gradleProperty("javaVersion").get()
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        options.encoding = "UTF-8"
    }

    test {
        useJUnitPlatform()
    }
}
```

**Step 4: Gradle Wrapper 설정**

Run: `gradle wrapper --gradle-version 8.12`

**Step 5: 빌드 확인**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL (소스가 아직 없으므로 빈 빌드 성공)

**Step 6: Commit**

```bash
git add settings.gradle.kts gradle.properties build.gradle.kts \
      gradle/ gradlew gradlew.bat
git commit -m "build: Gradle 빌드 환경 구성

IntelliJ Platform Gradle Plugin 2.x 기반 빌드 설정.
대상 플랫폼: IntelliJ IDEA 2024.1+, Java 21."
```

---

### Task 2: plugin.xml 및 디렉토리 구조 생성

**Files:**
- Create: `src/main/resources/META-INF/plugin.xml`

**Step 1: plugin.xml 생성**

```xml
<idea-plugin>
    <id>com.github.direnvloader</id>
    <name>Direnv Loader</name>
    <vendor>daejoon</vendor>
    <description><![CDATA[
    Run Configuration 실행 시 direnv 환경변수를 자동으로 주입합니다.
    ]]></description>

    <depends>com.intellij.modules.platform</depends>
    <depends>com.intellij.modules.java</depends>

    <extensions defaultExtensionNs="com.intellij">
        <notificationGroup
                id="Direnv Loader"
                displayType="BALLOON"/>
    </extensions>
</idea-plugin>
```

> 참고: `runConfigurationExtension` 등록은 Task 6에서 Extension 클래스 구현 후 추가한다.

**Step 2: 빌드 확인**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/resources/META-INF/plugin.xml
git commit -m "chore: plugin.xml 및 소스 디렉토리 구조 생성"
```

---

### Task 3: DirenvNotifier 구현 (TDD)

**Files:**
- Create: `src/main/java/com/github/direnvloader/DirenvNotifier.java`
- Create: `src/test/java/com/github/direnvloader/DirenvNotifierTest.java`

**Step 1: 실패하는 테스트 작성**

```java
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
```

**Step 2: 테스트 실행하여 실패 확인**

Run: `./gradlew test --tests "com.github.direnvloader.DirenvNotifierTest"`
Expected: FAIL (DirenvNotifier 클래스 미존재)

**Step 3: 최소 구현 작성**

```java
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
```

**Step 4: 테스트 실행하여 통과 확인**

Run: `./gradlew test --tests "com.github.direnvloader.DirenvNotifierTest"`
Expected: PASS (3 tests)

**Step 5: Commit**

```bash
git add src/main/java/com/github/direnvloader/DirenvNotifier.java \
      src/test/java/com/github/direnvloader/DirenvNotifierTest.java
git commit -m "feat: DirenvNotifier 구현

IDE Balloon 알림 헬퍼. 미설치/blocked/오류 3가지 알림 타입 지원."
```

---

### Task 4: DirenvCommandExecutor 구현 (TDD)

**Files:**
- Create: `src/main/java/com/github/direnvloader/DirenvCommandExecutor.java`
- Create: `src/test/java/com/github/direnvloader/DirenvCommandExecutorTest.java`

**Step 1: 실패하는 테스트 작성**

```java
package com.github.direnvloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DirenvCommandExecutorTest {

    // 유효한 JSON 출력을 Map으로 파싱하는지 확인
    @Test
    void parseExportJson_validJson_returnsMap() {
        String json = "{\"FOO\":\"bar\",\"BAZ\":\"qux\"}";

        Map<String, String> result = DirenvCommandExecutor.parseExportJson(json);

        assertEquals(2, result.size());
        assertEquals("bar", result.get("FOO"));
        assertEquals("qux", result.get("BAZ"));
    }

    // 빈 JSON 객체는 빈 Map 반환
    @Test
    void parseExportJson_emptyJson_returnsEmptyMap() {
        String json = "{}";

        Map<String, String> result = DirenvCommandExecutor.parseExportJson(json);

        assertTrue(result.isEmpty());
    }

    // null 또는 빈 문자열은 빈 Map 반환
    @Test
    void parseExportJson_nullOrEmpty_returnsEmptyMap() {
        assertTrue(DirenvCommandExecutor.parseExportJson(null).isEmpty());
        assertTrue(DirenvCommandExecutor.parseExportJson("").isEmpty());
        assertTrue(DirenvCommandExecutor.parseExportJson("  ").isEmpty());
    }

    // 잘못된 JSON은 IOException 발생
    @Test
    void parseExportJson_invalidJson_throwsException() {
        assertThrows(RuntimeException.class, () ->
                DirenvCommandExecutor.parseExportJson("not json"));
    }
}
```

**Step 2: 테스트 실행하여 실패 확인**

Run: `./gradlew test --tests "com.github.direnvloader.DirenvCommandExecutorTest"`
Expected: FAIL (DirenvCommandExecutor 클래스 미존재)

**Step 3: 최소 구현 작성**

```java
package com.github.direnvloader;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

// direnv CLI 실행기
public final class DirenvCommandExecutor {

    private static final int TIMEOUT_MS = 5_000;
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private DirenvCommandExecutor() {
    }

    // direnv export json 결과 JSON을 Map으로 파싱
    static Map<String, String> parseExportJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, String> result = GSON.fromJson(json, MAP_TYPE);
            return result != null ? result : Collections.emptyMap();
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("direnv JSON 파싱 실패: " + e.getMessage(), e);
        }
    }

    // direnv export json 실행하여 환경변수 Map 반환
    public static Map<String, String> exportJson(File workDir) throws DirenvException {
        ProcessOutput output = execute("export", "json", workDir);
        if (output.getExitCode() != 0) {
            String stderr = output.getStderr().trim();
            if (stderr.contains("is blocked")) {
                throw new DirenvBlockedException(stderr);
            }
            throw new DirenvException("direnv export 실패 (exit code " + output.getExitCode() + "): " + stderr);
        }
        String stdout = output.getStdout().trim();
        return parseExportJson(stdout);
    }

    // direnv allow 실행
    public static void allow(File workDir) throws DirenvException {
        ProcessOutput output = execute("allow", null, workDir);
        if (output.getExitCode() != 0) {
            throw new DirenvException("direnv allow 실패: " + output.getStderr().trim());
        }
    }

    // direnv 설치 여부 확인
    public static boolean isDirenvInstalled() {
        try {
            GeneralCommandLine cmd = new GeneralCommandLine("direnv", "version");
            cmd.setCharset(StandardCharsets.UTF_8);
            CapturingProcessHandler handler = new CapturingProcessHandler(cmd);
            ProcessOutput output = handler.runProcess(TIMEOUT_MS);
            return output.getExitCode() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // direnv 명령 실행 공통 메서드
    private static ProcessOutput execute(String command, String subCommand, File workDir)
            throws DirenvException {
        try {
            GeneralCommandLine cmd = subCommand != null
                    ? new GeneralCommandLine("direnv", command, subCommand)
                    : new GeneralCommandLine("direnv", command);
            cmd.setCharset(StandardCharsets.UTF_8);
            cmd.setWorkDirectory(workDir);
            CapturingProcessHandler handler = new CapturingProcessHandler(cmd);
            ProcessOutput output = handler.runProcess(TIMEOUT_MS);
            if (output.isTimeout()) {
                throw new DirenvException("direnv 명령이 타임아웃되었습니다 (" + TIMEOUT_MS + "ms)");
            }
            return output;
        } catch (DirenvException e) {
            throw e;
        } catch (Exception e) {
            throw new DirenvException("direnv 실행 실패: " + e.getMessage(), e);
        }
    }
}
```

**Step 4: DirenvException, DirenvBlockedException 생성**

`src/main/java/com/github/direnvloader/DirenvException.java`:
```java
package com.github.direnvloader;

// direnv 실행 관련 예외
public class DirenvException extends Exception {
    public DirenvException(String message) {
        super(message);
    }

    public DirenvException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

`src/main/java/com/github/direnvloader/DirenvBlockedException.java`:
```java
package com.github.direnvloader;

// .envrc가 blocked 상태일 때 발생하는 예외
public class DirenvBlockedException extends DirenvException {
    public DirenvBlockedException(String message) {
        super(message);
    }
}
```

**Step 5: 테스트 실행하여 통과 확인**

Run: `./gradlew test --tests "com.github.direnvloader.DirenvCommandExecutorTest"`
Expected: PASS (4 tests)

**Step 6: Commit**

```bash
git add src/main/java/com/github/direnvloader/DirenvCommandExecutor.java \
      src/main/java/com/github/direnvloader/DirenvException.java \
      src/main/java/com/github/direnvloader/DirenvBlockedException.java \
      src/test/java/com/github/direnvloader/DirenvCommandExecutorTest.java
git commit -m "feat: DirenvCommandExecutor 구현

direnv CLI 실행기. export json, allow, 설치 확인 기능.
JSON 파싱은 Gson 사용, 타임아웃 5초."
```

---

### Task 5: DirenvRunConfigurationExtension 구현 (TDD)

**Files:**
- Create: `src/main/java/com/github/direnvloader/DirenvRunConfigurationExtension.java`
- Create: `src/test/java/com/github/direnvloader/DirenvRunConfigurationExtensionTest.java`

**Step 1: 환경변수 병합 로직 테스트 작성**

```java
package com.github.direnvloader;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DirenvRunConfigurationExtensionTest {

    // direnv 값 위에 RC 설정값이 덮어쓰는지 확인
    @Test
    void mergeEnvironment_rcOverridesDirenv() {
        Map<String, String> direnvEnv = new HashMap<>();
        direnvEnv.put("FOO", "from_direnv");
        direnvEnv.put("BAR", "from_direnv");

        Map<String, String> rcEnv = new HashMap<>();
        rcEnv.put("FOO", "from_rc");

        Map<String, String> result = DirenvRunConfigurationExtension.mergeEnvironment(direnvEnv, rcEnv);

        assertEquals("from_rc", result.get("FOO"));
        assertEquals("from_direnv", result.get("BAR"));
    }

    // direnv 환경변수가 빈 경우 RC 값만 유지
    @Test
    void mergeEnvironment_emptyDirenv_returnsRcOnly() {
        Map<String, String> direnvEnv = new HashMap<>();
        Map<String, String> rcEnv = new HashMap<>();
        rcEnv.put("KEY", "value");

        Map<String, String> result = DirenvRunConfigurationExtension.mergeEnvironment(direnvEnv, rcEnv);

        assertEquals(1, result.size());
        assertEquals("value", result.get("KEY"));
    }

    // RC 환경변수가 빈 경우 direnv 값만 유지
    @Test
    void mergeEnvironment_emptyRc_returnsDirenvOnly() {
        Map<String, String> direnvEnv = new HashMap<>();
        direnvEnv.put("KEY", "value");
        Map<String, String> rcEnv = new HashMap<>();

        Map<String, String> result = DirenvRunConfigurationExtension.mergeEnvironment(direnvEnv, rcEnv);

        assertEquals(1, result.size());
        assertEquals("value", result.get("KEY"));
    }
}
```

**Step 2: 테스트 실행하여 실패 확인**

Run: `./gradlew test --tests "com.github.direnvloader.DirenvRunConfigurationExtensionTest"`
Expected: FAIL

**Step 3: Extension 클래스 구현**

```java
package com.github.direnvloader;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.impl.RunConfigurationExtensionBase;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Run Configuration에 direnv 환경변수 주입 기능을 추가하는 Extension
public class DirenvRunConfigurationExtension
        extends RunConfigurationExtensionBase<RunConfigurationBase<?>> {

    private static final String SERIALIZATION_ID = "com.github.direnvloader";
    private static final String ATTR_ENABLED = "direnv-enabled";
    private static final String ATTR_TRUST = "direnv-trust";

    @NotNull
    @Override
    protected String getSerializationId() {
        return SERIALIZATION_ID;
    }

    @Override
    protected void readExternal(@NotNull RunConfigurationBase<?> runConfiguration,
                                @NotNull Element element) {
        boolean enabled = Boolean.parseBoolean(element.getAttributeValue(ATTR_ENABLED, "false"));
        boolean trust = Boolean.parseBoolean(element.getAttributeValue(ATTR_TRUST, "false"));
        DirenvSettings settings = new DirenvSettings(enabled, trust);
        runConfiguration.putCopyableUserData(DirenvSettings.KEY, settings);
    }

    @Override
    protected void writeExternal(@NotNull RunConfigurationBase<?> runConfiguration,
                                 @NotNull Element element) {
        DirenvSettings settings = runConfiguration.getCopyableUserData(DirenvSettings.KEY);
        if (settings != null) {
            element.setAttribute(ATTR_ENABLED, String.valueOf(settings.isEnabled()));
            element.setAttribute(ATTR_TRUST, String.valueOf(settings.isTrust()));
        }
    }

    @Nullable
    @Override
    protected String getEditorTitle() {
        return "Direnv";
    }

    @Override
    protected boolean isApplicableFor(@NotNull RunConfigurationBase<?> configuration) {
        return true;
    }

    @Override
    public boolean isEnabledFor(@NotNull RunConfigurationBase<?> applicableConfiguration,
                                @Nullable RunnerSettings runnerSettings) {
        DirenvSettings settings = applicableConfiguration.getCopyableUserData(DirenvSettings.KEY);
        return settings != null && settings.isEnabled();
    }

    @Nullable
    @Override
    protected <T extends RunConfigurationBase<?>> SettingsEditor<T> createEditor(@NotNull T configuration) {
        return new DirenvSettingsEditor<>();
    }

    @Override
    protected void patchCommandLine(@NotNull RunConfigurationBase<?> configuration,
                                    @Nullable RunnerSettings runnerSettings,
                                    @NotNull GeneralCommandLine cmdLine,
                                    @NotNull String runnerId) throws ExecutionException {
        DirenvSettings settings = configuration.getCopyableUserData(DirenvSettings.KEY);
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        Project project = configuration.getProject();
        File workDir = resolveWorkDir(project);

        // direnv 설치 확인
        if (!DirenvCommandExecutor.isDirenvInstalled()) {
            DirenvNotifier.notifyNotInstalled(project);
            throw new ExecutionException("direnv가 설치되어 있지 않습니다.");
        }

        try {
            // Trust 옵션 활성화 시 direnv allow 실행
            if (settings.isTrust()) {
                DirenvCommandExecutor.allow(workDir);
            }

            // direnv export json으로 환경변수 로드
            Map<String, String> direnvEnv = DirenvCommandExecutor.exportJson(workDir);
            Map<String, String> rcEnv = cmdLine.getEnvironment();
            Map<String, String> merged = mergeEnvironment(direnvEnv, rcEnv);

            // 병합된 환경변수 적용
            cmdLine.getEnvironment().clear();
            cmdLine.getEnvironment().putAll(merged);

        } catch (DirenvBlockedException e) {
            DirenvNotifier.notifyBlocked(project);
            throw new ExecutionException(e.getMessage());
        } catch (DirenvException e) {
            DirenvNotifier.notifyError(project, e.getMessage());
            throw new ExecutionException(e.getMessage());
        }
    }

    // 환경변수 병합: direnv 값을 기본으로, RC 설정값이 우선
    static Map<String, String> mergeEnvironment(Map<String, String> direnvEnv,
                                                Map<String, String> rcEnv) {
        Map<String, String> merged = new LinkedHashMap<>(direnvEnv);
        merged.putAll(rcEnv);
        return merged;
    }

    // 프로젝트 basePath에서 작업 디렉토리 결정
    private File resolveWorkDir(Project project) {
        String basePath = project.getBasePath();
        if (basePath != null) {
            return new File(basePath);
        }
        return new File(System.getProperty("user.dir"));
    }
}
```

**Step 4: DirenvSettings 데이터 클래스 생성**

`src/main/java/com/github/direnvloader/DirenvSettings.java`:
```java
package com.github.direnvloader;

import com.intellij.openapi.util.Key;

// Run Configuration별 direnv 설정 데이터
public class DirenvSettings {

    static final Key<DirenvSettings> KEY = Key.create("direnv.settings");

    private boolean enabled;
    private boolean trust;

    public DirenvSettings(boolean enabled, boolean trust) {
        this.enabled = enabled;
        this.trust = trust;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTrust() {
        return trust;
    }

    public void setTrust(boolean trust) {
        this.trust = trust;
    }
}
```

**Step 5: DirenvSettingsEditor UI 클래스 생성**

`src/main/java/com/github/direnvloader/DirenvSettingsEditor.java`:
```java
package com.github.direnvloader;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

// Run Configuration 설정 패널 UI (체크박스 2개)
public class DirenvSettingsEditor<T extends RunConfigurationBase<?>> extends SettingsEditor<T> {

    private final JCheckBox enableDirenvCheckBox = new JCheckBox("Enable Direnv");
    private final JCheckBox trustEnvrcCheckBox = new JCheckBox("Trust .envrc (auto direnv allow)");
    private final JPanel panel = new JPanel();

    public DirenvSettingsEditor() {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(enableDirenvCheckBox);
        panel.add(trustEnvrcCheckBox);
    }

    @Override
    protected void resetEditorFrom(@NotNull T configuration) {
        DirenvSettings settings = configuration.getCopyableUserData(DirenvSettings.KEY);
        if (settings != null) {
            enableDirenvCheckBox.setSelected(settings.isEnabled());
            trustEnvrcCheckBox.setSelected(settings.isTrust());
        } else {
            enableDirenvCheckBox.setSelected(false);
            trustEnvrcCheckBox.setSelected(false);
        }
    }

    @Override
    protected void applyEditorTo(@NotNull T configuration) {
        DirenvSettings settings = new DirenvSettings(
                enableDirenvCheckBox.isSelected(),
                trustEnvrcCheckBox.isSelected()
        );
        configuration.putCopyableUserData(DirenvSettings.KEY, settings);
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return panel;
    }
}
```

**Step 6: 테스트 실행하여 통과 확인**

Run: `./gradlew test --tests "com.github.direnvloader.DirenvRunConfigurationExtensionTest"`
Expected: PASS (3 tests)

**Step 7: Commit**

```bash
git add src/main/java/com/github/direnvloader/DirenvRunConfigurationExtension.java \
      src/main/java/com/github/direnvloader/DirenvSettings.java \
      src/main/java/com/github/direnvloader/DirenvSettingsEditor.java \
      src/test/java/com/github/direnvloader/DirenvRunConfigurationExtensionTest.java
git commit -m "feat: DirenvRunConfigurationExtension 구현

RunConfigurationExtensionBase 기반 확장.
Enable Direnv / Trust .envrc 체크박스 UI.
환경변수 병합: RC 설정값 > direnv 값."
```

---

### Task 6: plugin.xml에 Extension 등록 및 통합 빌드 검증

**Files:**
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: plugin.xml에 runConfigurationExtension 등록**

`plugin.xml`의 `<extensions>` 블록에 추가:
```xml
<runConfigurationExtension
        implementation="com.github.direnvloader.DirenvRunConfigurationExtension"/>
```

**Step 2: 전체 테스트 실행**

Run: `./gradlew test`
Expected: PASS (전체 7 tests)

**Step 3: 플러그인 빌드 확인**

Run: `./gradlew buildPlugin`
Expected: BUILD SUCCESSFUL, `build/distributions/` 아래에 zip 생성

**Step 4: Commit**

```bash
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: plugin.xml에 RunConfigurationExtension 등록

플러그인 통합 빌드 및 전체 테스트 통과 확인."
```

---

### Task 7: 수동 검증 (runIde)

**Step 1: 개발 IDE 실행**

Run: `./gradlew runIde`

**Step 2: 수동 검증 체크리스트**

- [ ] Java Application Run Configuration 편집 화면에 "Direnv" 탭 표시
- [ ] Enable Direnv 체크박스 on/off 동작
- [ ] Trust .envrc 체크박스 on/off 동작
- [ ] 설정 저장 후 재오픈 시 값 유지
- [ ] .envrc 파일이 있는 프로젝트에서 실행 시 환경변수 주입 확인
- [ ] direnv 미설치 상태에서 실행 시 Balloon 경고 표시

**Step 3: 발견된 이슈가 있으면 수정 후 Commit**
