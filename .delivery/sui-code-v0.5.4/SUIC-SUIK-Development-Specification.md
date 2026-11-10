# sui-code Native Extension & IDE Kit Development Specification

规范版本 **0.4.2**；Core API **4**（兼容 API 1、2、3 声明）；SUIC **1.0**；SUIK **1.0**。

本文的 MUST/必须、MUST NOT/禁止、SHOULD/应为规范性要求。本文与 `core/native-packages/src/main/java/dev/suicode/packages` 中的生产验证器、加载器、授权会话及事务存储对应，不引用 JCode 插件 API。所有路径均相对仓库根目录。

## 1. 实现状态与总体架构

协议模块已实现格式识别、校验、权限门禁、独立 Plugin/Kit 解析及激活、受限组件描述、包状态事务、停用、卸载、回滚和声明式面板状态。`PackageTool` 使用生产验证器，是本文示例的验证入口。

Android 宿主 `app/.../dev/suicode/nativehost` 已连接独立 PackageStore、原生中心、语言注册、共享 PRoot/PTY 调度、LSP/DAP、工程模板和声明式 UI。旧 JCode 插件安装与市场入口已断开；历史磁盘数据保留，不能把旧包作为新包加载。设备测试状态以交付报告为准，本文不将 Linux x64 协议测试等同 Android ARM64 验收。

执行路径定义为：不可信包 → 格式/完整性验证 → 静态权限与职责检查 → 用户审阅 → 针对摘要授权 → host 提供受控 backend → 会话请求 → backend。安装和激活不会执行包中代码。新协议不加载 DEX、APK、WebView 脚本或 JCode 扩展实例。

官方六个签名包位于 `native-library/v1/`，源码位于 `native-library/src/`。开发样例位于 `examples/`，未签名且与官方目录分开。所有语言规则、模板和 IDE 工具声明在包内，Core 不含语言专属 QuickRun 编译规则。

## 2. Core API 类型定义

包名：`dev.suicode.packages`。调用均为进程内 host API，不是开放 Android IPC。扩展作者编写声明式文件，不能获得 `Context`、`ClassLoader`、任意 `File` 或 shell 对象。

| 类型 | 完整字段与约束 |
|---|---|
| `CoreApi` | `VERSION: Int = 3`、`MIN_SUPPORTED: Int = 1`；`compatible(min:Int,max:Int):Boolean` 判断区间交集 |
| `PackKind` | `PLUGIN`/`KIT`；属性 `magic: String`、`suffix: String`、`mime: String` |
| `Capability` | 枚举；wire 值见权限表 |
| `Operation` | 枚举；`permission: Capability`，见操作表 |
| `Request` | 必填 `operation: Operation`、`component: String`、`arguments: Map<String,String>`；可选 `packageId:String=""`、`digest:String=""`，宿主会话始终用已认证值覆盖这两项 |
| `Reply` | 必填 `accepted: Boolean`、`code: String`；可选 `values: Map<String,String> = emptyMap()` |
| `Consent` | 必填 `digest: String`、`grants: Set<Capability>`、`allowUnverified: Boolean` |
| `VerifiedPack` | 只由验证器创建；只读 `kind,id,version,digest,official,signer,permissions,manifest`；`signer` 可空，其余不可空 |
| `PackageFailure` | `code: String`；异常文本为 `CODE: detail` |

`digest` 是完整容器的 SHA-256 小写十六进制，64 字符。`signer` 为签名所用 SubjectPublicKeyInfo DER 的 SHA-256，不是证书名称。返回 `official=false` 不等于签名无效：可能未签名，也可能签名有效但公钥未被 host 固定为官方根。

## 3. 兼容性和版本策略

Manifest `api.min`/`api.max` 为闭区间正整数，必须与 `[MIN_SUPPORTED,VERSION]` 相交；新 `panel.nodes` 要求 `min>=2`。不兼容报 `E_API`，不尝试降级到 JCode API。容器版本与 API 版本独立；当前只识别格式 1.0，其他版本报 `E_FORMAT`。

包 `version` 仅支持三段非负整数：`major.minor.patch`；每段最多六位，禁止前导零、预发布和构建元数据。依赖区间为 `[min,max)`，按整数逐段比较，禁止空区间。当前规范明确不等价于 npm semver。

规范 0.x 允许在新版本中调整，但必须同步验证器、Schema、示例与变更记录。稳定 API 发布后，删除/改变参数、权限扩张及行为不兼容均需升 API 主版本；旧接口至少跨一个稳定主版本保留显式弃用记录，不可静默改义。

## 4. SUIC 文件格式

文件扩展名 `.suic`；MIME `application/vnd.suicode.plugin`。扩展名大小写敏感。

容器前 20 bytes：

| 偏移 | 长度 | 内容 |
|---:|---:|---|
| 0 | 8 | ASCII `SUICPKG` 后跟 `00` |
| 8 | 2 | 无符号 big-endian major，值 1 |
| 10 | 2 | 无符号 big-endian minor，值 0 |
| 12 | 8 | 无符号 big-endian ZIP payload 字节数，必须等于文件大小减 20 |
| 20 | N | ZIP 流，条目受下述严格约束 |

裸 ZIP、仅改扩展名、魔数与扩展名不匹配均拒绝。容器大小 20 bytes 至 32 MiB；最多 256 个条目；单条目展开不超过 8 MiB，总展开不超过 32 MiB。归档只在内存验证，不执行或直接解压到用户目录。资源只在审阅且获 process.run 授权后交由 Ubuntu 子进程执行；禁止执行 IDE 主进程代码。

## 5. SUIC 结构、JSON 编码与 Manifest

```text
manifest.json          必需
plugin/*.json          由组件条目引用
resources/**           可选数据
assets/**              可选数据
checksums.sha256       必需
signature.sig          已签名包必需；未签名包省略
```

路径仅允许 `[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)*`；禁止 `.`/`..` 段、绝对路径、反斜杠、重复路径和目录条目。打包工具拒绝符号链接。Manifest/组件/签名 JSON 各不超过 1 MiB、结构嵌套不超过 64 层，必须严格 UTF-8、无 BOM，递归按 key 排序、无空白分隔、末尾恰一个 LF。重复 key、额外空白及非规范 JSON 均拒绝。建议使用提供的打包器生成，勿手写归档。

Manifest 所有字段均必填，禁止额外字段：

| 字段 | 类型、含义 |
|---|---|
| `id` | 反向域名式小写 ID，正则见 Schema；稳定标识，更新不能改 ID |
| `name`,`author`,`description` | 非空字符串，各最多 1024 字符；展示文本，不产生信任 |
| `version` | 第 3 节版本 |
| `kind` | SUIC 必须为 `suic` |
| `api` | 对象；必填整数 `min`、`max`，`1 <= min <= max` |
| `permissions` | 不重复的允许权限字符串数组；可空 |
| `dependencies` | 对象数组；每项必填 `id`,`min`,`max`；禁止自依赖，安装集必须满足区间 |
| `components` | 对象数组；每项必填 `id`,`type`,`entry`；ID 在本包唯一；entry 必须是存在的 `plugin/` 下 JSON |

完整机器 Schema：[`schemas/suic-manifest.schema.json`](schemas/suic-manifest.schema.json)。完整合法示例：[`examples/minimal/manifest.json`](examples/minimal/manifest.json)；组件见同目录 `plugin/language.json`。静态 Schema 外，还须通过依赖、文件摘要、职责、签名、规范 JSON 及路径检查；仅通过 JSON Schema 不能视为合法包。

## 6. SUIC 生命周期

`untrusted → verified → reviewed → installed-disabled → enabled → disabled → uninstalled`。更新回到 reviewed，旧摘要的授权不能复用。验证失败没有生命周期回调，不落地代码。

- `PluginParser.parse(bytes: ByteArray, installed: Map<String,String> = emptyMap()): VerifiedPack`：安装预览/导入时调用；仅接受 SUIC。无权限；失败抛 `PackageFailure`，不改变状态。
- `PluginLoader.activate(pack: VerifiedPack, consent: Consent, backends: Map<Operation,CoreBackend>): CoreSession`：host 确认授权并激活时调用。kind 不匹配拒绝；授权摘要不匹配 `E_CONSENT`；未验证包未明确同意 `E_UNVERIFIED`。返回受限会话，不执行组件。
- `CoreSession.close(): Unit`：禁用/卸载/会话结束时调用，幂等；后续请求 `E_DISABLED`。

Loader 不回调包内 `onLoad`/shell hook；“启用”仅表示 host 可以分发已授权的声明。host 退出后会话失效，不自动恢复进程。

## 7. SimpleRun Provider 完整接口

现阶段接口是声明式 JSON + `CoreBackend`，不是 JCode runner 或任意原生类。

组件 type `simpleRun`（或 `basicBuild`），描述文件全部字段必填：

| 字段 | 类型与约束 |
|---|---|
| `languageId` | String，1..4096 字符；关联语言 ID |
| `environmentId` | String，1..4096 字符；关联 host 环境 |
| `prepare` | `Array<Array<String>>`，最多 16 步；每步 1..128 参数，每参数最多 4096 字符，无 NUL |
| `run` | `Array<String>`，1..128 参数，同上 |

每一步是 argv，解释器逐参数单引号转义，不执行插入路径中的 shell 元字符。支持完整参数占位符 `${source}`（保存后的 guest 文件）、`${directory}`（文件目录）、`${project}`（项目目录）、`${resource:resources/path}`（本包认证资源）。占位符必须占满参数；不支持 `${tool}` 或字符串内插。`.suik` command/workflow 另支持 `${input:node-id}`。未定义占位符报 E_COMPONENT，缺失输入报 E_CONTEXT。环境选择及验证由资源脚本实现，不允许静默安装或选择替代 JDK。

`NativeProviders.register(pack:VerifiedPack,session:CoreSession):Unit` 在启用时登记同包 language/simpleRun；`unregister(packageId:String):Unit` 撤销；`resolve(fileName:String):RunProvider?` 返回唯一大小写匹配的 Provider，未匹配返回 null，歧义报 E_CONFLICT。Kit 只能覆盖其显式依赖的 Basic Provider。`RunProvider.languageId:String`、`extensions:Set<String>` 提供标识。

`NativeRecipe.forComponent(pack:VerifiedPack,session:CoreSession,id:String,inputs:Map<String,String>=emptyMap()):NativeRecipe` 获取 SimpleRun/Build/Test/command 的认证 recipe；`authorize():Reply` 仅检查授权；`resources():Map<String,ByteArray>` 返回引用资源的副本；`script(source:String,resourceDirectory:String,project:String=source父目录):String` 返回安全 Bash 调度脚本。资源目录须匹配 `/tmp/sui-package-[a-zA-Z0-9-]+`，路径必须绝对且无 NUL。构造不启动进程；实际任务启动前重新授权；close 后 E_DISABLED。

实际调用：`CoreSession.request(Request(Operation.SIMPLE_RUN, componentId, arguments)): Reply`。调用时机为 host 取得当前文件、等待保存、解析真实路径和环境后；权限 `process.run` + `file.read`。组件必须登记为 SimpleRun/basicBuild，且用户须授权本摘要。没有 backend 返回 `E_UNAVAILABLE`，而不是接受后静默丢任务。

`CoreBackend.invoke(request: Request): Reply` 是 host 提供的唯一执行端口。所有参数必填。执行 backend 必须验证路径授权、环境身份、进程树归属，返回任务 ID/状态；必须通过同一 host 的停止机制清理子进程。Android 使用共享任务控制器等待保存、映射路径、分配 PTY 会话及进程 owner。Ctrl+F5 仅对激活源码执行；重复按键/忙任务不创建新进程。Core API 成功回复 STARTING 是排队状态，不是编译成功；实际退出码和输出在 Terminal 中。停止和关闭会话按 owner 清理整个子进程树，ProjectRunner 任务使用不同 owner。每任务 TMPDIR 位于认证资源目录下，Basic 编译临时目录使用该 TMPDIR；即使硬停止使 shell EXIT trap 没机会运行，host 的 NonCancellable NOFOLLOW 清理也会删除该任务临时文件。

## 8. 语言注册与文件识别

`language` 描述：必填 `languageId: String`（`[a-z][a-z0-9+-]{0,31}`）、`extensions: Array<String>`（非空、不重复、每项 `[A-Za-z0-9+_-]{1,16}`，无前导点）、`priority: Int`（0..100）。Schema：[`schemas/language.schema.json`](schemas/language.schema.json)。

扩展名匹配必须保持大小写，避免 `.C` 混为 C；C/C++ 在同一包登记多个后缀。发生多个声明冲突时 host 必须让用户选择，不能用安装先后静默覆盖；priority 只作建议。NativeProviders 用此声明分发单文件运行；LSP 后缀来自授权 Kit。priority 当前不用于静默胜出。

## 9. SUIK 文件格式

扩展名 `.suik`；MIME `application/vnd.suicode.ide-kit`。20-byte 结构与 SUIC 相同，但前 8 bytes 为 ASCII `SUIKPKG` + `00`。版本 1.0，大小和 ZIP 安全规则相同。跨类型解析报 `E_KIND`。

## 10. SUIK 目录与 Manifest

```text
manifest.json
ide/*.json
components/**
adapters/**
workflows/**
resources/**
assets/**
checksums.sha256
signature.sig          可选；官方包必须存在
```

SUIK Manifest 字段定义与第 5 节相同，但 `kind=suik`、组件 `entry` 必须指向 `ide/` 下 JSON、允许的类型与权限不同。两种完整独立 Schema 均将 kind、type、权限及路径固定，不能用一个自由 Manifest 同时解释两种包。

Schema：[`schemas/suik-manifest.schema.json`](schemas/suik-manifest.schema.json)；完整示例：[`examples/minimal-kit/manifest.json`](examples/minimal-kit/manifest.json)。Java Kit 的显式依赖示例见 [`examples/java-kit/manifest.json`](examples/java-kit/manifest.json)。组件 Schema 见 `schemas/*.schema.json`。

## 11. SUIK 组件注册与生命周期

`IdeKitParser.parse(bytes, installed=emptyMap()): VerifiedPack` 与 `IdeKitLoader.activate(pack,consent,backends): CoreSession` 参数/返回类型与 SUIC 一致，只接受 KIT。禁用必须关闭该包所有会话、进程及面板资源；禁用 Kit 不禁用它依赖的基础插件或环境。

组件 ID 正则 `[a-z][a-z0-9.-]{0,63}`；引用只在当前包内有效；同名 UI 用包 ID 命名空间隔离。`CoreSession` 将组件 ID 绑定到特定 Operation，Kit 声明 panel 不能改以该 ID 调用 BUILD。

## 12. IDE 专属操作 API

| Operation | 允许组件类型 | 必须声明并授权的权限 |
|---|---|---|
| SIMPLE_RUN | simpleRun / basicBuild（basicBuild 仅 SUIC）/ Kit command | process.run + file.read |
| LSP_START | lsp | language.lsp + process.run + file.read |
| DAP_START | dap | debug.dap + process.run + file.read |
| BUILD | build | process.run + file.read |
| TEST | test | process.run + file.read |
| FORMAT | format | file.write |
| LINT | lint | file.read |
| PROJECT_READ | project | project.read |
| PANEL_OPEN | panel | ui.panel |

`workflow`/`command` 可引用上述任一 Operation，但仍受对应权限和 backend 检查。普通操作描述必须恰有 `operation: String` 和 `arguments: Object<String,String>`；arguments 最多 128 key，每 value 最多 4096 字符。operation 必须为表中枚举。额外字段、类型不符报 `E_COMPONENT`/`E_BOUNDARY`。

`CoreSession.request` 同步返回 `Reply`；依序检查活跃状态、组件存在、组件操作绑定、SUIC 边界、声明及授权权限、backend 可用性。backend 抛异常转换为 `E_BACKEND`，异常不泄露到扩展。Android backends 实际连接编辑器、任务、语言和调试控制器。返回 STARTING/REQUESTED 表示异步请求已接收；E_BUSY 表示已有任务；READ/OPENED 表示同步读取/显示完成。reply.values 可含读取结果，不承诺所有异步操作有任务 ID。

## 13. LSP 与 DAP

LSP/DAP 组件使用第 12 节格式。LSP 调用 `Request(LSP_START,id,args)`，DAP 调用 `Request(DAP_START,id,args)`。结果统一 `Reply`；没有 adapter 返回 `E_UNAVAILABLE`。禁止声称只有描述文件就具备补全或调试。

实现 backend 时必须使用 Ubuntu ARM64 匹配工具及独立进程，LSP 使用标准 JSON-RPC 2.0/Content-Length framing，DAP 使用官方 DAP framing；host 负责关闭、取消、超时、路径 URI 映射，不能把 Android host 路径直接交给 guest。LSP 动态工作区编辑须经 file.write 单独授权，DAP 运行目标还须 process.run。当前 broker 单操作权限是第一道门，不能代替 backend 的复合权限检查；Android 执行这些复合检查；未启用相应 Kit、工具未安装或服务未就绪时返回不可用或显示明确安装入口，不自动执行安装。

## 14. Build / Test / Format / Lint

四种组件分别绑定 BUILD/TEST/FORMAT/LINT；调用、参数和 Reply 同第 12 节。host adapter 应提供实际工具版本、工作目录、结果、诊断和取消能力。Format 必须遵守保存版本条件并原子替换，不允许覆盖用户更新；Test 必须区分失败断言和 runner 错误。Build 产物放 host 分配的目录；不得未经同意向源目录写二进制。

官方 Java Kit 使用 Maven/JUnit 或已有 Gradle wrapper；C/C++ 使用 CMake/Ninja/CTest；Python 使用 compileall/unittest。项目输出保留在标准 target/build 或 `.sui-code/` 目录；单文件输出使用临时目录。command 的 argv 定义 Run Project；Format 使用实际 LSP 格式化，Lint 触发文档诊断；没有格式化能力返回 E_UNAVAILABLE。

## 15. Project 与工作流

`project` 绑定 PROJECT_READ，只能读取用户选择的 Project，不得枚举全部私有文件。无 Project 时基础 SimpleRun 不受影响。工作流为声明式操作请求，不提供脚本钩子；宿主须单独展示每个步骤的执行行为和权限。

本版本 workflow 描述仅支持一个 Operation 请求，不支持 DAG、循环或任意表达式；复杂工作流不能用未定义字段假装已实现。后续多步骤版本须扩展 Schema 并增加版本协商。

## 16. 第三方特殊功能

高级重构、分析面板、自定义调试 UI、数据工作流必须 `.suik`。SUIC type 集只有 language/simpleRun/environment/basicBuild，权限只有 file.read/process.run；任何 panel/lsp/dap/workflow 等在验证阶段报 `E_BOUNDARY`。资源数据不应被 host 当作可执行代码加载。

可验证第三方样例：[`examples/special-feature/`](examples/special-feature/)。其 PROJECT_READ / action=statistics 请求实际统计当前 Project（无 Project 时为 Internal Files）的普通文件数量与字节；不跟随符号链接，结果供原生表格显示。

## 17. UI 面板与交互接口

`panel` 描述全部字段：`title: String`（1..80）、`text: String`（0..65536）、`status`（ready/loading/empty/error）。可选 `nodes:JsonArray`（API2），详见 UI 规范。不得携带 HTML/JS/CSS/APK/DEX 注入。

`PanelState(id:String,title:String,text:String,status:String="ready",nodes:JsonArray=JsonArray(emptyList()))` 为实际模型。`PanelStore.put(session:CoreSession,panel:PanelState):Reply` 调用 PANEL_OPEN；只有 accepted 才保存状态。`snapshot():List<PanelState>` 返回状态快照；`release(packageId:String):Unit` 移除该包面板。调用时机为 host 渲染/更新/卸载，权限 ui.panel。无 UI backend 时 E_UNAVAILABLE，不创建伪面板。

UI 视觉、尺寸、主题及无障碍详见独立 [UI 规范](SuiCode-UI-Design-Specification.md)。Android `NativeKitPanel` 解释经过验证的 nodes，继承 native 主题。完整 node 字段、绑定和事件见 UI 规范。

## 18. 依赖与职责边界

依赖只按明确包 ID 和版本解析，不根据语言环境重复安装。Kit 可以依赖 SUIC；卸载被依赖包报 E_DEPENDENCY。禁止 SUIC 通过一个脚本向 host 请求高级 IDE 注册。每个 capability 是门禁而非执行许可证；权限同意不能扩张包类型职责。

SUIC/SUIK 共享纯粹的格式安全代码，但拥有不同 magic、kind、Schema、目录规则、解析入口、Loader 和允许组件集合。两者没有继承 JCode 原有生命周期。

## 19. 官方环境声明与获取

`environment` 描述字段全部必填：`environmentId:String`（1..4096）、`provider`（temurin/ubuntu-apt）、`tools:Array<String>`（非空、每项 `[A-Za-z0-9+_-]{1,40}`）、`configuration:String`（`/opt/sui-code/[a-z-]+.env`）。此声明不自动下载安装。

Java 采用官方 Temurin API及包校验；C/C++ 合包，工具 gcc/g++；Python 工具 python3。不得向包内塞编译器、解释器或大型工具链。host 应探测既有 env 和工具，优先保留 v0.3 用户安装；Kit 不得要求重复安装。

官方库须由 host 固定根验证目录及实际包，展示来源、摘要、签名、大小、版本、兼容性及日志。不允许使用 JCode 市场作为新协议源。官方公钥 `native-library/official-p256.der` 固定在 APK；六个 `.suic/.suik` 及签名 index 位于 `native-library/v1/`。开发样例不能获得官方信任。

## 20. 权限、隔离与安全边界

| wire 权限 | 能力范围 |
|---|---|
| file.read | 用户授权范围内文件读取 |
| file.write | 用户授权范围内原子写入 |
| process.run | 审阅后的执行任务 |
| language.lsp | LSP 连接声明 |
| debug.dap | 调试适配连接声明 |
| project.read | 当前选中 Project 元数据 |
| ui.panel | 命名空间内声明式面板 |

安装预览必须展示权限、组件类型、argv/行为、签名来源和依赖。Consent 必须绑定完整包摘要，grants 只能是声明权限子集。未签名/非固定根签名包必须明确标为未验证，并要求 allowUnverified。不能把未知公钥自动加入官方信任。

**PRoot 不是恶意代码安全沙箱。** 只有签名验证和进程标签不能实现文件/网络隔离。process.run 授权允许在应用 Ubuntu 中执行所审阅的代码，拥有应用/guest 可访问的文件和网络，因此只应授予可信包。授权门禁不构成敌对代码隔离，不可声称任意脚本已被安全沙箱化。禁止在 IDE 同进程载入第三方代码。

## 21. SHA-256 与签名

`checksums.sha256` 包含除本文件、signature.sig 外的每个文件，按 ASCII 路径排序，每行 `64位小写hash + 两个空格 + path + LF`。不允许缺项、多项、重排或空白变体；manifest 和组件必须在表内。

签名输入为原始字节拼接：`UTF8("SUI-PACK-SIGNATURE-V1\n") || magic8 || checksumsBytes`。签名算法仅 `ECDSA-P256-SHA256`（secp256r1，SHA256withECDSA，DER ASN.1 签名）。signature.sig 为规范 JSON，恰有 algorithm、publicKey、signature；后两项标准 Base64，publicKey 是 X.509 SubjectPublicKeyInfo DER。

验证任何存在的 signature.sig，坏签名直接 E_SIGNATURE，不降级为“未签名可继续”。官方判定要求公钥摘要和 DER 均匹配 host 固定根。全部容器另算 digest 以绑定安装同意；重新压缩后的完整 digest 可能不同，须重新同意。签名私钥不入仓库、包或 APK。

## 22. 安装、更新、卸载和回滚 API

`PackageStore(root:Path,verifier:PackageVerifier=PackageVerifier())` 必须传入应用私有目录，由单一 host 实例拥有，不得指向 Ubuntu/Project/用户源码目录。

| 方法及完整类型 | 时机、结果与失败 |
|---|---|
| `installed():Map<String,String>` | 返回已登记 ID→version，无外部权限 |
| `inspect(bytes:ByteArray,suffix:String):VerifiedPack` | 审阅前；与安装集检查依赖；失败不改状态 |
| `install(bytes:ByteArray,suffix:String,consent:Consent):VerifiedPack` | 审阅后；验证授权，存原始包并原子发布状态；初始 disabled；不启动代码 |
| `enable(id:String,backends:Map<Operation,CoreBackend>):CoreSession` | 用户启用；重新验证摘要/依赖/权限，再注册会话 |
| `disable(id:String):Unit` | 撤销会话，后续请求 disabled |
| `uninstall(id:String):Unit` | 被依赖则拒绝；移除登记并撤销会话；不删除工具链/用户数据 |
| `rollback(id:String,consent:Consent):VerifiedPack` | 用户选择上一摘要并重新授权；无上一版 E_ROLLBACK |
| `close():Unit` | host 结束，关闭所有当前会话 |

存储为 content-addressed `<digest>.<suffix>` 原始档案及原子 `state.json`；temp→fsync→ATOMIC_MOVE。失败不回退截断写；前状态保留，可能遗留惰性档案。支持单 host 实例的同步事务，**不支持跨进程并发写**。当前没有垃圾回收 API；保留档案供恢复，不自动删除环境。更新签名公钥变更 E_SIGNER_CHANGED，需用户显式卸载旧登记后再审阅。包被更新/禁用时旧会话失效。

## 23. 错误码与日志

| 码 | 意义 |
|---|---|
| E_MAGIC / E_KIND / E_FORMAT | 非法识别 / 类型冲突 / 不支持格式 |
| E_SIZE / E_ARCHIVE / E_PATH / E_STRUCTURE | 大小 / ZIP损坏 / 路径 / 目录结构错误 |
| E_CANONICAL / E_MANIFEST / E_COMPONENT | JSON编码 / Manifest / 组件不合法 |
| E_VERSION / E_API / E_DEPENDENCY | 版本语法 / Core不兼容 / 依赖不满足 |
| E_CHECKSUM / E_SIGNATURE | 摘要或签名无效 |
| E_BOUNDARY / E_PERMISSION / E_CONSENT / E_UNVERIFIED | 职责 / 权限 / 同意摘要 / 未验证包未同意 |
| E_DISABLED / E_UNAVAILABLE / E_BACKEND | 会话撤销 / 后端缺失 / 后端异常 |
| E_NOT_INSTALLED / E_ROLLBACK / E_SIGNER_CHANGED | 未安装 / 无上一版本 / 更新更换签名者 |

解析/存储函数抛 PackageFailure；外部 I/O 错误可抛 IOException，host 应保留原始 cause 供诊断，不得将其当作格式合法或成功。Session 用 Reply 返回，不抛 backend 异常。

日志须记录时间、包ID、digest、operation、错误码、任务关联号；不得记录私钥、授权 token、终端秘密或完整源码。当前模块不上传日志、不发送遥测，Android 通过操作反馈、Terminal/OutputLog 和安装日志呈现；持久化安装日志位于 Ubuntu `/opt/sui-code/logs/`。

## 24. Android ARM64 与 Ubuntu PRoot

运行平台目标 Android API33+、arm64-v8a、Ubuntu24.04 aarch64；host JVM21 编译不代表设备签名算法、原子文件系统或 PRoot 运行已验证。工具须逐一验证 guest 架构、发行渠道、SHA及适用版本，禁止使用 x64 工具冒充 ARM64。

协议 JVM 测试与真实 Ubuntu x64 运行/LSP/DAP 集成分开报告，Android 平板物理键盘、PRoot ptrace 和升级数据保留须实机验收。不得清理 v0.3 的 Ubuntu、java.env、GCC/G++/Python、Project或源码。迁移必须可失败恢复，保留旧配置直到新链路验证。

## 25. 开发、验证与测试流程

1. 复制 examples/minimal 或 minimal-kit，改为自己持有的反向域名ID；根据职责选格式。
2. 依 Schema 写完整 Manifest/描述，禁止额外字段；任何特殊IDE功能选 Kit。
3. 运行 `python3 tools/sui_packages/pack.py SOURCE OUTPUT.suic`（或 suik）；开发包默认为未签名。
4. 本地签名可 `openssl ecparam -name prime256v1 -genkey -noout -out /安全路径/key.pem`，打包器加 `--key`；自签不等于官方。
5. `bash gradlew :core:native-packages:validatePackage -PsuiPackage=/绝对路径/包.suic`。依赖加 `-PsuiInstalled=id=0.1.0,other.id=1.0.0`。验证失败进程非零，打印明确错误。
6. 用 `bash gradlew :core:native-packages:testDebugUnitTest` 跑职责、签名、权限、状态及事务测试。不要在用户实际数据目录测试故障注入。
7. 审阅执行行为和权限，再进行 host backend/Android 集成；CLI通过只代表包协议有效，不代表运行/调试功能已实现。

可验证测试包在 `examples/packages/`：minimal.suic、minimal-kit.suik 合法，illegal-plugin.suic 必须 E_BOUNDARY。其余三语言基础/Kit及特殊功能样例同样可验证；语言 Kit 需传入对应基础依赖。样例包均未签名，不能呈现“官方可信”。

## 26. 变更记录与公共发布状态

0.1.0：独立 binary envelope、双 Schema、规范 JSON、SHA/P256验证、静态职责限制、权限会话、独立Loader、原子存储、声明式面板和开发工具。尚未稳定发布 API，不保证第三方部署兼容，任何变更须同步本文件、UI规范、Schema、样例和单测。

0.2.0：Core API 2，兼容 API 1 包；加入真实 Android 宿主、NativeRecipe/NativeProviders、官方签名目录、Kit 元数据、DebugWorkflow、认证工程模板、原生 widgets。旧 API1 只读 panel 继续可用，`panel.nodes` 要求 API2。SUIC/SUIK 容器仍为 1.0。弃用接口先记录、至少一个稳定主版本后才删除；格式不兼容必须升容器 major。

本文不替代产品验收。正式构建的 SHA、签名、测试数量及未完成的硬件验证均见同一提交的交付报告。

## Android 宿主与执行协议（API2）

开发者仅提供声明、资源与包，不得到 Android Context 或同进程代码执行权。以下接口由应用集成者调用，第三方不能绕开会话授权直接调用 backend。

| 接口 | 参数和返回 | 调用时机、权限、失败与兼容 |
|---|---|---|
| `VerifiedPack.content(path:String):ByteArray` | 必填包内路径；返回认证字节副本 | 资源 staging 时；缺失 E_COMPONENT；不授予运行权；API1 扩展兼容 |
| `VerifiedPack.declarations():List<PackageComponent>` | 无参；每项 `id:String,type:String,descriptor:JsonObject` | 安装预览/登记；只读解析，无执行 |
| `CoreSession.hasCapability(capability:Capability):Boolean` | 必填枚举，返回 active 且声明/授权同时成立 | backend 复合权限；关闭后 false |
| `CoreSession.authorize(request:Request):Reply` | 必填请求；AUTHORIZED 或明确拒绝码 | 启动前检查；不调用 backend；必须仍在执行时检查有效会话 |
| `CoreSession.request(request:Request):Reply` | 必填请求；同步结果/异步接受状态 | 操作分发；认证 packageId/digest 覆盖输入值；缺 backend E_UNAVAILABLE，异常 E_BACKEND |
| `PackageStore.records():List<InstalledPackage>` | id/version/digest/kind/enabled/grants | 安装状态列表；无工具或文件执行 |
| `PackageStore.load(id:String):VerifiedPack` | 必填已安装 ID；重新校验档案与依赖 | 恢复/使用前；缺失 E_NOT_INSTALLED，损坏拒绝 |
| `PackageStore.previous(id:String):Pair<ByteArray,VerifiedPack>` | 上一档案与验证模型 | 回滚预览；不自动授权；无记录 E_ROLLBACK |
| `OfficialLibrary.verify(bytes:ByteArray,signature:ByteArray,key:PublicKey):List<LibraryItem>` | 三参必填；认证目录条目 | 下载前；无副作用；E_SIGNATURE/E_API/E_FORMAT/E_SIZE；API2 兼容 API1 范围 |
| `NativePackageHost.importDocument(uri:Uri):PackagePreview` | suspend；Android 文档 URI | 用户选本地包后；有界读取、魔数识别、依赖与签名检查；不安装或执行 |
| `NativePackageHost.download(item:LibraryItem):PackagePreview` | suspend；认证目录项 | 用户选择下载；HTTPS、大小、hash、签名、ID/version 全验证后预览 |
| `NativePackageHost.install(preview,grants,allowUnverified):Unit` | suspend；三项必填；摘要绑定授权 | 确认后原子安装并启用；失败保持归档可回滚；不安装语言环境 |
| `NativePackageHost.invoke(id:String,componentId:String,inputs:Map<String,String>=emptyMap()):Reply` | suspend；输入最多64项，键 node ID，值<=4096且无NUL | 用户点击同 Kit 命令；输入仅映射为 input.*，不可覆盖签名 descriptor 参数；缺会话 E_DISABLED |
| `NativePackageHost.enable/disable/uninstall(id:String):Unit` | suspend，必填包 ID | 用户操作；禁用/卸载取消所属任务/调试/面板；依赖不可满足时服务撤销；环境与用户文件不删 |
| `NativePackageHost.restore():Unit` / `detach():Unit` | 恢复登记 / 关闭内存会话 | VM 生命周期；恢复失败显示包 ID 与原因，不清除数据；不运行启动脚本 |

Android 中每个包的权限范围绑定完整 digest。进程任务共用已有终端管理器，owner 使用独立 `sui-quickrun:UUID/task` 命名空间。启动前保存失败或保存期间内容继续变化会取消；工作目录映射为绑定祖先下的真实源目录，支持空格、中文、引号与冒号；路径仅作为正确引用的参数。Terminal 保留 stdout/stderr/交互 stdin，QuickRun 停止/关闭会话清理所有所属进程，不触碰其他 ProjectRunner 会话。编译/运行的具体错误码由包脚本定义，官方 Basic 使用 65=编译、70=运行、78=环境错误。

### LSP adapter arguments

descriptor 为 `{operation:"LSP_START",arguments:{...}}`。以下字段全部必填且为字符串（最大4096字符）；JSON 数组/对象必须编码成字符串，不混用自由对象：

| 字段 | 内容 |
|---|---|
| serverId | 唯一目录/运行标识 |
| name | 展示名 |
| installCommand / verifyCommand / uninstallCommand | Ubuntu Bash 命令；安装/删除须用户在 Language Center 确认；验证可在已授权启用后检查 |
| runCommand | stdio 服务命令；`${workspaceId}` 替换为 host 生成的每根目录摘要；不提供未引用的用户路径内插 |
| languageIds / extensions | JSON 字符串数组；extensions 含前导点，位置对应语言，精确大小写先匹配 |
| rootDetectors | JSON 字符串数组，项目标记名，用于定位文件根目录 |
| initializationOptions | JSON 对象字符串，传给真实 LSP initialize |

激活要求 Kit 和所有依赖启用，并授权 language.lsp、process.run、file.read。启动失败显示原工具日志；完成、诊断、定义、引用、重命名、格式化、code actions 以服务实际 capabilities 为准。工作区编辑另外要求 file.write，host 在主线程等待实际写入后才 ack；过期格式化结果丢弃。禁用/更新后关闭并重新建立匹配服务，旧会话不再提供能力。

### DAP adapter arguments 与 DebugWorkflow

descriptor 为 `{operation:"DAP_START",arguments:{...}}`。必填字符串：id、name、installCommand、verifyCommand、uninstallCommand、adapterCommand、transport、debugType、extensions、workflow。transport=`stdio` 或 `lsp`（后者接 Kit RPC 返回的 TCP port）；extensions 为 JSON 字符串数组。可选 preLaunchResource 是本包 resources/ 下的脚本路径；经校验并 staging 后，以 `[script,source,project]` 执行，stdout 必须只输出 JSON，其内容成为 `prepared`，失败中止调试。

`DebugWorkflow.resolve(workflow:JsonObject,context:Map<String,JsonElement>,rpc:suspend(String,JsonArray)->JsonElement):ResolvedDebugPlan`：三个参数必填，返回 launch:JsonObject、cwd:String、port:Int?。仅在调试准备阶段由授权 host 调用；外部 RPC 可挂起，取消应向上传播。最多32步；API3仅允许空发现结果的有界重试（retryEmpty 1..30），无动态代码。

workflow 必须恰含 steps:Array、launch:Object、port:null或引用。step 必填 id（`[a-z][a-zA-Z0-9]{0,31}`，不可覆盖上下文）、command:String、arguments:Array；可选 jsonArguments:Boolean 将复合参数编码为 JSON 字符串；select:{field:String,equals:JSON} 从 RPC 数组唯一选择一项；expect:JSON 要求结果相等。`${value.field.0}` 为整值引用，保留 JSON 类型；输入上下文为 source、project、sourceUri、projectUri，可有 prepared。没有字段/唯一项 E_WORKFLOW，expect 不符 E_BUILD，port 必须1..65535。见官方 Java Kit 的 main-class/build/classpath/debug-session 工作流，及 C/C++、Python 的声明式 launch。

Kit command 可以绑定 DAP_START 并声明 arguments.action：start（默认）、state、continue、pause、next、stepIn、stepOut、stop。state 同步 READ 返回 values.state；无所属会话返回 INACTIVE。控制命令仅作用于同 Kit 当前调试会话，缺失会话 E_CONTEXT，未知动作 E_COMPONENT；成功 REQUESTED 是请求受理，不是单步已完成。start 要求当前源文件匹配该 Kit 的 DAP adapter。这样第三方 `.suik` 可通过原生面板构建自己的调试操作区，不能操控其他包会话。

断点、单步、变量、调用栈、表达式求值使用原 DAP 客户端；调试准备可取消，禁用 Kit 或停止时取消准备并关闭适配器。PRoot ptrace 是否支持具体调试器须实机测试；不得以 Linux Docker 中的断点通过代替设备验收。当前 DAP 不向客户端声明 runInTerminal；交互标准输入的保证适用于 QuickRun PTY，DAP 使用 internalConsole 输出。

### Build、Test、Project 模板和第三方工作流

BUILD/TEST 和 Kit command/workflow 的 SIMPLE_RUN 使用 arguments.argv：非空 JSON 字符串数组，最多128参数；其他规则同 NativeRecipe。整值占位符支持 source/directory/project/resource/input，未知占位符失败，不执行原始拼接。Android 保存所有脏文件并等待完成，再通过同一任务引擎启动；忙时 E_BUSY。

工程模板是 PROJECT_READ 组件，arguments 必含 role="template"、title、description、files（目标相对路径→认证 resources/ 路径的 JSON 对象字符串）。宿主实际创建 additionally 要 project.read + file.write。`NativePackageHost.templates():List<NativeProjectTemplate>` 返回授权模板；`applyTemplate(template:NativeProjectTemplate,root:File):Unit` suspend，重新校验当前模板/会话，拒绝绝对路径、穿越、符号链接和任何现存目标。`TemplatePublisher.apply(root:File,files:Map<String,ByteArray>):Unit` 先全量预检并 staging，失败回滚本次新建内容；并发修改过的文件不得回滚覆盖。模板失败不删除用户工程或源码。

PROJECT_READ 默认返回 name/root；action=statistics 另返回 files/bytes。无选中 Project 时读取 Internal Files；只在该根内遍历普通文件，不跟随符号链接。第三方统计工具示例通过真正的此接口显示结果。更复杂分析可用用户授权的 argv workflow 运行自有 resources，不能假冒未实现的重构 API。所有超出 Basic 运行职责的组件必须 SUIK，SUIC 静态验证阶段直接拒绝。

### 官方目录协议与更新

index.json 必须 canonical UTF-8 JSON+LF，根字段仅 format=1、packages。最多256条，每条必填 id/name/version/kind/url/bytes/sha256/author/description/dependencies/changeLog/signer/signature/api。URL 为 HTTPS Fork 的 raw.githubusercontent.com 路径；bytes20..32MiB，hash 小写64位，signer 为 pinned P256 SPKI DER 摘要，signature 为包签名信息，api 为闭区间。index.sig 为 DER ECDSA SHA-256 签名，签名输入为 UTF-8 `SUI-LIBRARY-V1\n` + 原始 index 字节。先验证目录签名，再解释下载地址；实际包还须重新验证 size/hash/signature/ID/version。新版本须审阅新 digest 权限，不静默升级。官方私钥仅在维护者受控环境，绝不随 SDK/APK/源码分发。


## 附录：SUIC 完整 Manifest Schema

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "urn:suicode:suic:manifest:1",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "id": {
      "type": "string",
      "pattern": "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9-]*)+$"
    },
    "name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 1024
    },
    "author": {
      "type": "string",
      "minLength": 1,
      "maxLength": 1024
    },
    "description": {
      "type": "string",
      "minLength": 1,
      "maxLength": 1024
    },
    "version": {
      "type": "string",
      "pattern": "^(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})$"
    },
    "kind": {
      "const": "suic"
    },
    "api": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "min": {
          "type": "integer",
          "minimum": 1
        },
        "max": {
          "type": "integer",
          "minimum": 1
        }
      },
      "required": [
        "min",
        "max"
      ]
    },
    "permissions": {
      "type": "array",
      "items": {
        "enum": [
          "file.read",
          "process.run"
        ]
      },
      "uniqueItems": true
    },
    "dependencies": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "id": {
            "type": "string",
            "pattern": "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9-]*)+$"
          },
          "min": {
            "type": "string",
            "pattern": "^(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})$"
          },
          "max": {
            "type": "string",
            "pattern": "^(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})$"
          }
        },
        "required": [
          "id",
          "min",
          "max"
        ]
      }
    },
    "components": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "id": {
            "type": "string",
            "pattern": "^[a-z][a-z0-9.-]{0,63}$"
          },
          "type": {
            "enum": [
              "language",
              "simpleRun",
              "environment",
              "basicBuild"
            ]
          },
          "entry": {
            "type": "string",
            "pattern": "^plugin/[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)*\\.json$"
          }
        },
        "required": [
          "id",
          "type",
          "entry"
        ]
      }
    },
    "card": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "version",
        "category"
      ],
      "properties": {
        "version": {
          "const": 1
        },
        "category": {
          "const": "general"
        },
        "fields": {
          "type": "array",
          "maxItems": 8,
          "items": {
            "type": "object",
            "additionalProperties": false,
            "required": [
              "id",
              "label",
              "type",
              "value"
            ],
            "properties": {
              "id": {
                "type": "string",
                "pattern": "^[a-z][a-z0-9.-]{0,63}$"
              },
              "label": {
                "type": "string",
                "minLength": 1,
                "maxLength": 80,
                "pattern": "^[^\\u0000\\u202a-\\u202e\\u2066-\\u2069]*$"
              },
              "type": {
                "enum": [
                  "text",
                  "link"
                ]
              },
              "value": {
                "type": "string",
                "minLength": 1,
                "maxLength": 1024,
                "pattern": "^[^\\u0000\\u202a-\\u202e\\u2066-\\u2069]*$"
              }
            },
            "allOf": [
              {
                "if": {
                  "properties": {
                    "type": {
                      "const": "link"
                    }
                  }
                },
                "then": {
                  "properties": {
                    "value": {
                      "pattern": "^https://[^/@\\s]+(?:[/?#].*)?$"
                    }
                  }
                }
              }
            ]
          }
        }
      }
    }
  },
  "required": [
    "id",
    "name",
    "author",
    "description",
    "version",
    "kind",
    "api",
    "permissions",
    "dependencies",
    "components"
  ],
  "allOf": [
    {
      "if": {
        "required": [
          "card"
        ]
      },
      "then": {
        "properties": {
          "api": {
            "properties": {
              "min": {
                "minimum": 4
              }
            }
          }
        }
      }
    }
  ]
}
```

完整合法 Manifest：

```json
{"api":{"max":2,"min":2},"author":"sui-code examples","components":[{"entry":"plugin/language.json","id":"language","type":"language"}],"dependencies":[],"description":"Local development example; not an official publisher","id":"dev.suicode.example.minimal","kind":"suic","name":"minimal","permissions":[],"version":"0.1.0"}
```

## 附录：SUIK 完整 Manifest Schema

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "urn:suicode:suik:manifest:1",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "id": {
      "type": "string",
      "pattern": "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9-]*)+$"
    },
    "name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 1024
    },
    "author": {
      "type": "string",
      "minLength": 1,
      "maxLength": 1024
    },
    "description": {
      "type": "string",
      "minLength": 1,
      "maxLength": 1024
    },
    "version": {
      "type": "string",
      "pattern": "^(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})$"
    },
    "kind": {
      "const": "suik"
    },
    "api": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "min": {
          "type": "integer",
          "minimum": 1
        },
        "max": {
          "type": "integer",
          "minimum": 1
        }
      },
      "required": [
        "min",
        "max"
      ]
    },
    "permissions": {
      "type": "array",
      "items": {
        "enum": [
          "file.read",
          "file.write",
          "process.run",
          "language.lsp",
          "debug.dap",
          "project.read",
          "ui.panel",
          "ui.localization"
        ]
      },
      "uniqueItems": true
    },
    "dependencies": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "id": {
            "type": "string",
            "pattern": "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9-]*)+$"
          },
          "min": {
            "type": "string",
            "pattern": "^(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})$"
          },
          "max": {
            "type": "string",
            "pattern": "^(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})$"
          }
        },
        "required": [
          "id",
          "min",
          "max"
        ]
      }
    },
    "components": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "id": {
            "type": "string",
            "pattern": "^[a-z][a-z0-9.-]{0,63}$"
          },
          "type": {
            "enum": [
              "language",
              "simpleRun",
              "environment",
              "lsp",
              "dap",
              "build",
              "test",
              "format",
              "lint",
              "project",
              "workflow",
              "command",
              "panel",
              "setup",
              "localization"
            ]
          },
          "entry": {
            "type": "string",
            "pattern": "^ide/[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)*\\.json$"
          }
        },
        "required": [
          "id",
          "type",
          "entry"
        ]
      }
    },
    "card": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "version",
        "category"
      ],
      "properties": {
        "version": {
          "const": 1
        },
        "category": {
          "enum": [
            "language-ide-kit",
            "localization",
            "development-tool",
            "utility",
            "general"
          ]
        },
        "basicPlugin": {
          "type": "string",
          "pattern": "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9-]*)+$"
        },
        "languageIds": {
          "type": "array",
          "maxItems": 16,
          "uniqueItems": true,
          "items": {
            "type": "string",
            "pattern": "^[a-z][a-z0-9+_.-]{0,63}$"
          }
        },
        "slots": {
          "type": "object",
          "additionalProperties": false,
          "properties": {
            "runtime": {
              "type": "string",
              "pattern": "^[a-z][a-z0-9.-]{0,63}$"
            },
            "lsp": {
              "type": "string",
              "pattern": "^[a-z][a-z0-9.-]{0,63}$"
            },
            "dap": {
              "type": "string",
              "pattern": "^[a-z][a-z0-9.-]{0,63}$"
            },
            "build": {
              "type": "string",
              "pattern": "^[a-z][a-z0-9.-]{0,63}$"
            },
            "test": {
              "type": "string",
              "pattern": "^[a-z][a-z0-9.-]{0,63}$"
            },
            "run": {
              "type": "string",
              "pattern": "^[a-z][a-z0-9.-]{0,63}$"
            },
            "format": {
              "type": "string",
              "pattern": "^[a-z][a-z0-9.-]{0,63}$"
            },
            "lint": {
              "type": "string",
              "pattern": "^[a-z][a-z0-9.-]{0,63}$"
            }
          }
        },
        "fields": {
          "type": "array",
          "maxItems": 8,
          "items": {
            "type": "object",
            "additionalProperties": false,
            "required": [
              "id",
              "label",
              "type",
              "value"
            ],
            "properties": {
              "id": {
                "type": "string",
                "pattern": "^[a-z][a-z0-9.-]{0,63}$"
              },
              "label": {
                "type": "string",
                "minLength": 1,
                "maxLength": 80,
                "pattern": "^[^\\u0000\\u202a-\\u202e\\u2066-\\u2069]*$"
              },
              "type": {
                "enum": [
                  "text",
                  "link"
                ]
              },
              "value": {
                "type": "string",
                "minLength": 1,
                "maxLength": 1024,
                "pattern": "^[^\\u0000\\u202a-\\u202e\\u2066-\\u2069]*$"
              }
            },
            "allOf": [
              {
                "if": {
                  "properties": {
                    "type": {
                      "const": "link"
                    }
                  }
                },
                "then": {
                  "properties": {
                    "value": {
                      "pattern": "^https://[^/@\\s]+(?:[/?#].*)?$"
                    }
                  }
                }
              }
            ]
          }
        },
        "actions": {
          "type": "array",
          "maxItems": 8,
          "uniqueItems": true,
          "items": {
            "type": "string",
            "pattern": "^[a-z][a-z0-9.-]{0,63}$"
          }
        }
      },
      "allOf": [
        {
          "if": {
            "properties": {
              "category": {
                "const": "language-ide-kit"
              }
            }
          },
          "then": {
            "properties": {
              "fields": {
                "maxItems": 0
              }
            }
          },
          "else": {
            "not": {
              "required": [
                "basicPlugin"
              ]
            },
            "properties": {
              "languageIds": {
                "maxItems": 0
              },
              "slots": {
                "maxProperties": 0
              }
            }
          }
        }
      ]
    }
  },
  "required": [
    "id",
    "name",
    "author",
    "description",
    "version",
    "kind",
    "api",
    "permissions",
    "dependencies",
    "components"
  ],
  "allOf": [
    {
      "if": {
        "properties": {
          "components": {
            "contains": {
              "type": "object",
              "properties": {
                "type": {
                  "const": "localization"
                }
              },
              "required": [
                "type"
              ]
            }
          }
        }
      },
      "then": {
        "properties": {
          "api": {
            "properties": {
              "min": {
                "minimum": 4
              }
            }
          },
          "permissions": {
            "contains": {
              "const": "ui.localization"
            }
          }
        }
      }
    },
    {
      "if": {
        "required": [
          "card"
        ]
      },
      "then": {
        "properties": {
          "api": {
            "properties": {
              "min": {
                "minimum": 4
              }
            }
          }
        }
      }
    }
  ]
}
```

完整合法 Manifest：

```json
{"api":{"max":2,"min":2},"author":"sui-code examples","components":[{"entry":"ide/overview.json","id":"overview","type":"panel"}],"dependencies":[],"description":"Local development example; not an official publisher","id":"dev.suicode.example.minimal-kit","kind":"suik","name":"minimal-kit","permissions":["ui.panel"],"version":"0.1.0"}
```


## 28. API 3：IDE Kit 依赖部署协议（规范 0.3.0）

API 1/2 的声明及授权保留。新增 `setup` 仅属于 `.suik`，Manifest `api.min` 必须 ≥3；`.suic` 使用它必须报 E_BOUNDARY。包格式仍为 1.0，不重命名 ZIP 充当独立格式。安装资源属于已签名/校验的资源文件，不会在导入、验证或激活时自动执行。

### 28.1 完整组件 Schema

下列 Schema 与 `schemas/setup.schema.json` 一致。除 JSON Schema 外，生产解析器还强制唯一 ID、文件/资源路径不能含 `.`/`..` 或通配符、至少一个探测条件、provider 与专用字段一致、资源必须确实在已认证 payload 中。`environment` 要求 environmentId，禁止 resource/packages；`ubuntu-apt` 要求非空 packages，禁止 environmentId/resource；`kit-resource` 要求 resource，禁止 environmentId。一个包最多一个 setup。

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "urn:suicode:setup:3",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "requirements": {
      "type": "array",
      "minItems": 1,
      "maxItems": 32,
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "id": {
            "type": "string",
            "pattern": "^[a-z][a-z0-9.-]{0,63}$"
          },
          "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 1024
          },
          "version": {
            "type": "string",
            "minLength": 1,
            "maxLength": 1024
          },
          "source": {
            "type": "string",
            "pattern": "^https://[^/]+",
            "maxLength": 1024
          },
          "provider": {
            "enum": [
              "environment",
              "ubuntu-apt",
              "kit-resource"
            ]
          },
          "files": {
            "type": "array",
            "maxItems": 64,
            "uniqueItems": true,
            "items": {
              "type": "string",
              "pattern": "^/(opt/sui-code|usr|bin|lib)/[A-Za-z0-9_./+*-]+$"
            }
          },
          "commands": {
            "type": "array",
            "maxItems": 64,
            "uniqueItems": true,
            "items": {
              "type": "string",
              "pattern": "^[A-Za-z0-9][A-Za-z0-9+_.-]{0,79}$"
            }
          },
          "anyCommands": {
            "type": "array",
            "maxItems": 64,
            "uniqueItems": true,
            "items": {
              "type": "string",
              "pattern": "^[A-Za-z0-9][A-Za-z0-9+_.-]{0,79}$"
            }
          },
          "packages": {
            "type": "array",
            "maxItems": 64,
            "uniqueItems": true,
            "items": {
              "type": "string",
              "pattern": "^[a-z0-9][a-z0-9+.-]{0,79}$"
            }
          },
          "bytes": {
            "type": [
              "integer",
              "null"
            ],
            "minimum": 0
          },
          "environmentId": {
            "enum": [
              "java",
              "cpp",
              "c",
              "python"
            ]
          },
          "resource": {
            "type": "string",
            "pattern": "^resources/[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)*$"
          },
          "dependsOn": {
            "type": "array",
            "maxItems": 32,
            "uniqueItems": true,
            "items": {
              "type": "string",
              "pattern": "^[a-z][a-z0-9.-]{0,63}$"
            }
          },
          "pythonModules": {
            "type": "array",
            "maxItems": 64,
            "uniqueItems": true,
            "items": {
              "type": "string",
              "pattern": "^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*$"
            }
          }
        },
        "required": [
          "id",
          "name",
          "version",
          "source",
          "provider",
          "files",
          "commands",
          "packages",
          "bytes"
        ]
      }
    }
  },
  "required": [
    "requirements"
  ]
}
```

合法 Manifest 和组件示例见 `examples/dependencies-kit/`（可直接打包验证）。Java/C_CPP/Python 的正式依赖定义见 native-library/src/*/suik/adapters/setup.json。未知字段失败 E_MANIFEST，未知 provider/无效条件 E_COMPONENT，越界路径 E_PATH；不允许 fallback 成任意用户 shell。

### 28.2 字段语义与执行边界

|字段|类型/必填|含义与失败行为|
|---|---|---|
|requirements|Array<KitRequirement>，必填1..32|唯一 ID，按声明顺序部署；运行环境在开发工具前|
|id/name/version|String，均必填|ID 为稳定组件键；version 是显示的已安装/所需版本说明，不自动放入 shell|
|source|HTTPS URL，必填|来源审核信息；APT 使用用户已配置且签名有效的 Ubuntu 仓库，不自行加入仓库、不关闭 TLS/GPG|
|provider|枚举，必填|environment 调用已有原生环境安装流程；ubuntu-apt 部署声明的包；kit-resource 执行认证资源|
|files|Array<String>，必填|绝对 guest 文件只读存在性检查；不执行其内容|
|commands|Array<String>，必填|所有命令必须可发现，Core 生成 command -v，无包提供的探测脚本|
|anyCommands|Array<String>，可选默认[]|至少一个可发现，适用于带版本的 lldb-dap 可执行文件|
|packages|Array<String>，必填|dpkg 已配置状态；安装时 apt 验证签名元数据和哈希，现有包不自动升级|
|environmentId|String，environment 必填|当前宿主注册 java、c、cpp、python；复用现有环境文件和安装器，不重新封装语言发行版|
|resource|String，kit-resource 必填|认证 payload 中 resources/ 路径；统一确认后交给既有 Ubuntu Setup PTY|
|bytes|Long?，必填|已知下载字节数；未知必须 null 并明确显示未知，不能显示0假装无下载|

### 28.3 生产接口

扩展作者提交声明，不能直接获取 Android 对象。以下是相同生产 SDK 中的宿主接口：

|接口|参数/返回|时机、权限、失败和兼容性|
|---|---|---|
|`KitRequirements.parse(d:JsonObject):List<KitRequirement>`|完整 setup 描述→不可执行的解析结果|安装验证和计划前；无权限/进程副作用；严格边界失败抛 PackageFailure；API3|
|`KitRequirements.forPack(pack:VerifiedPack):List<KitRequirement>`|认证包→全部前置条件|仅数据读取，无环境安装；API3|
|`VerifiedPack.resourcePaths():List<String>`|无参→排序的认证 resources/ 名称|宿主按认证 payload 部署临时资源，不接受未认证磁盘路径；API3|
|`DebugWorkflow.resolve(workflow:JsonObject,context:Map<String,JsonElement>,rpc:suspend(String,JsonArray)->JsonElement):ResolvedDebugPlan`|返回 launch:JsonObject、cwd:String、port:Int?|宿主在已获 DAP、RUN、FILE_READ 授权的后台协程调用；RPC 和取消由 host 执行；未就绪/多目标/编译失败给明确 E_WORKFLOW/E_BUILD|

Android 生产宿主 `NativeKitSetup` 实现 `inspect(pack:VerifiedPack,force:Boolean=false):List<KitDependencyStatus>`（IO、60秒缓存、可取消）；`prepare(preview:PackagePreview):KitSetupPlan`（IO、只形成计划）；`install(plan,grants:Set<Capability>,allowUnverified:Boolean):Unit`（启动应用生命周期安装任务）；`cancel():Unit`（请求安全边界取消）；`state:StateFlow<KitSetupState>`（进度、详细日志路径、Ready/Partially Ready）。这些 Android 接口不导出成第三方任意代码运行 API。

计划包括所有待装包的精确摘要、签名、来源及权限并列出每项现状、版本、架构、可用大小和执行行为。确认授权必须绑定精确包摘要。缺失的官方 Basic 包在同一计划内列出并安装；本地未知依赖必须先导入审阅，不能静默从第三方市场补齐。依赖脚本仅在统一确认后执行，必须授予 file.read/file.write/process.run，且所有依赖的权限都在同一次审阅中列出。

取消不强杀 apt/dpkg，不删除锁；等待当前包事务结束后停止下一步。已成功安装的共享工具保留，不能通过回滚包破坏其他 Kit。包状态仍用 PackageStore 原子提交并保留前一版本；失败保留原已验证包/数据和诊断日志；Partially Ready 列出未就绪项并提供相同计划的重新审阅与重试。禁用/卸载 Kit 不卸载语言环境或共享工具。

### 28.4 DAP 数据流与源路径

步骤可选 `retryEmpty:Int=1`（1..30）仅针对空数组发现结果每秒重试，协程取消立即传播；`select` 必填 field:String、equals:JsonElement，可选 path:Boolean=false、fallbackSingle:Boolean=false。path 按 URI 解码和 Unix Path.normalize 比较，不能使用 URLDecoder 的加号转空格规则。精确匹配唯一目标优先；仅在候选总数为1且 fallbackSingle=true 时回退。空结果报尚未索引/无入口；多结果列明候选、要求打开目标入口文件，禁止随意选择第一个。

LSP 使用 UTF-16 坐标和正确编码的 file URI，发请求前冲刷 debounced didChange；取消发 $/cancelRequest。Kit 可在 initializationOptions.virtualDocuments 中声明 scheme→LSP method（例如 jdt→java/classFileContents）；虚拟源码缓存在 app cache，以只读编辑器打开，不写回 JDK。project build、LSP、DAP 均使用稳定 `/workspace/...` 路径；临时 `/sui-quickrun` 仅可用于 Basic 单文件，不得写入持久 CMake 缓存。旧的 Kit 自有 build/debug 缓存迁移为带时间戳备份，用户源码不删。

### 28.5 0.3.0 变更及回归

新增 setup 组件、工具就绪状态、统一审阅流程、API3 SDK 和依赖示例。API1/2包继续加载；新字段不得假装 API2 兼容。所有主机、Schema、示例、签名包和本文同步提交。SDK 的 API-SOURCE-SHA256SUMS 必须对应生产源码。测试区分纯 JVM、Ubuntu x64 协议和 Android ARM64 实机；未连接设备时不能宣称 ANR 或断点实机验收通过。

`PackageVerifier.verify(bytes:ByteArray,suffix:String,installed:Map<String,String>=emptyMap(),checkDependencies:Boolean=true):VerifiedPack`：API3新增可选 checkDependencies。默认仍执行全部已安装依赖版本检查；仅宿主的隔离审阅/计划流程可传 false，此时仍验证依赖字段、区间、包身份、签名、哈希和职责边界，但不把预览视为可激活。PackageStore.install/load/enable始终使用true；禁止用预览跳过实际安装检查。第三方SDK的PackageTool仍按默认严格验证。

API3 `setup` 的 Manifest 还必须声明 file.read/file.write/process.run，否则验证阶段报 E_PERMISSION，不能等到确认按钮触发异常。`project` 组件 arguments 可选 `markers:String="[]"`（内容为相对文件名 JSON 数组）；用于没有编辑器源文件时识别可选 Project 上下文。宿主忽略含空段、`.`/`..`、绝对路径或无效字符的 marker，不允许越出 Project。此字段不创建 Project、不执行脚本，缺省时可继续从已打开源文件匹配 Kit。Java/C_CPP/Python 正式模板分别声明 pom.xml、CMakeLists.txt、main.py。Kit 安装结束会串行更新服务注册、刷新与探测目录并重试现有编辑文件，无需手动进入 Servers 再安装。

## 29. API 4：独立 UI 本地化资源（规范 0.4.0）

这是生产 `Localization.kt` / Android NativePackageHost 的实际协议。包头、SUIC/SUIK容器版本仍为1.0。旧API1/2/3 Kit及其授权不变；Core API4是新增声明能力，不要求更新现有语言环境或重装Kit。`.suic`不能声明localization；`.suik`可只有本地化组件，也可同时含LSP/DAP/项目/面板等声明。语言资源不自动获得其他组件权限。

### 29.1 Manifest、描述符及资源

新增组件 `{"id":"strings","type":"localization","entry":"ide/strings.json"}`，Manifest `api.min >= 4`，权限包含`ui.localization`。没有隐含的process.run/file.write/project.read，也没有隐含语言环境依赖。一个包同一locale只能有一个组件。完整可安装示例为`examples/zh-hans-kit`，预构建文件为`examples/packages/zh-hans-kit.suik`；它仅是部分翻译的未签名开发样例，不是正式完整中文版。

**完整合法 Manifest：**

```json
{
  "id": "dev.suicode.example.zh-hans",
  "name": "Simplified Chinese localization example",
  "author": "sui-code SDK examples",
  "description": "Partial development fixture, not a complete Chinese translation or official signed language pack.",
  "version": "0.1.0",
  "kind": "suik",
  "api": {
    "min": 4,
    "max": 4
  },
  "permissions": [
    "ui.localization"
  ],
  "dependencies": [],
  "components": [
    {
      "id": "strings",
      "type": "localization",
      "entry": "ide/strings.json"
    }
  ]
}
```

**完整描述符 Schema（Draft2020-12）：**

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "locale": {
      "type": "string",
      "pattern": "^[a-z]{2,3}(-[A-Za-z0-9]{2,8}){0,3}$",
      "not": {
        "const": "en"
      }
    },
    "displayName": {
      "type": "string",
      "minLength": 1,
      "maxLength": 80
    },
    "fallback": {
      "const": "en"
    },
    "resource": {
      "type": "string",
      "pattern": "^resources/i18n/[A-Za-z0-9_-]+\\.json$"
    }
  },
  "required": [
    "locale",
    "displayName",
    "fallback",
    "resource"
  ]
}
```

描述符必填字段：`locale:string`为大小写规范化BCP47（如zh-Hans、zh-CN），Java Locale规范化后必须相同；保留en不能覆盖。`displayName:string`长度1..80，纯文本，不得含控制字符或双向重排控制符。`fallback:string`当前只能为en，禁止循环/跨包回退。`resource:string`必须匹配resources/i18n/<ASCII文件名>.json；资源来自已认证包内容，禁止外部文件/URL/路径遍历。未知字段拒绝。

**完整资源 Schema：**

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "additionalProperties": false,
  "minProperties": 1,
  "maxProperties": 732,
  "properties": {
    "nav.files": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "nav.find": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "nav.languages": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "nav.packages": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "nav.settings": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.internal": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.filter": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.filterHint": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.matches": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.newFile": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.newFolder": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.refresh": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.more": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.content": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.names": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.current": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.query": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.scope": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.hint": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "run.file": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "run.stop": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "run.debug": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "run.continue": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "run.stopDebug": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "run.overview": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "project.build": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "project.test": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "project.run": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "project.tasks": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "packages.title": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "languages.title": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.language": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.english": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.fallback": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.empty": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.aTermuxStyleKeyRowEscTabCtrlArrows": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.actionButton": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.addProject": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.addToWorkspace": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.addVariable": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.addsJCodeSLogcatOutputIncludingProotAndThe": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.addsAnExtDevTabToTheRightPanel": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.advancedEnvironmentAndServerControls": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.advancedTasksJcodeRunYaml": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.allowJCodeToInstallAppsThenTapUpdateAgain": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.android12KillsAnAppSForkedProcessesOnce": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.androidCapsHowManyProcessesAnAppMayFork": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.androidDevice": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.androidStoppedTheLinuxEnvironmentBackgroundProcessLimit": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.appVersionAndUpdatesFromGitHubReleases": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.appearance": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.appendF1F12ChipsToTheRowWhileA": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.appendTheStackTraceWhenTheAppCrashesSo": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.autoSetupEnvironment": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.autoCloseIdleTerminals": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.automaticallyCloseTerminalsLeftIdleAtThePromptNo": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.back": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.backUpTarGz": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.backUpTheWholeLinuxEnvironment25GB": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.backgroundProcessLimit": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.backupRestore": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.basicRunSetup": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.bottomStatusBar": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.browserBack": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.browserForward": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.browserReloadPage": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.browserStopLoading": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.buildLogsAndToolOutputWillAppearHere": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.builtInRuleBasedFormatter": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.callStack": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.cancel": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.cancelInstallationSafely": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.cancelSafely": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.changeTabColor": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.changeManagedJDK": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.checkForUpdates": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.checkTrustedSourceUpdates": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.checking": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.chooseFile": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.chooseHowTheFileExplorerIsLaidOutApplies": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.chooseProvider": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.chooseWhichBuiltInCommandsThePaletteOffersContext": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.cleanUpAndUninstall": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.clear": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.clearOutput": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.clearSearch": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.close": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.closeSaved": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.closeUnbusy": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.closeAll": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.closeAnyway": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.closeAppFullyOnSwipeAway": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.closeOthers": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.closeProject": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.closeProjectText2c6af4": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.closeSavedProjectGroup": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.closeTab": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.closeTerminal": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.closeToTheRight": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.closeWorkspace": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.collapse": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.collapseAll": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.colorSearchPickFromScreen": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.colorPaletteAppliedAcrossTheApp": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.colorCodeEditorFileTabsLongPressAFile": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.commandPalette": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.completeUbuntuSetupBeforeInstallingJava": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.completeUbuntuSetupFirstExistingInstallationsAndJavaData": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.configurationErrorsAndOnSaveSyntaxChecksAppearHere": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.configureThisProjectThenCreateIt": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.confirm": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.confirmAndOpenTerminal": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.copy": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.copyCommands": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.couldNotReadTheFile": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.create": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.current": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.cursorDragSpeedHorizontal": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.cursorDragSpeedVertical": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.cut": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.cutMove": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.databases": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.debugSession": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.decreaseEditorFontSize": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.defaultFontSizeAndWordWrapForTheCode": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.delete": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.deleteForever": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.deleteProjectFilesToTrash": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.deleteProjectFiles": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.deletingAFileOrFolderOrDiscardingAChange": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.detail": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.details": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.devServerDidnTStartInTimeCheckThe": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.developerOptions": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.developmentToolsCheckingWhenAKitIsInstalled": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.deviceMemory": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.diagnosticLogExported": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.diagnosticLogging": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.disable": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.discard": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.dismiss": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.displayCutout": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.distro": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.downloadAndReview": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.dragAFingerOnTheEditorToMoveThe": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.dragTheDividerOrUseThisKeyboardAccessibleSlider": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.dragToMoveCursor": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.eNVVAR": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.edit": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.editVariable": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.editorBehavior": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.editorDefaults": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.editorFont": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.editorGestures": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.emptyTextc6c094": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.emptyTrash": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.emptyTheTrash": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.enable": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.enableDeveloperOptions": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.enterFullscreen": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.environment": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.environmentSetupProotDistroBootstrapAndTheFinalSmoke": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.environmentVariables": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.environmentWarning": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.excludeFilesFolders": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.excludeFilesAndFoldersAtTheProjectRootIn": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.exitFullscreen": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.exitJCode": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.expand": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.explorer": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.exportForMigration": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.exportSettings": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.exportToStorage": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.exportedFile": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.exportedIntoEveryTerminalAndBuildRunSessionE": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.export": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.extensions": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.extraKeysRow": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.fILES": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.fallBackToTheAppWideDefault": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.fileIcons": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.fileProperties": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.filesRemainRecoverableFromTrashRegisteredProjectRootsCannot": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.findText": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.finishDistroSetupFirstThenOpenTheTerminalAgain": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.finishEnvironmentSetupBeforeBuilding": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.finishEnvironmentSetupBeforeOpeningTheTerminal": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.finishEnvironmentSetupBeforeRunning": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.focusExplorer": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.folderExportSupportsLocalFoldersOnly": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.fontSize": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.fonts": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.formatDocument": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.formatter": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.formatterExtension": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.functionKeys": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.gLOBAL": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.getLatestWebView": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.gitSubmodule": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.goToLine": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.goToLineText77922d": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.goodDefaultsAre24Or8SpacesDepending": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.hardwareAcceleration": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.header": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.hideHeaderAndTabs": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.hideDetails": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.hideInstallationLog": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.hideLeftSidebar": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.hideRecentProjects": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.hideTabCloseButton": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.howJCodeDrawsTheUIEditorAndTerminal": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.howEditorAndTerminalTabsBehaveAppliesAppWide": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.howLongAToolchainInstallSDKLanguageServerDebugger": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.howTheRenderedMarkdownPreviewLaysOut": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.howTouchInputBehavesInTheEditorAppliesApp": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.iconSetUsedForFilesAndFoldersInThe": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.iconSetUsedForTheAppSOwnToolbars": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.idleTimeout": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.ifTheProjectIsUnderGitYouCanRestore": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.importSuicSuik": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.importAnExtension": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.importFiles": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.importFilesTextc98b4f": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.importFromPreviousInstall": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.importSettings": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.importingFolder": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.includeTheSystemLog": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.increaseEditorFontSize": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.install": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.installJavaBasicPluginFromTheSuiCodePlugin": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.installUpdate": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.installAnyway": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.installTheBasicLanguagePluginFromPluginCenterIDE": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.installationLog": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.installedJDKs": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.installingNowGivesYouTheNewVersionWithNothing": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.installing": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.itWasAddedToThisWorkspaceOpenItNow": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.jCodeSOwnFolderAndFileGlyphsFromThe": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.jCodeSOwnInEditorBrowserWithDevTools": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.javaLatestGA": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.keepAwakeTurnOff": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.keepAwakeTurnOn": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.keepDeletedFilesFor": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.keepEnabledForTheEditorSurfaceButLetUsers": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.keepForNow": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.keepTheLinuxRuntimeLeanByStoppingWorkYou": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.keepTheAppClearOfTheCameraNotchOr": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.kill": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.killProcess": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.kitCancellationRequestedSetupStaysOpenUntilTheCurrent": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.landscape": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.layOutTheAppInsideTheCutoutSSafe": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.legacyKitReviewUpdatedPrerequisites": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.lettersDigitsAndUnderscoreOnlyCanTStartWith": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.ligatures": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.linuxProcesses": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.location": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.lockScreenOrientation": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.manageDevice": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.manageEnvironments": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.markdownPreview": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.maxTerminalInstances": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.mode": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.modernSitesCanRenderBlankOrBrokenOnThis": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.monospaceTypefacesForTheCodeEditorAndTheTerminal": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.moreActions": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.moveDeletedFilesToTrash": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.moveProjectToTrash": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.moveToTrash": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.movingToADifferentlyNamedBuildWriteTheEnvironment": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.nativeUbuntuEnvironmentSignedDistributionPackages": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.nativeLanguageEnvironmentEclipseTemurinOpenJDK": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.newProject": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.newFile": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.newFolder": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.newTerminal": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.next": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.noFileOpen": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.noFilesYetUseTheNewFileNewFolder": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.noIssuesDetected": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.noNativePackagesInstalledChooseALibraryOrImport": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.noProjectYetAddOneWithAboveOrOpen": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.noRecentProjectsYet": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.noTerminalSessions": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.noVariablesYetTapAddVariableToCreateOne": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.noVirtualDevice": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.notNow": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.nothingRecordedYet": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.nothingRunningTerminalsBuildRunAndDebugSessionsAppear": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.oK": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.offUnlessYouTurnItOnWhenSomethingMisbehaves": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.offAPortraitPreviewLaysOutAtLandscapeWidth": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.olderItemsAreRemovedWhenJCodeStartsAndWhen": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.open": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openBrowser": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openExplorer": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openFolder": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openProject": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openProjectYAML": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openSettings": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openVirtualDevice": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openWorkspaceYAML": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openAFileToStartEditing": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openASourceFileForLanguageActionsProjectWorkflows": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openFile": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openFolderText6a9084": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openFolderAsProject": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openProjectYAMLText26247e": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openSavedProjectGroup": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openSetup": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openSharedFolderYAMLAdvanced": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openTheBackingConfigFilesDirectlyWhenYouWant": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.openWebPreviewsIn": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.outdated": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.overridesTheAppLevelDefaultForThisScope": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.pROJECT": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.pROJECTS": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.packageCancellationRequestedSetupStaysOpenUntilAptDpkg": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.pairJCodeWithThisPhoneSOwnAdbSo": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.paste": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.pasteImage": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.pickUpWhereYouLeftOffAfterClosingThe": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.pin": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.pinned": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.portrait": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.prerequisitesNotCheckedThisSession": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.pressBackAgainToExit": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.preview": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.project": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.projectYAMLWarning": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.projectActions": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.projectFiles": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.projectName": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.projectSettings": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.projectRootFoldersMustBeManagedThroughProjects": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.projectsOptionalContext": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.propertiesFullName": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.propertiesFullNames": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.provider": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.queryUbuntuVersionsRetry": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.queryLatestGARetryQuery": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.queryLatestOfficialPythonStableVersion": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.quickRunOpenAJavaFileInTheEditorAnd": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.readingContents": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.readingTheTrash": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.readyRuntimeAndAllRequiredToolsDetected": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.recentDiagnostics": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.recentProjectActions": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.reconnectTheVirtualDeviceToAdb": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.recordCrashes": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.recordDiagnostics": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.recorded": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.redo": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.refreshChecks": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.refreshInstalledJDKs": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.refreshPackageListsAndUpgradeInstalledPackagesAptGet": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.refreshReadiness": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.reload": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.remapTheHardwareVolumeButtonsToEditorTerminalActions": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.remove": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.removeFromListKeepFiles": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.removeInactiveJDK": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.removeInactiveJDKText012cf8": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.removeOnlyClearsTheRecentListFilesStayIntact": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.removeTheBundle": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.removesTheOnEditorAndTerminalTabsToAvoid": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.rename": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.renderTheUIEditorAndTerminalOnTheGPU": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.rendering": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.reopenTheLastWorkspaceProjectAndEditorTabsIncluding": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.resizeEditorAndPanel": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.resourceManagement": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.respectDeviceCutout": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.restart": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.restore": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.restoreLastSession": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.restoreLastSessionOnLaunch": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.restoreText8ff034": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.reviewRepairDependencies": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.reviewUbuntuPackageUpdates": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.reviewAndInstallRetry": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.reviewDependenciesRepair": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.reviewUpdateReinstall": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.rollback": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.runInBackground": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.runningProcess": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.runningProcesses": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.save": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.saveUnsavedChanges": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.saveAll": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.saveYourAppPreferencesToAFileThenRestore": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.scanningFolder": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.searchSettings": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.select": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.selectText": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.selectAll": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.selectAllVisible": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.selected": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.selectionActions": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.serial": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.setUpADB": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.setUpCompleteIDEKit": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.settingsExported": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.setupLog": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.sharedFolderYAMLWarning": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.show": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.showHeaderAndTabs": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.showSearchPlaceholder": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.showDetails": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.showHeaderAndTabsText2f5a6b": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.showLeftSidebar": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.showTheAndroidAppChooserEachTime": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.smokeTest": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.sourceControl": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.specifiedOnePatternPerLine": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.startADebugSessionToSeeItsConsoleOutput": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.startDebugging": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.startExtension": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.stepInto": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.stepOut": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.stepOver": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.stop": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.stopExtension": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.subShellsOpenInTheirOwnTab": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.switchEnvironment": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.systemFollowsYourDeviceSLightDarkSetting": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.tabColoring": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.tabSize": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.tabWidth": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.tabs": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.tapToStartANewTerminal": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.template": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.terminal": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.terminalFont": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.terminalPanel": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.terminalUnavailable": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.terminalOutputIssuesAndDebuggingShareTheBottomPanel": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.terminateExit": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.textSizeForTerminalSessionsAppliesToEveryOpen": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.textSizeInsideImportedVsixExtensionsAScaleRather": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.theChromiumEngineBehindJCodeSBuiltInBrowser": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.theBarAcrossTheTopOfTheWorkbenchWith": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.theBarAtTheBottomOfTheWorkbenchShowing": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.theBrowserThisProjectUsesWhenYouOpenA": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.theBrowserUsedWhenYouOpenARunningDev": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.theButtonBesideRunHidingItLeavesTheTerminal": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.theDeviceSDefaultBrowserApp": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.theMostAnEditorOrTerminalTabWidensBefore": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.theRootFolderIsPermanentManageProjectNamesThrough": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.themeBundle": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.theseControlsWriteBackToYAMLAndUpdateThe": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.theseSettingsPreserveTheSharedFolderJcodeConfigurationAnd": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.theseSettingsSaveToTheProjectJcodeAndOnly": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.toggleRightSidebar": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.toolchain": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.toolchainInstallTimeout": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.toolsForBuildingAndTestingJCodeExtensions": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.trash": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.trashRestore": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.treeShowsTheWholeProjectHierarchyListIsA": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.tryAShorterOrDifferentTermLikeFontOr": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.uIIcons": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.uILanguageResourcesNoLanguageEnvironmentRequired": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.unavailableThisMilestoneRequiresALinuxARM64Environment": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.undo": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.uninstall": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.uninstallPackage": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.unlockScreenOrientation": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.unpin": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.unsavedChanges": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.upOneLevel": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.upToDate": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.update": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.updateSystemPackages": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.updateWithoutYourEnvironment": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.updatedKitAvailableReviewItsSignedDependencyPlanTo": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.updatingPackages": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.value": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.variables": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.verifyAndSelect": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.verifySelectedEnvironment": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.view": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.viewMode": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.virtualMachines": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.volumeDown": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.volumeKeys": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.volumeUp": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.warnBeforeClosingRunningProcesses": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.webEngine": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.whenClosingAProjectOrWorkspaceWithARunning": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.whenExcluded": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.whenYouStartAnInteractiveShellBashZshInside": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.whenYouSwipeJCodeOffTheAndroidRecentsScreen": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.whereDeletedFilesGoBeforeTheyAreGoneCovers": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.whichFormatterTheEditorUsesBuiltInIsRule": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.wordWrap": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.wordWrapInPortrait": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.wrapLongLinesToTheEditorWidthInsteadOf": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.writesAppEventsToALogFileOnThis": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.yAMLFiles": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.previewTruncatedTheWholeFileIsRestoredIntact": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.languageReadiness": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.enabled": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.disabled": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.notInstalled": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.optionalNotInstalled": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "common.version": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "common.source": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "common.file": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "common.download": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "common.installLocation": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "common.environment": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "common.architecture": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.imported": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.importFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.exportFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.exportedFrom": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.copied": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.cut": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.created": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.createFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.renamed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.renameFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.deleteFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.movedToTrash": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.deleted": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.doneSelection": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.sort": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.trashSelected": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.deleteNamed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.pasted": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.trashPaused": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.pasteStopped": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "debug.target": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "debug.console": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "debug.missing": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "issues.position": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "project.deleteDescription": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "project.openDescription": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "project.importProgress": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "project.cloned": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "languages.context": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "languages.basicStatus": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "languages.kitStatus": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "languages.partial": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "packages.sourceContext": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "packages.metadata": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "packages.dependencies": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "packages.installedStatus": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "packages.record": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "packages.removeDescription": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "settings.panelHeight": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "settings.running": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "settings.updateVersion": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "settings.downloading": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "settings.noMatches": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "settings.collapse": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "settings.expand": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "settings.icons": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "settings.variableExists": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Input": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Startup": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Performance": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Webpreview": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.About": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Diagnostics": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Editor": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Developer": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.SpecifiedByInjected": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.ByInjectedonly": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Off": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Greyout": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Hide": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Hidden": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Withkeyboard": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Always": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.HideonSoftKeyboard": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.AlwaysShow": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Randomifnotexistthenremember": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Random": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Directorybasedthenremember": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Disabled": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.KeyLeft": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.KeyRight": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.KeyUp": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.KeyDown": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.ScrollUp": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.ScrollDown": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.System": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Light": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Dark": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Ready": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Notinstalled": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Unknown": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Notready": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Passed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Failed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Notrun": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Starting": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Running": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Paused": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Error": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.disconnected": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.terminated": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Tree": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.List": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.PluginLibrary": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.IDEKitLibrary": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Installed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Output": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Issues": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Tasks": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Modified": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "option.Size": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "settings.systemVolume": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.placeholderContent": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.placeholderNames": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.placeholderCurrent": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.openCurrent": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.minimum": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.searching": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.none": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.namesSummary": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.contentSummary": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "find.failed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.trashDescription": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.deleteDescription": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "trash.untilEmpty": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "trash.days": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.serverMissing": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.updateAvailable": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.updateFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.updateInstalled": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "status.importedSettings": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.unsavedNamed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.unsavedList": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "run.finishSetup": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "run.kitMissing": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.properties": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.kindDirectory": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "files.kindFile": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "packages.checking": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "packages.installingResources": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "packages.resourcesReady": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "trash.emptyDescription": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "project.createFolderHint": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "project.saveFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "project.templateUnavailable": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Debugcancelledfilescouldnotbesaved": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Putthecursoronasymboltorename": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Renamefailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Languageserverreturnednoformattingedits": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Saveorcloseopenfilesbeforeimportinga": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Theimportedfolderisnolongeravailable": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.RemovedfromtheprojectlistFilesremainin": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.ProjectfilesmovedtoTrashRestorefromFiles": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Projectclosed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Renamefaileddestinationexistsortheprojectis": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Cantcolorafileoutsidetheproject": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Workspaceconfigisunavailable": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Projectoverridesrequirealocalproject": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Finishorsafelycancelpackageinstallationbeforerestarting": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Restartpostponedwaitforaptdpkgtoreach": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Nounsavedchanges": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Nothingtoformat": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Alreadyformatted": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Formattingreplydiscardedbecausethefilechanged": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Restoretheremovedfilebeforesavingopenedits": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Saved1file": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.Projectoverridesrequirealocalprojectroot": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.noDefinition": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.noReferences": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.cannotRename": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.sourceLoadFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.fileMissing": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.applyEditsFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.formatted": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.templateFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.opened": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.readFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.emptyImport": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.importFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.importedFolder": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.projectDeleteStopped": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.renamed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.renameRejected": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.exportNotLocal": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.exporting": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.exported": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.exportFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.writeFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.bufferRetained": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.reloaded": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.reloadedMany": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.cannotDiscard": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.discardFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.discarded": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.saved": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.readOnly": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.cannotSave": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.saveFailed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.savedMany": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "message.fileGone": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "project.created": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Search": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Use": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Reinstall": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Working": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Resetthissetting": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Tabcolor": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Environmentsetup": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.PickaLinuxdistroandJCodeconfigurestherestautomatically": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Import": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Setupfreshinstead": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Done": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Browserengineisoutdated": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Addanenvironment": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Installanotherdistro": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Installedenvironments": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Removeenvironment": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Waitingforprogress": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.Continuewithoutenvironment": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.GotoDefinition": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.FindReferences": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "ui.RenameSymbol": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "manager.installed": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "manager.problems": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "manager.oneProblem": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "languages.basicTitle": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    },
    "project.blankDescription": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2048
    }
  }
}
```

资源为遵循本规范canonical JSON+LF规则的严格UTF-8 JSON对象（pack.py会规范化源码JSON），最多64KiB；1..注册键数个条目，每值为1..2048字符纯文本。Core只开放下表键；未知键/保护键拒绝E_LOCALIZATION。每条翻译必须保留对应英文的命名占位符集合（如{count}），不支持printf、HTML、ICU程序或脚本；不完整/额外花括号拒绝。资源不完整时逐键回退英文，不读取任意包提供的英文覆盖。校验器还检查UTF-8、深度、控制字符及原包SHA256/签名。

|公开键|Core英文默认值|
|---|---|
|`nav.files`|Files|
|`nav.find`|Find|
|`nav.languages`|Languages|
|`nav.packages`|Packages|
|`nav.settings`|Settings|
|`files.internal`|Internal Files|
|`files.filter`|Filter visible names|
|`files.filterHint`|Filters loaded entries only. Use Find for nested names and contents.|
|`files.matches`|{count} visible matches|
|`files.newFile`|New File|
|`files.newFolder`|New Folder|
|`files.refresh`|Refresh|
|`files.more`|More file actions|
|`find.content`|File contents|
|`find.names`|Files and folders|
|`find.current`|Current document|
|`find.query`|Search {scope}|
|`find.scope`|Search scope|
|`find.hint`|Search names or contents across the selected folder.|
|`run.file`|Run file|
|`run.stop`|Stop file|
|`run.debug`|Debug|
|`run.continue`|Continue|
|`run.stopDebug`|Stop debug|
|`run.overview`|Overview|
|`project.build`|Build|
|`project.test`|Test|
|`project.run`|Run Project|
|`project.tasks`|Advanced tasks|
|`packages.title`|Packages|
|`languages.title`|Languages|
|`ui.language`|Interface language|
|`ui.english`|English (default)|
|`ui.fallback`|Missing translations use English. Language resources do not install development environments.|


### 29.2 实际 Core 接口

所有接口在`dev.suicode.packages`，不依赖Android/JCode插件。新增能力`Capability.LOCALIZATION("ui.localization")`，这是被动资源授权，没有任意执行操作。

|接口|参数、返回值、调用时机与失败|
|---|---|
|`LocalizationDescriptor.parse(d: JsonObject): LocalizationDescriptor`|验证包安装/加载时调用，全部字段必填；返回locale/displayName/fallback/resource。非法字段/值抛PackageFailure，code E_LOCALIZATION（公共JSON字段类型错误可为E_MANIFEST）。不产生IO/副作用。API4。|
|`LocalizationResources.parse(bytes: ByteArray): Map<String,String>`|输入完整认证资源字节，返回不可执行的键/文本映射。校验上述限额与占位符；不合法抛PackageFailure（E_LOCALIZATION或通用JSON错误）。纯函数，不要求权限；它不等于启用。|
|`LocalizationResources.forPack(pack: VerifiedPack): List<LocalizationBundle>`|从认证归档中解析全部localization组件；仅取包内resource；缺失/错误失败。返回每个locale的bundle，包含packageId、locale、displayName、entries和selectionId。API4；宿主必须另外检查生命周期/授权。|
|`LocalizationBundle.selectionId: String`|只读，格式packageId/locale，用于明确选择；不能只按locale自动抢占。|
|`UiTranslations(bundles: List<LocalizationBundle> = emptyList(), selection: String? = null)`|宿主仅传入已启用且已授予ui.localization的资源，构建当前不可变快照。没有选择、不可用或重复selectionId时使用英文，防止冲突自动选取。|
|`UiTranslations.text(key: String, arguments: Map<String,String> = emptyMap()): String`|必填已注册key；arguments键集合必须等于该英文的占位符集合，无占位符时为空。返回当前译文或英文，并一次性按匹配替换参数；参数中的花括号保持字面量，不递归替换。未知key/缺失或额外参数抛IllegalArgumentException，属于宿主编程错误。无IO/权限升级。|
|`UiTextKeys.english: Map<String,String>`|公开只读键表，是本版本唯一允许翻译的Core键；新增键随API向前添加，移除键需重大API版本及弃用周期。安全授权/签名/来源/删除确认不在键表。|

### 29.3 Android加载、启停及选择

NativePackageHost在安装、恢复、启用、禁用、更新、卸载、回滚后的publish阶段，于IO线程读取已校验归档。只有enabled、有活跃CoreSession且记录授予ui.localization的包才进入`localizations: StateFlow<List<LocalizationBundle>>`。`selectedUiLanguage: StateFlow<String?>`保存用户明确选择；`selectUiLanguage(selection: String?)`为宿主界面操作，null选择英文，不存在selection抛IllegalArgumentException。偏好单独保存，不改系统Locale、不重建Ubuntu、不清任何项目数据。UI通过NativeLocalizationProvider和core/design的LocalUiTranslations订阅不可变快照；`@Composable uiText(key,arguments): String`与上述text契约相同。

Packages页面提供界面语言选择和来源ID。安装只增加可选资源，不自动切换；同locale的不同包各自列出，用户明确选择，不靠安装时间覆盖。禁用/卸载当前资源立即英文回退；偏好保留，重新启用相同ID/locale可恢复。进程重启恢复包和选择。资源更新仍走完整摘要/签名/授权审阅与原子包替换，解析失败回滚旧归档；不允许未确认脚本执行。API4仅已接入的导航、Files筛选、Find模式、主运行操作、Languages/Packages标题等公开键可被替换；未接入的既有设置/高级界面仍英文，完整翻译迁移属于后续范围。

### 29.4 安全、测试与兼容

localization仅可提供纯数据；禁止全局主题覆盖、HTML/View/DEX/JS注入和安全确认文案篡改。完整性/签名状态沿用标准包协议；未签名样例显示未验证且必须确认。权限检查在宿主生命周期而非仅靠Schema，插件越界仍E_BOUNDARY。无环境、下载或执行依赖的语言包审阅计划为零工具步骤；与已启用Java/C_CPP/Python Kit并存。测试覆盖安装/启用/禁用/卸载后资源回退、两来源冲突、占位符、错误key/locale/资源大小及损坏包；规范工具同步验证完整Schema及包样例。旧接口未弃用；后续开放键/新增回退机制必须同步SDK、规范、校验工具和测试。

### 29.5 Android启动兼容性修正（2026-11-09，API4接口及包格式不变）

Core占位符验证和渲染使用同一字面扫描器，不依赖Android ICU与JVM不同的正则宽容行为。包括默认英文在内的所有键必须在Android libcore上覆盖；不能以仅JVM通过替代首屏验证。NativeLocalizationProvider复用现有ViewModel的NativePackageHost，不在主题/主界面前另建Host；可选资源快照异常记录完整诊断并回退英文，UI子树的实际编程异常不被全局try/catch吞掉。语言与宽度的可选偏好若类型错误，保留文件及原值、记录错误并使用默认值；不会删除包状态、Ubuntu或用户文件。损坏包归档仍按既有校验拒绝，完整错误进入启动日志，不尝试自动卸载/重装或无限恢复。


## 30. UI 目录扩容版 0.4.1（sui-code 0.5.3）

规范补丁版本 **0.4.1**；Core API仍为4，`.suic`/`.suik`容器仍为1.0。本节是向后兼容的英文资源键扩容，不改变加载器、授权、包头、SHA-256、签名、原语言Kit或环境安装协议。旧0.4.0接口不弃用，12键示例继续部分覆盖与逐键英文回退。旧主程序不认识新键时以E_LOCALIZATION拒绝导入新包，而不是自动忽略未知键；完整语言包1.0.0需要sui-code 0.5.3及以后的同目录宿主。API min/max不能单独表述这一资源目录版本，因此发行说明必须另外注明最低应用版本。

英文源表在`localization/en.json`，公开键和值由`tools/localization/catalog.py --write`同步到生产`UiTextKeys.english`及上方完整闭合Schema。中文源码在`localization/zh-Hans/`，并通过正式`tools/sui_packages/pack.py`构建独立包；不复制到Android主程序资源或assets。公开键的新增或语义变更必须同时更新中文、Core、Schema、SDK、文档与测试；不允许给未知键、任意路径或脚本添加通配许可。每个资源仍最多65536字节、每值1..2048字符，只允许普通文本与一致的ASCII命名占位符；当前完整包仍在该上限之内，不需要扩容限制。缺少未来新增键继续回退英文，禁止由语言包自动下载安装其他依赖。

正式包标识`dev.suicode.localization.zh-hans`、版本1.0.0，与`dev.suicode.example.zh-hans`开发样例不同。它仅声明`ui.localization`、一个localization组件及空依赖列表。没有可用官方私钥时按未签名包交付，宿主必须继续显示Unverified publisher并取得来源及能力确认；不得为了完整翻译覆盖这些安全/权限/签名文案。其他语言Kit的作者文案、代码、日志、路径、真实文件名与进程输出保留原文。

`NativePackageHost.uiText(key: String, arguments: Map<String,String> = emptyMap()): String`是Android宿主事件文案适配器，读取已授权资源与用户选择的不可变快照，不进行磁盘或运行环境IO；返回值与`UiTranslations.text`完全一致。调用时机为生成宿主Toast/Snackbar等事件文本，未知键或错误参数抛IllegalArgumentException；无可用语言包时逐键返回英文。它不是扩展可执行入口，不增加权限，也不允许扩展执行任意代码。Compose控件仍读取`LocalUiTranslations`，只显示翻译，不更改枚举/配置标识、用户路径、命令或保存内容。

`tools/localization/audit.py`输出生产源码位置、键、中文值、未接入字符串、宿主受保护英文和技术原文。静态显示参数引用覆盖率只衡量扫描到的字面文案，不包含无法静态识别的动态后端/扩展作者文案，不能称为“全应用汉化率”。`localization/ui-literal-baseline.json`对新增未审阅硬编码显示文字设回归门槛；更新基线必须人工说明缺口，不能把删除扫描范围当作翻译。


## 0.4.2 / v0.5.4：统一扩展卡片与可选择依赖

Core API仍为4，容器仍为SUIC/SUIK1.0。旧API1–4包没有`card`时，`PackageCards.forPack`返回安全通用卡片；旧权限、签名及生命周期继续有效。含card的包必须声明api.min>=4，并需要v0.5.4宿主；旧宿主对未知字段安全拒绝。Card协议版本独立为1，未知版本拒绝，不猜测其含义。卡片分类只控制允许的显示结构，不授予权限，不改变签名状态，也不放宽SUIC职责边界。

### 生产Core接口

`PackageCards.VERSION: Int = 1`。`PackageCards.forPack(pack: VerifiedPack): PackageCard`在完整性、签名及组件验证后调用；安装前预览、安装后、更新后可重复调用，无IO、副作用或执行权限要求。无card返回默认值；无效类别、版本、字段、组件引用抛出`PackageFailure(E_CARD)`，错误字段集合可能为`E_MANIFEST`，插件越界为`E_BOUNDARY`，API不足为`E_API`。验证器在返回VerifiedPack前强制调用此接口，不能靠UI忽略非法字段。

`PackageCard`字段：`category: CardCategory=GENERAL`；`basicPlugin: String?=null`；`languageIds: List<String>=[]`；`slots: Map<LanguageKitSlot,String>={}`；`fields: List<CardField>=[]`；`actions: List<String>=[]`；`formatVersion: Int=1`。只包含经验证的数据，不包含信任标志或可执行回调。`CardCategory`枚举wire值为language-ide-kit、localization、development-tool、utility、general。`CardField(id:String,label:String,type:String,value:String)`的完整约束见下方Schema；label/value是作者提供的纯文本，不属于宿主安全授权文案。link必须为无凭证HTTPS地址；宿主仅在显式用户操作后打开，绝不作为HTML加载。

`LanguageKitSlot`固定为runtime、lsp、dap、build、test、run、format、lint。basicPlugin另外引用Manifest中的依赖ID。runtime引用provider=environment的只读requirement ID；其他槽位引用同包真实组件ID：lsp→lsp，dap→dap，build→build，test→test，run→operation=SIMPLE_RUN的command，format→format，lint→lint。未声明槽位由宿主显示灰色“未提供”，不能当作检测失败；已声明但安装或运行失败显示实际错误。语言IDE卡片不接受自定义fields，保证统一布局。localization类别必须确实包含localization组件。

非语言Kit可声明最多8个inert text/link字段、最多8个已有组件action；action只接受command/workflow/panel/build/test/format/lint，调用仍经过当前授权CoreSession、原能力检查及backend。禁用/卸载立即使操作不可用；更新后必须重新按摘要审核。宿主的包类型、发布者、来源、签名、权限和安装状态由VerifiedPack/PackageStore计算，不能由卡片字段替代。SUIC仅允许general类别与只读fields，禁止slots/basicPlugin/languageIds/actions，不允许以卡片绕过完整IDE Kit职责。

`KitRequirement`新增可选`dependsOn: List<String>=[]`、`pythonModules: List<String>=[]`及解析结果`explicitDependencies: Boolean=false`。dependsOn必须引用当前setup描述符中存在的ID，ID在整个包内唯一，禁止循环。pythonModules为Python标识符点分路径；宿主在审核前使用隔离的固定Ubuntu Python解释器和PathFinder逐层检查模块位置，不导入目标或父模块，不运行扩展声明的命令，不加载PYTHONPATH、当前目录、site或.pth启动文件。安装状态仅说明文件/工具存在，协议Ready必须由授权后的真实LSP/DAP启动确认。`KitRequirements.parse(d:JsonObject):List<KitRequirement>`校验单个描述符；`forPack(pack:VerifiedPack)`读取全包setup并检查包内ID唯一；`selection(requirements:List<KitRequirement>,requested:Set<String>):List<KitRequirement>`返回拓扑顺序的必要依赖闭包。三者均无IO或进程执行，不需要安装权限；非法图/未知选择抛E_COMPONENT，非法资源路径仍E_PATH。旧描述符未写dependsOn时，对非environment组件保守加入该包environment依赖；显式空数组表示没有依赖。隐式闭包也检测循环。environment要求只参加检测与审核，不由IDE Kit安装器下载JDK/GCC/Python。

日志必须按environment/package/component类别、owner、组件、来源、版本、发行版、创建时间和结果归属；Languages只呈现运行环境任务，Packages呈现扩展与IDE组件任务。检查状态与实际LSP/DAP运行状态分开，不能把安装探测成功显示成正在运行。任何安装脚本都必须经统一计划审阅、精确包摘要授权后由原PRoot/PTY执行。

Card完整Schema（语义引用和职责检查由生产验证器补充）：

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "urn:suicode:card:1",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "version",
    "category"
  ],
  "properties": {
    "version": {
      "const": 1
    },
    "category": {
      "enum": [
        "language-ide-kit",
        "localization",
        "development-tool",
        "utility",
        "general"
      ]
    },
    "basicPlugin": {
      "type": "string",
      "pattern": "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9-]*)+$"
    },
    "languageIds": {
      "type": "array",
      "maxItems": 16,
      "uniqueItems": true,
      "items": {
        "type": "string",
        "pattern": "^[a-z][a-z0-9+_.-]{0,63}$"
      }
    },
    "slots": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "runtime": {
          "type": "string",
          "pattern": "^[a-z][a-z0-9.-]{0,63}$"
        },
        "lsp": {
          "type": "string",
          "pattern": "^[a-z][a-z0-9.-]{0,63}$"
        },
        "dap": {
          "type": "string",
          "pattern": "^[a-z][a-z0-9.-]{0,63}$"
        },
        "build": {
          "type": "string",
          "pattern": "^[a-z][a-z0-9.-]{0,63}$"
        },
        "test": {
          "type": "string",
          "pattern": "^[a-z][a-z0-9.-]{0,63}$"
        },
        "run": {
          "type": "string",
          "pattern": "^[a-z][a-z0-9.-]{0,63}$"
        },
        "format": {
          "type": "string",
          "pattern": "^[a-z][a-z0-9.-]{0,63}$"
        },
        "lint": {
          "type": "string",
          "pattern": "^[a-z][a-z0-9.-]{0,63}$"
        }
      }
    },
    "fields": {
      "type": "array",
      "maxItems": 8,
      "items": {
        "type": "object",
        "additionalProperties": false,
        "required": [
          "id",
          "label",
          "type",
          "value"
        ],
        "properties": {
          "id": {
            "type": "string",
            "pattern": "^[a-z][a-z0-9.-]{0,63}$"
          },
          "label": {
            "type": "string",
            "minLength": 1,
            "maxLength": 80,
            "pattern": "^[^\\u0000\\u202a-\\u202e\\u2066-\\u2069]*$"
          },
          "type": {
            "enum": [
              "text",
              "link"
            ]
          },
          "value": {
            "type": "string",
            "minLength": 1,
            "maxLength": 1024,
            "pattern": "^[^\\u0000\\u202a-\\u202e\\u2066-\\u2069]*$"
          }
        },
        "allOf": [
          {
            "if": {
              "properties": {
                "type": {
                  "const": "link"
                }
              }
            },
            "then": {
              "properties": {
                "value": {
                  "pattern": "^https://[^/@\\s]+(?:[/?#].*)?$"
                }
              }
            }
          }
        ]
      }
    },
    "actions": {
      "type": "array",
      "maxItems": 8,
      "uniqueItems": true,
      "items": {
        "type": "string",
        "pattern": "^[a-z][a-z0-9.-]{0,63}$"
      }
    }
  },
  "allOf": [
    {
      "if": {
        "properties": {
          "category": {
            "const": "language-ide-kit"
          }
        }
      },
      "then": {
        "properties": {
          "fields": {
            "maxItems": 0
          }
        }
      },
      "else": {
        "not": {
          "required": [
            "basicPlugin"
          ]
        },
        "properties": {
          "languageIds": {
            "maxItems": 0
          },
          "slots": {
            "maxProperties": 0
          }
        }
      }
    }
  ]
}
```

### 0.4.2 installation ownership and catalog provenance

`KitRequirements.selection(requirements: List<KitRequirement>, requested: Set<String>): List<KitRequirement>` returns a dependency-first closure. Unknown IDs or cycles fail `E_COMPONENT`. An omitted legacy `dependsOn` conservatively includes environment requirements; an explicit empty array means no requirement dependencies. Legacy requirements sharing a kit-resource installer are reviewed as one inseparable group; force repair is refused until the Kit declares the selective component contract. New official Kits declare independent requirements and their resource installer consumes the reviewed selection. Environment requirements are read-only in Packages, including legacy API3 Kits: the host must never turn them into a runtime download. Runtime installation and configuration repair are owned by Languages. Selected IDE tools whose closure lacks a usable runtime are blocked with a diagnostic; installed tools are retained. Skipping optional tools yields Partially Ready, not a simulated Ready state. Component probes do not execute package installers. `packages` specifies reviewed APT installation targets; when file/command/module/alternative-command checks are supplied, those actual tool checks determine installed status. Package database checks are the fallback only when no actual tool condition is declared. A working preinstalled versioned LLDB, pip-installed pylsp or custom compiler must not be rejected solely because an Ubuntu meta-package is absent. Installed status is separate from the running LSP/DAP protocol state. `pythonModules` contains only dotted Python identifiers. Before consent, the host uses `/usr/bin/python3 -I -S` and `PathFinder.find_spec` to inspect each path segment without importing the target or parent modules, ignoring startup site/.pth files, PYTHONPATH and the current directory. Version discovery reads dpkg metadata, including executable ownership when a meta-package is absent; it never invokes extension-named binaries. A required/source version is explicitly labelled when no installed package version is available. Existence does not assert that a server can initialize: actual LSP/DAP protocol readiness and errors are reported independently after authorization. Host-owned runtime validation remains distinct from author-declared tool probes.

The production host's `NativeKitSetup.prepare(preview, selectedComponents: Set<String>? = null, forceComponents: Set<String> = emptySet())` inspects all declared requirements but constructs installers only for the selected closure. `null` selects all non-runtime components. `forceComponents` must be a subset of selected non-runtime IDs; it explicitly requests a reviewed repair/update even when the installed check succeeds. Normal installation never reinstalls healthy components. APT updates pin reviewed signed candidates and do not upgrade unrelated requested components. The review binds the archive digest, exact source, versions, known download sizes, permissions, distro and creation time (maximum one hour). Changing selections invalidates approval until the refreshed plan has been reviewed. Grouped scripts receive `SUI_KIT_COMPONENTS`, the space-separated pending selected requirement IDs sharing that resource; a grouped installer must not deploy omitted siblings. All scripts remain authenticated package resources and require explicit `process.run`, `file.read`, `file.write` consent. Core retains independent final checks after command errors. Cancellation uses the existing Setup PTY and safe apt/dpkg boundaries; installed components are retained and package archives remain recoverable. Removing a Kit must not remove the language runtime or user data.

Official directory format1 may list an approved **unsigned** package with both `signer` and `signature` empty. The catalog signature authenticates reviewed origin/size/hash; it does not sign that package or grant trust. A nonempty signer must equal the existing pinned official public-key digest and contain a signature. `cardCategory` is optional and validated against the card categories above (legacy default `general`). On download, package identity/version/author/category, byte count, SHA-256 and actual package signature status must agree with the catalog. An unsigned listed localization package still needs unverified-publisher confirmation; no trust root is added. Local imports follow the same verifier and renderer.

Installation log metadata records task type (`environment` or `ide-component`), component/package ID, distro, source, timestamp, outcome and persistent log location. Languages reads only environment records, Packages only IDE records. Archives survive navigation/restarts. Logging failures are diagnosed and cannot prevent cancellation cleanup. Historical unindexed Ubuntu logs are retained. The host reads bounded log tails off the UI thread.

Runnable card templates and the corresponding real archives are indexed in [examples/CARDS.md](examples/CARDS.md). The SDK copies the exact production `PackageCards`, `KitRequirements`, verifier and independent loaders; it does not emulate Android backends. Old API1–4 packages with no card declaration continue using a generic host card.

### Host integration: unchanged-archive tool repair

`NativePackageHost.onServicesReload: (packageId: String) -> Unit` is an Android host callback, not an extension-granted operation. It is emitted after committed install/activation/publish when the installed enabled record is unchanged but the Kit declares LSP services. This covers installing an optional Java Debug bundle or Python formatting backend without changing the package archive digest. The workbench asynchronously closes only that owner's LSP sessions and reopens its documents; unrelated servers and debug owners remain alive. Changed records use the dependency-aware service invalidation flow instead. Resource-only localization Kits never emit this callback. Errors in incomplete legacy server descriptors are diagnosed in their host card rather than crashing composition. No callback grants capabilities or changes publisher trust.
