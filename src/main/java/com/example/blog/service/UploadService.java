package com.example.blog.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.example.blog.common.BusinessException;
import com.example.blog.config.OssProperties;
import com.example.blog.entity.Media;
import com.example.blog.mapper.MediaMapper;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
            "image/pjpeg", "image/x-png");

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final OssProperties ossProperties;
    private final MediaMapper mediaMapper;

    @Value("${blog.upload.local-path:./uploads}")
    private String localPath;

    @Value("${blog.upload.max-size:5242880}")
    private long maxSize;

    private Path uploadDir;

    @PostConstruct
    public void init() {
        uploadDir = Paths.get(localPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
            log.info("Upload directory: {}", uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录: " + uploadDir, e);
        }
    }

    public String upload(MultipartFile file) {
        validateFile(file);
        String originalName = file.getOriginalFilename();
        String extension = FileUtil.extName(originalName);
        String fileName = IdUtil.simpleUUID() + (StrUtil.isNotBlank(extension) ? "." + extension.toLowerCase(Locale.ROOT) : "");

        try {
            String url;
            if (ossProperties.isConfigured()) {
                url = uploadToOss(file, fileName);
            } else {
                log.warn("OSS 未配置，使用本地存储。请配置 blog.oss.* 或环境变量 OSS_ACCESS_KEY_ID 等");
                url = uploadToLocal(file, fileName);
            }
            Media media = new Media();
            media.setUrl(url);
            media.setOriginalName(originalName);
            media.setSize(file.getSize());
            media.setCreateTime(LocalDateTime.now());
            mediaMapper.insert(media);
            return url;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    public Path getUploadDir() {
        return uploadDir;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小超出限制");
        }
        String contentType = file.getContentType();
        String extension = FileUtil.extName(file.getOriginalFilename());
        boolean typeOk = contentType != null && ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
        boolean extOk = StrUtil.isNotBlank(extension)
                && ALLOWED_EXT.contains(extension.toLowerCase(Locale.ROOT));
        if (!typeOk && !extOk) {
            throw new BusinessException("不支持的文件类型，仅支持 JPG/PNG/GIF/WebP");
        }
    }

    private String uploadToLocal(MultipartFile file, String fileName) throws IOException {
        Path target = uploadDir.resolve(fileName);
        file.transferTo(target);
        return "/uploads/" + fileName;
    }

    private String uploadToOss(MultipartFile file, String fileName) throws IOException {
        OSS ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret());
        try {
            String objectKey = "blog/" + fileName;
            ossClient.putObject(ossProperties.getBucketName(), objectKey, file.getInputStream());
            String url = ossProperties.resolvePublicUrl(objectKey);
            log.info("OSS upload success: {}", url);
            return url;
        } finally {
            ossClient.shutdown();
        }
    }
}
