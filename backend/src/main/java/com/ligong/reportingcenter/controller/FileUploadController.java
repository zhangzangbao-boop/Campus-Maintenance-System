package com.ligong.reportingcenter.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/upload")
@Slf4j
public class FileUploadController {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @PostMapping("/images")
    public List<ImageUploadResponse> uploadImages(@RequestParam("files") MultipartFile[] files) {
        List<ImageUploadResponse> responses = new ArrayList<>();
        
        try {
            // 创建上传目录
            Path path = Paths.get(uploadPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    // 生成唯一文件名
                    String originalFilename = file.getOriginalFilename();
                    String extension = "";
                    if (originalFilename != null && originalFilename.contains(".")) {
                        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    }
                    String filename = UUID.randomUUID().toString() + extension;
                    
                    // 保存文件
                    Path filePath = path.resolve(filename);
                    Files.write(filePath, file.getBytes());
                    
                    // 返回文件信息
                    String fileUrl = "/uploads/" + filename;
                    responses.add(new ImageUploadResponse(fileUrl, filename));
                }
            }
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
        
        return responses;
    }
    
    public static class ImageUploadResponse {
        private String url;
        private String name;
        
        public ImageUploadResponse(String url, String name) {
            this.url = url;
            this.name = name;
        }
        
        public String getUrl() {
            return url;
        }
        
        public String getName() {
            return name;
        }
    }
}