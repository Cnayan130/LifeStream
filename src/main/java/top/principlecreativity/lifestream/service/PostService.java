package top.principlecreativity.lifestream.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.principlecreativity.lifestream.entity.Post;
import top.principlecreativity.lifestream.entity.Role;
import top.principlecreativity.lifestream.entity.Tag;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.exception.ResourceNotFoundException;
import top.principlecreativity.lifestream.repository.PostRepository;
import top.principlecreativity.lifestream.repository.TagRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TagService tagService;

    public Page<Post> getAllPublishedPosts(Pageable pageable) {
        return postRepository.findByPublishedTrue(pageable);
    }

    public Page<Post> getPostsByAuthor(User author, Pageable pageable) {
        return postRepository.findByAuthor(author, pageable);
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
    }

    public Page<Post> searchPosts(String keyword, Pageable pageable) {
        return postRepository.findByTitleContainingAndPublishedTrue(keyword, pageable);
    }

    public Page<Post> getPostsByTag(String tagName, Pageable pageable) {
        return postRepository.findByTagName(tagName, pageable);
    }

    public Page<Post> getPostsByYearAndMonth(int year, int month, Pageable pageable) {
        return postRepository.findByYearAndMonth(year, month, pageable);
    }

    public List<Integer> getAllPublishedYears() {
        return postRepository.findAllPublishedYears();
    }

    /**
     * 获取精选文章
     * @param limit 数量限制
     * @return 精选文章列表
     */
    public List<Post> getFeaturedPosts(int limit) {
        // 这里可以根据你的业务逻辑来实现"精选"的定义
        // 例如：最近发布的、评论最多的、浏览量最高的等
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> featuredPosts = postRepository.findByPublishedTrue(pageable);
        return featuredPosts.getContent();
    }

    /**
     * 获取最近发布的文章
     * @param limit 数量限制
     * @return 最近文章列表
     */
    public List<Post> getRecentPosts(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findByPublishedTrue(pageable).getContent();
    }

    /**
     * 获取相关文章
     * @param post 当前文章
     * @param limit 数量限制
     * @return 相关文章列表
     */
    public List<Post> getRelatedPosts(Post post, int limit) {
        if (post.getTags() == null || post.getTags().isEmpty()) {
            // 如果没有标签，返回最近的文章
            return getRecentPosts(limit);
        }

        // 找出有相同标签的文章
        Set<String> tagNames = post.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        List<Post> relatedPosts = new ArrayList<>();
        for (String tagName : tagNames) {
            Page<Post> posts = postRepository.findByTagName(tagName, PageRequest.of(0, limit));
            relatedPosts.addAll(posts.getContent());
            if (relatedPosts.size() >= limit) {
                break;
            }
        }

        // 移除当前文章
        relatedPosts = relatedPosts.stream()
                .filter(p -> !p.getId().equals(post.getId()))
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());

        // 如果相关文章不足，补充最近文章
        if (relatedPosts.size() < limit) {
            List<Post> recentPosts = getRecentPosts(limit * 2);
            List<Post> finalRelatedPosts = relatedPosts;
            recentPosts = recentPosts.stream()
                    .filter(p -> !p.getId().equals(post.getId()) &&
                            !finalRelatedPosts.contains(p))
                    .limit(limit - relatedPosts.size())
                    .toList();

            relatedPosts.addAll(recentPosts);
        }

        return relatedPosts;
    }

    /**
     * 获取上一篇文章
     * @param currentPost 当前文章
     * @return 上一篇文章，如果没有则返回null
     */
    public Post getPreviousPost(Post currentPost) {
        return postRepository.findFirstByCreatedAtBeforeAndPublishedTrueOrderByCreatedAtDesc(
                currentPost.getCreatedAt());
    }

    /**
     * 获取下一篇文章
     * @param currentPost 当前文章
     * @return 下一篇文章，如果没有则返回null
     */
    public Post getNextPost(Post currentPost) {
        return postRepository.findFirstByCreatedAtAfterAndPublishedTrueOrderByCreatedAt(
                currentPost.getCreatedAt());
    }

    @Transactional
    public Post createPost(Post post, Set<String> tagNames, User author) {
        post.setAuthor(author);

        // Process tags
        Set<Tag> tags = new HashSet<>();
        if (tagNames != null && !tagNames.isEmpty()) {
            tagNames.forEach(name -> {
                Tag tag = tagService.getOrCreateTag(name);
                tags.add(tag);
            });
        }
        post.setTags(tags);

        return postRepository.save(post);
    }

    @Transactional
    public Post updatePost(Long id, Post postDetails, Set<String> tagNames) {
        Post post = getPostById(id);

        post.setTitle(postDetails.getTitle());
        post.setContent(postDetails.getContent());
        post.setSummary(postDetails.getSummary());
        post.setCoverImage(postDetails.getCoverImage());
        post.setPublished(postDetails.isPublished());

        // Update tags if provided
        if (tagNames != null) {
            Set<Tag> tags = new HashSet<>();
            tagNames.forEach(name -> {
                Tag tag = tagService.getOrCreateTag(name);
                tags.add(tag);
            });
            post.setTags(tags);
        }

        return postRepository.save(post);
    }

    @Transactional
    public void deletePost(Long id) {
        Post post = getPostById(id);
        postRepository.delete(post);
    }

    @Transactional
    public Post togglePublishStatus(Long id) {
        Post post = getPostById(id);
        post.setPublished(!post.isPublished());
        return postRepository.save(post);
    }

    public Page<Post> getVisiblePosts(User currentUser, Pageable pageable) {
        if (currentUser == null) {
            return postRepository.findByPublishedTrue(pageable);
        }

        // 如果是管理员，返回所有文章
        if (currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == Role.ERole.ROLE_ADMIN)) {
            return postRepository.findAll(pageable);
        }

        // 返回当前用户的所有文章和其他人的已发布文章
        return postRepository.findByAuthorOrPublishedTrue(currentUser, true, pageable);
    }
}
