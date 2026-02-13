# Direnv Loader Plugin Design

## 개요

IntelliJ IDEA Run Configuration 실행 시 `direnv` 환경변수를 자동으로 주입하는 플러그인.

## 요구사항

| 항목 | 결정 |
|---|---|
| 대상 IDE | IntelliJ IDEA (Community + Ultimate) |
| 플랫폼 버전 | 2024.1+ |
| 모듈 구조 | 단일 모듈 |
| RC 타입 | Java/Kotlin Application (`JavaRunConfigurationBase`) |
| 설정 범위 | 개별 Run Configuration 단위 |
| 언어 | Java 21 |

## 아키텍처

### 접근 방식: RunConfigurationExtension 기반

`RunConfigurationExtensionBase`를 상속하여 Run Configuration 설정 패널에 UI를 추가하고, 실행 직전 `patchCommandLine()`에서 환경변수를 주입한다.

### 프로젝트 구조

```
DirenvLoader/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── src/main/
│   ├── java/com/github/direnvloader/
│   │   ├── DirenvRunConfigurationExtension.java
│   │   ├── DirenvCommandExecutor.java
│   │   └── DirenvNotifier.java
│   └── resources/
│       └── META-INF/
│           └── plugin.xml
└── src/test/
    └── java/com/github/direnvloader/
        ├── DirenvCommandExecutorTest.java
        └── DirenvRunConfigurationExtensionTest.java
```

## 핵심 클래스

### DirenvRunConfigurationExtension

`RunConfigurationExtensionBase` 상속. Run Configuration 설정 패널에 UI를 추가하고 실행 시점에 환경변수를 주입한다.

- `getSerializationId()` - 설정 직렬화 ID
- `getEditorTitle()` - "Direnv" 섹션 제목
- `createEditor()` - Enable Direnv, Trust .envrc 체크박스 2개 UI 패널
- `readExternal(element)` / `writeExternal(element)` - XML 설정 저장/복원
- `isApplicableFor(rc)` - `JavaRunConfigurationBase` 타입만 true
- `patchCommandLine(cmdLine)` - 실행 직전, 환경변수 병합

### DirenvCommandExecutor

`direnv` CLI 호출 담당.

- `exportJson(workDir)` - `direnv export json` 실행 → `Map<String,String>` 반환
- `allow(workDir)` - `direnv allow` 실행
- `isDirenvInstalled()` - direnv 설치 여부 확인
- `GeneralCommandLine` API 사용, 타임아웃 5초

### DirenvNotifier

IDE Notification 헬퍼.

- `notifyBlocked(project)` - .envrc 미허용 경고
- `notifyNotInstalled(project)` - direnv 미설치 경고
- `notifyError(project, msg)` - 일반 오류 알림
- `NotificationGroup` + Balloon 타입

## 실행 흐름

```
Run 버튼 클릭
  → isApplicableFor(rc) 확인
  → Enable Direnv 체크 확인
  → isDirenvInstalled() 확인 (미설치 시 알림 + 중단)
  → Trust .envrc 체크 시 allow(workDir) 실행
  → exportJson(workDir) 실행
    → 성공: JSON 파싱 → 환경변수 병합
    → blocked: notifyBlocked() → 중단
    → 오류: notifyError() → 중단
  → patchCommandLine()에서 병합된 환경변수 적용
```

## 환경변수 병합 규칙

우선순위: **RC 설정값 > direnv 값 > 시스템 값**

- direnv에서 가져온 값을 기본으로 설정
- Run Configuration에서 사용자가 직접 설정한 환경변수가 덮어쓰기(우선)

## 에러 처리

| 상황 | 처리 |
|---|---|
| direnv 미설치 | Balloon 경고 + 실행 중단 |
| .envrc blocked | Balloon 경고 + 실행 중단 |
| direnv export 타임아웃 (5초) | Balloon 오류 + 실행 중단 |
| JSON 파싱 실패 | Balloon 오류 + 실행 중단 |
| working directory null | 프로젝트 basePath로 fallback |

실행 중단 시 `ExecutionException`을 throw하여 IDE가 자연스럽게 실행을 취소한다.

## plugin.xml

```xml
<idea-plugin>
    <id>com.github.direnvloader</id>
    <name>Direnv Loader</name>
    <vendor>daejoon</vendor>
    <depends>com.intellij.modules.java</depends>

    <extensions defaultExtensionNs="com.intellij">
        <runConfigurationExtension
            implementation="com.github.direnvloader.DirenvRunConfigurationExtension"/>
        <notificationGroup
            id="Direnv Loader"
            displayType="BALLOON"/>
    </extensions>
</idea-plugin>
```
