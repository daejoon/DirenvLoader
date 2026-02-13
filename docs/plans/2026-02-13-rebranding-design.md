# DirenvLoader Plugin Rebranding Design

## Overview

DirenvLoader 플러그인의 패키지명 변경, 알림 메시지 영문화, 커스텀 아이콘 적용을 수행한다.

## 1. 패키지명 변경

- **변경**: `com.github.direnvloader` → `com.ddoong2.direnvloader`
- **영향 파일**:
  - 소스 7개: `DirenvException`, `DirenvBlockedException`, `DirenvCommandExecutor`, `DirenvNotifier`, `DirenvRunConfigurationExtension`, `DirenvSettings`, `DirenvSettingsEditor`
  - 테스트 3개: `DirenvCommandExecutorTest`, `DirenvNotifierTest`, `DirenvRunConfigurationExtensionTest`
  - `plugin.xml`: `<id>`, `<implementation>` 속성
  - `direnv-spring-boot.xml`: 패키지 참조 (현재 비어 있으나 향후 대비)
  - `gradle.properties`: `pluginGroup` 값
- **디렉토리 구조**: `com/github/direnvloader/` → `com/ddoong2/direnvloader/`
- **접근법**: 전면 일괄 변경 (원자적 변경 필수)

## 2. DirenvNotifier 영문화

| 메서드 | 현재 (한글) | 변경 (영어) |
|--------|------------|------------|
| `createNotInstalledNotification` | direnv가 설치되어 있지 않습니다. direnv를 설치한 후 다시 시도하세요. | direnv is not installed. Please install direnv and try again. |
| `createBlockedNotification` | .envrc가 허용되지 않았습니다. 'Trust .envrc' 옵션을 활성화하거나 터미널에서 'direnv allow'를 실행하세요. | .envrc is not allowed. Enable 'Trust .envrc' or run 'direnv allow' in terminal. |
| `createErrorNotification` | direnv 실행 중 오류가 발생했습니다: | Error occurred while running direnv: |
| `createLoadedNotification` | direnv 환경변수 N개를 로드했습니다. | Loaded N direnv environment variable(s). |

## 3. 플러그인 아이콘

- direnv 공식 로고를 참고하여 SVG 아이콘 제작
- 크기: 16x16 (tool window), 40x40 (plugin listing)
- 배치: `src/main/resources/icons/`
- `plugin.xml`에 `<icon>` 설정 추가
- IntelliJ Platform 가이드라인에 따라 light/dark 테마 대응
