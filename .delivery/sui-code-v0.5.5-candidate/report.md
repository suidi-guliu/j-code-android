# sui-code v0.5.5 集中改进与验收报告

生产基线 `4a19b04ec7406f6c25683142a735a9d137403a6f`。构建源码 `a0313fde95ec1a7c31782e89fd6ee8d63790bdb4`，分支 `sui-code/v0.5.5-debug-ux`。

## APK 与扩展包

- `dev.suicode.ide.debug`、版本 `0.5.5`、versionCode `1070808`、仅 `arm64-v8a`。APK SHA-256 `d402133998806495e405acc313c9a868f6bcbbf9177d3682d1663169172a63f3`。
- `apksigner` 验证 v0.5.4、v0.5.5 和 instrumentation APK 的证书一致，证书 SHA-256 `62c2da3484d337626eac6711e5ea7015913e23b8932e9f2b2bd220b843579fa0`。
- Java、C/C++、Python IDE Kit 为 1.3.0。三份官方包与目录用原公钥验签，基础插件保持 1.1.0。中文包是同 ID 的独立 1.0.2，仍需未验证发布者确认。升级场景使用原始 1.2.0 Kit 和中文 1.0.1 记录验证数据保留。完整摘要见 `SHA256SUMS`。

## 改动、复现与证据

| 工作项 | 生产代码与复现方法 | 本地实测及日志 |
| --- | --- | --- |
| P0 调试输入 | 适配器 DAP 和用户 PTY 分通道。Python/Java 审阅 `runInTerminal` 请求，LLDB 在同一个调试器进程树内连接 PTY。准备、预检和启动共用工作目录绑定。启动失败释放 PTY，停止后保留取消记录和输出。分别调试 `input()`、`Scanner.nextLine()`、`scanf`、`getline`，在输入前后设断点，输入中文，再单步、继续、EOF、终止和重开。 | Debian 13 x64 真 PRoot、真 debugpy/JDT LS/LLDB，四组共 32 个场景通过。日志 `linux-evidence.zip/debian/debug-*`。Java EOF 的适配器报告 0，程序 PTY 退出码为 1。JDT LS 1.63.0 在 JDK 21 上出现 OSGi JavaSE 25 依赖错误，JDK 25 会话通过。 |
| C++ 跳转 | 编辑器按标识符实际起点发起定义请求，clangd 读取 CMake 编译数据库并跟踪索引状态。对 `src/main.cpp` 中 `add` 检查 `include/math.hpp`、`src/math.cpp` 的跳转，修改、重开并移除定义后重测。 | Debian 真 clangd 与生产 `LspSession` 的 20 个 URI、行、列断言通过，含中文空格路径、三个编译单元和缺定义的实际链接失败。Java `System.out.pr`、Python `pathlib.Pa` 补全及跨文件定义通过。日志 `debian/clangd/`、`java-lsp.log`、`python-lsp.log`。 |
| 任务结果 | 启动门闩和真实完成码按任务持久化，支持开始/结束时间、来源、耗时、原始输出、取消和重跑。对三语言模板分别执行 Build、Test、Run，再引入编译、断言、退出 23 的失败，测试停止和单文件输入。 | 本地 Debian 快照通过 49/49 个已取得结果的场景，完整矩阵以 Ubuntu CI 证据为准。初始宿主错误包括 PRoot 覆盖 `/workspace`、Maven DNS 和冷缓存等待超时，均保留为环境诊断。日志 `debian/tasks/`、`tasks.log`、`debian/maven-warmup.log`。 |
| 响应式工作台 | 编辑器与底部面板最大化/恢复保留分栏尺寸，长详情使用可滚动窗口，调试变量/栈区域可在底部 dock 展开。操作后切换横竖屏并使用 130% 字号查看按钮、终端、编辑器。 | APK 和 instrumentation 已编译。Android16 新装、覆盖安装、旋转、字号和键盘以本次 Actions 的 `android-evidence.zip` 为准。分屏及具体平板尺寸仍需设备确认。 |
| 终端管理 | 标签重命名、横向滚动/数量、空闲 shell 复用、结束后清理。活动终端通过进程归属及内核空闲检查保留。运行时切换面板、隐藏终端，重命名后输入和停止，检查日志及同项目无关进程。 | JVM 的空闲、活动、未知 PID 判定通过。真实工具调试终止场景中独立进程继续运行，终端输出保留。Android PTY 视图场景以 `android-evidence.zip` 为准。 |

本地 JVM 测试共 102 项，通过且无跳过。另有 94 份 API4 描述符校验和官方目录/包验签。SDK、补丁、APK 和签名证据随整合包提供。构建接入已有 Wasmtime 原生任务，防止 clean build 漏打包插件运行库。

## 逐项验收记录

| 任务书验收项 | 证据与当前状态 |
| --- | --- |
| Python 输入、继续和后续断点 | Linux 实测通过 `debug-Python` |
| Java Scanner 输入和断点 | Linux JDK 25 实测通过 `debug-Java-jdk25` |
| C/C++ scanf 或 getline 输入和断点 | Linux C 与 C++ 实测通过 `debug-C`、`debug-C_CPP` |
| DAP 和 PTY 分离 | Linux 发包数据与用户输入独立断言通过，Android 见 CI 原生 PTY 日志 |
| 调试停止清理并保留输出 | Linux 真实终止与独立进程保留断言通过，Android 见 CI |
| 正确 C++ 工程中 add 跳转 | Linux 20 个精确位置断言通过 |
| 缺少定义、LSP 未就绪和索引状态 | 缺定义真实链接失败已验证。Android 的状态提示需结合 CI UI 和平板复核 |
| 三语言任务成功和退出 0 | 以 Ubuntu CI 的 `task-results.json` 与原始日志为准 |
| 三语言任务非零退出和取消 | 以 Ubuntu CI 的 49 个场景与持久化历史为准 |
| 编辑器和面板最大化及恢复 | 以 Android CI `workbench-maximize-and-target` 为准。真机调高和分屏待测 |
| 长 Kit、日志和依赖详情 | 生产响应式窗口已编译，端到端可用高度和各操作区仍需平板复核 |
| 终端标签和活动进程保护 | JVM 内核检测通过，Android 重命名与隐藏见 CI。平板上的标签批量整理和溢出待测 |
| Python 补全和三语言断点/单步/变量 | Linux 真实补全及真实调试通过 |
| 中文包、数据、旧 Kit、工具与原图标 | APK 身份签名同 v0.5.4，扩展包签名与独立中文包已核验。其余按 Android 升级证据与平板实测 |
| Android 16 启动、升级、键盘与布局 | 以 Android CI 的 fresh 和 upgrade 两份结果为准。实体平板验收待测 |

## 验收边界

- Linux PRoot 日志记录宿主架构和工具版本。本地 Ubuntu 镜像的 APT 出站连接失败，Debian 复跑证据明确标注 Debian。Ubuntu 24.04 独立运行结果由 GitHub Actions 生成。
- Android16 CI 使用 x86_64 AVD 和 ARM64 native bridge。其调试场景验证真实 Android PTY 和工作台 UI，debugpy 程序通过 adb reverse 连接并在 runner 执行。升级 fixture 可验证旧扩展包及项目和配置文件字节保留，已安装工具及 Ubuntu 运行时须在平板实机上确认。
- 联想 ARM64 平板未连接本轮环境。物理键盘、具体触屏输入法、长文本粘贴、分屏、旧 Kit 及实际环境保留等实机验收仍待执行。
