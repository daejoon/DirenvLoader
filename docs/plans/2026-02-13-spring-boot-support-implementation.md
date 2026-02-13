# Spring Boot Run Configuration 지원 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** IntelliJ Ultimate의 Spring Boot Run Configuration에서 Direnv 환경변수 주입을 지원하도록 빌드/설정을 수정한다.

**Architecture:** platformType을 IU로 전환하고 Spring Boot 플러그인을 optional 의존성으로 추가한다. 기존 `DirenvRunConfigurationExtension`이 이미 모든 RC 타입에 적용되므로 코드 변경은 불필요하다.

**Tech Stack:** IntelliJ Platform Gradle Plugin 2.3.0, Java 17, IntelliJ IDEA Ultimate 2024.1.7

---

### Task 1: platformType을 IU로 변경

**Files:**
- Modify: `gradle.properties:5`

**Step 1: platformType 변경**

`gradle.properties` 5번째 줄을 수정:

```properties
platformType = IU
```

**Step 2: 빌드 확인**

Run: `./gradlew assemble`
Expected: BUILD SUCCESSFUL

**Step 3: 커밋**

```bash
git add gradle.properties
git commit -m "chore: platformType을 IU로 변경"
```

---

### Task 2: Spring Boot 플러그인 빌드 의존성 추가

**Files:**
- Modify: `build.gradle.kts:22`

**Step 1: bundledPlugin 추가**

`build.gradle.kts`의 `intellijPlatform` 블록에서 `bundledPlugin("com.intellij.java")` 다음 줄에 추가:

```kotlin
bundledPlugin("com.intellij.spring.boot")
```

**Step 2: 빌드 확인**

Run: `./gradlew assemble`
Expected: BUILD SUCCESSFUL

**Step 3: 커밋**

```bash
git add build.gradle.kts
git commit -m "chore: Spring Boot 플러그인 빌드 의존성 추가"
```

---

### Task 3: plugin.xml에 optional 의존성 선언

**Files:**
- Modify: `src/main/resources/META-INF/plugin.xml:10`
- Create: `src/main/resources/META-INF/direnv-spring-boot.xml`

**Step 1: direnv-spring-boot.xml 생성**

`src/main/resources/META-INF/direnv-spring-boot.xml` 파일 생성:

```xml
<idea-plugin>
    <!-- Spring Boot Run Configuration 지원을 위한 optional dependency config -->
</idea-plugin>
```

**Step 2: plugin.xml에 optional depends 추가**

`plugin.xml`의 `<depends>com.intellij.modules.java</depends>` 다음 줄에 추가:

```xml
<depends optional="true" config-file="direnv-spring-boot.xml">com.intellij.spring.boot</depends>
```

**Step 3: 빌드 확인**

Run: `./gradlew assemble`
Expected: BUILD SUCCESSFUL

**Step 4: 기존 테스트 통과 확인**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, 모든 테스트 통과

**Step 5: 커밋**

```bash
git add src/main/resources/META-INF/direnv-spring-boot.xml src/main/resources/META-INF/plugin.xml
git commit -m "feat: Spring Boot Run Configuration optional 의존성 추가"
```

---

### Task 4: 수동 검증

**Step 1: runIde 실행**

Run: `./gradlew runIde`
Expected: IntelliJ IDEA Ultimate가 실행됨

**Step 2: Spring Boot Run Configuration 확인**

1. Ultimate IDE에서 Spring Boot 프로젝트를 열거나 Spring Boot RC를 생성
2. Run Configuration 설정에서 "Direnv" 탭이 표시되는지 확인
3. Enable Direnv 체크박스가 동작하는지 확인
