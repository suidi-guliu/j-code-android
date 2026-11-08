# sui-code v0.5 整合开发、测试与交付报告

基线：`sui-code/final-product` / `50aec6ab2f9dbde20982c4b9247238f88e7b17b0`。开发分支：`sui-code/v0.5-workbench-reliability`。main、官方上游和已验证v0.4交付文件保持不变；不创建Release。

## 1. 实现功能

**Languages ANR**：代码定位到 MainViewModel 从主线程调用目录检查，后者持锁同步等待 PRoot 进程，并可能执行 apt update。现在目录加载/探测在IO，浏览不隐式联网；单飞、60秒缓存、批量探测、超时强制终止、取消与未知结果保护保留既有状态。显式更新检查放在高级入口。Debug 包提供 SuiPerformance 主线程堆栈/页面Trace及只读logcat脚本。没有设备日志时不把代码证据说成实机追踪。

**IDE Kit依赖**：Core API3新增Kit专属setup组件、完整Schema和示例。一次审阅依赖包、状态、版本、来源、架构、权限、摘要及可获得下载大小；复用运行环境，通过既有Setup PTY部署缺失项。Java包含JDT LS、Java Debug、Maven；C/C++包含clangd、LLDB DAP、CMake/Ninja/jq；Python包含pylsp/debugpy/测试工具。提供进度、日志、安全边界取消、重新审阅/重试和Ready/Partially Ready。已成功安装的共享工具保留，包归档原子更新并能回滚。

**LSP/DAP**：正确编码file URI且不把加号解码成空格；请求前同步最新缓冲区，保留服务器filterText，取消传到服务器。只有文字修改或Ctrl+Space触发补全；锚点/快照保护拒绝过期结果。冷索引阶段最多三次200ms后台重试，不增加正常请求超时。Kit声明的虚拟JDK源码以只读缓存打开。Java启动目标精确匹配优先、唯一工程入口回退、空索引有界重试；多入口给出候选。DAP关闭先发送并等待terminateDebuggee再关传输；清理观察任务和超时请求。LSP启动/关闭竞争会回收迟到的进程传输。

**C/C++路径**：Project任务和LSP/DAP使用稳定guest目录，不再把/sui-quickrun写进持久CMake缓存。新版Kit为旧build/debug缓存保留时间戳备份再重新生成，clangd指向真实compile_commands，调试使用Debug信息并显示断点验证结果。新模板将入口展开为可逐行调试的源码。

**键盘/终端**：Ctrl+F5连接Activity普通及shortcut分发、Linux F5扫描码和去重门禁；编辑器Ctrl+Enter兼容入口不拦截终端输入。QuickRun Stop只结束任务子树，保留shell与输出。su改用--session-command保留控制终端。剪贴板/IME输入清理重复bracketed paste包装。

**Files/Project/数据**：常驻Internal Files不依赖Workspace/Project；增加递归名称搜索、排序、多选复制/剪切/粘贴/移动、属性和批量Trash，保留创建、导入、导出和恢复。冲突拒绝覆盖且保留剩余剪贴板；文件事务与保存确认同步打开标签/断点，删除保留只读缓冲区。Project有最近列表、切换/关闭、Unicode重命名、导出、移除登记和确认后删除到Trash；非工程统计N/A。DocumentsProvider保留授权身份，限制路径/名称/链接和工程根操作。Trash先持久化恢复意图再移动，元数据后续写入失败仍可恢复，不自动清理含用户载荷的损坏条目。安装期间保护Ubuntu切换、重启和Setup会话。

**工作台**：保留主题/配色/图标风格，实质重组为Files/Find/Languages/Packages/Settings导航轨、相邻Run file/Debug/Overview、独立Project Build/Test/Run Project，以及原有可调高度底部Terminal/Output/Issues/Debug/Tasks。Packages内部分类替代重复入口；旧.jcode/run.yaml在Advanced tasks继续可访问。

升级后在Languages审阅三套官方Kit **1.1.0** 更新计划。新依赖、虚拟源码、项目标记和CMake迁移元数据来自新版签名Kit；不会静默执行新摘要的脚本。既有1.0.0归档和环境保留，Basic运行不要求卸载或重装JDK。

## 2. 关键文件

- JCodeShell.kt、workbench/WorkbenchChromeBars.kt、NativeCommandStrip.kt、WorkbenchPanels.kt：新工作台和Project入口。
- nativehost/NativeKitSetup.kt、NativePackageReview.kt、NativeLanguageCenter.kt、NativeLanguageServices.kt、NativePackageHost.kt：计划、权限、安装和服务注册。
- core/distro/DistroService.kt、CatalogProbeBatch.kt、ProotManager.kt、DistroModels.kt：异步检查、路径和PTY。
- core/lsp/LspSession.kt、app/lsp/LspController.kt、editor/LspCompletions.kt、core/editor/EditorView.kt、feature/editor-pane/EditorPane.kt：同步、触发、URI与取消。
- core/debug/DebugSession.kt、app/debug/DebugController.kt、NativeDebugPlans.kt、core/native-packages/DebugWorkflow.kt：调试与终止。
- core/fs、feature/explorer、provider/ProjectsDocumentsProvider.kt：文件操作、安全与恢复。
- native-library/src、docs/specifications、tools/sui_packages、tools/device：正式包、SDK、规范及可执行验证。

完整修改清单和补丁随最终交付。

## 3. 分支与提交

分支`sui-code/v0.5-workbench-reliability`；可靠性阶段提交`6718501`；整合功能提交`5806a99`。最终源码归档、APK构建和交付提交的对应关系记入apk-metadata.json及Git提交记录。

## 4. 构建

JDK21、Android SDK36、Gradle wrapper、NDK27.2及既有ARM64原生工具链。最终Debug构建包含全部原生模块。Standalone SDK已独立编译，12个API源文件与生产源码逐字节一致。`:app:assembleDebug`已成功，耗时2分12秒，697项任务（31执行、666复用）。APK为86,956,384字节。完整build.log、签名/badging及apk-metadata.json随交付；不以编译成功代替实机验收。

## 5. 自动化和集成测试

云端JVM：**233例，0失败**。

|模块|数量|
|---|---:|
|core/fs|26|
|core/distro|66|
|core/native-packages|53|
|core/editor / core/term|3 / 4|
|core/design|57|
|core/lsp / core/debug|1 / 1|
|app|22|

覆盖无Workspace文件、路径/符号链接/覆盖保护、搜索、Trash写入故障、包权限/回滚、真实单文件执行、保存门禁、重复键、补全过滤/触发、LSP启动取消和DAP先终止再关闭等。

以下为**Ubuntu24.04 x64容器**真实测试，不是Android/ARM64：

|流程|结果|
|---|---|
|三语言Project Build/Test/Run|9条工作流通过，真实测试及Hello42输出|
|Java System.out.pr/JDK定义|JDT LS真实成员候选及classFileContents通过|
|Java Maven项目|辅助文件上下文发现唯一入口、中文/空格/+/hash路径的项目定义跳转、真实第5行断点→第6行单步→value41→继续输出42→结束通过；第二次会话terminateDebuggee后/proc确认子进程消失，语言服务器仍存活|
|CMake/clangd/LLDB|重现旧临时别名缓存，迁移保留备份、真实CTest、头文件定义和真实断点/单步/继续/终止通过，项目math.hpp的add补全通过；一次x64冷定义查询约896ms|
|Python pylsp/debugpy|os.path补全及项目断点/单步/继续通过；最终一次x64测量冷约1017ms、稳定约113ms（非平板性能承诺）|
|PTY|旧su -c复现两条job-control警告且tcgetpgrp失败；新--session-command无警告，前台进程组/交互输入通过|
|官方Java工具|真实下载并校验JDT LS、Java Debug、Maven；Maven3.9.11在保留的JDK27运行|
|规范/SDK|88个声明通过Schema/内嵌快照；独立生产验证器检查17个包，非法插件被E_BOUNDARY拒绝|

Java环境文件在工程流程前后逐字节一致。日志/XML/脚本随交付；故障注入不冒充实机掉电测试。

## 6. 安装身份、签名与数据

实际核验身份`dev.suicode.ide.debug`，versionName`0.5.0`，versionCode`1070803`（v0.4为1070802），ARM64。apksigner已实际比较v0.1、v0.4与v0.5，三者证书相同：`62c2da3484d337626eac6711e5ea7015913e23b8932e9f2b2bd220b843579fa0`。

不清除数据，不重装Ubuntu，不改变既有JDK/工具链/数据库/配置身份。私钥不纳入源码、SDK或Artifact。元数据兼容检查不等于实机覆盖安装已经通过。

## 7. 下载、哈希及公共规范

安装包：`sui-code-v0.5-debug.apk`，**86,956,384字节**。SHA-256：

```text
d8c968ff33288a50d1ebdbb4b85ce7da5d2d4ce2e5cf48bc8979b864d9d1be8a
```

[整合交付工作流及Artifact入口](https://github.com/suidi-guliu/j-code-android/actions/workflows/deliver-sui-code-v0.5.yml)；选择成功运行，在Artifacts下载`sui-code-v0.5-integrated`。工作流校验预构建APK、不重新签名，上传后下载回验全部字节和SHA；运行页Summary提供该次Artifact直达入口。GitHub Artifact需要登录，保留30天；并非永久Release。最终完成状态及具体运行/Artifact地址以本报告末尾的公开交付回执及最终消息为准。

[独立云端回归运行](https://github.com/suidi-guliu/j-code-android/actions/runs/42486471448)。完整包同时包含源码、SDK、6个签名官方包、两个规范、补丁、文件清单、测试证据和构建日志。

- [扩展开发规范](https://github.com/suidi-guliu/j-code-android/blob/sui-code/v0.5-workbench-reliability/docs/specifications/SUIC-SUIK-Development-Specification.md)
- [UI设计规范](https://github.com/suidi-guliu/j-code-android/blob/sui-code/v0.5-workbench-reliability/docs/specifications/SuiCode-UI-Design-Specification.md)

## 8. 尚未验证与限制

- 没有连接ARM64设备，不能获取该设备logcat/Perfetto或执行覆盖安装。**不能宣称Languages实机ANR已经消失，或Java/C++断点实机已命中。** 代码阻塞链、回收链已修复并有测试/埋点，设备结论需最后一次整包验收。
- UI触控、横竖屏、外接Fn键、Stop后屏幕输出、OEM/IME粘贴、PRoot ptrace/进程清理及真实升级数据保留均待设备确认。
- Ready表示声明依赖预检满足；首次工程索引与服务连接单独验证。来源要求版本与存在性检查不混称为独立测得的已安装版本。
- 更新Kit需确认统一计划；缺少未知本地依赖需先导入。取消等待包事务安全边界，已安装共享工具保留，不自动卸载/回退语言环境。
- 外部SAF受授权URI/提供器约束；不声称能管理整个Android系统。打开标签移动同步覆盖应用及其DocumentsProvider操作，不能保证推断其他进程离线改名后的历史路径。
- 未列出的第三方LSP/DAP版本及高级能力不能据此宣布已验收。APK不重新封装JDK/Python/GCC发行版。

## 9. ARM64平板一次性验收

1. 备份工程/源码和导出，记录java.env/JDK/工具版本；覆盖安装或adb install -r，不卸载、不清数据，签名冲突立即停止。
2. 验证原Ubuntu、JDK、GCC/G++、Python、包、设置和文件仍在；Languages审阅三套官方1.1.0 Kit计划，确认已装组件复用、缺失工具统一部署，无须逐项去Servers。
3. 冷启动/重复进入Languages、断网/恢复网络、进出与取消，观察响应；用tools/device/collect-workbench-logcat.sh采集。脚本不清日志、不强停、不重装。
4. 无Project创建多层中文/空格目录及源码，编辑保存/重开；验证多选复制/移动、冲突、重命名、搜索、排序、属性、Trash恢复、导出和打开标签同步。
5. Ctrl+F5/Ctrl+Enter、长按去重、自动保存、缺环境、编译错误、异常、中文/Scanner输入、连续文件和长程序Stop；终端/输出应保留。
6. 三语言Project完整Build/Test/Run；Java System.out.pr与项目/JDK定义；Java Maven及C++可执行行真实断点/单步/继续/终止；C++math.hpp/add及真实compile_commands；Python方向键不自动弹窗并测冷/热延迟。
7. Run/Debug/Overview相邻且区分文件/工程上下文；Packages单入口、Languages分层、底部终端调高、横竖屏/鼠标/键盘；旧.jcode/run.yaml在Advanced tasks仍能运行/编辑。
8. 关闭会话/应用后检查子进程；取消安装、网络重试、错误日志及恢复入口；最后复查原数据与工具，设备结果单独记录。
