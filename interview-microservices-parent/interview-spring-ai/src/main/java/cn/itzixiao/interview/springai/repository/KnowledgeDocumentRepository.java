package cn.itzixiao.interview.springai.repository;

import cn.itzixiao.interview.springai.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库文档 Repository
 *
 * @author itzixiao
 * @since 2024-01-01
 */
@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    /**
     * 根据标题模糊查询
     */
    List<KnowledgeDocument> findByTitleContaining(String title);

    /**
     * 根据文档类型查询
     */
    List<KnowledgeDocument> findByDocType(String docType);

    /**
     * 一次性查询片段总数和字符总数（避免多次 findAll）
     */
    @Query("SELECT COALESCE(SUM(d.chunkCount), 0), COALESCE(SUM(d.charCount), 0) FROM KnowledgeDocument d")
    List<Object[]> sumChunksAndChars();
}
