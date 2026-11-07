# sui-code v0.1：Java 环境管理器首轮开发报告

日期：2026-11-07。状态：可构建的首个功能切片，尚未完成 ARM64 平板验收。

## 基线与开发约束

- Fork：`https://github.com/suidi-guliu/j-code-android.git`；基线 `e1a14ba2199af6ed3c13c185dcca197cb70fe8e2`。开始时工作区干净，云端恢复分支为 `work`。
- 独立分支：`sui-code/v0.1-java-environment`。只作本地提交；未修改或推送官方上游，未创建正式 Release，未发布环境配置。
- 源码提交：`28e511b`（GA 解析、安装脚本和测试）、`0c63847`（Toolchains/PTY 集成及额外失败路径测试）。
- 本轮保留 JCode 名称、图标、主题和布局；新增页面复用原有 Toolchains 详情槽位，不做第四阶段 UI 改造。
- 构建通过 `-PjcodeApplicationId=dev.suicode.ide` 隔离身份，产物包名为 `dev.suicode.ide.debug`，显示名为 `JCode (debug)`。manifest 验证 minSdk/targetSdk 为 33、仅 arm64-v8a。
- `v0.1` 是 sui-code 本轮里程碑；APK 的 versionName/versionCode 暂沿用上游 `1.7.7 / 1070799`，没有伪装成正式发布版本。

## 源码分析与最小设计

| 已有机制 | 实际实现与本轮复用 |
| --- | --- |
| Toolchains | `SdkCatalogLoader` 从 assets YAML 加载 SDK 项，`DistroService` 维护安装状态，`SdkManagerFeature.DetailPage` 提供详情。本轮新增 `sui-java-ga`，不破坏依赖旧 `jdk` ID 的现有流程。旧 apt Java 入口仍保留。 |
| 原有 Java 安装 | YAML 中 `jdk` 使用 apt-cache/apt-get、update-alternatives 和 `/etc/profile.d/jcode-java.sh`，还关联 ktlint；它不等同于动态上游 GA 发现，也会切换系统默认 JDK。本轮独立并存安装避免这两个行为。 |
| Ubuntu / PRoot / PTY | `DistroService` 与 `ProotManager` 管理 guest；`TerminalSessionManager` 创建真实 PTY，PRoot 使用 `--kill-on-exit`；`SetupTerminalRunner` 暂存脚本并用 OSC 7713 报告退出状态。本轮直接复用，不创建第二套终端。 |
| 环境变量 | 用户登录 shell 通过 `su -` 和 profile 取环境，非交互命令不保证读取全部 profile。本轮写 `/opt/sui-code/java.env`，原生 Java 适配器显式 source；只有不存在可用系统 JDK 时才添加专属登录 profile，不替换原有 JDK/default/profile 文件。 |
| 扩展管理 | 官方市场基址在 `ExtensionInstaller.BASE_URL`，现有代码还支持通用 VSIX/来源功能。本轮 Java 管理器完全不调用这些入口，不下载语言插件，不引入 VS Code Marketplace、Open VSX 或第三方替代源；本轮页面说明该流程未启用插件，并指向原有 Extensions 中的官方 Java Dev Pack；未宣称设备兼容性已经验证。现有全局扩展来源策略尚未改造，不能据此声称整个应用已强制拦截所有第三方导入。 |
| Build & Run | `ProjectRunner` 通过项目探测及 `.jcode/run.yaml` 生成 RunPlan，由现有终端运行并保存项目改动。本轮不重复建设此系统；新增 `NativeLanguageAdapter` 和 Java 适配器契约，后续可接入当前文件 QuickRun 分发。 |

交互路径：Toolchains → **Java (latest GA · Temurin)** → Query latest GA → 查看版本/来源/文件/URL/SHA-256/安装目录/步骤 → 确认 → 自动打开 Ubuntu Setup 终端。

元数据来自 Eclipse Adoptium API：先查询 `/v3/info/available_releases` 的 `most_recent_feature_release`，再请求 `/v3/assets/feature_releases/{feature}/ga`，过滤 Linux、aarch64、JDK、HotSpot、Eclipse。EA、错误主版本/架构、缺失校验值、非官方归档地址均失败关闭，不退回第三方源。确认安装固定到已展示的版本及摘要，不在执行时偷偷重新解析 latest。

API 字段已对照 Adoptium 官方 API 源码模型和路由核查：仓库 `adoptium/api.adoptium.net`，核查时提交 `14e1ee64fb8aec1c5aecb4aeedefeaa8dd52329c`；这属于契约核查，不是实时 API 请求通过的证据。

## 已实现

- 根据当前 guest 架构解析 aarch64；安装脚本再次检查 `uname -m`，拒绝环境变化后的架构错配。本轮页面仅允许 ARM64。
- 查询/失败重试、完整安装计划、独立的用户确认；确认取消不触发安装。
- 在真实 Setup PTY 安装；没有静默 exec 回退。等待 PTY 后再次核对 distro ID，避免复用其他发行版的 Setup 会话。
- HTTPS 下载，严格使用元数据 SHA-256，校验通过后才解包；失败不降低 TLS/摘要要求。
- 在 `/opt/sui-code/jdks` 私有临时目录验证，再移动到版本化目录；已存在的目的目录直接保留并报错，不覆盖。
- 验证 `java -version`、`javac -version`、`jshell --version`，实际执行 jshell 表达式、编译并运行 HelloWorld，检查精确输出。
- 写入独立 JAVA_HOME/PATH 配置；已有可运行系统 java/javac 时保持系统默认不变。使用新 JDK可在终端执行 `. /opt/sui-code/java.env`；适配器会自动读取该文件。
- 安装日志在 PTY 与 `/opt/sui-code/logs/java-<timestamp>.log` 保留，详情页轮询最后 64 KiB；重试产生新日志，不丢弃旧文件。
- 取消/超时接入现有 PTY 关闭及会话通知清理；安装脚本用 flock 防止并发写入，支持终止后重新取锁。退出码经 Bash pipefail/tee 保留。
- Java 原生适配器识别 `.java`，使用已管理 JDK的 source-file launch 命令，正确引用含空格/单引号的路径。它尚未连接编辑器事件。

## 验证与未完成项

| 检查 | 结果与边界 |
| --- | --- |
| 云端恢复的构建环境 | JDK 21、Gradle 8.14.3、SDK 36、NDK 27.2.12479018、CMake 3.30.5、Rust 1.99.1/cargo-ndk 4.1.2 可用；实际构建验证通过。 |
| 原生 ARM64 库 | 两个 Cargo JNI 构建成功；APK 包含 `libripgrep_ffi.so` 与 `libjcode_wasm.so`。 |
| `:app:assembleDebug` | 通过，独立应用 ID；只构建 Debug。 |
| JavaEnvironmentTest | 10 项通过，0 失败/错误/跳过。覆盖 GA/EA/主版本/架构、来源与摘要验证、QuickRun 引号、安装成功、已有目录保护、校验失败后重试、JDK 验证失败、终止后重试、架构变化、下载失败时保护已选环境。安装器契约测试使用模拟归档/工具，不能替代设备验证。 |
| GuestShellScriptTest | 本次执行 3 项通过，0 失败/错误/跳过。 |
| EffectiveWordWrapTest | 2 项单独强制重跑通过，0 失败/错误/跳过；不是把旧快照的 UP-TO-DATE 当作本轮执行。 |
| 真实 JDK 安装器主机冒烟 | 用生产脚本在独立工作区路径、Linux x64 上下载官方 Temurin 21.0.9+10；SHA-256 与官方 `.sha256.txt` 一致，java/javac/jshell、HelloWorld 全部通过，原系统 JDK保留。它不是最新 GA、不是 ARM64、不是 PRoot 实测。 |
| APK | apksigner 校验通过（v2）；aapt 校验独立包名、显示名及 ARM64；产物摘要见下文。 |
| 实时最新 GA 查询 | **未通过云端实测**：云网络代理对 `api.adoptium.net` CONNECT 返回 403。没有更换第三方查询源或硬编码“最新”。如需在云端复测，需在环境设置允许该域名后重试；本轮未修改或发布环境配置。平板端使用自身网络，仍须验收。 |
| 设备 UI / Ubuntu PRoot 安装 / 取消 | **未执行**：`adb devices` 没有设备。尚未验证平板端终端自动聚焦、生命周期、进程终止和 ARM64 JDK/JDI 行为。强杀可能留下私有 staging 目录，后续重试不会覆盖它或已装 JDK；设备验收需检查并整理残留。 |
| QuickRun | **未接入**：Ctrl+F5、当前文件选择、自动保存、统一输入/输出及停止任务尚未实现。只有原生适配器接口/Java 命令构造；C/C++、Python 等适配器未实现。 |
| 语言插件 | 官方市场索引查询通过，列出 `Java Dev Pack / jcode.lang.java 0.2.3`，指向官方 `dist_v2/jcode.lang.java-0.2.3.jext`，metadata min/target JCode 1.3.5。**本轮未安装、未验证平板兼容性**，管理流程未启用插件；没有以第三方市场替代。 |
| 工程调试、平板美化 | 未开始，遵守阶段顺序。 |

现有 Gradle/Kotlin 弃用警告仍存在；Android metrics 无法写只读用户目录的警告不影响本轮构建。主机 jshell 用户首选项目录也有只读警告，表达式输出及退出状态已单独验证。

## 复现构建

```bash
cd /workspace/j-code-android
source /workspace/setup/env.sh
bash gradlew --no-daemon --max-workers=4 \
  :native:ripgrep-ffi:cargoBuildDebugJniLibs \
  :native:wasmtime-ffi:cargoBuildDebugJniLibs
bash gradlew --no-daemon --max-workers=4 \
  -PjcodeApplicationId=dev.suicode.ide \
  :app:assembleDebug :core:distro:testDebugUnitTest
bash gradlew --no-daemon --max-workers=4 \
  :core:config:testDebugUnitTest --rerun-tasks
```

Java 安装前 guest 需已有 `bash/curl/ca-certificates/sha256sum/tar/mktemp/flock`。缺失时日志明确报错；本轮不擅自修订 apt 源或安装其他插件。

## 产物

- 路径：`/workspace/artifacts/sui-code-v0.1/sui-code-v0.1-debug.apk`
- SHA-256：`1554f7e7cb7359b4b90161d08e76bf515b0e0805adf4777bdb8851ef23f2cb06`
- 目录中一并交付签名/manifest 检查、构建与主机验证日志、JUnit XML、SHA256SUMS 和设备验收清单。
- APK 使用本机 Debug 签名，仅用于测试；没有上传商店或创建 Release。

## 修改文件清单

| 文件 | 修改目的 |
| --- | --- |
| `core/distro/src/main/java/dev/blamspot/jcode/core/distro/JavaEnvironment.kt` | GA 元数据、官方来源限制、JavaRelease、原生适配器接口/Java 命令。 |
| `core/distro/src/main/java/dev/blamspot/jcode/core/distro/JavaInstallScript.kt` | 下载、校验、并存安装、验证和环境配置。 |
| `core/distro/src/main/java/dev/blamspot/jcode/core/distro/JavaEnvironmentManager.kt` | 查询/确认安装生命周期、任务取消、日志、发行版绑定。 |
| `core/distro/src/main/java/dev/blamspot/jcode/core/distro/DistroService.kt` | 原有 distro 服务提供 Java 管理器及 PTY 回调。 |
| `core/distro/src/main/java/dev/blamspot/jcode/core/distro/SdkCatalogLoader.kt` | 新增独立 Java GA Toolchains 项。 |
| `core/distro/src/test/java/dev/blamspot/jcode/core/distro/JavaEnvironmentTest.kt` | 10 项行为/失败路径测试。 |
| `feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt` | 安装计划、确认、重试、取消和日志入口。 |
| `feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/SdkManagerFeature.kt` | 复用既有详情路由。 |
| `feature/sdk-manager/build.gradle.kts` | 使用已有版本目录中的协程依赖。 |
| `app/src/main/java/dev/blamspot/jcode/MainViewModel.kt` | 将 Java 安装接入既有 Setup runner。 |
| `app/src/main/java/dev/blamspot/jcode/workbench/SetupTerminalRunner.kt` | 显式聚焦、任务取消/超时、当前 distro 校验。 |
| `app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchModel.kt` | 传递 Setup 聚焦事件。 |
| `app/src/main/java/dev/blamspot/jcode/JCodeShell.kt` | 收到用户确认后的事件才打开既有终端抽屉。 |
| `docs/sui-code-v0.1.md` | 本设计、结果、限制及文件清单。 |

下一步应先完成目标平板的 Ubuntu/ARM64 安装与取消验收，再连接 Java 当前文件 QuickRun；不提前进入整体 UI 重构。
