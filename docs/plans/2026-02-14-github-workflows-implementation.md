# GitHub Actions Workflows 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** DirenvLoader 플러그인의 CI/CD를 위한 GitHub Actions 워크플로우 3개(build, release, run-ui-tests)를 추가한다.

**Architecture:** JetBrains 공식 intellij-platform-plugin-template의 워크플로우 패턴을 기반으로, 프로젝트 설정(Java 17, Qodana 제외)에 맞게 커스터마이징한다. build → release 순서로 draft release 생성 후 publish하는 파이프라인을 구성한다.

**Tech Stack:** GitHub Actions, Gradle, IntelliJ Platform Plugin SDK, org.jetbrains.changelog

---

### Task 1: Build 워크플로우 생성

**Files:**
- Create: `.github/workflows/build.yml`

**참고 문서:**
- `docs/plans/2026-02-14-github-workflows-design.md` - 설계 문서
- `build.gradle.kts` - Gradle 빌드 설정 (changelog, signing, publishing, pluginVerification 구성 확인)
- `gradle.properties` - 플러그인 버전, Java 버전(17), 플랫폼 설정

**Step 1: .github/workflows 디렉토리 생성**

Run: `mkdir -p .github/workflows`

**Step 2: build.yml 파일 작성**

```yaml
# GitHub Actions Workflow is created for testing and preparing the plugin release in the following steps:
# - Validate Gradle Wrapper.
# - Run 'test' and 'verifyPlugin' tasks.
# - Run the 'buildPlugin' task and prepare artifact for further tests.
# - Run the 'runPluginVerifier' task.
# - Create a draft release.
#
# The workflow is triggered on push and pull_request events.
#
# GitHub Actions reference: https://help.github.com/en/actions

name: Build
on:
  push:
    branches: [ main ]
  pull_request:

concurrency:
  group: ${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}
  cancel-in-progress: true

jobs:

  build:
    name: Build
    runs-on: ubuntu-latest
    steps:
      - name: Maximize Build Space
        uses: jlumbroso/free-disk-space@v1.3.1
        with:
          tool-cache: false
          large-packages: false

      - name: Fetch Sources
        uses: actions/checkout@v6

      - name: Setup Java
        uses: actions/setup-java@v5
        with:
          distribution: zulu
          java-version: 17

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v5

      - name: Build Plugin
        run: ./gradlew buildPlugin

      - name: Prepare Plugin Artifact
        id: artifact
        shell: bash
        run: |
          cd ${{ github.workspace }}/build/distributions
          FILENAME=`ls *.zip`
          unzip "$FILENAME" -d content
          echo "filename=${FILENAME:0:-4}" >> $GITHUB_OUTPUT

      - name: Upload artifact
        uses: actions/upload-artifact@v6
        with:
          name: ${{ steps.artifact.outputs.filename }}
          path: ./build/distributions/content/*/*

  test:
    name: Test
    needs: [ build ]
    runs-on: ubuntu-latest
    steps:
      - name: Maximize Build Space
        uses: jlumbroso/free-disk-space@v1.3.1
        with:
          tool-cache: false
          large-packages: false

      - name: Fetch Sources
        uses: actions/checkout@v6

      - name: Setup Java
        uses: actions/setup-java@v5
        with:
          distribution: zulu
          java-version: 17

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v5
        with:
          cache-read-only: true

      - name: Run Tests
        run: ./gradlew check

      - name: Collect Tests Result
        if: ${{ failure() }}
        uses: actions/upload-artifact@v6
        with:
          name: tests-result
          path: ${{ github.workspace }}/build/reports/tests

  verify:
    name: Verify plugin
    needs: [ build ]
    runs-on: ubuntu-latest
    steps:
      - name: Maximize Build Space
        uses: jlumbroso/free-disk-space@v1.3.1
        with:
          tool-cache: false
          large-packages: false

      - name: Fetch Sources
        uses: actions/checkout@v6

      - name: Setup Java
        uses: actions/setup-java@v5
        with:
          distribution: zulu
          java-version: 17

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v5
        with:
          cache-read-only: true

      - name: Run Plugin Verification tasks
        run: ./gradlew verifyPlugin

      - name: Collect Plugin Verifier Result
        if: ${{ always() }}
        uses: actions/upload-artifact@v6
        with:
          name: pluginVerifier-result
          path: ${{ github.workspace }}/build/reports/pluginVerifier

  releaseDraft:
    name: Release draft
    if: github.event_name != 'pull_request'
    needs: [ build, test, verify ]
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - name: Fetch Sources
        uses: actions/checkout@v6

      - name: Setup Java
        uses: actions/setup-java@v5
        with:
          distribution: zulu
          java-version: 17

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v5
        with:
          cache-read-only: true

      - name: Remove Old Release Drafts
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          gh api repos/{owner}/{repo}/releases \
            --jq '.[] | select(.draft == true) | .id' \
            | xargs -I '{}' gh api -X DELETE repos/{owner}/{repo}/releases/{}

      - name: Create Release Draft
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          VERSION=$(./gradlew properties --property version --quiet --console=plain | tail -n 1 | cut -f2- -d ' ')
          RELEASE_NOTE="./build/tmp/release_note.txt"
          ./gradlew getChangelog --unreleased --no-header --quiet --console=plain --output-file=$RELEASE_NOTE
          gh release create $VERSION \
            --draft \
            --title $VERSION \
            --notes-file $RELEASE_NOTE
```

**Step 3: YAML 문법 검증**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/build.yml'))"`
Expected: 에러 없이 정상 종료

**Step 4: 커밋**

```bash
git add .github/workflows/build.yml
git commit -m "ci: Build 워크플로우 추가 (test, verify, draft release)"
```

---

### Task 2: Release 워크플로우 생성

**Files:**
- Create: `.github/workflows/release.yml`

**참고 문서:**
- `build.gradle.kts:62-78` - signing, publishing 설정 (환경변수: CERTIFICATE_CHAIN, PRIVATE_KEY, PRIVATE_KEY_PASSWORD, PUBLISH_TOKEN)
- `build.gradle.kts:44-50` - changelog 연동 (`getChangelog`, `patchChangelog` task 사용)

**Step 1: release.yml 파일 작성**

```yaml
# GitHub Actions Workflow created for handling the release process based on the draft release prepared
# with the Build workflow.
#
# Running the publishPlugin task requires all the following secrets to be provided:
# PUBLISH_TOKEN, PRIVATE_KEY, PRIVATE_KEY_PASSWORD, CERTIFICATE_CHAIN.
#
# See https://plugins.jetbrains.com/docs/intellij/plugin-signing.html for more information.

name: Release
on:
  release:
    types: [ prereleased, released ]

jobs:

  release:
    name: Publish Plugin
    runs-on: ubuntu-latest
    permissions:
      contents: write
      pull-requests: write
    steps:
      - name: Maximize Build Space
        uses: jlumbroso/free-disk-space@v1.3.1
        with:
          tool-cache: false
          large-packages: false

      - name: Fetch Sources
        uses: actions/checkout@v6
        with:
          ref: ${{ github.event.release.tag_name }}

      - name: Setup Java
        uses: actions/setup-java@v5
        with:
          distribution: zulu
          java-version: 17

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v5
        with:
          cache-read-only: true

      - name: Patch Changelog
        if: ${{ github.event.release.body != '' }}
        env:
          CHANGELOG: ${{ github.event.release.body }}
        run: |
          RELEASE_NOTE="./build/tmp/release_note.txt"
          mkdir -p "$(dirname "$RELEASE_NOTE")"
          echo "$CHANGELOG" > $RELEASE_NOTE
          ./gradlew patchChangelog --release-note-file=$RELEASE_NOTE

      - name: Publish Plugin
        env:
          PUBLISH_TOKEN: ${{ secrets.PUBLISH_TOKEN }}
          CERTIFICATE_CHAIN: ${{ secrets.CERTIFICATE_CHAIN }}
          PRIVATE_KEY: ${{ secrets.PRIVATE_KEY }}
          PRIVATE_KEY_PASSWORD: ${{ secrets.PRIVATE_KEY_PASSWORD }}
        run: ./gradlew publishPlugin

      - name: Upload Release Asset
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: gh release upload ${{ github.event.release.tag_name }} ./build/distributions/*

      - name: Create Pull Request
        if: ${{ github.event.release.body != '' }}
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          VERSION="${{ github.event.release.tag_name }}"
          BRANCH="changelog-update-$VERSION"
          LABEL="release changelog"

          git config user.email "action@github.com"
          git config user.name "GitHub Action"

          git checkout -b $BRANCH
          git commit -am "Changelog update - $VERSION"
          git push --set-upstream origin $BRANCH

          gh label create "$LABEL" \
            --description "Pull requests with release changelog update" \
            --force \
            || true

          gh pr create \
            --title "Changelog update - \`$VERSION\`" \
            --body "Current pull request contains patched \`CHANGELOG.md\` file for the \`$VERSION\` version." \
            --label "$LABEL" \
            --head $BRANCH
```

**Step 2: YAML 문법 검증**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/release.yml'))"`
Expected: 에러 없이 정상 종료

**Step 3: 커밋**

```bash
git add .github/workflows/release.yml
git commit -m "ci: Release 워크플로우 추가 (publish, changelog PR)"
```

---

### Task 3: Run UI Tests 워크플로우 생성

**Files:**
- Create: `.github/workflows/run-ui-tests.yml`

**참고 문서:**
- https://github.com/JetBrains/intellij-ui-test-robot - UI 테스트 로봇 라이브러리

**Step 1: run-ui-tests.yml 파일 작성**

```yaml
# GitHub Actions Workflow for launching UI tests on Linux, Windows, and Mac in the following steps:
# - Prepare and launch IDE with your plugin and robot-server plugin, which is needed to interact with the UI.
# - Wait for IDE to start.
# - Run UI tests with a separate Gradle task.
#
# Please check https://github.com/JetBrains/intellij-ui-test-robot for information about
# UI tests with IntelliJ Platform.
#
# Workflow is triggered manually.

name: Run UI Tests
on:
  workflow_dispatch

jobs:

  testUI:
    runs-on: ${{ matrix.os }}
    strategy:
      fail-fast: false
      matrix:
        include:
          - os: ubuntu-latest
            runIde: |
              export DISPLAY=:99.0
              Xvfb -ac :99 -screen 0 1920x1080x16 &
              gradle runIdeForUiTests &
          - os: windows-latest
            runIde: start gradlew.bat runIdeForUiTests
          - os: macos-latest
            runIde: ./gradlew runIdeForUiTests &

    steps:
      - name: Fetch Sources
        uses: actions/checkout@v6

      - name: Setup Java
        uses: actions/setup-java@v5
        with:
          distribution: zulu
          java-version: 17

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v5
        with:
          cache-read-only: true

      - name: Run IDE
        run: ${{ matrix.runIde }}

      - name: Health Check
        uses: jtalk/url-health-check-action@v4
        with:
          url: http://127.0.0.1:8082
          max-attempts: 15
          retry-delay: 30s

      - name: Tests
        run: ./gradlew test
```

**Step 2: YAML 문법 검증**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/run-ui-tests.yml'))"`
Expected: 에러 없이 정상 종료

**Step 3: 커밋**

```bash
git add .github/workflows/run-ui-tests.yml
git commit -m "ci: Run UI Tests 워크플로우 추가 (수동 트리거, 멀티 OS)"
```
