# EDT Synchronous Execution 오류 수정 — Task

> 관련 문서: [← 구현 Plan](./2026-03-28-fix-edt-synchronous-execution-plan.md)
> 날짜: 2026-03-28
> 브랜치: `main`

## 실행 가이드

```
[A-1] isDirenvInstalled() 수정
         │
[S-1] 테스트 실행 & 빌드 검증
         │
[S-2] 커밋
         │
[V-1] 최종 검증
```

---

## Lane A: isDirenvInstalled() EDT-safe 전환

> 경로 접두사: `src/main/java/com/ddoong2/direnvloader/`

- [x] **A-1** `isDirenvInstalled()` 메서드를 `ProgressManager.runProcessWithProgressSynchronously()`로 래핑
  - 파일: `DirenvCommandExecutor.java` (62~72행)
  - 변경:
    - `import com.intellij.openapi.progress.ProgressManager;` 추가
    - `import java.util.concurrent.atomic.AtomicBoolean;` 추가
    - `CapturingProcessHandler.runProcess()` 호출을 `ProgressManager` 블록 내부로 이동
    - `AtomicBoolean`으로 결과 전달
    - 프로그레스 타이틀: `"Checking direnv installation..."`
    - cancellable: `true`, project: `null`
  - 검증: 컴파일 성공

---

## 동기화: 커밋

> 선행 조건: A-1 완료

- [x] **S-1** 검증: `./gradlew test` 전체 통과
- [x] **S-2** 검증: `./gradlew build` 성공
- [x] **S-3** 커밋: `isDirenvInstalled()의 EDT 동기 실행 오류 수정`

---

## 최종 검증

- [x] **V-1** `./gradlew test` 전체 통과
- [x] **V-2** `./gradlew build` 성공
