package com.shf.lock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shf.lock.pojo.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    @Update("update tb_stock set count=count-#{count} where product_code=#{productCode} and warehouse=#{warehouse} and count>=#{count}")
    int updateStock(@Param("productCode") String productCode,@Param("warehouse") String warehouse, @Param("count") Integer count);

    @Select("select * from tb_stock where product_code=#{productCode} for update")
    List<Stock> queryStock(@Param("productCode") String productCode);
}
