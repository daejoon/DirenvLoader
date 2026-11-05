# Changelog

## Unreleased

## 1.1.0

### Changed

- Require IntelliJ IDEA 2025.1 (build 251) or later; supported builds are now 251–263.*

### Fixed

- Fix `.envrc file not found` for subprojects in a Multi-Project Workspace by running direnv in the Run Configuration's working directory, falling back to the project root when it is empty or does not exist

## 1.0.6

### Changed

- Extend IntelliJ IDEA compatibility through build 263.*

## 1.0.5

### Changed

- Extend IntelliJ IDEA compatibility through build 262.*

## 1.0.4

### Fixed

- Fix `Synchronous execution under ReadAction` error by adding `isReadAccessAllowed()` check and using `executeOnPooledThread()` instead of `ProgressManager`

## 1.0.3

### Fixed

- Fix `Synchronous execution on EDT` error in `exportJson()`/`allow()` by adding EDT detection to `execute()` method
- Simplify `isDirenvInstalled()` by reusing EDT-safe `execute()` instead of duplicated logic

## 1.0.2

### Fixed

- Fix `Synchronous execution on EDT` error in `isDirenvInstalled()` by running process via `ProgressManager` on background thread

## 1.0.1

### Fixed

- Add compatibility support for IntelliJ IDEA 2026.1 (build 261) by extending `untilBuild` range from 253.* to 261.*
