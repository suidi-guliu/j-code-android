# 架构升级实际状态与迁移审计

基线：`sui-code/core-foundation` 的 `f4955b99d132d025c0478d5e64c15bfe6dfb860b`。
分支：`sui-code/native-ecosystem-ui`。本表必须随接线更新，不能以规范页或样例替代功能验收。

| 项目 | 当前事实 |
|---|---|
| 独立二进制包格式/识别 | 模块实现SUICPKG/SUIKPKG魔数、1.0头、长度及独立MIME |
| 独立解析器/加载器 | PluginParser/IdeKitParser、PluginLoader/IdeKitLoader；不依赖JCode API |
| Manifest/组件职责 | 双Schema、静态验证、越界拒绝 |
| SHA/签名 | 实际SHA-256、P256签名验证及host固定根匹配；尚未部署官方根 |
| 授权生命周期 | 摘要绑定Consent、声明权限与授权交集、请求绑定组件Operation、close后拒绝 |
| 安装与回滚 | 私有单host存储、原子状态发布、更新撤销旧会话、依赖保护、显式rollback |
| UI扩展基础 | PanelState/PanelStore及DesignSystem；没有Compose renderer |
| 公共规范/模板/验证工具 | 两份Markdown、完整Schema、完整Manifest、样例二进制、实际生产CLI |
| Android包导入及管理界面 | 未接入 |
| 清除JCode扩展生态依赖 | 未完成；旧调用保留用于安全迁移 |
| 四语言迁移新包 | 未完成；原QuickRun继续保留，示例不是正式运行包 |
| 六个完整官方语言包 | 未完成；无假冒“完整IDE”空包 |
| 官方目录和生产签名根 | 未完成 |
| LSP/DAP/Build/Test等backend | 通用请求门禁有实现，实际语言backend未接入；E_UNAVAILABLE |
| 全局UI与功能中心重构 | 仅设计契约/Token已实现，屏幕重构未完成 |
| 新安装仅Core | 未切换，避免在迁移完成前破坏既有功能 |
| 用户环境/源码 | 本阶段不更改Android应用、工具环境或迁移用户数据 |
| 新产品APK | 本阶段不将旧APK改名冒充新架构成果 |

## 真实耦合位置

- `app/.../MainViewModel.kt` 直接持有 `MarketplaceServiceLocator.extensionInstaller`；既有安装、贡献项和后台启停状态均由此管理。
- `app/.../JCodeShell.kt` 从 installedExtensions/extensionActivations 生成语言、设置、SCM、数据库等入口；直接删除市场模块会同时破坏这些工作流。
- `app/.../ext/NativeExtensionLoader.kt` 用 DexClassLoader 共享进程和 JCode API；新协议明确不复用这一加载器。
- `app/.../workbench/VsCodeExtensionHost.kt`、旧 `.jext/.vsix` 安装分支与新格式无关，不可仅改名作为新生态。
- `core/distro/.../QuickRun.kt`、`NativeQuickRun.kt`、`JavaEnvironment.kt` 目前仍含语言实现。迁移时应将语言规则移出Core，通过新协议对接已有保存屏障、PTY、GuestProcessTree和环境探测；不能先删除再补功能。
- 原 `LICENSE` 为MIT，版权声明仍保留。还需在最终重构交付中对新增语言工具、图标、字体及其他资源单独进行许可证清单核对。

## 下一迁移门槛

1. 为包声明建立受限运行plan解释器和真正Core执行backend，替换四语言Core硬编码；包作者不能获得任意host shell。
2. 新旧环境只读探测和幂等迁移；明确新安装/旧安装分支，不删旧配置；失败回到旧登记。
3. 补齐真实语言服务、调试、工程和工具适配，验证卸载Kit不影响基础运行。
4. 在Android host接入本地导入、审阅、授权、包管理及官方目录；随后移除原扩展市场/生命周期和桥接，而非共存后宣称独立完成。
5. 将DesignSystem应用到所有页面并完成横/竖、深浅主题、键盘/触控和无障碍测试，再交付同签名递增版本APK。

新模块测试是JVM x64验证，不代表Android ARM64/PRoot实际运行通过。签名算法和原子保存均须在最终设备集成验收。

## 本阶段实际验证结果

云端 Linux x64 / JDK21：Gradle `:core:native-packages:testDebugUnitTest` **30项通过，失败0、错误0、跳过0**。包括格式/类型、规范JSON/UTF-8/深度、SHA/P256、权限/职责、不同版本更新与回滚、会话撤销、安装失败保留旧状态和Kit依赖卸载保护。生产CLI分别接受minimal.suic、minimal-kit.suik；非法包非零退出并打印 `E_BOUNDARY: Plugin permission outside SimpleRun scope`。

`check_specifications.py` 验证完整Schema/内嵌Manifest快照同步、19个合法组件描述及非法样例拒绝。GitHub Actions独立重跑相同协议测试，生成源码补丁并上传规范、样例、工具、模块源码和测试XML；它是协议SDK交付，不是最终Android产品APK。
