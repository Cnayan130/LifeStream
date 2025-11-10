package top.principlecreativity.lifestream.controller.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import top.principlecreativity.lifestream.entity.Album;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.payload.AlbumRequest;
import top.principlecreativity.lifestream.payload.AlbumResponse;
import top.principlecreativity.lifestream.payload.ApiResponse;
import top.principlecreativity.lifestream.payload.PagedResponse;
import top.principlecreativity.lifestream.security.CurrentUser;
import top.principlecreativity.lifestream.security.UserPrincipal;
import top.principlecreativity.lifestream.service.AlbumService;
import top.principlecreativity.lifestream.service.UserService;
import top.principlecreativity.lifestream.util.AppConstants;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/albums")
public class AlbumController {

    @Autowired
    private AlbumService albumService;

    @Autowired
    private UserService userService;

    @GetMapping
    public PagedResponse<AlbumResponse> getAlbums(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Album> albums = albumService.getAllAlbums(pageable);

        List<AlbumResponse> content = albums.getContent().stream()
                .map(this::convertToAlbumResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(content, albums.getNumber(), albums.getSize(),
                albums.getTotalElements(), albums.getTotalPages(), albums.isLast());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlbumResponse> getAlbumById(@PathVariable Long id) {
        Album album = albumService.getAlbumById(id);
        return ResponseEntity.ok(convertToAlbumResponse(album));
    }

    @GetMapping("/user/{username}")
    public PagedResponse<AlbumResponse> getAlbumsByUsername(
            @PathVariable String username,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {

        User user = userService.getUserByUsername(username);
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Album> albums = albumService.getAlbumsByUser(user, pageable);

        List<AlbumResponse> content = albums.getContent().stream()
                .map(this::convertToAlbumResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(content, albums.getNumber(), albums.getSize(),
                albums.getTotalElements(), albums.getTotalPages(), albums.isLast());
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AlbumResponse> createAlbum(@Valid @RequestBody AlbumRequest albumRequest,
                                                     @CurrentUser UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());

        Album album = new Album();
        album.setName(albumRequest.getName());
        album.setDescription(albumRequest.getDescription());

        Album newAlbum = albumService.createAlbum(album, user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(newAlbum.getId()).toUri();

        return ResponseEntity.created(location).body(convertToAlbumResponse(newAlbum));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AlbumResponse> updateAlbum(@PathVariable Long id,
                                                     @Valid @RequestBody AlbumRequest albumRequest,
                                                     @CurrentUser UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());
        Album currentAlbum = albumService.getAlbumById(id);

        // Check if the current user is the creator of the album
        if (!currentAlbum.getCreator().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(null);
        }

        Album album = new Album();
        album.setName(albumRequest.getName());
        album.setDescription(albumRequest.getDescription());

        Album updatedAlbum = albumService.updateAlbum(id, album);

        return ResponseEntity.ok(convertToAlbumResponse(updatedAlbum));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse> deleteAlbum(@PathVariable Long id,
                                                   @CurrentUser UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());
        Album currentAlbum = albumService.getAlbumById(id);

        // Check if the current user is the creator of the album
        if (!currentAlbum.getCreator().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "You don't have permission to delete this album"));
        }

        albumService.deleteAlbum(id);

        return ResponseEntity.ok(new ApiResponse(true, "Album deleted successfully"));
    }

    private AlbumResponse convertToAlbumResponse(Album album) {
        AlbumResponse albumResponse = new AlbumResponse();
        albumResponse.setId(album.getId());
        albumResponse.setName(album.getName());
        albumResponse.setDescription(album.getDescription());
        albumResponse.setCreatedAt(album.getCreatedAt());
        albumResponse.setUpdatedAt(album.getUpdatedAt());

        albumResponse.setCreatorName(album.getCreator().getUsername());
        albumResponse.setCreatorId(album.getCreator().getId());

        albumResponse.setImageCount(album.getImages() != null ? album.getImages().size() : 0);

        return albumResponse;
    }
}