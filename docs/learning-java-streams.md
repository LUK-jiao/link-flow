# Java Stream 和方法引用学习资料

## 为什么现在要学这个

在 `link-flow` 项目里，分页查询后经常会出现这种代码：

```java
List<CampaignDTO> records = campaignPage.getRecords()
        .stream()
        .map(this::convertToDTO)
        .toList();
```

这段代码的意思是：

```text
拿到 Campaign 列表
        ↓
转成 Stream 流
        ↓
把每一个 Campaign 转成 CampaignDTO
        ↓
收集成 List<CampaignDTO>
```

其中：

```java
this::convertToDTO
```

等价于：

```java
campaign -> this.convertToDTO(campaign)
```

也就是说，`this::convertToDTO` 是一种方法引用，表示“对流里的每一个元素调用当前对象的 `convertToDTO` 方法”。

## 推荐资料

1. Oracle Java Stream API 文档

   地址：https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html

   重点理解：

   - Stream 是“对一组数据进行流水线处理”的工具
   - `filter` 用来筛选
   - `map` 用来转换
   - `sorted` 用来排序
   - `toList` / `collect` 用来生成最终结果
   - Stream 操作分为中间操作和终止操作

2. Oracle Java 方法引用教程

   地址：https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html

   重点理解：

   - 方法引用是 lambda 的简写
   - `this::methodName`
   - `ClassName::staticMethodName`
   - `ClassName::instanceMethodName`
   - `ClassName::new`

3. Java `java.util.function` 包

   地址：https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html

   重点理解：

   - `Function<T, R>`：输入 T，返回 R
   - `Predicate<T>`：输入 T，返回 boolean
   - `Consumer<T>`：输入 T，不返回结果
   - `Supplier<T>`：不输入，返回 T

## 先记住的核心模型

Stream 代码可以先按这个模型理解：

```text
数据源
  .stream()
  .中间操作1()
  .中间操作2()
  .终止操作()
```

例子：

```java
List<CampaignDTO> dtoList = campaignList.stream()
        .filter(campaign -> campaign.getStatus() == 1)
        .map(this::convertToDTO)
        .toList();
```

对应含义：

```text
campaignList
        ↓
只保留 status == 1 的活动
        ↓
把 Campaign 转成 CampaignDTO
        ↓
生成 List<CampaignDTO>
```

## 常见操作

### map：转换元素

```java
List<String> names = users.stream()
        .map(UserDTO::getUsername)
        .toList();
```

含义：

```text
List<UserDTO> -> List<String>
```

### filter：过滤元素

```java
List<Campaign> draftCampaigns = campaigns.stream()
        .filter(campaign -> campaign.getStatus() == 0)
        .toList();
```

含义：

```text
只保留草稿状态的 Campaign
```

### sorted：排序

```java
List<Campaign> sortedCampaigns = campaigns.stream()
        .sorted(Comparator.comparing(Campaign::getCreateTime).reversed())
        .toList();
```

含义：

```text
按 createTime 倒序排列
```

### anyMatch：判断是否存在

```java
boolean hasApproving = campaigns.stream()
        .anyMatch(campaign -> campaign.getStatus() == 1);
```

含义：

```text
是否存在审批中的活动
```

### collect groupingBy：分组

```java
Map<Integer, List<Campaign>> statusMap = campaigns.stream()
        .collect(Collectors.groupingBy(Campaign::getStatus));
```

含义：

```text
按照 status 对 Campaign 分组
```

## 方法引用的几种形式

### 当前对象的方法

```java
.map(this::convertToDTO)
```

等价于：

```java
.map(campaign -> this.convertToDTO(campaign))
```

要求：

```java
private CampaignDTO convertToDTO(Campaign campaign)
```

### 静态方法

```java
.map(CampaignConverter::toDTO)
```

等价于：

```java
.map(campaign -> CampaignConverter.toDTO(campaign))
```

要求：

```java
public static CampaignDTO toDTO(Campaign campaign)
```

### 对象 getter

```java
.map(Campaign::getName)
```

等价于：

```java
.map(campaign -> campaign.getName())
```

### 构造方法

```java
.map(CampaignDTO::new)
```

等价于：

```java
.map(campaign -> new CampaignDTO(campaign))
```

要求 `CampaignDTO` 有对应构造方法。

## 常见坑

### 1. map 和 forEach 不要混用

如果目标是“转换成新列表”，用 `map`：

```java
List<CampaignDTO> dtoList = campaigns.stream()
        .map(this::convertToDTO)
        .toList();
```

不要写成：

```java
List<CampaignDTO> dtoList = new ArrayList<>();
campaigns.stream().forEach(campaign -> dtoList.add(convertToDTO(campaign)));
```

后者有副作用，代码也更绕。

### 2. Stream 只能消费一次

错误示例：

```java
Stream<Campaign> stream = campaigns.stream();
long count = stream.count();
List<Campaign> list = stream.toList();
```

第二次使用 `stream` 会报错。

### 3. 不要在 Stream 里修改原集合

错误示例：

```java
campaigns.stream()
        .forEach(campaign -> campaigns.remove(campaign));
```

遍历时修改源集合容易出问题。

### 4. 能用普通 for 循环时，也可以用普通 for 循环

Stream 适合表达“筛选、转换、聚合”。如果业务逻辑很复杂，普通循环可能更清楚。

## 练习题

### 练习 1：把实体转成 DTO

给定：

```java
List<Campaign> campaigns;
```

请用 Stream 写出：

```java
List<CampaignDTO> dtoList;
```

要求使用：

```java
this::convertToDTO
```

### 练习 2：筛选草稿活动

给定：

```java
List<Campaign> campaigns;
```

请筛选出：

```text
status == 0
```

的活动。

### 练习 3：提取活动名称

给定：

```java
List<Campaign> campaigns;
```

请得到：

```java
List<String> campaignNames;
```

要求使用方法引用。

### 练习 4：按创建时间倒序

给定：

```java
List<Campaign> campaigns;
```

请按照 `createTime` 从新到旧排序。

### 练习 5：判断是否存在审批中的活动

给定：

```java
List<Campaign> campaigns;
```

请判断是否存在：

```text
status == 1
```

的活动。

### 练习 6：按状态分组

给定：

```java
List<Campaign> campaigns;
```

请得到：

```java
Map<Integer, List<Campaign>> statusMap;
```

key 是 `status`，value 是对应状态下的活动列表。

### 练习 7：改写项目代码

把下面代码：

```java
List<CampaignDTO> dtoList = new ArrayList<>();
for (Campaign campaign : campaigns) {
    dtoList.add(convertToDTO(campaign));
}
```

改成 Stream 写法。

### 练习 8：看懂方法引用

把下面方法引用还原成 lambda：

```java
.map(this::convertToDTO)
```

```java
.map(Campaign::getName)
```

```java
.sorted(Comparator.comparing(Campaign::getCreateTime))
```

## 参考答案

### 练习 1

```java
List<CampaignDTO> dtoList = campaigns.stream()
        .map(this::convertToDTO)
        .toList();
```

### 练习 2

```java
List<Campaign> draftCampaigns = campaigns.stream()
        .filter(campaign -> campaign.getStatus() == 0)
        .toList();
```

### 练习 3

```java
List<String> campaignNames = campaigns.stream()
        .map(Campaign::getName)
        .toList();
```

### 练习 4

```java
List<Campaign> sortedCampaigns = campaigns.stream()
        .sorted(Comparator.comparing(Campaign::getCreateTime).reversed())
        .toList();
```

### 练习 5

```java
boolean hasApproving = campaigns.stream()
        .anyMatch(campaign -> campaign.getStatus() == 1);
```

### 练习 6

```java
Map<Integer, List<Campaign>> statusMap = campaigns.stream()
        .collect(Collectors.groupingBy(Campaign::getStatus));
```

### 练习 7

```java
List<CampaignDTO> dtoList = campaigns.stream()
        .map(this::convertToDTO)
        .toList();
```

### 练习 8

```java
.map(campaign -> this.convertToDTO(campaign))
```

```java
.map(campaign -> campaign.getName())
```

```java
.sorted(Comparator.comparing(campaign -> campaign.getCreateTime()))
```

