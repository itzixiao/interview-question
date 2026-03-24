package cn.itzixiao.interview.springai.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 知识库文档实体
 *
 * @author itzixiao
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文档标题
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 原始文件名
     */
    @Column(length = 200)
    private String filename;

    /**
     * 文档内容（可选，大文本存储）
     */
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    /**
     * 文本片段数量
     */
    @Column(name = "chunk_count")
    private Integer chunkCount;

    /**
     * 字符数
     */
    @Column(name = "char_count")
    private Integer charCount;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 文档类型：text, pdf, word, markdown
     */
    @Column(name = "doc_type", length = 50)
    private String docType;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
