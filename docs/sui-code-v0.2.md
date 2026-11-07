# sui-code v0.2 — 原生 Java QuickRun 开发与验收报告

日期：2026-11-08（Asia/Shanghai）。状态：代码、云端测试、Debug 构建通过；ARM64 平板操作与覆盖升级待实机确认。

## 1. 本轮实现的功能

- 从 v0.1 最新有效提交 `5496f14a1d4a3dfa3a9c3528fbe3f8ce154e1f4d` 创建 `sui-code/v0.2-java-quickrun`，没有基于缺少 v0.1 的 main 重新开发。
- 编辑器获得焦点时，物理键盘 `Ctrl+F5` 通过 `EditorView.dispatchKeyEvent` 调用原生 QuickRun，不经过工程/Run Configuration。
- 从 ViewModel 当时的激活标签捕获文件与编辑状态，只接收 `.java` 文件。先等待 `saveTabAwait` 完成；保存失败、保存期间又有编辑、无法读取本地文件或 Ubuntu 改变时，不创建运行进程。
- 非 Java 标签不启动其他运行任务。长按 repeatCount 和运行中重复启动由原子门闩拦截；再次运行前关闭上一条已完成的 QuickRun 会话，避免累积会话及 Output 捕获记录。
- 每次启动明确加载 `/opt/sui-code/java.env`，清除继承的 JAVA_HOME 后读取受管理的配置，使用绝对路径 java/javac。没有环境文件、JDK 工具缺失或 Java 启动失败时明确报错；不改用 Android Java、其他 JDK，不调用 apt、不触发安装器。
- 通过既有 PRoot/PTY/TerminalSessionManager 创建独立可管理终端，将真实文件父目录绑定到 guest `/sui-quickrun`；该目录作为工作目录，因而相对资源仍来自当前文件目录。命令里的用户路径一律 shellQuote，长脚本先暂存，不把源码或长命令灌入 PTY。
- 快捷键会自动打开并聚焦既有 Terminal 面板。stdin 直接连接 PTY，stdout、stderr 和异常堆栈原样显示，并汇入已有 Output 通道。
- 先将已保存文件快照复制到私有临时目录，执行 `javac -proc:none -d <临时目录>` 做编译诊断；编译失败返回 65。随后使用同一 JDK 的源文件启动；运行失败返回 70，并保留原 Java 退出码及堆栈。环境错误返回 78，文件错误 66，准备失败 73。
- 云端 Java 27 测试证明：源文件路径不匹配 package 时，Java 27 会拒绝源文件启动。本实现用 JDK 自带的 JavacTask AST 解析包名，将快照放到匹配包名的私有目录再源文件启动，不用正则猜 Java 语法。工作目录保持为原文件父目录。生成的 `.class`、辅助源码和快照都在临时目录，正常退出由 trap 清理。
- 使用已有 Stop 控件停止 QuickRun；即使没有工程，运行期间该原有控件也可用。停止会关闭 QuickRun PTY，先按唯一 owner 标记终止 guest tracee，再清理不响应终止的 Java/子进程，然后关闭 PRoot launcher。手动关闭会话、明确退出应用及 ViewModel 释放也走此清理路径。
- QuickRun 的会话、门闩、标记与 ProjectRunner 分开管理。停止 QuickRun 不停止现有工程任务。Java 原生适配器通过 registry 分发，后续可添加 C/GCC、C++/G++、Python 适配器；本轮未实现或安装这些语言环境。
- 未重设 JCode 名称、图标、主题和布局。仅接入快捷键及复用现有 Stop/Terminal 功能，没有视觉重设计。

## 2. 修改的关键文件

| 文件 | 用途 |
| --- | --- |
| `core/distro/.../QuickRun.kt` | 语言 registry、重复触发门闩、保存完成门控、Java 临时编译/源启动计划、可测试的停止策略。 |
| `core/distro/.../JavaEnvironment.kt` | 复用 JavaLanguageAdapter 接口，接入实际 Java 执行计划。 |
| `app/.../JavaQuickRunController.kt` | 当前文件生命周期、保存等待、文件绑定、创建/聚焦/停止独立终端、会话回收。 |
| `app/.../MainViewModel.kt` | 捕获激活标签，连接保存与 QuickRun，转发完成事件，退出清理。 |
| `core/editor/.../EditorView.kt`、`feature/editor-pane/.../EditorPane.kt` | 焦点内 Ctrl+F5 和 repeatCount 传递；真实连接 Controller，而非空处理器。 |
| `core/design/.../EditorSaveActions.kt` | 附加 QuickRun 操作属性；保留原设计 SDK 构造函数的 JVM 描述符。 |
| `core/term/.../TerminalSessionManager.kt`、`core/distro/.../GuestProcessTree.kt` | 可选的专属 owner 标记与先杀 tracee 后关 launcher；原 createSession 及默认参数 ABI 保留，普通终端行为不变。 |
| `app/.../JCodeShell.kt`、`workbench/WorkbenchModel.kt`、`workbench/WorkbenchChromeBars.kt` | 合并既有终端聚焦通道，连接 Stop 和无工程运行状态。 |
| `app/.../OutputLog.kt` | 关闭 QuickRun 会话时回收自身捕获状态，不清空其他工程输出。 |
| `feature/sdk-manager/.../JavaEnvironmentPage.kt` | 更新原管理器中的功能说明；安装器、JDK 存储和日志流程不变。 |
| `app/build.gradle.kts` | 提供明确的正整数 versionCode 构建覆盖项。 |
| `core/distro/.../JavaQuickRunTest.kt`、`JavaEnvironmentTest.kt` | 新执行链路测试与 v0.1 回归。 |
| `core/design/.../ExtensionAbiSurfaceTest.kt` | 扩展 ABI 检查及原构造签名回归。 |
| `app/src/test/.../ProjectRunnerQuickRunRegressionTest.kt` | 原有工程命令暂存与真实 shell 执行回归。 |
| `.github/workflows/deliver-sui-code-v0.2.yml`、`.delivery/sui-code-v0.2/` | 原 APK 上传、下载回验及升级身份/签名检查，仅用于 v0.2 分支。 |

完整源码差异以交付的 `changes.patch` 为准；原 `ProjectRunner.kt` 未修改。v0.1 的 APK、报告、补丁及交付工作流未覆盖。

## 3. 分支名称与提交 SHA

分支：`sui-code/v0.2-java-quickrun`。

- `95d5ae2`：Java 计划、键盘入口、停止策略及测试。
- `6421a95`：当前编辑文件保存—运行—停止链路、独立升级身份及工程回归。
- `e8697745ad769fb6180c7dd0355c63f41a357d8d`：保留扩展/终端 ABI，回收 Output 捕获。最终 APK 源码对应此提交。
- 后续文档/工作流/交付提交不改变上述 APK 源码；最终分支 HEAD 在 GitHub 可查。

## 4. 构建结果

`BUILD SUCCESSFUL`，Android Debug APK 已生成。使用已验证云端工具链：JDK 21、Gradle 8.14.3、SDK 36、NDK 27.2.12479018、CMake 3.30.5。既有 Cargo JNI 输出已保留，APK 包含 `libripgrep_ffi.so`、`libjcode_wasm.so`，仅 arm64-v8a。

```bash
cd /workspace/j-code-android
source /workspace/setup/env.sh
export SUI_QUICKRUN_TEST_JAVA_HOME=/workspace/toolchains/temurin27/jdk-27.0.1.0.2+1
bash gradlew --no-daemon --max-workers=4 \
  -PjcodeApplicationId=dev.suicode.ide \
  -PsuiVersionCode=1070800 -PjcodeVersionName=1.7.7-sui.0.2 \
  :core:design:testDebugUnitTest --tests '*ExtensionAbiSurfaceTest' \
  :core:distro:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug
```

本轮功能包由本机保留的 v0.1 Debug 密钥签名；Actions 只搬运已验证 APK，不重新构建、不生成另一把签名密钥。

## 5. 测试用例、数量及通过结果

共 **52 项通过，0 失败、0 错误、0 跳过**。core/distro 的 29 项在功能验收构建实际执行；兼容性最终构建对未变化的该模块复用有效结果，core/design 和 app 实际执行。没有将旧 v0.1 快照的测试结果当成本轮新结果。

| 测试组 | 数量 | 结果 |
| --- | ---: | --- |
| JavaQuickRunTest | 16 | 全部通过，真实 JVM/PTY 与策略测试，见下表。 |
| JavaEnvironmentTest（v0.1） | 10 | 全部通过：GA 解析、校验、并存安装、失败/取消等。 |
| GuestShellScriptTest | 3 | 全部通过：用户环境经 su 传递、root 路径等。 |
| ExtensionAbiSurfaceTest | 5 | 全部通过：原扩展引用表面与设计 SDK 构造签名。 |
| ProjectRunnerQuickRunRegressionTest | 1 | 真实执行原工程 Run Configuration 的暂存脚本，未加载 Java 环境，输出 `PROJECT_RUN_OK`。 |
| ManagedCacheTest / MemoryPressureTest / UpdateCheckerTest | 6 / 4 / 7 | 全部通过。 |

JavaQuickRunTest 的 16 个用例：

1. 仅 Java 分发，无工程依赖。
2. repeatCount 与并发门闩抑制重复启动。
3. 保存失败后不解析/执行过期文件。
4. 保存真正挂起时等待完成再解析文件。
5. 保存与路径使用顺序。
6. 中文、空格、引号目录中的 HelloWorld，无源码目录 class 垃圾。
7. package 与当前目录相对资源读取。
8. 中文、空格、单引号、分号、美元符等合法文件名不成为 shell 代码。
9. 缺少 managed java.env 明确失败，不用主机 Java 回退。
10. 配置内 JDK 损坏时明确失败，不回退。
11. 编译失败 65 与运行失败 70 区分，异常堆栈保留。
12. Linux 真实 PTY 标准输入，含中文输入。
13. 连续运行不同文件，输出独立，无源码目录 class 文件。
14. 停止策略首先终止 tracee，并清理不响应终止和脱离父进程的进程，不碰其他 owner。
15. 正常停止不进行多余强杀。
16. 真实 Linux PTY 长时 Java 程序创建 `sleep` 子进程并安装慢退出 hook；按生产同一停止策略清理后，owner 进程集合为空，另一个未标记任务仍存活。

执行 Java 测试的是官方 **Temurin 27.0.1.0.2+1 / Linux x64**，归档来自 Adoptium GitHub 发布，下载后用官方 `.sha256.txt` 验证。构建 Gradle 使用 JDK 21；没有把构建用 JDK 21 误报为 Java 27 执行测试。

## 6. APK 包身份、版本及签名兼容性

| 项目 | v0.1 原交付 APK | v0.2 |
| --- | --- | --- |
| applicationId | dev.suicode.ide.debug | dev.suicode.ide.debug |
| versionCode | 1070799 | **1070800** |
| versionName | 1.7.7 | **1.7.7-sui.0.2** |
| 签名证书 SHA-256 | 62c2da3484d337626eac6711e5ea7015913e23b8932e9f2b2bd220b843579fa0 | **完全一致** |
| minSdk / targetSdk | 33 / 33 | 33 / 33 |
| ABI | arm64-v8a | arm64-v8a |

`apksigner verify` 与 `aapt dump badging` 均通过。核查的 v0.1 APK SHA-256 仍为 `1554f7e7cb7359b4b90161d08e76bf515b0e0805adf4777bdb8851ef23f2cb06`。

结论：与原交付 v0.1 满足同包名、同证书、更高 versionCode 的覆盖更新条件；没有数据清除、JDK 重装或 rootfs 迁移逻辑。预期保留应用内 Ubuntu 和 `/opt/sui-code/jdks/temurin-27.0.1.0-aarch64`。尚未在目标设备实际执行覆盖安装；如果系统报签名冲突或安装失败，停止并保留原应用，不卸载、不清除数据。

## 7. APK 下载方式及 SHA-256

交付文件：`sui-code-v0.2-debug.apk`、`report.md`、`changes.patch`。

- APK 字节数：**88,733,580**。
- APK SHA-256：**e0672c0823403c8176d1bf91d822cf143c0073186e4f35d9373d82b79d7c010e**。
- Actions 工作流：`Deliver original sui-code v0.2 APK`，仅允许 Fork 的 v0.2 分支。
- Artifact 名：`sui-code-v0.2-java-quickrun-debug`，保留 30 天，登录 GitHub 后下载 ZIP。
- 源码补丁基线为 v0.1 最新提交 `5496f14`，不包含交付用 APK 二进制。
- 最终工作流运行页及 Artifact 的真实入口在本次交付回复中提供；不会用容器内部路径冒充下载地址。

## 8. 尚未验证或已知存在的问题

- `adb devices` 没有连接设备。本轮所有运行测试均为云端 Linux x64；**没有宣称 ARM64 PRoot、物理键盘、Android UI 或更新安装实机通过**。
- 用户提供的 v0.1 ARM64 实机验收是既有基线，本轮没有重新验证或破坏它，也不要求重装 JDK。
- 必须实机确认：Ctrl+F5 的硬件键映射/焦点、dirty 文件自动保存、Terminal 自动聚焦、中文输入法与 stdin、Stop/关闭/退出时 Android `/proc` 可见性与 PRoot 清理、长时间运行及连续执行。
- Linux host PTY 的停止测试使用生产同一清理策略，但信号和进程枚举适配器是 Linux host，不能替代 Android `Os.kill`/SELinux 的集成验收。
- Java 预编译校验、JDK AST helper 和源启动会额外启动 JVM，优先保证错误分类与 Java 27 包名兼容，平板冷启动耗时尚待测量。
- 正常退出会清除临时文件；被强杀、系统杀进程等情况下可能留下应用私有 `/tmp/sui-quickrun-*` 临时目录，不在源码目录生成 class，不改 JDK 安装目录。
- 本轮只支持可读写的真实本地文件。无法保存、无真实文件路径的标签会报错，不静默运行旧内容。尚未实现远程文档/SAF-only 文档复制运行。
- 无 main 方法等入口问题在预编译通过后报告为运行失败；本轮不做工程构建、依赖下载或复杂 classpath 配置。
- Stop 会关闭该 QuickRun 会话；运行完成时会话保留输出。已有 Output 通道保留已收集输出；没有新增第二套终端或运行面板。
- 其他语言、调试器和 UI 改造未开始。

## 9. 用户 ARM64 平板逐步验收流程

1. 下载本次 Actions Artifact ZIP，解压并核对 APK SHA-256。保留 v0.1 安装包；不要卸载 v0.1、清数据或重新配置 Ubuntu。
2. 在系统安装器中对 `dev.suicode.ide.debug` 执行**更新**。若提示冲突/失败，停止并反馈错误，不卸载。安装后在应用信息确认版本 `1.7.7-sui.0.2`。
3. 打开原有 Ubuntu 24.04 环境，确认已有文件、工程、终端配置和 JDK 目录仍在。可用原命令 `. /opt/sui-code/java.env; java -version; javac -version` 作保留验证；QuickRun 本身不需要手工执行这些命令。
4. 新建或打开独立的 `HelloWorld.java`，不创建工程、不添加 Run Configuration。代码打印 `Hello from sui-code!`。点击编辑器使其获得焦点，按 Ctrl+F5，确认终端自动出现并输出该行及 `[QuickRun completed] exit 0`。
5. 在编辑器修改输出文本但不点保存，再按 Ctrl+F5。应执行新内容；检查磁盘文件已保存。针对不可写文件应报错，不应启动旧文件。
6. 将文件放到含中文、空格、单引号的目录中重复运行；再用合法的非 public 类/特殊文件名测试。测试带 package 的文件及从当前目录读取 `message.txt`。
7. 加入语法错误，确认显示 `[QuickRun compilation failed]`；修复后让 main 抛异常，确认堆栈及 `[QuickRun runtime failed]`，两者能区分。
8. 使用 Scanner/System.in 读取一行并打印，按 Ctrl+F5 后在 Terminal 内输入文字及回车，应显示输入结果。
9. 使用循环打印或 sleep 的长运行程序。长按 Ctrl+F5，不应产生多个 QuickRun 终端。点既有 Stop；再用关闭相关终端和明确退出应用分别测试。返回后检查没有残留 Java/子进程，已有其他工程任务不应被 QuickRun 的 Stop 杀死。
10. 在不同目录连续运行多个 Java 文件，再运行一个原有 ProjectRunner 工程，验证旧 Run/Debug 行为；再次打开 Java 环境管理器，确认原 JDK 状态及安装日志仍可访问。
11. 关闭并重新打开应用，重复 HelloWorld，确认无须手工 source，已有 Ubuntu/JDK 数据仍保留。

最终状态：**覆盖更新的包级条件通过；JDK 不迁移、不重装；Ctrl+F5 已接通真实保存—PRoot—Java—PTY 链路；交互输入和停止已实现并通过 host 测试；ARM64 平板上的更新、快捷键、交互及清理效果仍须按上述步骤确认。**
