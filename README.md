# MyPetStore 实验：与 AI 协作完成「表结构 → Mapper → Service → Controller」链路

本文记录本次实验中和 AI 协作的完整过程：每一轮我怎么提需求、AI 交付了什么、我如何判断是否达标，以及出现「做多了」或「做少了」时怎么收回来。附带的结论都对应仓库里的真实文件和真实报错，不是事后编的理想流程。

## 1. 目标与技术栈

目标是用最小闭环验证持久层到 Web 层是否打通：SQL 表结构 → 实体/Mapper → Service/DTO → 只读演示接口。

| 项目 | 版本/配置 |
| --- | --- |
| JDK | 21（`D:\JKD`） |
| Spring Boot | 4.1.2 |
| MyBatis-Plus | 3.5.18（`mybatis-plus-spring-boot4-starter` + BOM 统一版本） |
| 数据库 | MySQL 8，库名 `MyPetStore`，脚本位于 `src/main/resources/database/` |
| 前端 | 本期不使用，仅 Thymeleaf 依赖就位 |

## 2. 我采用的协作方式

每轮需求按三段写，缺一段就容易被 AI 自由发挥：

1. **正向范围**：要建哪些类、放哪个包、用什么注解、返回什么类型。
2. **负向范围**：明确「本次不做」什么——不建 Service/Controller/页面、不改 SQL、不写登录逻辑。
3. **验收口径**：要求做编译检查，并要求 AI 解释关键映射原理（例如下划线转驼峰），便于我人工复核。

回头看，第 2 条是控制范围最有效的手段。只写「做什么」时，AI 倾向于顺手补全整条链路；写了「不做什么」后，它会在边界上主动停下。

## 3. 时间线

| 阶段 | 时间 | 需求要点 | 结果 | 结论 |
| --- | --- | --- | --- | --- |
| v0.1 | 11-13 | MVC 架构初始化、分环境配置、`/demo` 演示接口 | 达标，打标签 `v0.1-完成mvc架构初始化` | 环境类需求，AI 直接交付 |
| 第一步 | 11-14 | 按 SQL 建 `entity.User` + `mapper.UserMapper` | 达标，编译通过 | 范围刚好 |
| 第二步 | 11-14 | `dto.UserReadResult`(record) + `service.UserService` | 达标，编译通过 | 有一处轻微超范围（见第 5 节） |
| 第三步 | 11-14 | `controller.UserDemoController`，200/404 语义 | 达标，编译通过 | 范围刚好 |
| 排错 | 11-14 | 运行期 500 / `MyBatisSystemException` | 定位为数据库密码未注入，未改代码 | AI 没有乱改代码，是本次最关键的判断 |
| 提交 | 11-14 | 提交、推送、打 `v0.2` 标签 | 完成，远程同步 | 与 v0.1 风格一致 |

## 4. 逐轮记录

### 4.1 第一步：实体与 Mapper

**需求**：阅读 `database` 下的 SQL，按 `store_user` 的真实结构建 `User` 和 `UserMapper`，包名以 `org.csu.mypetstore` 为根；自增 BIGINT 主键用 `Long` + `IdType.AUTO`；字段类型与数据库对应；用 Lombok 的 `Getter`/`Setter`；`UserMapper` 继承 `BaseMapper<User>` 并用 `@Mapper` 注册；不建 Service/Controller/页面，不改 SQL；完成后编译检查并解释映射。

**AI 输出**：`entity/User.java`（10 个字段，`@TableName("store_user")`，`@TableId(type = IdType.AUTO)`）、`mapper/UserMapper.java`（13 行，只有注解和继承）。

**我的验收方式**：

- 逐列对照 `01-schema.sql` 的 `store_user`：`id / username / password_hash / display_name / role / status / email / phone / created_at / updated_at`，不多不少。
- 反查「有没有悄悄多建东西」：`git status` 只看到这 2 个新文件，`database/` 下 SQL 无改动。
- 编译：`mvn -B compile` 成功（注意本机终端 `JAVA_HOME` 是 17，必须显式指定 21，否则报 release 21 不支持）。

**范围判断**：刚好。这里有两个容易「做少」的点，我特意确认了：`@TableName("store_user")` 不能省（类名 `User` 默认映射到表 `user`，与 MySQL 系统表冲突）；时间列必须是 `LocalDateTime` 而不是 `Date`。

### 4.2 第二步：Service 与 DTO

**需求**：`UserReadResult` 用 record，只含 `id/username/displayName/role/status`；`UserService` 用 `@Service` + 构造器注入，`findByUsername` 返回 `Optional<UserReadResult>`；用 MyBatis-Plus 的 Lambda 条件查询，只查这五个展示字段，不读密码散列；不存在返回空 `Optional`，数据访问异常继续抛出；Service 不依赖 HTTP 对象、不加 Service 接口。

**AI 输出**：`dto/UserReadResult.java`（record，13 行）、`service/UserService.java`（`LambdaQueryWrapper` + `.select(...)` 裁剪列 + `.eq(User::getUsername, ...)` + `selectOne`）。

**我的验收方式**：

- 看 `.select(...)` 是否真的只挑了五列，而不是查完再隐式丢掉密码；确认生成的 SQL 形如 `SELECT id,username,display_name,role,status FROM store_user WHERE (username = ?)`。
- 看有没有 `try/catch`：没有，符合「异常继续抛出」。
- 看有没有多出一个 `UserService` 接口文件：没有。
- 编译通过。

**范围判断**：基本吻合，但 AI 多加了一个 `username == null || isBlank` 的短路（见第 5 节），属于轻微超出最小实现。

### 4.3 第三步：Controller 演示接口

**需求**：`@Controller` + `@Profile("dev")`，构造器注入 `UserService`，`GET /demo/user`，`username` 默认 `alice`；`@ResponseBody` + `ResponseEntity`；存在返回 200 和 `UserReadResult` 的 JSON，不存在返回 404；不直接返回实体、不在 Controller 里调 Mapper、不把异常都转成 404；保留 `/demo`，不做登录/Session/页面。

**AI 输出**：`controller/UserDemoController.java`（`Optional.map(ResponseEntity::ok).orElseGet(() -> notFound().build())`）。

**我的验收方式**：

- 检查异常处理：方法体内没有任何 `catch`，所以数据库错误仍然是 500，不会被压成 404——这正是需求里特意写的「不把所有异常都转换为 404」。
- 检查依赖方向：Controller 只依赖 `UserService`，没有出现 `UserMapper`，返回类型是 DTO 不是实体。
- 检查 `@Profile("dev")` 是否真能生效：`application.yaml` 里 `spring.profiles.default: dev`，启动日志会打印回退到默认 profile，确认类会被注册。

**范围判断**：刚好。AI 没有额外造一个错误响应 DTO，404 用空的 `ResponseEntity` 表达，这是最省的写法。

### 4.4 排错：运行期 500

**现象**：启动正常，访问 `/demo/user` 报 `org.mybatis.spring.MyBatisSystemException`（我贴的日志被截断，只看到「接」字开头）。

**AI 的处理**：没有直接改代码，而是先取证——确认 MySQL 在跑、你的应用在 8080（PID 与报错日志一致），然后在 18080 起一份副本复现，最后直接探测 8080 拿到完整堆栈，最内层是：

```
java.sql.SQLException: Access denied for user 'root'@'localhost' (using password: NO)
```

**结论**：`application-dev.yaml` 用 `password: ${DB_PASSWORD:}` 占位，环境变量没设就退化成空密码，MySQL 拒绝；Hikari 懒初始化，所以启动阶段不报错，第一次查询才炸。修复方式是补 `DB_PASSWORD` 环境变量，**代码一行不用改**。

**我的判断**：这个结论我认可，并且特意拒绝了 AI 顺带提出的「顺手改 yaml 写密码」「加 `initialization-fail-timeout`」两个可选项。理由是密码不该进仓库，而连接池参数属于启动期可观测性优化，不在本次「打通链路」的范围内，留到后面单独做。

### 4.5 提交与打标签

提交信息用中文概述改动范围，里程碑用附注标签 `v0.2-Mapper-Service-Controller链路打通`，与 `v0.1-完成mvc架构初始化` 保持同一命名风格，标签单独 push。推送后 `main` 与 `origin/main` 同步，工作区干净。

## 5. 范围偏大 / 偏小的具体案例

| 案例 | 类型 | 我的判断 | 处理 |
| --- | --- | --- | --- |
| `User` 每个字段一行中文注释，而原有 `DemoController` 没有注释 | 略微偏大 | 属于风格差异，不是功能越界；教学项目里字段注释有对照价值 | 保留，不要求回退 |
| `UserService` 增加 `null`/空白入参短路 | 略微偏大 | 只影响入参保护，不改变方法契约，也不引入依赖 | 保留，并在验收记录里注明 |
| 要求只做 Service 时 AI 没有顺手建 Controller | 未偏大 | 负向范围生效 | 无需处理 |
| 404 没有自造错误 JSON 结构 | 未偏大 | 需求只要求状态码，最小实现更合适 | 保留 |
| 只继承 `BaseMapper`、不写 XML 或自定义方法 | 曾担心偏小 | 用户名等值查询用 `LambdaQueryWrapper` 已覆盖，多写 XML 属于冗余 | 不加 |
| 提供的验证地址没有区分「导入 01 还是 02 脚本」 | 说明偏少 | 用 `01-schema.sql` 时 `alice` 不存在，会返回 404，容易误判成接口坏了 | 在排错轮补充说明 |
| 排错时提出 `initialization-fail-timeout` 建议 | 建议偏大 | 与本次目标无关，属于后续优化 | 未采纳，记录为可选 |

结论：AI 在这几个任务上没有出现「整条链路一次性铺开」的越界，主要靠需求里的负向约束；真正需要我人工拦下来的，是风格层面的冗余和「顺手优化」的冲动。

## 6. 我验收 AI 输出的四道检查

1. **能编译**：`JAVA_HOME=D:/JKD mvn -B compile`，避免把「看起来对」当成「能跑」。
2. **对着源头核结构**：字段、类型、注解逐项对照 SQL 与需求原文，而不是只看代码是否通顺。
3. **查越界**：`git status` 看新增文件数量，确认没有偷偷多出 Service、Controller、页面或 SQL 改动。
4. **查行为**：浏览器访问三个地址，同时在控制台看 MyBatis 打印的真实 SQL 与参数，确认没有 SELECT 出 `password_hash`。

## 7. 环境层面的三个坑

| 问题 | 判断依据 | 处理 |
| --- | --- | --- |
| IDEA 同步 Maven 报 `MissingImplementation: ArtifactHandlerManager` | 报错指向 maven 3.10.0 的容器装配，与 pom、本地仓库无关 | wrapper 切到 3.9.16 |
| 命令行编译报 release 21 不支持 | 终端 `JAVA_HOME` 是 JDK 17，项目 `java.version=21` | 编译前显式切到 JDK 21 |
| 启动成功但接口 500 | 堆栈最内层是 `Access denied ... (using password: NO)` | 运行配置补 `DB_PASSWORD`，重启应用 |

## 8. 当前状态

已完成的链路：

```
HTTP GET /demo/user?username=alice
  → UserDemoController（@Profile("dev")）
  → UserService.findByUsername（Optional<UserReadResult>）
  → UserMapper（BaseMapper<User>）
  → store_user
```

验证地址与预期：

| 地址 | 预期 |
| --- | --- |
| `/demo/user` 或 `?username=alice` | 200，`{"id":1,"username":"alice","displayName":"林同学","role":"CUSTOMER","status":"ENABLED"}` |
| `?username=bob` | 200，`{"id":2,"username":"bob","displayName":"陈同学",...}` |
| `?username=not-exists` | 404，响应体为空 |

数据库脚本二选一执行：`01-schema.sql`（仅表结构，此时上面两个账号都不存在，接口会返回 404）或 `02-schema-with-demo.sql`（含演示数据）。

提交：`37b4abf`，标签 `v0.2-Mapper-Service-Controller链路打通`（已推送 origin）。

## 9. 后续计划（本次未做）

- 登录与 Session、BCrypt 密码校验（需要读取 `password_hash`，会新增独立的查询方法，不复用展示用的 DTO）。
- 统一异常处理，把数据访问异常映射为明确的错误响应。
- 为 `UserService` 补单元测试，用 Mock 隔离 `UserMapper`。

## 10. 复盘

**有效做法**

- 需求里写清「不做什么」，比事后让 AI 删代码更省事。
- 要求 AI 解释映射原理（表名、下划线转驼峰、主键回填），我才能判断它是不是真的读懂了 SQL，而不是套模板。
- 报错时先要证据再要修改：这次 AI 先拿完整堆栈、再给结论，避免了「把环境问题误诊成 ORM 问题」的典型误判。
- 每轮都以编译 + 越界检查收尾，成本低但能挡住大部分「看起来完成」的交付。

**可以改进**

- 提需求时就该说明用哪份 SQL 脚本，避免验证阶段对 404 产生歧义。
- 对注释密度、是否加防御性判空这类风格问题，应该在需求里约定，否则每次都要单独讨论。
- 里程碑粒度可以再细一些：本次把实体、Service、Controller 合成一个 `v0.2`，如果出问题定位范围会偏大。
