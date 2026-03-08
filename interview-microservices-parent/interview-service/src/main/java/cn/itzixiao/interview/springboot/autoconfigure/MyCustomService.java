package cn.itzixiao.interview.springboot.autoconfigure;

/**
 * 自定义服务类
 * 由 MyCustomAutoConfiguration 自动配置创建
 */
public class MyCustomService {

    private final String name;
    private final String value;

    public MyCustomService(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String sayHello() {
        return "Hello from " + name + " with value " + value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
