# Jsoup 网络爬虫实战

## 一、Jsoup 简介

### 1.1 什么是 Jsoup？

Jsoup 是一款 Java 的 HTML 解析器，可直接解析某个 URL 地址、HTML 文本内容。它提供了一套非常省力的 API，可通过 DOM、CSS 以及类似于 jQuery 的操作方法来取出和操作数据。

**核心优势：**

- ✅ **简单易用** - 类似 jQuery 的 CSS Selector 语法
- ✅ **功能强大** - 支持 HTTP 请求、HTML 解析、数据提取
- ✅ **安全可靠** - 防止 XSS 攻击，自动处理相对 URL
- ✅ **轻量级** - 无依赖，jar 包仅约 400KB

### 1.2 Maven 依赖

```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.16.1</version>
</dependency>
```

---

## 二、核心 API 详解

### 2.1 连接与请求

```java
/**
 * 基础连接示例
 */
public class ConnectionDemo {
    
    public static void main(String[] args) throws IOException {
        // 1. 从 URL 加载文档
        Document doc = Jsoup.connect("https://example.com")
            .timeout(30000)                    // 超时时间（毫秒）
            .userAgent("Mozilla/5.0...")      // 设置 User-Agent
            .header("Accept", "text/html")     // 自定义请求头
            .cookie("auth", "token")           // 设置 Cookie
            .data("key", "value")              // POST 参数
            .method(Connection.Method.GET)     // 请求方法
            .execute()                         // 执行请求
            .parse();                          // 解析响应
        
        // 2. 从 HTML 字符串解析
        String html = "<html><body><h1>Hello</h1></body></html>";
        Document doc2 = Jsoup.parse(html);
        
        // 3. 从本地文件解析
        File input = new File("/path/to/file.html");
        Document doc3 = Jsoup.parse(input, "UTF-8");
    }
}
```

### 2.2 选择器语法

```java
/**
 * CSS 选择器示例
 */
public class SelectorDemo {
    
    public void selectors(Document doc) {
        // 1. 基础选择器
        Elements links = doc.select("a");                    // 所有 <a> 标签
        Elements navLinks = doc.select("#nav a");           // id="nav" 下的 <a>
        Elements articles = doc.select(".article");         // class="article"
        
        // 2. 属性选择器
        Elements absLinks = doc.select("a[href]");          // 有 href 属性的 <a>
        Elements extLinks = doc.select("a[href^=http]");    // href 以 http 开头
        Elements pdfLinks = doc.select("a[href$=.pdf]");    // href 以 .pdf 结尾
        Elements contains = doc.select("a[href*=google]");  // href 包含 google
        
        // 3. 组合选择器
        Elements items = doc.select("div.item > span");     // 直接子元素
        Elements siblings = doc.select("h1 ~ p");           // 兄弟元素
        Elements first = doc.select("li:first-child");      // 第一个子元素
        Elements odd = doc.select("tr:odd");                // 奇数行
        
        // 4. 伪类选择器
        Elements hasImg = doc.select("div:has(img)");       // 包含 <img> 的 <div>
        Elements notNav = doc.select("div:not(.nav)");      // 不含 nav 类的 <div>
        Elements eq2 = doc.select("li:eq(2)");              // 第 3 个 li（从0开始）
    }
}
```

### 2.3 数据提取

```java
/**
 * 数据提取示例
 */
public class DataExtractionDemo {
    
    public void extractData(Document doc) {
        // 1. 获取元素内容
        Element title = doc.selectFirst("title");
        String titleText = title.text();           // 获取文本内容
        String titleHtml = title.html();           // 获取 HTML 内容
        
        // 2. 获取属性值
        Element link = doc.selectFirst("a");
        String href = link.attr("href");           // 获取 href 属性
        String absHref = link.absUrl("href");      // 获取绝对 URL
        
        // 3. 遍历元素列表
        Elements newsItems = doc.select(".news-item");
        for (Element item : newsItems) {
            String headline = item.selectFirst("h2").text();
            String summary = item.selectFirst(".summary").text();
            String url = item.selectFirst("a").absUrl("href");
            
            System.out.printf("标题: %s, 链接: %s%n", headline, url);
        }
        
        // 4. 使用 Stream API
        List<String> titles = doc.select("h2").stream()
            .map(Element::text)
            .filter(text -> text.length() > 10)
            .collect(Collectors.toList());
    }
}
```

---

## 三、实战案例：批量下载网站资源

### 3.1 场景描述

实现一个 HTTP 批量下载服务，功能包括：
- 解析目标网站的所有资源（CSS、JS、图片、字体等）
- 支持按资源类型过滤
- 多线程并行下载
- 实时进度追踪

### 3.2 完整实现

```java
/**
 * HTTP 批量下载服务
 * 使用 Jsoup 解析网站资源，多线程并行下载
 */
@Service
public class HttpDownloaderService {
    
    private static final Logger log = LoggerFactory.getLogger(HttpDownloaderService.class);
    
    /** 任务存储 */
    private static final ConcurrentHashMap<String, DownloadTask> taskMap = new ConcurrentHashMap<>();
    
    /** 进度存储 */
    private static final ConcurrentHashMap<String, DownloadProgress> progressMap = new ConcurrentHashMap<>();

    /**
     * 解析网站资源
     * 使用 Jsoup 连接目标网站，解析 HTML 文档，提取所有资源链接
     */
    public List<ResourceInfo> parseResources(String targetUrl, List<String> resourceTypes) {
        List<ResourceInfo> resources = new ArrayList<>();
        Set<String> urlSet = new HashSet<>();  // 去重

        try {
            log.info("开始解析网站资源: {}", targetUrl);
            
            // 1. 使用 Jsoup 连接目标网站
            Document doc = Jsoup.connect(targetUrl)
                    .timeout(30000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();

            // 2. 解析基础 URL
            URL baseUrl = new URL(targetUrl);
            String basePath = baseUrl.getProtocol() + "://" + baseUrl.getHost();
            if (baseUrl.getPort() != -1) {
                basePath += ":" + baseUrl.getPort();
            }

            // 3. 解析 CSS 文件
            Elements cssLinks = doc.select("link[rel=stylesheet]");
            for (Element link : cssLinks) {
                String href = link.attr("href");
                String fullUrl = resolveUrl(basePath, targetUrl, href);
                addResourceIfMatch(resources, urlSet, fullUrl, resourceTypes);
            }

            // 4. 解析 JS 文件
            Elements jsScripts = doc.select("script[src]");
            for (Element script : jsScripts) {
                String src = script.attr("src");
                String fullUrl = resolveUrl(basePath, targetUrl, src);
                addResourceIfMatch(resources, urlSet, fullUrl, resourceTypes);
            }

            // 5. 解析图片
            Elements images = doc.select("img[src]");
            for (Element img : images) {
                String src = img.attr("src");
                String fullUrl = resolveUrl(basePath, targetUrl, src);
                addResourceIfMatch(resources, urlSet, fullUrl, resourceTypes);
            }

            // 6. 解析字体文件
            Elements links = doc.select("link[href]");
            for (Element link : links) {
                String href = link.attr("href");
                if (href.contains(".woff") || href.contains(".woff2") || href.contains(".ttf")) {
                    String fullUrl = resolveUrl(basePath, targetUrl, href);
                    addResourceIfMatch(resources, urlSet, fullUrl, resourceTypes);
                }
            }

            // 7. 解析媒体资源（video, audio, source）
            Elements mediaElements = doc.select("video[src], audio[src], source[src]");
            for (Element media : mediaElements) {
                String src = media.attr("src");
                String fullUrl = resolveUrl(basePath, targetUrl, src);
                addResourceIfMatch(resources, urlSet, fullUrl, resourceTypes);
            }

            log.info("解析完成，共发现 {} 个资源", resources.size());

        } catch (IOException e) {
            log.error("解析网站资源失败: {}", targetUrl, e);
            throw new RuntimeException("解析网站资源失败: " + e.getMessage());
        }

        return resources;
    }

    /**
     * 解析完整 URL
     * 处理相对路径、绝对路径、协议相对路径
     */
    private String resolveUrl(String basePath, String baseUrl, String relativeUrl) {
        if (StringUtils.isEmpty(relativeUrl)) {
            return null;
        }

        // 已经是完整 URL
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl;
        }

        // 协议相对 URL（//example.com/path）
        if (relativeUrl.startsWith("//")) {
            return basePath.substring(0, basePath.indexOf(":")) + ":" + relativeUrl;
        }

        // 相对路径，使用 URL 类解析
        try {
            URL base = new URL(baseUrl);
            URL resolved = new URL(base, relativeUrl);
            return resolved.toString();
        } catch (MalformedURLException e) {
            log.warn("URL 解析失败: base={}, relative={}", baseUrl, relativeUrl);
            return null;
        }
    }

    /**
     * 添加资源（如果类型匹配且未重复）
     */
    private void addResourceIfMatch(List<ResourceInfo> resources, Set<String> urlSet, 
                                    String url, List<String> resourceTypes) {
        if (StringUtils.isEmpty(url) || urlSet.contains(url)) {
            return;
        }

        String type = inferResourceType(url);

        // 如果指定了资源类型过滤
        if (resourceTypes != null && !resourceTypes.isEmpty()) {
            if (!resourceTypes.contains(type)) {
                return;
            }
        }

        urlSet.add(url);

        ResourceInfo resource = new ResourceInfo();
        resource.setResourceId(UUID.randomUUID().toString().replace("-", ""));
        resource.setUrl(url);
        resource.setType(type);
        resource.setStatus(0);  // 待下载

        resources.add(resource);
    }

    /**
     * 根据 URL 推断资源类型
     */
    private String inferResourceType(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".css")) return "css";
        if (lowerUrl.endsWith(".js")) return "js";
        if (lowerUrl.matches(".*\\.(jpg|jpeg|png|gif|webp|svg).*")) return "img";
        if (lowerUrl.matches(".*\\.(woff|woff2|ttf|otf|eot).*")) return "font";
        if (lowerUrl.matches(".*\\.(mp4|webm|ogg).*")) return "video";
        if (lowerUrl.matches(".*\\.(mp3|wav|ogg).*")) return "audio";
        return "other";
    }
}
```

### 3.3 关键技术点

| 技术 | 作用 | 示例 |
|-----|------|------|
| `Jsoup.connect()` | 发起 HTTP 请求 | `Jsoup.connect(url).timeout(30000).get()` |
| `doc.select()` | CSS 选择器查询 | `doc.select("img[src]")` |
| `element.attr()` | 获取属性值 | `img.attr("src")` |
| `URL(base, relative)` | 解析相对 URL | `new URL(baseUrl, relativeUrl)` |
| `absUrl()` | 获取绝对 URL | `link.absUrl("href")` |

### 3.4 注意事项

**1. 反爬虫策略应对**

```java
// 设置合理的 User-Agent
.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")

// 添加延时，避免请求过快
Thread.sleep(1000);

// 使用代理
Jsoup.connect(url).proxy("proxy.example.com", 8080).get();
```

**2. 处理动态内容**

```java
// Jsoup 无法执行 JavaScript，对于 SPA 应用需要配合 Selenium
// 或使用浏览器开发者工具获取实际请求的 API
```

**3. 编码问题处理**

```java
// 指定编码解析
Document doc = Jsoup.parse(input, "UTF-8", baseUrl);

// 自动检测编码
Document doc = Jsoup.connect(url).execute().parse();
```

---

## 四、高频面试题

**问题 1：Jsoup 和 HttpClient 有什么区别？**

**答：**

| 特性 | Jsoup | HttpClient |
|-----|-------|-----------|
| 主要功能 | HTML 解析 + HTTP 请求 | HTTP 请求 |
| 解析能力 | 强大的 DOM/CSS 选择器 | 无 |
| 使用场景 | 网页抓取、数据提取 | API 调用、文件下载 |
| 依赖 | 独立库 | JDK 11+ 内置 |

**问题 2：如何处理相对路径的 URL？**

**答：**

```java
// 方法1：使用 Jsoup 的 absUrl()
element.absUrl("href");

// 方法2：使用 URL 类解析
URL base = new URL(baseUrl);
URL resolved = new URL(base, relativeUrl);

// 方法3：手动拼接（不推荐）
String fullUrl = baseUrl + (relativeUrl.startsWith("/") ? "" : "/") + relativeUrl;
```

**问题 3：Jsoup 能抓取动态渲染的页面吗？**

**答：**

Jsoup 只能获取静态 HTML，无法执行 JavaScript。对于动态渲染的页面（如 Vue、React 应用），需要：
1. 使用 Selenium + WebDriver 模拟浏览器
2. 分析网络请求，直接调用后端 API
3. 使用 Puppeteer 等无头浏览器工具

---

## 五、参考资源

- **官方文档**：https://jsoup.org/
- **Cookbook**：https://jsoup.org/cookbook/
- **API 文档**：https://jsoup.org/apidocs/

---

**维护者：** itzixiao  
**最后更新：** 2026-04-02  
**配套代码：** `interview-microservices-parent/interview-service/src/main/java/cn/itzixiao/interview/downloader/service/HttpDownloaderService.java`
