# LifeStream 博客项目

## 1. 项目概述

**LifeStream** 是一个旨在记录灵感、分享瞬间的数字生命流平台。它不仅支持技术文章的发布与讨论，还集成了云端相册功能，为用户提供视觉与文字双重维度的社交体验。项目核心设计哲学为 **“流光玻璃 (Luminous Glass)”**，通过大量的毛玻璃特效、渐变色及流畅动画提供极致的 UI 交互体验。

## 2. 核心技术栈

### 2.1 后端技术

* **核心框架**：Spring Boot 4.0.1
* **安全框架**：Spring Security 7
* **持久层**：Spring Data JPA + Hibernate。
* **数据库**：MySQL 8.0+。
* **身份认证**：JSON Web Token (JWT)。
* **日志系统**：自定义异步日志服务 (LogService)。

### 2.2 前端技术

* **模板引擎**：Thymeleaf。
* **CSS 框架**：Bootstrap 5.2.3。
* **交互脚本**：jQuery 3.6.0。
* **富文本编辑器**：Summernote。
* **组件库**：Select2, LightGallery。

---

## 3. 系统架构与模块设计

### 3.1 安全与认证模块

系统采用双重认证策略，兼顾 Web 端渲染与 API 调用：

* **JWT 机制**：`JwtTokenProvider` 负责生成 HS512 算法加密的令牌。
* **混合认证**：`JwtAuthenticationFilter` 同时支持从请求头 (Authorization Bearer) 和 HttpOnly Cookie 中提取 Token。
* **防御措施**：
* **CSRF 保护**：通过 `CsrfCookieFilter` 将令牌写入 Cookie，防止跨站请求伪造。
* **密码加密**：使用 `BCryptPasswordEncoder` 存储强加密后的密码。
* **权限控制**：基于角色的访问控制 (RBAC)，预设 `ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN`。



### 3.2 内容管理模块 (Posts)

* **文章功能**：支持 CRUD、草稿/发布状态切换、摘要自动提取。
* **分类体系**：多对多标签系统 (Tag System)。
* **归档与搜索**：支持基于关键词的全文搜索及基于年月的时光轴归档。

### 3.3 多媒体管理模块 (Albums & Images)

* **云相册**：用户可以创建多个相册，支持封面设置。
* **文件存储**：`FileStorageService` 负责将文件物理存储在服务器磁盘，并在数据库记录元数据。
* **图片处理**：支持拖拽上传、进度条显示，并通过 API 实现图片的安全流式下载/预览。

### 3.4 用户空间 (Profile)

* **动态展示**：展示个人文章、相册统计及加入时间。
* **资料编辑**：支持用户名查重、个人简介修改及头像异步上传更新。

---

## 4. 核心数据模型 (Entities)

| 实体类 | 说明 | 核心字段 |
| --- | --- | --- |
| `User` | 用户基础信息 | username, email, password, avatarUrl, bio |
| `Post` | 文章实体 | title, content, summary, published, author, tags |
| `Album` | 相册实体 | name, description, creator, images |
| `Image` | 图片元数据 | filename, path, contentType, fileSize, album |
| `Comment` | 评论实体 | content, post, author, parentComment (支持回复) |
| `Log` | 系统日志 | type, userId, ipAddress, details, successful |

---

## 5. API 规范摘要

### 5.1 认证接口 (`/api/auth`)

* `POST /signin`：登录并获取 JWT 令牌。
* `POST /signup`：新用户注册。
* `POST /logout`：注销并清除认证 Cookie。

### 5.2 内容接口 (`/api/posts`)

* `GET /`：获取分页的已发布文章列表。
* `POST /`：创建新文章 (需 USER 权限)。
* `PUT /{id}`：修改文章。

### 5.3 图片接口 (`/api/images`)

* `POST /upload`：上传图片到指定相册。
* `GET /download/{id}`：获取图片流 (支持浏览器直接渲染)。

---

## 6. 环境配置要求

* **JDK**: 21。
* **Database**: MySQL 8.0。
* **配置文件** (`application.properties`)：
* `app.jwtSecret`: 必须设置为至少 64 字节的长随机字符串以支持 HS512。
* `file.upload-dir`: 定义图片物理存放路径（默认 `./uploads`）。
* `spring.threads.virtual.enabled`: 建议开启 Java 21 虚拟线程优化。



## 7. 安装与启动

1. **数据库初始化**：创建名为 `life-stream` 的数据库，并配置 `spring.datasource`。
2. **数据初始化**：系统启动后，`DataInitializer` 会自动创建初始角色。在 `dev` 环境下会生成测试账号 (`admin/admin123`, `user/user123`)。
3. **运行项目**：执行 `LifeStreamApplication.java` 的 `main` 方法。

---

**版本**：0.1.7测试版  
**维护**：Starinova开发组
