---
type: clarify
subject: Multi-Project Workspace에서 direnv 작업 디렉토리 결정 오류 수정
date: 2026-11-05
ticket: _해당 없음_
branch: main
requester: daejoon
---

# Multi-Project Workspace에서 direnv 작업 디렉토리 결정 오류 수정 — Clarify

> Clarify · 요구사항 정의

## 📋 요약

| 항목 | 값 |
|------|----|
| 날짜 | 2026-11-05 |
| 티켓 | `_해당 없음_` |
| 브랜치 | `main` |
| 요청자 | daejoon |
| 성공 기준 | 9건 |
| 에지 케이스 | 10건 |
| 미해소/가정 | 0건 |
| 다음 단계 | code-writer (바로 구현) |

## 🎯 목표

Multi-Project Workspace 플러그인(`org.jetbrains.plugins.workspace`) 환경에서 하위 프로젝트 Run Configuration 실행 시 `.envrc file not found`로 실패하는 버그 수정. direnv 작업 디렉토리를 `project.basePath` 고정에서 Run Configuration working directory 우선으로 바꾸고, 그 값이 `basePath`와 같으면 RC 모듈의 content root를 사용하며, 최소 지원 버전을 251(2025.1)로 상향함

## 📦 범위

### ✅ 포함

- `DirenvRunConfigurationExtension.resolveWorkDir()`의 작업 디렉토리 결정 규칙 변경 (`params.workingDirectory` → `basePath`와 같으면 RC 모듈 content root → `project.basePath` → `user.dir`)
- `direnv allow`·`direnv export json` 두 명령 모두 새 작업 디렉토리 사용
- 작업 디렉토리 결정 로직 단위 테스트 추가
- `gradle.properties` 변경: `pluginVersion` 1.1.0, `sinceBuildVersion` 251, `platformVersion` 2025.1.7
- `build.gradle.kts`에 Multi-Project Workspace 플러그인을 설치한 검증용 runIde task 추가 (`intellijPlatformTesting.runIde`)
- `CHANGELOG.md` `## 1.1.0`에 영어 Fixed(작업 디렉토리)·Changed(최소 지원 2025.1) 항목 추가
- `plugin.xml` 설명의 작업 디렉토리 문구를 새 규칙으로 수정 (영어)

### ❌ 제외

- Direnv 설정 패널에 경로 입력란 추가 (Run Configuration XML 저장 형식 변경 없음)
- workspace 전용 API(`SubprojectInfoProvider`·`SubprojectManager`) 직접 사용
- `plugin.xml`에 workspace 플러그인 depends 추가 (runIde 검증 전용으로만 사용)
- `.envrc` 미존재 시 동작 변경
- 에러 알림 문구 변경
- `updateJavaParameters`를 거치지 않는 Run Configuration(Gradle 등) 지원 확대
- `untilBuildVersion` 변경
- `direnv export json` 출력의 null 값(`"DIRENV_DIFF": null` 등) 처리 — 별도 작업
- Java toolchain(17)·GitHub Actions workflow의 JDK 버전 변경

## 🔒 제약조건

| 구분 | 내용 |
|------|------|
| 기술 | Kotlin, Java 17 toolchain 유지, `sinceBuild` 251·`untilBuild` 263.*. 작업 디렉토리는 `JavaParameters.getWorkingDirectory()`와 RC 모듈의 `ModuleRootManager` content root만 사용하고 workspace 플러그인 API 미사용 |
| 비즈니스 | 단일 프로젝트 사용자 동작 유지 (working directory가 비었거나 프로젝트 루트면 결과 동일). 241~243 IDE 사용자는 Marketplace 호환 범위에 따라 1.0.6에 머묾 |
| 규칙 | `DirenvSettings.KEY` 의미·`plugin.xml` extension id/depends 변경 금지. working directory null 가능성 처리 필수. `CHANGELOG.md`는 영어 |

## ✅ 성공 기준

> **검증 시점**: 각 항목은 관찰 가능한 결과여야 함. 구현 완료 후 한 줄씩 체크.

- [ ] **AC-1** `learning-ws` workspace에서 Trust가 켜진 `BackendCoreApplication`(ice-core)·`FrontApiApp`(dcr) 실행 시 `.envrc file not found` 없이 각 하위 프로젝트 `.envrc` 변수가 프로세스 환경에 주입됨
- [ ] **AC-2** `params.workingDirectory`가 존재하는 디렉토리면 그 경로에서 `direnv allow`·`direnv export json` 실행됨
- [ ] **AC-3** `params.workingDirectory`가 null·빈 문자열·존재하지 않는 경로면 `project.basePath`에서 실행됨
- [ ] **AC-4** RC working directory가 `project.basePath`와 같고 RC 모듈에 존재하는 content root가 있으면 첫 번째 존재 content root에서 `direnv allow`·`direnv export json` 실행됨
- [ ] **AC-5** `project.basePath`도 null이면 `user.dir`에서 실행됨
- [ ] **AC-6** 단일 프로젝트(working directory 비었거나 프로젝트 루트) 실행 결과가 변경 전과 동일함
- [ ] **AC-7** 빌드된 플러그인의 `plugin.xml`에 `since-build="251"`, `until-build="263.*"`가 기록됨
- [ ] **AC-8** 검증용 runIde task 실행 시 Multi-Project Workspace 플러그인이 설치된 2025.1.7 개발 IDE가 기동됨
- [ ] **AC-9** Marketplace changeNotes(1.1.0)에 작업 디렉토리 수정과 최소 지원 버전 변경 내역이 영어로 표시됨

## ⚠️ 에지 케이스

| 상황 | 처리 방식 |
|------|----------|
| `params.workingDirectory`가 null 또는 공백 | `project.basePath`로 대체 |
| `params.workingDirectory`가 존재하지 않거나 디렉토리가 아닌 경로 | `project.basePath`로 대체 |
| `params.workingDirectory`가 상대 경로 | `project.basePath` 기준 절대 경로로 변환 후 존재 여부 판정 |
| workspace 하위 프로젝트 RC가 Gradle source set 모듈(`ice-core.main` 등)을 사용해 플랫폼이 working directory를 workspace 폴더로 채움 | working directory가 `basePath`와 같으므로 RC 모듈의 첫 번째 존재 content root(예: `ice-core/src/main`)에서 실행, direnv가 상위 `.envrc` 탐색 |
| working directory가 `basePath`와 같지만 RC 모듈이 없거나 존재하는 content root 없음 | `project.basePath` 사용 |
| working directory가 `basePath`와 다른 존재 디렉토리 | 모듈 content root와 무관하게 working directory 사용 |
| working directory와 프로젝트 루트 사이(하위 모듈)에 별도 `.envrc` 존재 | direnv 규칙대로 가장 가까운 상위 `.envrc` 적용 |
| 결정한 디렉토리와 그 상위에 `.envrc` 없음 (Trust 켜짐) | 기존 동작 유지 — `direnv allow` 실패로 에러 알림 + `ExecutionException`으로 실행 중단 |
| 결정한 디렉토리와 그 상위에 `.envrc` 없음 (Trust 꺼짐) | 기존 동작 유지 — `direnv export json` 성공(exit 0), 로드 결과 알림 후 실행 계속 |
| `project.basePath` null | 기존 동작 유지 — `user.dir` 사용 |

## 📝 합의된 결정 사항

> AskUserQuestion 라운드에서 사용자가 명시적으로 선택한 옵션을 기록. `(개발 판단)` 표기 항목은 사용자 선택이 아닌 코드 관례 기반 결정. 이후 spec/plan에서 같은 질문을 재차 묻지 않기 위함.

- **BackendCore `.envrc` 위치** → 각 하위 프로젝트 루트에 존재, workspace 폴더는 그 상위가 아닌 별도 위치
- **direnv 작업 디렉토리 결정 순서** → Run Configuration working directory(`params.workingDirectory`) 우선, 비었거나 없는 경로면 `project.basePath`
- **`.envrc` 미존재 시 동작** → 현재 동작 유지 (Trust 켜짐: 에러+중단, Trust 꺼짐: export 성공)
- **반영 범위** → 코드 수정 + `CHANGELOG.md` 항목 + `plugin.xml` 설명 문구 + `pluginVersion` 1.1.0 상향
- **workspace 플러그인 식별** → JetBrains Multi-Project Workspace(`org.jetbrains.plugins.workspace`), 테스트 workspace는 `../learning-ws`
- **`.envrc` 탐색 규칙** → direnv 규칙 그대로 — working directory에서 가장 가까운 상위 `.envrc` 적용, 없으면 기존 미존재 동작
- **AC-1 검증 방식** → 최소 지원 버전을 올려 runIde로 workspace 재현 (1차 답변 242, 플랫폼 확인 후 251로 재확정)
- **최소 지원 버전** → 251(2025.1), `platformVersion` 2025.1.7 — 빈 working directory를 하위 프로젝트 경로로 채우는 플랫폼 동작이 251부터 존재
- **runIde 구성** → 별도 runIde task에 workspace 플러그인 추가, 기존 runIde·컴파일 클래스패스·`plugin.xml` depends 변경 없음
- **`direnv export json` null 값 처리** → 이번 범위에서 제외, 별도 작업
- **범위 수정** → `sinceBuildVersion` 변경 제외·241 유지 → 251 상향 포함
- **CHANGELOG 기록 섹션 (개발 판단)** → `## 1.1.0` 섹션에 Fixed·Changed 항목 기록 — 근거: changeNotes가 `pluginVersion` 섹션을 우선 렌더링함(`build.gradle.kts:49-54`), 기존 릴리스 관례도 버전 섹션 사용
- **상대 경로 working directory 처리 (개발 판단)** → `project.basePath` 기준으로 해석 — 근거: IntelliJ Run Configuration working directory 해석 관례
- **작업 디렉토리 결정 로직 위치 (개발 판단)** → `companion object`의 `internal` 함수로 분리해 IDE 없이 단위 테스트 — 근거: 기존 `mergeEnvironment` 테스트 구조
- **runIde용 workspace 플러그인 버전 (개발 판단)** → 251.26927.53 — 근거: Marketplace에서 251.* 호환 최신 빌드
- **Java toolchain (개발 판단)** → 17 유지 — 근거: 2025.1.7 플랫폼 클래스 바이트코드 major 61(Java 17) 확인
- **AC-1 실패 원인 확인** → workspace 플러그인은 모듈 첫 content root가 하위 프로젝트 루트와 정확히 같을 때만 하위 프로젝트 경로를 제공, Gradle source set 모듈은 workspace 폴더로 대체됨(`SubprojectManager.getSubprojectByModule`)
- **모듈 content root 사용 조건** → RC working directory가 `project.basePath`와 같을 때(미설정 기본값·`$PROJECT_DIR$` 포함) RC 모듈의 첫 번째 존재 content root 사용
- **범위 수정** → 모듈 content root 기반 탐색 제외 → 포함
- **최소 지원 버전 재확인** → 251 유지
- **working directory 무효 시 모듈 적용 (개발 판단)** → 무효한 working directory는 `basePath`로 대체되므로 같은 규칙으로 모듈 content root 적용 — 근거: 사용자 결정 '`basePath`와 같으면 모듈 기준'의 대체 결과 동일
- **content root 조회 방식 (개발 판단)** → `ModuleBasedConfiguration.configurationModule.module`의 `ModuleRootManager.contentRoots`를 ReadAction 안에서 조회 — 근거: 플랫폼 공통 API, workspace 플러그인 비의존

## ❓ 미해소 항목 / 가정

> 질문으로 확정하지 못해 잠정 가정으로 진행하는 항목. 후속 단계(spec/plan/task/code-writer) 착수 전 검증 필요.

| 항목 | 상태 / 검증 |
|------|------------|
| _해당 없음_ | _해당 없음_ |

## 🔗 다음 단계

코드 1개 함수 변경 + 빌드 설정·문서·버전 반영이라 경로 A(바로 구현) 권장

<!--
원칙:
- 5개 영역(목표·범위·제약·성공기준·에지) 구조 고정. 비어 있는 항목은 _해당 없음_ 표기.
- 성공 기준 체크박스는 `[ ]` 정적 상태. 실제 진행도 체크는 task 문서에서.
- 결정 사항 섹션은 AskUserQuestion 답변을 누적해 spec/plan/task로 컨텍스트 이어주기 위한 기록.
- 미해소 항목/가정 섹션은 확답 불가해 `가정`으로 진행한 항목을 가시화해 후속 단계에서 검증받기 위한 기록. 비면 _해당 없음_.
- frontmatter는 docs-writer가 메타데이터 추출에 사용.
-->
