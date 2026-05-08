package com.zhidian.controller.admin;

import com.alibaba.excel.EasyExcel;
import com.zhidian.excellistener.ExcelReadListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/admin/rag")
@Slf4j
public class RAGController {
    private final VectorStore vectorStore;

    public RAGController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostMapping("/import")
    public String importDocuments(@RequestParam("file") MultipartFile file) throws IOException {
        EasyExcel.read(file.getInputStream(), new ExcelReadListener(vectorStore)).sheet(0).doRead();
        return "success";
    }
}
