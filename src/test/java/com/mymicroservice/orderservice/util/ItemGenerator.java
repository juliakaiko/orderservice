package com.mymicroservice.orderservice.util;

import com.mymicroservice.orderservice.model.Item;
import java.math.BigDecimal;

public class ItemGenerator {

    public static Item generateItem() {

        return  Item.builder()
                .name("TestItem")
                .price(BigDecimal.valueOf(100))
                .build();
    }
}
