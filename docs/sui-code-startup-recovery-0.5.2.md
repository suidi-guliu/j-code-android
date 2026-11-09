# sui-code v0.5.2 启动紧急修复报告

基线：`sui-code/v0.5-ui-polish` / `f6bf5ad29e8e8ddabdc0b546357e4ec70c29a1b1`。修复分支：`sui-code/v0.5.2-startup-recovery`。P0仅修启动与补测试/诊断，不继续UI设计或三语言功能开发；main、官方上游及旧v0.4/v0.5/v0.5.1交付目录保持不变。

## 已完成的 Android 验证与真实下载

2026-11-09，[Android 启动及交付工作流](https://github.com/suidi-guliu/j-code-android/actions/runs/42557387384) **Success，6分0秒**；[下载最终 Artifact](https://github.com/suidi-guliu/j-code-android/actions/runs/42557387384/artifacts/13647164754)（需登录GitHub，保留30天）。Artifact名`sui-code-v0.5.2-startup-recovery`，内含唯一修复主APK、报告、源码补丁/源码包、SDK、构建日志、完整Android截图与测试日志。Artifact归档摘要为`2726a6307295d7b341c0bb5ce3458043bad6a64b6f1c6eecb17274a224a8843b`；它与下文主APK摘要是不同对象。

实际执行源码/测试/交付提交：`8cc1e1b0694944b9adea3223d22b2bac4bd5ae93`，主APK生产源码仍是`4caff73cd264fa3b9c0cf3f4624da4e1da968b27`，之后只改测试、报告、交付机制。上传后再次下载，全部文件SHA256和逐字节比较通过。

- Android16独立x86_64 AVD，ARM64 APK通过系统native bridge运行，**不是ARM64实机**。
- 原v0.5首屏正常；保留数据覆盖v0.5.1后，在真实Activity中再次复现`PatternSyntaxException / UiTranslations.text`。
- 覆盖v0.5.2后重复启动、原项目元数据与13个夹具文件SHA256保留通过；另一个新AVD全新安装和重复启动通过。
- **8例Android回归 + 2例原版Project创建/保留验证全部通过**，无未通过用例被跳过以交付。坏偏好、有效/损坏/缺失语言资源、英文回退及原API3包共存均包含在内。
- 没有安装Ubuntu的AVD正常显示既有Environment setup引导；环境目录哨兵不冒充已运行Ubuntu/JDK，真实平板启动及完整环境保留仍需下一节验收。

## 1. 已复现的实际错误与证据边界

在Android16 x86_64模拟器（提供ARM64 native bridge）的ART/libcore上，直接以原始已交付v0.5.1 APK的DEX运行`UiTranslations.text("nav.files", emptyMap())`，得到：

```text
SHIPPING_DEX_ENGLISH_STARTUP_FAILED
java.util.regex.PatternSyntaxException: Syntax error in regexp pattern near index 22
\{([a-z][a-zA-Z0-9]*)}
 at com.android.icu.util.regex.PatternNative.compileImpl(Native Method)
 at java.util.regex.Pattern.compile(Pattern.java:921)
 at kotlin.text.Regex.<init>(Regex.kt:90)
 at dev.suicode.packages.UiTranslations.text(Localization.kt:72)
```

原表达式未转义结尾右花括号。OpenJDK正则允许这一写法，Android的ICU拒绝；所以此前JVM测试通过不能证明Android首屏可用。默认英文也会进入该函数，不需要用户安装或选择中文包。v0.5.1的Run file/导航等首屏文字会调用此路径。这是已验证的真实崩溃点，不是凭用户描述猜测异常。

修复版同一实际APK DEX在相同Android运行时返回`SHIPPING_DEX_ENGLISH_STARTUP_PASS: Files`。Linux ICU76也独立复现旧式U_REGEX_RULE_SYNTAX，但只作为辅助证据，不冒充Android结果。

没有用户平板logcat，不能证明该设备仅有这一问题，也不能宣称用户真实ARM64设备闪退已解决。首屏与保留数据升级由最终Actions的独立KVM模拟器验证强制把关；只有通过才上传最终Artifact，具体通过结果见上方运行回执与Artifact中的Android原始日志。

## 2. 实际修复

1. **去除平台差异**：Core占位符验证/渲染共用字面扫描器，按ASCII命名槽解析与一次性替换，不再依赖ICU/JVM的正则宽容行为。保留资源键、占位符集合、安全校验、英文回退与API4/包格式。
2. **初始化顺序**：MainActivity恢复由JCodeRoot正常创建ViewModel；NativeLocalizationProvider在既有主题下复用同一NativePackageHost，不在主界面前额外构造Host。可选快照构建失败记录完整异常并回退英文；协程取消重新抛出；不包裹吞掉整个UI子树异常。
3. **可选偏好**：新增语言选择、第二栏宽度从不受信任的SharedPreferences值进行类型/范围检查，缺省/错类型/非有限值安全回退。保留原文件与坏值，不清DataStore、包状态或用户目录；写入仍由用户操作触发。
4. **坏资源隔离**：单个语言归档损坏/缺失按原校验拒绝、保留错误和文件，资源列表回退。包恢复/读取异常进入诊断，不自动卸载、覆盖、重装或无限重试。原API3六个官方包及存储格式不变。
5. **启动诊断**：`SuiStartup`记录生命周期阶段与完整异常栈；未捕获异常先写诊断再交还Android原处理器，不把真实致命错误假装成成功。私有`files/sui-code/diagnostics/startup.log`有界轮转；同类可选异常每进程只记录一次。只读采集脚本`tools/device/collect-startup-diagnostics.sh`不清日志、不强停、不卸载或重装。

## 3. 启动差异审计

已比较v0.5.0到v0.5.1的MainActivity/JCodeRoot、NativePackageHost构造/restore、MainViewModel初始化、本地化、偏好、布局模型与资源。

- AndroidManifest、Application声明、图标、包身份在v0.5.1 UI修整中没有导致此次差异的更换；本修复继续保留。
- WorkbenchPaneSizing对缺省、NaN、无穷、极大/负值及窄屏给出有限非负边界；没有复现其纯数学计算异常。新增可选偏好类型错误确可在此前getFloat/getString处导致ClassCastException，已独立保护，不混称用户根因。
- NativePackageHost旧存储格式没有变，API1/2/3与API4兼容；本地化不应要求Ubuntu/JDK或在首屏执行脚本。
- core/fs、core/distro、core/lsp、core/debug、core/term及三语言原始包/安装环境未改；第二栏拖动/持久化、Files/Find改进、Project、Run/Debug/Overview、底部终端继续保留。

## 4. 测试

云端JVM：**254例，0失败**。fs26、distro66、native-packages64、design61、editor3、term4、lsp1、debug1、search3、app25。新增全部Core英文键、槽解析、旧API3六包重开与API4共存、坏偏好保留/默认值及极端布局输入覆盖；三语言基础文件执行使用真实Linux测试工具/JDK27，不能当ARM64实机验收。

[独立GitHub JVM回归](https://github.com/suidi-guliu/j-code-android/actions/runs/42551247185)。Schema89个声明、非法包拒绝及9个Launcher源资源基线校验继续通过。

Android测试已编译4个方法：旧ICU表达式与修复英文渲染、真实Activity重复启动、坏SharedPreferences保留、语言包启用后损坏/缺失的回退及首屏。最终交付工作流在两个独立AVD分别执行（8例回归，另有2例原版Project创建/保留验证），并包含：

- 原v0.5正常首屏、经原v0.5生产ViewModel API创建真实可选Project（不直接改数据库）；真实六个API3包与项目文件配置夹具。
- 保留数据覆盖安装原v0.5.1并采集实际Activity崩溃。
- 覆盖修复版、三次冷进程启动、坏UI偏好启动及真实Project元数据继续可读。
- 13个旧包/源码/配置/环境路径哨兵SHA256保留；新AVD全新安装；截图、层级、logcat、诊断和仪器测试结果。

**无Ubuntu的模拟器正常首屏是既有Environment setup引导页，不假装含已安装环境的完整工作台已实测。** Project元数据通过原版生产API创建与读取；环境路径哨兵只是保留数据夹具，不是运行过的Ubuntu/JDK。 不会据此宣称真实Ubuntu24.04/JDK/GCC/Python运行通过。云端宿主/dev/kvm不存在；本地TCG模拟器可运行ART探针，但Framework包管理启动严重受限，因此使用GitHub KVM完成Activity测试。Artifact中的运行时回执记录实际通过情况，失败时只保留诊断，不分发未经启动验证的APK。

## 5. 构建、签名与图标

ARM64完整Debug构建成功，另有开发用instrumentation测试APK。对外修复版只有一个：

- 文件：`sui-code-v0.5.2-startup-debug.apk`
- 大小：**87,061,016字节**
- 包名：`dev.suicode.ide.debug`
- versionName / versionCode：`0.5.2 / 1070805`，高于v0.5.1的1070804。
- 原签名证书SHA256：`62c2da3484d337626eac6711e5ea7015913e23b8932e9f2b2bd220b843579fa0`，已实际与v0.5.1比较一致。
- APK内15个Launcher资源与原v0.5.1逐字节相同（该版已恢复v0.5原图标），不设计新图标。
- 主APK构建源码：`4caff73cd264fa3b9c0cf3f4624da4e1da968b27`；其后的测试/交付提交不改此APK。

```text
APK SHA-256
05ded85495070c8e4735fd3c539f1da02ea29ada120af70c47d9811a04abf3b5
```

满足相同身份/证书和更高版本的覆盖安装条件；没有数据迁移/清理或环境重装代码。旧v0.4/v0.5/v0.5.1全部SHA256SUMS单独核验。升级数据实机结果仍需用户平板确认，不要求卸载或清数据。

## 6. 下载与交付内容

[启动验证及整合交付工作流](https://github.com/suidi-guliu/j-code-android/actions/workflows/deliver-startup-v0.5.2.yml)。成功运行的Artifacts提供`sui-code-v0.5.2-startup-recovery`，登录GitHub下载，保留30天。只有Android升级/全新启动验证通过后才上传，并再次下载逐字节回验。

包含主APK、report.md、changes.patch、源码、SDK、文件清单、构建/测试证据、Android截图/日志和开发用测试输入。instrumentation APK封装在测试输入zip中，是开发验证工具，不是另一个中间应用版本，也不要求用户安装。运行时报告/元数据及SHA256SUMS由Actions追加真实结果，主APK不重构或重新签名。

[原生扩展规范](https://github.com/suidi-guliu/j-code-android/blob/sui-code/v0.5.2-startup-recovery/docs/specifications/SUIC-SUIK-Development-Specification.md)仍为API4/包格式1.0，已追加Android兼容实现说明，接口和安全边界不变。

## 7. 仍需用户ARM64平板确认

模拟器不是MediaTek真实ARM64设备；没有该平板logcat，不能宣布实机故障消失。最终APK只用于这轮启动恢复验收，不提前推进UI或三语言完整功能验收。

1. 备份重要文件后直接覆盖安装；不要卸载、清数据或重新安装Ubuntu。签名冲突立即停止。
2. 点击图标确认首屏出现、退出/重开3次，确认图标、Project、文件、扩展、原Ubuntu/JDK路径及偏好仍在。
3. 确认第二栏、Files/Find、Languages、Run/Debug/Overview及底部终端仍可访问；不要求立刻重测全部语言。
4. 若仍闪退，保留安装和数据；后续能连接ADB时运行只读诊断采集脚本。新包会尽可能留下完整异常阶段与栈，不反复自动重启。
