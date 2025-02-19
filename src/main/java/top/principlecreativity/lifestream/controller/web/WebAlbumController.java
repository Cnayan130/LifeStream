package top.principlecreativity.lifestream.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import top.principlecreativity.lifestream.entity.Album;
import top.principlecreativity.lifestream.entity.Image;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.security.CurrentUser;
import top.principlecreativity.lifestream.security.UserPrincipal;
import top.principlecreativity.lifestream.service.AlbumService;
import top.principlecreativity.lifestream.service.FileStorageService;
import top.principlecreativity.lifestream.service.UserService;
import top.principlecreativity.lifestream.util.AppConstants;

@Controller
public class WebAlbumController {

    @Autowired
    private AlbumService albumService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserService userService;

    @GetMapping("/albums")
    public String getAllAlbums(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(required = false) String sort,
            Model model) {

        Sort.Direction direction = Sort.Direction.DESC;
        String sortBy = "createdAt";

        if ("oldest".equals(sort)) {
            direction = Sort.Direction.ASC;
        } else if ("name".equals(sort)) {
            sortBy = "name";
            direction = Sort.Direction.ASC;
        } else if ("popular".equals(sort)) {
            // 这需要在AlbumService中自定义实现
            sortBy = "imageCount";
        }

        Pageable pageable = PageRequest.of(page, size, direction, sortBy);
        Page<Album> albums = albumService.getAllAlbums(pageable);

        model.addAttribute("albumPage", albums);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", albums.getTotalPages());
        model.addAttribute("paginationUrl", "/albums" + (sort != null ? "?sort=" + sort : ""));

        return "albums/list";
    }

    @GetMapping("/albums/{id}")
    public String getAlbumById(
            @PathVariable Long id,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            Model model,
            @CurrentUser UserPrincipal currentUser) {

        Album album = albumService.getAlbumById(id);

        // 检查当前用户是否是创建者
        boolean isOwner = false;
        if (currentUser != null && album.getCreator() != null) {
            isOwner = album.getCreator().getId().equals(currentUser.getId());
        }
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("album", album);

        // 获取相册中的图片
        Pageable pageable = PageRequest.of(page, size);
        Page<Image> imagesPage = fileStorageService.getImagesByAlbum(album, pageable);

        model.addAttribute("images", imagesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", imagesPage.getTotalPages());

        return "albums/detail";
    }

    @GetMapping("/albums/user/{username}")
    public String getAlbumsByUsername(
            @PathVariable String username,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            Model model) {

        User user = userService.getUserByUsername(username);
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Album> albums = albumService.getAlbumsByUser(user, pageable);

        model.addAttribute("albumPage", albums);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", albums.getTotalPages());
        model.addAttribute("paginationUrl", "/albums/user/" + username);
        model.addAttribute("username", username);

        return "albums/list";
    }

    @GetMapping("/albums/new")
    public String newAlbumForm() {
        return "albums/form";
    }
}