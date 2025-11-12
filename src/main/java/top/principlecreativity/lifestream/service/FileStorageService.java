package top.principlecreativity.lifestream.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import top.principlecreativity.lifestream.entity.Album;
import top.principlecreativity.lifestream.entity.Image;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.exception.FileStorageException;
import top.principlecreativity.lifestream.exception.ResourceNotFoundException;
import top.principlecreativity.lifestream.repository.AlbumRepository;
import top.principlecreativity.lifestream.repository.ImageRepository;
import top.principlecreativity.lifestream.util.ImageResizeUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final Path fileStorageLocation;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Transactional
    public Image storeFile(MultipartFile file, String description, User uploader, Long albumId) {
        // Normalize file name
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        // Check if the file's name contains invalid characters
        if (fileName.contains("..")) {
            throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
        }

        // Generate unique file name
        String fileExtension = "";
        if (fileName.contains(".")) {
            fileExtension = fileName.substring(fileName.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Create image record
            Image image = new Image();
            image.setFilename(uniqueFileName);
            image.setPath(targetLocation.toString());
            image.setContentType(file.getContentType());
            image.setFileSize(file.getSize());
            image.setDescription(description);
            image.setUploader(uploader);

            // Associate with album if provided
            if (albumId != null) {
                Album album = albumRepository.findById(albumId)
                        .orElseThrow(() -> new ResourceNotFoundException("Album", "id", albumId));
                image.setAlbum(album);
            }

            return imageRepository.save(image);

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Image getImage(Long id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", id));
    }

    public Page<Image> getImagesByUser(User user, Pageable pageable) {
        return imageRepository.findByUploader(user, pageable);
    }

    public Page<Image> getImagesByAlbum(Album album, Pageable pageable) {
        return imageRepository.findByAlbum(album, pageable);
    }

    @Transactional
    public void deleteImage(Long id) {
        Image image = getImage(id);

        // Delete the file from storage
        try {
            Path filePath = Paths.get(image.getPath());
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new FileStorageException("Could not delete file", ex);
        }

        // Delete from database
        imageRepository.delete(image);
    }

    // Add these methods to FileStorageService.java

    /**
     * Store avatar image for a user (Resized to 256x256)
     * @param file The image file to store
     * @param user The user who is uploading the avatar
     * @return The stored image entity
     */
    @Transactional
    public Image storeAvatar(MultipartFile file, User user) {
        // 1. 依然保留文件名的清理检查（为了安全）
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFileName.contains("..")) {
            throw new FileStorageException("Sorry! Filename contains invalid path sequence " + originalFileName);
        }

        // 2. [修改] 生成唯一文件名，强制使用 .jpg 后缀
        // 因为 ImageResizeUtil 会将图片统一转换为 JPG 格式
        String uniqueFileName = "avatar_" + user.getId() + "_" + UUID.randomUUID().toString() + ".jpg";

        try {
            // 3. [新增] 调用工具类进行裁剪和缩放 (256x256)
            // 这会返回处理后的字节数组
            byte[] resizedBytes = ImageResizeUtil.resizeImage(file.getInputStream(), 256, "jpg");

            // 4. [修改] 将处理后的字节数组转换为输入流，用于保存
            ByteArrayInputStream inputStream = new ByteArrayInputStream(resizedBytes);

            // 5. 复制文件到目标位置 (覆盖同名文件)
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 6. 创建数据库记录
            Image image = new Image();
            image.setFilename(uniqueFileName);
            image.setPath(targetLocation.toString());

            // [修改] ContentType 必须是 image/jpeg，因为我们转换了格式
            image.setContentType("image/jpeg");

            // [修改] 大小必须是处理后的字节长度，而不是原始文件大小
            image.setFileSize((long) resizedBytes.length);

            image.setDescription("Profile avatar for " + user.getUsername());
            image.setUploader(user);

            // [可选] 保存原文件名，以便将来参考 (看你的Image实体是否有这个字段)
            // image.setOriginalFileName(originalFileName);

            return imageRepository.save(image);

        } catch (IOException ex) {
            throw new FileStorageException("Could not store avatar file " + uniqueFileName + ". Please try again!", ex);
        }
    }

    /**
     * Count images uploaded by a specific user
     * @param user The user whose images to count
     * @return Count of images
     */
    public long countImagesByUser(User user) {
        return imageRepository.countByUploader(user);
    }

    public Image updateImage(Image image) {
        return imageRepository.save(image);
    }
}