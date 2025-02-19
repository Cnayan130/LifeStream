package top.principlecreativity.lifestream.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import top.principlecreativity.lifestream.entity.Tag;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);

    @Query("SELECT t FROM Tag t JOIN t.posts p WHERE p.published = true GROUP BY t ORDER BY COUNT(p) DESC")
    List<Tag> findPopularTags(Pageable pageable);
}
