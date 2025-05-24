package cn.myh.java;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AutoTextCrawler {
    private static final String BASE_URL = "https://sanguo.5000yan.com/";
    private static final String OUTPUT_DIR = "D:\\新建文件夹\\";
    private static final int MAX_URL_LIMIT = 100;
    private static final int REQUEST_DELAY_MS = 200;  // 请求间隔2秒
    private static final Set<String> visitedUrls = new LinkedHashSet<>();
    private static int chapterCount = 0;

    public static void main(String[] args) {
        // 确保输出目录存在
        createOutputDirectory();
        crawlAllChapters(BASE_URL);
    }

    private static void createOutputDirectory() {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.println("无法创建目录: " + OUTPUT_DIR);
            System.exit(1);
        }
    }

    private static void crawlAllChapters(String startUrl) {
        Queue<String> urlQueue = new LinkedList<>();
        urlQueue.offer(startUrl);

        while (!urlQueue.isEmpty() && visitedUrls.size() < MAX_URL_LIMIT) {
            String currentUrl = urlQueue.poll();
            if (visitedUrls.contains(currentUrl)) continue;

            try {
                System.out.println("正在爬取: " + currentUrl);
                Document doc = Jsoup.connect(currentUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                        .header("Accept-Language", "zh-CN,zh;q=0.9")
                        .header("Referer", BASE_URL)
                        .timeout(10_000)
                        .get();

                // 标记为已访问
                visitedUrls.add(currentUrl);
                System.out.println("已处理URL数: " + visitedUrls.size() + "/" + MAX_URL_LIMIT);

                // 提取并保存章节内容
                if (currentUrl.matches(".*/(n/\\d+|\\d+)\\.html")) {  // 更宽松的URL匹配
                    crawlChapterContent(currentUrl);
                }

                // 提取新链接
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String newUrl = link.absUrl("href");
                    if (isValidUrl(newUrl) && !visitedUrls.contains(newUrl)) {
                        urlQueue.offer(newUrl);
                    }
                }

                // 请求间隔
                TimeUnit.MILLISECONDS.sleep(REQUEST_DELAY_MS);

            } catch (IOException | InterruptedException e) {
                System.err.println("处理URL失败: " + currentUrl + " | 错误: " + e.getMessage());
            }
        }
    }

    private static boolean isValidUrl(String url) {
        return url.startsWith(BASE_URL) 
                && !url.contains("javascript:")
                && !url.contains("#")
                && !url.endsWith(".css")
                && !url.endsWith(".js")
                && visitedUrls.size() < MAX_URL_LIMIT;
    }

    private static void crawlChapterContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)...")
                    .timeout(10_000)
                    .get();

            // 提取标题
            Element titleElement = doc.selectFirst("h1");
            if (titleElement == null) {
                System.err.println("未找到标题: " + url);
                return;
            }
            String title = titleElement.text().replaceAll(" - 三国演义$", "").trim();
            System.out.println("正在保存章节: " + title);

            // 提取正文
            Elements paragraphs = doc.select("div");
            if (paragraphs.isEmpty()) {
                System.err.println("未找到正文内容: " + url);
                return;
            }

            // 构建内容
            StringBuilder content = new StringBuilder();
            content.append(title).append("\n\n");
            paragraphs.forEach(p -> content.append(p.text()).append("\n\n"));

            // 生成文件名（处理非法字符）
            String safeTitle = title
                    .replaceAll("[\\\\/:*?\"<>|]", "_")
                    .replaceAll("\\s+", " ")
                    .substring(0, Math.min(title.length(), 50));
            String fileName = String.format("%03d_%s.txt", ++chapterCount, safeTitle);
            File outputFile = new File(OUTPUT_DIR + fileName);

            // 写入文件（UTF-8编码）
            FileUtils.writeStringToFile(outputFile, content.toString(), StandardCharsets.UTF_8);
            System.out.println("已保存文件: " + outputFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("保存章节失败: " + url + " | 错误: " + e.getMessage());
        }
    }
}