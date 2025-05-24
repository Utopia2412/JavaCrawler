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
import org.jfree.data.general.DefaultPieDataset;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.GridLayout;
import org.jfree.chart.ChartPanel;

public class EnhancedDataCrawler {
    // ==================== 配置参数 ====================
    private static final String BASE_URL = "https://movie.douban.com/top250?start=";
    private static final String MOVIE_DETAIL_URL = "https://movie.douban.com/subject/%s/";
    private static final String JDBC_URL = "jdbc:h2:file:./data/enhanced_douban_db";
    private static final String OUTPUT_DIR = "D://新建文件夹//enhanced//";
    private static final String REPORT_PATH = OUTPUT_DIR + "enhanced_douban_report.txt";
    private static final int MAX_RETRY = 3;
    private static List<String> userAgents = new ArrayList<>();
    private static Random random = new Random();

    // ==================== 增强版电影实体类 ====================
    private static class EnhancedMovie {
        String title;           // 电影标题
        String originalTitle;   // 原始标题
        double rating;         // 评分
        int votes;             // 评价人数
        String year;           // 上映年份
        String directors;      // 导演
        String screenwriters;  // 编剧
        String actors;         // 主演
        String genres;         // 类型
        String countries;      // 制片国家/地区
        String languages;      // 语言
        String runtime;        // 片长
        String imdbId;         // IMDb链接
        String summary;        // 剧情简介
        String awards;         // 获奖情况

        public EnhancedMovie() {}

        @Override
        public String toString() {
            return String.format("%s (%s) - %.1f分 [%s]\n导演: %s\n主演: %s\n类型: %s\n地区: %s\n年份: %s",
                    title, originalTitle, rating, genres, directors, actors, genres, countries, year);
        }
    }

    // ==================== Main 入口 ====================
    public static void main(String[] args) {
        initUserAgents();
        createOutputDir();
        List<EnhancedMovie> movies = crawlTop250();
        saveToDatabase(movies);
        showOptimizedChart(movies);
        generateTextReport(movies);
    }

    // ==================== 核心方法 ====================
    private static void initUserAgents() {
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Safari/605.1.15");
    }

    private static void createOutputDir() {
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR));
            System.out.println("[SUCCESS] 输出目录已创建: " + OUTPUT_DIR);
        } catch (IOException e) {
            System.err.println("[ERROR] 目录创建失败: " + e.getMessage());
        }
    }

    private static List<EnhancedMovie> crawlTop250() {
        List<EnhancedMovie> movies = new ArrayList<>();
        try {
            for (int page = 0; page < 250; page += 25) {
                int currentPage = (page / 25) + 1;
                System.out.printf("[PROGRESS] 正在抓取第 %d 页（start=%d）%n", currentPage, page);

                String html = fetchPageWithRetry(BASE_URL + page);
                if (html == null) continue;

                List<String> movieIds = parseMovieIds(html);
                for (String movieId : movieIds) {
                    String detailUrl = String.format(MOVIE_DETAIL_URL, movieId);
                    String detailHtml = fetchPageWithRetry(detailUrl);
                    if (detailHtml != null) {
                        EnhancedMovie movie = parseMovieDetail(detailHtml);
                        if (movie != null) {
                            movies.add(movie);
                            System.out.printf("[SUCCESS] 已抓取电影: %s%n", movie.title);
                        }
                    }
                    Thread.sleep(1000 + random.nextInt(2000)); // 反爬延迟
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return movies;
    }

    private static List<String> parseMovieIds(String html) {
        List<String> movieIds = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        Elements items = doc.select(".item .info .hd a");
        for (Element item : items) {
            String href = item.attr("href");
            String movieId = href.replaceAll(".*/subject/(\\d+)/.*", "$1");
            movieIds.add(movieId);
        }
        return movieIds;
    }

    private static EnhancedMovie parseMovieDetail(String html) {
        try {
            Document doc = Jsoup.parse(html);
            EnhancedMovie movie = new EnhancedMovie();

            // 解析基本信息
            movie.title = Optional.ofNullable(doc.selectFirst("h1 span[property='v:itemreviewed']"))
                    .map(Element::text).orElse("未知标题");
            movie.rating = Optional.ofNullable(doc.selectFirst(".rating_self_r strong[property='v:average']"))
                    .map(e -> Double.parseDouble(e.text())).orElse(0.0);
            movie.votes = Optional.ofNullable(doc.selectFirst(".rating_people span[property='v:votes']"))
                    .map(e -> Integer.parseInt(e.text())).orElse(0);

            // 解析详细信息
            Element info = doc.selectFirst("#info");
            if (info != null) {
                String infoText = info.text();
                movie.originalTitle = extractInfo(infoText, "原名:", "\n");
                movie.directors = extractInfo(infoText, "导演:", "编剧:");
                movie.screenwriters = extractInfo(infoText, "编剧:", "主演:");
                movie.actors = extractInfo(infoText, "主演:", "类型:");
                movie.genres = extractInfo(infoText, "类型:", "制片国家/地区:");
                movie.countries = extractInfo(infoText, "制片国家/地区:", "语言:");
                movie.languages = extractInfo(infoText, "语言:", "上映日期:");
                movie.runtime = extractInfo(infoText, "片长:", "又名:");
                movie.imdbId = extractInfo(infoText, "IMDb:", "\n");
                // 提取年份信息
                Pattern yearPattern = Pattern.compile("(\\d{4})");
                Matcher yearMatcher = yearPattern.matcher(infoText);
                movie.year = yearMatcher.find() ? yearMatcher.group(1) : null;
            }

            // 解析剧情简介
            movie.summary = Optional.ofNullable(doc.selectFirst("[property='v:summary']"))
                    .map(Element::text)
                    .map(s -> s.trim())
                    .orElse("");

            // 解析获奖情况
            Elements awards = doc.select(".mod");
            if (!awards.isEmpty()) {
                movie.awards = awards.stream()
                        .map(Element::text)
                        .collect(Collectors.joining("\n"));
            }

            return movie;
        } catch (Exception e) {
            System.err.println("[ERROR] 解析电影详情失败: " + e.getMessage());
            return null;
        }
    }

    private static String extractInfo(String text, String startMark, String endMark) {
        try {
            int start = text.indexOf(startMark) + startMark.length();
            int end = text.indexOf(endMark, start);
            return end > start ? text.substring(start, end).trim() : "";
        } catch (Exception e) {
            return "";
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
    private static void saveToDatabase(List<EnhancedMovie> movies) {
        System.out.println("[INFO] 开始存储数据到数据库...");
        long startTime = System.currentTimeMillis();

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);
            
            // 创建增强版电影表
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS enhanced_movies (" +
                    "title VARCHAR(255), " +
                    "original_title VARCHAR(255), " +
                    "rating DOUBLE, " +
                    "votes INTEGER, " +
                    "movie_year VARCHAR(4), " +
                    "directors VARCHAR(500), " +
                    "screenwriters VARCHAR(500), " +
                    "actors VARCHAR(1000), " +
                    "genres VARCHAR(255), " +
                    "countries VARCHAR(255), " +
                    "languages VARCHAR(255), " +
                    "runtime VARCHAR(50), " +
                    "imdb_id VARCHAR(50), " +
                    "summary CLOB, " +
                    "awards CLOB)");

            // 准备插入语句
            String sql = "INSERT INTO enhanced_movies VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (EnhancedMovie movie : movies) {
                    pstmt.setString(1, movie.title);
                    pstmt.setString(2, movie.originalTitle);
                    pstmt.setDouble(3, movie.rating);
                    pstmt.setInt(4, movie.votes);
                    pstmt.setString(5, movie.year != null ? movie.year.replaceAll("[^0-9]", "") : null);
                    pstmt.setString(6, movie.directors);
                    pstmt.setString(7, movie.screenwriters);
                    pstmt.setString(8, movie.actors);
                    pstmt.setString(9, movie.genres);
                    pstmt.setString(10, movie.countries);
                    pstmt.setString(11, movie.languages);
                    pstmt.setString(12, movie.runtime);
                    pstmt.setString(13, movie.imdbId);
                    pstmt.setString(14, movie.summary);
                    pstmt.setString(15, movie.awards);
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
    private static void showOptimizedChart(List<EnhancedMovie> movies) {
        // 创建多个图表展示不同维度的数据
        JFrame frame = new JFrame("豆瓣Top250电影统计分析");
        frame.setLayout(new GridLayout(2, 2));

        // 1. 年代分布
        DefaultCategoryDataset decadeDataset = new DefaultCategoryDataset();
        movies.stream()
                .filter(m -> m.year != null && !m.year.isEmpty())
                .collect(Collectors.groupingBy(
                        m -> m.year.substring(0, 3) + "0s",
                        TreeMap::new,
                        Collectors.counting()
                ))
                .forEach((k, v) -> decadeDataset.addValue(v, "数量", k));

        JFreeChart decadeChart = ChartFactory.createBarChart(
                "年代分布", "年代", "电影数量", decadeDataset
        );
        frame.add(new ChartPanel(decadeChart));

        // 2. 评分分布
        DefaultCategoryDataset ratingDataset = new DefaultCategoryDataset();
        movies.stream()
                .collect(Collectors.groupingBy(
                        m -> String.format("%.1f", Math.floor(m.rating * 2) / 2),
                        TreeMap::new,
                        Collectors.counting()
                ))
                .forEach((k, v) -> ratingDataset.addValue(v, "数量", k));

        JFreeChart ratingChart = ChartFactory.createBarChart(
                "评分分布", "评分", "电影数量", ratingDataset
        );
        frame.add(new ChartPanel(ratingChart));

        // 3. 类型分布
        DefaultPieDataset genreDataset = new DefaultPieDataset();
        movies.stream()
                .filter(m -> m.genres != null && !m.genres.isEmpty())
                .flatMap(m -> Arrays.stream(m.genres.split(" ")))
                .collect(Collectors.groupingBy(
                        genre -> genre,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .forEach(e -> genreDataset.setValue(e.getKey(), e.getValue()));

        JFreeChart genreChart = ChartFactory.createPieChart(
                "Top10类型分布", genreDataset, true, true, false
        );
        frame.add(new ChartPanel(genreChart));

        // 4. 国家/地区分布
        DefaultPieDataset countryDataset = new DefaultPieDataset();
        movies.stream()
                .filter(m -> m.countries != null && !m.countries.isEmpty())
                .flatMap(m -> Arrays.stream(m.countries.split(" ")))
                .collect(Collectors.groupingBy(
                        country -> country,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .forEach(e -> countryDataset.setValue(e.getKey(), e.getValue()));

        JFreeChart countryChart = ChartFactory.createPieChart(
                "Top10国家/地区分布", countryDataset, true, true, false
        );
        frame.add(new ChartPanel(countryChart));

        // 设置窗口属性
        frame.setSize(1200, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        System.out.println("[INFO] 统计图表窗口已打开");
    }

    // ==================== 文本报告模块 ====================
    private static void generateTextReport(List<EnhancedMovie> movies) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(REPORT_PATH))) {
            writer.write("=============== 豆瓣电影Top250增强版分析报告 ===============\n");
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

            // 年代分布统计
            writer.write("\n【年代分布】\n");
            Map<String, Long> decadeDistribution = movies.stream()
                    .filter(m -> m.year != null && !m.year.isEmpty())
                    .collect(Collectors.groupingBy(
                            m -> m.year.substring(0, 3) + "0s",
                            TreeMap::new,
                            Collectors.counting()
                    ));
            decadeDistribution.forEach((k, v) -> writeFormattedLine(writer, k, v));

            // 类型分布统计
            writer.write("\n【电影类型分布】\n");
            Map<String, Long> genreDistribution = movies.stream()
                    .filter(m -> m.genres != null && !m.genres.isEmpty())
                    .flatMap(m -> Arrays.stream(m.genres.split(" ")))
                    .collect(Collectors.groupingBy(
                            genre -> genre,
                            TreeMap::new,
                            Collectors.counting()
                    ));
            genreDistribution.forEach((k, v) -> writeFormattedLine(writer, k, v));

            // 国家/地区分布统计
            writer.write("\n【制片国家/地区分布】\n");
            Map<String, Long> countryDistribution = movies.stream()
                    .filter(m -> m.countries != null && !m.countries.isEmpty())
                    .flatMap(m -> Arrays.stream(m.countries.split(" ")))
                    .collect(Collectors.groupingBy(
                            country -> country,
                            TreeMap::new,
                            Collectors.counting()
                    ));
            countryDistribution.forEach((k, v) -> writeFormattedLine(writer, k, v));

            System.out.println("[SUCCESS] 增强版文本报告已生成: " + REPORT_PATH);
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