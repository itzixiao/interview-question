package cn.itzixiao.interview.actuator;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * 自定义应用信息贡献者
 * <p>
 * 作用：在/actuator/info 端点中显示自定义应用信息
 * 使用场景：
 * 1. 显示应用版本号
 * 2. 显示 Git 提交信息
 * 3. 显示构建时间
 */
@Component
public class CustomInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("app", "interview-service")
                .withDetail("version", "1.0.0-SNAPSHOT")
                .withDetail("description", "面试刷题系统-服务层");

        // 添加 Java 版本信息
        builder.withDetail("java.version", System.getProperty("java.version"))
                .withDetail("java.vendor", System.getProperty("java.vendor"));

        // 添加操作系统信息
        builder.withDetail("os.name", System.getProperty("os.name"))
                .withDetail("os.arch", System.getProperty("os.arch"))
                .withDetail("os.version", System.getProperty("os.version"));

        // 添加内存信息
        Runtime runtime = Runtime.getRuntime();
        builder.withDetail("memory", new java.util.HashMap<String, Object>() {{
            put("maxMemory", runtime.maxMemory() / (1024 * 1024) + " MB");
            put("totalMemory", runtime.totalMemory() / (1024 * 1024) + " MB");
            put("freeMemory", runtime.freeMemory() / (1024 * 1024) + " MB");
        }});
    }
}
