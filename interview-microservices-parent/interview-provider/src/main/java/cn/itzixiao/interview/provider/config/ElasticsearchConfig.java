package cn.itzixiao.interview.provider.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.util.StringUtils;

/**
 * Elasticsearch 配置类
 * <p>
 * Spring Data Elasticsearch 5.x (ES 8.x) 使用新的 ElasticsearchClient
 */
@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String esUris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {
        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder =
                ClientConfiguration.builder().connectedTo(esUris.replace("http://", "").replace("https://", ""));

        // 配置超时时间
        builder.withConnectTimeout(java.time.Duration.ofSeconds(60));
        builder.withSocketTimeout(java.time.Duration.ofSeconds(60));

        // 如果配置了用户名密码，则添加认证
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            builder.withBasicAuth(username, password);
        }

        return builder.build();
    }
}
