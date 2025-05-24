<<<<<<< HEAD
# Java Web 爬虫项目

## 项目简介

这是一个基于Java开发的网页爬虫项目，主要用于爬取网页文本和图片内容。项目包含多个爬虫实现，支持自动化爬取和内容保存功能。

## 功能特点

- 支持文本内容爬取和保存
- 支持图片内容爬取和下载
- 自动化URL遍历和内容提取
- 内置反爬虫策略和请求延迟
- 支持断点续传和错误重试

## 项目结构

```
my-java-wedcrawler/
├── src/main/java/cn/myh/java/
│   ├── TextCrawl.java          # 基础文本爬虫
│   ├── AutoTextCrawler.java    # 自动化文本爬虫
│   └── AutoImaCrawler.java     # 自动化图片爬虫
├── pom.xml                     # Maven配置文件
└── docs/                       # 项目文档
    ├── design/                 # 设计文档
    ├── user-guide/            # 用户手册
    └── dev-guide/             # 开发者指南
```

## 技术栈

- Java 8+
- JSoup：HTML解析
- Apache Commons IO：文件操作
- Maven：项目管理

## 快速开始

1. 克隆项目到本地
2. 使用Maven安装依赖：`mvn install`
3. 运行需要的爬虫类：
   - TextCrawl：基础文本爬虫示例
   - AutoTextCrawler：自动化文本爬虫
   - AutoImaCrawler：自动化图片爬虫

## 配置说明

每个爬虫类都包含以下主要配置项：

- BASE_URL：目标网站URL
- OUTPUT_DIR：输出目录
- MAX_URL_LIMIT：最大URL处理数量
- REQUEST_DELAY_MS：请求延迟时间

## 注意事项

1. 请遵守目标网站的robots.txt规则
2. 合理设置请求延迟，避免对目标服务器造成压力
3. 确保输出目录具有写入权限
4. 建议使用try-catch处理可能的异常

## 文档

详细文档请参考docs目录：

- [设计文档](docs/design/README.md)
- [用户手册](docs/user-guide/README.md)
- [开发者指南](docs/dev-guide/README.md)

## 许可证

MIT License
=======
# JavaCrawler
My java WedCrawler of CourseWork，Simple but works  
>>>>>>> cee6bd039eeff55d38cd3c90dcbf6f040ca17d02
