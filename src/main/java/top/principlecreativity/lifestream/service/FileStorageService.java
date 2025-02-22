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
     * Store avatar image for a user
     * @param file The image file to store
     * @param user The user who is uploading the avatar
     * @return The stored image entity
     */
    @Transactional
    public Image storeAvatar(MultipartFile file, User user) {
        // Normalize file name
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        // Check if the file's name contains invalid characters
        if (fileName.contains("..")) {
            throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
        }

        // Generate unique file name with avatar prefix
        String fileExtension = "";
        if (fileName.contains(".")) {
            fileExtension = fileName.substring(fileName.lastIndexOf("."));
        }
        String uniqueFileName = "avatar_" + user.getId() + "_" + UUID.randomUUID().toString() + fileExtension;

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
            image.setDescription("Profile avatar for " + user.getUsername());
            image.setUploader(user);

            return imageRepository.save(image);

        } catch (IOException ex) {
            throw new FileStorageException("Could not store avatar file " + fileName + ". Please try again!", ex);
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
}