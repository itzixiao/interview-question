package cn.itzixiao.interview.springai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Spring AI 智能体应用启动类
 * 
 * <p>基于 Spring AI 框架实现的智能体示例，展示以下核心功能：</p>
 * <ul>
 *     <li>对话聊天：与 AI 进行多轮对话</li>
 *     <li>RAG 知识库：检索增强生成，基于知识库回答问题</li>
 *     <li>Function Calling：AI 调用外部函数获取实时数据</li>
 *     <li>多模态支持：文本、图像处理</li>
 *     <li>Agent 编排：复杂任务的智能规划和执行</li>
 * </ul>
 * 
 * @author itzixiao
 * @since 2024-01-01
 */
@SpringBootApplication
@EnableAspectJAutoProxy
public class SpringAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiApplication.class, args);
    }
}
