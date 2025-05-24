# 开发者指南

## 代码结构

### 项目组织

```
src/main/java/cn/myh/java/
├── TextCrawl.java          # 基础文本爬虫实现
├── AutoTextCrawler.java    # 自动化文本爬虫实现
└── AutoImaCrawler.java     # 自动化图片爬虫实现
```

## 核心类说明

### 1. TextCrawl

基础文本爬虫类，实现单页面的文本内容爬取：

主要方法：
- `main()`：程序入口
- `jsoup()`：执行爬取逻辑，包含以下步骤：
  - 连接目标网页
  - 提取页面标题
  - 提取正文内容
  - 保存到文件

### 2. AutoTextCrawler

自动化文本爬虫类，支持自动遍历网站并爬取文本内容：

主要方法：
- `createOutputDirectory()`：创建输出目录
- `crawlAllChapters()`：遍历并爬取所有章节
- `isValidUrl()`：验证URL有效性
- `crawlChapterContent()`：爬取章节内容

关键特性：
- URL去重处理
- 请求延迟控制
- 错误重试机制

### 3. AutoImaCrawler

自动化图片爬虫类，专门用于图片内容的爬取：

主要方法：
- `crawlAndDownload()`：爬取和下载图片
- `processUrl()`：处理单个URL
- `extractNewUrls()`：提取新URL
- `downloadImages()`：下载图片文件

特色功能：
- URL优先级队列
- 内存使用监控
- 图片格式统计

## 开发规范

### 1. 代码风格

- 使用4空格缩进
- 类名使用PascalCase
- 方法名使用camelCase
- 常量使用UPPER_SNAKE_CASE

### 2. 异常处理

```java
try {
    // 网络请求、文件操作等
} catch (IOException e) {
    System.err.println("错误信息: " + e.getMessage());
    // 可选：记录日志、重试操作
}
```

### 3. 配置管理

- 使用静态常量定义配置项
- 关键参数提供默认值
- 考虑配置的可扩展性

### 4. 日志处理

- 使用System.out输出正常信息
- 使用System.err输出错误信息
- 记录关键操作节点

## 扩展开发

### 1. 添加新爬虫

1. 创建新的爬虫类
2. 实现必要的配置项
3. 编写核心爬取逻辑
4. 添加错误处理机制

示例框架：
```java
public class NewCrawler {
    // 配置项
    private static final String BASE_URL = "目标网站URL";
    private static final String OUTPUT_DIR = "输出目录";
    
    // 核心方法
    public static void main(String[] args) {
        // 初始化操作
        // 执行爬取逻辑
    }
    
    private static void crawl() {
        // 实现爬取逻辑
    }
}
```

### 2. 功能扩展

1. URL处理扩展
   - 自定义URL过滤规则
   - 实现优先级处理
   - 添加URL分类逻辑

2. 内容处理扩展
   - 自定义内容提取规则
   - 添加数据清洗逻辑
   - 实现特殊格式处理

3. 存储方式扩展
   - 添加数据库支持
   - 实现分布式存储
   - 支持云存储服务

## 测试指南

### 1. 单元测试

- 测试URL验证逻辑
- 测试内容提取功能
- 测试文件保存操作

### 2. 集成测试

- 测试完整爬取流程
- 验证错误处理机制
- 检查资源释放情况

### 3. 性能测试

- 测试内存使用情况
- 验证并发处理能力
- 检查网络请求效率

## 部署说明

### 1. 环境配置

- 安装Java运行环境
- 配置Maven环境
- 设置系统环境变量

### 2. 打包部署

```bash
# 打包
mvn clean package

# 运行
java -jar target/my-java-wedcrawler-1.0.jar
```

### 3. 运行维护

- 定期检查日志
- 监控系统资源
- 及时更新配置

## 常见问题

### 1. 开发问题

- 代码编译错误
  - 检查依赖配置
  - 验证Java版本
  - 确认代码语法

- 运行时异常
  - 检查配置参数
  - 验证运行环境
  - 查看错误日志

### 2. 部署问题

- 环境配置
  - 确认Java版本
  - 检查Maven设置
  - 验证系统权限

- 运行维护
  - 监控系统资源
  - 检查日志记录
  - 及时处理异常