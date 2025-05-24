# 用户手册

## 环境要求

- Java 8 或更高版本
- Maven 3.6 或更高版本
- 足够的磁盘空间用于存储爬取的内容
- 稳定的网络连接
- 推荐内存：4GB及以上

## 安装步骤

1. 克隆项目到本地
2. 进入项目目录：`cd my-java-wedcrawler`
3. 安装依赖：`mvn install`
4. 验证安装：`mvn test`

## 使用说明

### 1. 基础文本爬虫 (TextCrawl)

适用于简单的单页面文本爬取，支持自定义URL和输出目录。基于Java网络编程技术实现，主要使用HttpURLConnection进行网络请求，同时支持多种HTML解析方式：

#### 网络请求实现
- 使用HttpURLConnection建立连接
- 支持HTTP/HTTPS协议
- 可配置连接和读取超时
- 支持自定义请求头和Cookie

#### HTML解析技术
- 内置JSoup解析器，支持DOM操作
- 支持XPath表达式提取数据
- 提供CSS选择器语法
- 自动处理字符编码

#### 数据存储方案
- 文本文件存储（TXT格式）
- 结构化存储（JSON/CSV）
- 关系型数据库（MySQL）
- NoSQL数据库（MongoDB）

#### 基本配置示例：

```java
public class TextCrawl {
    private static String url = "https://sanguo.5000yan.com/";
    private static String outputDir = "D:\\新建文件夹\\";
    private static int timeout = 5000; // 连接超时时间（毫秒）
    private static String charset = "UTF-8"; // 网页编码
}
```

配置说明：
- `url`：目标网页地址，支持HTTP和HTTPS
- `outputDir`：输出目录路径，默认为`D:\新建文件夹\`
- `timeout`：连接超时设置，默认5000毫秒
- `charset`：网页编码格式，默认UTF-8

高级配置：
- 支持设置User-Agent
- 可配置代理服务器
- 支持自定义请求头
- 支持Gzip压缩
- Cookie管理
- 会话保持

#### 反爬虫策略
- 动态User-Agent轮换
- IP代理池支持
- 请求频率控制
- 随机延时处理
- 请求头伪装
- 验证码识别接口
- 异常请求重试机制

#### 异常处理
- 网络连接异常重试
- 解析失败容错
- 存储失败备份
- 代理失效切换
- 请求限制自适应

使用示例：
```java
// 基本使用
TextCrawl.setUrl("https://example.com");
TextCrawl.crawl();

// 高级配置
TextCrawl.setTimeout(10000);
TextCrawl.setCharset("GBK");
TextCrawl.setUserAgent("Mozilla/5.0...");
```

运行方法：
```bash
java -cp target/my-java-wedcrawler-1.0.jar cn.myh.java.TextCrawl
```

### 2. 自动文本爬虫 (AutoTextCrawler)

用于自动爬取整个网站的文本内容，具有智能链接提取和内容过滤功能。采用多线程并发爬取，支持分布式部署：

#### 网络请求框架
- 基于Apache HttpClient
- 连接池优化
- 支持异步请求
- 自动重定向处理

#### HTML解析引擎
- JSoup深度解析
- 正则表达式匹配
- 自定义内容提取器
- 智能编码识别

#### 数据持久化
- 文件系统存储
  - 分类目录管理
  - 自动文件命名
  - 增量更新支持
- 数据库存储
  - MySQL批量插入
  - MongoDB文档存储
  - Redis缓存支持
- 分布式存储
  - HDFS支持
  - ElasticSearch索引

#### 基本配置示例：

```java
public class AutoTextCrawler {
    private static final String BASE_URL = "https://sanguo.5000yan.com/";
    private static final String OUTPUT_DIR = "D:\\新建文件夹\\";
    private static final int MAX_URL_LIMIT = 100;
    private static final int REQUEST_DELAY_MS = 200;
    private static final int THREAD_COUNT = 3; // 并发线程数
    private static final boolean FOLLOW_EXTERNAL = false; // 是否爬取外部链接
}
```

配置说明：
- `BASE_URL`：起始URL，爬虫从此地址开始
- `OUTPUT_DIR`：输出目录，用于存储爬取的内容
- `MAX_URL_LIMIT`：最大处理URL数量，防止过度爬取
- `REQUEST_DELAY_MS`：请求延迟，避免对目标站点造成压力
- `THREAD_COUNT`：并发线程数，提高爬取效率
- `FOLLOW_EXTERNAL`：是否允许爬取外部链接

高级功能：
- URL过滤器：自定义URL匹配规则
- 内容提取器：支持XPath和CSS选择器
- 数据存储：支持多种格式（TXT、JSON、CSV）
- 断点续爬：支持中断后继续爬取

使用示例：
```java
// 配置爬虫
AutoTextCrawler crawler = new AutoTextCrawler();
crawler.setThreadCount(5);
crawler.addUrlFilter(".*/article/.*");
crawler.setContentSelector("div.content");

// 开始爬取
crawler.start();
```

运行方法：
```bash
java -cp target/my-java-wedcrawler-1.0.jar cn.myh.java.AutoTextCrawler
```

### 3. 自动图片爬虫 (AutoImaCrawler)

专门用于爬取网站图片，支持多种图片格式和智能去重：

```java
public class AutoImaCrawler {
    private static final String BASE_URL = "https://www.nipic.com/";
    private static final String SAVE_DIR = "D:\\新建文件夹\\";
    private static final int MAX_URL_COUNT = 50;
    private static final int RETRY_DELAY = 200;
    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "png", "gif"};
    private static final int MIN_IMAGE_SIZE = 10240; // 最小图片大小（字节）
}
```

配置说明：
- `BASE_URL`：图片网站URL，支持多个图片源
- `SAVE_DIR`：图片保存目录，自动创建子目录
- `MAX_URL_COUNT`：最大处理URL数量，控制爬取范围
- `RETRY_DELAY`：重试延迟时间，处理下载失败
- `ALLOWED_EXTENSIONS`：允许下载的图片格式
- `MIN_IMAGE_SIZE`：图片大小过滤阈值

特色功能：
- 图片去重：基于内容hash
- 智能命名：保留原始文件名
- 批量下载：多线程并发
- 断点续传：支持大文件下载

使用示例：
```java
// 配置图片爬虫
AutoImaCrawler crawler = new AutoImaCrawler();
crawler.addImagePattern(".*/photos/.*");
crawler.setMinSize(20480);
crawler.enableDeduplication(true);

// 开始下载
crawler.start();
```

运行方法：
```bash
java -cp target/my-java-wedcrawler-1.0.jar cn.myh.java.AutoImaCrawler
```

## 配置优化

### 1. 性能调优

网络优化：
- 增加 `MAX_URL_LIMIT` 扩大爬取范围
- 调整 `REQUEST_DELAY_MS` 平衡速度和稳定性
- 配置代理池避免IP限制
- 启用GZIP压缩减少传输量

并发优化：
- 调整线程数适应服务器负载
- 使用连接池复用HTTP连接
- 实现请求队列削峰填谷

存储优化：
- 使用缓冲区批量写入
- 采用异步IO提高写入性能
- 启用数据压缩节省空间

### 2. 内存优化

资源管理：
- 及时关闭文件句柄
- 使用软引用缓存数据
- 定期触发垃圾回收

数据结构：
- 使用布隆过滤器去重
- 采用LRU缓存热点数据
- 分批处理避免OOM

## 错误码说明

### 1. 通用错误码

- E001：网络连接失败
- E002：DNS解析错误
- E003：超时
- E004：认证失败
- E005：资源不存在

### 2. 爬虫特定错误码

- C001：URL格式错误
- C002：内容解析失败
- C003：存储路径无效
- C004：配置参数无效
- C005：并发数超限

## 常见问题

### 1. 爬取失败

可能原因：
- 网络连接不稳定
- 目标网站有反爬措施
- URL格式不正确
- 服务器返回403/404
- 代理服务器不可用

解决方案：
- 检查网络连接和DNS设置
- 增加请求延迟和随机等待
- 验证URL格式和编码
- 配置User-Agent和Cookie
- 更换代理服务器

### 2. 内容保存失败

可能原因：
- 磁盘空间不足
- 文件权限受限
- 路径不存在或无效
- 文件被占用
- 编码格式不匹配

解决方案：
- 定期清理临时文件
- 检查文件系统权限
- 自动创建必要目录
- 使用文件锁机制
- 统一使用UTF-8编码

### 3. 性能问题

症状：
- 爬取速度慢
- CPU使用率高
- 内存占用大
- 磁盘IO频繁

优化方案：
- 调整并发线程数
- 优化数据结构
- 使用连接池
- 实现分布式爬取

## 最佳实践

### 1. 爬虫配置

基础配置：
- 设置合理的请求间隔
- 配置请求超时时间
- 使用随机User-Agent
- 启用断点续传

高级配置：
- 实现IP代理池
- 配置Cookie管理
- 启用HTTPS证书验证
- 设置并发控制

### 2. 数据处理

数据提取：
- 使用XPath定位内容
- 处理特殊字符转义
- 清理HTML标签
- 规范化数据格式

数据存储：
- 使用事务保证一致性
- 实现增量更新
- 备份重要数据
- 压缩历史记录

### 3. 异常处理

错误恢复：
- 实现重试机制
- 记录详细日志
- 保存错误现场
- 发送异常通知

资源清理：
- 释放系统资源
- 关闭网络连接
- 删除临时文件
- 重置运行状态

## 注意事项

1. 遵守网站规则
   - 仔细阅读robots.txt
   - 控制访问频率和并发
   - 避免恶意爬取
   - 遵守版权规定

2. 资源管理
   - 实现资源池化
   - 控制内存使用
   - 定期备份数据
   - 清理过期缓存

3. 错误处理
   - 实现全局异常处理
   - 记录详细错误日志
   - 设置重试策略
   - 优雅降级和退出

4. 安全防护
   - 验证SSL证书
   - 加密敏感数据
   - 限制访问范围
   - 防止信息泄露