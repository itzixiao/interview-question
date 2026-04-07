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
        
        // 4. 处理 POST 请求（登录示例）
        Document loginPage = Jsoup.connect("https://example.com/login")
            .data("username", "user")
            .data("password", "pass")
            .method(Connection.Method.POST)
            .execute()
            .parse();
        
        // 5. 获取响应信息
        Connection.Response response = Jsoup.connect("https://example.com")
            .execute();
        int statusCode = response.statusCode();
        String contentType = response.contentType();
        Map<String, String> cookies = response.cookies();
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
        
        // 5. 高级选择器（实战常用）
        Elements visibleText = doc.select("p:matchesOwn(\\S+)");  // 包含非空白文本的 <p>
        Elements regexMatch = doc.select("img[src~=(?i)\\.(png|jpe?g)]");  // 正则匹配
        Elements containsText = doc.select("a:contains(下一页)");  // 包含指定文本
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
        
        // 5. 清理文本（去除多余空白）
        String cleanText = doc.selectFirst("article").ownText();
        
        // 6. 获取外层 HTML
        String outerHtml = doc.selectFirst("div.content").outerHtml();
    }
}
```

### 2.4 DOM 操作与修改

```java
/**
 * DOM 操作示例
 */
public class DomManipulationDemo {
    
    public void manipulateDom(Document doc) {
        Element div = doc.selectFirst("div.content");
        
        // 1. 添加元素
        div.append("<p>新增段落</p>");        // 追加子元素（末尾）
        div.prepend("<p>开头段落</p>");       // 前置子元素（开头）
        
        // 2. 添加文本
        div.appendText(" 附加文本");
        div.prependText("前置文本 ");
        
        // 3. 设置属性
        div.attr("data-custom", "value");
        div.addClass("new-class");
        div.removeClass("old-class");
        
        // 4. 修改内容
        div.text("纯文本内容");
        div.html("<strong>HTML 内容</strong>");
        
        // 5. 删除元素
        div.selectFirst(".unwanted").remove();
        
        // 6. 替换元素
        Element oldElement = doc.selectFirst("div.old");
        oldElement.replaceWith(Element.createShell(doc).tag("div").text("新内容"));
    }
}
```

### 2.5 HTML 清理（防止 XSS）

```java
/**
 * HTML 清理示例
 */
public class HtmlCleanerDemo {
    
    public void cleanHtml() {
        String unsafeHtml = "<p onclick='alert(1)'>安全文本<script>alert(1)</script></p>";
        
        // 1. 基础清理（移除危险标签和属性）
        String clean = Jsoup.clean(unsafeHtml, Safelist.basic());
        // 输出: <p>安全文本</p>
        
        // 2. 自定义安全策略
        Safelist customSafelist = Safelist.relaxed()
            .addTags("iframe", "embed")
            .addAttributes(":all", "data-*")
            .addProtocols("img", "src", "cid", "http", "https");
        
        String customClean = Jsoup.clean(unsafeHtml, customSafelist);
        
        // 3. 清理并格式化
        Document dirtyDoc = Jsoup.parse(unsafeHtml);
        new Cleaner(Safelist.basic()).clean(dirtyDoc);
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

---

## 四、实战案例：新闻网站数据抓取

### 4.1 场景描述

抓取新闻网站的标题、作者、发布时间、正文内容，并保存为结构化数据。

### 4.2 完整实现

```java
/**
 * 新闻网站爬虫
 * 抓取新闻列表和详情页
 */
@Component
public class NewsCrawlerService {
    
    private static final Logger log = LoggerFactory.getLogger(NewsCrawlerService.class);
    
    /**
     * 抓取新闻列表页
     */
    public List<NewsDTO> crawlNewsList(String listUrl) throws IOException {
        List<NewsDTO> newsList = new ArrayList<>();
        
        Document doc = Jsoup.connect(listUrl)
            .timeout(30000)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .get();
        
        // 选择新闻条目
        Elements newsItems = doc.select(".news-item");
        
        for (Element item : newsItems) {
            NewsDTO news = new NewsDTO();
            
            // 提取标题和链接
            Element titleLink = item.selectFirst("h2 a");
            if (titleLink != null) {
                news.setTitle(titleLink.text());
                news.setUrl(titleLink.absUrl("href"));
            }
            
            // 提取作者
            Element author = item.selectFirst(".author");
            if (author != null) {
                news.setAuthor(author.text());
            }
            
            // 提取发布时间
            Element time = item.selectFirst(".publish-time");
            if (time != null) {
                news.setPublishTime(parseTime(time.text()));
            }
            
            newsList.add(news);
        }
        
        log.info("抓取到 {} 条新闻", newsList.size());
        return newsList;
    }
    
    /**
     * 抓取新闻详情
     */
    public NewsDetailDTO crawlNewsDetail(String detailUrl) throws IOException {
        Document doc = Jsoup.connect(detailUrl)
            .timeout(30000)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .get();
        
        NewsDetailDTO detail = new NewsDetailDTO();
        
        // 提取标题
        Element title = doc.selectFirst("h1.article-title");
        detail.setTitle(title != null ? title.text() : "");
        
        // 提取正文（去除广告、推荐等干扰元素）
        Element content = doc.selectFirst("div.article-content");
        if (content != null) {
            // 移除干扰元素
            content.select(".ad, .recommend, .share").remove();
            detail.setContent(content.html());
            detail.setPlainText(content.text());
        }
        
        // 提取标签
        Elements tags = doc.select(".tags a");
        List<String> tagList = tags.stream()
            .map(Element::text)
            .collect(Collectors.toList());
        detail.setTags(tagList);
        
        // 提取图片
        Elements images = doc.select("article img[src]");
        List<String> imageUrls = images.stream()
            .map(img -> img.absUrl("src"))
            .collect(Collectors.toList());
        detail.setImages(imageUrls);
        
        return detail;
    }
    
    /**
     * 分页抓取
     */
    public List<NewsDTO> crawlMultiplePages(String baseUrl, int maxPages) throws IOException {
        List<NewsDTO> allNews = new ArrayList<>();
        
        for (int page = 1; page <= maxPages; page++) {
            String pageUrl = baseUrl + "?page=" + page;
            log.info("正在抓取第 {} 页: {}", page, pageUrl);
            
            List<NewsDTO> pageNews = crawlNewsList(pageUrl);
            if (pageNews.isEmpty()) {
                log.info("第 {} 页无数据，停止抓取", page);
                break;
            }
            
            allNews.addAll(pageNews);
            
            // 礼貌延迟（避免被封 IP）
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return allNews;
    }
    
    /**
     * 解析时间字符串
     */
    private LocalDateTime parseTime(String timeStr) {
        // 实现时间解析逻辑
        return LocalDateTime.now();
    }
}
```

### 4.3 关键技术点

| 技术 | 作用 | 示例 |
|-----|------|------|
| `Jsoup.connect()` | 发起 HTTP 请求 | `Jsoup.connect(url).timeout(30000).get()` |
| `doc.select()` | CSS 选择器查询 | `doc.select("img[src]")` |
| `element.attr()` | 获取属性值 | `img.attr("src")` |
| `element.absUrl()` | 获取绝对 URL | `link.absUrl("href")` |
| `element.remove()` | 删除干扰元素 | `content.select(".ad").remove()` |
| `URL(base, relative)` | 解析相对 URL | `new URL(baseUrl, relativeUrl)` |

---

## 五、反爬虫策略与合规性

### 5.1 常见反爬虫策略及应对

| 反爬策略 | 原理 | 应对方案 |
|---------|------|---------|
| **User-Agent 检测** | 检查请求头是否为浏览器 | 设置真实 UA |
| **IP 频率限制** | 同一 IP 请求过快 | 延时 + 代理池 |
| **Cookie 验证** | 需要特定 Cookie | 先访问首页获取 Cookie |
| **动态渲染** | 内容通过 JS 加载 | Selenium / Playwright |
| **验证码** | 图形/滑块验证 | 打码平台 / 人工介入 |
| **数据加密** | 接口返回加密数据 | 逆向分析解密算法 |

### 5.2 最佳实践代码

```java
/**
 * 合规爬虫示例
 */
public class EthicalCrawler {
    
    public Document fetchWithBestPractice(String url) throws IOException {
        return Jsoup.connect(url)
            // 1. 设置真实 User-Agent
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                     + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            
            // 2. 设置 Referer
            .referrer("https://www.google.com/")
            
            // 3. 设置超时
            .timeout(30000)
            
            // 4. 跟随重定向
            .followRedirects(true)
            
            // 5. 忽略 ContentType 错误
            .ignoreContentType(true)
            
            // 6. 忽略 HTTP 错误（如 404）
            .ignoreHttpErrors(true)
            
            // 7. 设置代理（可选）
            // .proxy("proxy.example.com", 8080)
            
            .get();
    }
    
    /**
     * 礼貌延迟
     */
    public void politeDelay() {
        try {
            // 随机延迟 1-3 秒
            Thread.sleep(1000 + new Random().nextInt(2000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 5.3 法律合规注意事项

1. **遵守 robots.txt**
   ```
   # 检查网站的 robots.txt
   https://example.com/robots.txt
   ```

2. **尊重版权**
   - 不要抓取付费内容
   - 不要用于商业用途（除非获得授权）
   - 注明来源

3. **控制频率**
   - 避免对目标网站造成性能影响
   - 建议 QPS < 1

4. **数据使用**
   - 个人信息需遵守 GDPR/个人信息保护法
   - 敏感数据脱敏处理

### 5.4 处理动态内容

```java
/**
 * Jsoup 无法执行 JavaScript，对于 SPA 应用需要其他方案
 */
public class DynamicContentHandler {
    
    // 方案1：直接调用后端 API（推荐）
    public String fetchViaAPI() throws IOException {
        return Jsoup.connect("https://api.example.com/news?page=1")
            .header("Accept", "application/json")
            .execute()
            .body();
    }
    
    // 方案2：使用 Selenium（适用于复杂场景）
    /*
    WebDriver driver = new ChromeDriver();
    driver.get("https://example.com");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".content")));
    String html = driver.getPageSource();
    Document doc = Jsoup.parse(html);
    driver.quit();
    */
}
```

---

## 六、常见问题与解决方案

### 6.1 编码问题

```java
// 问题：中文乱码
// 解决：指定编码
Document doc = Jsoup.connect(url)
    .get();  // Jsoup 会自动检测编码

// 手动指定编码
String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
Document doc2 = Jsoup.parse(html);
```

### 6.2 超时问题

```java
// 问题：请求超时
// 解决：增加超时时间 + 重试机制
public Document fetchWithRetry(String url, int maxRetries) throws IOException {
    IOException lastException = null;
    
    for (int i = 0; i < maxRetries; i++) {
        try {
            return Jsoup.connect(url)
                .timeout(30000)
                .get();
        } catch (IOException e) {
            lastException = e;
            log.warn("请求失败，第 {} 次重试: {}", i + 1, url);
            try {
                Thread.sleep(2000);  // 重试前等待
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    throw lastException;
}
```

### 6.3 内存问题

```java
// 问题：抓取大页面 OOM
// 解决：限制最大字节数
Document doc = Jsoup.connect(url)
    .maxBodySize(10 * 1024 * 1024)  // 限制 10MB
    .get();

// 只提取需要的内容，避免保留整个 Document
String content = doc.selectFirst(".article").html();
doc = null;  // 释放内存
```

---

## 七、高频面试题

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

**问题 4：如何防止爬虫被封 IP？**

**答：**

1. **设置合理 User-Agent**
   ```java
   .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
   ```

2. **控制请求频率**
   ```java
   Thread.sleep(1000 + new Random().nextInt(2000));  // 随机延迟 1-3 秒
   ```

3. **使用代理池**
   ```java
   .proxy("proxy.example.com", 8080)
   ```

4. **设置 Cookie**
   ```java
   .cookie("session", "valid-session-token")
   ```

5. **遵守 robots.txt**
   - 检查并遵守网站的爬虫规则

**问题 5：Jsoup 如何防止 XSS 攻击？**

**答：**

使用 `Jsoup.clean()` 方法清理用户输入的 HTML：

```java
String unsafeHtml = "<p onclick='alert(1)'>内容<script>alert(1)</script></p>";

// 基础清理（移除危险标签和属性）
String clean = Jsoup.clean(unsafeHtml, Safelist.basic());
// 输出: <p>内容</p>

// 自定义安全策略
Safelist custom = Safelist.relaxed()
    .addTags("iframe")
    .addAttributes(":all", "data-*");
String customClean = Jsoup.clean(unsafeHtml, custom);
```

**问题 6：如何处理大页面的内存问题？**

**答：**

```java
// 1. 限制最大字节数
Document doc = Jsoup.connect(url)
    .maxBodySize(10 * 1024 * 1024)  // 限制 10MB
    .get();

// 2. 提取需要的内容后立即释放大对象
String content = doc.selectFirst(".article").html();
doc = null;  // 帮助 GC 回收

// 3. 只解析必要的部分
Element body = doc.body();
doc = null;  // 丢弃 Document，只保留需要的 Element
```

**问题 7：Jsoup 的选择器语法有哪些常用技巧？**

**答：**

```java
// 1. 组合选择器
doc.select("div.item > span");      // 直接子元素
doc.select("h1 ~ p");               // 兄弟元素

// 2. 伪类选择器
doc.select("li:first-child");       // 第一个
doc.select("tr:odd");               // 奇数行
doc.select("a:contains(下一页)");   // 包含文本

// 3. 属性选择器
doc.select("a[href^=http]");        // href 以 http 开头
doc.select("a[href$=.pdf]");        // href 以 .pdf 结尾
doc.select("a[href*=google]");      // href 包含 google

// 4. 正则匹配
doc.select("img[src~=(?i)\\.(png|jpe?g)]");

// 5. 排除选择器
doc.select("div:not(.nav)");
```

**问题 8：如何实现分页抓取？**

**答：**

```java
public List<NewsDTO> crawlMultiplePages(String baseUrl, int maxPages) throws IOException {
    List<NewsDTO> allNews = new ArrayList<>();
    
    for (int page = 1; page <= maxPages; page++) {
        String pageUrl = baseUrl + "?page=" + page;
        List<NewsDTO> pageNews = crawlNewsList(pageUrl);
        
        if (pageNews.isEmpty()) {
            break;  // 无数据则停止
        }
        
        allNews.addAll(pageNews);
        
        // 礼貌延迟
        Thread.sleep(1000);
    }
    
    return allNews;
}
```

**问题 9：Jsoup 能处理表单提交吗？**

**答：**

可以，通过 POST 方法模拟表单提交：

```java
Document result = Jsoup.connect("https://example.com/search")
    .data("keyword", "Java")
    .data("category", "tech")
    .data("page", "1")
    .method(Connection.Method.POST)
    .execute()
    .parse();
```

**问题 10：如何提取表格数据？**

**答：**

```java
Document doc = Jsoup.connect(url).get();

// 选择表格
Element table = doc.selectFirst("table.data-table");

// 遍历行
Elements rows = table.select("tr");
for (Element row : rows) {
    Elements cells = row.select("th, td");
    for (Element cell : cells) {
        System.out.print(cell.text() + "\t");
    }
    System.out.println();
}
```

---

## 八、参考资源

- **官方文档**：https://jsoup.org/
- **Cookbook**：https://jsoup.org/cookbook/
- **API 文档**：https://jsoup.org/apidocs/
- **选择器语法**：https://jsoup.org/cookbook/extracting-data/selector-syntax

---

**维护者：** itzixiao  
**最后更新：** 2026-04-07  
**配套代码：** `interview-microservices-parent/interview-service/src/main/java/cn/itzixiao/interview/downloader/service/HttpDownloaderService.java`
