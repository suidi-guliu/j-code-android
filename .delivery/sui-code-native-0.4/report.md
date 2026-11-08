# sui-code 0.4.0 原生生态与统一 IDE 交付报告

本报告对应独立分支 `sui-code/final-product`。这是统一 Debug 集成包；没有发布 GitHub Release，没有修改 main 或官方上游。云端验证与 ARM64 实机验收分别列出，未将 JVM/Linux x64 结果描述为平板通过。

## 1. 本轮实现的功能

- 独立 `.suic` / `.suik` 二进制容器、Manifest、专属解析器/加载器、生命周期、摘要授权和事务包存储。格式使用20字节独立头，不能把 ZIP 改后缀冒充原生包。
- Android Native Package Manager：真正的本地导入、官方 HTTPS 下载、SHA-256/P256 校验、权限及执行行为审阅、启用/禁用/更新/卸载、重新授权回滚。官方六个签名文件、目录及固定公钥已提交并可从 Fork 获取。
- Java、合并 C/C++、Python 三套 Basic Plugin + IDE Kit。Basic 不依赖 Kit；语言运行规则、LSP/DAP、模板、工程工作流和面板来自包内声明/认证资源，未复用 JCode 插件 API、包格式、市场或生命周期。
- 统一 Ctrl+F5 执行链：激活编辑器文件→等待保存→真实路径映射→加载已有环境→共用 PRoot/PTY→Terminal。支持中文、空格、引号/特殊字符路径、交互输入、持续输出、重复键抑制、忙任务拒绝、停止及关闭会话清理。编译输出使用任务专属临时目录，强杀也由宿主清理。
- Kit 接入真实 Java JDT LS / Java Debug、C/C++ clangd / LLDB、Python pylsp / debugpy；复用编辑器现有智能和诊断客户端。LSP 写编辑受 file.write 授权及版本检查，调试准备可取消，停用 Kit 会撤销所属进程/会话。
- Java Maven/JUnit、C/C++ CMake/Ninja/CTest、Python unittest 的实际多文件模板与 Build/Test/Run。第三方 `.suik` 可贡献同机制的 argv 工作流和受控原生面板；统计示例读取真实文件数据，调试面板可调用同 Kit 的状态、继续、暂停、单步和停止。
- Language Center 复用 v0.1 Java GA 查询/安装/日志，增加受保护的 JDK 列表、选择和闲置安装删除。C/C++ 作为 GCC/G++/GDB/CMake/Ninja 组合项，APT 安装/更新需审阅、取消等待安全边界。Python 从官方 CPython feed 查询最新稳定版本，安装/更新明确采用 Ubuntu 签名包版本。
- 常驻 Internal Files 保留原 `filesDir/workspace/projects` 路径，普通文件无需创建 Workspace/Project。保留 Project、编辑保存和导出；模板发布拒绝覆盖/穿越/链接，失败回滚保护并发编辑。导出不跟随符号链接、不复制 node_modules，并使用新目标目录避免覆盖旧导出。
- sui-code 独立 launcher、全局 Light/Dark 主题、统一 Token、4/8dp 圆角、排版、图标和触控尺寸。文件/运行/语言/Plugin/IDE Kit/Package/设置导航可见，管理中心使用完整页面；文件栏预留编辑空间，Terminal 上下停靠并支持拖拽/可键盘操作的设置滑块。

## 2. 关键修改文件与公共规范

| 范围 | 关键位置 |
|---|---|
| 协议/权限/存储/解释器 | `core/native-packages/src/main/java/dev/suicode/packages/` |
| Android 包宿主及 UI | `app/src/main/java/dev/suicode/nativehost/` |
| 编辑器保存和 IDE 调度 | `MainViewModel.kt`、`JavaQuickRunController.kt`、`JCodeShell.kt` |
| LSP / DAP | `app/.../lsp/LspController.kt`、`app/.../debug/DebugController.kt`、`core/lsp/`、`core/debug/` |
| PRoot、环境和进程 | `core/distro/` 的 DistroService、GuestProcessTree、Java/NativeEnvironment 管理器 |
| 文件安全和工程模板 | `core/fs/` 的 SafeLocalFiles、TemplatePublisher、ExportTree |
| 自有设计系统 | `core/design/` 的 SuiDesign、M3Theme、Radius、Space、ControlSize；Workbench 导航和 Shell |
| 六包与官方目录 | `native-library/src/`、`native-library/v1/`、`app/src/main/assets/sui-native/` |
| SDK、验证与协议测试 | `tools/sui_packages/`、各模块 `src/test/` |
| 构建身份/品牌 | `app/build.gradle.kts`、launcher/strings 资源 |
| CI/交付 | `.github/workflows/verify-native-package-protocol.yml`、`deliver-sui-code-native-0.4.yml` |

完整清单随 Artifact 的 `modified-files.txt` 提供。公开规范为真实仓库文件，包含完整接口、Schema、示例、版本、权限、格式和失败规则：

1. [sui-code Native Extension & IDE Kit Development Specification](https://github.com/suidi-guliu/j-code-android/blob/sui-code/final-product/docs/specifications/SUIC-SUIK-Development-Specification.md)
2. [sui-code UI Extension & Design Specification](https://github.com/suidi-guliu/j-code-android/blob/sui-code/final-product/docs/specifications/SuiCode-UI-Design-Specification.md)

当前规范0.2.0、Core API2（兼容 API1）、SUIC/SUIK1.0。`panel.nodes` 要求 API2。两份规范和示例均在 source.zip 与 native-sdk.zip 中；README 和规范索引提供入口。

## 3. 分支和提交

- 仓库：`suidi-guliu/j-code-android`。
- 起点：`fa44a36efad247c153a7c831125e3de88b60634c`，父提交 `f47d29676cfc8dad621056d415b8a7f5bf84fb4e`。
- 分支：`sui-code/final-product`。
- `6c2b7b1`：独立 API2、六个签名包、SDK 和公开规范。
- `211e06d`：Android 宿主、统一运行/IDE、环境管理、文件安全和全局原生设计。
- APK 生产代码基线：`70c458a5fe0010d9981aa433ea85c7c9259edc16`，包括强停临时目录回收修复。
- 后续提交仅完善 GitHub runner 配置、测试证据和交付；最终分支提交见仓库历史。源码包和 changes.patch 包含交付配置，不包含历史 APK 或签名私钥。

## 4. 构建结果

Codex Cloud 实际执行 Gradle `:app:assembleDebug`，包含原 ARM64 原生模块，最终构建 **BUILD SUCCESSFUL（38秒，697项任务）**。开发期间执行 Kotlin 编译和测试；本次只对外统一交付此 APK。

另实际生成独立 JVM SDK，复制11份生产 API 源文件并输出字节校验清单；不依赖 Android SDK 的 `classes` 构建通过（43秒）。此 SDK 不模拟 Android backend；完整宿主在 Android 源码包中。

## 5. 测试与证据

各模块最后有效测试 XML 合计 **194项通过，0失败、0错误、0跳过**：

| 模块 | 用例数 | 结果 |
|---|---:|---|
| core/fs | 16 | 通过 |
| core/distro | 55 | 通过 |
| core/native-packages | 47 | 通过 |
| core/design | 57 | 通过 |
| app | 19 | 通过 |

实际执行命令和日志：regression14–17 对应各模块回归；最终包/路径/TMPDIR变更后重新执行 native-packages、distro、app。XML、APK/SDK构建日志及集成日志在 `test-evidence.zip`，不包含私钥或用户环境。

| 验证层级 | 实际覆盖 |
|---|---|
| JVM 单元/集成 | 容器格式、Manifest、API兼容、权限/身份防伪、坏签名/摘要/越界拒绝、依赖、安装/更新/回滚/重启恢复、文件冲突/失败/链接、模板回滚、JDK选择失败保护、原 ProjectRunner 回归、内存/缓存、主题Token与响应式边界、受控UI树 |
| Linux x64 执行 | 实际 Java/C/C++/Python 编译/解释；自动保存顺序、中文/空格/特殊路径、无 Project 文件、环境缺失、编译/运行异常、PTY stdin、持续程序停止及子进程清理、连续运行、重复键 gate、无源码垃圾 |
| Ubuntu24.04 x64 Docker | 三套真实项目共6次 Build/Test，另3次 Run；官方 JDTLS/Java Debug/Maven 下载和摘要验证；安装前后 java.env 字节一致 |
| 真正 LSP/DAP 协议 | Java initialize/main解析/build/classpath/断点/调用栈/真实输出/结束；C/C++、Python initialize/completion/formatting，以及断点/调用栈/scopes/variables/真实输出/结束 |
| SDK/CLI | 独立编译后的生产验证器验证15个合法包；非法 .suic 因 E_BOUNDARY 被拒绝；官方6包固定根签名在单测验证 |
| GitHub 发布读取 | 六个正式包和两份规范 HTTP200，下载大小/SHA或原文与提交内容一致 |
| GitHub runner | [GitHub Actions 42441863555](https://github.com/suidi-guliu/j-code-android/actions/runs/42441863555) 实际 Success，5分3秒；194项宿主测试、合法包验证和非法包拒绝任务完成，真实测试 Artifact 已产生。 |
| Android ARM64 / 平板 | 未执行；没有连接 adb 实机。未把上述测试算作 Android GUI/PRoot/升级实测 |

GitHub JVM 契约任务不重复构建 ARM64 Rust JNI（x64 JVM 测试不能加载它）；最终 APK 已在配置完整的 Codex Cloud 执行正常 assembleDebug。初始 GitHub任务缺 cargo-ndk 的失败有公开诊断，后续 CI 配置明确区分这两种验证。

## 6. APK 身份、版本及升级兼容性

| 项目 | 实际结果 |
|---|---|
| applicationId | `dev.suicode.ide.debug` |
| versionName | `0.4.0` |
| versionCode | `1070802`，高于已知旧版1070801 |
| launcher label | sui-code |
| ABI | arm64-v8a，11个原生.so，无其他 ABI |
| APK 字节数 | 86760725 |
| signer certificate SHA-256 | `62c2da3484d337626eac6711e5ea7015913e23b8932e9f2b2bd220b843579fa0` |
| v0.1 比对 | 对原始 APK 实际 apksigner 检验，证书完全一致 |

原 v0.1 APK 的 SHA-256 仍为 `1554f7e7cb7359b4b90161d08e76bf515b0e0805adf4777bdb8851ef23f2cb06`；原 v0.3 仍为 `661bd7c90f06bf5630d0c4e73b85b378665a4baa4b51db0a7ce94af61ce9010e`。旧 APK、报告和交付目录未覆盖。

**包名、版本及签名满足覆盖安装条件。** 不需要卸载，不需要重装 Ubuntu/JDK；没有清数据迁移。保留原应用私有数据及 Ubuntu、java.env、JDK/GCC/G++/Python路径。实际平板覆盖安装前后的数据比对仍需第9节实测，不把静态兼容性核查当作真实升级已通过。

## 7. 下载入口与 SHA-256

APK：`sui-code-native-0.4-debug.apk`。

SHA-256：

```text
d392d22003dbe8aee0e7cb63669fac8b5285cb7b2f3ab01bb5ade4046ffd4128
```

[最终交付工作流与 Artifact 入口](https://github.com/suidi-guliu/j-code-android/actions/workflows/deliver-sui-code-native-0.4.yml?query=branch%3Asui-code%2Ffinal-product)。选择成功运行，在 Artifacts 下载 `sui-code-native-0.4-complete`；GitHub 要求登录。最终聊天回复提供该成功运行和具体 Artifact 链接。

包内含 APK、report.md、changes.patch、modified-files.txt、source.zip、native-sdk.zip、official-packages.zip、test-evidence.zip、apk-metadata.json、两份规范和 SHA256SUMS。完整源码包括公开规范/示例；SDK有独立JVM构建；官方包 ZIP 含六个原生文件、签名目录、公钥。私钥、已安装Ubuntu和用户数据不交付。

交付 Actions 只上传已构建 APK，重新核对大小、SHA、旧证书、版本及 ABI，并下载上传结果逐文件 cmp / SHA 验证；不会创建 Release、合并或推送 main。

## 8. 尚未验证与明确限制

- **硬件未验证**：真实 ARM64/PRoot ptrace、外接键盘 Ctrl+F5、Android Terminal 输入/停止/关闭、IME、旋转/分屏、主题切换、TalkBack、SAF授权写出，以及覆盖安装保留数据，均待目标平板。云端不能替代这些项目。
- DAP 当前使用 internalConsole，没有声明 runInTerminal；QuickRun 已接入交互 PTY stdin。调试器在 PRoot 内能否正常使用 ptrace 必须按设备验证，失败应保留日志处理。
- Python 的最新稳定版本查询与安装渠道明确分开：查询官方 CPython，安装/更新使用 Ubuntu 签名支持版本，不声称 Ubuntu 包就是最新上游版本；本轮没有新增 CPython 源码编译安装器。
- `.suik` UI 是受控原生 widgets/命令，不支持任意 DEX/HTML/JS 或覆盖核心主题。process.run 执行的是用户审阅的 Ubuntu 程序；**PRoot 不是恶意代码安全沙箱**。
- 旧 JCode `.jext/.vsix` 不作为新包加载，也没有旧市场替代入口；历史源码及旧插件磁盘数据保留。只删除/禁用原生包不删除工具链或文件。
- UI 已完成源代码整合与构建，云端 Token/布局边界检查通过；没有虚构平板截图、自动化 Android GUI 点击或真实设备验收。

## 9. ARM64 平板逐步验收（统一 APK，一次升级）

1. 保留旧应用，记录已有文件、Project、终端配置、Ubuntu版本、`. /opt/sui-code/java.env; java -version` 和 gcc/g++/python3 版本。不要卸载或清数据。
2. 下载最终 Artifact，核对 APK 的 SHA-256，点击 APK 覆盖安装，或由已授权 adb 使用 `adb install -r`。若系统拒绝签名/升级，停止并保留旧版，不能通过卸载来绕过。
3. 重开后检查已有 Ubuntu、JDK路径、环境文件、GCC/G++、Python、Project、设置和用户文件均存在。普通终端显式加载原 java.env 应仍可运行。
4. 打开 Plugin Center，先审阅安装三份 Basic Plugin。此时**不安装 IDE Kit**，用现存环境验证以下单文件流程；没有安装语言环境时应明确报错而非自动下载。
5. 在 Internal Files 多级中文/空格目录新建 HelloWorld.java，无 Workspace/Project；修改但不手动保存，外接键盘 Ctrl+F5，确认最新内容已保存并在 Terminal 输出。确认源码旁没有.class。
6. 分别建立 C、C++、Python 小程序；验证 C17/C++配置、错误信息、stdin输入、运行异常、不同目录和连续执行。长按Ctrl+F5不得产生并发重复任务。
7. 运行持续输出/生成子进程的程序，按停止、关闭对应终端、再次运行；确认没有残留程序和重复输出，原有其他终端/ProjectRunner不受影响。
8. 在 IDE Kit Center 审阅安装三份 Kit；Language Center 明确确认安装 IDE 工具。**已有 JDK 不需重装**，Java Kit不会通过 apt 安装第二套JDK。
9. 用Kit模板创建 Java、C/C++、Python Project，逐项 Build/Test/Run；编译错误和断言失败应有真实输出，不显示虚假成功。打开文件检查智能、诊断、格式化、引用/重命名等服务器实际能力。
10. 每种语言设置断点，检查停止、变量、调用栈、单步、继续和结束。验证Kit调试面板按钮；若PRoot ptrace失败，保留Debug/Language Center日志，不重装Ubuntu。
11. 运行时禁用Kit应停止所属服务；卸载Kit后Basic仍能单文件运行，环境不丢失。通过Package Manager导入SDK合法/非法包，验证权限拒绝、错误签名/摘要拒绝，以及更新/回滚/重启恢复。
12. Java管理页刷新已安装JDK，确认原JDK被识别且可选；使用中的或活跃JDK不得删除。需要测试删除时仅删除另装的闲置测试JDK，不删除原v0.1安装。
13. 横屏、竖屏、分屏、Light/Dark和大字体逐一检查；文件栏不能盖住编辑器，Terminal高度拖拽/键盘滑块可用，导航、保存、停止、确认按钮可达。
14. 导出普通文件及Project到授权存储并核对内容；重启应用再次检查数据和环境。此导出不跟随符号链接，也不包含node_modules，不等同完整Ubuntu备份。

验收结论：静态升级兼容性已证实，可使用覆盖安装路径；原JDK/Ubuntu不被迁移或重装；Ctrl+F5已经连接真实统一执行链，交互输入和停止已经实现。最终设备运行、升级保留和 UI/PRoot 表现仍需以上 ARM64 实机步骤，不能提前宣称全部设备验收完成。
