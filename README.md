# Direnv Loader

<!-- 🔧 Marketplace 퍼블리시 후 PLUGIN_ID를 실제 숫자 ID로 교체하세요 -->
<!-- 🔧 GitHub Actions 워크플로우 추가 후 Build 배지의 워크플로우 이름을 맞춰주세요 -->
![Build](https://github.com/daejoon/DirenvLoader/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/30187-direnv-loader.svg)](https://plugins.jetbrains.com/plugin/30187-direnv-loader)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/30187-direnv-loader.svg)](https://plugins.jetbrains.com/plugin/30187-direnv-loader)

[direnv](https://direnv.net/) 환경변수를 IntelliJ 계열 IDE의 Run Configuration에 자동으로 주입하는 플러그인입니다.
[better-direnv](https://github.com/Fapiko/intellij-better-direnv)가 더 이상 유지보수되지 않아 대체 플러그인으로 개발되었습니다.

## Features

- **자동 환경변수 주입** — Run Configuration 실행 시 `direnv export json`을 호출하여 환경변수를 프로세스에 자동 병합합니다.
- **Run Configuration별 개별 제어** — 각 Run Configuration마다 direnv 사용 여부를 체크박스로 독립 설정할 수 있습니다.
- **Trust 관리** — "Trust .envrc" 옵션으로 blocked 상태의 `.envrc`를 자동 허용하여 수동 `direnv allow` 실행이 필요 없습니다.
- **다양한 Run Configuration 지원** — Java Application, JUnit, Gradle Task, Spring Boot 등 대부분의 Run Configuration 타입에서 동작합니다.
- **알림 제공** — 환경변수 로드 성공(변수 수), 경고(direnv 미설치, `.envrc` blocked), 오류(명령 실패, 타임아웃)를 Balloon 알림으로 표시합니다.

## How It Works

1. Run Configuration을 열고 Direnv 설정 패널에서 **"Enable Direnv"** 를 체크합니다.
2. 필요시 **"Trust .envrc"** 를 체크하여 `.envrc` 파일을 자동 허용합니다.
3. Run Configuration을 실행하면 direnv 환경변수가 자동으로 로드됩니다.

## Requirements

- [direnv](https://direnv.net/)가 시스템 PATH에 설치되어 있어야 합니다.
- 프로젝트 디렉토리에 유효한 `.envrc` 파일이 있어야 합니다.
- IntelliJ IDEA 2024.1 이상 (빌드 241+)

## Tech Stack

- Java 17
- Gradle Kotlin DSL
- IntelliJ Platform Plugin SDK
- JUnit 5

## Build

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 개발 IDE에서 플러그인 실행
./gradlew runIde

# 배포 아카이브 생성
./gradlew buildPlugin
```

## License

이 프로젝트는 [MIT License](LICENSE)로 배포됩니다.

