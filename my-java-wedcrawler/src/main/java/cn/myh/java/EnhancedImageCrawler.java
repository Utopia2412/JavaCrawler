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
import java.util.Random;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/* 1，"https://www.nipic.com/topic/show_29204_1.html"
 * 2."https://www.nipic.com/media/shipai/dongzhi/index.html"
 * 3."https://www.nipic.com/media/bishua/shuimo/index.html"
 * 4."https://www.nipic.com/photo/renwu/nvxing/index.html"
 * 5."https://www.nipic.com/topic/show_27075_1.html"
 * 
 * 
*/


public class EnhancedImageCrawler {
    // 配置常量
    private static final String TARGET_URL = "https://www.nipic.com/topic/show_27075_1.html";
    private static final String SAVE_DIR = "D:\\新建文件夹\\";
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Safari/605.1.15"
    };
    private static final Random random = new Random();
    private static final Map<String, Integer> imageStats = new HashMap<>();

    public static void main(String[] args) {
        try {
            System.out.println("开始爬取网页：" + TARGET_URL);
            Document document = Jsoup.connect(TARGET_URL)
                    .userAgent(getRandomUserAgent())
                    .header("Referer", "https://www.nipic.com/")
                    .timeout(15000)
                    .get();

            // 保存HTML源码
            String htmlFileName = "page_source_" + UUID.randomUUID() + ".txt";
            saveHtmlToFile(document.html(), htmlFileName);
            System.out.println("网页源码已保存至: " + SAVE_DIR + htmlFileName);

            // 初始化图片统计
            imageStats.put("jpg", 0);
            imageStats.put("jpeg", 0);
            imageStats.put("png", 0);
            int successCount = 0;

            // 查找所有图片容器
            Elements imageContainers = document.select("li");
            System.out.println("找到图片容器数量：" + imageContainers.size());

            for (Element container : imageContainers) {
                Element imgElement = container.selectFirst("img");
                if (imgElement == null) continue;

                // 获取所有可能的图片URL
                Set<String> imgUrls = new HashSet<>();
                String dataSrc = imgElement.attr("data-src");
                String src = imgElement.attr("src");
                
                if (!dataSrc.isEmpty()) imgUrls.add(dataSrc);
                if (!src.isEmpty()) imgUrls.add(src);
                
                if (imgUrls.isEmpty()) {
                    System.out.println("跳过：未找到有效的图片链接");
                    continue;
                }
                
                // 处理所有找到的图片URL
                for (String imgUrl : imgUrls) {
                    System.out.println("发现图片链接：" + imgUrl);
                    String imgUrlLower = imgUrl.toLowerCase();
                    String extension = null;

                    if (imgUrlLower.endsWith(".jpg") || imgUrlLower.endsWith(".jpeg") || imgUrlLower.endsWith(".png")) {
                        if (imgUrlLower.endsWith(".jpg")) {
                            extension = ".jpg";
                            imageStats.put("jpg", imageStats.get("jpg") + 1);
                        } else if (imgUrlLower.endsWith(".jpeg")) {
                            extension = ".jpeg";
                            imageStats.put("jpeg", imageStats.get("jpeg") + 1);
                        } else {
                            extension = ".png";
                            imageStats.put("png", imageStats.get("png") + 1);
                        }
                    } else {
                        System.out.println("跳过不支持的图片格式：" + imgUrl);
                        continue;
                    }

                    // 处理URL格式
                    imgUrl = processImageUrl(imgUrl);
                    if (imgUrl == null || imgUrl.contains("grey.gif")) {
                        System.out.println("跳过无效图片URL: " + imgUrl);
                        continue;
                    }

                    System.out.println("正在下载" + extension.substring(1) + "图片：" + imgUrl);
                    try {
                        downloadImage(imgUrl, extension);
                        successCount++;
                    } catch (IOException e) {
                        System.err.println("下载图片失败：" + imgUrl + "，错误信息：" + e.getMessage());
                    }
                    
                    // 反爬延迟
                    Thread.sleep(50 + random.nextInt(100));
                }
            }

            // 输出统计信息
            System.out.println("爬取完成！成功下载" + successCount + "张图片");
            System.out.println("图片统计信息：");
            System.out.println("- JPG图片：" + imageStats.get("jpg") + "张");
            System.out.println("- JPEG图片：" + imageStats.get("jpeg") + "张");
            System.out.println("- PNG图片：" + imageStats.get("png") + "张");
            System.out.println("页面图片容器总数：" + imageContainers.size());

        } catch (IOException | InterruptedException e) {
            System.err.println("[错误] 程序运行异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

  
    
    /**
     * 保存网页源码到文本文件
     */
    private static void saveHtmlToFile(String htmlContent, String fileName) throws IOException {
        File saveDir = new File(SAVE_DIR);
        if (!saveDir.exists()) saveDir.mkdirs();
        
        File outputFile = new File(SAVE_DIR + fileName);
        FileUtils.writeStringToFile(outputFile, htmlContent, StandardCharsets.UTF_8);
    }

    /**
     * 处理图片URL格式
     */
    private static String processImageUrl(String imgUrl) {
        if (imgUrl.startsWith("//")) {
            return "https:" + imgUrl;
        } else if (imgUrl.startsWith("/")) {
            return "https://www.nipic.com" + imgUrl;
        } else if (!imgUrl.startsWith("http")) {
            return "https://" + imgUrl;
        }
        return imgUrl;
    }

    /**
     * 下载图片到本地
     */
    private static void downloadImage(String imgUrl, String extension) throws IOException {
        Connection.Response response = Jsoup.connect(imgUrl)
                .userAgent(getRandomUserAgent())
                .ignoreContentType(true)
                .timeout(5000)
                .execute();

        String fileName = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(10);
        File outputFile = new File(SAVE_DIR + fileName + extension);
        FileUtils.copyInputStreamToFile(
            new ByteArrayInputStream(response.bodyAsBytes()),
            outputFile
        );
        System.out.println("成功下载图片：" + outputFile.getAbsolutePath());
    }

    /**
     * 获取随机User-Agent
     */
    private static String getRandomUserAgent() {
        return USER_AGENTS[random.nextInt(USER_AGENTS.length)];
    }
}