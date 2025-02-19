package top.principlecreativity.lifestream.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.principlecreativity.lifestream.entity.Comment;
import top.principlecreativity.lifestream.entity.Post;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.exception.ResourceNotFoundException;
import top.principlecreativity.lifestream.repository.CommentRepository;

import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostService postService;

    public List<Comment> getRootCommentsByPost(Post post) {
        return commentRepository.findByPostAndParentCommentIsNullOrderByCreatedAtDesc(post);
    }

    public Comment getCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
    }

    public Page<Comment> getCommentsByUser(Long userId, Pageable pageable) {
        return commentRepository.findByAuthorId(userId, pageable);
    }

    @Transactional
    public Comment addComment(Long postId, Comment comment, User author) {
        Post post = postService.getPostById(postId);

        comment.setAuthor(author);
        comment.setPost(post);

        return commentRepository.save(comment);
    }

    @Transactional
    public Comment replyToComment(Long parentCommentId, Comment reply, User author) {
        Comment parentComment = getCommentById(parentCommentId);

        reply.setAuthor(author);
        reply.setPost(parentComment.getPost());
        reply.setParentComment(parentComment);

        return commentRepository.save(reply);
    }

    @Transactional
    public Comment updateComment(Long id, String content) {
        Comment comment = getCommentById(id);
        comment.setContent(content);
        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Long id) {
        Comment comment = getCommentById(id);
        commentRepository.delete(comment);
    }

    public Long countCommentsByPost(Post post) {
        return commentRepository.countByPost(post);
    }
}
