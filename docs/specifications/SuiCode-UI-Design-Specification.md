# sui-code UI Extension & Design Specification

规范版本 **0.1.0（实验设计协议）**；对应 Core API **1**、SUIC/SUIK **1.0**。配套规范：[原生扩展总规范](SUIC-SUIK-Development-Specification.md)。本规范不允许重新依赖 JCode UI 插件体系。

## 1. 范围、实现状态与品牌

品牌名为 `sui-code`。应用包身份、签名和私有数据目录不因视觉品牌改变。保留上游 MIT 版权与第三方许可证；不得把改名作为移除版权的依据。

**当前实现的是独立设计 Token、响应式计算、声明式面板模型与授权状态存储。Android 全局 Compose 渲染、图标资产和页面迁移尚未完成；现有应用仍显示旧 UI。本文的产品设计要求不是“已完成 UI”的证明。** 本规范明确区分可调用 API 与必须满足的渲染要求，避免第三方调用不存在的 Compose API。

sui-code 的视觉目标为信息清晰、编辑器优先、平板高效。不得用过度胶囊化卡片压缩工作区。应用主导航和扩展面板由 host 管理；扩展仅贡献命名空间内内容，不能替换应用入口。

## 2. Light / Dark 模式

系统跟随、Light、Dark 为产品应支持的三个选项；模式切换不重启会话、不丢失编辑/终端状态。两套颜色必须覆盖同一 Token 集，不允许以反色算法临时生成 Dark。

实际 API：`DesignSystem.colors(dark:Boolean):Map<String,String>`，参数必填，返回只读语义的映射快照；不需权限，不读取系统设置，没有 I/O，不修改全局主题。调用时机为 host 创建/切换主题；第三方 panel 必须继承 host 选择，不得自设全局模式。当前函数不代表 Android 系统主题选择器已接入。

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

新增图标资源必须有明确许可，不复制上游商标作为 sui-code 自有标识。图标按钮必须有本地化 contentDescription；装饰图标明确不可聚焦。当前规范未交付最终品牌图标资产，不将图标表述为已上线。

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
| `PanelState` | 构造必填 `id:String,title:String,text:String`；可选 `status:String="ready"`。id为`[a-z][a-z0-9.-]{0,63}`；title1..80；text<=65536；status枚举ready/loading/empty/error。非法参数抛IllegalArgumentException |
| `PanelStore.put` | 必填 `session:CoreSession,panel:PanelState`；返回Reply。校验已激活会话、组件ID、PANEL_OPEN、ui.panel授权和backend；只有accepted才保存 |
| `PanelStore.snapshot` | 无参数，返回`List<PanelState>`快照，不触发渲染/执行 |
| `PanelStore.release` | 必填`packageId:String`，返回Unit；移除该包命名空间状态，禁用/卸载时host调用 |

内部键为 `packageId:panelId`；同包更新替换自身状态，不覆盖其他包。Snapshot含纯模型，不授予修改全局布局能力。源码未实现自定义布局DSL，不能在manifest添加任意UI类型。

完整样例见 [`examples/special-feature/ide/statistics.json`](examples/special-feature/ide/statistics.json) 及 [`examples/minimal-kit/`](examples/minimal-kit/)。验证通过不等于Android已经显示该面板。

## 17. 工具栏、菜单和命令

组件 type `command`/`workflow` 使用 `{operation,arguments}` 描述；Schema与总规范一致。命令只能调用自身组件注册的 Operation，权限继承请求所需能力；不能任意路由其他包或提升成Core内部管理命令。

本版本**没有导出工具栏/菜单位置注册或快捷键覆盖API**。第三方不得捏造 `registerToolbar` 等接口。扩展槽位待 host renderer 完成后独立版本发布；在此之前只能声明经过验证的请求，不伪称能注入菜单。

## 18. UI 生命周期与状态

验证不创建面板；安装默认disabled；启用会话后经过PANEL_OPEN授权才可更新。禁用/卸载时host先撤销会话并release包面板，关闭关联任务；重新启用须走实际状态读取，不复用已close会话。

状态更新应在host指定UI调度器应用；模型可在任意线程创建，PanelStore方法同步保护。当前纯模块没有Android LifecycleOwner或Compose记忆状态。旋转恢复须保留业务状态并重建布局，不能将Activity引用保存进扩展模型。

## 19. 事件、交互与响应

实际统一接口为 `CoreSession.request(Request):Reply`，请求字段、权限和失败码见总规范。PANEL_OPEN参数由PanelStore写入title/text/status。`Reply.accepted=false`必须显示可理解失败，不能继续显示假成功。

当前没有公共click事件订阅、回调URL、DOM消息或任意事件总线；面板是只读声明式内容。需要复杂交互的第三方Kit必须等待明确版本的host能力，不能通过脚本/原JCodeWebView桥接绕过。后续接口须定义事件来源、作用域、取消及版本并增加测试。

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

横屏和竖屏都必须覆盖，分屏及键盘弹出后重新计算。禁止简单缩小全部控件。侧栏若会令编辑器不可用，优先收起或切换布局，而不是覆盖编辑器主要输入区。现有Compose界面尚未完成这些迁移，不能据模型单测宣称布局通过。

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

0.1.0：公开Token、widthClass/terminalHeight纯计算、PanelState/PanelStore、权限模型和声明式UI边界。对应API1、SUIC1.0、SUIK1.0。尚无稳定Compose扩展ABI、最终图标资产或AndroidUI重构完成声明。

Token删除、语义改变、字段删改、事件/权限变化须在设计规范、源码、Schema、样例和测试同步发布。稳定期兼容策略遵循总规范第3节；拒绝依赖JCode UI插件或隐式全局覆盖来保持“兼容”。
