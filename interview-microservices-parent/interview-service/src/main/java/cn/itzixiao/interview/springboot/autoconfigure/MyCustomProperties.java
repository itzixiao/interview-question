package cn.itzixiao.interview.springboot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 自定义配置属性类
 *
 * 对应 application.yml 或 application.properties 中的配置：
 * my.custom.name=MyName
 * my.custom.value=MyValue
 * my.custom.enabled=true
 */
@ConfigurationProperties(prefix = "my.custom")
public class MyCustomProperties {

    /**
     * 名称
     */
    private String name = "default";

    /**
     * 值
     */
    private String value = "defaultValue";

    /**
     * 是否启用
     */
    private boolean enabled = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
