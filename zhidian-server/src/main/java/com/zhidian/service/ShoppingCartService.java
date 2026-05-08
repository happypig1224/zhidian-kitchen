package com.zhidian.service;

import com.zhidian.dto.ShoppingCartDTO;
import com.zhidian.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> showShoppingCart();

    void cleanShoppingCart();

    void subShopping(ShoppingCartDTO shoppingCartDTO);

    void addShoppingCartByUserId(ShoppingCartDTO shoppingCartDTO, Long userId);
}
