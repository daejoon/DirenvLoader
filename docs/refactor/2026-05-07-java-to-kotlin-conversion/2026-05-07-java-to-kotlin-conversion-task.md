# Java → Kotlin 전면 변환 — Task

> 날짜: 2026-05-07
> 브랜치: `main` (별도 작업 브랜치 신설 권장)
> 카테고리: refactor

## 개요

- DirenvLoader 플러그인의 Java 소스 10개(`main` 7 + `test` 3, 총 603줄)를 Kotlin으로 전면 변환
- `idiomatic Kotlin` 적극 적용, 패키지·클래스명 완전 유지로 `plugin.xml` 호환성 보존
- 단일 PR/커밋 단위로 일괄 변환

## 실행 가이드

### 전체 흐름

```
A. 빌드 설정 ──→ B. 디렉토리 준비 ──→ C. main 변환 ──┐
                                                  ├──→ E. Java 제거 ──→ S. 검증 ──→ V. 최종
                                  ──→ D. test 변환 ──┘
```

- Lane C와 Lane D는 빌드 설정이 끝난 뒤 병렬 진행 가능
- Lane E(기존 Java 제거)는 C·D 양쪽 완료 후 시작
- 모든 변경 후 단일 커밋(`S` 단계)으로 묶음

### 핵심 원칙

- Idiomatic Kotlin 적극 적용 (`data class`, `val/var`, null safety, `when`, 확장 함수)
- 정적 키(`DirenvSettings.KEY`)는 `@JvmField` 또는 `const val`로 호환성 유지
- `@Throws` 어노테이션으로 체크 예외 호환성 명시
- IntelliJ EDT/ReadAction 호출 패턴 보존(최근 EDT 수정 커밋 패턴 유지)
- `plugin.xml` 참조 fully-qualified name `com.ddoong2.direnvloader.*` 1:1 보존

---

## Lane A: 빌드 설정

> 경로 접두사: `build.gradle.kts`, `gradle/libs.versions.toml`
> 공통 설명: Kotlin JVM 플러그인 도입 및 컴파일 타깃 정렬

- [x] **A-1** Kotlin Gradle 플러그인 추가
  - 파일: `gradle/libs.versions.toml`, `build.gradle.kts`
  - 변경: `kotlin = "1.9.25"` 버전 추가 + `kotlin-jvm` 플러그인 alias 정의 후 `plugins` 블록에 적용
  - 검증: 후속 Lane S-2 빌드 단계에서 일괄 검증

- [x] **A-2** Kotlin 컴파일 옵션 설정
  - 파일: `build.gradle.kts`
  - 변경: `kotlin { jvmToolchain(17) }` + `KotlinCompile { kotlinOptions { jvmTarget = "17" } }` 추가 (`javaVersion` 프로퍼티 재사용)
  - 검증: 후속 Lane S-2 빌드 단계에서 일괄 검증

- [x] **A-3** kotlin-stdlib 의존성 처리
  - 파일: `gradle.properties`
  - 변경: `kotlin.stdlib.default.dependency=false` 추가 — IntelliJ Platform 번들 stdlib 사용으로 충돌 회피
  - 검증: 후속 Lane S-2 빌드 단계에서 일괄 검증

---

## Lane B: 디렉토리 준비

> 경로 접두사: `src/main`, `src/test`
> 공통 설명: Kotlin 표준 디렉토리 레이아웃 신설

- [x] **B-1** main Kotlin 디렉토리 생성
  - 파일: `src/main/kotlin/com/ddoong2/direnvloader/`
  - 변경: 빈 디렉토리 생성 (Kotlin 소스 배치 대상)
  - 검증: `ls src/main/kotlin/com/ddoong2/direnvloader` 성공

- [x] **B-2** test Kotlin 디렉토리 생성
  - 파일: `src/test/kotlin/com/ddoong2/direnvloader/`
  - 변경: 빈 디렉토리 생성
  - 검증: `ls src/test/kotlin/com/ddoong2/direnvloader` 성공

---

## Lane C: main 소스 변환

> 경로 접두사: `src/main/kotlin/com/ddoong2/direnvloader/`
> 공통 설명: 의존성 낮은 순서대로 변환하여 컴파일 단계 격리

- [x] **C-1** `DirenvException.kt` 작성
  - 파일: `src/main/kotlin/com/ddoong2/direnvloader/DirenvException.kt`
  - 변경: `class DirenvException(message: String, cause: Throwable? = null) : Exception(message, cause)` 형태로 변환
  - 검증: 컴파일 성공
  - 주의: 기존 클래스 분리 구조 유지 (sealed class 통합 금지)

- [x] **C-2** `DirenvBlockedException.kt` 작성
  - 파일: `src/main/kotlin/com/ddoong2/direnvloader/DirenvBlockedException.kt`
  - 변경: `DirenvException` 상속 구조 그대로 변환
  - 검증: 컴파일 성공

- [x] **C-3** `DirenvSettings.kt` 작성
  - 파일: `src/main/kotlin/com/ddoong2/direnvloader/DirenvSettings.kt`
  - 변경: `data class DirenvSettings`로 변환, `companion object` 내부 `KEY`를 `@JvmField val KEY: Key<DirenvSettings>`로 노출
  - 검증: `DirenvSettings.KEY` 정적 접근 호환성 유지 확인 (Java 측 호출 시 `Class.KEY`)
  - 주의: CopyableUserData 직렬화 의미 보존, `KEY` 인스턴스 동일성 유지

- [x] **C-4** `DirenvNotifier.kt` 작성
  - 파일: `src/main/kotlin/com/ddoong2/direnvloader/DirenvNotifier.kt`
  - 변경: 알림 표시 함수를 `object` 또는 top-level 함수로 정리, `when` 표현식으로 알림 타입 분기
  - 검증: 컴파일 성공

- [x] **C-5** `DirenvCommandExecutor.kt` 작성
  - 파일: `src/main/kotlin/com/ddoong2/direnvloader/DirenvCommandExecutor.kt`
  - 변경: `GeneralCommandLine` 호출부 null safety 적용, working directory 분기 의미 보존, 체크 예외 던지는 함수에 `@Throws(DirenvException::class)` 부여
  - 검증: 컴파일 성공, 기존 EDT/Read-Lock 호출 패턴(최근 커밋 `db95be8`/`db7a97e`/`9d39a03`) 동일 유지
  - 주의: blocked·실행 불가·빈 출력 분기 누락 금지

- [x] **C-6** `DirenvSettingsEditor.kt` 작성
  - 파일: `src/main/kotlin/com/ddoong2/direnvloader/DirenvSettingsEditor.kt`
  - 변경: Swing UI 컴포넌트 초기화부를 Kotlin 프로퍼티 + `apply` 블록으로 정리
  - 검증: 컴파일 성공

- [x] **C-7** `DirenvRunConfigurationExtension.kt` 작성
  - 파일: `src/main/kotlin/com/ddoong2/direnvloader/DirenvRunConfigurationExtension.kt`
  - 변경: Run Configuration Extension override 메서드 변환, null safety 강화
  - 검증: 컴파일 성공, `plugin.xml`의 extension 등록 클래스명과 일치 확인

---

## Lane D: test 소스 변환

> 경로 접두사: `src/test/kotlin/com/ddoong2/direnvloader/`
> 공통 설명: JUnit 5 + Mockito 사용 패턴 그대로 유지

- [x] **D-1** `DirenvCommandExecutorTest.kt` 작성
  - 파일: `src/test/kotlin/com/ddoong2/direnvloader/DirenvCommandExecutorTest.kt`
  - 변경: `@Test` 메서드 시그니처 Kotlin 변환, `assertThrows` 람다 형태 적용
  - 검증: 단일 테스트 클래스 단독 실행 통과 (`./gradlew test --tests DirenvCommandExecutorTest`)

- [x] **D-2** `DirenvNotifierTest.kt` 작성
  - 파일: `src/test/kotlin/com/ddoong2/direnvloader/DirenvNotifierTest.kt`
  - 변경: Mockito 호출부 Kotlin 문법으로 변환 (`any()` 제네릭 추론 주의)
  - 검증: 단일 테스트 클래스 단독 실행 통과

- [x] **D-3** `DirenvRunConfigurationExtensionTest.kt` 작성
  - 파일: `src/test/kotlin/com/ddoong2/direnvloader/DirenvRunConfigurationExtensionTest.kt`
  - 변경: IntelliJ Platform 테스트 프레임워크 호출부 Kotlin 변환
  - 검증: 단일 테스트 클래스 단독 실행 통과

---

## Lane E: 기존 Java 정리

> 경로 접두사: `src/main/java`, `src/test/java`
> 공통 설명: Lane C·D 완료 후 진행. 디렉토리 자체 제거

- [x] **E-1** `src/main/java` 하위 Java 파일 7개 삭제
  - 파일: `src/main/java/com/ddoong2/direnvloader/*.java`
  - 변경: 7개 `.java` 파일 `git rm`으로 일괄 제거
  - 검증: `git status`에서 7건 deleted 확인

- [x] **E-2** `src/test/java` 하위 Java 파일 3개 삭제
  - 파일: `src/test/java/com/ddoong2/direnvloader/*.java`
  - 변경: 3개 `.java` 파일 `git rm`으로 일괄 제거
  - 검증: `git status`에서 3건 deleted 확인

- [x] **E-3** 빈 `src/main/java`, `src/test/java` 디렉토리 제거
  - 파일: `src/main/java`, `src/test/java`
  - 변경: macOS 환경에서 git rm이 빈 디렉토리 자동 정리. 별도 `rmdir` 불필요
  - 검증: `ls src/main/`, `ls src/test/` 결과 java 디렉토리 부재

---

## 동기화: 검증 및 커밋

> 선행 조건: Lane A·B·C·D·E 모든 항목 완료

- [x] **S-1** 전체 테스트 실행
  - 명령: `./gradlew test`
  - 검증: BUILD SUCCESSFUL (48s), 테스트 실패 0건

- [x] **S-2** 전체 빌드 실행
  - 명령: `./gradlew build`
  - 검증: BUILD SUCCESSFUL (12s), `verifyPlugin` 포함 정상 통과

- [ ] **S-3** 단일 커밋 생성
  - 명령: `commit` 스킬 사용
  - 메시지 예시: `Java 코드를 Kotlin으로 전면 변환`
  - 주의: 빌드 설정·소스·디렉토리 정리를 단일 논리 변경으로 묶음

---

## 최종 검증

- [x] **V-1** Java 파일 0건 확인
  - 명령: `find src -name "*.java" | wc -l` 결과 `0`
  - 검증: 잔여 Java 파일 없음

- [x] **V-2** Kotlin 파일 10건 확인
  - 명령: `find src -name "*.kt" | wc -l` 결과 `10`
  - 검증: main 7개 + test 3개 모두 존재

- [x] **V-3** 패키지·클래스명 1:1 보존 확인
  - 명령: `grep com.ddoong2.direnvloader src/main/resources/META-INF/*.xml`
  - 검증: `<id>com.ddoong2.direnvloader</id>` + `DirenvRunConfigurationExtension` FQN 정상 노출

- [ ] **V-4** runIde 수동 동작 확인 (선택)
  - 명령: `./gradlew runIde`
  - 검증: 개발 IDE 부팅, Run Configuration UI에서 direnv 설정 노출 확인
  - 비고: 성공 기준에 포함되지 않은 선택 검증 단계
