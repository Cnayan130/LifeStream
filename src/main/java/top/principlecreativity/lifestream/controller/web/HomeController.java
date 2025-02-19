package top.principlecreativity.lifestream.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import top.principlecreativity.lifestream.entity.Album;
import top.principlecreativity.lifestream.entity.Post;
import top.principlecreativity.lifestream.entity.Tag;
import top.principlecreativity.lifestream.service.AlbumService;
import top.principlecreativity.lifestream.service.PostService;
import top.principlecreativity.lifestream.service.TagService;

import java.util.List;

@Controller
public class HomeController {
    @Autowired
    private PostService postService;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private TagService tagService;

    @GetMapping("/")
    public String home(Model model) {
        try {
            // 获取精选文章
            List<Post> featuredPosts = postService.getFeaturedPosts(6);
            model.addAttribute("featuredPosts", featuredPosts);

            // 获取最新相册
            List<Album> recentAlbums = albumService.getRecentAlbums(4);
            model.addAttribute("recentAlbums", recentAlbums);

            // 获取热门标签
            List<Tag> popularTags = tagService.getPopularTags(10);
            model.addAttribute("popularTags", popularTags);

            return "index";
        } catch (Exception e) {
            // 添加错误日志
            e.printStackTrace();
            return "error";
        }
    }
}