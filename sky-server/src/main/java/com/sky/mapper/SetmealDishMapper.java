package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品ID查询关联套餐
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdsBySetmealId(List<Long> dishIds);
}
