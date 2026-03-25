# AGENTS

## Project Context & Operations

### Project Summary
- 목적: IntelliJ 계열 IDE Run Configuration에 `direnv` 환경변수 주입을 제공하는 플러그인을 유지보수한다.
- 구조: Gradle 단일 모듈 아키텍처를 사용한다.
- 주요 기술 스택: Java 17, Gradle Kotlin DSL, IntelliJ Platform Plugin SDK (`org.jetbrains.intellij.platform`), JUnit 5.

### Operational Commands
- 전체 빌드: `./gradlew build`
- 전체 테스트: `./gradlew test`
- 플러그인 실행(개발 IDE): `./gradlew runIde`
- 플러그인 검증: `./gradlew verifyPlugin`
- Plugin Verifier 실행: `./gradlew runPluginVerifier`
- 배포 아카이브 생성: `./gradlew buildPlugin`

## Golden Rules

### Immutable
- 시크릿(`PUBLISH_TOKEN`, 개인 키, 인증서)은 코드/문서에 하드코딩하지 않는다.
- `plugin.xml` 및 `META-INF/direnv-*.xml`의 extension id/depends 관계를 임의 변경하지 않는다.
- `DirenvSettings.KEY`(CopyableUserData 키)의 의미를 깨는 변경을 금지한다.

### Do's
- 새 optional dependency 추가 시 `plugin.xml`의 depends 선언 + `src/main/resources/META-INF/direnv-*.xml` 매핑을 같이 수정한다.
- `direnv` 실패 경로(실행 불가, blocked, 빈 출력)를 예외 없이 처리한다.
- 변경 시 최소 1개 이상 관련 테스트(또는 테스트 불가 사유)를 명시한다.
- `CHANGELOG.md`는 영어로 작성한다. (JetBrains Marketplace 배포 시 `changeNotes`로 렌더링되므로)

### Don'ts
- `GeneralCommandLine`/IDE API 호출에서 working directory null 가능성을 무시하지 않는다.
- 불필요한 신규 라이브러리를 추가하지 않는다. 필요 시 사유와 영향 범위를 남긴다.
- 코드 스타일 정리만을 목적으로 대규모 포맷 변경을 하지 않는다.