package cn.myh.java;

import org.apache.commons.io.FileUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoImaCrawler {
    // 配置常量
    private static final String BASE_URL = "https://www.nipic.com/";
    private static final String SAVE_DIR = "D:\\新建文件夹\\";
    private static final int MAX_URL_COUNT = 50; // 最大URL爬取数量限制
    private static final int MAX_RETRY_COUNT = 3; // 最大重试次数
    private static final int RETRY_DELAY = 200; // 重试延迟（毫秒）
    private static final int SCROLL_WAIT = 30; // 动态加载等待时间（毫秒）
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Safari/605.1.15",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edge/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36"
    };
    private static final Random random = new Random();
    private static final Map<String, Integer> imageStats = new HashMap<>();
    private static int currentUrlCount = 0;
    private static final PriorityQueue<UrlEntry> urlQueue = new PriorityQueue<>(); // URL优先级队列
    private static final Set<String> processedUrls = new HashSet<>(); // 已处理的URL集合
    private static final int MAX_MEMORY_USAGE = 100 * 1024 * 1024; // 最大内存使用限制（100MB）
    
    // URL条目类，用于优先级队列
    private static class UrlEntry implements Comparable<UrlEntry> {
        String url;
        int priority;
        long timestamp;
        
        public UrlEntry(String url, int priority) {
            this.url = url;
            this.priority = priority;
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public int compareTo(UrlEntry other) {
            int result = Integer.compare(other.priority, this.priority); // 优先级高的先处理
            if (result == 0) {
                result = Long.compare(this.timestamp, other.timestamp); // 时间戳早的先处理
            }
            return result;
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("开始自动爬取图片，起始页面：" + BASE_URL);
            // 初始化图片统计
            imageStats.put("jpg", 0);
            imageStats.put("jpeg", 0);
            imageStats.put("png", 0);
            
            // 开始URL爬取和图片下载
            crawlAndDownload(BASE_URL);
            
            // 输出最终统计信息
            printFinalStats();
        } catch (Exception e) {
            System.err.println("程序运行异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void crawlAndDownload(String baseUrl) {
        // 初始化URL队列
        urlQueue.offer(new UrlEntry(baseUrl, 10)); // 起始URL优先级最高
        String baseHost = extractHost(baseUrl);
        
        while (!urlQueue.isEmpty() && currentUrlCount < MAX_URL_COUNT) {
            // 检查内存使用情况
            if (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() > MAX_MEMORY_USAGE) {
                System.out.println("警告：内存使用接近限制，执行垃圾回收...");
                System.gc();
                try {
                    Thread.sleep(1000); // 等待GC完成
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            UrlEntry entry = urlQueue.poll();
            if (entry == null) break;
            
            String currentUrl = entry.url;
            processUrl(currentUrl, baseHost);
        }
    }

    private static void processUrl(String url, String baseHost) {
        if (processedUrls.contains(url)) {
            System.out.println("跳过已处理的URL：" + url);
            return;
        }

        int retryCount = 0;
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                System.out.println("\n处理URL：" + url + (retryCount > 0 ? " (重试次数: " + retryCount + ")" : ""));
                
                // 使用增强的连接配置
                Connection conn = Jsoup.connect(url)
                        .userAgent(getRandomUserAgent())
                        .header("Referer", BASE_URL)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .timeout(15000)
                        .maxBodySize(0);
                
                Document document = conn.get();
                
                // 等待动态加载
                Thread.sleep(SCROLL_WAIT);
                
                // 保存HTML源码
                saveHtmlToFile(document.html(), url);
                
                // 提取新的URL
                extractNewUrls(document, baseHost);
                
                // 下载图片
                downloadImages(document);
                
                // 标记URL为已处理
                processedUrls.add(url);
                
                // 反爬延迟（使用指数退避）
                int delay = 1000 + random.nextInt(2000) * (retryCount + 1);
                Thread.sleep(delay);
                
                break; // 成功处理，退出重试循环
                
            } catch (Exception e) {
                retryCount++;
                System.err.println("处理URL失败：" + url + "，错误：" + e.getMessage() + 
                    (retryCount < MAX_RETRY_COUNT ? "，将在" + RETRY_DELAY + "ms后重试" : "，已达到最大重试次数"));
                
                if (retryCount < MAX_RETRY_COUNT) {
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    private static void extractNewUrls(Document document, String baseHost) {
        Elements links = document.select("a[href]");
        for (Element link : links) {
            String newUrl = link.attr("abs:href").trim();
            if (isValidUrl(newUrl, baseHost)) {
                if (currentUrlCount >= MAX_URL_COUNT) {
                    System.out.println("已达到最大URL爬取数量限制：" + MAX_URL_COUNT);
                    return;
                }
                if (!processedUrls.contains(newUrl) && !isUrlInQueue(newUrl)) {
                    // 计算URL优先级
                    int priority = calculateUrlPriority(link);
                    urlQueue.offer(new UrlEntry(newUrl, priority));
                    currentUrlCount++;
                    System.out.println("新增URL：" + newUrl + " (优先级: " + priority + ", " + currentUrlCount + "/" + MAX_URL_COUNT + ")");
                }
            }
        }
    }
    
    private static boolean isUrlInQueue(String url) {
        return urlQueue.stream().anyMatch(entry -> entry.url.equals(url));
    }
    
    private static int calculateUrlPriority(Element link) {
        int priority = 5; // 默认优先级
        
        // 根据链接文本包含的关键词调整优先级
        String linkText = link.text().toLowerCase();
        if (linkText.contains("图片") || linkText.contains("相册") || linkText.contains("photo")) {
            priority += 3;
        }
        
        // 根据链接的位置调整优先级
        if (link.parents().size() < 3) { // 靠近页面顶部的链接
            priority += 2;
        }
        
        // 根据链接是否有缩略图调整优先级
        if (!link.select("img").isEmpty()) {
            priority += 2;
        }
        
        return Math.min(priority, 10); // 确保优先级不超过10
    }

    private static void downloadImages(Document document) {
        Elements imageElements = document.select("img");
        System.out.println("找到图片元素数量：" + imageElements.size());

        for (Element img : imageElements) {
            Set<String> imgUrls = new HashSet<>();
            String dataSrc = img.attr("data-src");
            String src = img.attr("src");
            
            if (!dataSrc.isEmpty()) imgUrls.add(dataSrc);
            if (!src.isEmpty()) imgUrls.add(src);
            
            for (String imgUrl : imgUrls) {
                processImage(imgUrl);
            }
        }
    }

    private static void processImage(String imgUrl) {
        try {
            String imgUrlLower = imgUrl.toLowerCase();
            String extension = getImageExtension(imgUrlLower);
            if (extension == null) return;

            imgUrl = processImageUrl(imgUrl);
            if (imgUrl == null || imgUrl.contains("grey.gif")) return;

            System.out.println("下载" + extension.substring(1) + "图片：" + imgUrl);
            downloadImage(imgUrl, extension);
            
            // 反爬延迟
            Thread.sleep(50 + random.nextInt(100));
            
        } catch (Exception e) {
            System.err.println("处理图片失败：" + imgUrl + "，错误：" + e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    private static String extractHost(String url) {
        try {
            Pattern pattern = Pattern.compile("(https?://)?[^/\\s]*");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group();
            }
        } catch (Exception e) {
            System.err.println("提取主机名失败：" + e.getMessage());
        }
        return url;
    }

    private static boolean isValidUrl(String url, String baseHost) {
        return url != null && !url.isEmpty() 
            && url.contains(baseHost)
            && !url.contains("#")
            && !url.contains("javascript:")
            && !url.endsWith(".jpg")
            && !url.endsWith(".jpeg")
            && !url.endsWith(".png")
            && !url.endsWith(".gif");
    }

    private static String getImageExtension(String url) {
        if (url.endsWith(".jpg")) {
            imageStats.put("jpg", imageStats.get("jpg") + 1);
            return ".jpg";
        } else if (url.endsWith(".jpeg")) {
            imageStats.put("jpeg", imageStats.get("jpeg") + 1);
            return ".jpeg";
        } else if (url.endsWith(".png")) {
            imageStats.put("png", imageStats.get("png") + 1);
            return ".png";
        }
        return null;
    }

    private static void saveHtmlToFile(String htmlContent, String url) {
        try {
            String fileName = "page_" + UUID.randomUUID().toString().substring(0, 8) + ".html";
            File saveDir = new File(SAVE_DIR);
            if (!saveDir.exists()) saveDir.mkdirs();
            
            File outputFile = new File(SAVE_DIR + fileName);
            FileUtils.writeStringToFile(outputFile, htmlContent, StandardCharsets.UTF_8);
            System.out.println("保存HTML源码：" + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("保存HTML失败：" + url + "，错误：" + e.getMessage());
        }
    }

    private static String processImageUrl(String imgUrl) {
        if (imgUrl.startsWith("//")) {
            return "https:" + imgUrl;
        } else if (imgUrl.startsWith("/")) {
            return BASE_URL + imgUrl.substring(1);
        } else if (!imgUrl.startsWith("http")) {
            return "https://" + imgUrl;
        }
        return imgUrl;
    }

    private static void downloadImage(String imgUrl, String extension) throws IOException {
        int retryCount = 0;
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                Connection.Response response = Jsoup.connect(imgUrl)
                        .userAgent(getRandomUserAgent())
                        .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                        .header("Accept-Encoding", "gzip, deflate, br")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .ignoreContentType(true)
                        .timeout(5000 * (retryCount + 1)) // 递增超时时间
                        .maxBodySize(10 * 1024 * 1024) // 限制图片大小为10MB
                        .execute();

                // 验证响应内容类型
                String contentType = response.contentType();
                if (contentType != null && !contentType.toLowerCase().startsWith("image/")) {
                    throw new IOException("无效的图片内容类型：" + contentType);
                }

                String fileName = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
                File outputFile = new File(SAVE_DIR + fileName + extension);
                
                // 使用try-with-resources确保资源正确关闭
                try (ByteArrayInputStream bis = new ByteArrayInputStream(response.bodyAsBytes())) {
                    FileUtils.copyInputStreamToFile(bis, outputFile);
                }
                
                // 验证文件大小
                if (outputFile.length() < 100) { // 小于100字节可能是无效图片
                    outputFile.delete();
                    throw new IOException("下载的图片文件过小，可能是无效图片");
                }
                
                System.out.println("成功下载图片：" + outputFile.getAbsolutePath());
                break; // 成功下载，退出重试循环
                
            } catch (IOException e) {
                retryCount++;
                System.err.println("下载图片失败：" + imgUrl + "，错误：" + e.getMessage() +
                    (retryCount < MAX_RETRY_COUNT ? "，将在" + RETRY_DELAY + "ms后重试" : "，已达到最大重试次数"));
                
                if (retryCount < MAX_RETRY_COUNT) {
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    throw e; // 重试失败，抛出最后一次的异常
                }
            }
        }
    }

    private static String getRandomUserAgent() {
        return USER_AGENTS[random.nextInt(USER_AGENTS.length)];
    }

    private static void printFinalStats() {
        int totalImages = imageStats.values().stream().mapToInt(Integer::intValue).sum();
        System.out.println("\n爬取完成！统计信息：");
        System.out.println("总计爬取URL数量：" + currentUrlCount);
        System.out.println("总计下载图片数量：" + totalImages);
        System.out.println("图片类型统计：");
        System.out.println("- JPG图片：" + imageStats.get("jpg") + "张");
        System.out.println("- JPEG图片：" + imageStats.get("jpeg") + "张");
        System.out.println("- PNG图片：" + imageStats.get("png") + "张");
    }
}