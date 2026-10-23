package com.simplehearing.sharedmedia;

import com.simplehearing.sharedmedia.dto.SharedMediaResponse;
import com.simplehearing.sharedmedia.entity.SharedMedia;
import com.simplehearing.storage.StorageService;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Turns posts into responses — resolves uploader/reader names and presigns file links in one batch. */
@Component
public class SharedMediaMapper {

    private final UserRepository userRepository;
    private final StorageService storageService;

    public SharedMediaMapper(UserRepository userRepository, StorageService storageService) {
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    /** {@code patientNames} (patient id → name) is optional; pass an empty map when the list is for one child. */
    public List<SharedMediaResponse> toResponses(List<SharedMedia> items, Map<UUID, String> patientNames) {
        Set<UUID> userIds = new HashSet<>();
        for (SharedMedia m : items) {
            userIds.add(m.getUploadedBy());
            if (m.getReadBy() != null) userIds.add(m.getReadBy());
        }
        Map<UUID, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return items.stream().map(m -> toResponse(m, users, patientNames)).toList();
    }

    private SharedMediaResponse toResponse(SharedMedia m, Map<UUID, User> users, Map<UUID, String> patientNames) {
        User uploader = users.get(m.getUploadedBy());
        String uploaderName = uploader != null ? uploader.getFirstName() + " " + uploader.getLastName() : "Unknown";
        User reader = m.getReadBy() != null ? users.get(m.getReadBy()) : null;
        String readerName = reader != null ? reader.getFirstName() + " " + reader.getLastName() : null;
        String url = m.getFileUrl() != null ? storageService.presign(m.getFileUrl(), Duration.ofHours(1)) : null;
        return SharedMediaResponse.from(m, uploaderName,
                uploader != null ? uploader.getRole() : null,
                uploader != null ? uploader.customRoleOrNull() : null,
                url, readerName, patientNames.get(m.getPatientId()));
    }
}
