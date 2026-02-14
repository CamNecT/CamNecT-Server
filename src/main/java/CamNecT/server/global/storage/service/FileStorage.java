package CamNecT.server.global.storage.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    String save(String prefix, MultipartFile file);
    Resource loadAsResource(String storageKey);
    void delete(String storageKey);
}
