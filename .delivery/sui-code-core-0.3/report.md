# sui-code core 0.3 集成交付报告

## 范围及依赖

本版从 v0.1 功能分支继续，保留 Java QuickRun 成果，以文件系统 → 通用执行框架 → 语言适配器 → 环境管理 → 工程兼容的依赖组织集成。未修改 main、官方上游或现有主题、图标和布局；仅新增功能入口及状态提示。没有创建 Release。

源码基准：v0.1 `5496f14a1d4a3dfa3a9c3528fbe3f8ce154e1f4d`；v0.2 `b1a98181f80cb2d5b5fe9e8c4abe94396791024b`。
独立分支：`sui-code/core-foundation`。
本轮功能提交：`0eaaff1`（统一多语言适配及 Ubuntu 环境管理）、`8e0a8c06b4ab3d5554e98b266991eceb014d0c43`（常驻文件根目录及安全保存）。APK 对应后者；之后只有报告、交付工作流和原始产物提交。

## 实现及关键文件

- `core/fs/InternalFiles.kt`、`SafeLocalFiles.kt`、`FsOperations.kt`、`FsImplementations.kt`、`Trash.kt`、`WorkspaceManager.kt`：常驻应用私有根目录复用 `filesDir/workspace/projects`，不要求 Workspace/Project 数据库记录即可创建多层文件夹、文件并编辑保存；不迁移或清空原有数据。临时同目录写入、fsync、原子替换，失败保留原内容。创建禁止覆盖、路径穿越及符号链接穿越；复制暂存后发布、删除不跟随符号链接。工程已登记的根目录及其祖先禁止通过普通文件操作重命名/剪切/删除，避免数据库路径失效。
- `feature/explorer/ExplorerFeature.kt`、`ExplorerView.kt`、`app/JCodeShell.kt`：复用原有 Explorer 和菜单，提供 Internal Files / Current Project Files；保留创建 Project、工程导航、原有导出实现及扩展机制。
- `core/distro/QuickRun.kt`、`app/JavaQuickRunController.kt`、`EditorView.kt`、`EditorPane.kt`、`EditorSaveActions.kt`：四种语言共用当前标签识别、等待保存成功、真实路径解析、重复触发门禁、PRoot 映射、PTY 会话所有权、完成事件和停止流程。类名 JavaQuickRunController 为历史名称，内部已使用语言注册表分发，没有为每种语言复制控制器。非支持文件不启动其他任务。保存失败不会运行旧内容。
- `QuickRunFileMount` 针对 PRoot 的冒号分隔规则选择无冒号祖先挂载，保留相对子路径。shell 参数统一引用，支持中文、空格、引号及冒号路径。
- `NativeLanguageAdapter`、`NativeQuickRun.kt`：Java、C、C++、Python 只提供后缀及执行脚本。Java 每次显式加载 `/opt/sui-code/java.env`，不回退其他 JDK；真实 JDK 编译检查后使用源文件启动，解析 package，不向源码目录写 class。C/C++ 临时目录编译并执行，包含目录和工作目录保持源目录；Python 保留源文件的 `__file__`、导入路径及 `__main__` 语义，语法检查后执行且不写 pycache。错误码区分准备失败 73、环境缺失 78、编译/语法错误 65、程序运行失败 70。
- `TerminalSessionManager.kt`、`GuestProcessTree.kt`、`OutputLog.kt`：复用原 PTY/PRoot，专属 QuickRun 所有权标签，直接交互输入、实时输出、停止及会话关闭清理。正常 ProjectRunner 会话没有 QuickRun 标签；原有运行配置入口保留。Ctrl+F5 从编辑器实际连接到保存、启动及终端聚焦；忽略按键重复并防止并发启动。
- `NativeEnvironment.kt`、`NativeEnvironmentManager.kt`、`SdkCatalogLoader.kt`、`NativeEnvironmentPage.kt`：复用现有 DistroService、Toolchains、Setup 终端及安装日志入口。保留 v0.1 官方 Adoptium Java 查询、校验与安装器；C/C++/Python 查询 Ubuntu 已配置的签名 APT 软件源，显示架构、已装/候选版本、来源及步骤，用户确认后固定请求包版本。不是追求上游最新 GCC/Python，而是 Ubuntu 稳定渠道。下载验证后安装，成功烟测才原子发布各语言 env。C/C++ 编译运行验证，Python 验证解释器和 venv。
- 已有可用环境优先保留。未配置 C/C++/Python env 时，只明确使用 Ubuntu 既有 `/usr/bin` 工具，不自动安装；已存在但失效的配置会报错。Java 不允许该回退。插件安装来源未扩展至第三方市场。
- 安装取消在安全阶段边界执行，不强杀 dpkg；Setup 活动期间正常关闭/应用内重启受到保护。失败可重试并配置未完成的包。没有全系统 apt upgrade 或删除 JDK，但 APT 可能安装/配置依赖包；不能承诺依赖完全不变。

## 构建与自动化验证

Gradle Debug 构建成功，使用 JDK 21、SDK 36、NDK 27.2、真实 Rust/Cargo JNI，不使用占位 native 库。构建参数：

```sh
source /workspace/setup/env.sh
export SUI_QUICKRUN_TEST_JAVA_HOME=/workspace/toolchains/temurin27/jdk-27.0.1.0.2+1
bash gradlew --no-daemon --max-workers=4 \
  -PjcodeApplicationId=dev.suicode.ide \
  -PsuiVersionCode=1070801 -PjcodeVersionName=1.7.7-sui.core.0.3 \
  :core:fs:testDebugUnitTest :core:distro:testDebugUnitTest \
  :core:design:testDebugUnitTest --tests '*ExtensionAbiSurfaceTest' \
  :app:testDebugUnitTest :app:assembleDebug
```

JUnit 合计 **84 项，通过 84、失败 0、错误 0、跳过 0**：

| 测试组 | 数量 | 覆盖 |
|---|---:|---|
| SafeLocalFilesTest | 10 | 多层中文路径、碰撞、穿越/链接、保存中断、权限、复制/删除/导入、无数据库根目录、工程保护 |
| GuestShellScriptTest | 3 | shell 参数引用 |
| JavaEnvironmentTest | 10 | 原 Java 环境安装回归 |
| JavaQuickRunTest | 16 | 真实 Java 27、保存屏障、package、错误、PTY 输入、停止长任务及子进程、重复触发 |
| NativeQuickRunTest | 11 | 真实 GCC/G++/Python、PTY、编译/运行错误、路径映射、Python 导入/main/dataclasses、无缓存 |
| NativeEnvironmentTest | 10 | 严格安装计划、模拟 apt/dpkg 失败/取消、实际编译烟测、保留旧环境 |
| ExtensionAbiSurfaceTest | 5 | 公共接口兼容 |
| InternalFilesQuickRunIntegrationTest | 1 | 无工程内部文件保存到四种真实运行时的集成链路 |
| ProjectRunnerQuickRunRegressionTest | 1 | 原工程运行配置实际执行 |
| ManagedCache/MemoryPressure/UpdateChecker | 17 | 原有稳定性回归 |

上述运行测试在 Linux x64 云端完成（Java Temurin 27、GCC/G++ 14.2、Python 3.13.5），不是 Android ARM64 实机验收。没有连接 adb 设备。Android 焦点/快捷键、UI 生命周期、实际 Ubuntu PRoot 清理和 SAF 导出仍需最终统一实机验收。

### 补充真实 Ubuntu APT 集成验证

使用官方 `ubuntu:24.04` 镜像（digest `sha256:fe81560990fae86dc4743202b2cb4d3df98d0bcf949173cecef15ce1127ef852`），实际发行版 Ubuntu 24.04.5 LTS、amd64。直接调用已编译生产代码生成的查询、安装及运行脚本，真实 APT 安装三组环境成功：GCC/G++ 13.3.0、Python 3.12.3（含 venv/pip）。三次安装验证成功；三次适配器运行成功并接收管道输入，输出 `C=42`、`CPP=input`、`PY=你好`，退出均为 0。安装前后的模拟既有 java.env 逐字节一致。此容器没有真实设备 JDK，不能据此声称设备 JDK 已验证。

首次下载因 sudo 清除代理变量而超时；为一次性测试容器显式配置受信任代理和 CA 后重跑成功，没有关闭 TLS/签名校验，没有更改应用源代码。用户设备网络并不依赖云端代理。容器已清理。以上 3 组安装及 3 组运行是额外集成检查，不计入 84 项 JUnit，也不是 Android PRoot/ARM64 实测；交互 PTY 由前述宿主测试覆盖，此容器采用管道输入。

## APK 与升级安全

- 文件：`sui-code-core-0.3-debug.apk`
- 大小：**88846380 bytes**
- SHA-256：`661bd7c90f06bf5630d0c4e73b85b378665a4baa4b51db0a7ce94af61ce9010e`
- applicationId：`dev.suicode.ide.debug`，显示名仍为 `JCode (debug)`，仅 ARM64，minSdk/targetSdk 33。
- versionCode：`1070801`，高于 v0.1 `1070799` 及 v0.2 `1070800`；versionName：`1.7.7-sui.core.0.3`。
- apksigner 验证通过；实际证书 SHA-256：`62c2da3484d337626eac6711e5ea7015913e23b8932e9f2b2bd220b843579fa0`，与原始 v0.1 APK 一致。
- 包身份、签名及版本条件允许直接覆盖升级 v0.1；云端没有执行 Android 覆盖安装，数据保留仍需最终设备验证。代码不删除或重建已有 Ubuntu/JDK，不要求重新安装 JDK，不触碰 `/opt/sui-code/java.env` 的既有有效选择。
- v0.1 APK 原始 SHA-256 `1554f7e7cb7359b4b90161d08e76bf515b0e0805adf4777bdb8851ef23f2cb06` 保持不变；其报告及原安装包不覆盖。

## 下载与交付机制

独立分支工作流 `.github/workflows/deliver-sui-code-core-0.3.yml` 上传同一份已构建签名 APK、`report.md`、`changes.patch`。工作流不重新构建，不上传私钥，不创建 Release；检查原始 SHA/大小、实际包身份、旧新版证书，然后下载 Artifact 逐字节比较三份文件。Artifact 名 `sui-code-core-0.3-integrated-debug`，保留 30 天，下载通常需登录 GitHub。运行及 Artifact 的真实地址在最终交付回复中提供，以实际成功运行生成的 ID 为准。

`changes.patch` 从 v0.1 有效提交到本轮报告/工作流提交生成，排除 `.delivery` 二进制载荷，包含 Java QuickRun 及后续通用核心改动。原始源码提交及报告可在分支浏览。

## 已知限制与最终 ARM64 验收

未声称所有 Android 行为已实机通过。统一环境管理通过统一入口协调原 Java 专用发行包安装器和 Ubuntu APT 管理器，未强行将两种不同事务合并。系统强制结束可能中断包管理或留下临时目录；下次重试尝试修复 pending dpkg，不能保证在系统强杀时无残留。文件原子替换面向应用私有本地文件系统，不支持原子替换的文件系统直接报错，不退回截断写入。SAF/外部存储保留旧机制，外部提供者行为未设备验证。当前仍是单文件运行器，不承诺完整多文件构建系统、所有语言/版本或 Android 调试器。已登记工程根目录的移动/删除须走工程管理。

最终仅安装本集成版一次，建议先正常导出/备份重要文件，**不得卸载或清除数据**：

1. 校验下载 APK SHA，覆盖安装现有 v0.1；若 Android 拒绝升级，记录错误并停止，不卸载。
2. 打开现有 Ubuntu，显式加载 `/opt/sui-code/java.env`，确认旧 JDK 路径、java/javac/jshell 与已有文件保留；重启应用再确认。
3. 不创建工程，在 Internal Files 创建 `中文 空格/多层/HelloWorld.java`，输入未保存修改后 Ctrl+F5，确认自动保存和真实输出；尝试文件名/目录包含引号、冒号。非支持标签不得触发其他任务。
4. 在终端测试 Scanner/System.in、编译错误、异常堆栈；运行长循环及创建子进程，点击停止、关闭对应终端后确认没有残留 Java 子进程。长按快捷键、连续切换文件运行并检查会话释放。
5. 在 Toolchains 查询并确认安装缺少的 GCC/G++/Python；检查来源、版本、日志、取消和失败重试，重新打开应用确认环境持久化；Java 原配置不变。
6. 分别创建 .c、.cpp、.py 文件，Ctrl+F5 测试交互输入、编译/语法错误、运行异常、工作目录/本地头文件和 Python 同目录导入；每次运行后源码目录不应出现 class、二进制或 pycache。
7. 测试根目录多层创建、重名失败、复制、重命名、删除/回收站、导入和编辑保存；确认普通操作不能移动或删除已登记工程根目录及祖先。
8. 创建/打开原有 Project，使用原 Run/Debug/运行配置、导出及扩展；切换内部根目录再运行单文件，确认工程和普通文件互不影响。外接键盘、触摸入口、终端关闭和应用后台/恢复均需实机确认。
