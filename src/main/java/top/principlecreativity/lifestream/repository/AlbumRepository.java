package top.principlecreativity.lifestream.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.principlecreativity.lifestream.entity.Album;
import top.principlecreativity.lifestream.entity.User;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    Page<Album> findByCreator(User creator, Pageable pageable);
    // Add to AlbumRepository.java
    long countByCreator(User creator);
}
