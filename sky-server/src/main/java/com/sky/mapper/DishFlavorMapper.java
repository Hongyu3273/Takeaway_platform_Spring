package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;



@Mapper
public interface DishFlavorMapper {

    void insertBatch(List<DishFlavor> flavors);


    /**
     * 根据菜品id删除口味
     * @param dishId
     */
    @Delete("DELETE from dish_flavor where dish_id = #{dishId}")
    void deleteByDishId(Long dishId);


    /**
     * 根据菜品id查询对应的口味数据
     * @param id
     * @return
     */
    @Select(("select * from dish_flavor where dish_id = #{dishiId}"))
    List<DishFlavor> getByDishId(Long id);
}
