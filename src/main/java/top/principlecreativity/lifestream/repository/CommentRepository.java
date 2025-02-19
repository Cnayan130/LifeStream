package top.principlecreativity.lifestream.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.principlecreativity.lifestream.entity.Comment;
import top.principlecreativity.lifestream.entity.Post;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostAndParentCommentIsNullOrderByCreatedAtDesc(Post post);
    Page<Comment> findByAuthorId(Long authorId, Pageable pageable);
    Long countByPost(Post post);
}