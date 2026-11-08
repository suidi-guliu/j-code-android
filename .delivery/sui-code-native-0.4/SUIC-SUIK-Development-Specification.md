# sui-code Native Extension & IDE Kit Development Specification

规范版本 **0.2.0**；Core API **2**（兼容 API 1 声明）；SUIC **1.0**；SUIK **1.0**。

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
| `CoreApi` | `VERSION: Int = 2`、`MIN_SUPPORTED: Int = 1`；`compatible(min:Int,max:Int):Boolean` 判断区间交集 |
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

`DebugWorkflow.resolve(workflow:JsonObject,context:Map<String,JsonElement>,rpc:suspend(String,JsonArray)->JsonElement):ResolvedDebugPlan`：三个参数必填，返回 launch:JsonObject、cwd:String、port:Int?。仅在调试准备阶段由授权 host 调用；外部 RPC 可挂起，取消应向上传播。最多32步，无循环，无动态代码。

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
          "ui.panel"
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
              "panel"
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
  ]
}
```

完整合法 Manifest：

```json
{"api":{"max":2,"min":2},"author":"sui-code examples","components":[{"entry":"ide/overview.json","id":"overview","type":"panel"}],"dependencies":[],"description":"Local development example; not an official publisher","id":"dev.suicode.example.minimal-kit","kind":"suik","name":"minimal-kit","permissions":["ui.panel"],"version":"0.1.0"}
```
