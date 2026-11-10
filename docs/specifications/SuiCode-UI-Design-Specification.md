# sui-code UI Extension & Design Specification

规范版本 **0.4.3**；对应 Core API **4**（兼容 API1 只读面板及 API2 原生交互面板）、SUIC/SUIK **1.0**。配套规范：[原生扩展总规范](SUIC-SUIK-Development-Specification.md)。本规范不允许重新依赖 JCode UI 插件体系。

## 1. 范围、实现状态与品牌

品牌名为 `sui-code`。应用包身份、签名和私有数据目录不因视觉品牌改变。保留上游 MIT 版权与第三方许可证；不得把改名作为移除版权的依据。

Android 使用 `SuiDesign.kt` 的 sui-code Light/Dark Palette、4/8dp 圆角、统一间距和 Material Design 许可符号；应用品牌资产独立。管理中心在完整编辑区页面呈现，常驻内部文件区无需 Project，Terminal 使用原 PTY 的上下分割布局。所有物理设备表现仍需按交付报告进行实机验收。

sui-code 的视觉目标为信息清晰、编辑器优先、平板高效。不得用过度胶囊化卡片压缩工作区。应用主导航和扩展面板由 host 管理；扩展仅贡献命名空间内内容，不能替换应用入口。

## 2. Light / Dark 模式

系统跟随、Light、Dark 为产品应支持的三个选项；模式切换不重启会话、不丢失编辑/终端状态。两套颜色必须覆盖同一 Token 集，不允许以反色算法临时生成 Dark。

实际 API：`DesignSystem.colors(dark:Boolean):Map<String,String>`，参数必填，返回只读语义的映射快照；不需权限，不读取系统设置，没有 I/O，不修改全局主题。调用时机为 host 创建/切换主题；第三方 panel 必须继承 host 选择，不得自设全局模式。Android ThemeMode.System/Light/Dark 由原设置持久化，Native Theme 直接消费此颜色表。

## 3. 颜色 Token 完整定义

源码：`core/native-packages/src/main/java/dev/suicode/packages/DesignSystem.kt`。所有值为不透明 sRGB `#RRGGBB`。

| Token | Light | Dark | 用途 |
|---|---|---|---|
| background | #F4F6FA | #11151C | 应用根背景 |
| surface | #FFFFFF | #191F29 | 编辑器/面板 |
| surfaceRaised | #E9EEF6 | #232C39 | 菜单、选中层背景 |
| text | #182231 | #E8EDF5 | 主文本 |
| textSecondary | #4D5D73 | #AAB8CB | 辅助文本 |
| accent | #205CA8 | #85B4FF | 主操作、选中状态 |
| onAccent | #FFFFFF | #0A2547 | 主按钮文本 |
| border | #75849A | #526176 | 控件边界 |
| error | #AF2525 | #FFB4AB | 错误 |
| success | #176A36 | #8BD5A4 | 成功 |
| warning | #795100 | #F2CA78 | 警告/未验证 |
| focus | #174C91 | #ABC9FF | 键盘焦点 |

禁止扩展写入 Token 表、改 Android 系统栏颜色或使用自定义 CSS 覆盖核心区域。正文对比度至少 4.5:1，大文本和必需非文本控件至少 3:1；实际渲染需验证组合，不应仅凭颜色表宣布无障碍通过。语法高亮不得拿诊断颜色作为唯一状态信息。

## 4. 字体、字号与排版

`DesignSystem.typographySp:Map<String,Int>`：title22、section18、body14、label12、editor14、terminal14。单位 sp；禁止把 dp 当文字尺寸。正文默认系统无衬线，代码/Terminal 使用等宽字体并测试中日韩回退。应用应允许用户调整编辑器/终端字号，不能强制所有面板固定小字。

正文行高建议 1.45 倍，代码行高 1.4 倍；按钮最多两行且不可截断关键危险动作。字号遵守系统字体缩放；字体放大不得导致安装确认按钮不可访问。需明确代码字符和 UI 文案的语言方向，不改变源码字符顺序。

## 5. 图标规范

使用统一线性图标语义，基准 24dp，推荐线宽 1.75–2dp；显示可小于触摸目标。文件、运行、停止、Project、语言、插件、Kit、包、设置使用一致语义，不用同一图标代表不同危险动作。

新增图标资源必须有明确许可，不复制上游商标作为 sui-code 自有标识。图标按钮必须有本地化 contentDescription；装饰图标明确不可聚焦。品牌 launcher 是独立代码括号图形，语义符号使用固定 sui-native 映射，许可声明随源码保留。

## 6. 间距系统

实际 `DesignSystem.spacing:List<Int>` 为 `0,4,8,12,16,20,24,32,40,48`，单位 dp。组件内部常用 8/12，页面边距 16/24，分组 24/32；边界 1dp 单独定义。扩展不应使用随机负 margin 或将内容绘制到 host 边界外。

## 7. 圆角

实际 `DesignSystem.radius:Map<String,Int>`：small4、medium8，单位 dp。输入框/按钮/菜单用 small，卡片/对话框用 medium。禁止大面积胶囊形容器；状态徽标可为紧凑矩形，不能造成整页圆角风格漂移。

## 8. 基础组件

| 组件 | 渲染要求 | 交互与错误 |
|---|---|---|
| Card | surface，medium圆角，16dp内边距；不用卡片嵌套卡片承载编辑器 | 不把整张卡误作多个动作按钮 |
| Panel | 明确标题、内容、状态区；边界与编辑器分离 | 关闭只撤销该面板，不停止无关任务 |
| Button | primary/secondary/danger，至少48dp触摸区域 | loading时防重复；危险动作明确对象 |
| Input | 可见标签、焦点边框、错误辅助文字 | 错误不清空已输入数据 |
| Menu | 按任务分组，键盘/触控均可操作 | 不靠 hover 才能访问动作 |
| Dialog | 限宽、可滚动正文、固定可达操作区 | 返回键不默认确认执行 |

这些是 host renderer 的要求，不是可导入的 `SuiButton()` 等 API。本版本未导出此类 Compose 函数，第三方不能写不存在的组件调用。

## 9. 主导航与信息架构

产品导航应分工作区与管理区：文件/Project、编辑器、终端/输出/Problems、Language Center、Plugin Center、IDE Kit Center、Package Manager、设置。不能把所有管理功能塞进一个长滚动页面。

主区切换保留编辑器与运行会话；管理页不能强制关闭文件。当前项目与内部根目录是不同上下文，切换不创建虚假数据库工程记录。扩展禁止添加第二套顶级导航和覆盖默认返回行为。

## 10. 编辑器、标签与文件管理

编辑器占主要空间；横向标签允许滚动，未保存状态以文字/图标可访问表示。关闭未保存文件必须走 host 保存/确认机制。Ctrl+F5 仅对激活编辑器文件，不误发给隐藏面板。

文件树应显示常驻内部根目录和 Project 上下文，支持多层创建、重命名、复制及导出；不要求普通文件拥有 Workspace。对已登记工程根目录/祖先的保护不得因重构移除。触控长按和菜单动作必须保留物理键盘替代入口。

## 11. Terminal、QuickRun、Build 与 Problems

编辑器与 Terminal 并行，不以全屏模态终端取代日常工作区。终端保留真正 PTY 输入、选择、滚动及停止任务；QuickRun/Project任务有明确归属，停止不得杀死无关会话。

实际 `DesignSystem.terminalHeight(availableDp:Int,requestedDp:Int):Int`：available必须>0，否则IllegalArgumentException；返回 requested 限制在 `min(160,floor(available*0.55))..floor(available*0.55)` 内（上界至少1）。纯布局计算，无权限和副作用。调用时机为 host 窗口/拖拽变化，不代表现有终端已接入拖拽组件。

Build与QuickRun输出要标出来源、编译/运行阶段、退出状态；Problems支持定位，不把异常堆栈裁剪为成功提示。终端内容不应默认全量读给屏幕阅读器，应提供摘要及按需读取。

## 12. Language Center

正式支持项 Java、C/C++、Python；C与C++合并展示。每项应独立显示基础插件状态、环境状态和IDE Kit状态，不用一个“已安装”掩盖三者区别。

环境详情显示来源、版本、架构、位置和检测结果；复用既有Ubuntu/JDK，不以迁移为由重装。环境缺失与插件缺失分开处理。安装确认应展示真实下载与执行行为，保持日志可查看、失败可重试、安全边界取消。

## 13. Plugin Center

只展示 `.suic` 官方目录、本地导入及已安装项；不能接入 JCode 市场。支持审阅、安装、禁用、更新、卸载；状态来源须来自 PackageStore/验证器，而非文件后缀判断。

详情显示基础语言注册、SimpleRun、环境声明；高级功能误放SUIC时拒绝并说明 E_BOUNDARY。未验证包使用 warning 及明确文字，不得用蓝色勾暗示官方认可。

## 14. IDE Kit Center

只展示 `.suik`；呈现 LSP/DAP/Build/Test/Format/Lint/Project/面板的实际 backend可用状态。只有Manifest声明不能显示“已支持调试”。缺依赖显示明确包ID及版本范围，用户同意后协调安装，不重复安装语言环境。

卸载 Kit 要解释保留基础插件、环境和用户数据。第三方特殊功能从该中心本地导入和管理；不开放第三方在线市场。

## 15. Package Manager

统一呈现包来源、完整摘要、签名者、官方根匹配状态、Core兼容性、权限、依赖、大小、安装日志与上一版本恢复入口。来源字符串/作者字段不构成信任。

安装先验证、后审阅、再授权、最后原子发布；安装进度不得在验证失败后显示成功。新版本摘要变化必须重新审阅。签名错误必须拒绝，不能提供“忽略损坏继续”。失败保留旧版并可见恢复方法。

## 16. `.suik` UI 组件 API

实际支持声明式 `panel`，不提供第三方同进程 Compose、HTML、JS 或DEX渲染。

| 接口 | 参数、返回值、约束 |
|---|---|
| `PanelState` | 构造必填 `id:String,title:String,text:String`；可选 `status:String="ready"`、`nodes:JsonArray=emptyArray`。id为`[a-z][a-z0-9.-]{0,63}`；title1..80；text<=65536；status枚举ready/loading/empty/error。非法参数抛IllegalArgumentException |
| `PanelStore.put` | 必填 `session:CoreSession,panel:PanelState`；返回Reply。校验已激活会话、组件ID、PANEL_OPEN、ui.panel授权和backend；只有accepted才保存 |
| `PanelStore.snapshot` | 无参数，返回`List<PanelState>`快照，不触发渲染/执行 |
| `PanelStore.release` | 必填`packageId:String`，返回Unit；移除该包命名空间状态，禁用/卸载时host调用 |

内部键为 `packageId:panelId`；同包更新替换自身状态，不覆盖其他包。Snapshot含纯模型，不授予修改全局布局能力。API2 允许下节列出的有限 widgets；未知字段、全局样式覆盖和未知类型拒绝。

完整样例见 [`examples/special-feature/ide/statistics.json`](examples/special-feature/ide/statistics.json) 及 [`examples/minimal-kit/`](examples/minimal-kit/)。打开 panel 操作会由 Android NativeKitPanel 真实渲染；包禁用/卸载关闭面板及状态。

### Native widget tree：完整字段及事件契约

panel Schema 见 [panel.schema.json](schemas/panel.schema.json)。所有 node 必填 type、id；id 匹配 `[a-z][a-z0-9.-]{0,63}` 且在面板内唯一；禁止未列字段。总节点<=128，嵌套<=8层；超限 E_SIZE，非法形状 E_COMPONENT；仅 API2+ Kit 可用。

| type | 额外必填字段 | 参数类型及行为 |
|---|---|---|
| column / row | children | Array<Node>；纵向布局 / 可换行横向布局；间距由 native token 决定 |
| text | text | String<=4096；正文渲染，不解释 HTML |
| metric | text | String<=4096；数值摘要样式，仍是可访问文本 |
| input | label,value | String<=4096；可见标签与默认值；onValueChange 写入面板 id 状态；<=4096字符 |
| toggle | label,value | String；value 仅 "true"/"false"；选择写入 id 状态 |
| button | label,command | String；command 必须是本 Kit 已声明非 PANEL_OPEN 的可操作组件；点击运行同组件授权流程 |
| table | columns,rows | columns:Array<String<=80>，1..8列；rows:Array<Array<String<=4096>>，<=100行且每行列数一致 |

`UiNodes.validate(nodes:JsonArray,commands:Set<String>?=null):Unit` 是真实验证入口：nodes 必填；可选 commands 指定同 Kit 可操作组件集合，生产包解析总是传入该集合；失败抛 PackageFailure，不渲染或执行。模型 PanelState 构造也验证树结构。`NativeKitPanel(host:NativePackageHost,panel:PanelState):Unit` 是 Android Composable renderer，由宿主打开 panel 后调用；这是 host 实现细节，第三方不加载 Kotlin UI 类。

button 的数据流为 click → 收集 input/toggle 当前字符串 → `invoke(packageId,command,inputs)` → CoreSession 权限门禁 → backend → Reply。输入仅在面板内存在，关闭后释放，不持久保存秘密。处理期间按钮禁用并展示进度，失败保留输入和明确错误码；不得把 REQUESTED/STARTING 当作完成。

结果文本支持 `${result:command.field}`，command 为同包命令 ID，field 为 Reply.values key。替换仅显示文字，不变成 shell 或下一条命令；未知值显示 `—`。若 workflow 要使用输入，仅可通过整 argv 参数 `${input:node-id}`，宿主正确转义。table/text/metric 的结果更新在主线程的 Compose 状态中完成，不允许扩展修改全局 state。禁用包清理面板，取消不可确认危险执行。

完整第三方统计面板见 `examples/special-feature/ide/statistics.json`，真实读取 PROJECT_READ 数据，没有 Project 时读取常驻 Internal Files。复杂功能可组合受限 native widgets 与已授权工作流；没有任意视图/DEX/JS 入口。新 node 类型需新 API 与同步 Schema，不得静默忽略未知节点。

## 17. 工具栏、菜单和命令

组件 type `command`/`workflow` 使用 `{operation,arguments}` 描述；Schema与总规范一致。命令只能调用自身组件注册的 Operation，权限继承请求所需能力；不能任意路由其他包或提升成Core内部管理命令。

本版本**没有导出工具栏/菜单位置注册或快捷键覆盖API**。第三方不得捏造 `registerToolbar` 等接口。扩展槽位待 host renderer 完成后独立版本发布；在此之前只能声明经过验证的请求，不伪称能注入菜单。

## 18. UI 生命周期与状态

验证不创建面板；安装默认disabled；启用会话后经过PANEL_OPEN授权才可更新。禁用/卸载时host先撤销会话并release包面板，关闭关联任务；重新启用须走实际状态读取，不复用已close会话。

状态更新应在host指定UI调度器应用；模型可在任意线程创建，PanelStore方法同步保护。当前纯模块没有Android LifecycleOwner或Compose记忆状态。旋转恢复须保留业务状态并重建布局，不能将Activity引用保存进扩展模型。

## 19. 事件、交互与响应

实际统一接口为 `CoreSession.request(Request):Reply`，请求字段、权限和失败码见总规范。PANEL_OPEN参数由PanelStore写入title/text/status。`Reply.accepted=false`必须显示可理解失败，不能继续显示假成功。

button 点击执行同 Kit 的已声明 command；input/toggle 写入面板范围状态，再以 input.<node-id> 传入请求。结果 values 绑定 `${result:component.field}`；没有值显示破折号，不能填入模拟成功值。异步请求状态不等于任务完成。禁止 DOM 消息、回调 URL、任意全局事件总线和旧 WebView 桥。

## 20. 主题继承与样式隔离

Panel描述没有自定义颜色、字体、间距、全屏或overlay字段；额外字段会被生产验证器拒绝。只有host renderer应用DesignSystem。第三方不能修改全局Token、系统状态栏、主导航、编辑器字号、用户选择或其他包状态。

自带assets只作为被审阅的数据；未定义资源解释器的内容不能自动执行。任意HTML/CSS/JS是禁止的替代渲染通道。当前隔离来自纯声明式模型，不声称可安全加载任意原生第三方UI代码。

## 21. 响应式与平板布局

实际 `DesignSystem.widthClass(widthDp:Int):WidthClass`：参数>0；返回COMPACT（<600）、MEDIUM（600..839）、EXPANDED（>=840），非法输入抛IllegalArgumentException。按可用窗口宽度，不按物理设备品牌。

| 宽度 | 产品布局要求 |
|---|---|
| Compact | 单主面板，文件/管理导航按需展示；不永久压缩编辑器；返回路径明确 |
| Medium | 挤压式可收起文件栏；保留编辑器及终端上下并行 |
| Expanded | 侧导航+文件/Project栏+编辑器；终端可调高；管理页独立分栏 |

横屏和竖屏都必须覆盖，分屏及键盘弹出后重新计算。禁止简单缩小全部控件。侧栏若会令编辑器不可用，优先收起或切换布局，而不是覆盖编辑器主要输入区。Android 依据可用宽度和紧凑高度选择常驻/抽屉导航，编辑器预留左栏宽度，Terminal 上下停靠。布局算法测试不能代替截图、TalkBack 和平板旋转实测。

## 22. 触控与物理键盘

触摸目标至少48×48dp，视觉图标可24dp；拖拽边界须有可替代菜单。键盘焦点可见，Tab遍历逻辑与视觉顺序一致，Esc关闭临时UI而不丢数据。

保留Ctrl+F5单文件运行、保存、切换标签、终端输入；扩展不抢占Core保留快捷键。长按/重复按键不能重复启动安装或运行进程。系统输入法遮挡时保持输入目标和确认按钮可达。

## 23. 无障碍和键盘导航

每个交互控件有角色、名称、状态；图标按钮提供描述，加载状态可读，错误与输入关联。不能只靠颜色区分签名信任、错误、未保存或禁用状态。

必须使用TalkBack/大字体/外接键盘在真实Android验证。焦点切换到终端/对话框后可回到原编辑位置，禁止焦点陷阱。动画遵循系统减少动画设置，不因加载无限闪烁。

## 24. 加载、空状态、错误及反馈

PanelState.status枚举：ready正常；loading正在获取；empty无内容；error失败。没有progress数值字段，不得伪造百分比。

四个功能中心分别设计：空目录/无包、查询中、无网络、依赖缺失、架构不符、摘要失败、签名失败、用户取消、安装恢复待处理。错误显示可操作下一步且保留日志；禁止“重装Ubuntu/清除数据”作为默认恢复。危险执行与未验证包需显式确认，取消不能等同同意。

## 25. 第三方兼容规则及验收

SUIK UI扩展必须：使用API1支持的描述结构；不注册SUIC面板；遵守host颜色、字号、尺寸与命名空间；在backend缺失时接受E_UNAVAILABLE；禁用后停止请求；不以资产脚本越权。

验收矩阵：Light/Dark × 横/竖 × Compact/Medium/Expanded × 触控/键盘；检查所有功能中心、文件树、编辑器、终端、安装确认、权限、错误/空状态及大字体。协议测试只证明模型/权限/范围约束，不等于完整产品视觉验收。

## 26. 版本与变更

0.1.0：公开Token、widthClass/terminalHeight纯计算、PanelState/PanelStore、权限模型和声明式UI边界。对应API1、SUIC1.0、SUIK1.0。不导出第三方同进程 Compose ABI。

0.3.0：API2 native widget tree、同 Kit 命令绑定、输入和结果状态、全局 Light/Dark 原生主题、完整页面管理中心、尺寸驱动文件栏和上下 Terminal。保留 API1 文本 panel。

Token删除、语义改变、字段删改、事件/权限变化须在设计规范、源码、Schema、样例和测试同步发布。稳定期兼容策略遵循总规范第3节；拒绝依赖JCode UI插件或隐式全局覆盖来保持“兼容”。


## 27. v0.5 工作台信息架构与兼容性（规范 0.3.0）

保留当前主题 Token、配色、字体及用户选择，工作台按功能重新组织。左侧常驻导航轨为 Files、Find、Languages、Packages、Settings；Files 内部总目录不以 Workspace/Project 为前提，Project 为可选上下文，可切换最近项目或返回 Internal Files。目录树保留创建、导入、导出、Trash，增加名称搜索、排序、多选复制/剪切/粘贴、移动和属性。文件夹名/路径 Unicode 不做破坏性替换，文件冲突默认拒绝覆盖。文件重命名/移动与打开缓冲区的路径身份同步，删除后缓冲区保留只读直到恢复，禁止后台保存重新生成旧路径。

主操作带相邻显示 **Run file、Debug、Overview**；当前文件 Basic QuickRun 与 Project Build/Test/Run Project 必须使用不同文案和动作。Overview 在运行旁显示 Kit 声明式面板，不再要求用户进入包管理器。Advanced tasks 保留 .jcode/run.yaml 的配置、运行和编辑能力；从日常主导航移除旧 Run 大入口，但不得删除配置或隐藏唯一入口。Source Control/Database/VM 在 More 工具菜单保留（仅已可用能力显示）。底部 Terminal/Output/Issues/Debug/Tasks 继续使用已有会话和高度拖动机制。小宽度时操作带可横向滚动，不把所有按钮挤成不可触控的小图标。

Languages 按语言展示运行环境、Basic 插件、IDE Kit、开发工具就绪层级；常规动作是一次审阅完整 IDE Kit 计划。SDK/Servers/Debuggers 仍有高级入口，不能作为普通用户逐项安装的前置步骤。Packages 单入口分 Plugin Library、IDE Kit Library、Installed，不再有重复主导航。计划加载与探测必须在 IO/后台执行；页面卸载取消探测，已确认的安装任务由应用拥有并持续提供进度/取消/日志，不因切换标签失去状态。

补全自动触发来自文本修改或显式 Ctrl+Space；方向键、点击、拖动选择和滚动不得无条件新开补全。请求绑定文档、快照和光标，旧结果不能覆盖新位置，取消需传给 LSP。Ctrl+F5 从 Activity 常规及 shortcut 两条 Android 物理键盘分发路径进入同一保存/QuickRun链，拒绝重复按下；Ctrl+Enter 是部分键盘未向 Android 上报 F5 时的兼容入口。所有快捷键必须保留可点击的功能入口。

QuickRun Stop 只终止当前任务进程树，交互 shell、终端及已输出文本保留。关闭会话才释放 PTY；二者不得共用销毁动作。断点未验证时必须显示源路径、行号与 adapter 消息，不能假装已命中。错误展示应包含操作、原因、日志及重试方式；E_BACKEND 仅作为诊断码，不可成为唯一用户文案。项目统计无工程上下文时明确 N/A。

`.suik` 面板仍仅通过 UiNodes API 扩展，不能覆盖导航轨、全局主题、底部终端或主运行带。遵守前述48dp触控、键盘焦点、TalkBack标签、动态字级、横竖屏响应和主题继承规范；0.3.0不授权任意第三方Android View/HTML脚本注入。

文件删除事务必须先持久化恢复意图，再移动原文件，最后更新统计；统计写入失败不使已移动数据失去可恢复索引。不能自动清理仍含用户数据但元数据损坏的 Trash 条目。重命名/移动同步标签和断点路径；影响当前调试源时结束该调试并明确提示重新启动，避免继续使用失效源映射。系统 DocumentsProvider 保留旧 Root ID/authority 授权身份，仅改显示名称和内部安全实现；拒绝越界名称、符号链接和从系统文件入口直接移动/删除登记中的工程根。

## 28. UI polish与本地化扩展（规范0.4.0 / API4）

导航轨固定68dp，第二栏独立测量内容宽度，分隔条12dp；`WorkbenchPaneSizing.bounds(available:Float,fontScale:Float,requested:Float?):Bounds`输入当前可用宽度dp、系统字号缩放及可选用户内容宽度。返回modal/minimum/maximum/width，非有限输入安全回退。内容最小260×clamp(fontScale,1,1.5)dp，编辑区至少320×该缩放dp；空间不足使用抽屉，最大内容560dp。初始内容为窗口30%，限制300..420dp再按实际边界夹取。拖动/方向键16dp步进/无障碍setProgress均受边界限制，释放才持久化；缩小窗口夹取显示值而不覆盖原偏好。横竖屏、分屏由实测宽度重算。底部终端位置与原高度调节独立保留。

Files仅过滤已加载树/当前目录名称，不递归走盘；Find通过明确下拉模式选择文件内容、文件和目录名称、当前文档，显示根范围，可选Internal Files/Project，保留大小写/正则。工具栏用响应式FlowRow；Files保留新文件、新目录和刷新，其他动作进入溢出菜单；不得删除功能。行操作48dp触控区域，深树缩进有界，长名称省略但逐行Properties提供完整名称和路径。

普通创建仅有Project，文件/目录由Internal Files创建。原Workspace数据库、.jcode及PRoot/workspace映射保留；旧容器以saved project group兼容显示，不允许新建。高级共享配置保留，但不再以Default Workspace作用户前置概念。

UI本地化仅由.suik的localization声明及ui.localization权限提供。完整定义、Schema、公开键、英文回退、生命周期、参数类型和错误行为见[总规范第29节](SUIC-SUIK-Development-Specification.md#29-api-4独立-ui-本地化资源规范-040)。`LocalUiTranslations`为CompositionLocal<UiTranslations>；`uiText(key:String,arguments:Map<String,String>=emptyMap()):String`在Compose读取快照，不进行IO。第三方UI只使用其认证资源和Core开放键，不能覆盖主题、全局布局、权限确认或签名状态。默认英文，不安装任何额外运行环境。当前局部键接入不宣称已完成全应用中文翻译。

本轮Launcher图标逐字节保持原v0.5，包括常规/Debug/Beta前景和Adaptive入口；所有临时S-dot方案停用。后续图标设计由用户另行提供。本轮没有改变配色、字体大小、圆角及主题。


### UI 本地化目录版 0.4.1 / 宿主0.5.3

Core API4与UiTranslations/uiText接口保持兼容。资源键完整目录、参数和值约束见总规范第29/30节与`localization/en.json`。完整简体中文包由独立`.suik`安装，不内置中文到APK，不修改系统Locale；用户明确选择后通过CompositionLocal即时重新组合。启用、禁用、更新、缺失归档和卸载均由原资源生命周期控制，失效时回退英文并留存SuiStartup诊断。来源、权限及签名审阅保持宿主受保护英文，不能被包替换。

菜单、按钮、提示、设置标题及状态使用资源键；保存到磁盘的枚举名/配置键与显示文案分离。文件名、用户代码、终端输出、扩展作者自带标题保持原文，禁止自动对任何任意String进行字典替换。宿主事件从当前快照生成，Compose不在首屏同步扫描资源。无环境时使用原暂缓配置能力访问被动资源，不能伪装Ubuntu已安装；运行与调试仍检查真实环境。

横屏、竖屏、第二栏宽度及字体缩放沿用现有约束，文本使用既有换行、滚动和省略控件；不能以缩小中文字号或裁掉操作替代适配。覆盖表、剩余高级界面缺口与Android截图由`docs/localization/locale-key-audit.md`及版本交付报告公开。公开键中文值100%仅指资源表，不能据此宣称所有动态界面、工具输出及第三方面板都已翻译。


## 0.4.2：统一扩展卡片与管理职责

所有官方/第三方插件和Kit共用宿主OutlinedCard、现有Material主题、间距和响应式FlowRow，不能由扩展提供Android代码、DEX、JS或HTML。包类型、名称、版本、真实发布者、签名/未验证状态、来源、权限及生命周期操作具有相同布局；官方目录收录与包作者签名是不同概念。扩展卡片字段不得覆盖这些宿主安全信息。

Core数据接口为`PackageCards.forPack(VerifiedPack):PackageCard`、`CardCategory`、`LanguageKitSlot`、`CardField`，参数/返回值、默认值、错误和版本约束详见原生开发规范0.4.2。它们只提供不可执行的声明数据；安装、启停、更新、回滚、卸载仍由宿主原生命周期执行。语言IDE Kit固定顺序显示基础插件、运行环境、LSP、DAP、Build、Test、Run、Format、Lint，缺少声明显示灰色未提供，检测失败显示错误，两者不得混用。运行环境只读；IDE工具的逐组件选择、依赖闭包、来源及影响审阅、安装与日志位于该Kit卡片。非语言类别允许经Schema验证的纯文本/HTTPS链接及已有组件操作，布局、触控尺寸、键盘导航、主题继承和卸载失效仍归宿主。

Languages只管理官方运行环境及其日志，搜索使用语言中英文别名、环境、实际版本和检测状态。Packages的Plugin Library/IDE Kit Library/Installed共用顶部固定搜索，按名称、发布者、类别、版本和状态即时过滤；切换标签保留查询，清除操作明确，无结果显示空态。搜索仅过滤缓存元数据，不能调用Files/Find全文引擎或同步触发安装/网络请求。本地化Kit与语言Kit并列，任意locale均使用同一分类和生命周期，切换、禁用和卸载继续采用API4英文逐键回退。所有新增宿主文案使用公开UI资源键，作者内容保持字面文本。

### 0.4.2 runtime, search, status and log ownership

Languages contains runtime-only Java/JDK, C/GCC, C++/G++ and Python cards. Its fixed search filters environment names, compiler aliases, detected versions and installed state without archive IO or network access. Runtime discovery runs in cancellable background work. Packages has its own fixed search and keeps the query when switching Plugin Library / IDE Kit Library / Installed. It filters both signed-directory and locally imported package metadata, publisher, category, version and state; these queries never enter Files/Find search.

All package types use the same `NativePackageCard` host renderer and existing Material tokens. Catalog-only entries show reviewable provenance but do not execute or pre-load unverified component descriptors. Installed language-ide-kit cards reserve all runtime/LSP/DAP/Build/Test/Run/Format/Lint slots. Absent declarations use reduced-emphasis “Not provided”; declared capabilities are explicitly separate from actual Starting/Ready/Error state. LSP runtime state and bounded logs come from the running controller, never the catalog's installation flag. DAP slots observe the actual DebugController state only for the package owning that debug session; other packages show Not running. A declared slot never implies a live protocol session. Restart is blocked during an active debug session. The component selection review offers independent checkboxes, dependency closure, sources and transfer estimates; runtime requirements are read-only and link conceptually to Languages, without executing duplicate downloads.

Localization cards are peers of language Kits and expose the same lifecycle controls plus the host language selector. A directory entry for an unsigned localization pack must still display Unverified publisher. Trust, capability approval and final install consent use host-owned English security labels, so an imported translation cannot rewrite those decisions. All ordinary management strings use API4 keys with independent Chinese resources and English fallback. Disable/uninstall safely falls back to English while retaining the prior locale selection for re-enablement.

Current installation progress is separate from history. The runtime page reads only `environment` archives and Packages only `ide-component` archives. History shows component, source, time, distro and outcome and opens bounded log tails asynchronously; the same task must not be repeated across these two pages. File/project management and bottom-terminal sizing are unchanged by this ownership model.

## 0.4.3 / v0.5.5: focused workbench and debug interaction

The design tokens, official S. launcher artwork, colors and navigation identity are unchanged. `WorkbenchFocusControls(mode: String="NORMAL", setMode: (String)->Unit)` is host UI state, not a dynamic package code API. NORMAL retains the stored sidebar width and bottom-dock fraction. EDITOR temporarily removes the persistent sidebar and dock; DOCK expands the bottom panel and keeps the editor state composed at zero height. A pinned editor/dock maximize/restore control remains reachable; restoring reuses saved dimensions instead of persisting temporary maximized sizes. Opening a program-input terminal restores a usable normal layout. Hiding/resizing/panel switching does not stop a task or detach its process ownership.

`WorkbenchDetailDialog(title: String, onDismiss: ()->Unit, content: @Composable ()->Unit)` and `WorkbenchReviewDialog(onDismissRequest, title, text, confirmButton, dismissButton)` are **host Compose** containers. They are not callable package-supplied Kotlin/DEX. Their window is bounded by available width/height/IME, with a scrollable body and reachable close/back/actions; they never grant capabilities. Kit cards can open the same real operations in a large detail window. Installation review preserves the existing exact plan/source/capability checks. Closing a review window does not silently cancel an already running installer; explicit safe cancel remains available. Settings and long logs offer a large-window view without replacing editor files or stopping the terminal.

Debug program input uses the existing Terminal tab and PTY. Its non-modal transport toolbar and collapsible/resizable variables/stack region are available without an AlertDialog blocking the editor. DAP stdout is protocol, not a text input destination. Permission/source review is the only intentionally modal launch step; commands/environment changes are inspected before execution, with protected host trust text.

Terminal labels are stable `Shell N` or language/task/file labels. Completion/cancellation/code is distinct from a shell title; user rename is preserved. Tabs scroll within a weighted region while count/new/overflow controls remain accessible. Closing one running session uses the existing confirmation. Close-finished selects only ended task sessions proven idle by kernel evidence; unknown/restricted process metadata is busy. Reuse-idle focuses a matching existing shell without killing or merging process trees. Pinned/relocated/running sessions retain their protections. `TerminalIdleProbe.inspect(pid, expectedHostDirectory?, proc=/proc): Proof` is a host utility; its `idle/detail` result is evidence, not a permission grant. Kernels without the children pseudo-file use owned-UID `stat` relationships. All backend work stays off the UI thread; inability to inspect causes a conservative refusal, not automatic termination.

Task result cards use the host's localized status keys, actual exit/start/end/duration, source context and log location. Unknown exit is shown as unknown. Long raw output is previewed with explicit bounds, never described as complete after truncation. New English keys are Core-owned; optional updated Chinese `.suik` resources preserve older translations and API4 fallback/lifecycle. Unverified publisher labels are not converted into official trust.
