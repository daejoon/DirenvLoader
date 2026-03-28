# Changelog

## Unreleased

### Added

### Changed

### Deprecated

### Removed

### Fixed

### Security

## 1.0.2

### Fixed
- Fix `Synchronous execution on EDT` error in `isDirenvInstalled()` by running process via `ProgressManager` on background thread

## 1.0.1

### Fixed
- Add compatibility support for IntelliJ IDEA 2026.1 (build 261) by extending `untilBuild` range from 253.* to 261.*
