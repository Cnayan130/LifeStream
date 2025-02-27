package top.principlecreativity.lifestream.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import top.principlecreativity.lifestream.entity.Post;
import top.principlecreativity.lifestream.entity.User;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByPublishedTrue(Pageable pageable);
    Page<Post> findByAuthor(User author, Pageable pageable);
    Page<Post> findByTitleContainingAndPublishedTrue(String title, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t.name = :tagName AND p.published = true")
    Page<Post> findByTagName(String tagName, Pageable pageable);

    @Query("SELECT DISTINCT YEAR(p.createdAt) FROM Post p WHERE p.published = true ORDER BY YEAR(p.createdAt) DESC")
    List<Integer> findAllPublishedYears();

    @Query("SELECT p FROM Post p WHERE YEAR(p.createdAt) = :year AND MONTH(p.createdAt) = :month AND p.published = true")
    Page<Post> findByYearAndMonth(int year, int month, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR p.content LIKE CONCAT('%', :keyword, '%') AND p.published = true")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 查找指定创建时间之前的第一篇已发布文章
     */
    Post findFirstByCreatedAtBeforeAndPublishedTrueOrderByCreatedAtDesc(LocalDateTime createdAt);

    /**
     * 查找指定创建时间之后的第一篇已发布文章
     */
    Post findFirstByCreatedAtAfterAndPublishedTrueOrderByCreatedAt(LocalDateTime createdAt);

    Page<Post> findByAuthorOrPublishedTrue(User author, boolean published, Pageable pageable);

    // Add to PostRepository.java
    Page<Post> findByAuthorAndPublishedTrue(User author, Pageable pageable);
    long countByAuthor(User author);

}