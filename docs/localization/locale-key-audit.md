# UI 本地化资源接入清单

由 `python3 tools/localization/audit.py` 从生产源码及独立语言包资源生成。清单为词法扫描，动态后端日志和扩展作者文案不据此声称已翻译。**资源键翻译率不是全应用汉化率。**

公开键：732；中文值：732；键引用位置：900；仍有硬编码位置：667（分别列出受保护宿主英文、技术原文及缺口）。

## 页面/源码汇总

|源码|全部键引用|显示参数内键引用|未接入缺口|受保护英文|技术原文|
|---|---:|---:|---:|---:|---:|
|app/src/main/java/dev/blamspot/jcode/BackendService.kt|0|0|0|0|2|
|app/src/main/java/dev/blamspot/jcode/CommandPalette.kt|0|0|2|0|0|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt|21|15|0|0|3|
|app/src/main/java/dev/blamspot/jcode/IssuesPanel.kt|5|5|0|0|4|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt|141|139|21|1|19|
|app/src/main/java/dev/blamspot/jcode/JavaQuickRunController.kt|0|0|0|0|1|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt|62|62|42|0|54|
|app/src/main/java/dev/blamspot/jcode/PaletteCommandsUi.kt|3|3|7|0|0|
|app/src/main/java/dev/blamspot/jcode/ProviderReleaseFetcher.kt|0|0|0|0|1|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt|0|0|15|0|1|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt|8|8|5|0|3|
|app/src/main/java/dev/blamspot/jcode/ToolchainManagerPanel.kt|0|0|6|0|3|
|app/src/main/java/dev/blamspot/jcode/WorkbenchStatusBar.kt|0|0|2|0|1|
|app/src/main/java/dev/blamspot/jcode/debug/AndroidAppProject.kt|0|0|1|0|1|
|app/src/main/java/dev/blamspot/jcode/debug/AndroidDebugAttach.kt|0|0|0|0|1|
|app/src/main/java/dev/blamspot/jcode/ext/NativeExtensionPage.kt|0|0|2|0|0|
|app/src/main/java/dev/blamspot/jcode/lsp/LspController.kt|0|0|2|0|0|
|app/src/main/java/dev/blamspot/jcode/run/BuildConfigPage.kt|0|0|7|0|0|
|app/src/main/java/dev/blamspot/jcode/run/ProjectRunner.kt|0|0|2|0|4|
|app/src/main/java/dev/blamspot/jcode/run/RunConfigPage.kt|0|0|11|0|0|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt|0|0|30|0|1|
|app/src/main/java/dev/blamspot/jcode/workbench/BrowserPage.kt|0|0|14|0|0|
|app/src/main/java/dev/blamspot/jcode/workbench/BuiltinBrowser.kt|0|0|9|0|1|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt|0|0|26|0|16|
|app/src/main/java/dev/blamspot/jcode/workbench/ExtensionDevPanel.kt|0|0|2|0|2|
|app/src/main/java/dev/blamspot/jcode/workbench/ExtensionWebView.kt|0|0|0|0|1|
|app/src/main/java/dev/blamspot/jcode/workbench/MarkdownPreviewPage.kt|0|0|11|0|2|
|app/src/main/java/dev/blamspot/jcode/workbench/NativeCommandStrip.kt|12|11|0|0|0|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt|19|9|0|0|2|
|app/src/main/java/dev/blamspot/jcode/workbench/SetupTerminalRunner.kt|0|0|1|0|0|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt|16|16|7|0|2|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChrome.kt|0|0|1|0|1|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt|23|23|0|0|1|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPaneResize.kt|0|0|2|0|1|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt|20|20|0|0|2|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/LspDialogs.kt|0|0|6|0|2|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt|23|23|1|0|7|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt|0|0|16|0|4|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt|0|0|52|0|4|
|app/src/main/java/dev/suicode/nativehost/NativeKitPanel.kt|2|2|0|0|5|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt|28|28|0|0|11|
|app/src/main/java/dev/suicode/nativehost/NativeLocalization.kt|4|4|0|0|2|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt|41|38|0|3|19|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt|0|0|0|22|1|
|core/design/src/main/java/dev/blamspot/jcode/design/CompactSearchField.kt|0|0|1|0|0|
|core/design/src/main/java/dev/blamspot/jcode/design/IconPackLoader.kt|0|0|0|0|2|
|core/design/src/main/java/dev/blamspot/jcode/design/JCodeLineIconSet.kt|0|0|1|0|0|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerDetailScreen.kt|7|7|0|0|1|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerPanelHeader.kt|5|5|0|0|2|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerSectionCard.kt|0|0|2|0|0|
|core/design/src/main/java/dev/blamspot/jcode/design/SettingsAutocompleteRow.kt|0|0|2|0|0|
|core/design/src/main/java/dev/blamspot/jcode/design/SettingsResettableRow.kt|1|1|0|0|0|
|core/design/src/main/java/dev/blamspot/jcode/design/TabColorDialog.kt|4|4|0|0|0|
|core/design/src/main/java/dev/blamspot/jcode/design/ThemeBundle.kt|0|0|3|0|0|
|core/design/src/main/java/dev/blamspot/jcode/design/UiIconSet.kt|0|0|1|0|0|
|core/design/src/main/java/dev/blamspot/jcode/design/VersionSection.kt|0|0|3|0|0|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt|23|23|0|0|0|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/LocalizedPageTitle.kt|5|0|0|0|0|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/VariableInspector.kt|0|0|9|0|0|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt|84|76|0|0|26|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt|0|0|4|0|18|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/VsixPackage.kt|0|0|1|0|7|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt|20|20|23|0|3|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt|19|19|10|0|3|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt|11|11|10|0|1|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt|293|252|0|1|19|

## 实际资源键绑定

|源码位置|Core 资源键|简体中文|状态|
|---|---|---|---|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:67|ui.debugSession|调试会话|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:103|debug.target|调试 {target}|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:110|ui.startDebugging|开始调试|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:114|ui.startDebugging|开始调试|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:126|debug.missing|未安装适用于 {target} 的调试引擎。请从 IDE Kit 配置调试依赖。|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:143|run.continue|继续|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:144|ui.stepOver|单步跳过|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:145|ui.stepInto|单步进入|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:146|ui.stepOut|单步跳出|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:148|ui.stop|停止|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:181|ui.callStack|调用栈|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:216|ui.variables|变量|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:298|ui.startADebugSessionToSeeItsConsoleOutput|启动调试会话后，其控制台输出会显示在这里。|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:307|debug.console|调试：{target}|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:307|run.debug|调试|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:325|option.Starting|正在启动…|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:326|option.Running|运行中|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:327|option.Paused|已暂停|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:328|option.Error|错误|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:329|option.terminated|已结束|已绑定|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:329|option.disconnected|未连接|已绑定|
|app/src/main/java/dev/blamspot/jcode/IssuesPanel.kt:65|ui.noIssuesDetected|未发现问题。|已绑定|
|app/src/main/java/dev/blamspot/jcode/IssuesPanel.kt:70|ui.configurationErrorsAndOnSaveSyntaxChecksAppearHere|配置错误和保存时的语法检查结果将显示在这里。|已绑定|
|app/src/main/java/dev/blamspot/jcode/IssuesPanel.kt:165|issues.position|第 {line} 行，第 {column} 列|已绑定|
|app/src/main/java/dev/blamspot/jcode/IssuesPanel.kt:213|ui.hideDetails|隐藏详情|已绑定|
|app/src/main/java/dev/blamspot/jcode/IssuesPanel.kt:213|ui.showDetails|显示详情|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:778|ui.importAnExtension|导入扩展|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:800|ui.chooseFile|选择文件|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:805|ui.cancel|取消|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:834|ui.cleanUpAndUninstall|清理并卸载|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:834|ui.removeTheBundle|移除迁移包|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:840|ui.keepForNow|暂时保留|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:852|ui.updateWithoutYourEnvironment|不迁移环境，直接更新？|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:858|ui.installingNowGivesYouTheNewVersionWithNothing|现在安装将获得一个尚无数据的新版本应用；当前应用保持不变。也可先释放空间，再重试迁移更新。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:863|ui.installAnyway|仍然安装|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:871|ui.notNow|暂不|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1149|ui.restart|重新启动|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1157|ui.reload|重新加载|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1164|ui.androidStoppedTheLinuxEnvironmentBackgroundProcessLimit|Android 已停止 Linux 环境（后台进程数量限制）。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1165|ui.details|详情|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1170|ui.environment|环境|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1191|status.serverMissing|尚未安装 {name}，这些文件的代码智能不可用。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1192|ui.install|安装|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1207|status.updateAvailable|有可用更新：v{version}|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1208|ui.update|更新|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1216|option.About|关于|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1229|ui.allowJCodeToInstallAppsThenTapUpdateAgain|请允许安装应用，然后再次点击更新。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1233|status.updateFailed|更新失败：{reason}|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1237|status.updateInstalled|已安装更新 — v{version}。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1312|ui.preview|预览|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1458|ui.settingsExported|设置已导出|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1458|status.exportFailed|导出失败：{reason}|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1469|ui.couldNotReadTheFile|无法读取文件|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1474|status.importedSettings|已导入 {count} 项设置|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1474|status.importFailed|导入失败：{reason}|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1503|ui.diagnosticLogExported|诊断日志已导出|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1503|status.exportFailed|导出失败：{reason}|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1611|ui.environment|环境|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1934|ui.closeSaved|关闭已保存标签页|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1948|ui.switchEnvironment|切换环境？|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1962|ui.restart|重新启动|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1965|ui.cancel|取消|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:2086|ui.noVirtualDevice|暂无虚拟设备|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:2604|ui.finishEnvironmentSetupBeforeOpeningTheTerminal|请先完成环境配置，再打开终端。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:2834|ui.finishEnvironmentSetupBeforeRunning|请先完成环境配置，再运行程序。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:2919|ui.devServerDidnTStartInTimeCheckThe|开发服务器未及时启动，请检查运行终端。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:2945|ui.finishEnvironmentSetupBeforeBuilding|请先完成环境配置，再进行构建。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:2992|ui.kitCancellationRequestedSetupStaysOpenUntilTheCurrent|已请求取消 IDE Kit 安装；当前包操作安全结束后停止，安装终端会保留。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:2997|ui.packageCancellationRequestedSetupStaysOpenUntilAptDpkg|已请求取消软件包安装；apt/dpkg 到达安全停止点后结束，安装终端会保留。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3152|ui.closeWorkspace|关闭项目组？|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3152|ui.closeProjectText2c6af4|关闭项目？|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3161|ui.closeAnyway|仍然关闭|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3166|ui.cancel|取消|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3178|ui.cancel|取消|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3287|ui.unlockScreenOrientation|解锁屏幕方向|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3287|ui.lockScreenOrientation|锁定屏幕方向|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3293|ui.exitFullscreen|退出全屏|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3293|ui.enterFullscreen|进入全屏|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3299|ui.keepAwakeTurnOff|关闭保持唤醒|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3299|ui.keepAwakeTurnOn|开启保持唤醒|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3305|ui.showHeaderAndTabs|显示顶部栏和标签页|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3305|ui.hideHeaderAndTabs|隐藏顶部栏和标签页|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3312|ui.goToLine|跳转到行…|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3319|ui.formatDocument|格式化文档|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3326|ui.increaseEditorFontSize|增大编辑器字号|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3333|ui.decreaseEditorFontSize|减小编辑器字号|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3340|ui.colorSearchPickFromScreen|颜色查找（从屏幕取色）|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3349|ui.openVirtualDevice|打开虚拟设备|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3359|ui.openBrowser|打开浏览器|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3365|ui.browserBack|浏览器：后退|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3374|ui.browserForward|浏览器：前进|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3384|ui.browserStopLoading|浏览器：停止加载|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3384|ui.browserReloadPage|浏览器：重新加载|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3394|ui.newProject|新建项目|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3401|ui.openFolder|打开文件夹|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3408|ui.autoSetupEnvironment|自动配置环境|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3415|ui.focusExplorer|聚焦文件管理器|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3422|ui.showSearchPlaceholder|显示搜索提示|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3429|ui.reconnectTheVirtualDeviceToAdb|重新连接虚拟设备与 adb|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3439|ui.openSettings|打开设置|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3446|ui.openWorkspaceYAML|打开共享目录 YAML|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3453|ui.openProjectYAML|打开项目 YAML|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4419|nav.files|文件|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4419|nav.find|查找|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4419|nav.languages|语言|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4419|nav.packages|扩展包|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4419|nav.settings|设置|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4463|ui.hideRecentProjects|隐藏最近项目|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4463|ui.projectsOptionalContext|项目 · 可选上下文|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4468|ui.remove|移除|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4471|ui.removeOnlyClearsTheRecentListFilesStayIntact|移除仅清除最近记录，文件将完整保留。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4487|files.internal|内部文件|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4488|ui.projectFiles|项目文件|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4783|ui.showHeaderAndTabsText2f5a6b|显示顶部栏和标签页|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4858|ui.noFileOpen|未打开文件|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4860|ui.openExplorer|打开文件管理器|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4925|files.newFolder|新建文件夹|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4926|ui.openFolder|打开文件夹|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4935|ui.noRecentProjectsYet|暂无最近项目。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5005|common.version|版本 {version}|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5078|ui.recentProjectActions|最近项目操作|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5087|ui.exportToStorage|导出到存储空间|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5189|ui.terminal|终端|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5189|option.Output|输出|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5189|option.Issues|问题|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5189|run.debug|调试|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5189|option.Tasks|任务|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5189|ui.view|查看|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5215|ui.close|关闭|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5291|ui.resizeEditorAndPanel|调整编辑器和面板大小|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5390|ui.pasteImage|粘贴图片|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5504|ui.unsavedChanges|未保存的更改|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5507|files.unsavedNamed|“{name}”存在未保存的更改。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5516|ui.discard|放弃更改|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5520|ui.save|保存|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5524|ui.cancel|取消|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5547|ui.runningProcess|运行中的进程|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5547|ui.runningProcesses|运行中的进程|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5558|ui.kill|终止|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5560|ui.closeUnbusy|关闭空闲标签页|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5564|ui.cancel|取消|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5656|ui.pinned|已固定|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5684|ui.closeTerminal|关闭终端|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5705|ui.close|关闭|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5709|ui.unpin|取消固定|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5709|ui.pin|固定|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5713|ui.clear|清空|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5716|ui.closeOthers|关闭其他标签页|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5719|ui.closeAll|全部关闭|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5726|ui.newTerminal|新建终端|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5759|ui.terminalUnavailable|终端不可用|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5765|ui.finishDistroSetupFirstThenOpenTheTerminalAgain|请先完成发行版配置，然后重新打开终端。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5772|ui.openSetup|打开环境配置|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5856|ui.copy|复制|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5858|ui.paste|粘贴|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5861|ui.selectText|选择文本|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5862|ui.selectAll|全选|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5863|ui.clear|清空|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5885|ui.noTerminalSessions|暂无终端会话|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5890|ui.tapToStartANewTerminal|点击 + 新建终端|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5917|ui.buildLogsAndToolOutputWillAppearHere|构建日志和工具输出将显示在这里。|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5955|ui.clearOutput|清空输出|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:6026|ui.pressBackAgainToExit|再按一次返回退出|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:6034|ui.exitJCode|退出应用？|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:6042|ui.terminateExit|终止并退出|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:6050|ui.runInBackground|在后台运行|已绑定|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:6055|ui.cancel|取消|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2530|message.Debugcancelledfilescouldnotbesaved|调试已取消：无法保存文件。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2704|message.noDefinition|未找到“{symbol}”的定义|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2715|message.noReferences|未找到“{symbol}”的引用|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2724|message.Putthecursoronasymboltorename|请将光标放在要重命名的符号上。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2739|message.cannotRename|无法在此处重命名“{symbol}”|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2748|message.Renamefailed|重命名失败|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2764|message.sourceLoadFailed|语言服务无法加载源文件：{path}|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2775|message.fileMissing|无法打开 {name}：文件不存在|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2867|message.applyEditsFailed|应用编辑失败：{reason}|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2969|message.formatted|已格式化 {name}|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2970|message.Languageserverreturnednoformattingedits|语言服务未返回格式化更改。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3515|project.createFolderHint|请在“内部文件”中新建文件夹，或创建可选项目。已有目录元数据将保留。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3516|project.saveFailed|创建已取消：无法保存已有文件。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3520|project.templateUnavailable|请启用原生 IDE Kit，并授予 project.read／file.write 权限以使用此模板。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3534|project.created|已创建项目“{name}”{template}|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3535|message.templateFailed|项目已保留；模板应用失败：{reason}|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3574|message.opened|已打开“{name}”。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3586|message.readFailed|无法读取“{name}”：{reason}。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3590|message.emptyImport|“{name}”为空，没有可导入的内容。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3602|message.Saveorcloseopenfilesbeforeimportinga|导入文件夹前，请先保存或关闭已打开的文件。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3611|message.importFailed|导入失败：{reason}。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3626|message.importedFolder|已导入“{name}”。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3635|message.Saveorcloseopenfilesbeforeimportinga|导入文件夹前，请先保存或关闭已打开的文件。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3709|message.Theimportedfolderisnolongeravailable|导入的文件夹已不可用。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3744|message.RemovedfromtheprojectlistFilesremainin|已从项目列表移除，文件仍保留在“内部文件”中。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3757|message.ProjectfilesmovedtoTrashRestorefromFiles|项目文件已移到回收站，可从“文件 → 回收站”恢复。Ubuntu 和已安装工具均已保留。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3758|message.projectDeleteStopped|项目删除已停止：{reason}。重试前请检查回收站和内部文件。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3801|message.Projectclosed|项目已关闭。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3836|message.renamed|已重命名为“{name}”。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3836|message.Renamefaileddestinationexistsortheprojectis|重命名失败：目标已存在或项目不可用。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3837|message.renameRejected|重命名被拒绝：{reason}。项目文件未被覆盖。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3848|message.exportNotLocal|导出失败：“{name}”不是本地文件夹。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3852|message.exporting|正在导出“{name}”…|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3855|message.exported|已从“{name}”导出 {count} 个文件。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3856|message.exportFailed|导出失败：{reason}|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:4000|message.Cantcolorafileoutsidetheproject|无法为项目外的文件设置标签颜色。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:4104|message.Workspaceconfigisunavailable|共享目录配置不可用。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:4115|message.Projectoverridesrequirealocalproject|项目覆盖设置需要本地项目。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:5322|message.Finishorsafelycancelpackageinstallationbeforerestarting|请完成或安全取消软件包安装后再重启。已有 Ubuntu 和数据会保留。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:5327|message.Restartpostponedwaitforaptdpkgtoreach|已推迟重启，请等待 apt/dpkg 到达安全停止点。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:5841|message.writeFailed|无法写入 {name}：{reason}|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:5994|message.bufferRetained|{name} 已移到回收站或删除。打开的缓冲区仍以只读方式保留；保存前请恢复文件。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6087|message.reloaded|已从磁盘重新加载 {name}|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6088|message.reloadedMany|已从磁盘重新加载 {count} 个文件|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6106|message.Nounsavedchanges|没有未保存的更改|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6130|message.cannotDiscard|无法放弃“{name}”的更改：不支持此文件来源|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6137|message.discardFailed|放弃 {name} 的更改失败：{reason}|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6142|message.discarded|已放弃 {name} 的更改|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6153|message.Nothingtoformat|没有可格式化的内容|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6160|message.formatted|已格式化 {name}|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6167|message.Alreadyformatted|格式已规范，无需更改。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6174|message.formatted|已格式化 {name}|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6187|message.Formattingreplydiscardedbecausethefilechanged|文件已变化，已忽略过期的格式化结果。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6219|message.saved|已保存 {name}|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6228|message.readOnly|{name} 为只读。保存前请恢复已移除的文件，或导出缓冲区内容。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6231|message.Restoretheremovedfilebeforesavingopenedits|保存前请恢复已移除的文件；打开的编辑内容仍已保留。|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6233|message.cannotSave|无法保存“{name}”：不支持此文件来源|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6268|message.saveFailed|保存 {name} 失败：{reason}|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6284|message.Saved1file|已保存 1 个文件|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6284|message.savedMany|已保存 {count} 个文件|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6313|message.fileGone|文件已不存在：{name}|已绑定|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6529|message.Projectoverridesrequirealocalprojectroot|项目覆盖设置需要本地项目根目录。|已绑定|
|app/src/main/java/dev/blamspot/jcode/PaletteCommandsUi.kt:155|ui.cancel|取消|已绑定|
|app/src/main/java/dev/blamspot/jcode/PaletteCommandsUi.kt:221|ui.cancel|取消|已绑定|
|app/src/main/java/dev/blamspot/jcode/PaletteCommandsUi.kt:308|ui.Done|完成|已绑定|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:139|ui.nothingRunningTerminalsBuildRunAndDebugSessionsAppear|暂无运行任务。终端、构建运行和调试会话会显示在这里。|已绑定|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:336|ui.startExtension|启动扩展|已绑定|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:340|ui.startExtension|启动扩展|已绑定|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:347|ui.stopExtension|停止扩展|已绑定|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:351|ui.stopExtension|停止扩展|已绑定|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:387|ui.deviceMemory|设备内存|已绑定|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:449|ui.killProcess|终止进程|已绑定|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:453|ui.killProcess|终止进程|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/NativeCommandStrip.kt:53|run.kitMissing|请在“扩展包”中安装并启用对应语言的 IDE Kit。|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/NativeCommandStrip.kt:61|run.stop|停止文件运行|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/NativeCommandStrip.kt:61|run.file|运行文件|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/NativeCommandStrip.kt:68|run.continue|继续|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/NativeCommandStrip.kt:68|run.stopDebug|停止调试|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/NativeCommandStrip.kt:68|run.debug|调试|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/NativeCommandStrip.kt:69|run.overview|概览|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/NativeCommandStrip.kt:71|project.build|构建|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/NativeCommandStrip.kt:72|project.test|测试|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/NativeCommandStrip.kt:73|project.run|运行项目|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/NativeCommandStrip.kt:74|project.tasks|高级任务|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/NativeCommandStrip.kt:76|ui.dismiss|忽略|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:93|find.content|文件内容|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:93|find.names|文件和文件夹|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:93|find.current|当前文档|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:97|find.content|文件内容|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:97|find.names|文件和文件夹|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:97|find.current|当前文档|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:199|find.failed|搜索失败：{reason}。请检查表达式或选择其他范围。|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:220|find.hint|在所选目录中搜索名称或文件内容。|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:222|files.internal|内部文件|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:223|ui.project|项目|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:230|find.placeholderContent|搜索文件内容|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:230|find.placeholderNames|搜索文件和文件夹名称|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:230|find.placeholderCurrent|搜索当前文档|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:257|find.openCurrent|打开文件以搜索当前文档|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:258|find.minimum|至少输入 {count} 个字符|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:259|find.searching|正在搜索…|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:260|find.none|无结果|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:262|find.namesSummary|{count} 个文件／文件夹|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:263|find.contentSummary|{files} 个文件中有 {count} 个结果|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:213|ui.emptyTheTrash|清空回收站？|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:222|ui.emptyTextc6c094|空|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:235|ui.cancel|取消|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:258|ui.trash|回收站|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:261|ui.readingTheTrash|正在读取回收站…|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:262|ui.emptyTextc6c094|空|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:271|ui.emptyTrash|清空回收站|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:283|ui.readingTheTrash|正在读取回收站…|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:285|trash.emptyDescription|回收站为空。删除的文件和文件夹将保留{retention}。|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:285|trash.untilEmpty|直到手动清空|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:285|trash.days|{count} 天|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:387|ui.back|返回|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:391|ui.back|返回|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:419|ui.restore|恢复|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:420|ui.deleteForever|永久删除|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:466|ui.previewTruncatedTheWholeFileIsRestoredIntact|…预览已截断，恢复时仍会保留完整文件。|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:96|ui.hideLeftSidebar|隐藏左侧栏|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:139|files.internal|内部文件|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:152|ui.closeSavedProjectGroup|关闭已保存的项目组|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:152|ui.closeProject|关闭项目|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:160|ui.addProject|添加项目|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:167|ui.openFolderText6a9084|打开文件夹|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:175|ui.openFolder|打开文件夹|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:190|ui.advancedTasksJcodeRunYaml|高级任务（.jcode/run.yaml）|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:191|ui.sourceControl|版本控制|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:192|ui.databases|数据库|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:193|ui.virtualMachines|虚拟机|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:241|ui.hideLeftSidebar|隐藏左侧栏|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:241|ui.showLeftSidebar|显示左侧栏|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:271|files.internal|内部文件|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:285|ui.saveUnsavedChanges|保存（存在未保存更改）|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:285|ui.save|保存|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:294|ui.undo|撤销|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:295|ui.redo|重做|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:296|ui.discard|放弃更改|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:297|ui.saveAll|全部保存|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:313|ui.terminal|终端|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:333|ui.commandPalette|命令面板|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:346|ui.toggleRightSidebar|切换底部面板|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:94|files.internal|内部文件|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:98|ui.noProjectYetAddOneWithAboveOrOpen|尚无项目。点击上方 + 添加项目，或打开已有文件夹。|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:131|ui.pROJECTS|项目|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:188|ui.projectActions|项目操作|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:192|ui.projectActions|项目操作|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:202|ui.rename|重命名|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:203|ui.removeFromListKeepFiles|从列表移除（保留文件）|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:204|ui.deleteProjectFilesToTrash|将项目文件移到回收站|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:207|ui.openSavedProjectGroup|打开已保存的项目组|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:207|ui.open|打开|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:208|ui.projectSettings|项目设置|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:209|ui.exportToStorage|导出到存储空间|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:220|ui.deleteProjectFiles|删除项目文件？|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:220|project.deleteDescription|{name}<br>{path}<br>整个目录将移到回收站以便恢复。“从列表移除”是另一项独立操作。|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:220|ui.moveProjectToTrash|将项目移到回收站|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:220|ui.cancel|取消|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:226|ui.rename|重命名|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:232|ui.name|名称|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:239|ui.rename|重命名|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:245|ui.cancel|取消|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:61|ui.openFolderAsProject|作为项目打开文件夹？|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:62|project.openDescription|{name}<br>无需项目也可通过“内部文件”访问文件。作为项目打开会添加可选工具和上下文。|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:63|ui.openProject|打开项目|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:64|ui.cancel|取消|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:100|ui.scanningFolder|正在扫描文件夹|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:100|ui.importingFolder|正在导入文件夹|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:115|project.importProgress|{done} / {total} 个文件|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:122|ui.readingContents|正在读取内容…|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:146|project.cloned|已克隆“{name}”|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:149|ui.itWasAddedToThisWorkspaceOpenItNow|已添加到项目列表。现在打开，或保留在列表中稍后打开？|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:154|ui.openFolderText6a9084|打开文件夹|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:155|ui.addToWorkspace|添加到项目列表|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:213|ui.name|名称|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:216|ui.projectName|项目名称|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:222|ui.template|模板|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:240|ui.newProject|新建项目|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:251|ui.configureThisProjectThenCreateIt|设置项目选项，然后创建项目。|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:298|ui.next|下一步|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:298|ui.create|创建|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:324|ui.back|返回|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:324|ui.cancel|取消|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:392|ui.empty|（空）|已绑定|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:395|project.blankDescription|空白项目文件夹。|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeKitPanel.kt:46|find.none|无结果|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeKitPanel.kt:49|ui.close|关闭|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:51|ui.languageReadiness|← 语言就绪状态|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:52|ui.checkTrustedSourceUpdates|检查可信源更新|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:58|languages.title|语言|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:59|languages.context|Ubuntu {distro} · {architecture}<br>复用已安装的运行环境。确认一次安装计划后，IDE Kit 会部署必需的开发工具。|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:63|packages.checking|正在检查依赖…|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:63|packages.installingResources|正在安装已审阅的扩展包资源|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:63|packages.resourcesReady|就绪 · 资源已启用，无需语言开发环境|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:64|ui.cancelSafely|安全取消|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:66|ui.hideInstallationLog|隐藏安装日志|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:66|ui.installationLog|安装日志|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:77|languages.basicStatus|基础插件：{status}|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:77|status.enabled|已启用|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:77|status.disabled|已禁用|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:77|status.notInstalled|未安装|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:78|languages.kitStatus|IDE Kit：{status}|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:78|status.enabled|已启用|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:78|status.disabled|已禁用|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:78|status.optionalNotInstalled|可选 · 未安装|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:79|ui.updatedKitAvailableReviewItsSignedDependencyPlanTo|有新版 IDE Kit 可用。请审阅其已签名的依赖计划以使用新版工作流。|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:80|ui.developmentToolsCheckingWhenAKitIsInstalled|开发工具：安装 IDE Kit 后检查|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:80|ui.legacyKitReviewUpdatedPrerequisites|旧版 IDE Kit：请审阅更新后的依赖|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:80|ui.readyRuntimeAndAllRequiredToolsDetected|就绪 · 已检测到运行环境和全部必需工具|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:80|languages.partial|部分就绪 · 缺少：{tools}|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:83|ui.basicRunSetup|配置基础运行|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:84|ui.setUpCompleteIDEKit|配置完整 IDE Kit|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:84|ui.reviewRepairDependencies|审阅／修复依赖|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:89|ui.refreshReadiness|刷新就绪状态|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:90|ui.advancedEnvironmentAndServerControls|高级环境与服务设置|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLocalization.kt:37|ui.language|界面语言|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLocalization.kt:39|ui.english|English（默认）|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLocalization.kt:41|ui.english|English（默认）|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativeLocalization.kt:45|ui.fallback|缺失的翻译使用英文。语言资源不会安装任何开发环境。|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:50|packages.title|扩展包|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:51|packages.sourceContext|源文件上下文：{path}|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:51|ui.openASourceFileForLanguageActionsProjectWorkflows|打开源文件可使用语言操作；项目工作流使用当前选中的项目|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:52|ui.language|界面语言|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:55|option.PluginLibrary|基础插件目录|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:55|option.IDEKitLibrary|IDE Kit 目录|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:55|option.Installed|已安装|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:58|files.refresh|刷新|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:59|ui.importSuicSuik|导入 .suic / .suik|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:62|packages.checking|正在检查依赖…|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:62|packages.installingResources|正在安装已审阅的扩展包资源|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:62|packages.resourcesReady|就绪 · 资源已启用，无需语言开发环境|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:65|ui.cancelInstallationSafely|安全取消安装|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:68|ui.installationLog|安装日志|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:69|ui.setupLog|配置日志|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:69|ui.close|关闭|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:81|packages.metadata|{version} · Core API {api} · {bytes} 字节|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:82|packages.dependencies|{author} · {description}<br>依赖：{dependencies}|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:86|packages.installedStatus|已安装 {version} · {status}|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:86|status.enabled|已启用|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:86|status.disabled|已禁用|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:87|ui.downloadAndReview|下载并审阅|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:87|ui.reviewUpdateReinstall|审阅更新／重新安装|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:92|ui.noNativePackagesInstalledChooseALibraryOrImport|尚未安装原生扩展包。请选择扩展目录或导入扩展包。|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:97|packages.record|{id}<br>{version} · .{kind} · {status}|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:97|status.enabled|已启用|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:97|status.disabled|已禁用|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:101|ui.disable|禁用|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:101|ui.enable|启用|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:102|ui.rollback|回滚|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:104|ui.uILanguageResourcesNoLanguageEnvironmentRequired|界面语言资源 · 无需语言开发环境|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:106|ui.prerequisitesNotCheckedThisSession|本次会话尚未检查依赖|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:107|ui.reviewDependenciesRepair|审阅依赖／修复|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:118|ui.uninstall|卸载|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:119|ui.uninstallPackage|卸载扩展包？|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:119|packages.removeDescription|{id}<br>将保留 Ubuntu、语言环境、项目和源文件。|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:119|ui.uninstall|卸载|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:119|ui.cancel|取消|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:126|ui.language|界面语言|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:126|ui.close|关闭|已绑定|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:127|ui.close|关闭|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerDetailScreen.kt:148|ui.Use|使用|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerDetailScreen.kt:175|ui.remove|移除|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerDetailScreen.kt:183|ui.install|安装|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerDetailScreen.kt:190|ui.update|更新|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerDetailScreen.kt:190|ui.Reinstall|重新安装|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerDetailScreen.kt:201|ui.uninstall|卸载|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerDetailScreen.kt:233|ui.Working|正在处理…|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerPanelHeader.kt:110|ui.Search|查找|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerPanelHeader.kt:127|files.refresh|刷新|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerPanelHeader.kt:133|manager.installed|已安装 {count} 项|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerPanelHeader.kt:178|manager.oneProblem|1 个问题 — 在问题面板中查看|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerPanelHeader.kt:178|manager.problems|{count} 个问题 — 在问题面板中查看|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/SettingsResettableRow.kt:88|ui.Resetthissetting|重置此设置|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/TabColorDialog.kt:43|ui.Tabcolor|标签页颜色|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/TabColorDialog.kt:78|option.Random|随机|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/TabColorDialog.kt:83|ui.clear|清空|已绑定|
|core/design/src/main/java/dev/blamspot/jcode/design/TabColorDialog.kt:85|ui.cancel|取消|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:163|ui.noFileOpen|未打开文件|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:168|ui.openAFileToStartEditing|打开文件以开始编辑|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:212|ui.openFile|打开文件|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:266|ui.pinned|已固定|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:298|ui.unsavedChanges|未保存的更改|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:305|ui.closeTab|关闭标签页|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:331|ui.unpin|取消固定|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:331|ui.pin|固定|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:334|ui.changeTabColor|更改标签页颜色|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:336|ui.close|关闭|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:337|ui.closeOthers|关闭其他标签页|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:338|ui.closeToTheRight|关闭右侧标签页|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:614|ui.copy|复制|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:615|ui.cut|剪切|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:616|ui.paste|粘贴|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:619|ui.selectText|选择文本|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:620|ui.selectAll|全选|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:622|ui.goToLineText77922d|跳转到行|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:625|ui.findText|查找文本|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:632|ui.GotoDefinition|跳转到定义|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:632|ui.FindReferences|查找引用|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:632|ui.RenameSymbol|重命名符号|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/EditorPane.kt:632|ui.formatDocument|格式化文档|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/LocalizedPageTitle.kt:9|nav.settings|设置|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/LocalizedPageTitle.kt:10|ui.environment|环境|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/LocalizedPageTitle.kt:11|ui.trash|回收站|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/LocalizedPageTitle.kt:12|project.tasks|高级任务|已绑定|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/LocalizedPageTitle.kt:13|project.build|构建|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:169|files.matches|{count} 个可见匹配项|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:294|status.imported|已导入 {count} 个文件|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:295|status.importFailed|导入失败：{reason}|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:306|ui.exportedFile|文件已导出|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:307|status.exportFailed|导出失败：{reason}|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:318|status.exportedFrom|已从“{name}”导出 {count} 个文件|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:319|status.exportFailed|导出失败：{reason}|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:340|ui.theRootFolderIsPermanentManageProjectNamesThrough|根目录始终保留。请通过项目管理修改项目名称。|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:351|status.copied|已复制“{name}”|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:355|status.cut|已剪切“{name}”|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:373|ui.folderExportSupportsLocalFoldersOnly|仅支持导出本地文件夹|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:383|files.filter|文件名筛选|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:384|files.filterHint|仅筛选已加载的条目。查找子目录名称或文件内容请使用“查找”。|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:387|files.doneSelection|完成（{count}）|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:387|ui.select|选择|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:388|files.sort|排序：{sort}|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:388|ui.name|名称|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:388|option.Modified|修改时间|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:388|option.Size|大小|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:391|ui.selectionActions|选中项操作 ▾|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:394|ui.selectAllVisible|选择全部可见项|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:395|ui.copy|复制|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:396|ui.cutMove|剪切／移动|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:396|ui.projectRootFoldersMustBeManagedThroughProjects|请通过项目管理操作项目目录／根目录|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:397|ui.moveToTrash|移到回收站|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:399|ui.propertiesFullNames|属性／完整名称|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:399|files.properties|{name}<br>{path}<br>{kind} · {bytes} 字节<br>修改时间：{modified}|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:399|files.kindDirectory|文件夹|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:399|files.kindFile|文件|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:427|files.pasteStopped|粘贴在“{name}”处停止：{reason}。未覆盖冲突文件，剩余剪贴板条目已保留。|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:432|files.pasted|已粘贴 {count} 项|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:493|ui.fileProperties|文件属性|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:493|ui.close|关闭|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:494|files.trashSelected|将 {count} 个选中项移到回收站？|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:494|ui.filesRemainRecoverableFromTrashRegisteredProjectRootsCannot|文件可从回收站恢复。请在项目管理中删除已登记的项目根目录。|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:501|files.trashPaused|回收站操作已暂停：{reason}|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:503|ui.moveToTrash|移到回收站|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:503|ui.cancel|取消|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:507|files.newFolder|新建文件夹|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:507|files.newFile|新建文件|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:520|status.created|已创建“{name}”|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:522|status.createFailed|创建失败：{reason}|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:533|ui.rename|重命名|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:543|status.renamed|已重命名为“{name}”|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:545|status.renameFailed|重命名失败：{reason}|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:562|files.deleteNamed|删除“{name}”？|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:565|files.trashDescription|所选条目及其内容将移到回收站，保留{retention}，期间可以恢复。|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:565|trash.untilEmpty|直到手动清空|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:565|trash.days|{count} 天|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:565|files.deleteDescription|将永久删除所选条目及其内容。如果项目使用 Git，可从版本库中恢复。|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:592|status.movedToTrash|已将“{name}”移到回收站|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:592|status.deleted|已删除“{name}”|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:594|status.deleteFailed|删除失败：{reason}|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:598|ui.moveToTrash|移到回收站|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:600|ui.delete|删除|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:604|ui.cancel|取消|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:658|ui.moreActions|更多操作|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:665|ui.moreActions|更多操作|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:675|ui.copy|复制|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:676|ui.cut|剪切|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:677|ui.rename|重命名|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:679|ui.delete|删除|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:683|ui.open|打开|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:685|ui.newFile|新建文件|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:686|ui.newFolder|新建文件夹|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:687|ui.importFilesTextc98b4f|导入文件…|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:689|ui.propertiesFullName|属性／完整名称|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:690|ui.export|导出…|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:724|ui.upOneLevel|上一级|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:728|ui.upOneLevel|上一级|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:808|ui.noFilesYetUseTheNewFileNewFolder|暂无文件。<br>请使用上方“新建文件”或“新建文件夹”按钮。|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:885|ui.collapse|折叠|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:885|ui.expand|展开|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:1056|files.newFile|新建文件|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:1057|files.newFolder|新建文件夹|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:1058|files.refresh|刷新|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:1062|files.more|更多文件操作|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:1064|ui.importFiles|导入文件|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:1065|ui.paste|粘贴|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:1066|ui.collapseAll|全部折叠|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:1067|ui.trashRestore|回收站／恢复|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:1124|ui.oK|确定|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:1134|ui.cancel|取消|已绑定|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:1147|ui.gitSubmodule|Git 子模块|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:459|ui.Import|导入|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:462|ui.Setupfreshinstead|改为全新配置|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:542|files.refresh|刷新|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:645|ui.Done|完成|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:645|ui.Continuewithoutenvironment|暂不配置环境，继续使用|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:679|ui.Browserengineisoutdated|浏览器引擎版本过旧|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:705|ui.getLatestWebView|获取最新 WebView|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:726|ui.Addanenvironment|添加环境|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:733|ui.Installanotherdistro|安装其他发行版|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:760|ui.Installedenvironments|已安装环境|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:787|ui.Removeenvironment|移除环境|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:797|ui.remove|移除|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:803|ui.cancel|取消|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:866|ui.remove|移除|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:887|ui.Environmentsetup|环境配置|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:889|ui.PickaLinuxdistroandJCodeconfigurestherestautomatically|选择 Linux 发行版，应用会自动完成后续配置。|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:1044|ui.setupLog|配置日志|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:1046|ui.Waitingforprogress|等待进度|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:1052|option.Hide|隐藏|已绑定|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:1052|ui.show|显示|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:48|ui.javaLatestGA|Java · 最新正式版|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:49|ui.nativeLanguageEnvironmentEclipseTemurinOpenJDK|原生语言环境 · Eclipse Temurin OpenJDK|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:51|ui.unavailableThisMilestoneRequiresALinuxARM64Environment|不可用：此版本需要 Linux ARM64 环境。|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:51|ui.completeUbuntuSetupBeforeInstallingJava|请先完成 Ubuntu 配置，再安装 Java。|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:54|ui.queryLatestGARetryQuery|查询最新正式版／重试|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:68|ui.reviewAndInstallRetry|审阅并安装／重试|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:71|ui.cancel|取消|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:72|ui.installedJDKs|已安装的 JDK|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:73|ui.refreshInstalledJDKs|刷新已安装的 JDK|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:77|ui.verifyAndSelect|验证并选择|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:78|ui.removeInactiveJDK|移除未使用的 JDK|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:81|ui.quickRunOpenAJavaFileInTheEditorAnd|QuickRun：打开 .java 文件并按 Ctrl+F5。运行前将保存当前文件，然后在 Ubuntu 中执行。|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:82|ui.installJavaBasicPluginFromTheSuiCodePlugin|从 sui-code 扩展目录安装 Java 基础插件以运行单个文件。Java IDE Kit 提供语言服务和调试，不使用 JCode 插件市场。|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:87|ui.removeInactiveJDKText012cf8|移除未使用的 JDK？|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:87|ui.changeManagedJDK|切换托管 JDK？|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:89|ui.confirm|确认|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:89|ui.cancel|取消|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:96|ui.confirmAndOpenTerminal|确认并打开终端|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:97|ui.cancel|取消|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:27|ui.nativeUbuntuEnvironmentSignedDistributionPackages|原生 Ubuntu 环境 · 已签名的发行版软件包|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:30|ui.completeUbuntuSetupFirstExistingInstallationsAndJavaData|请先完成 Ubuntu 配置。不会重置已有安装或 Java 数据。|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:32|ui.queryUbuntuVersionsRetry|查询 Ubuntu 版本／重试|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:33|ui.verifySelectedEnvironment|验证所选环境|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:35|ui.queryLatestOfficialPythonStableVersion|查询 Python 官方最新稳定版|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:48|ui.reviewUbuntuPackageUpdates|审阅 Ubuntu 软件包更新|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:49|ui.reviewAndInstallRetry|审阅并安装／重试|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:51|ui.cancelSafely|安全取消|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:53|ui.installTheBasicLanguagePluginFromPluginCenterIDE|从扩展包页面安装基础语言插件。IDE Kit 提供代码智能、调试和项目工具。语言环境安装独立于两种扩展格式。|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:61|ui.confirmAndOpenTerminal|确认并打开终端|已绑定|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:62|ui.cancel|取消|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:298|ui.gLOBAL|全局|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:299|ui.fILES|文件|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:300|ui.pROJECT|项目|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:317|ui.eNVVAR|环境变量|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:336|ui.appearance|外观|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:338|ui.appearance|外观|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:339|ui.systemFollowsYourDeviceSLightDarkSetting|系统模式跟随设备的浅色／深色设置。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:343|ui.mode|模式|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:345|option.System|跟随系统|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:345|option.Light|浅色|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:345|option.Dark|深色|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:354|ui.themeBundle|主题方案|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:355|ui.colorPaletteAppliedAcrossTheApp|应用全局使用的配色方案。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:376|ui.uIIcons|界面图标|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:377|ui.iconSetUsedForTheAppSOwnToolbars|应用工具栏、标签页和菜单使用的图标集。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:395|ui.fileIcons|文件图标|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:396|ui.iconSetUsedForFilesAndFoldersInThe|文件管理器、标签页及搜索结果中的文件和文件夹图标。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:403|ui.jCodeSOwnFolderAndFileGlyphsFromThe|使用当前界面图标集中的文件夹和文件图标。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:421|ui.fonts|字体|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:422|ui.monospaceTypefacesForTheCodeEditorAndTheTerminal|编辑器和终端使用的等宽字体，可通过扩展增加字体。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:433|ui.editorFont|编辑器字体|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:442|ui.terminalFont|终端字体|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:453|ui.terminal|终端|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:454|ui.textSizeForTerminalSessionsAppliesToEveryOpen|终端会话的文字大小，对所有已打开的终端生效。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:459|ui.fontSize|字号|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:469|ui.extensions|扩展|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:470|ui.textSizeInsideImportedVsixExtensionsAScaleRather|导入的 .vsix 扩展页面的文字缩放比例。每个扩展有自己的页面样式，因此这里使用比例而非固定字号。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:476|ui.fontSize|字号|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:488|ui.displayCutout|屏幕开孔|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:489|ui.keepTheAppClearOfTheCameraNotchOr|避免界面被刘海或摄像头开孔遮挡。关闭后，应用会绘制到开孔区域以获得更完整的显示空间。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:493|ui.respectDeviceCutout|避让屏幕开孔|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:494|ui.layOutTheAppInsideTheCutoutSSafe|在屏幕开孔安全区域内布局，避免内容被遮挡。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:504|ui.terminalPanel|终端面板|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:505|ui.terminalOutputIssuesAndDebuggingShareTheBottomPanel|终端、输出、问题和调试共用底部面板。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:509|settings.panelHeight|面板高度：可用区域的 {percent}%|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:511|ui.dragTheDividerOrUseThisKeyboardAccessibleSlider|可拖动分隔条，或使用支持键盘操作的滑块。面板始终为编辑器留出空间。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:515|ui.header|顶部栏|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:516|ui.theBarAcrossTheTopOfTheWorkbenchWith|工作台顶部栏，显示项目名称与快捷操作。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:521|ui.actionButton|操作按钮|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:522|ui.theButtonBesideRunHidingItLeavesTheTerminal|运行旁的操作按钮。隐藏后，仍可从终端面板访问终端。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:533|ui.bottomStatusBar|底部状态栏|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:534|ui.theBarAtTheBottomOfTheWorkbenchShowing|工作台底部状态栏，显示分支、发行版及光标位置。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:538|ui.show|显示|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:549|ui.extraKeysRow|附加按键栏|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:550|ui.aTermuxStyleKeyRowEscTabCtrlArrows|在编辑器或终端输入时，于键盘上方显示 Esc、Tab、Ctrl、方向键等附加按键。可分别设置横屏和竖屏时的显示方式。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:554|ui.portrait|竖屏|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:563|ui.landscape|横屏|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:572|ui.functionKeys|功能键|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:573|ui.appendF1F12ChipsToTheRowWhileA|终端获得焦点时，在附加按键栏显示 F1–F12（适用于 htop、midnight commander 等终端界面）。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:583|option.Input|输入|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:585|ui.volumeKeys|音量键|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:586|ui.remapTheHardwareVolumeButtonsToEditorTerminalActions|将物理音量键映射为编辑器或终端操作。“系统默认”保留音量控制。方向键与滚动作用于当前焦点所在的编辑器或终端，长按可重复执行。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:591|ui.volumeUp|音量增大键|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:600|ui.volumeDown|音量减小键|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:611|ui.commandPalette|命令面板|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:612|ui.chooseWhichBuiltInCommandsThePaletteOffersContext|选择命令面板中提供的内置命令。依赖上下文的命令仅在相应视图获得焦点时显示，例如跳转到行需要打开编辑器。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:632|option.Startup|启动|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:634|ui.restoreLastSession|恢复上次会话|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:635|ui.pickUpWhereYouLeftOffAfterClosingThe|关闭应用后，下次从上次的位置继续。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:639|ui.restoreLastSessionOnLaunch|启动时恢复上次会话|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:640|ui.reopenTheLastWorkspaceProjectAndEditorTabsIncluding|启动时恢复上次的目录上下文、项目和编辑器标签页，包括未保存的更改；缺失的文件将跳过。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:653|option.Performance|性能|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:655|ui.rendering|渲染|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:656|ui.howJCodeDrawsTheUIEditorAndTerminal|界面、编辑器和终端的渲染方式。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:660|ui.hardwareAcceleration|硬件加速|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:661|ui.renderTheUIEditorAndTerminalOnTheGPU|使用 GPU 渲染界面、编辑器和终端。仅在排查设备渲染问题时关闭；软件渲染明显更慢。下次启动应用后生效。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:669|ui.resourceManagement|资源管理|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:670|ui.keepTheLinuxRuntimeLeanByStoppingWorkYou|停止不再使用的任务以节省 Linux 运行环境资源。每个终端、运行任务和调试会话都会占用 PRoot 进程与内存。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:674|ui.warnBeforeClosingRunningProcesses|关闭运行中进程前警告|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:675|ui.whenClosingAProjectOrWorkspaceWithARunning|关闭存在运行中终端命令、构建运行任务或调试会话的项目或目录上下文前，先确认是否停止它们。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:682|ui.closeAppFullyOnSwipeAway|划走后完全退出应用|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:683|ui.whenYouSwipeJCodeOffTheAndroidRecentsScreen|从 Android 最近任务中划走应用时，停止 Linux 环境、终端、运行任务和虚拟机并完全退出，不在后台继续运行。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:690|ui.autoCloseIdleTerminals|自动关闭空闲终端|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:691|ui.automaticallyCloseTerminalsLeftIdleAtThePromptNo|自动关闭停留在提示符、没有运行程序的空闲终端，以释放进程和内存。不会自动关闭正在执行命令的终端。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:699|ui.idleTimeout|空闲超时|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:708|ui.maxTerminalInstances|终端实例数量上限|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:716|ui.toolchainInstallTimeout|工具链安装超时|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:717|ui.howLongAToolchainInstallSDKLanguageServerDebugger|工具链（SDK、语言服务、调试器）安装的最长允许时间，超时后取消。在慢速网络安装大型 SDK 时可适当增大。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:725|ui.subShellsOpenInTheirOwnTab|在独立标签页打开子 Shell|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:726|ui.whenYouStartAnInteractiveShellBashZshInside|在终端内启动 bash、zsh 等交互式 Shell 时，使用独立临时标签页；退出子 Shell 后关闭并回到父终端。脚本及管道中的 Shell 仍留在当前标签页。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:745|option.Webpreview|网页预览|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:747|ui.openWebPreviewsIn|网页预览打开方式|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:749|ui.theBrowserThisProjectUsesWhenYouOpenA|项目打开开发服务器页面或终端链接时使用的浏览器。“使用全局默认”跟随应用设置。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:751|ui.theBrowserUsedWhenYouOpenARunningDev|打开开发服务器页面或终端链接时使用的浏览器。本地项目可以在项目设置中覆盖此项。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:773|ui.fallBackToTheAppWideDefault|使用应用默认设置|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:774|ui.theDeviceSDefaultBrowserApp|设备默认浏览器|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:775|ui.showTheAndroidAppChooserEachTime|每次显示 Android 应用选择器|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:776|ui.jCodeSOwnInEditorBrowserWithDevTools|编辑器内置浏览器（含开发工具）|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:793|ui.webEngine|网页引擎|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:794|ui.theChromiumEngineBehindJCodeSBuiltInBrowser|内置浏览器和网页预览使用设备的 WebView（Chromium）引擎。它属于系统组件，应用只能读取、无法选择提供程序；无法更新时继续使用系统自带引擎。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:823|ui.outdated|版本过旧|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:831|ui.current|当前版本|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:837|ui.provider|提供程序|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:840|ui.modernSitesCanRenderBlankOrBrokenOnThis|旧引擎可能导致现代网页空白或显示异常。请安装最新版 Android System WebView，再在开发者选项中选择 WebView 实现。部分设备锁定提供程序，此时仍使用系统自带引擎。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:847|ui.getLatestWebView|获取最新 WebView|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:866|ui.chooseProvider|选择提供程序…|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:883|ui.environment|环境|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:885|ui.environment|环境|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:886|ui.environmentSetupProotDistroBootstrapAndTheFinalSmoke|管理 PRoot、发行版初始化及最终冒烟测试。可在配置页安装、切换或移除环境。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:893|option.Ready|就绪|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:893|option.Notinstalled|未安装|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:896|ui.distro|发行版|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:899|option.Notinstalled|未安装|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:900|option.Unknown|未知|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:904|ui.toolchain|工具链|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:906|option.Ready|就绪|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:907|option.Notready|未就绪|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:908|option.Unknown|未知|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:912|ui.smokeTest|冒烟测试|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:914|option.Passed|通过|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:915|option.Failed|失败|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:916|option.Notrun|尚未运行|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:925|settings.running|正在执行：{step}|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:938|ui.manageEnvironments|管理环境|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:939|ui.refreshChecks|刷新检查结果|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:948|ui.importFromPreviousInstall|从旧安装导入|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:955|ui.backUpTheWholeLinuxEnvironment25GB|将完整 Linux 环境（约 2.5 GB）备份为 .tar.gz，可在本设备或其他设备上恢复。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:960|ui.backUpTarGz|备份（.tar.gz）|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:961|ui.restoreText8ff034|恢复…|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:967|ui.movingToADifferentlyNamedBuildWriteTheEnvironment|迁移到不同包名的版本时，可将环境、项目、扩展和设置写入共享 JCode 目录，再从新安装的应用中导入。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:972|ui.exportForMigration|导出以迁移|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:976|ui.refreshPackageListsAndUpgradeInstalledPackagesAptGet|刷新软件包索引并升级已安装软件包（apt-get update && upgrade）。在安装终端执行，可能耗时较长并消耗流量。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:981|ui.updatingPackages|正在更新软件包…|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:981|ui.updateSystemPackages|更新系统软件包|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:989|ui.backgroundProcessLimit|后台进程数量限制|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:990|ui.androidCapsHowManyProcessesAnAppMayFork|Android 限制应用可派生的进程数量，超出时会终止进程，可能中断整个 Linux 环境。需要通过 adb 命令提高上限，应用无法自行修改。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1005|ui.linuxProcesses|Linux 进程|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1010|ui.android12KillsAnAppSForkedProcessesOnce|Android 12 及以上版本会在应用派生进程超过上限（默认 32 个）时终止它们。PRoot、Shell 及其子进程均计入，因此长时间构建可能导致终端退出而应用仍在运行。可从已连接的电脑或设备自身的 adb 执行以下命令提高上限。设置重启后保留，恢复出厂设置后需重新配置。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1021|ui.copyCommands|复制命令|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1027|ui.androidDevice|Android 设备|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1028|ui.pairJCodeWithThisPhoneSOwnAdbSo|连接本设备的 adb，以安装并启动构建产物。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1034|ui.serial|序列号|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1036|ui.manageDevice|管理设备|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1036|ui.setUpADB|配置 ADB|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1044|option.About|关于|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1047|ui.appVersionAndUpdatesFromGitHubReleases|应用版本与 GitHub 更新。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1057|common.version|版本 {version}|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1066|settings.updateVersion|更新：v{version}|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1074|ui.upToDate|已是最新版本|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1085|ui.installUpdate|安装更新|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1086|settings.downloading|正在下载… {percent}%|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1087|ui.installing|正在安装…|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1094|ui.checking|正在检查…|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1094|ui.checkForUpdates|检查更新|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1102|ui.backupRestore|备份与恢复|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1103|ui.saveYourAppPreferencesToAFileThenRestore|将应用偏好导出到文件，以便在本设备或其他设备上恢复。主题与编辑器设置保存在目录配置中。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1108|ui.exportSettings|导出设置…|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1109|ui.importSettings|导入设置…|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1115|option.Diagnostics|诊断|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1117|ui.diagnosticLogging|诊断日志|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1118|ui.offUnlessYouTurnItOnWhenSomethingMisbehaves|默认关闭。遇到异常时，可开启日志并导出以提交问题报告，完成后再关闭。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1132|ui.recordDiagnostics|记录诊断信息|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1133|ui.writesAppEventsToALogFileOnThis|将应用事件记录到本设备文件，不自动上传；仅在用户选择时导出。文件路径以占位符替换以便分享日志。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1141|ui.detail|详细程度|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1150|ui.includeTheSystemLog|包含系统日志|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1151|ui.addsJCodeSLogcatOutputIncludingProotAndThe|包含 JCode 的 logcat 输出，以及 PRoot 和其中的 Linux 环境日志，便于诊断工具链、扩展和语言服务。只能读取本应用日志，无法读取其他应用日志。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1158|ui.recordCrashes|记录崩溃|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1159|ui.appendTheStackTraceWhenTheAppCrashesSo|应用崩溃时附加异常堆栈，以便日志同时记录故障及其发生前的过程。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1165|ui.recorded|已记录|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1166|ui.location|位置|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1170|ui.view|查看|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1175|ui.export|导出…|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1180|ui.clear|清空|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1192|option.Editor|编辑器|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1194|ui.editorDefaults|编辑器默认设置|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1195|ui.defaultFontSizeAndWordWrapForTheCode|代码编辑器的默认字号与换行方式。共享目录或项目可在自己的设置页中覆盖字号。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1201|ui.fontSize|字号|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1209|ui.wordWrap|自动换行|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1210|ui.wrapLongLinesToTheEditorWidthInsteadOf|根据编辑器宽度换行，避免横向滚动。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1218|ui.editorGestures|编辑器手势|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1219|ui.howTouchInputBehavesInTheEditorAppliesApp|编辑器触控行为，应用全局生效。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1223|ui.dragToMoveCursor|拖动移动光标|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1224|ui.dragAFingerOnTheEditorToMoveThe|在编辑器中拖动手指以移动光标，视图会跟随光标滚动。长按仍然选择文本，此设置应用全局生效。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1232|ui.cursorDragSpeedVertical|光标拖动速度 — 垂直|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1240|ui.cursorDragSpeedHorizontal|光标拖动速度 — 水平|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1251|ui.tabs|标签页|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1252|ui.howEditorAndTerminalTabsBehaveAppliesAppWide|编辑器和终端标签页的行为，应用全局生效。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1256|ui.hideTabCloseButton|隐藏标签页关闭按钮|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1257|ui.removesTheOnEditorAndTerminalTabsToAvoid|隐藏编辑器和终端标签页上的 ×，避免误触关闭。仍可通过长按菜单关闭标签页。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1264|ui.tabWidth|标签页宽度|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1265|ui.theMostAnEditorOrTerminalTabWidensBefore|编辑器和终端标签页的最大宽度。超出时从名称中间省略，例如 build.gradle.kts 显示为 build.g…kts。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1273|ui.tabColoring|标签页颜色|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1274|ui.colorCodeEditorFileTabsLongPressAFile|为编辑器文件标签页设置颜色。长按标签页可手动设色，颜色保存在项目的 .jcode 中。项目可以覆盖此默认设置。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1285|ui.formatter|格式化工具|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1286|ui.whichFormatterTheEditorUsesBuiltInIsRule|编辑器使用的格式化工具。内置工具基于规则，安装格式化扩展后也会显示在这里。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1293|ui.builtInRuleBasedFormatter|内置规则格式化工具|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1293|ui.formatterExtension|格式化扩展|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1302|ui.markdownPreview|Markdown 预览|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1303|ui.howTheRenderedMarkdownPreviewLaysOut|Markdown 预览的布局方式。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1308|ui.wordWrapInPortrait|竖屏预览自动换行|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1309|ui.offAPortraitPreviewLaysOutAtLandscapeWidth|关闭后，竖屏 Markdown 预览将采用横屏宽度（屏幕高度，并遵循开孔设置），通过横向滚动查看，避免宽表格和代码被折行。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1319|ui.explorer|文件管理器|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1321|ui.excludeFilesFolders|排除文件／文件夹|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1322|ui.excludeFilesAndFoldersAtTheProjectRootIn|在文件管理器中排除项目根目录下的文件和文件夹。自动注入的规则来自项目 .gitignore，由版本控制扩展同步。排除项默认显示为灰色，也可完全隐藏。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1326|ui.mode|模式|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1335|ui.whenExcluded|排除后的显示方式|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1347|ui.specifiedOnePatternPerLine|指定规则 — 每行一条|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1357|ui.trash|回收站|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1358|ui.whereDeletedFilesGoBeforeTheyAreGoneCovers|删除文件前的暂存位置，适用于文件管理器中的删除和版本控制中的放弃更改。可从文件管理器工具栏打开回收站。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1362|ui.moveDeletedFilesToTrash|将删除的文件移到回收站|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1363|ui.deletingAFileOrFolderOrDiscardingAChange|删除文件、文件夹或放弃版本控制中的更改时，保留可恢复的副本。关闭后将立即永久删除。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1371|ui.keepDeletedFilesFor|已删除文件保留时间|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1372|ui.olderItemsAreRemovedWhenJCodeStartsAndWhen|应用启动或打开回收站时，会清理超期项目。回收站位于应用私有存储，其内容会计入应用占用空间。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1385|option.Developer|开发者|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1387|ui.developerOptions|开发者选项|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1388|ui.toolsForBuildingAndTestingJCodeExtensions|构建和测试 JCode 扩展的工具。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1393|ui.enableDeveloperOptions|启用开发者选项|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1394|ui.addsAnExtDevTabToTheRightPanel|在工具面板中加入扩展开发页，提供检查器、Manifest 校验和实时日志，用于调试未签名的 .jext 或 .vsix。导入扩展无需启用此项，不影响已签名扩展。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1421|ui.theseSettingsPreserveTheSharedFolderJcodeConfigurationAnd|这些设置保存在共享目录的 .jcode 配置中，除非被项目配置覆盖。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1422|ui.theseSettingsSaveToTheProjectJcodeAndOnly|这些设置保存在项目的 .jcode 中，仅影响当前选中的本地项目。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1431|ui.sharedFolderYAMLWarning|共享目录 YAML 警告|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1436|ui.projectYAMLWarning|项目 YAML 警告|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1441|ui.environmentWarning|环境警告|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1444|option.Editor|编辑器|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1446|ui.editorBehavior|编辑器行为|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1447|ui.theseControlsWriteBackToYAMLAndUpdateThe|这些选项会写入 YAML 并立即更新已打开的编辑器。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1451|ui.fontSize|字号|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1459|ui.tabSize|制表符宽度|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1460|ui.goodDefaultsAre24Or8SpacesDepending|根据项目选择 2、4 或 8 个空格。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1469|ui.ligatures|字体连字|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1470|ui.keepEnabledForTheEditorSurfaceButLetUsers|默认启用字体连字，也可在长时间编写代码时关闭。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1483|ui.tabColoring|标签页颜色|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1484|ui.overridesTheAppLevelDefaultForThisScope|覆盖此范围内的应用默认设置。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1495|ui.explorer|文件管理器|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1496|ui.chooseHowTheFileExplorerIsLaidOutApplies|选择文件管理器的布局方式，应用于当前编辑范围。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1500|ui.viewMode|视图模式|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1501|ui.treeShowsTheWholeProjectHierarchyListIsA|树形视图显示完整目录层级；列表视图显示单个文件夹并提供路径导航。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1510|option.Tree|树形|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1510|option.List|列表|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1516|option.Tree|树形|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1516|option.List|列表|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1528|nav.files|文件|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1530|ui.yAMLFiles|YAML 文件|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1531|ui.openTheBackingConfigFilesDirectlyWhenYouWant|直接打开配置文件以使用完整配置能力。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1535|ui.openSharedFolderYAMLAdvanced|打开共享目录 YAML（高级）|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1537|ui.openProjectYAMLText26247e|打开项目 YAML|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1571|option.SpecifiedByInjected|指定规则＋自动注入|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1572|option.ByInjectedonly|仅自动注入|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1573|option.Off|关闭|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1578|option.Greyout|显示为灰色|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1579|option.Hide|隐藏|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1584|option.Hidden|隐藏|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1585|option.Withkeyboard|显示键盘时|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1586|option.Always|始终|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1591|option.Hidden|隐藏|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1592|option.HideonSoftKeyboard|显示软键盘时隐藏|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1593|option.AlwaysShow|始终显示|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1597|ui.terminal|终端|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1598|ui.commandPalette|命令面板|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1599|option.Hidden|隐藏|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1603|option.Randomifnotexistthenremember|随机（未设置时生成并记住）|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1604|option.Random|随机|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1605|option.Directorybasedthenremember|按目录生成并记住|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1606|option.Disabled|已禁用|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1611|settings.systemVolume|系统默认（{key}）|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1612|ui.undo|撤销|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1613|ui.redo|重做|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1614|option.KeyLeft|向左|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1615|option.KeyRight|向右|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1616|option.KeyUp|向上|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1617|option.KeyDown|向下|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1618|option.ScrollUp|向上滚动|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1619|option.ScrollDown|向下滚动|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1620|ui.commandPalette|命令面板|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1669|ui.searchSettings|搜索设置|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1686|ui.clearSearch|清空搜索|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1710|settings.noMatches|没有与“{query}”匹配的设置|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1715|ui.tryAShorterOrDifferentTermLikeFontOr|请尝试更短或不同的关键词，例如“字体”或“主题”。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1779|settings.collapse|折叠{title}|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1779|settings.expand|展开{title}|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1906|ui.selected|已选中|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1927|settings.icons|{count} 个图标|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1958|settings.icons|{count} 个图标|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2015|ui.selected|已选中|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2042|ui.recentDiagnostics|最近诊断|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2045|ui.nothingRecordedYet|尚无记录。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2067|ui.close|关闭|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2070|ui.copy|复制|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2113|ui.environmentVariables|环境变量|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2120|ui.exportedIntoEveryTerminalAndBuildRunSessionE|这些变量（如 API 密钥、GOPRIVATE、JAVA_OPTS）会导出到终端和构建运行会话，对新打开的终端生效。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2127|ui.noVariablesYetTapAddVariableToCreateOne|暂无变量。点击“添加变量”新建。|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2143|ui.empty|（空）|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2150|ui.edit|编辑|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2151|ui.delete|删除|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2156|ui.addVariable|添加变量|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2192|ui.addVariable|添加变量|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2192|ui.editVariable|编辑变量|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2198|ui.name|名称|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2203|settings.variableExists|名为“{name}”的变量已存在|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2205|ui.lettersDigitsAndUnderscoreOnlyCanTStartWith|仅允许字母、数字和下划线，且不能以数字开头|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2213|ui.value|值|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2220|ui.save|保存|已绑定|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2225|ui.cancel|取消|已绑定|

## 保留原文和未完成项

|源码位置|源码文字|分类|
|---|---|---|
|app/src/main/java/dev/blamspot/jcode/BackendService.kt:223|"$it%"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/BackendService.kt:224|"${it.size} running"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/CommandPalette.kt:82|"Search commands"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/CommandPalette.kt:91|"No matching commands"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:103|"target"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:126|"target"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/DebugSessionPanel.kt:307|"target"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/IssuesPanel.kt:96|"${messages.size}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/IssuesPanel.kt:135|"${diags.size}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/IssuesPanel.kt:165|"line"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/IssuesPanel.kt:165|"column"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:784|"Choose a JCode extension (.jext) or a VS Code extension (.vsix). Which one it ""is comes from what is inside the file, so a package saved under any name ""still lands where it belongs.\n\n""Neither is verified by the marketplace and both run code in the Linux ""runtime, so import one only from a publisher you trust.\n\n""From a .vsix, JCode runs the part of the VS Code API that extensions built ""around a webview use — a side panel, commands, settings, and the current ""file and theme. Extensions that add languages, themes, snippets, debuggers ""or tasks will not work: JCode has its own systems for those, and you will ""be told what is missing, both on import and if it is asked for while ""running.\n\n"|protected-host-English|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:815|"Finish moving from ${cleanup.sourcePackage}"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:819|"Your environment, projects, extensions and settings are now here.\n\n"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:820|"The ${cleanup.bytes / (1024 * 1024)} MB migration bundle in the shared JCode folder "|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:821|"is a copy and can go — "|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:823|"the old app still has everything until you remove it.\n\n"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:824|"Removing it frees the rest of the space it is using. Android will ask you "|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:825|"to confirm the uninstall itself."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:827|"nothing else needs it."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:855|"This update installs as a new app, so your Linux environment, projects, ""extensions and settings have to be copied across first — and "|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:857|"${blocked.reason}.\n\n"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1175|"Open terminals run ${prompt.sessionDistro}; toolchains install "|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1176|"into ${prompt.installedInto}. Open a new terminal to use it."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1191|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1207|"version"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1233|"reason"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1237|"version"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1458|"reason"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1474|"count"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1474|"reason"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1503|"reason"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1951|"JCode restarts to switch to ${pending.environmentId}. Editors and unsaved changes "|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1952|"are kept; these will be stopped:\n"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:1953|"•  $it"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:2088|"Running an APK inside JCode comes from the Android Dev Pack. ""Install it from the Marketplace and open this again."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:2839|"'${config.name}' has no terminals. Tap Configure to set it up."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:2949|"'${config.name}' has no command. Tap Configure to set it up."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3155|"Still running — closing will stop:\n"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:3156|"•  $it"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4455|"workspace-tool"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:4988|"JCode"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5005|"version"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5377|"${extension.name} actions"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5507|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5508|"These files have unsaved changes:\n"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5508|"•  $it"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5550|"Still running:\n"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5550|"•  $it"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:5896|"Workspace target: $target"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:6036|"Still running:\n"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/JCodeShell.kt:6036|"•  $it"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/JavaQuickRunController.kt:79|"$label · ${actual.name}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:377|"A blank project folder."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:470|"Import .suic or .suik through Package Manager to review the publisher, permissions and execution behavior."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:475|"Native library: ${it.message}"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:479|"Legacy JCode/VSIX packages are not supported by the native ecosystem."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:917|"Removed the migration bundle (${freed / (1024 * 1024)} MB)."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:965|"Migration bundle written to ${dir.absolutePath} (${parts.size} parts)."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:967|"Migration export failed: ${it.message ?: "unknown error"}"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:1101|"Migration import failed: ${it.message ?: "unknown error"}"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:1116|"Imported settings, projects and extensions."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:1503|"Virtual device"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:1504|"This phone"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2441|"The native official library is maintained by sui-code. Third-party packages can be imported locally after review."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2704|"symbol"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2715|"symbol"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2739|"symbol"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2745|"Renamed to \"$trimmed\" in ${edit.fileCount} "|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2746|"file"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2746|"files"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2764|"path"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2765|"SHA-256"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2767|"language-sources/$digest/"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2767|"Source.java"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2767|".class"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2767|".java"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2775|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2824|"$actionLabel needs a language server; none supports $name"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2828|"$actionLabel needs an enabled IDE Kit for $name; install one from Native Packages"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2830|"$actionLabel needs ${entry?.name ?: descriptor.id}; install it from Toolchains"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2831|"$actionLabel needs the Linux environment to be ready"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2839|"${status.name} failed to start${status.detail?.let { ": $it" } ?: ""}"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2840|"${status.name} is still starting"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2846|"${status.name} doesn't support $actionLabel"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2867|"reason"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2867|"error"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:2969|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3019|"IDE Kit edit rejected: file.write permission or language scope is missing"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3127|"sui-code builds are delivered through the Fork's verified Actions artifacts; the upstream app updater is disabled."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3178|"Native package restore failed"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3494|"Added '${project.name}' to the workspace."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3526|"Workspace '${node.name}' created"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3534|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3534|"template"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3534|" · ${it.title}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3535|"reason"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3549|"That folder is the JCode Projects root — pick a project or workspace folder inside it."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3574|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3586|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3586|"reason"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3586|"unknown error"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3590|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3596|"'$label' is too large to import — needs ${formatBytes(scan.totalBytes)}, "|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3597|"only ${formatBytes(sources.usableSpace)} free."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3611|"reason"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3611|"unknown error"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3620|"Imported Workspace '${it.name}'."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3626|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3704|"Opened ${if (isWorkspace) "Workspace" else "Project"} '${project.name}'."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3713|"Imported ${if (isWorkspace) "Workspace" else "Project"} '${it.name}'."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3758|"reason"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3836|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3837|"reason"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3848|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3852|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3855|"count"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3855|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3856|"reason"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:3856|"unknown error"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:4080|"Removed environment '$label'."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:4080|"Could not remove '$label'."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:5007|"message"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:5541|"Device sandbox"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:5586|"adb pair $target"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:5841|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:5841|"reason"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:5841|"error"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:5994|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6087|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6088|"count"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6130|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6137|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6137|"reason"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6137|"error"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6142|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6160|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6174|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6219|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6228|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6233|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6268|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6268|"reason"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6268|"error"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6284|"count"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6308|"language-sources"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6313|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6317|"Binary preview is not implemented yet for ${file.name}."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/MainViewModel.kt:6359|"Binary preview is not implemented yet for ${node.name}."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/PaletteCommandsUi.kt:138|"Go to Line"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/PaletteCommandsUi.kt:144|"line or line:column (1–$lineCount)"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/PaletteCommandsUi.kt:152|"Go"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/PaletteCommandsUi.kt:216|"Tap anywhere to sample a color"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/PaletteCommandsUi.kt:273|"Sampled Color"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/PaletteCommandsUi.kt:299|"Copy $label"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/PaletteCommandsUi.kt:311|"Pick again"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/ProviderReleaseFetcher.kt:204|"description"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:131|"Back to projects"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:138|"Run"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:292|"New advanced task"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:317|"Add build task"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:385|"Back"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:424|"Cancel"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:589|"No device"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:597|"Looking for devices…"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:608|"Set up"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:648|"Run on"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:665|"Cancel"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:691|"${target.state} · ${target.serial}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:699|"Selected"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:718|"Device sandbox"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:720|"Run a built APK in a tab — no install, no ADB"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/RunDebugPanel.kt:727|"Open"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:150|"Terminal · ${terminalTitleFor(id) ?: session.label}"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:160|"Build & Run · $runningProjectName"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:169|"Debug · ${debug.debugTargetName ?: "session"}"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:321|"Extension · ${info.name}"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:392|"%.1f GB free · %.1f GB"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:419|"${proc.name} (app)"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:427|"pid ${proc.pid}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/TaskManagerPanel.kt:433|"${proc.rssKb / 1024} MB"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/ToolchainManagerPanel.kt:114|"Manager · opens its own page"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/ToolchainManagerPanel.kt:133|"${it.category.label} · ${it.description}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/ToolchainManagerPanel.kt:151|"${it.category} · ${it.description}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/ToolchainManagerPanel.kt:169|"${it.category} · ${it.description}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/ToolchainManagerPanel.kt:209|"Toolchains"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/ToolchainManagerPanel.kt:239|"All"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/ToolchainManagerPanel.kt:257|"Environment required"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/ToolchainManagerPanel.kt:269|"Nothing available."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/ToolchainManagerPanel.kt:269|"Nothing matches “$query”."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/WorkbenchStatusBar.kt:227|"UTF-8"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/WorkbenchStatusBar.kt:232|"Other encodings aren't supported yet"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/WorkbenchStatusBar.kt:273|"CRLF"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/debug/AndroidAppProject.kt:46|"apply false"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/debug/AndroidAppProject.kt:47|"com.android.application"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/debug/AndroidDebugAttach.kt:145|" ($text)"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/ext/NativeExtensionPage.kt:94|"${extension.name} could not be loaded"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/ext/NativeExtensionPage.kt:99|"Unknown error."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/lsp/LspController.kt:383|"SuiPerformance"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/lsp/LspController.kt:383|"lsp request server=${managed.descriptor.id} elapsedMs=${android.os.SystemClock.elapsedRealtime()-started}"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/BuildConfigPage.kt:48|"Build configuration"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/BuildConfigPage.kt:50|"A build task for publish/deployment. Stored in this project's .jcode/run.yaml."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/BuildConfigPage.kt:57|"Name"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/BuildConfigPage.kt:62|"Command (bash)"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/BuildConfigPage.kt:63|"Runs in a dedicated terminal — e.g. dotnet publish -c Release -o \"\$HOME/.jcode-build/out\"."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/BuildConfigPage.kt:72|"Saved"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/BuildConfigPage.kt:72|"Save"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/ProjectRunner.kt:314|"package.json"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/run/ProjectRunner.kt:314|"${dir.name}/package.json"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/run/ProjectRunner.kt:434|"<SpaRoot>\\s*([^<]+?)\\s*</SpaRoot>"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/ProjectRunner.kt:436|"package.json"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/run/ProjectRunner.kt:562|"apply false"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/ProjectRunner.kt:563|"com.android.application"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/run/RunConfigPage.kt:54|"Run"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/RunConfigPage.kt:64|"Build & Run configuration"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/RunConfigPage.kt:66|"Stored in this project's .jcode/run.yaml."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/RunConfigPage.kt:73|"Name"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/RunConfigPage.kt:78|"Ready port"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/RunConfigPage.kt:79|"Opened in the browser when the run is ready — blank for none."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/RunConfigPage.kt:86|"Command (bash)"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/RunConfigPage.kt:87|"The one command this config runs. It executes verbosely in a terminal; the run ""tracks its process — running until it exits (done) or you stop it (killed)."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/RunConfigPage.kt:91|"e.g. dotnet run"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/RunConfigPage.kt:98|"Saved"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/run/RunConfigPage.kt:98|"Save"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:137|"Android device"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:140|"ADB bridge"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:152|"Pair JCode with this phone's own adb. Once connected, adb, ./gradlew installDebug ""and flutter run inside the runtime install and launch straight onto this device — no ""computer, no cable."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:159|"1. Install adb"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:160|"The distro's native adb client, installed from the Toolchains manager."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:164|"Open in Toolchains"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:164|"Install adb…"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:171|"2. Enable wireless debugging"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:172|"Developer options → Wireless debugging. It generally needs Wi-Fi, and Android ""switches it back off on every reboot."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:179|"Developer options are hidden. Open device info and tap Build number seven ""times, then come back."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:185|"Open device info"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:191|"Open developer options"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:198|"3. Pair"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:199|"Put JCode and Settings side by side in split-screen FIRST, then tap \"Pair ""device with pairing code\" in Wireless debugging. Android stops listening for the ""pairing the instant that dialog leaves the foreground, so simply switching back to ""JCode to type the code cancels it — the code will always be reported as wrong. The ""port fills itself in while the dialog is up; the code is used once and never stored."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:206|"Pairing code"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:209|"6 digits"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:214|"Pairing port"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:217|"Shown under the pairing code, after the colon."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:224|"Pairing…"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:224|"Pair"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:246|"Paired. Continue to step 4."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:254|"4. Connect"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:255|"Starts the relay, finds this phone's adbd over mDNS and connects the guest's ""adb to it."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:265|"Connecting…"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:265|"Connect"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:292|"No devices listed yet."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:304|"Backend override"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:305|"Dial a fixed host:port instead of the one discovered over mDNS. Leave empty ""unless discovery is failing."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:311|"adbd endpoint"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:319|"Apply"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/AndroidDevicePage.kt:330|"Clear"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BrowserPage.kt:220|"Back"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BrowserPage.kt:227|"Forward"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BrowserPage.kt:235|"Stop"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BrowserPage.kt:235|"Reload"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BrowserPage.kt:253|"Site information: ${trust.summary}"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BrowserPage.kt:284|"More browser options"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BrowserPage.kt:620|"Clear this site's data"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BrowserPage.kt:673|"Request mobile site"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BrowserPage.kt:673|"Request desktop site"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BrowserPage.kt:688|"Reload without cache"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BrowserPage.kt:698|"Clear cookies and site data"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BrowserPage.kt:710|"Copy URL"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BrowserPage.kt:711|"Open in system browser"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BrowserPage.kt:714|"DevTools"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BuiltinBrowser.kt:93|"Browser"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BuiltinBrowser.kt:217|"about:blank"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BuiltinBrowser.kt:219|"http://"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BuiltinBrowser.kt:219|"https://"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BuiltinBrowser.kt:220|"about:"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BuiltinBrowser.kt:220|"file://"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BuiltinBrowser.kt:220|"data:"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BuiltinBrowser.kt:224|"localhost"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/BuiltinBrowser.kt:225|"http://"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/BuiltinBrowser.kt:225|"https://"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:212|"${pane.label} options"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:221|"Refresh"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:226|"Clear site data"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:235|"Preserve log"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:240|"Copy all requests"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:240|"Copy all messages"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:249|"${statusLabel(it)}\t${it.method}\t${it.url}\t${it.durationMs}ms"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:252|"[${it.level}] ${it.message}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:258|"Clear network"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:258|"Clear console"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:330|"Console messages from the page appear here. Type JavaScript below to ""run it in the page."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:507|"×${row.count}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:534|"${entry.source.substringAfterLast('/').ifBlank { entry.source }}:${entry.line}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:652|"document"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:652|"html"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:653|"style"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:653|"css"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:654|"js"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:685|"‹ Back"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:861|"${formatBytes(survey.usage)} used of ${formatBytes(survey.quota)}"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:901|"Clear"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:910|"Nothing stored here."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:938|"HttpOnly"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:997|"Back to storage"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1010|"Delete"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1042|"${db.name} · v${db.version}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1049|"no object stores"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1056|"${store.name} — ${if (store.count >= 0) "${store.count} records" else "count unavailable"}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1078|"${bucket.name} · ${bucket.count} entries"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1096|"… and ${bucket.count - bucket.urls.size} more"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1128|"Unregister"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1426|"$label $n"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1475|"${e.durationMs}ms"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1483|"GET"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1483|"${e.method} · ${e.kind}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1518|"Back to requests"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1557|"Loaded by the browser, not by page script. Resource timing reports its ""URL, size, duration, and — where the origin allows it — the status; the ""response body and headers aren't exposed to the page, so there's nothing ""more to show here."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1588|"No response body was captured — it was empty, binary, or larger than the ""capture limit."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1693|"Copy"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1709|"… truncated at the 16 KB capture limit"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1808|"Refresh snapshot"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/DevToolsPanel.kt:1814|"read-only DOM at the moment you refresh"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/ExtensionDevPanel.kt:160|"contributes"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/ExtensionDevPanel.kt:162|"— none resolved —"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/ExtensionDevPanel.kt:183|"No problems found ✓"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/ExtensionDevPanel.kt:260|"$label:"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/ExtensionWebView.kt:616|"title"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/MarkdownPreviewPage.kt:143|"<pre>Preview unavailable.</pre>"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/MarkdownPreviewPage.kt:145|"document.getElementById('md').innerHTML = "|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/MarkdownPreviewPage.kt:145|";""window.jcodeRenderMermaid&&window.jcodeRenderMermaid();"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/MarkdownPreviewPage.kt:343|"Copy"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/MarkdownPreviewPage.kt:348|"Select Text"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/MarkdownPreviewPage.kt:353|"Find text"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/MarkdownPreviewPage.kt:354|"Go to line"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/MarkdownPreviewPage.kt:356|"View Source Code"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/MarkdownPreviewPage.kt:429|"Find"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/MarkdownPreviewPage.kt:435|"$active/$total"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/MarkdownPreviewPage.kt:440|"Previous match"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/MarkdownPreviewPage.kt:443|"Next match"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/MarkdownPreviewPage.kt:446|"Close find"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:237|"Aa"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/SearchToolPanel.kt:366|"${match.lineNumber + 1}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/SetupTerminalRunner.kt:224|"Setup"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:216|"This permanently destroys "|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:216|" item"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:217|"). It cannot be undone."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:263|" item"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:264|" · kept for "|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:285|"retention"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:285|"count"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:488|"… and "|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/TrashPage.kt:488|" more."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChrome.kt:72|"shimmer"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChrome.kt:76|"shimmerAlpha"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchChromeBars.kt:123|"sui-code"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPaneResize.kt:27|"Resize navigation panel. Arrow keys adjust width."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPaneResize.kt:28|"${bounds.width.toInt()} dp"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPaneResize.kt:31|"Reset panel width"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:220|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/WorkbenchPanels.kt:220|"path"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/LspDialogs.kt:49|"1 result"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/LspDialogs.kt:49|"${picker.entries.size} results"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/LspDialogs.kt:60|"Close"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/LspDialogs.kt:82|":${entry.displayLine}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/LspDialogs.kt:111|"Rename \"$symbol\""|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/LspDialogs.kt:116|"New name"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/LspDialogs.kt:123|"Rename"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/LspDialogs.kt:125|"Cancel"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:62|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:105|"\"${progress.label}\""|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:115|"done"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:115|"total"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:146|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:392|"empty"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:395|"empty"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/dialog/WorkbenchDialogs.kt:404|"$requires — ${missing.joinToString(", ")} will be installed during setup"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:102|"github.com/owner/repo"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:106|"Add"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:119|"Installs as unsigned third-party code — only add sources you trust."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:126|"Sources"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:136|"Refresh sources"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:147|"No sources yet. Add a repo above to install and update VSIX extensions from it."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:191|"Install"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:192|"Update"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:193|"Reinstall"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:246|"github.com"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:260|"Latest release"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:260|"v${release.version}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:261|"Installed"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:261|"v$it"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:262|"Asset"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:264|"Latest release"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:264|"Checking…"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:265|"Latest release"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:265|"No .vsix release found"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionSourcesPage.kt:275|"Remove"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:126|"Extensions"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:164|"Updated ${pendingReloadNames.first()} — reload to apply"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:166|"${pendingReloadNames.size} extensions updated — reload to apply"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:178|"Reload"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:191|"Loading marketplace…"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:192|"No extensions match “$query”."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:193|"Refresh to load installable extensions."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:268|"DB Managers"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:270|"Install a database-manager extension (e.g. SQL Client) from Extensions to browse databases here."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:289|"DB Managers"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:353|"Back to DB Managers"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:441|"Source Control"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:549|"VM Manager"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:551|"Install a VM Manager extension from Extensions to run virtual machines here."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:664|"Not supported on this JCode"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:665|"$reason. An installed copy keeps working; this only stops it being installed here."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:671|"Removed. Install puts it back from its source."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:673|"Removed. Close this tab to dismiss it — nothing here offers it any more."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:678|"Samples"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:678|"Example usage."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:688|"Requirements"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:689|"Toolchains and extensions this extension needs or suggests."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:692|"Required"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:696|"Suggested"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:759|"Extension settings"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:768|"No extensions installed yet."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:770|"None of your ${installed.size} extensions has anything to configure."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:845|"Collapse ${ext.name}"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:845|"Expand ${ext.name}"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:856|"Set up your Git identity (name/email) and authentication for this device."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:861|"Configure Git…"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:936|"Settings"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1057|"API capabilities"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1070|"exec"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1070|"Run commands in the Linux runtime (as root)"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1071|"fs"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1071|"Read and write project files"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1072|"workbench"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1072|"Open files/URLs, show notices, read the focused file"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1073|"Extension-defined capability"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1097|"Keep running in background"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1099|"Keep the panel alive (agent session, unsent input) when you close or switch away from it"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1118|"Activation"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1189|"Unsupported"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1206|"VSIX"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1352|"Install ${entry.name}"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1357|"Required items are installed automatically; if any of them fails, the ""extension isn't installed."|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1368|"Install ${entry.name}"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1370|"Cancel"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1381|"Required"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1384|"Installed"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1384|"Auto-install"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1407|"Suggested"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1413|"Installed"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1417|"Install"|unbound-gap|
|app/src/main/java/dev/blamspot/jcode/workbench/marketplace/ExtensionsPanel.kt:1419|"unavailable"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeKitPanel.kt:63|"text"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeKitPanel.kt:64|"text"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeKitPanel.kt:67|"label"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeKitPanel.kt:71|"label"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeKitPanel.kt:73|"label"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:59|"distro"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:59|"architecture"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:63|"Checking prerequisites…"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:63|"Installing reviewed package resources"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:63|"Ready · package resources enabled; no language environment required"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:67|"${setup.logPath.orEmpty()}\n${setup.log.takeLast(12000)}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:76|" Basic Plugin"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:77|"status"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:78|"status"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:80|"tools"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeLanguageCenter.kt:81|"${if(it.ready) "✓" else "○"} ${it.requirement.name} · ${it.installedVersion}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeLocalization.kt:39|"${it.displayName} (${it.locale})"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativeLocalization.kt:42|"${bundle.displayName} (${bundle.locale})\n${bundle.packageId}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:51|"path"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:62|"Checking prerequisites…"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:62|"Installing reviewed package resources"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:62|"Ready · package resources enabled; no language environment required"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:81|"version"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:81|"api"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:81|"bytes"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:82|"author"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:82|"description"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:82|"dependencies"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:83|"SHA-256: ${item.sha256}"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:86|"version"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:86|"status"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:96|"name"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:97|"id"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:97|"version"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:97|"kind"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:97|"status"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:98|"Verified sui-code signature"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:98|"Unverified publisher"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:99|"Permissions: "|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageCenter.kt:119|"id"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:37|"Review ${if(kit) "IDE Kit installation plan" else "native plugin"}"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:39|"${preview.pack.id} · ${preview.pack.version}\n${preview.source}\n${preview.bytes.size} bytes · SHA-256 ${preview.pack.digest}"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:40|"Official signature verified"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:40|"Unverified publisher"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:41|"Checking installed tools and trusted source metadata on background threads…"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:42|"Retry plan"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:44|"Ubuntu ${p.distroId} · ${p.architecture} · Core API ${CoreApi.VERSION}"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:45|"Shared Ubuntu package download: $it bytes (including apt dependencies)"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:46|"Required package: ${d.pack.id} ${d.pack.version}\n${d.source}\n${d.bytes.size} bytes · SHA-256 ${d.pack.digest}\nPermissions: ${d.pack.permissions.joinToString { it.wire }}"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:50|"${r.name}: ${if(row.ready) "Reuse installed" else "Install / repair"}"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:51|"${row.installedVersion}\nRequired/source version: ${r.version}\nSource: ${r.source}\nDownload: ${if(row.ready) "0 bytes · reuse installed" else if(r.provider=="ubuntu-apt" && p.aptDownloadBytes!=null) "included in shared Ubuntu transaction" else r.bytes?.let { "$it bytes" } ?: "size unavailable from source; shown in terminal before transfer"}"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:52|"ubuntu-apt"|verbatim-technical-or-resource-id|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:52|"Uses the shared Ubuntu download shown above."|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:53|"Behavior: ${r.provider} · ${r.packages.joinToString()}${r.resource?.let { " · authenticated $it" }.orEmpty()}"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:55|"Completed tools stay installed on cancellation or uninstall. Package archives update atomically and support rollback. No environment, project or user data is removed."|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:57|"UI language: ${b.displayName} (${b.locale}) · ${b.entries.size} keys · English fallback. No development environment is required by this resource component."|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:58|"Permissions and execution: Ubuntu processes can access app/guest files; PRoot is not a hostile-code sandbox. Install only publishers you trust. Installation uses the existing Setup PTY. apt/dpkg cancellation waits for a safe boundary."|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:59|"I trust this unverified publisher"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:60|"Approve all listed permissions"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:62|"${it.type}: ${it.id}\n${it.descriptor}"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:68|"Confirm plan and install"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:69|"Cancel safely"|protected-host-English|
|app/src/main/java/dev/suicode/nativehost/NativePackageReview.kt:69|"Cancel"|protected-host-English|
|core/design/src/main/java/dev/blamspot/jcode/design/CompactSearchField.kt:95|"Clear search"|unbound-gap|
|core/design/src/main/java/dev/blamspot/jcode/design/IconPackLoader.kt:129|"description"|verbatim-technical-or-resource-id|
|core/design/src/main/java/dev/blamspot/jcode/design/IconPackLoader.kt:183|"description"|verbatim-technical-or-resource-id|
|core/design/src/main/java/dev/blamspot/jcode/design/JCodeLineIconSet.kt:130|"Custom minimal line icons."|unbound-gap|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerDetailScreen.kt:241|"$clamped%"|verbatim-technical-or-resource-id|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerPanelHeader.kt:133|"count"|verbatim-technical-or-resource-id|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerPanelHeader.kt:178|"count"|verbatim-technical-or-resource-id|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerSectionCard.kt:66|"Collapse"|unbound-gap|
|core/design/src/main/java/dev/blamspot/jcode/design/ManagerSectionCard.kt:66|"Expand"|unbound-gap|
|core/design/src/main/java/dev/blamspot/jcode/design/SettingsAutocompleteRow.kt:143|"Hide suggestions"|unbound-gap|
|core/design/src/main/java/dev/blamspot/jcode/design/SettingsAutocompleteRow.kt:143|"Show suggestions"|unbound-gap|
|core/design/src/main/java/dev/blamspot/jcode/design/ThemeBundle.kt:42|"Soft pastel theme (Mocha / Latte)."|unbound-gap|
|core/design/src/main/java/dev/blamspot/jcode/design/ThemeBundle.kt:93|"Near-black canvas with a bright blue accent."|unbound-gap|
|core/design/src/main/java/dev/blamspot/jcode/design/ThemeBundle.kt:144|"Deep navy with soft, glowing accents."|unbound-gap|
|core/design/src/main/java/dev/blamspot/jcode/design/UiIconSet.kt:162|"Fixed semantic symbols for the native design system."|unbound-gap|
|core/design/src/main/java/dev/blamspot/jcode/design/VersionSection.kt:48|"Versions"|unbound-gap|
|core/design/src/main/java/dev/blamspot/jcode/design/VersionSection.kt:48|"Version"|unbound-gap|
|core/design/src/main/java/dev/blamspot/jcode/design/VersionSection.kt:98|"Loading versions…"|unbound-gap|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/VariableInspector.kt:177|"Inspect"|unbound-gap|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/VariableInspector.kt:183|"Copying…"|unbound-gap|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/VariableInspector.kt:183|"Copy value"|unbound-gap|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/VariableInspector.kt:261|"Fields"|unbound-gap|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/VariableInspector.kt:290|"Copying…"|unbound-gap|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/VariableInspector.kt:290|"Copy"|unbound-gap|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/VariableInspector.kt:301|"Close"|unbound-gap|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/VariableInspector.kt:336|"Collapse"|unbound-gap|
|feature/editor-pane/src/main/java/dev/blamspot/jcode/feature/editor/pane/VariableInspector.kt:336|"Expand"|unbound-gap|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:294|"count"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:295|"reason"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:307|"reason"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:318|"count"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:318|"name"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:319|"reason"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:351|"name"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:355|"name"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:387|"count"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:388|"sort"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:427|"name"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:427|"reason"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:432|"count"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:494|"count"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:501|"reason"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:520|"name"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:522|"reason"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:543|"name"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:545|"reason"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:562|"name"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:565|"retention"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:565|"count"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:592|"name"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:592|"name"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:594|"reason"|verbatim-technical-or-resource-id|
|feature/explorer/src/main/java/dev/blamspot/jcode/feature/explorer/ExplorerView.kt:999|"${formatSize(row.node.sizeBytes)} · ${formatModified(row.node.modifiedAtMillis)}"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:60|"shortDescription"|unbound-gap|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:60|"description"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:359|"label"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:363|"description"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:377|"description"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:536|"label"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:536|"Run"|unbound-gap|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:547|"label"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:558|"description"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:572|"title"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:572|"Sample"|unbound-gap|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:573|"description"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:674|"label"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:696|"label"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:696|"Run"|unbound-gap|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:705|"label"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:723|"description"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:784|"label"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:785|"detail"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:785|"insert"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:789|"title"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/ExtensionInstaller.kt:790|"snippet"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/VsixPackage.kt:110|"description"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/VsixPackage.kt:154|"markdownDescription"|unbound-gap|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/VsixPackage.kt:155|"description"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/VsixPackage.kt:190|"title"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/VsixPackage.kt:230|"title"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/VsixPackage.kt:288|"title"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/VsixPackage.kt:294|"$category: $title"|verbatim-technical-or-resource-id|
|feature/marketplace/src/main/java/dev/blamspot/jcode/feature/marketplace/VsixPackage.kt:295|"icon"|verbatim-technical-or-resource-id|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:150|"JCode"|verbatim-technical-or-resource-id|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:158|"Starting $distroLabel…"|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:446|"Your previous environment is set up."|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:447|"Your previous install left its Linux environment, projects, extensions ""and settings behind. Importing them puts JCode back where it was."|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:449|"Allow storage access above to continue."|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:500|"Environment ready — ${environmentState.runtime.selectedDistro.label} is set up."|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:501|"Choose the Linux distro JCode should prepare for your embedded environment."|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:502|"Allow storage access above to continue."|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:534|"Use ${environmentState.runtime.selectedDistro.label}"|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:538|"Restore from backup…"|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:547|"Or restore a .tar.gz backup into the selected distro — brings back its toolchains, VMs and files instead of a fresh download. Pick the distro that matches your backup first."|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:620|"$it%"|verbatim-technical-or-resource-id|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:681|"This device's WebView is Chromium $engineVersion, which modern sites can "|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:682|"render blank or broken. JCode's built-in browser and web previews use it. ""Recommended: install the latest Android System WebView, then pick it under ""Developer options → WebView implementation. If your device doesn't allow the ""switch, JCode keeps working on the ROM's engine."|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:728|"Install another Linux distro alongside the ones above. Each keeps its own SDKs ""and language servers."|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:762|"Switch which environment terminals and builds target. SDKs and language servers stay ""installed per environment. Open terminals keep their original environment."|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:790|"Remove ${target?.label ?: deleteId}? Its rootfs and everything installed inside it "|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:791|"(SDKs, language servers, packages) will be deleted. This cannot be undone."|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:854|"Emulated (QEMU)"|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:854|"Native"|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:855|" · active"|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:924|"Lets JCode migrate projects created by older versions from the shared /JCode ""folder. Projects now live in app storage — browse them via the \"JCode Projects\" ""entry in your Files app, or export them from the workspace menu."|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:936|"Storage access granted"|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:952|"Allow \"All files access\""|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:956|"Without it, projects fall back to app-private storage and are removed ""when the app is uninstalled. In the settings screen that opens, turn on ""\"Allow access to manage all files\" for JCode, then return here."|unbound-gap|
|feature/onboarding/src/main/java/dev/blamspot/jcode/feature/onboarding/OnboardingFeature.kt:1089|"$ $text"|verbatim-technical-or-resource-id|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:50|"Environment: ${environment.runtime.selectedDistro.label} · Linux ${environment.runtime.selectedDistro.arch.rootfsKey}"|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:58|"Version: ${release.version}"|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:59|"Source: ${release.source}"|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:60|"File: ${release.fileName} (${release.size / 1024 / 1024} MiB)"|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:61|"Download: ${release.downloadUrl}"|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:62|"SHA-256: ${release.sha256}"|verbatim-technical-or-resource-id|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:63|"Install location: ${release.home}"|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:64|"1. Download the reviewed archive over HTTPS.\n2. Check SHA-256 before extraction.\n3. Stage alongside existing JDKs.\n4. Verify java, javac, jshell; compile and run HelloWorld.\n5. Configure the managed JAVA_HOME and PATH. Preserve an existing usable system default."|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:75|"${if(item.active) "Active · " else ""}${item.version}\n${item.home}"|verbatim-technical-or-resource-id|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:83|"Install log in Ubuntu: $it"|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:88|"$home\n${if(remove) "Only this inactive managed installation will be deleted. Active or in-use JDKs are protected." else "java, javac and jshell will be verified before updating java.env. Existing terminals and language services need a restart."}"|verbatim-technical-or-resource-id|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:94|"Install Java ${release.version}?"|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/JavaEnvironmentPage.kt:95|"${release.source}\n${release.fileName}\n\nThe Ubuntu Setup terminal will open. Download, checksum verification and HelloWorld must succeed before activation. Existing JDKs and an existing usable system default will be kept.\n\nNo language plugin will be downloaded."|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:28|"Environment: ${environment.runtime.selectedDistro.label}"|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:36|"Latest upstream: $it\nSource: ${PythonReleaseProvider.SOURCE}\nUbuntu's supported package version may be older; it is installed and updated through signed Ubuntu repositories."|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:41|"Architecture: ${reviewed.architecture}"|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:42|"Source: the configured, signed Ubuntu package repositories. No extension marketplace is used."|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:43|"${pkg.name}: ${pkg.selected} · ${if (pkg.installed != null) "keep installed version" else "Ubuntu candidate"}"|verbatim-technical-or-resource-id|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:44|"1. Preserve an existing usable managed tool.\n2. Finish pending dpkg configuration, if any.\n3. Download reviewed packages with apt signature/hash verification.\n4. Keep installed requested versions; apt may configure required dependencies.\n5. Run a compile/interpreter smoke test, then atomically configure the native QuickRun environment."|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:45|"Cancellation is cooperative: apt/dpkg is allowed to finish its current operation. Completed packages are retained; no lock files are removed. Other language environments, including Java, are not overwritten."|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:52|"Open a .${language.let { when (it) { NativeEnvironment.C -> "c"; NativeEnvironment.Cpp -> "cpp"; NativeEnvironment.Python -> "py" } }} file in the editor and press Ctrl+F5. Runtime execution never installs packages automatically."|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:54|"Persistent Ubuntu log: $it"|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:59|"Install ${language.title}?"|unbound-gap|
|feature/sdk-manager/src/main/java/dev/blamspot/jcode/feature/sdkmanager/NativeEnvironmentPage.kt:60|"Ubuntu packages: ${reviewed.packages.joinToString { "${it.name}=${if(reviewed.upgrade) it.candidate else it.selected}" }}\n\nThe existing Setup terminal will open. Review apt output there. Cancellation waits for a safe boundary; this action does not remove or upgrade your JDK."|unbound-gap|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:509|"percent"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:814|"Chromium $engineVersion"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:892|"proot"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:925|"step"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1033|"ADB bridge"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1046|"JCode"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1057|"version"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1066|"version"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1086|"percent"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1293|"builtin"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1351|".jcode"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1402|"Compile and pack your extension with the JCode extension make tool, then import the ""unsigned .jext from the Extensions panel — the Ext Dev tab auto-reloads it on each ""rebuild. Only signed packages (signed privately by the JCode maintainers) reach the ""marketplace."|protected-host-English|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1510|"Tree"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1516|"Tree"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1710|"query"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1779|"title"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1779|"title"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1927|"count"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:1958|"count"|verbatim-technical-or-resource-id|
|feature/settings/src/main/java/dev/blamspot/jcode/feature/settings/SettingsFeature.kt:2203|"name"|verbatim-technical-or-resource-id|
