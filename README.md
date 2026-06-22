# 校园报修管理系统

一个基于 Spring Boot + React 的校园报修管理系统，支持学生报修、维修工处理、管理员管理的完整业务流程。

## 项目结构

```
-reportingcenter/
├── backend/              # 后端项目 (Spring Boot)
│   ├── src/             # 源代码
│   │   ├── main/
│   │   │   ├── java/    # Java 代码
│   │   │   └── resources/ # 资源文件
│   │   └── test/        # 测试代码
│   ├── pom.xml          # Maven 配置
│   └── README.md        # 后端说明文档
├── frontend/             # 前端项目 (React + Vite)
│   ├── src/             # 源代码
│   │   ├── Admin/      # 管理员模块
│   │   ├── Student/    # 学生模块
│   │   ├── Worker/     # 维修工模块
│   │   └── services/   # API 服务
│   ├── package.json    # 依赖配置
│   └── vite.config.js  # Vite 配置
│   └── README.md        # 前端说明文档
└── README.md             # 本文件
```

## 技术栈

### 后端
- Spring Boot 3.x
- Spring Security + JWT
- JPA / Hibernate
- MySQL
- Maven

### 前端
- React 19
- Vite（构建工具）
- Ant Design 5
- React Router
- Ant Design Charts（数据可视化）

## 快速开始

### 🚀 一键启动（推荐）

**最简单的方式**：使用项目提供的启动脚本

#### Windows 批处理脚本（推荐）

**双击运行**或在命令行运行：

```bash
# 一键启动前后端
start-all.bat
```

或在项目根目录双击 `start-all.bat` 文件。

这将自动：
1. 启动后端服务（http://localhost:8080）
2. 启动前端开发服务器（http://localhost:3000）
3. 在两个独立的命令行窗口中运行

#### 单独启动后端

```bash
cd backend
start-backend.bat
```

或双击 `backend/start-backend.bat` 文件。

#### 单独启动前端

```bash
cd frontend
start-frontend.bat
```

或双击 `frontend/start-frontend.bat` 文件。

#### PowerShell 脚本（需设置执行策略）

如果已设置 PowerShell 执行策略，可使用：

```powershell
.\start-all.ps1
```

---

### 📋 手动启动

#### 后端启动

```bash
cd backend
mvn spring-boot:run
```

访问：http://localhost:8080

#### 前端开发

```bash
cd frontend
npm install  # 首次运行需要安装依赖
npm run dev
```

开发服务器运行在 http://localhost:3000，API 请求自动代理到后端。

---

### 🔨 前端构建与集成

```bash
cd frontend
npm run build
```

构建产物自动输出到 `backend/src/main/resources/static/`，与后端集成。

启动后端后访问 http://localhost:8080 即可使用完整应用。

---

### ⚙️ 环境要求

- **Java**: JDK 21+ (启动脚本自动配置)
- **Maven**: 3.6+
- **Node.js**: 18+
- **MySQL**: 8.0+ (需提前启动服务)

---

### 📝 首次运行注意事项

1. **数据库配置**：
   - 确保 MySQL 服务已启动
   - 数据库 `repairdb` 会自动创建
   - 初始账号在 `backend/src/main/resources/schema.sql`

2. **前端依赖**：
   - 首次运行前端需要先执行 `npm install`

3. **启动顺序**：
   - 建议先启动后端，等待数据库初始化完成
   - 再启动前端开发服务器

4. **PowerShell 执行策略**（仅对 .ps1 脚本）：
   - 如果遇到"禁止运行脚本"错误，使用 .bat 脚本
   - 或设置执行策略：`Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser`

## 功能特性

### 学生端
- 提交报修申请（支持图片上传）
- 查看我的报修单列表
- 查看报修单详情和进度
- 评价维修服务
- 个人信息管理

### 维修工端
- 查看分配的任务列表
- 开始/完成维修任务
- 添加维修记录和说明
- 查看任务详情
- 个人信息管理

### 管理员端
- 用户管理（学生、维修工的增删改查）
- 工单管理（分配、驳回、状态跟踪）
- 数据统计分析（分类统计、地点统计、评分统计）
- 系统备份与恢复
- 反馈管理
- 密码重置

## 数据库配置

在 `backend/src/main/resources/application.yml` 中配置数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/repairdb
    username: your_username
    password: your_password
```

首次启动时会自动创建数据库表结构。

## API 文档

主要 API 端点：

- `/api/auth/**` - 认证相关
- `/api/tickets/**` - 报修单管理
- `/api/users/**` - 用户管理
- `/api/categories/**` - 分类管理
- `/api/admin/backup/**` - 备份管理（需管理员权限）

详细 API 文档请参考后端 README.md。

## 开发指南

### 分支管理
- `main` - 生产分支
- `develop` - 开发分支
- `feature/*` - 功能分支

### 提交规范
- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `refactor`: 代码重构
- `test`: 测试相关

## 部署说明

### 后端部署

```bash
cd backend
mvn clean package
java -jar target/repairing-center-0.0.1-SNAPSHOT.jar
```

### 前端部署

前端已配置自动集成，构建产物直接输出到后端静态资源目录：

```bash
cd frontend
npm run build  # 自动输出到 backend/src/main/resources/static/
```

或使用一体化构建：

```bash
# 在项目根目录
cd frontend && npm run build && cd ../backend && mvn clean package
```

## 许可证

本项目仅供学习使用。