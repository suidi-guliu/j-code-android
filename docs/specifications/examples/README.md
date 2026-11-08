# 可执行协议验证样例

这些是未签名的 **开发协议样例**，不是完整官方语言产品；没有嵌入 JDK、编译器、解释器或其他大型工具链。

| 目录 | 格式 | 内容 |
|---|---|---|
| minimal | suic | 最小语言注册插件 |
| minimal-kit | suik | 最小声明式面板 |
| java | suic | Java注册、环境声明及示例SimpleRun |
| c-cpp | suic | C/C++合包注册、环境声明及示例SimpleRun |
| python | suic | Python注册、环境声明及示例SimpleRun |
| java-kit / c-cpp-kit / python-kit | suik | 显式依赖、面板和lint请求样例 |
| special-feature | suik | 第三方分析工作流及统计面板 |
| illegal-plugin | suic | 故意越界声明panel和ui.panel权限，必须拒绝 |

`packages/` 包含实际二进制格式文件。用仓库根目录运行：

```sh
python3 tools/sui_packages/generate_examples.py
python3 tools/sui_packages/check_specifications.py
bash gradlew :core:native-packages:testDebugUnitTest
bash gradlew :core:native-packages:validatePackage -PsuiPackage="$PWD/docs/specifications/examples/packages/minimal.suic"
bash gradlew :core:native-packages:validatePackage -PsuiPackage="$PWD/docs/specifications/examples/packages/minimal-kit.suik"
# 以下必须失败且包含 E_BOUNDARY：
bash gradlew :core:native-packages:validatePackage -PsuiPackage="$PWD/docs/specifications/examples/packages/illegal-plugin.suic"
# 有依赖的Kit：
bash gradlew :core:native-packages:validatePackage -PsuiPackage="$PWD/docs/specifications/examples/packages/java-kit.suik" -PsuiInstalled=dev.suicode.example.java=0.1.0
```

Schema/规范同步检查使用开发依赖 `jsonschema`，见 `tools/sui_packages/requirements-dev.txt`。生产 JVM/Android 验证器不依赖 Python/jsonschema。

签名工具见 `pack.py --help`。私钥必须保存在安全目录且不可提交。未被应用固定信任的签名仍显示 unverified。打包成功不代替验证，验证成功不代表Android执行backend已经实现；这些 Basic 语言示例采用实际 NativeRecipe 支持的 resource/source 参数；安装授权后可运行。统计 Kit 按钮调用真实 PROJECT_READ。官方完整 IDE Kit、安装工具和模板源码另见 `native-library/src/`。
