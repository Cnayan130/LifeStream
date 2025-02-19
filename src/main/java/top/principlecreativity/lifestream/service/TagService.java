package top.principlecreativity.lifestream.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.principlecreativity.lifestream.entity.Tag;
import top.principlecreativity.lifestream.repository.TagRepository;

import java.util.List;

@Service
public class TagService {

    @Autowired
    private TagRepository tagRepository;

    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    public List<Tag> getPopularTags(int limit) {
        return tagRepository.findPopularTags(PageRequest.of(0, limit));
    }

    @Transactional
    public Tag getOrCreateTag(String name) {
        return tagRepository.findByName(name)
                .orElseGet(() -> {
                    Tag newTag = new Tag();
                    newTag.setName(name);
                    return tagRepository.save(newTag);
                });
    }

    @Transactional
    public void deleteUnusedTags() {
        List<Tag> allTags = tagRepository.findAll();
        allTags.forEach(tag -> {
            if (tag.getPosts() == null || tag.getPosts().isEmpty()) {
                tagRepository.delete(tag);
            }
        });
    }
}
