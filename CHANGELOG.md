# Changelog / 更新日志

## v3.0.0 (2026-03-11) - Fork Release

Based on [sky130/MiWu](https://github.com/sky130/MiWu) v2.0.8.
基于 [sky130/MiWu](https://github.com/sky130/MiWu) v2.0.8。

### Bug Fixes / 问题修复

- **Tiles 持久化修复**: 修复设备重启后 Tiles/卡片设备列表丢失的问题 (#33, #43, #47)
  - 根因: `deviceListFlow` 是冷 Flow，仅在 UI 组件（ViewModel）中被收集。TileService 在设备重启后独立启动时，没有 UI 收集 Flow，导致内存中的 `deviceList` 为空
  - 修复: 在 `LocalRepositoryImpl` 初始化时通过 `deviceListFlow.launchIn(scope)` 主动收集 Flow，确保设备数据从 Room 数据库加载
- **Tiles persistence fix**: Fixed device tile configuration loss after watch reboot (#33, #43, #47)
  - Root cause: `deviceListFlow` was a cold Flow only collected by UI ViewModels. When TileService started independently after reboot, the in-memory `deviceList` remained empty
  - Fix: eagerly collect `deviceListFlow` in `LocalRepositoryImpl.init{}` via `launchIn(scope)`

### New Features / 新功能

- **场景快捷面板 (SceneTile)**: 新增 Wear OS Tile，可在表盘左右滑动的卡片中显示米家场景列表
  - 显示最多 4 个场景
  - 一键点击即可触发场景执行（如: 离家/到家/睡觉模式）
  - 通过 `SceneRunActivity` 处理场景执行
  - 场景数据刷新后自动更新 Tile
- **Scene Quick Panel (SceneTile)**: New Wear OS Tile showing Mi Home scenes
  - Displays up to 4 scenes
  - One-tap to trigger scene execution (e.g., Leave Home, Arrive Home, Sleep)
  - Handles scene execution via lightweight `SceneRunActivity`
  - Auto-refreshes tile when scene data updates

### Technical Changes / 技术变更

- targetSdkVersion: 35
- compileSdkVersion: 36
- Kotlin: 2.3.0
- 新增 `SceneTileService` 和 `SceneRunActivity`
- `AppRepositoryImpl.refreshScenes()` 现在会自动刷新 SceneTile
