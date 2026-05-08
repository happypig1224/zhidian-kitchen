package com.zhidian.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.zhidian.entity.SetmealDish;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/29 22:42
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ContentRowHeight(20)
@HeadRowHeight(25)
public class ExcelSetmeal {
    @ExcelProperty("套餐名称")
    @ColumnWidth(20)
    private String name;
    
    @ExcelProperty("分类名称")
    @ColumnWidth(15)
    private String categoryName;
    
    @ExcelProperty("套餐价格")
    @ColumnWidth(15)
    private String price;
    
    @ExcelProperty("销量")
    @ColumnWidth(15)
    private String salesVolume;
    
    @ExcelProperty("描述信息")
    @ColumnWidth(30)
    private String description;
    
    @ExcelProperty("图片")
    @ColumnWidth(30)
    private String image;
}
