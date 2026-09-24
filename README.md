# Treasure Family Backend（宝藏之家后端）

对应前端项目 `treasure-family-frontend` 的货物管理 API。

当前前端还把数据存在浏览器 `localStorage`。这个后端已经按同样的货物字段和分类提供了接口，之后可以把前端改成请求这里。

## 环境

- JDK 17+
- Maven 3.9+

本机如果还没装 Java，先安装 Temurin 17，再执行下面命令。

## 启动

在 `treasure-family-backend` 目录：

```bash
mvn spring-boot:run
```

服务地址：http://localhost:8080

H2 控制台：http://localhost:8080/h2-console

- JDBC URL：`jdbc:h2:file:./data/treasure-family`
- 用户名：`sa`
- 密码：空

## 接口

统一返回：

```json
{ "code": 0, "message": "ok", "data": {} }
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/goods` | 货物列表，可选 `keyword`、`category`、`onlyLow`、`sortBy` |
| GET | `/api/goods/{id}` | 货物详情 |
| POST | `/api/goods` | 新增货物 |
| PUT | `/api/goods/{id}` | 编辑货物 |
| DELETE | `/api/goods/{id}` | 删除货物 |
| DELETE | `/api/goods` | 清空全部货物 |
| GET | `/api/stats` | 看板统计 |
| GET | `/api/stats/categories` | 分类库存价值 |
| GET | `/api/alerts` | 低库存补货提醒 |
| GET | `/api/categories` | 分类列表 |

分类值和前端一致：`食品饮料`、`生鲜果蔬`、`日用百货`、`家居厨房`、`文具办公`、`其他`。

新增货物示例：

```json
{
  "name": "矿泉水",
  "category": "食品饮料",
  "price": 2.5,
  "stock": 24,
  "unit": "瓶",
  "supplier": "农夫山泉",
  "threshold": 10
}
```

已允许前端本地地址跨域：`http://localhost:5173`。

## 数据库说明

本地默认连的是 **H2 文件库**，数据在项目目录的 `data/` 里。这已经是数据库，但只适合本机开发。

线上不要用 H2，请改成云厂商的 **MySQL**。重启服务器、换机器时，H2 文件很容易丢；多开几个服务实例也无法共用同一份数据。

## 线上怎么用

GitHub Pages 只能放前端网页，Java 后端和数据库必须另外部署。

1. 开通一台能跑 Java 的云服务器（阿里云 ECS、腾讯云 CVM 等），并开通一个 MySQL（可以用云数据库 RDS，也可以装在同一台服务器上）。
2. 在 MySQL 里建库，例如 `treasure_family`，并创建一个专用账号。
3. 在后端目录打包：

```bash
mvn -DskipTests package
```

4. 把 `target/treasure-family-backend-0.0.1-SNAPSHOT.jar` 上传到服务器，用生产配置启动：

```bash
java -jar treasure-family-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

同时设置环境变量（不要把密码写进代码里）：

```bash
set SPRING_PROFILES_ACTIVE=prod
set MYSQL_HOST=你的数据库地址
set MYSQL_PORT=3306
set MYSQL_DATABASE=treasure_family
set MYSQL_USER=你的账号
set MYSQL_PASSWORD=你的密码
set CORS_ORIGINS=https://liangjs0505-dotcom.github.io
```

Linux 用 `export` 代替 `set`。安全组 / 防火墙要放行 `8080`，或前面加 Nginx 反代到 80/443。

5. 浏览器访问 `http://服务器IP:8080/api/health`，能返回 `Treasure Family Backend` 就说明服务通了。

6. 前端现在还把货物存在浏览器里，**还没有请求这个后端**。后端上线后，还需要把前端改成调用 `http://你的后端地址/api/...`，页面上的数据才会进 MySQL。

常见云数据库填写示例：`MYSQL_HOST` 填 RDS 内网或公网地址，`MYSQL_DATABASE` 填库名，账号密码用控制台创建的那个。
