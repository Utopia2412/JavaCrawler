package cn.myh.java;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*URL池存储链接*/
public class UrlPool {
    public static void main(String[] args) {
        getUrl("https://www.nipic.com/"); // 爬取首页地址
    }

    private static void getUrl(String baseUrl) {
        Map<String, Boolean> oldMap = new LinkedHashMap<>(); // oldMap映射存储已经访问过的链接，boolean值表示是否访问过
        Pattern pattern = Pattern.compile("(https?://)?[^/\\s]*"); // 决定路径：正则表达式匹配首页网站和站内链接
        Matcher matcher = pattern.matcher(baseUrl);
        String oldLinkHost = "";
        // 找到新匹配，存储到以访问的Map中
        if (matcher.find()) {
            oldLinkHost = matcher.group();
        }
        oldMap.put(baseUrl, false);
        oldMap = crawlerLinks(oldLinkHost, oldMap);
        for (Map.Entry<String, Boolean> mapping : oldMap.entrySet()) {
            System.out.println("链接：" + mapping.getKey());
        }
    }

    private static Map<String, Boolean> crawlerLinks(String oldLinkHost, Map<String, Boolean> oldMap) {
        Map<String, Boolean> newMap = new LinkedHashMap<>(); // 创建新Map判断是否是遍历过的http
        String oldLink = "";
        for (Map.Entry<String, Boolean> mapping : oldMap.entrySet()) {
            if (!mapping.getValue()) {
                try {
                    oldLink = mapping.getKey();
                    System.out.println("链接：" + mapping.getKey() + "---ChecksStatus_" + mapping.getValue());
                    URL url = new URL(oldLink);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestMethod("GET");
                    if (httpURLConnection.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(httpURLConnection.getInputStream()));
                        Pattern pattern = Pattern
                                .compile("<a.*?href=[\\\"']?((https?://)?/?[^\\\"']+)[\\\"']?.*?>(.+)</a>");
                        Matcher matcher = null;
                        String line = "";
                        while ((line = reader.readLine()) != null) {
                            matcher = pattern.matcher(line);
                            if (matcher.find()) {
                                String newLink = matcher.group(1).trim();
                                if (!newLink.startsWith("http")) {
                                    if (newLink.startsWith("/")) {
                                        newLink = oldLinkHost + newLink;
                                    } else {
                                        newLink = oldLinkHost + "/" + newLink;
                                    }
                                }
                                if (newLink.endsWith("/")) {
                                    newLink = newLink.substring(0, newLink.length() - 1);
                                }
                                if (!newMap.containsKey(newLink) && !oldMap.containsKey(newLink)
                                        && newLink.contains(oldLinkHost)) {
                                    newMap.put(newLink, false);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        oldMap.replace(oldLink, false, true);
        if (!newMap.isEmpty()) {
            oldMap.putAll(newMap);
            oldMap.putAll(crawlerLinks(oldLinkHost, oldMap));
        }
        return oldMap;
    }
}
