package top.principlecreativity.lifestream.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import top.principlecreativity.lifestream.entity.Post;
import top.principlecreativity.lifestream.entity.Tag;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.payload.*;
import top.principlecreativity.lifestream.security.CurrentUser;
import top.principlecreativity.lifestream.security.UserPrincipal;
import top.principlecreativity.lifestream.service.PostService;
import top.principlecreativity.lifestream.service.UserService;
import top.principlecreativity.lifestream.util.AppConstants;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @GetMapping
    public PagedResponse<PostResponse> getPosts(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Post> posts = postService.getAllPublishedPosts(pageable);

        List<PostResponse> content = posts.getContent().stream()
                .map(this::convertToPostResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(content, posts.getNumber(), posts.getSize(),
                posts.getTotalElements(), posts.getTotalPages(), posts.isLast());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long id) {
        Post post = postService.getPostById(id);
        return ResponseEntity.ok(convertToPostResponse(post));
    }

    @GetMapping("/user/{username}")
    public PagedResponse<PostResponse> getPostsByUsername(
            @PathVariable String username,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {

        User user = userService.getUserByUsername(username);
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Post> posts = postService.getPostsByAuthor(user, pageable);

        List<PostResponse> content = posts.getContent().stream()
                .map(this::convertToPostResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(content, posts.getNumber(), posts.getSize(),
                posts.getTotalElements(), posts.getTotalPages(), posts.isLast());
    }

    @GetMapping("/tag/{tag}")
    public PagedResponse<PostResponse> getPostsByTag(
            @PathVariable String tag,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Post> posts = postService.getPostsByTag(tag, pageable);

        List<PostResponse> content = posts.getContent().stream()
                .map(this::convertToPostResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(content, posts.getNumber(), posts.getSize(),
                posts.getTotalElements(), posts.getTotalPages(), posts.isLast());
    }

    @GetMapping("/archives/{year}/{month}")
    public PagedResponse<PostResponse> getPostsByYearAndMonth(
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Post> posts = postService.getPostsByYearAndMonth(year, month, pageable);

        List<PostResponse> content = posts.getContent().stream()
                .map(this::convertToPostResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(content, posts.getNumber(), posts.getSize(),
                posts.getTotalElements(), posts.getTotalPages(), posts.isLast());
    }

    @GetMapping("/search")
    public PagedResponse<PostResponse> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Post> posts = postService.searchPosts(keyword, pageable);

        List<PostResponse> content = posts.getContent().stream()
                .map(this::convertToPostResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(content, posts.getNumber(), posts.getSize(),
                posts.getTotalElements(), posts.getTotalPages(), posts.isLast());
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody PostRequest postRequest,
                                                   @CurrentUser UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());

        Post post = new Post();
        post.setTitle(postRequest.getTitle());
        post.setContent(postRequest.getContent());
        post.setSummary(postRequest.getSummary());
        post.setCoverImage(postRequest.getCoverImage());
        post.setPublished(postRequest.isPublished());

        Post newPost = postService.createPost(post, postRequest.getTags(), user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(newPost.getId()).toUri();

        return ResponseEntity.created(location).body(convertToPostResponse(newPost));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long id,
                                                   @Valid @RequestBody PostRequest postRequest,
                                                   @CurrentUser UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());
        Post currentPost = postService.getPostById(id);

        // Check if the current user is the author of the post
        if (!currentPost.getAuthor().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(null);
        }

        Post post = new Post();
        post.setTitle(postRequest.getTitle());
        post.setContent(postRequest.getContent());
        post.setSummary(postRequest.getSummary());
        post.setCoverImage(postRequest.getCoverImage());
        post.setPublished(postRequest.isPublished());

        Post updatedPost = postService.updatePost(id, post, postRequest.getTags());

        return ResponseEntity.ok(convertToPostResponse(updatedPost));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse> deletePost(@PathVariable Long id,
                                                  @CurrentUser UserPrincipal currentUser) {
        // 获取文章
        Post post = postService.getPostById(id);

        // 检查当前用户是否是作者
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "您没有权限删除这篇文章"));
        }

        // 执行删除
        postService.deletePost(id);
        return ResponseEntity.ok(new ApiResponse(true, "文章删除成功"));
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PostResponse> togglePublishStatus(@PathVariable Long id,
                                                            @CurrentUser UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());
        Post currentPost = postService.getPostById(id);

        // Check if the current user is the author of the post
        if (!currentPost.getAuthor().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(null);
        }

        Post updatedPost = postService.togglePublishStatus(id);

        return ResponseEntity.ok(convertToPostResponse(updatedPost));
    }

    private PostResponse convertToPostResponse(Post post) {
        PostResponse postResponse = new PostResponse();
        postResponse.setId(post.getId());
        postResponse.setTitle(post.getTitle());
        postResponse.setContent(post.getContent());
        postResponse.setSummary(post.getSummary());
        postResponse.setCoverImage(post.getCoverImage());
        postResponse.setPublished(post.isPublished());
        postResponse.setCreatedAt(post.getCreatedAt());
        postResponse.setUpdatedAt(post.getUpdatedAt());

        UserProfile author = new UserProfile(
                post.getAuthor().getId(),
                post.getAuthor().getUsername(),
                post.getAuthor().getEmail(),
                post.getAuthor().getCreatedAt(),
                post.getAuthor().getBio(),
                post.getAuthor().getAvatarUrl()
        );
        postResponse.setAuthor(author);

        postResponse.setTags(post.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet()));

        return postResponse;
    }

}
