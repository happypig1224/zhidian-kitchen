package com.zhidian.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地文件存储工具类
 */
@Component
@Slf4j
public class LocalFileUtil {

    @Value("${sky.local-file.base-path:/upload}")
    private String basePath;

    @Value("${sky.local-file.base-url:http://localhost:8081/upload}")
    private String baseUrl;

    /**
     * 上传文件到本地存储
     * @param file 文件
     * @param dir 存储目录
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, String dir) {
        try {
            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            // 获取文件扩展名
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString().replaceAll("-", "") + extension;
            
            // 创建日期目录
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String relativePath = dir + dateDir + "/" + fileName;
            
            // 创建目标文件路径
            Path targetPath = Paths.get(basePath, relativePath);
            File targetFile = targetPath.toFile();
            
            // 确保目录存在
            File parentDir = targetFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // 保存文件
            file.transferTo(targetFile);
            
            // 返回文件访问URL
            String fileUrl = baseUrl + "/" + relativePath;
            
            log.info("文件上传成功，本地路径: {}, 访问URL: {}", targetPath, fileUrl);
            
            return fileUrl;
        } catch (IOException e) {
            log.error("本地文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 删除本地文件
     * @param fileUrl 文件URL
     * @return 是否删除成功
     */
    public boolean deleteFile(String fileUrl) {
        try {
            // 从URL中提取相对路径
            String relativePath = fileUrl.replace(baseUrl + "/", "");
            Path filePath = Paths.get(basePath, relativePath);
            
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("删除本地文件失败: {}", fileUrl, e);
            return false;
        }
    }

    /**
     * 检查文件是否存在
     * @param fileUrl 文件URL
     * @return 是否存在
     */
    public boolean fileExists(String fileUrl) {
        try {
            String relativePath = fileUrl.replace(baseUrl + "/", "");
            Path filePath = Paths.get(basePath, relativePath);
            return Files.exists(filePath);
        } catch (Exception e) {
            return false;
        }
    }
}