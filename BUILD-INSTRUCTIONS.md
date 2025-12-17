# 前后端整合构建说明

## 项目结构
- `frontend/` - React 前端项目源代码
- `src/main/resources/static/` - Spring Boot 静态资源目录

## 构建步骤

### 方法一：使用 Maven 自动构建（推荐）
如果您的环境中有网络连接，可以使用以下命令自动构建：

```bash
mvn clean package
```

这将自动下载 Node.js 和 npm，安装前端依赖，并构建 React 项目。

### 方法二：手动构建

1. 进入前端项目目录：
   ```bash
   cd frontend
   ```

2. 安装依赖：
   ```bash
   npm install
   ```

3. 构建项目：
   ```bash
   npm run build
   ```

4. 构建产物将位于 `frontend/build/` 目录中

5. 使用 Maven 构建 Spring Boot 项目：
   ```bash
   cd ..
   mvn clean package
   ```

## 运行项目

构建完成后，使用以下命令运行项目：

```bash
mvn spring-boot:run
```

或者运行构建好的 JAR 文件：

```bash
java -jar target/repairing-center-0.0.1-SNAPSHOT.jar
```

## 访问应用

项目启动后，在浏览器中访问：

- 主页：http://localhost:8080
- 登录页面：http://localhost:8080
- 学生主页：http://localhost:8080/stuhome
- 维修工主页：http://localhost:8080/workerhome
- 管理员主页：http://localhost:8080/adminhome

## 注意事项

1. 确保数据库连接配置正确
2. 第一次运行时，系统会自动创建数据库表结构
3. 默认会生成一个管理员账户，可以在控制台日志中找到初始密码