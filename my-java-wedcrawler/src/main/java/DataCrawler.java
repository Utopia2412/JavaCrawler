import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class DataCrawler {

    // ==================== 配置参数 ====================
    private static final String BASE_URL = "https://movie.douban.com/top250?start=";
    private static final String JDBC_URL = "jdbc:h2:file:./data/douban_db"; // 数据库保存路径
    private static final String OUTPUT_DIR = "D://新建文件夹//";
    private static final String REPORT_PATH = OUTPUT_DIR + "douban_report.txt";
    private static final int MAX_RETRY = 3;
    private static List<String> userAgents = new ArrayList<>();
    private static Random random = new Random();

    // ==================== 电影实体类 ====================
    private static class Movie {
        String title;
        double rating;
        int votes;

        public Movie(String title, double rating, int votes) {
            this.title = title;
            this.rating = rating;
            this.votes = votes;
        }
    }

    // ==================== Main 入口 ====================
    public static void main(String[] args) {
        initUserAgents();
        createOutputDir();
        List<Movie> movies = crawlTop250();
        saveToDatabase(movies);
        showOptimizedChart(movies);
        generateTextReport(movies);
    }

    // ==================== 核心方法 ====================

    // 初始化 User-Agent 池
    private static void initUserAgents() {
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Safari/605.1.15");
    }

    // 创建输出目录
    private static void createOutputDir() {
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR));
            System.out.println("[SUCCESS] 输出目录已创建: " + OUTPUT_DIR);
        } catch (IOException e) {
            System.err.println("[ERROR] 目录创建失败: " + e.getMessage());
        }
    }

    // 抓取豆瓣 Top250 数据（核心逻辑）
    private static List<Movie> crawlTop250() {
        List<Movie> movies = new ArrayList<>();
        try {
            for (int page = 0; page < 250; page += 25) {
                int currentPage = (page / 25) + 1;
                System.out.printf("[PROGRESS] 正在抓取第 %d 页（start=%d）%n", currentPage, page);

                String html = fetchPageWithRetry(BASE_URL + page);
                if (html == null) continue;

                parseHtmlToMovies(html, movies);
                Thread.sleep(1000 + random.nextInt(2000)); // 反爬延迟
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return movies;
    }

    // 解析 HTML 并填充 movies 列表
    private static void parseHtmlToMovies(String html, List<Movie> movies) {
        Document doc = Jsoup.parse(html);
        Elements items = doc.select("div.item");
        for (Element item : items) {
            try {
                String title = Optional.ofNullable(item.selectFirst("span.title"))
                        .map(Element::text).orElse("未知标题");

                double rating = Optional.ofNullable(item.selectFirst("span.rating_num"))
                        .map(e -> Double.parseDouble(e.text())).orElse(0.0);

                int votes = Optional.ofNullable(item.selectFirst("span[property=v\\:votes]"))
                        .map(e -> Integer.parseInt(e.text().replaceAll("\\D+", ""))).orElse(0);

                movies.add(new Movie(title, rating, votes));
            } catch (Exception e) {
                System.err.println("[ERROR] 解析条目失败: " + e.getMessage());
            }
        }
    }

    // ==================== 网络请求模块 ====================
    private static String fetchPageWithRetry(String url) {
        for (int retry = 1; retry <= MAX_RETRY; retry++) {
            try {
                String result = fetchPage(url);
                System.out.printf("[DEBUG] 请求成功: %s (第 %d 次尝试)%n", url, retry);
                return result;
            } catch (IOException e) {
                handleRetryError(url, retry, e);
            }
        }
        return null;
    }

    private static String fetchPage(String url) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", userAgents.get(random.nextInt(userAgents.size())));
        request.setHeader("Referer", "https://www.douban.com/");

        try (CloseableHttpResponse response = client.execute(request)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("HTTP状态码异常: " + response.getStatusLine().getStatusCode());
            }
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        }
    }

    private static void handleRetryError(String url, int retry, IOException e) {
        System.err.printf("[WARN] 请求失败: %s (第 %d 次重试)，原因: %s%n", url, retry, e.getMessage());
        if (retry == MAX_RETRY) {
            System.err.printf("[ERROR] 已达到最大重试次数 (%d)%n", MAX_RETRY);
            return;
        }
        try {
            Thread.sleep(3000 * retry); // 指数退避
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== 数据存储模块 ====================
    private static void saveToDatabase(List<Movie> movies) {
        System.out.println("[INFO] 开始存储数据到数据库...");
        long startTime = System.currentTimeMillis();

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS movies (title VARCHAR(255), rating DOUBLE, votes INT)");

            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO movies VALUES (?, ?, ?)")) {
                for (Movie movie : movies) {
                    pstmt.setString(1, movie.title);
                    pstmt.setDouble(2, movie.rating);
                    pstmt.setInt(3, movie.votes);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit();
                System.out.printf("[SUCCESS] 数据存储完成，耗时 %dms，插入 %d 条数据%n",
                        (System.currentTimeMillis() - startTime), movies.size());
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 数据库操作失败: " + e.getMessage());
        }
    }

    // ==================== 可视化模块 ====================
    private static void showOptimizedChart(List<Movie> movies) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // 按0.5分间隔聚合数据
        movies.stream()
                .collect(Collectors.groupingBy(
                        m -> String.format("%.1f-%.1f", Math.floor(m.rating * 2)/2, Math.floor(m.rating * 2)/2 + 0.5),
                        TreeMap::new,
                        Collectors.counting()
                ))
                .forEach((k, v) -> dataset.addValue(v, "数量", k));

        JFreeChart chart = ChartFactory.createBarChart(
                "豆瓣Top250评分分布（0.5分间隔）", // 标题
                "评分区间", 
                "电影数量", 
                dataset
        );

        ChartFrame frame = new ChartFrame("统计结果", chart);
        frame.pack();
        frame.setVisible(true);
        System.out.println("[INFO] 图表窗口已打开");
    }

    // ==================== 文本报告模块 ====================
    private static void generateTextReport(List<Movie> movies) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(REPORT_PATH))) {
            writer.write("=============== 豆瓣电影Top250分析报告 ===============\n");
            writer.write(String.format("数据总量: %d 部\n\n", movies.size()));

            // 评分分布统计
            writer.write("【评分区间分布】\n");
            Map<String, Long> ratingDistribution = movies.stream()
                    .collect(Collectors.groupingBy(
                            m -> String.format("%.1f-%.1f", Math.floor(m.rating * 2)/2, Math.floor(m.rating * 2)/2 + 0.5),
                            TreeMap::new,
                            Collectors.counting()
                    ));
            ratingDistribution.forEach((k, v) -> writeFormattedLine(writer, k, v));

            // 数据质量分析
            writer.write("\n【数据质量】\n");
            long missingTitle = movies.stream().filter(m -> m.title.equals("未知标题")).count();
            writeFormattedLine(writer, "标题缺失记录", missingTitle);

            System.out.println("[SUCCESS] 文本报告已生成: " + REPORT_PATH);
        } catch (IOException e) {
            System.err.println("[ERROR] 文件写入失败: " + e.getMessage());
        }
    }

    private static void writeFormattedLine(BufferedWriter writer, String key, long value) {
        try {
            writer.write(String.format("▌ %-15s : %4d 条\n", key, value));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}