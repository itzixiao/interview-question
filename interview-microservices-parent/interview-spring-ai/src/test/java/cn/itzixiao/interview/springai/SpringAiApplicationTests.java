package cn.itzixiao.interview.springai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Spring AI 应用测试
 *
 * @author itzixiao
 * @since 2024-01-01
 */
@SpringBootTest(classes = SpringAiApplication.class)
@TestPropertySource(properties = {
    "spring.ai.openai.api-key=test-key",
    "spring.ai.openai.chat.options.model=gpt-3.5-turbo"
})
class SpringAiApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文能正常加载
    }
}
