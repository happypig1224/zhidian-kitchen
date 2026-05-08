package com.zhidian;

import com.zhidian.entity.DishFlavor;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/10 13:38
 */
public class ZhiWeiMain {
    public static void main(String[] args) {
        List<DishFlavor> flavors = new ArrayList<>();
        StringBuilder sb=new StringBuilder();
        flavors.add(DishFlavor.builder().name("辣度").value("不要辣").build());
        flavors.add(DishFlavor.builder().name("辣度").value("微辣").build());
        flavors.add(DishFlavor.builder().name("辣度").value("重辣").build());
        flavors.add(DishFlavor.builder().name("甜度").value("不要甜").build());
        sb.append("口味:");
        for (int i = 0; i < flavors.size(); i++) {
            if(i==0){
                sb.append(flavors.get(i).getValue() + ",");
            }else{
                sb.append("," + flavors.get(i).getValue());
            }
        }
        System.out.println("sb = " + sb);
    }
}
