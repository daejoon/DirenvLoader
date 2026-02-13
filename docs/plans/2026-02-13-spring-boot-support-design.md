# Spring Boot Run Configuration 지원 설계

## 목적

IntelliJ Ultimate의 Spring Boot Run Configuration에서도 Direnv 환경변수 자동 주입이 동작하도록 빌드/설정을 수정한다.

## 현황

- `DirenvRunConfigurationExtension.isApplicableFor()`가 모든 RC 타입에 true 반환
- `patchCommandLine()`이 제너릭 `GeneralCommandLine` API를 사용
- 코드 수준에서는 이미 Spring Boot RC를 지원하나, `platformType = IC`로 빌드되어 검증 불가

## 접근 방식

IC 기반 빌드를 IU로 전환하되, `plugin.xml`의 `<depends>` 선언으로 IC 호환성을 유지한다.
Spring Boot 플러그인은 optional 의존성으로 선언한다.

## 변경 사항

### 1. gradle.properties

- `platformType`을 `IC`에서 `IU`로 변경

### 2. build.gradle.kts

- `bundledPlugin("com.intellij.spring.boot")` 추가

### 3. plugin.xml

- `<depends optional="true" config-file="direnv-spring-boot.xml">com.intellij.spring.boot</depends>` 추가

### 4. direnv-spring-boot.xml (신규)

- `src/main/resources/META-INF/direnv-spring-boot.xml` 생성
- Spring Boot 전용 Extension 등록 불필요 (기존 Extension이 모든 RC에 적용)

### 5. 코드 변경

- 없음

## IC 호환성

- `plugin.xml`의 `<depends>com.intellij.modules.platform</depends>`가 IC 호환성을 결정
- IU로 빌드해도 IU 전용 API를 사용하지 않으면 IC에서 정상 동작
- Spring Boot는 optional 의존이므로 없어도 플러그인 정상 로드

## 테스트

- 기존 단위 테스트 통과 확인
- `./gradlew runIde`로 Ultimate 실행 후 Spring Boot RC에서 Direnv 탭 표시 확인 (수동)
