# sui-code v0.5.4 综合修复与扩展管理交付报告

本报告的初始 Android 状态为 **Android16 CI 通过**。只有文末存在成功工作流的实际验收回执，才表示 Android16 模拟器已通过。真实联想 ARM64 平板尚未进行 v0.5.4 验收，不能把以下 Ubuntu x64 或 Android x86_64/ARM64 native bridge 测试称为真实平板测试。

## 1. 实现内容及根因

工程任务原先把 Android 工程转换成 `/workspace/...`，但创建运行终端时可能传入空 binds；即使文件存在于 Android，任务也看不到源码目录。另外，C++ 工程脚本的相邻迁移资源以前没有完整暂存。现在由 `NativeTaskPaths`、`RuntimeMounts` 统一提供源码、工作目录、默认和显式挂载，按父目录到子目录排序；启动终端前实际在 PRoot 内验证目录及源码访问。LSP 使用同一项目根和路径映射。普通 Internal Files 文件不要求登记 Project。

中文和空格保留。对会被 CMake 列表、JVM classpath 或 PRoot bind 分隔符解释的路径字符，使用稳定的 `/sui-projects/<hash>` 挂载别名及宿主管理的符号链接，不移动或重命名用户目录。旧 `/workspace` 映射继续存在。实际测试包含 `中文 空格 + dollar$ quote' semi; colon:`、多层目录及重复运行。

`c.env` 修复改成同目录临时文件和 `mv -fT`，不再硬链接临时文件；识别有效配置、失效链接、缺失、不可读文件、目录与特殊文件。保留有效自定义选择，备份后提交，后续失败则回滚。已有 GCC/G++ 实际可编译时不执行 apt。Languages 增加离线配置修复：缺少实际工具时明确拒绝安装，不会暗中下载。

Java/C++ LSP 现在使用真实项目挂载、稳定根、正确的 URI 映射和顺序化 didOpen/didChange。JDT LS 的检测不再要求 Maven/Java Debug 同时存在；配置缓存区分版本与架构。clangd 的初始化 include 路径使用当前工作区；编译数据库保留真实工作目录。协议 EOF、错误帧和请求失败显示 Error，提供有界日志及安全重启；调试未结束时禁止重启语言服务器。中文包启停不再无故重启语言服务；语言 Kit 的更新也只重新评估发生变化的包及其依赖者，避免关闭其他包承载的 Java 调试连接。DAP 卡片状态来自对应包拥有的真实调试会话。

Languages 只管理 JDK/GCC/G++/Python，复用原环境管理器；不加载 Kit 组件探测，不展示插件、IDE 组件安装计划或 Kit 日志。Packages 统一 Plugin Library / IDE Kit Library / Installed，所有 IDE 依赖安装、修复、更新、逐组件选择和日志归属这里。运行环境在 Kit 计划内只读检测，不能重复安装。已有可用工具不会因缺少 Ubuntu 元包记录而被误判为未安装。

新的 Core API4 `card.version=1` 声明经过生产验证器校验后动态渲染。官方和第三方使用同一宿主卡片；语言 Kit 固定保留全部能力槽，未声明的槽置灰；其他类别仅允许受限文本、HTTPS 链接和真实已声明命令。类别不授予权限、不决定信任、不执行 JS/DEX/HTML。旧包无新字段时安全回退通用卡片。

简体中文 1.0.1 是独立的 localization 卡片，目录中单独发现，保留原包 ID、旧选择及所有 732 条原译文，扩展至 **783 条**。目录签名只证明审核过的 URL/大小/hash；此中文包仍为未签名发布者，必须确认来源及 `ui.localization`，没有伪造签名或修改信任根。它无需 Ubuntu/JDK/Python。

## 2. 强制验收逐项结果

“云端通过”严格指所列测试平台；UI 项目的 Android CI 状态必须结合文末回执阅读。每项的 ARM64 真机结果仍待用户集中验收。

| 验收项 | 实现位置 | 测试方法及当前实际结果 |
|---|---|---|
| Java Build/Test/Run 正常 | `JavaQuickRunController.kt`、`RuntimePaths.kt`、Java `project-task.sh` | Ubuntu24.04 x64 实际 Maven Build/Test/Run、重复运行、两类路径通过；日志 `task-results.json` |
| C/C++ Build/Test/Run 正常 | 共用调度器、C_CPP `project-task.sh`、完整资源暂存 | 实际 CMake Configure/Build、CTest、运行及重复运行通过；C 和 C++ 单文件均通过 |
| Python Build/Test/Run 正常 | 共用调度器、Python `project-task.sh` | 实际 Python 工程 Build/Test/Run、单文件及重复执行通过 |
| Project 路径挂载修复 | `RuntimePaths.kt`、`ProjectPathRegistry.kt`、`ProotManager.kt`、`DistroService.kt` | 7 个路径单元测试；空挂载失败复现、正确绑定恢复、特殊路径真实执行通过 |
| c.env 异常安全修复 | `NativeEnvironment.kt`、`NativeEnvironmentManager.kt` | 6 个实际现代 Termux PRoot `--link2symlink` 场景通过：正常、缺失、失效链接、不可读、目录、后续提交失败回滚 |
| GCC/G++ 状态检测正确 | `NativeLanguageCenter.kt`、`NativeKitSetup.verify` | 已安装编译器实际编译/执行，修复均未调用 apt；配置异常与工具存在分别显示；Android UI Android16 CI 通过（ARM64 真机待验） |
| Java 智能补全正常 | `LspController.kt`、`LspSession.kt`、Java LSP 适配器 | 实际 JDT LS：System.out、println、ArrayList、自定义类成员、工程跳转、保存/关闭/重开通过；另验证没有 Java Debug 时 JDT 单独可用 |
| C/C++ 智能补全正常 | 同一控制器、C_CPP 适配器与编译数据库迁移 | 实际 clangd：C printf、STL cout、add、自定义头文件/跨文件跳转、特殊路径、重开通过；数据库 directory 均实际可访问，无临时 QuickRun 路径污染 |
| Python 补全回归正常 | 同一控制器、原 pylsp 适配器 | 实际 pylsp Path、项目符号跳转、文档修改/重开通过；增加可选 autopep8 后端后真实格式化编辑响应也通过；未改动原补全自动触发规则 |
| Debug 断点回归正常 | 原 `NativeDebugPlans` / `DebugController`，共用路径映射 | Java Debug、LLDB18、debugpy 实际断点、栈/变量、单步、继续、输出及结束通过；Linux x64 直接协议测试，不冒充 Android PRoot 调试 |
| Languages 仅管理官方语言环境 | `NativeLanguageCenter.kt`、原 JDK/native 环境页面 | 源码无 NativePackageHost/Kit 探测或 Kit 日志；独立搜索和环境详情接通，Android UI Android16 CI 通过（ARM64 真机待验） |
| IDE Kit 组件管理集中到 Packages | `NativePackageCenter.kt`、`NativePackageCard.kt`、`NativeKitSetup.kt` | 旧 LSP/DAP 管理动作转到 Packages；实际安装资源仅经统一计划执行，Android UI Android16 CI 通过（ARM64 真机待验） |
| IDE Kit 支持逐组件可选安装 | `KitRequirements.kt`、`NativePackageReview.kt`、安装资源 | DAG 闭包/环/非法 ID 测试通过；真实 JDT-only、Java Debug-only、Maven-only 部署通过；遗漏组件未被安装；强制修复提交失败回滚通过 |
| 安装及检测日志不跨页面重复 | `InstallationLogArchive.kt`、`InstallationLogHistory.kt` | 环境/IDE 两种归属索引；重启重读、分类隔离、65536 字节有界读取测试通过；历史未索引 Ubuntu 日志保留 |
| 中文 .suik 在 IDE Kit Library 有独立卡片 | 已签名官方目录、独立 localization Manifest、统一卡片 | 生产目录/hash/包验证通过；无运行环境计划、未签名确认、启停卸载回退已有单元测试；Android UI Android16 CI 通过（ARM64 真机待验） |
| 第三方 .suic/.suik 有统一规格卡片 | `PackageCards.kt`、`NativePackageCard.kt` | 真正模板和打包文件通过生产 SDK；Android 普通插件/普通 Kit 动态卡片测试已编译，Android16 CI 通过（ARM64 真机待验） |
| 第三方语言 Kit 统一字段，未声明项灰色 | `LanguageKitSlot`、统一 renderer | 校验不存在/类型不符引用并拒绝；Python 第三方样例只声明 Runtime/LSP，其他槽保留为 Not provided；Android UI Android16 CI 通过（ARM64 真机待验） |
| 非语言类别支持受限自定义字段 | `PackageCards.kt`、`third-party-utility-card` | 仅 Text/HTTPS；拒绝 HTML/JS、不安全链接和虚构命令；真实项目统计命令引用通过验证 |
| .suik 卡片类别声明及 Schema 完成 | `card.schema.json`、SUIC/SUIK Manifest Schema、生产 verifier | Core4 最低版本、旧包默认、职责边界、非法类别/恶意字段测试通过；标签不影响权限与签名 |
| 官方规范和 SDK 同步更新 | 两个规范0.4.2、14个实际生产 API 源文件、三个新模板 | Schema/嵌入快照/94 个描述符检查通过；独立 JVM SDK 编译成功；有效示例通过，非法 .suic 被 E_BOUNDARY 拒绝 |
| Languages 搜索正常 | `NativeLanguageCenter.kt` | 中英名称/编译器/已安装/版本即时本地筛选、清除及空状态；生产 Compose UI 测试Android16 CI 通过（ARM64 真机待验） |
| Packages 搜索正常 | `NativePackageCenter.kt` | 官方/第三方/类型/发布者/版本/状态/中文资源本地筛选、跨标签保留、清除及空状态；生产 Compose UI 测试Android16 CI 通过（ARM64 真机待验） |
| 中文切换及重启持久化正常 | 原 `NativeLocalization`、中文1.0.1、原首选项 ID | 保留732旧值，783键完整校验；单元启停/存储重开/回退通过；Android真实 SAF/覆盖更新/冷启动生命周期Android16 CI 通过（ARM64 真机待验） |
| 原文件、工程、插件、环境保留 | 无数据清理迁移；保留旧目录、state、版本及签名 | 所有旧交付未改动；API3共存、原档案和配置单元测试通过；准备了原v0.5.3 APK+真实旧插件/中文包+文件/项目/运行环境保留哨兵的升级场景，Android16 CI 通过（ARM64 真机待验） |
| Android 启动无回归闪退 | 保留v0.5.2字面量占位符解析和可降级启动 | JVM渲染通过；Android16实际ICU旧错误重现/新解析/重复Activity启动/损坏偏好和资源回退测试Android16 CI 通过（ARM64 真机待验） |
| 正式应用图标替换 | 原 SVG、透明 PNG、六个 adaptive launcher 入口 | SVG SHA固定且未改变；背景#151922、单独透明前景，无重复背景；机械等比例安全区转换；资源编译通过；Android mask/48/96/216px及桌面显示Android16 CI 通过（ARM64 真机待验）；联想桌面待真机 |

## 3. 自动化测试证据

- JVM：**195/195 通过**：app32、native-packages79、distro81、lsp3。XML、Gradle日志在 `test-evidence.zip`。构建 `BUILD SUCCESSFUL`，并已编译 Android 测试 APK；这不等于 Android 测试已经运行。
- 安装审核前安全回归：真实进程执行两项测试，声明的恶意版本命令和父模块均未被执行；只读取工具/模块位置与包数据库。
- 实际 Ubuntu24.04 amd64：37项工程/单文件/路径/重复运行用例，6个严格匹配候选和实际跳转的 LSP 工程场景，另2个没有 Java Debug 的真实 JDT 场景；三语言实际 DAP 流程；6项配置错误/修复/回滚；Java/C/C++/Python 四种真实 PTY 中文输入和4项生产 Core 进程停止/终端保留验证。
- Stop 测试调用 APK 同一已编译 `GuestProcessTree`，Linux测试绑定只把 Android Os 信号接口映射为真正内核信号；不是虚构进程状态。停止目标后终端及输出、另一个 owner 的任务仍存在。并未把 Linux PTY 等同于 Android PTY。
- 使用官方 Termux/proot 源码提交 `52d65a4dc140ca554d241a1438ddd4f6b00c9813` 编译的现代 PRoot。Ubuntu apt 的旧5.1.0无法正确处理此容器内部分 statx 路径；最初失败日志保留，不能把该旧测试工具问题冒充应用根因。APK 自带 PRoot 二进制没有因测试而替换。
- 初始依赖/模板测试、旧固定目录数量/权限和脚本长度检查的失败及修正日志不算通过数量；最终生产验证器和 Schema 均通过。组件回滚故障是有意注入真实提交失败，不是假响应。
- Android测试设计：两个独立的新建、受控 Android16 x86_64 AVD（ARM64 native bridge）；upgrade 从原v0.5.3安装并恢复旧API3/中文1.0.0及16个文件输入；fresh 独立安装。实际 SAF、来源/权限确认、语言切换/重启/禁用/启用/卸载、字体130%/竖屏、15项 instrumentation/每模式、原Project登记保留、系统Launcher与framework图标测试。必须以文末实际回执为准。
- 翻译率：资源目录 **783/783=100%**；原732值未变。扫描的常用界面显示绑定覆盖率约96.38%，全应用字面量扫描约69.5%；这不是全应用100%汉化。安全来源/权限确认保留宿主英文，编译器/服务器原始输出及部分旧高级页面仍有英文。

## 4. 版本、覆盖安装及数据安全

APK：`sui-code-v0.5.4-debug.apk`，`dev.suicode.ide.debug`，`0.5.4 / 1070807`，ARM64。

- 文件大小：`91273537` bytes；SHA-256：`356933706493edd834d0b0c92d23d09ee503a37aab1bc7a6aa2210b32378a115`。
- 新旧APK实际证书SHA-256均为 `62c2da3484d337626eac6711e5ea7015913e23b8932e9f2b2bd220b843579fa0`。
- 可以按 Android 签名/版本规则覆盖v0.5.3；无需卸载、清数据或重装Ubuntu。真实平板的升级结果待用户最终验收。
- 未删除或重新打包 JDK/GCC/Python/Ubuntu；运行和修复复用既有工具。包禁用/卸载保留工具及用户数据。共享Ubuntu运行包不执行自动 apt purge；原JDK管理保留选择/安全删除非活动JDK能力。
- 正式上游/main及v0.4–v0.5.3交付文件未修改；无正式Release。

中文包：`sui-code-zh-hans-1.0.1.suik`，`19316` bytes，SHA-256 `da707d37d8e9bfcfb5afff25104614fc8792041d69a36038d4f6b162e1fcb5b4`。同ID可更新1.0.0；旧1.0.0文件及原安装内容不要求卸载。此包无作者数字签名，界面必须继续显示未经验证并要求确认。

`official-packages.zip` 包含原三套基础插件（原签名字节保留）、三个1.2.0 IDE Kit、已签名目录和原公钥；各实际包散列见 `PACKAGE-SHA256SUMS`。信任根未改变。`native-sdk.zip`、`source.zip`、`changes.patch`、两份规范、构建和测试证据一并交付。

## 5. 源码与公开规范

基线：`862cbae35d14954cb87eb03097c0d9dddf5c38ac`。分支：`sui-code/v0.5.4-ide-reliability-management`。生产APK源码提交：`339017321e43f5fc3d7c98ff7e031a86ff6ad56c`；工作流补充源码归档的实际提交另见 `apk-metadata.json`。

主要提交：136a6de（挂载/配置事务）、822f1c0（统一LSP路径与协议）、8b41b12（卡片与依赖/规范）、85c39de（独立环境/扩展管理/图标）、e3241aa（真实工具识别与Android管理测试），后续UI状态/验收提交见分支历史和 `modified-files.txt`。

- https://github.com/suidi-guliu/j-code-android/blob/sui-code/v0.5.4-ide-reliability-management/docs/specifications/SUIC-SUIK-Development-Specification.md
- https://github.com/suidi-guliu/j-code-android/blob/sui-code/v0.5.4-ide-reliability-management/docs/specifications/SuiCode-UI-Design-Specification.md
- https://github.com/suidi-guliu/j-code-android/blob/sui-code/v0.5.4-ide-reliability-management/docs/specifications/examples/CARDS.md

## 6. 下载与验收限制

GitHub Actions工作流：`deliver-ide-reliability-v0.5.4.yml`，通过后才上传正式Artifact并下载回比对大小/SHA-256。实际Run/Artifact地址由验收回执和最终聊天提供，不编造尚不存在的链接。Artifact可能需要登录GitHub，保留30天；仓库该分支交付目录同时保留文件。容器内部路径不作为下载入口。

尚未实测：联想Dimensity8300 ARM64上的Android16真实Ubuntu PRoot、三套用户现有完整工程的LSP/DAP/终端联动，OEM圆形图标、外接物理键盘、系统省电/后台限制与长时间使用。云端JDT首次初始化约8–53秒、首次补全约6–12秒，后续请求显著更快；并没有把首次索引耗时称为零等待或增大协议超时。Linux容器PID1不主动回收的已退出测试子进程不计为仍运行的Java进程；真实Android退出/后台场景仍需真机观察。

## 7. ARM64平板集中验收步骤

1. 保留现有安装和数据，记录当前项目与已安装包。核对APK散列后直接覆盖安装；不要卸载、清数据或重装Ubuntu。
2. 连续冷启动3次，确认中文选择、原文件、Project、6套原插件/Kit和现有Ubuntu/JDK/GCC/Python仍存在，第二栏宽度、Files/Find与底部终端可用。
3. Languages应只显示运行环境；搜索Java/GCC/C++/Python、版本和状态。已有GCC时不应提示重复下载。对损坏c.env使用“使用已安装工具修复配置”，查看备份/日志并实际运行C与C++，无网络时仍可修复现有工具配置。
4. Packages分别打开基础插件、IDE Kit、已安装页，测试中英/发布者/类型/版本/状态搜索与清空。中文包应有独立卡片并明确未签名。更新1.0.1后检查即刻切换、重启保持；禁用或卸载安全回退英文，再启用/安装恢复原语言选择。
5. 在对应Kit内只选择一个缺失工具，刷新计划后检查自动依赖、可信来源、版本/大小、权限及安装影响，再统一确认。不得要求去Languages安装LSP/DAP。已安装工具默认复用；主动选择更新/修复才重新审核下载。观察当前及历史日志不跨页面重复。
6. 依次用原Java Maven、C/C++ CMake、Python工程执行Build/Test/Run；再测无Project的单文件、中文/空格目录、重复执行。检查工作目录、正常/异常输出及中文交互输入；Stop后终端和已有输出应保留，程序应结束。
7. Java测试System.out.pr、标准库、自定义类与跳转；C++测试STL、math.hpp和add跳转；Python补全回归。保存、关闭/重开、切换工程，检查服务器Starting/Ready/Error、日志及重启；有活动Debug时不得随意重启LSP。
8. 三语言断点、栈/变量、单步、继续、终止；确认原Python和已通过的Java/C++调试未回退。再测试物理Ctrl+F5和终端输入/粘贴。
9. 桌面查看正式S.图标及两侧符号/原渐变，背景#151922，无重复背景；检查系统圆形mask和小尺寸可读性。不要为清图标缓存清除应用数据。
10. 如失败，保留日志、实际工程路径、服务器状态和重现步骤，通过现有诊断导出收集启动/任务日志；不要用删除环境的方式重试。


## Android 验收记录与最终复验

首轮 Run https://github.com/suidi-guliu/j-code-android/actions/runs/42739177332 未发布APK Artifact。升级场景自动化未正确清空 Packages 搜索字段；全新场景15项 instrumentation通过后，桌面点击被Android首次Taskbar教程遮挡。已修正实际搜索控件定位和系统浮层处理，未将首轮计为通过。

第二轮 Run https://github.com/suidi-guliu/j-code-android/actions/runs/42742398725 已成功完成保留数据升级与全新安装、30项 instrumentation、2项Project检查和Artifact上传后下载字节核验。最终候选在此后加入审核前被动探测安全修复，因此本APK仍须新的完整Android CI验收回执；前一轮成功不冒充当前二进制通过。


## 最终候选的实际 Android16 CI 回执

本APK的保留数据升级与全新安装两组场景通过，30项Android instrumentation及2项原Project登记检查通过；真实SAF和来源/权限确认、中文选择/重启/启停/卸载回退、多次冷启动、130%字体和竖屏、官方/第三方卡片、两个独立搜索、真实Android Adaptive Icon渲染及系统Launcher点击通过。两个Android16 x86_64 AVD使用ARM64 native bridge，不是真实ARM64平板。升级环境工具路径为保留哨兵，未冒充已执行Ubuntu工具；真实Ubuntu任务/LSP/DAP结果单独列在前文。上传后的Artifact已由同一工作流重新下载，全部大小和SHA-256逐字节验证通过。

[运行与构建/测试日志](https://github.com/suidi-guliu/j-code-android/actions/runs/42744903416) · [最终Artifact下载](https://github.com/suidi-guliu/j-code-android/actions/runs/42744903416/artifacts/13731581394)（登录GitHub，保留30天）。公开仓库中同一APK和中文包另有永久版本直链：

- https://raw.githubusercontent.com/suidi-guliu/j-code-android/0826710/.delivery/sui-code-v0.5.4/sui-code-v0.5.4-debug.apk
- https://raw.githubusercontent.com/suidi-guliu/j-code-android/0826710/.delivery/sui-code-v0.5.4/sui-code-zh-hans-1.0.1.suik

本轮真实工程/LSP/DAP集成测试使用交付的三套1.2.0 IDE Kit。覆盖安装APK会保留原Kit；请在Packages → IDE Kit Library审核更新Java、C/C++、Python Kit至1.2.0，或导入official-packages.zip中的对应.suik。更新复用已经安装且检测正常的工具，无需卸载Kit、Basic插件、Ubuntu、JDK、GCC/G++或Python。中文1.0.1可同ID覆盖更新，保留原732条翻译；该包仍为未签名。

生产APK源码为339017321e43f5fc3d7c98ff7e031a86ff6ad56c；Android CI/Artifact源码归档为0826710。后续回执文档提交不更改上述已验证APK。原始Artifact内报告也包含工作流生成的实际验收回执；本仓库报告补充下载入口及Kit更新步骤。
