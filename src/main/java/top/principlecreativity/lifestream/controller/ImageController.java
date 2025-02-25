package top.principlecreativity.lifestream.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import top.principlecreativity.lifestream.entity.Album;
import top.principlecreativity.lifestream.entity.Image;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.payload.ApiResponse;
import top.principlecreativity.lifestream.payload.ImageResponse;
import top.principlecreativity.lifestream.payload.PagedResponse;
import top.principlecreativity.lifestream.security.CurrentUser;
import top.principlecreativity.lifestream.security.UserPrincipal;
import top.principlecreativity.lifestream.service.AlbumService;
import top.principlecreativity.lifestream.service.FileStorageService;
import top.principlecreativity.lifestream.service.UserService;
import top.principlecreativity.lifestream.util.AppConstants;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserService userService;

    @Autowired
    private AlbumService albumService;

    // 添加这个新方法来获取当前用户的图片
    @GetMapping("/user/me")
    @PreAuthorize("hasRole('USER')")
    public PagedResponse<ImageResponse> getCurrentUserImages(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @CurrentUser UserPrincipal currentUser) {

        User user = userService.getUserById(currentUser.getId());
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "uploadedAt");
        Page<Image> images = fileStorageService.getImagesByUser(user, pageable);

        List<ImageResponse> content = images.getContent().stream()
                .map(this::convertToImageResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(content, images.getNumber(), images.getSize(),
                images.getTotalElements(), images.getTotalPages(), images.isLast());
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ImageResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "albumId", required = false) Long albumId,
            @CurrentUser UserPrincipal currentUser) {

        User user = userService.getUserById(currentUser.getId());

        Image image = fileStorageService.storeFile(file, description, user, albumId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(image.getId()).toUri();

        return ResponseEntity.created(location).body(convertToImageResponse(image));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImageResponse> getImage(@PathVariable Long id) {
        Image image = fileStorageService.getImage(id);
        return ResponseEntity.ok(convertToImageResponse(image));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadImage(@PathVariable Long id, HttpServletRequest request) throws IOException {
        Image image = fileStorageService.getImage(id);

        Path path = Paths.get(image.getPath());
        Resource resource = new org.springframework.core.io.UrlResource(path.toUri());

        // Try to determine file's content type
        String contentType = image.getContentType();
        if(contentType == null) {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        }

        // Fallback to a default content type if type could not be determined
        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + image.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/user/{username}")
    public PagedResponse<ImageResponse> getImagesByUsername(
            @PathVariable String username,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {

        User user = userService.getUserByUsername(username);
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "uploadedAt");
        Page<Image> images = fileStorageService.getImagesByUser(user, pageable);

        List<ImageResponse> content = images.getContent().stream()
                .map(this::convertToImageResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(content, images.getNumber(), images.getSize(),
                images.getTotalElements(), images.getTotalPages(), images.isLast());
    }

    @GetMapping("/album/{albumId}")
    public PagedResponse<ImageResponse> getImagesByAlbum(
            @PathVariable Long albumId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {

        Album album = albumService.getAlbumById(albumId);
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "uploadedAt");
        Page<Image> images = fileStorageService.getImagesByAlbum(album, pageable);

        List<ImageResponse> content = images.getContent().stream()
                .map(this::convertToImageResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(content, images.getNumber(), images.getSize(),
                images.getTotalElements(), images.getTotalPages(), images.isLast());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse> deleteImage(@PathVariable Long id,
                                                   @CurrentUser UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());
        Image image = fileStorageService.getImage(id);

        // Check if the current user is the uploader of the image
        if (!image.getUploader().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "You don't have permission to delete this image"));
        }

        fileStorageService.deleteImage(id);

        return ResponseEntity.ok(new ApiResponse(true, "Image deleted successfully"));
    }

    // 添加这个新方法处理图片更新
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateImage(
            @PathVariable Long id,
            @RequestBody ImageUpdateRequest updateRequest,
            @CurrentUser UserPrincipal currentUser) {

        User user = userService.getUserById(currentUser.getId());
        Image image = fileStorageService.getImage(id);

        // Check if the current user is the uploader of the image
        if (!image.getUploader().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "You don't have permission to update this image"));
        }

        // Update image description
        if (updateRequest.getDescription() != null) {
            image.setDescription(updateRequest.getDescription());
        }

        // Update album if provided
        if (updateRequest.getAlbumId() != null) {
            Album album = albumService.getAlbumById(updateRequest.getAlbumId());
            image.setAlbum(album);
        }

        Image updatedImage = fileStorageService.updateImage(image);

        return ResponseEntity.ok(convertToImageResponse(updatedImage));
    }

    private ImageResponse convertToImageResponse(Image image) {
        ImageResponse imageResponse = new ImageResponse();
        imageResponse.setId(image.getId());
        imageResponse.setFilename(image.getFilename());
        imageResponse.setContentType(image.getContentType());
        imageResponse.setSize(image.getFileSize());
        imageResponse.setDescription(image.getDescription());
        imageResponse.setUploadedAt(image.getUploadedAt());

        imageResponse.setUploaderName(image.getUploader().getUsername());
        imageResponse.setUploaderId(image.getUploader().getId());

        if (image.getAlbum() != null) {
            imageResponse.setAlbumId(image.getAlbum().getId());
            imageResponse.setAlbumName(image.getAlbum().getName());
        }

        // Create download URL
        String downloadUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/images/download/")
                .path(image.getId().toString())
                .toUriString();

        imageResponse.setDownloadUrl(downloadUrl);

        return imageResponse;
    }
}

// 添加这个请求类来支持图片更新
@Getter
class ImageUpdateRequest {
    // Getters and Setters
    private String description;
    private Long albumId;

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
    }
}