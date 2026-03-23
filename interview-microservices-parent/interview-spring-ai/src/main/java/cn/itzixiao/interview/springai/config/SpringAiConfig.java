package cn.itzixiao.interview.springai.config;

import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置类
 *
 * @author itzixiao
 * @since 2024-01-01
 */
@Configuration
public class SpringAiConfig {

    /**
     * 配置简单的内存向量存储
     *
     * @param embeddingClient 嵌入客户端
     * @return VectorStore 向量存储
     */
    @Bean
    public VectorStore vectorStore(EmbeddingClient embeddingClient) {
        return new SimpleVectorStore(embeddingClient);
    }
}
