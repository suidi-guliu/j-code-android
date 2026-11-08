# sui-code v0.5.1 UI polish — 实现、测试与交付报告

基线为已交付v0.5 `d4ea352432d075a13e84bc9cfe3e558455ea3701`，任务分支`sui-code/v0.5-ui-polish`。已检查并保留该分支原有历史；图标临时方案按用户最新指令撤销。本轮不扩展三语言运行功能，不修改main/官方上游，不创建Release，不改旧v0.4/v0.5交付。

## 1. 任务结果

|任务|实际修改及边界|
|---|---|
|恢复图标|常规、Debug、Beta前景恢复原v0.5；全9个Launcher源资源逐字节校验。没有生成新图标或调整渐变/比例。历史建议文档已停用。最终APK内15个编译后的Launcher资源也与原v0.5逐字节相同。|
|第二栏|修复原284/236dp总宽度包含68dp导航轨导致内容仅216/168dp的根因；独立计算内容宽度，按实测可用宽度、字号和编辑区下限夹取。持久化用户宽度；拖动、方向键16dp步进、无障碍调节/重置；窄窗口用抽屉。底部终端位置/高度调节独立保留。|
|响应式操作|Files工具栏保留新文件/目录/刷新，其他动作及批量动作进入可达溢出菜单；行操作48dp，深树缩进有界，Properties能看完整名称和路径。Find模式下拉与选项分行，输入区可滚动。Languages/Packages按钮改为FlowRow，界面语言选择用弹窗避免挤占列表。|
|Workspace入口|NewItemDialog移除类型参数/控件；OpenFolderTypeDialog只添加可选Project上下文；VM拒绝过期Workspace创建请求。命令面板分类改为Files，新建Project语义明确；设置中的旧共享配置标作Files/高级共享YAML。旧容器可作为saved project group打开/关闭，数据库、.jcode、用户目录与PRoot /workspace映射保留。|
|Files / Find|Files只筛选已加载树/当前目录名称，不递归走盘；明确提示Find用于深层搜索。Find保留内容/名称/当前缓冲区三模式，名称包含目录，显示并可选Internal Files/Project范围；保留大小写/正则；文件定位代码行，目录定位Files。旧搜索输入立即取消、错误正则可恢复，快照避免同数量结果陈旧；目录搜索不跟随符号链接。|
|本地化|Core API4新增Kit-only localization声明与ui.localization授权；认证资源校验、公开键、命名占位符、英文逐键回退、明确来源选择、启停/更新/卸载及恢复流程已接通。资源包不要求Ubuntu/PTY或语言环境。源码、SDK、完整Schema、两规范和可安装示例同步更新。|

**本轮并未完成整套中文翻译。** `zh-hans-kit.suik`是独立、未签名、12条部分翻译的开发示例；安装需正常未验证发布者确认及ui.localization授权。它不是官方完整中文语言包。Core只含英文默认值；中文文本位于独立包资源/开发测试中。当前导航、Files筛选、Find模式、主运行操作和中心标题等公开键已接入，其余既有高级界面继续英文回退。

## 2. 布局与生命周期要点

导航轨68dp，内容初始为窗口30%（先限制300..420dp，再按实际边界夹取）；最小260×字号缩放，最大560dp，并保留至少320×字号缩放的编辑区。缩放使用1..1.5范围；低于容纳条件切抽屉。12dp分隔条在释放时保存内容dp宽度，不在每个指针事件写磁盘。缩小窗口只夹取显示值，不覆盖之前较宽偏好，放大可恢复。

Files各项既有复制/剪切/移动/粘贴、创建、搜索、排序、多选、属性、Trash恢复、导出仍有入口。普通文件无需Project。旧Workspace元数据只兼容读取/管理，不迁移或删除文件，不重命名真实PRoot路径。

语言选择在Packages → Interface language。安装资源仅增加可选项，默认英文；不同包同locale按来源分别选择，不自动抢占。选中包禁用/卸载后立即英文回退，保留偏好以便重新启用恢复。security/permission/signature确认文本不开放翻译，纯数据不能覆盖全局主题或执行代码。

## 3. 关键修改文件与提交

- JCodeShell.kt、WorkbenchPaneSizing.kt、WorkbenchPaneResize.kt：宽度模型、拖动/持久化、抽屉。
- ExplorerView.kt / ExplorerFeature.kt、SearchToolPanel.kt、SearchEngine.kt：快速过滤、动作菜单、完整搜索与目录定位。
- WorkbenchDialogs.kt、WorkbenchChromeBars.kt、WorkbenchPanels.kt、SettingsFeature.kt、MainViewModel.kt：Project-only创建、旧容器兼容。
- Localization.kt、PackageVerifier.kt、Descriptors.kt、CoreApi.kt、NativePackageHost.kt、NativeLocalization.kt、NativeKitSetup.kt：真实本地化协议与加载/授权/无环境安装。
- docs/specifications、tools/sui_packages、tools/ui：正式规范、SDK、示例及图标校验工具。

完整提交/文件清单与补丁随Artifact；APK构建源码与归档提交的对应关系写入apk-metadata.json。

## 4. 自动化及基础集成测试

云端JVM回归已通过 **248例，0失败**：fs26、distro66、native-packages61、design61、editor3、term4、lsp1、debug1、search3、app22。[GitHub独立回归也已成功](https://github.com/suidi-guliu/j-code-android/actions/runs/42495679946)（7分13秒）。最终`:app:assembleDebug :app:testDebugUnitTest`也已成功（2分52秒，703项任务）；签名、包身份与图标均已实际核验。范围包括：窗口/字号/旋转夹取模型、真实文件与目录名称搜索/大小写/正则/取消/链接边界、真实PackageStore安装启停卸载与英文回退、无环境依赖、占位符/保护键/未知来源冲突、认证资源缺失/校验失败/API权限边界；以及既有三语言QuickRun、文件安全、包管理、保存、终端、LSP/DAP生命周期的JVM回归。

Schema/示例检查89个有效声明，非法插件仍被拒绝。Launcher基线校验9个资源。Standalone SDK已独立构建（34秒）；13个生产API源文件逐字节相同，生产验证器检查18个包：17个合法包通过，非法插件E_BOUNDARY拒绝。

本轮**不重复宣称**上轮Ubuntu x64三语言LSP/DAP协议测试为新增实机验证；本轮未改变LSP/DAP运行器核心。用户已反馈v0.5文件/项目/插件/扩展保留、Languages迅速打开且暂未重现ANR，这只记录为用户已观察结果，不扩大为本包新实机通过。

## 5. APK及升级安全

实际包名`dev.suicode.ide.debug`、versionName`0.5.1`、versionCode`1070804`（高于1070803）、仅ARM64。apksigner已实际与v0.5比较且一致，证书SHA256为`62c2da3484d337626eac6711e5ea7015913e23b8932e9f2b2bd220b843579fa0`。APK文件：`sui-code-v0.5.1-ui-polish-debug.apk`，**87,042,788字节**。SHA-256：

```text
fcd8b4801e8fa01173ea06bea657f13b14ae1568043bb360f10e5f5d8a47b27f
```

badging、签名与15项编译图标比对日志随交付。功能提交`cda9bdc`，版本配置修正提交`2470902`；APK以`2470902a12e5d1d192febb1b21069ca503ffc78c`构建，后续只追加报告与交付元数据。

本轮没有数据迁移/清理、没有环境重装、没有应用身份变化。布局偏好和语言选择使用新增独立偏好键。旧v0.4/v0.5 SHA256SUMS应保持通过。身份/签名/版本条件通过不能替代真实设备覆盖升级数据保留验收。

## 6. 下载及公共规范

[整合交付工作流及Artifact入口](https://github.com/suidi-guliu/j-code-android/actions/workflows/deliver-ui-polish-v0.5.yml)：在成功运行的Artifacts下载`sui-code-v0.5-ui-polish-integrated`，实际成功运行与直达入口见文末公开回执；Artifact内报告是构建时冻结版本。工作流不重构/重新签名APK，上传后下载并逐字节核验全部交付文件。包含一个整合APK、report.md、changes.patch、modified-files.txt、source.zip、native-sdk.zip、部分中文示例包、构建日志、校验值及测试证据。Artifact需登录GitHub，保留30天。六个官方语言包及官方目录保持v0.5原字节，不重新封装运行环境；新本地化开发样例单独提供。

- [Native Extension Development Specification](https://github.com/suidi-guliu/j-code-android/blob/sui-code/v0.5-ui-polish/docs/specifications/SUIC-SUIK-Development-Specification.md)
- [UI Extension Design Specification](https://github.com/suidi-guliu/j-code-android/blob/sui-code/v0.5-ui-polish/docs/specifications/SuiCode-UI-Design-Specification.md)

## 7. 仍需ARM64实机验收

没有连接用户平板，不能宣称像素布局、拖动手感、鼠标悬停、TalkBack、键盘、横竖屏/分屏、大字号、升级数据保留或Languages性能已在新包实测。没有伪造logcat、截图、Perfetto或Android端到端结论。

一次性覆盖安装后：

1. 不卸载/不清数据，确认图标与原v0.5一致，原文件/项目/扩展/Ubuntu仍在。
2. 横屏/竖屏/分屏/字号变化检查第二栏；拖动、键盘调整后重启验证宽度恢复；确认编辑区保留，终端仍在底部且可调高。
3. 检查Files/Find/Languages/Packages操作不裁剪；长文件名经Properties查看完整路径；溢出菜单可访问全部文件操作。
4. 新建Project、普通文件和多层目录；确认没有Workspace创建选项；旧项目组、共享配置及导出仍可用。
5. Files只筛选当前可见名称；Find名称搜索包含深层目录并能打开，内容搜索能跳行，正则错误可恢复，切换范围/快速输入不显示过期结果。
6. 导入部分中文示例.suik，审阅未验证来源/权限；确认不会要求安装JDK/Python/Ubuntu。选择中文后检查接入键和英文回退；禁用/卸载即时英文，重启与重新启用恢复。
7. Languages冷开/反复进入保持迅速；确认Run file/Debug/Overview与终端入口仍在。三语言完整实机功能回归留下一阶段统一进行。

## 8. 公开交付回执

- [交付运行：Success](https://github.com/suidi-guliu/j-code-android/actions/runs/42496587688)，交付提交`3063629`。上传、下载及全部字节/SHA256回验成功；实际包身份、ARM64、旧签名、旧交付校验和15项Launcher资源比较通过。
- [下载完整Artifact：sui-code-v0.5-ui-polish-integrated](https://github.com/suidi-guliu/j-code-android/actions/runs/42496587688/artifacts/13623357817)。登录GitHub后下载，保留30天；只有一个最终APK。
- [Gradle完整构建日志](https://github.com/suidi-guliu/j-code-android/blob/sui-code/v0.5-ui-polish/.delivery/sui-code-v0.5-ui-polish/build.log)；[实际APK元数据](https://github.com/suidi-guliu/j-code-android/blob/sui-code/v0.5-ui-polish/.delivery/sui-code-v0.5-ui-polish/apk-metadata.json)；[SHA256SUMS](https://github.com/suidi-guliu/j-code-android/blob/sui-code/v0.5-ui-polish/.delivery/sui-code-v0.5-ui-polish/SHA256SUMS)。
- 功能提交`cda9bdc`；版本/实际构建提交`2470902a12e5d1d192febb1b21069ca503ffc78c`；冻结源码归档提交`6535aef1216478ca7612b5319bd68f0c9df52782`。之后本公开报告只追加真实交付回执，未改变APK。
- 完成了生产源码、相关测试、ARM64构建、签名/图标/历史产物核验及真实下载入口；第7节的Android平板实机项仍待验证，不用云端结果替代。
