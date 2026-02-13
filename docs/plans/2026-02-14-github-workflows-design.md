# GitHub Actions Workflows 설계

## 개요

DirenvLoader IntelliJ 플러그인의 CI/CD를 위한 GitHub Actions 워크플로우 3개를 추가한다.
JetBrains 공식 [intellij-platform-plugin-template](https://github.com/JetBrains/intellij-platform-plugin-template)의 검증된 패턴을 기반으로 커스터마이징한다.

## 접근 방식

JetBrains 공식 템플릿 기반. 프로젝트 특성에 맞게 다음을 조정:

- Java 21 → **17** (프로젝트 설정)
- Qodana 코드 검사 **제외** (사용자 결정)
- Codecov 코드 커버리지 **제외** (미설정)
- 나머지 패턴은 공식 템플릿 준수

## 워크플로우 상세

### 1. Build (`build.yml`)

**트리거:** `push` (main 브랜치) + `pull_request`
**동시성 제어:** 같은 PR/브랜치에 대해 이전 실행 자동 취소

**Jobs:**

| Job | 의존성 | 내용 |
|-----|--------|------|
| `build` | - | Gradle Wrapper 검증, `buildPlugin`, 아티팩트 업로드 |
| `test` | build | `check` 실행, 실패 시 테스트 결과 업로드 |
| `verify` | build | `verifyPlugin`, Plugin Verifier 결과 업로드 |
| `releaseDraft` | build, test, verify | main push 시만 draft release 생성 (CHANGELOG 기반) |

### 2. Release (`release.yml`)

**트리거:** `release` 이벤트 (`prereleased`, `released`)

**Steps:**
1. CHANGELOG.md 패치 (`patchChangelog`)
2. JetBrains Marketplace 배포 (`publishPlugin`)
3. GitHub Release에 빌드 결과물 첨부
4. Changelog 업데이트 PR 자동 생성

**필요 Secrets:** `PUBLISH_TOKEN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`, `CERTIFICATE_CHAIN`

### 3. Run UI Tests (`run-ui-tests.yml`)

**트리거:** `workflow_dispatch` (수동)

**Matrix:** Linux, Windows, macOS 3개 OS 병렬 실행
**Steps:** IDE 실행 → Health Check → UI 테스트 실행

## 환경 설정

| 항목 | 값 |
|------|-----|
| Runner | `ubuntu-latest` (build/release), matrix (UI tests) |
| Java | Zulu 17 |
| Gradle | `gradle/actions/setup-gradle@v5` (캐시 포함) |
| Checkout | `actions/checkout@v6` |
| Artifact | `actions/upload-artifact@v6` |

## 파일 구조

```
.github/
└── workflows/
    ├── build.yml
    ├── release.yml
    └── run-ui-tests.yml
```
