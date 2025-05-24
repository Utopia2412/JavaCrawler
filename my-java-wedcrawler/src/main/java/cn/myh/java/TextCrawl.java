package cn.myh.java;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Random;
//https://sanguo.5000yan.com/    demowite
public class TextCrawl {
    private static String url = "https://sanguo.5000yan.com/";
    private static final Random random = new Random();

    public static void main(String[] args) {
        jsoup();
    }

    private static void jsoup() {
        try {
            System.out.println("开始爬取网页：" + url);
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                    .get();

            // 提取网页标题
            String title = document.title();
            System.out.println("页面标题：" + title);

            // 提取正文内容
            Elements contentElements = document.select("div");
            if (contentElements.isEmpty()) {
                System.out.println("未找到正文内容");
                return;
            }

            StringBuilder content = new StringBuilder();
            content.append("标题：").append(title).append("\n\n");

            // 提取正文段落
            for (Element element : contentElements) {
                String text = element.text().trim();
                if (!text.isEmpty()) {
                    content.append(text).append("\n\n");
                }
            }

            // 生成文件名
            String randomNum = String.format("%04d", random.nextInt(10000));
            // 从content中提取前10个字符作为文件名前缀
            String prefix = content.toString().replaceAll("[\\\\/:*?\"<>|]", "").substring(0, Math.min(10, content.length()));
            String fileName = prefix + "_" + randomNum + ".txt";
            File outputFile = new File("D:\\新建文件夹\\" + fileName);

            // 确保目录存在
            outputFile.getParentFile().mkdirs();

            // 写入文件
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(content.toString());
                System.out.println("成功保存文本内容到：" + outputFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("保存文件失败：" + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("爬取过程发生错误：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}