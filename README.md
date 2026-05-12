# VoxPocket

一款基于本地大语言模型的智能助手应用，支持流畅的对话体验和完整的 Markdown 渲染。

## 功能特性

### 🤖 AI 对话
- 与本地部署的大语言模型进行实时对话
- 支持流式输出，实时显示 AI 响应
- 自动保存对话历史到本地数据库

### 💭 思考过程
- 显示 AI 的思考过程（如果模型支持）
- 可折叠/展开的思考过程卡片
- 一键隐藏/显示所有思考过程

### 📝 Markdown 渲染
支持完整的 Markdown 格式：
- **标题**：# H1 / ## H2 / ### H3
- **加粗**：**粗体文字**
- *斜体*：*斜体文字*
- ~~删除线~~：~~删除文字~~
- `行内代码`：灰色背景 + 绿色等宽字体
- 代码块：```多行代码```，绿色等宽字体
- 引用：> 引用文字（灰色斜体 + 竖线）
- 列表：- 无序列表 / 1. 有序列表
- 链接：[链接文字](https://example.com)，蓝色下划线
- 分割线：--- / ***

### 🎨 界面设计
- 深色主题，护眼设计
- 统一的品牌视觉（VoxPocket 紫）
- 用户消息和 AI 消息气泡清晰区分
- 流畅的动画效果

### ⚙️ 本地存储
- Room 数据库存储对话历史
- 永久保存所有对话记录
- 支持删除单个对话

## 技术架构

### 核心技术栈
- **UI 框架**：Jetpack Compose + Material3
- **编程语言**：Kotlin
- **数据库**：Room（本地持久化）
- **网络**：OkHttp + Gson（API 调用）
- **协程**：Kotlin Coroutines（异步处理）
- **最低 Android 版本**：Android 9 (API 28)

### 项目结构
```
com.voxpocket/
├── data/
│   ├── api/          # API 接口定义
│   ├── database/     # Room 数据库
│   └── repository/   # 数据仓库
├── ui/
│   ├── component/    # UI 组件
│   ├── page/         # 页面
│   ├── theme/        # 主题配置
│   └── viewmodel/    # ViewModel
└── MainActivity.kt   # 主入口
```

## 使用说明

### 1. 配置 API
首次使用需要配置大语言模型的 API：
1. 进入设置页面
2. 填写 API 地址（支持 OpenAI 兼容格式）
3. 填写 API Key
4. 保存配置

### 2. 开始对话
- 在主界面输入问题
- 点击发送按钮
- 等待 AI 回复（支持流式输出）
- 对话自动保存

### 3. 管理对话
- 左滑删除单个对话
- 点击头像切换对话
- 查看历史对话记录

### 4. 思考过程
- AI 回复时会显示思考过程卡片
- 点击展开/收起按钮查看详细内容
- 使用顶部开关一键隐藏/显示所有思考过程

## 打包说明

### 环境要求
- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.x

### 构建 Debug APK
```bash
cd d:\Desktop\Phone
.\gradlew.bat assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

### 构建 Release APK
```bash
.\gradlew.bat assembleRelease
```

Release APK 需要签名配置，请参考 Android Studio 官方文档配置签名密钥。

### 注意事项
- 确保 NDK 已正确配置（arm64-v8a）
- 确保 GGUF 模型文件路径配置正确
- 首次构建可能需要下载 Gradle 依赖，请保持网络连接

## 开发指南

### 添加新的 UI 组件
在 `ui/component/` 目录下创建新的 Composable 函数。

### 修改主题颜色
在 `ui/theme/Color.kt` 中修改颜色定义。

### 数据库操作
使用 Room DAO 进行数据持久化，具体请参考 `data/database/` 目录下的代码。

## License

本项目仅供学习和交流使用。

## 联系方式

如有问题或建议，请通过 GitHub Issues 反馈。
