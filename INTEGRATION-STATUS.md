# 前后端整合状态说明

## 当前状态

1. **后端**：
   - Spring Boot 后端项目已完全可用
   - REST API 接口已实现
   - 数据库连接和实体映射正常工作

2. **前端**：
   - React 项目源代码已集成到 [frontend](file:///D:/xjz20/workspace/reportingcenter/src/main/resources/static) 目录中
   - 包含完整的管理员、学生和维修工三端功能
   - 配置了 Maven 插件用于自动化构建

3. **整合**：
   - 配置了 Maven 构建流程，可以自动构建前端项目并将其集成到 Spring Boot 应用中
   - 提供了手动构建的说明文档

## 如何完成整合

### 自动构建方式（推荐）

如果您的环境中可以访问互联网，可以使用以下命令：

```bash
mvn clean package
```

这将：
1. 自动下载 Node.js 和 npm
2. 安装前端依赖
3. 构建 React 项目
4. 将构建产物复制到 Spring Boot 静态资源目录
5. 构建完整的 Spring Boot 应用

### 手动构建方式

如果自动构建遇到问题，可以按照以下步骤操作：

1. 在有 Node.js 环境的机器上进入 frontend 目录
2. 运行 `npm install` 安装依赖
3. 运行 `npm run build` 构建项目
4. 将构建产物复制到 Spring Boot 的静态资源目录
5. 使用 `mvn clean package` 构建 Spring Boot 应用

## 运行应用

构建完成后，使用以下命令运行应用：

```bash
mvn spring-boot:run
```

或

```bash
java -jar target/repairing-center-0.0.1-SNAPSHOT.jar
```

## 访问应用

应用启动后，可以在浏览器中访问以下地址：

- 主页：http://localhost:8080
- 登录页：http://localhost:8080/login
- 学生主页：http://localhost:8080/stuhome
- 维修工主页：http://localhost:8080/workerhome
- 管理员主页：http://localhost:8080/adminhome

## 注意事项

1. 确保数据库配置正确
2. 第一次运行时会自动创建数据库表结构
3. 系统包含完整的用户认证和授权机制
4. 前端使用 Ant Design 组件库，提供了良好的用户界面体验