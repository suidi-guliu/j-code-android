# sui-code Native Extension & IDE Kit Development Specification

规范版本 **0.1.0（实验协议，非稳定产品承诺）**；Core API **1**；SUIC **1.0**；SUIK **1.0**。

本文的 MUST/必须、MUST NOT/禁止、SHOULD/应为规范性要求。本文与 `core/native-packages/src/main/java/dev/suicode/packages` 中的生产验证器、加载器、授权会话及事务存储对应，不引用 JCode 插件 API。所有路径均相对仓库根目录。

## 1. 实现状态与总体架构

协议模块已实现格式识别、校验、权限门禁、独立 Plugin/Kit 解析及激活、受限组件描述、包状态事务、停用、卸载、回滚和声明式面板状态。`PackageTool` 使用生产验证器，是本文示例的验证入口。

**当前 Android 应用尚未接入此新模块。旧 JCode 扩展调用尚未从应用移除；四语言 QuickRun 仍使用 core-foundation 的实现。本文不声称新包已能在 Android UI 中安装，不声称 LSP、DAP、完整语言 IDE Kit 或新主题已完成。**

执行路径定义为：不可信包 → 格式/完整性验证 → 静态权限与职责检查 → 用户审阅 → 针对摘要授权 → host 提供受控 backend → 会话请求 → backend。安装和激活不会执行包中代码。新协议不加载 DEX、APK、WebView 脚本或 JCode 扩展实例。

三种官方语言最终均使用同一机制；示例目录中的 Java/C_CPP/Python 内容是协议开发样例，不是已发布的完整语言实现。不得将样例名称、签名或能力误标为官方已审核产品。

## 2. Core API 类型定义

包名：`dev.suicode.packages`。调用均为进程内 host API，不是开放 Android IPC。扩展作者编写声明式文件，不能获得 `Context`、`ClassLoader`、任意 `File` 或 shell 对象。

| 类型 | 完整字段与约束 |
|---|---|
| `CoreApi` | `VERSION: Int = 1`，只读常量 |
| `PackKind` | `PLUGIN`/`KIT`；属性 `magic: String`、`suffix: String`、`mime: String` |
| `Capability` | 枚举；wire 值见权限表 |
| `Operation` | 枚举；`permission: Capability`，见操作表 |
| `Request` | 必填 `operation: Operation`、`component: String`、`arguments: Map<String,String>`；没有默认参数 |
| `Reply` | 必填 `accepted: Boolean`、`code: String`；可选 `values: Map<String,String> = emptyMap()` |
| `Consent` | 必填 `digest: String`、`grants: Set<Capability>`、`allowUnverified: Boolean` |
| `VerifiedPack` | 只由验证器创建；只读 `kind,id,version,digest,official,signer,permissions,manifest`；`signer` 可空，其余不可空 |
| `PackageFailure` | `code: String`；异常文本为 `CODE: detail` |

`digest` 是完整容器的 SHA-256 小写十六进制，64 字符。`signer` 为签名所用 SubjectPublicKeyInfo DER 的 SHA-256，不是证书名称。返回 `official=false` 不等于签名无效：可能未签名，也可能签名有效但公钥未被 host 固定为官方根。

## 3. 兼容性和版本策略

Manifest `api.min`/`api.max` 为闭区间正整数，必须包含 `CoreApi.VERSION`。不兼容报 `E_API`，不尝试降级到 JCode API。容器版本与 API 版本独立；当前只识别格式 1.0，其他版本报 `E_FORMAT`。

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

裸 ZIP、仅改扩展名、魔数与扩展名不匹配均拒绝。容器大小 20 bytes 至 32 MiB；最多 256 个条目；单条目展开不超过 8 MiB，总展开不超过 32 MiB。归档只在内存验证，不执行或直接解压到用户目录。未来执行型资源需另行发布安全协议，不得利用当前 JSON 承载任意主进程代码。

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

每一步是 argv，不是 shell 拼接。`prepare` 和 `run` 仅为经过验证的声明；当前通用模块不展开变量或启动这些命令。示例 `${tool}`、`${source}` 是 host 可识别的保留占位符；**当前 Android backend 尚未实现该解释器，不能将样例当作已可运行的语言包**。不得通过 shell 包装器绕过执行行为预览。

实际调用：`CoreSession.request(Request(Operation.SIMPLE_RUN, componentId, arguments)): Reply`。调用时机为 host 取得当前文件、等待保存、解析真实路径和环境后；权限 `process.run`。组件必须登记为 SimpleRun/basicBuild，且用户须授权本摘要。没有 backend 返回 `E_UNAVAILABLE`，而不是接受后静默丢任务。

`CoreBackend.invoke(request: Request): Reply` 是 host 提供的唯一执行端口。所有参数必填。执行 backend 必须验证路径授权、环境身份、进程树归属，返回任务 ID/状态；必须通过同一 host 的停止机制清理子进程。原 core-foundation 的文件保存、PTY、PRoot 可用于该适配，但当前尚未迁入新包 API。此处不虚构 `startJava()` 或已实现的任务 ID 协议。

## 8. 语言注册与文件识别

`language` 描述：必填 `languageId: String`（`[a-z][a-z0-9+-]{0,31}`）、`extensions: Array<String>`（非空、不重复、每项 `[A-Za-z0-9+_-]{1,16}`，无前导点）、`priority: Int`（0..100）。Schema：[`schemas/language.schema.json`](schemas/language.schema.json)。

扩展名匹配必须保持大小写，避免 `.C` 混为 C；C/C++ 在同一包登记多个后缀。发生多个声明冲突时 host 必须让用户选择，不能用安装先后静默覆盖；priority 只作建议。当前模块验证声明，不自动覆盖应用编辑器的识别注册表。

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
| SIMPLE_RUN | simpleRun / basicBuild（basicBuild 仅 SUIC） | process.run |
| LSP_START | lsp | language.lsp |
| DAP_START | dap | debug.dap |
| BUILD | build | process.run |
| TEST | test | process.run |
| FORMAT | format | file.write |
| LINT | lint | file.read |
| PROJECT_READ | project | project.read |
| PANEL_OPEN | panel | ui.panel |

`workflow`/`command` 可引用上述任一 Operation，但仍受对应权限和 backend 检查。普通操作描述必须恰有 `operation: String` 和 `arguments: Object<String,String>`；arguments 最多 128 key，每 value 最多 4096 字符。operation 必须为表中枚举。额外字段、类型不符报 `E_COMPONENT`/`E_BOUNDARY`。

`CoreSession.request` 同步返回 `Reply`；依序检查活跃状态、组件存在、组件操作绑定、SUIC 边界、声明及授权权限、backend 可用性。backend 抛异常转换为 `E_BACKEND`，异常不泄露到扩展。backend 必须自行实现真正的异步任务队列，不能在 UI 线程长时间阻塞；当前通用模块未实现这些 IDE 服务。

## 13. LSP 与 DAP

LSP/DAP 组件使用第 12 节格式。LSP 调用 `Request(LSP_START,id,args)`，DAP 调用 `Request(DAP_START,id,args)`。结果统一 `Reply`；没有 adapter 返回 `E_UNAVAILABLE`。禁止声称只有描述文件就具备补全或调试。

实现 backend 时必须使用 Ubuntu ARM64 匹配工具及独立进程，LSP 使用标准 JSON-RPC 2.0/Content-Length framing，DAP 使用官方 DAP framing；host 负责关闭、取消、超时、路径 URI 映射，不能把 Android host 路径直接交给 guest。LSP 动态工作区编辑须经 file.write 单独授权，DAP 运行目标还须 process.run。当前 broker 单操作权限是第一道门，不能代替 backend 的复合权限检查；此类 backend 未提供前保持不可用。

## 14. Build / Test / Format / Lint

四种组件分别绑定 BUILD/TEST/FORMAT/LINT；调用、参数和 Reply 同第 12 节。host adapter 应提供实际工具版本、工作目录、结果、诊断和取消能力。Format 必须遵守保存版本条件并原子替换，不允许覆盖用户更新；Test 必须区分失败断言和 runner 错误。Build 产物放 host 分配的目录；不得未经同意向源目录写二进制。

当前框架只验证及分发这些操作，未随规范附送真实 Gradle/Maven/CMake/pytest adapter；缺失服务必须明确报不可用。

## 15. Project 与工作流

`project` 绑定 PROJECT_READ，只能读取用户选择的 Project，不得枚举全部私有文件。无 Project 时基础 SimpleRun 不受影响。工作流为声明式操作请求，不提供脚本钩子；宿主须单独展示每个步骤的执行行为和权限。

本版本 workflow 描述仅支持一个 Operation 请求，不支持 DAG、循环或任意表达式；复杂工作流不能用未定义字段假装已实现。后续多步骤版本须扩展 Schema 并增加版本协商。

## 16. 第三方特殊功能

高级重构、分析面板、自定义调试 UI、数据工作流必须 `.suik`。SUIC type 集只有 language/simpleRun/environment/basicBuild，权限只有 file.read/process.run；任何 panel/lsp/dap/workflow 等在验证阶段报 `E_BOUNDARY`。资源数据不应被 host 当作可执行代码加载。

可验证第三方样例：[`examples/special-feature/`](examples/special-feature/)。其 LINT 分析请求展示扩展点和错误行为，未安装实际分析 backend 时返回不可用，不伪造统计结果。

## 17. UI 面板与交互接口

`panel` 描述全部字段：`title: String`（1..80）、`text: String`（0..65536）、`status`（ready/loading/empty/error）。不得携带 HTML/JS/CSS/APK/DEX 注入。

`PanelState(id:String,title:String,text:String,status:String="ready")` 为实际模型。`PanelStore.put(session:CoreSession,panel:PanelState):Reply` 调用 PANEL_OPEN；只有 accepted 才保存状态。`snapshot():List<PanelState>` 返回状态快照；`release(packageId:String):Unit` 移除该包面板。调用时机为 host 渲染/更新/卸载，权限 ui.panel。无 UI backend 时 E_UNAVAILABLE，不创建伪面板。

UI 视觉、尺寸、主题及无障碍详见独立 [UI 规范](SuiCode-UI-Design-Specification.md)。当前 PanelStore 不等于已实现 Compose renderer。

## 18. 依赖与职责边界

依赖只按明确包 ID 和版本解析，不根据语言环境重复安装。Kit 可以依赖 SUIC；卸载被依赖包报 E_DEPENDENCY。禁止 SUIC 通过一个脚本向 host 请求高级 IDE 注册。每个 capability 是门禁而非执行许可证；权限同意不能扩张包类型职责。

SUIC/SUIK 共享纯粹的格式安全代码，但拥有不同 magic、kind、Schema、目录规则、解析入口、Loader 和允许组件集合。两者没有继承 JCode 原有生命周期。

## 19. 官方环境声明与获取

`environment` 描述字段全部必填：`environmentId:String`（1..4096）、`provider`（temurin/ubuntu-apt）、`tools:Array<String>`（非空、每项 `[A-Za-z0-9+_-]{1,40}`）、`configuration:String`（`/opt/sui-code/[a-z-]+.env`）。此声明不自动下载安装。

Java 采用官方 Temurin API及包校验；C/C++ 合包，工具 gcc/g++；Python 工具 python3。不得向包内塞编译器、解释器或大型工具链。host 应探测既有 env 和工具，优先保留 v0.3 用户安装；Kit 不得要求重复安装。

官方库须由 host 固定根验证目录及实际包，展示来源、摘要、签名、大小、版本、兼容性及日志。不允许使用 JCode 市场作为新协议源。当前官方签名根尚未部署，**尚无可宣称已审核的官方包目录**；开发样例不能替代正式库。

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

**PRoot 不是恶意代码安全沙箱。** 只有签名验证和进程标签不能实现文件/网络隔离。当前新模块不执行包代码；未来执行 backend 在缺少有效隔离和授权时必须拒绝，不可声称 broker 已使任意脚本安全。禁止在 IDE 同进程载入第三方代码。

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

日志须记录时间、包ID、digest、operation、错误码、任务关联号；不得记录私钥、授权 token、终端秘密或完整源码。当前模块不上传日志、不发送遥测，应用日志呈现尚需接入。

## 24. Android ARM64 与 Ubuntu PRoot

运行平台目标 Android API33+、arm64-v8a、Ubuntu24.04 aarch64；host JVM21 编译不代表设备签名算法、原子文件系统或 PRoot 运行已验证。工具须逐一验证 guest 架构、发行渠道、SHA及适用版本，禁止使用 x64 工具冒充 ARM64。

当前协议模块可在 JVM单测中执行；未声明设备集成已通过。不得清理 v0.3 的 Ubuntu、java.env、GCC/G++/Python、Project或源码。迁移必须可失败恢复，保留旧配置直到新链路验证。

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

此规范不替代完整产品验收。Android接线、正式签名根、官方目录、真实六个语言包、LSP/DAP、全局UI重构均必须另外通过测试，禁止仅以文档或验证样例计为完成。

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
{"api":{"max":1,"min":1},"author":"sui-code examples","components":[{"entry":"plugin/language.json","id":"language","type":"language"}],"dependencies":[],"description":"Development example; not an official installed language package","id":"dev.suicode.example.minimal","kind":"suic","name":"minimal","permissions":[],"version":"0.1.0"}
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
{"api":{"max":1,"min":1},"author":"sui-code examples","components":[{"entry":"ide/overview.json","id":"overview","type":"panel"}],"dependencies":[],"description":"Development example; not an official installed language package","id":"dev.suicode.example.minimal-kit","kind":"suik","name":"minimal-kit","permissions":["ui.panel"],"version":"0.1.0"}
```
