package top.principlecreativity.lifestream.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.principlecreativity.lifestream.entity.Album;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.exception.ResourceNotFoundException;
import top.principlecreativity.lifestream.repository.AlbumRepository;

import java.util.List;

@Service
public class AlbumService {

    @Autowired
    private AlbumRepository albumRepository;

    public Page<Album> getAllAlbums(Pageable pageable) {
        return albumRepository.findAll(pageable);
    }

    public Page<Album> getAlbumsByUser(User user, Pageable pageable) {
        return albumRepository.findByCreator(user, pageable);
    }

    public Album getAlbumById(Long id) {
        return albumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Album", "id", id));
    }

    @Transactional
    public Album createAlbum(Album album, User creator) {
        album.setCreator(creator);
        return albumRepository.save(album);
    }

    @Transactional
    public Album updateAlbum(Long id, Album albumDetails) {
        Album album = getAlbumById(id);

        album.setName(albumDetails.getName());
        album.setDescription(albumDetails.getDescription());

        return albumRepository.save(album);
    }

    @Transactional
    public void deleteAlbum(Long id) {
        Album album = getAlbumById(id);
        albumRepository.delete(album);
    }

    /**
     * 获取最新相册
     * @param limit 数量限制
     * @return 最新相册列表
     */
    public List<Album> getRecentAlbums(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return albumRepository.findAll(pageable).getContent();
    }

    // Add this method to AlbumService.java

    /**
     * Count albums created by a specific user
     * @param user The user whose albums to count
     * @return Count of albums
     */
    public long countAlbumsByUser(User user) {
        return albumRepository.countByCreator(user);
    }
}