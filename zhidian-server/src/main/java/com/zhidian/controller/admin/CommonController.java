package com.zhidian.controller.admin;

import com.zhidian.result.Result;
//import com.sky.utils.AliOssUtil;
import com.zhidian.service.TencentService;
import com.zhidian.utils.LocalFileUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Tag(name = "通用接口")
@Slf4j
public class CommonController {

    @Autowired
    private TencentService tencent;
    
    @Autowired
    private LocalFileUtil localFileUtil;


    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @Operation(summary="文件上传")
    public Result<String> upload(MultipartFile file) throws IOException {
        log.info("文件上传：{}",file);
        //原始文件名
        String originalFilename = file.getOriginalFilename();
        //截取原始文件名的后缀   dfdfdf.png
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        //构造新文件名称
        String objectName = UUID.randomUUID().toString() + extension;

        //使用腾讯云存储
        String filePath = tencent.uploadImage(file, objectName);

        //使用本地存储
        //String filePath = localFileUtil.uploadFile(file, "images/");

        return Result.success(filePath);

    }
}