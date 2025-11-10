package top.principlecreativity.lifestream.controller.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import top.principlecreativity.lifestream.entity.Comment;
import top.principlecreativity.lifestream.entity.Post;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.payload.ApiResponse;
import top.principlecreativity.lifestream.payload.CommentRequest;
import top.principlecreativity.lifestream.payload.CommentResponse;
import top.principlecreativity.lifestream.payload.PagedResponse;
import top.principlecreativity.lifestream.security.CurrentUser;
import top.principlecreativity.lifestream.security.UserPrincipal;
import top.principlecreativity.lifestream.service.CommentService;
import top.principlecreativity.lifestream.service.PostService;
import top.principlecreativity.lifestream.service.UserService;
import top.principlecreativity.lifestream.util.AppConstants;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @GetMapping("/posts/{postId}/comments")
    public List<CommentResponse> getCommentsByPostId(@PathVariable Long postId) {
        Post post = postService.getPostById(postId);
        List<Comment> comments = commentService.getRootCommentsByPost(post);

        return comments.stream()
                .map(this::convertToCommentResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/posts/{postId}/comments")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long postId,
                                                      @Valid @RequestBody CommentRequest commentRequest,
                                                      @CurrentUser UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());

        Comment comment = new Comment();
        comment.setContent(commentRequest.getContent());

        Comment newComment = commentService.addComment(postId, comment, user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{commentId}")
                .buildAndExpand(newComment.getId()).toUri();

        return ResponseEntity.created(location).body(convertToCommentResponse(newComment));
    }

    @PostMapping("/comments/{commentId}/replies")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommentResponse> replyToComment(@PathVariable Long commentId,
                                                          @Valid @RequestBody CommentRequest commentRequest,
                                                          @CurrentUser UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());

        Comment reply = new Comment();
        reply.setContent(commentRequest.getContent());

        Comment newReply = commentService.replyToComment(commentId, reply, user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{replyId}")
                .buildAndExpand(newReply.getId()).toUri();

        return ResponseEntity.created(location).body(convertToCommentResponse(newReply));
    }

    @PutMapping("/comments/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommentResponse> updateComment(@PathVariable Long id,
                                                         @Valid @RequestBody CommentRequest commentRequest,
                                                         @CurrentUser UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());
        Comment currentComment = commentService.getCommentById(id);

        // Check if the current user is the author of the comment
        if (!currentComment.getAuthor().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(null);
        }

        Comment updatedComment = commentService.updateComment(id, commentRequest.getContent());

        return ResponseEntity.ok(convertToCommentResponse(updatedComment));
    }

    @DeleteMapping("/comments/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse> deleteComment(@PathVariable Long id,
                                                     @CurrentUser UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());
        Comment currentComment = commentService.getCommentById(id);

        // Check if the current user is the author of the comment
        if (!currentComment.getAuthor().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "You don't have permission to delete this comment"));
        }

        commentService.deleteComment(id);

        return ResponseEntity.ok(new ApiResponse(true, "Comment deleted successfully"));
    }

    @GetMapping("/users/{username}/comments")
    public PagedResponse<CommentResponse> getCommentsByUsername(
            @PathVariable String username,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {

        User user = userService.getUserByUsername(username);
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Comment> comments = commentService.getCommentsByUser(user.getId(), pageable);

        List<CommentResponse> content = comments.getContent().stream()
                .map(this::convertToCommentResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(content, comments.getNumber(), comments.getSize(),
                comments.getTotalElements(), comments.getTotalPages(), comments.isLast());
    }

    private CommentResponse convertToCommentResponse(Comment comment) {
        CommentResponse commentResponse = new CommentResponse();
        commentResponse.setId(comment.getId());
        commentResponse.setContent(comment.getContent());
        commentResponse.setCreatedAt(comment.getCreatedAt());
        commentResponse.setUpdatedAt(comment.getUpdatedAt());

        commentResponse.setUsername(comment.getAuthor().getUsername());
        commentResponse.setUserId(comment.getAuthor().getId());
        commentResponse.setUserAvatar(comment.getAuthor().getAvatarUrl());

        commentResponse.setPostId(comment.getPost().getId());
        commentResponse.setPostTitle(comment.getPost().getTitle());

        if (comment.getParentComment() != null) {
            commentResponse.setParentId(comment.getParentComment().getId());
        }

        return commentResponse;
    }
}