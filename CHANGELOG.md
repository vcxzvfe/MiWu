# Changelog / 更新日志

## v3.1.0 (2026-03-16)

### Bug Fixes / 问题修复

- **扫码登录修复**: 修复二维码扫码登录完全失效的问题。`LoginQrCode.toQrCode()` 中 `data` 和 `loginUrl` 字段互换，导致二维码显示的是轮询 URL 而非可扫描数据，同时轮询请求发送到错误地址。此外修复 `generateLoginQrCode()` 中 URL 参数编码问题
- **QR Code Login Fix**: Fixed QR scan login being completely broken. `LoginQrCode.toQrCode()` had `data` and `loginUrl` fields swapped — the QR code displayed the polling URL instead of scannable data, and polling hit the wrong endpoint. Also fixed URL parameter encoding in `generateLoginQrCode()`
- **miot-api-impl 依赖修复**: 添加缺失的 `slf4j-api` 依赖，修复 `MiotLoginProviderImpl` 中日志记录器的编译问题
- **miot-api-impl dependency fix**: Added missing `slf4j-api` dependency for `MiotLoginProviderImpl` logger
- **登录日志增强**: 在登录流程（二维码生成、轮询、账密登录）中添加详细日志，便于排查认证失败问题
- **Login Logging**: Added comprehensive logging throughout login flow (QR generation, polling, classic login) to aid debugging

### New Features / 新功能

- **场景长按操作**: 实现 `SceneFragment` 中未完成的长按处理。长按场景条目弹出确认对话框，显示场景名称和动作数量，可直接执行场景
- **Scene Long-Press**: Implemented the previously unfinished scene long-click handler. Long-pressing a scene shows a confirmation dialog with scene name, action count, and option to execute

### UI Improvements / UI 优化

- **Galaxy Watch Ultra 适配**: 新增 `values-sw220dp-round` 资源限定符，为大尺寸圆形屏幕（480x480）优化边距和间距
- **Galaxy Watch Ultra Support**: Added `values-sw220dp-round` resource qualifier with optimized padding for large round displays
- **触控区域优化**: 场景和设备列表项最小高度提升至 56dp，提升 Wear OS 上的点击精度
- **Improved Touch Targets**: Minimum height of scene/device items increased to 56dp
- **列表项样式优化**: 圆角从 15dp 增至 18dp，内边距和间距改善
- **Modern List Items**: Corner radius increased from 15dp to 18dp, improved padding and spacing
- **RecyclerView 滚动优化**: 设置 `clipToPadding=false`，列表内容可平滑滚动至标题栏下方
- **登录页二维码布局**: 改善二维码图片在圆形屏幕上的居中和边距

### Security / 安全

- **签名凭据安全化**: 将发布签名信息（keystore 密码、别名、密钥密码）从 `build.gradle.kts` 硬编码移至 `local.properties`，该文件已被 `.gitignore` 排除
- **Signing Credentials**: Moved release signing config from hardcoded `build.gradle.kts` to `local.properties` (excluded from version control)

---

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
