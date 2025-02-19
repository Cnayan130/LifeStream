package top.principlecreativity.lifestream.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.principlecreativity.lifestream.entity.Album;
import top.principlecreativity.lifestream.entity.Image;
import top.principlecreativity.lifestream.entity.User;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    Page<Image> findByUploader(User uploader, Pageable pageable);
    Page<Image> findByAlbum(Album album, Pageable pageable);
}