package com.shf.lock.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
@TableName("tb_stock")
public class Stock {
//    private Integer stock = 5000;

    private Long id;

    private String productCode;

    private String warehouse;

    private Integer count;

//    @Version
    private Integer version;
}
