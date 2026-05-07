# MyBatis 学习资料

## 学习顺序

1. MyBatis 官方 Getting Started

   地址：https://mybatis.org/mybatis-3/getting-started.html

   重点理解：

   - `SqlSessionFactory`
   - `SqlSession`
   - Mapper 接口
   - XML 的 `namespace + id`
   - Mapper 方法如何对应到一条 SQL

2. MyBatis Mapper XML 官方文档

   地址：https://mybatis.org/mybatis-3/sqlmap-xml.html

   重点理解：

   - `<select>` / `<insert>` / `<update>` / `<delete>`
   - `parameterType`
   - `resultType`
   - `resultMap`
   - `#{property}`
   - `${expression}`

3. MyBatis-Spring Mapper 扫描

   地址：https://mybatis.org/spring/mappers.html

   重点理解：

   - `@MapperScan`
   - `MapperFactoryBean`
   - Spring 为什么能注入 Mapper 接口
   - Mapper 接口代理对象是怎么来的

4. MyBatis-Spring SqlSessionFactoryBean

   地址：https://mybatis.org/spring/factorybean.html

   重点理解：

   - Spring 中的 `SqlSessionFactory`
   - MyBatis 配置如何接入 Spring
   - Spring Boot 自动配置背后创建了什么

5. MyBatis-Plus CRUD 接口

   地址：https://mybatis.plus/guide/crud-interface.html

   重点理解：

   - `BaseMapper`
   - 通用 CRUD
   - `Wrapper`
   - `Page`
   - MyBatis-Plus 如何在 MyBatis 基础上自动注册常用 SQL

## 核心心智模型

MyBatis 的调用链路可以先理解成：

```text
Spring 扫描 Mapper 接口
        ↓
给 Mapper 创建代理对象
        ↓
调用 campaignMapper.insert(campaign)
        ↓
代理对象找到 statementId
        ↓
statementId = Mapper 全限定名 + 方法名
        ↓
例如 com.linkflow.campaign.mapper.CampaignMapper.insert
        ↓
MyBatis 找 XML 或注解里的 SQL
        ↓
执行 SQL
```

MyBatis-Plus 在这个链路里额外做的事情是：

```text
如果 Mapper 继承 BaseMapper
MyBatis-Plus 启动时会自动注册 insert、updateById、selectById 等 MappedStatement
```

所以不要把 `BaseMapper#insert` 理解成“接口里有 Java 方法实现”，而要理解成：

```text
BaseMapper 声明方法
MyBatis-Plus 注册 SQL
MyBatis Mapper 代理执行 SQL
```

## 当前项目里要特别注意

如果 `CampaignMapper.xml` 里显式定义了：

```xml
<insert id="insert">
```

那么它对应的 statementId 是：

```text
com.linkflow.campaign.mapper.CampaignMapper.insert
```

这会和 `BaseMapper#insert` 的方法名相同。此时调用：

```java
campaignMapper.insert(campaign);
```

实际执行的更可能是 XML 中自定义的 `insert` SQL，而不是 MyBatis-Plus 自动注入的默认 SQL。

