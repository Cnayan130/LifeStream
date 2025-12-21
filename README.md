# LifeStream 现代流媒体博客系统 (SpringBootv4.0.1)

## 📖 项目简介

**LifeStream** 是一款面向 2026 年标准构建的高性能、现代化个人动态流媒体与博客系统。项目基于最新的 **Spring Boot 4.0.1** 架构，利用 **JDK 21** 的虚拟线程（Virtual Threads）特性，在极低资源消耗下提供卓越的并发处理能力。系统集成了深度定制的 **Spring Security 7** 安全框架，支持无缝的 API/Web 混合认证模式，旨在为开发者提供一个既美观又健壮的个人内容管理平台。

---

## 🚀 核心技术栈 (2025 Edition)

### 1. 后端架构

* **核心框架**: Spring Boot 4.0.1 (基于 Spring Framework 7.0)
* **并发模型**: JDK 21 虚拟线程 (Project Loom) 全面启用
* **持久层**: Spring Data JPA + Hibernate 7.0 (Jakarta EE 11 兼容)
* **安全体系**: Spring Security 7.x (支持 JWT + HttpOnly Cookie 双重校验)
* **API 标准**: Spring Framework 7 内置 REST API 版本化管理
* **工具链**: Lombok, Jackson (JDK 21 Record 深度集成), Maven 3.9+

### 2. 前端技术

* **渲染引擎**: Thymeleaf 3.1+ (带缓存优化)
* **UI 框架**: Bootstrap 5.3 (支持暗黑模式自适应)
* **动态逻辑**: Vanilla JS (ES2024+) + jQuery 3.7
* **交互组件**: Summernote 富文本编辑器、Font Awesome 6+ 矢量图标库

---

## ✨ 核心功能亮点

### 🔐 零信任安全架构

* **双模认证**: 同时支持移动端/App 的原生 JWT Header 校验，以及浏览器端的无状态 `HttpOnly/Secure/SameSite=Strict` Cookie 校验，有效防御 XSS 与 CSRF 攻击。
* **细粒度鉴权**: 基于 `@EnableMethodSecurity` 的方法级权限控制，严格区分管理角色（ADMIN）与普通用户（USER）。

### 📝 现代化内容管理

* **流媒体博客**: 支持图文并茂的文章发布，内置高性能图片上传与存储服务，支持自动化的本地/云端存储切换。
* **相册归档**: 提供独立的相册空间，支持多级目录管理与响应式灯箱大图预览。
* **交互系统**: 嵌套式评论回复逻辑，支持实时状态异步刷新。

### ⚙️ 高性能运维支持

* **虚拟线程调优**: 针对 IO 密集型操作（如图片处理、邮件发送）自动映射至虚拟线程，极大提升单机吞吐量。
* **弹性配置**: 深度适配 Spring Boot 4 的多环境 Profile 管理（Dev/Prod/Test）。

---

## 🛠️ 快速启动

### 1. 环境依赖

* **Java**: JDK 21 或更高版本
* **数据库**: MySQL 8.4 (LTS) 或 PostgreSQL 16+
* **构建工具**: Maven 3.9.x

### 2. 数据库配置

在 `src/main/resources/application.properties` 中调整连接信息：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/life_stream?serverTimezone=Asia/Shanghai
spring.datasource.username=your_username
spring.datasource.password=your_password

# 开启 JDK 21 虚拟线程支持
spring.threads.virtual.enabled=true

```

### 3. 构建与运行

```bash
# 编译并打包
mvn clean package -DskipTests

# 启动应用
java -jar target/lifestream-4.0.1.jar

```

---

## 📂 项目结构规范

```text
top.principlecreativity.lifestream
├── config          # 全局配置 (Security, MVC, Async)
├── controller      # 路由层 (API 与 Web 分离)
│   ├── api         # RESTful 接口 (v1/v2)
│   └── web         # Thymeleaf 页面跳转
├── entity          # 领域模型 (JPA Entities / Records)
├── payload         # 数据传输对象 (DTOs)
├── repository      # 数据持久化接口
├── security        # 安全核心 (JWT, AuthProvider)
├── service         # 业务逻辑层
└── exception       # 全局异常处理机制

```

---

## 🌐 API 概览 (Partial)

| 终点 (Endpoint) | 方法 | 功能描述 | 权限 |
| --- | --- | --- | --- |
| `/api/auth/signin` | POST | 用户登录并获取 JWT/Cookie | 公开 |
| `/api/auth/logout` | POST | 安全注销并清除令牌 | 已登录 |
| `/api/posts` | GET | 分页获取文章列表 | 公开 |
| `/api/posts` | POST | 发布新文章 | ADMIN/USER |
| `/api/images/upload` | POST | 图片文件上传 | 已登录 |

---

## 🤝 贡献与反馈

1. 欢迎 Fork 本项目并提交 Pull Request。
2. 对于 Spring Boot 4 或 JDK 21 相关的新特性适配建议，请通过 Issue 反馈。
3. 项目遵循 **GNU GPL v3** 开源协议。
