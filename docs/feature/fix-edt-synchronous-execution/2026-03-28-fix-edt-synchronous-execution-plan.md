# EDT Synchronous Execution 오류 수정 — 구현 Plan

> 관련 문서: [Task →](./2026-03-28-fix-edt-synchronous-execution-task.md)
> 날짜: 2026-03-28
> 브랜치: `main`

## 구현 원칙

- 변경 범위를 `DirenvCommandExecutor.isDirenvInstalled()` 한 메서드로 최소화한다.
- `ProgressManager.getInstance().runProcessWithProgressSynchronously()`를 사용하여 프로세스 실행을 백그라운드 스레드로 이동한다.
- 기존 메서드 시그니처(`public static boolean isDirenvInstalled()`)를 유지하여 호출부 변경을 방지한다.
- 단일 커밋으로 완료한다.

## 배경

`DirenvRunConfigurationExtension.updateJavaParameters()`는 Run Configuration 실행 시 EDT에서 호출된다. 이 메서드 내부에서 `DirenvCommandExecutor.isDirenvInstalled()`가 `CapturingProcessHandler.runProcess()`를 직접 호출하여 `direnv version` 프로세스를 EDT에서 동기 대기한다. IntelliJ 2024+에서 `OSProcessHandler.checkEdtAndReadAction()`이 이를 감지하여 오류를 발생시킨다.

## Phase 1: isDirenvInstalled() EDT-safe 전환

> `CapturingProcessHandler.runProcess()` 직접 호출을 `ProgressManager.runProcessWithProgressSynchronously()` 래핑으로 교체

### 1-1. `isDirenvInstalled()` 메서드 수정

- 파일: `src/main/java/com/ddoong2/direnvloader/DirenvCommandExecutor.java`
- 변경:
  - `ProgressManager.getInstance().runProcessWithProgressSynchronously()` 내부에서 `CapturingProcessHandler.runProcess()` 호출
  - `Ref<Boolean>` 또는 `AtomicBoolean`으로 결과를 외부로 전달
  - 프로그레스 다이얼로그 타이틀: `"Checking direnv installation..."` (사용자에게 표시)
  - cancellable: `true` (사용자가 취소 가능)
- 효과: 프로세스 실행이 백그라운드 스레드에서 수행되어 EDT 블로킹 오류가 사라진다

**Before:**

```java
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
```

**After:**

```java
public static boolean isDirenvInstalled() {
    AtomicBoolean result = new AtomicBoolean(false);
    try {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                GeneralCommandLine cmd = new GeneralCommandLine("direnv", "version");
                cmd.setCharset(StandardCharsets.UTF_8);
                CapturingProcessHandler handler = new CapturingProcessHandler(cmd);
                ProcessOutput output = handler.runProcess(TIMEOUT_MS);
                result.set(output.getExitCode() == 0);
            } catch (Exception e) {
                result.set(false);
            }
        }, "Checking direnv installation...", true, null);
    } catch (Exception e) {
        return false;
    }
    return result.get();
}
```

### 1-2. 기존 테스트 통과 확인

- 파일: `src/test/java/com/ddoong2/direnvloader/DirenvCommandExecutorTest.java`
- 변경: 없음 (기존 테스트는 `parseExportJson()`만 테스트하므로 영향 없음)
- 검증: `./gradlew test` 전체 통과

## 검증 계획

| 검증 항목 | 방법 | 기대 결과 |
|----------|------|----------|
| EDT 오류 해소 | IDE에서 Run Configuration 실행 | `Synchronous execution on EDT` 오류 미발생 |
| direnv 미설치 | direnv 없는 환경에서 실행 | `false` 반환, 예외 없음 |
| 기존 테스트 | `./gradlew test` | 전체 통과 |
| 빌드 | `./gradlew build` | 성공 |
