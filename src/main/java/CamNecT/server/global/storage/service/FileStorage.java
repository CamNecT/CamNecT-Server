package CamNecT.server.global.storage.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    String save(String prefix, MultipartFile file);
    void delete(String storageKey);
}
