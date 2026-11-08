# sui-code

Android ARM64 平板原生多语言 IDE，基于 JCode 的许可源码继续开发。保留原项目版权、MIT 许可证和第三方许可证。独立应用身份 `dev.suicode.ide.debug`，升级不得清除已有 Ubuntu、JDK、工具链或用户文件。

- 常驻 Internal Files：无需 Workspace/Project 创建、编辑和保存文件；保留 Project 与导出。
- 独立 `.suic` Basic Plugin：Java、合并 C/C++、Python，统一 Ctrl+F5 单文件运行与 PTY 交互/停止。
- 独立 `.suik` IDE Kit：真实 LSP/DAP、工程模板、Build/Test/Run 和受控原生面板。
- 自有签名扩展库、本地导入、权限审阅、启停/更新/回滚；无 JCode 或 VS Code 市场替代入口。
- sui-code 原生 Light/Dark 设计系统及尺寸驱动的文件栏、编辑器、底部 Terminal。

## v0.5.1 UI polish

本轮保留原v0.5图标，提供自适应可调侧栏、Project-only创建入口、Files/Find搜索分工和API4 `.suik` 本地化基础设施。完整中文翻译包另行交付。[本轮报告](docs/sui-code-ui-polish-0.5.1.md)。

## 公开开发规范与源码

- [Native Extension & IDE Kit Development Specification](docs/specifications/SUIC-SUIK-Development-Specification.md)
- [UI Extension & Design Specification](docs/specifications/SuiCode-UI-Design-Specification.md)
- [规范索引、完整 Schema、SDK 与示例](docs/specifications/README.md)
- [官方六个签名包与可审阅源码](native-library/)
- [v0.5 工作台整合测试与升级报告](docs/sui-code-workbench-0.5.md)
- [v0.5 整合 APK、源码、SDK 和测试证据下载](https://github.com/suidi-guliu/j-code-android/actions/runs/42486796937/artifacts/13620305237)（需登录，Artifact保留30天）
- [最终原生架构说明和数据保护](docs/sui-code-native-architecture.md)
- [原 JCode README 与项目来源](docs/upstream-README.md) · [LICENSE](LICENSE)

v0.5 开发分支 `sui-code/v0.5-workbench-reliability`（基于已验证 v0.4 的 `50aec6a`），不修改 main、不发布正式 Release。最终 Debug APK、源码包、SDK、SHA-256 和测试报告通过该分支的 GitHub Actions 交付。云端 Linux x64 测试与 Android ARM64 平板验收严格分开；具体构建和验证结果以交付报告为准。

## 开发验证

使用 JDK21、Android SDK36 和仓库 Gradle wrapper。运行语言集成测试时，另将 `SUI_QUICKRUN_TEST_JAVA_HOME` 指向经过验证的 JDK27。

```sh
python3 -m pip install -r tools/sui_packages/requirements-dev.txt
python3 tools/sui_packages/check_specifications.py
bash gradlew :core:native-packages:testDebugUnitTest :core:distro:testDebugUnitTest :core:fs:testDebugUnitTest :core:design:testDebugUnitTest :app:testDebugUnitTest
bash gradlew :app:assembleDebug
```

默认 Debug 身份为 `dev.suicode.ide.debug`、versionCode1070804；签名必须使用与已安装版本相同的私钥。不要在没有核对实际签名证书的情况下卸载旧版。签名私钥不包含在源码、SDK 或 Artifact 中。
