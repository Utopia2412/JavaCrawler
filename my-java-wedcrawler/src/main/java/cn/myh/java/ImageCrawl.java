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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ImageCrawl {

    //private static String url = "https://www.nipic.com/topic/show_29204_1.html";
    private static String url = "https://www.nipic.com/media/shipai/dongzhi/index.html";

    public static void main(String[] args) {
        jsoup();
    }

    private static void jsoup() {
        try {
            System.out.println("开始爬取网页：" + url);
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                    .get();

            // 优化选择器，查找所有 img 标签
            Elements select = document.select("img");
            System.out.println("找到图片元素数量（所有img）:" + select.size());

            Map<String, Integer> imageStats = new HashMap<>();
            imageStats.put("jpg", 0);
            imageStats.put("jpeg", 0);
            imageStats.put("png", 0);
            int successCount = 0;
            
            for (Element element : select) {
                String imgUrl = element.attr("src");
                if (imgUrl.isEmpty()) {
                    imgUrl = element.attr("data-src");
                }
                if (imgUrl.isEmpty()) {
                    imgUrl = element.attr("data-original");
                }
                System.out.println("发现图片链接：" + imgUrl);
                if (imgUrl.isEmpty()) {
                    System.out.println("跳过空图片链接");
                    continue;
                }
                
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
                
                if (imgUrl.startsWith("//")) {
                    imgUrl = "https:" + imgUrl;
                } else if (!imgUrl.startsWith("http")) {
                    imgUrl = "https://" + imgUrl;
                }
                
                System.out.println("正在下载" + extension.substring(1) + "图片：" + imgUrl);
                try {
                    Connection.Response response = Jsoup.connect(imgUrl)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                            .ignoreContentType(true)
                            .timeout(5000)
                            .execute();

                    String fileName = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(10);
                    File outputFile = new File("D:\\新建文件夹\\" + fileName + extension);
                    FileUtils.copyInputStreamToFile(new ByteArrayInputStream(response.bodyAsBytes()), outputFile);
                    successCount++;
                    System.out.println("成功下载图片：" + outputFile.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("下载图片失败：" + imgUrl + "，错误信息：" + e.getMessage());
                }
            }
            
            System.out.println("爬取完成！成功下载" + successCount + "张图片");
            System.out.println("图片统计信息：");
            System.out.println("- JPG图片：" + imageStats.get("jpg") + "张");
            System.out.println("- JPEG图片：" + imageStats.get("jpeg") + "张");
            System.out.println("- PNG图片：" + imageStats.get("png") + "张");
            System.out.println("页面img标签总数：" + select.size());

        } catch (IOException e) {
            System.err.println("爬取过程发生错误：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }


}
