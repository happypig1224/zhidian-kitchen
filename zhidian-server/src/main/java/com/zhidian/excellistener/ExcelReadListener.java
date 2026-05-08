package com.zhidian.excellistener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/30 14:23
 */
@Component
@Slf4j
public class ExcelReadListener implements ReadListener<Map<Integer,String>> {
    private List<Map<Integer, String>> dataList = new ArrayList<>();
    private final VectorStore vectorStore;

    public ExcelReadListener(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        dataList.add(data);
        StringBuilder builder = new StringBuilder();
        Set<Map.Entry<Integer, String>> entries = data.entrySet();
        entries.forEach(entry -> {
            String value = entry.getValue();
            if (value != null && !value.trim().isEmpty()) {
                builder.append(value).append("; ");
            }
        });
        
        if (builder.length() > 0) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "excel");
            metadata.put("timestamp", System.currentTimeMillis());
            Document document = new Document(builder.toString(), metadata);
            vectorStore.add(List.of(document));
        }
        
        if (dataList.size() >= 100) {
            processBatch();
            dataList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (!dataList.isEmpty()) {
            processBatch();
        }
        log.info("Excel 数据导入完成，总计{}条记录", dataList.size());
    }

    private void processBatch() {
        log.info("批量处理 {} 行数据", dataList.size());
    }
}
