# LifeStream博客系统

一个基于 Spring Boot 的个人博客系统，提供文章发布、图片归档、评论等功能。

## 技术栈

### 后端
- Java 11 +
- Spring Boot 2.7.x
- Spring Security
- Spring Data JPA
- MySQL/PostgreSQL
- Maven

### 前端
- Thymeleaf
- Bootstrap 5
- jQuery
- Font Awesome
- Summernote (富文本编辑器)

## 功能特性

- 用户认证与授权
    - 基于 JWT 的认证
    - 角色管理 (管理员、普通用户)

- 文章管理
    - 创建、编辑、删除文章
    - 富文本编辑器支持
    - 标签管理
    - 文章搜索
    - 按标签或日期归档

- 评论系统
    - 发表评论和回复
    - 评论管理

- 图片管理
    - 相册创建和管理
    - 图片上传和展示
    - 图片查看器

- 个人信息
    - 用户资料设置
    - 头像设置

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── yourblog/
│   │           ├── config/         # 配置类
│   │           ├── controller/     # 控制器
│   │           ├── entity/         # 实体类
│   │           ├── exception/      # 异常处理
│   │           ├── payload/        # 请求/响应对象
│   │           ├── repository/     # 数据访问层
│   │           ├── security/       # 安全配置
│   │           ├── service/        # 业务逻辑层
│   │           └── util/           # 工具类
│   └── resources/
│       ├── static/                 # 静态资源
│       │   ├── css/                # 样式文件
│       │   ├── js/                 # JavaScript文件
│       │   └── images/             # 图片资源
│       ├── templates/              # Thymeleaf模板
│       │   ├── fragments/          # 布局片段
│       │   ├── auth/               # 认证相关页面
│       │   ├── posts/              # 文章相关页面
│       │   ├── albums/             # 相册相关页面
│       │   └── error/              # 错误页面
│       └── application.properties  # 应用配置
└── test/                           # 测试代码
```

## 安装与运行

### 前提条件

- JDK 11+
- Maven 3.6+
- MySQL 8+ 或 PostgreSQL 12+

### 步骤

1. 克隆仓库
   ```bash
   git clone https://github.com/yourusername/yourblog.git
   cd yourblog
   ```

2. 配置数据库
   在 `application.properties` 中修改数据库连接信息：
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/yourblog_db?useSSL=false&serverTimezone=UTC
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. 构建项目
   ```bash
   mvn clean package
   ```

4. 运行应用
   ```bash
   java -jar target/yourblog-0.0.1-SNAPSHOT.jar
   ```
   或使用 Maven:
   ```bash
   mvn spring-boot:run
   ```

5. 访问应用
   打开浏览器访问 `http://localhost:8080`

## 初始账号

应用启动后，系统会自动创建以下测试账号（仅在开发环境）:

- 管理员账号:
    - 用户名: admin
    - 密码: admin123

- 普通用户账号:
    - 用户名: user
    - 密码: user123

## 开发指南

### 添加新功能

1. 在 `entity` 包中创建新的实体类
2. 在 `repository` 包中创建对应的 Repository 接口
3. 在 `service` 包中实现业务逻辑
4. 在 `controller` 包中创建新的控制器
5. 在 `templates` 目录中添加所需的 Thymeleaf 模板

### 配置说明

- 开发环境: `application-dev.properties`
- 生产环境: `application-prod.properties`

可以通过以下方式指定环境:
```bash
java -jar yourblog.jar --spring.profiles.active=prod
```

## 部署

### 使用 Docker

1. 构建 Docker 镜像
   ```bash
   docker build -t yourblog .
   ```

2. 运行容器
   ```bash
   docker run -p 8080:8080 yourblog
   ```

### 使用传统方式

1. 配置生产环境
   ```bash
   java -jar yourblog.jar --spring.profiles.active=prod
   ```

2. 使用 Nginx 作为反向代理（推荐）

## 贡献指南

1. Fork 仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 许可证

此项目基于 PC-NC 许可证

Copyright (c) 2025 PrincipleCreativityOrg.

Licensed under the PrincipleCreativity Non-Commercial License (PC-NC).

查看 [LICENSE](LICENSE) 文件了解详情
