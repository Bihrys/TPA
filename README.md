# TPA Fabric Mod

一个简单实用的 **Minecraft Fabric 服务端模组**，提供常用的玩家传送功能，例如 **TPA、Home、Back** 等。

因为 Fabric 服务端缺少简单好用的 TPA 模组，所以我写了这个模组。

---

![demo](图片URL)

> 支持 Fabric 服务端使用，轻量、简单、即装即用。

---

# ✨ 主要功能

### 📍 玩家传送请求 (TPA)
向其他玩家发送传送请求。

```
/tpa <玩家>
```

对方可以选择接受或拒绝。

```
/tpaaccept
/tpadeny
```

---

### 🏠 Home 系统
玩家可以设置多个家并快速传送。

设置家的位置
```
/sethome <名称>
```

传送回家
```
/home <名称>
```

删除家的位置
```
/delhome <名称>
```

---

### 💀 Back 系统
玩家死亡后可以返回上一次死亡地点。

```
/back
```

---

### ⏳ 传送读条机制

所有传送都有 **5秒读条**：

- 期间 **移动会取消传送**
- **受到伤害会取消传送**

这样可以避免战斗中滥用传送。

---

# 🚀 使用方法

### 1 克隆仓库

```bash
git clone https://github.com/你的用户名/TPA.git
```

### 2 编译模组

```bash
gradlew build
```

### 3 安装模组

把生成的 `.jar` 文件放入服务器 `mods` 文件夹。

```
server/
 ├─ mods/
 │   └─ TPA.jar
```

然后启动服务器即可。

---

# 🛠 技术栈

- Java
- Fabric Loader
- Fabric API
- Minecraft 1.21

---

# 📦 适用环境

- Minecraft **Fabric Server**
- Fabric Loader **0.15+**
- Fabric API **0.102.0+**

---

# 📜 License

本项目使用 **MIT License** 开源。

你可以自由使用、修改和分发。

---

# ❤️ 项目说明

这是一个个人学习与实践项目，如果你有建议或者发现 Bug，欢迎提交 Issue 或 Pull Request。
