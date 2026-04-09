package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface DishService {

    /**
     * 新增菜品及其对应口味
     *
     * @param dishDTO
     */
    public void saveWithFlavor(DishDTO dishDTO);


    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 菜品批量删除
     *
     * @param ids
     */

    void deleteBatch(List<Long> ids);


    /**
     * 根据ID查找口味数据
     *
     * @return
     */
    DishVO getByIdWithFlavor(Long id);

    /**
     * 修改菜品信息及对应口味信息
     *
     * @param dishDTO
     */
    void updateWithFlavor(DishDTO dishDTO);


    /**
     * 启售停售菜品
     *
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);


    /**
     * 根据分类id查询菜品
     *
     * @param dish
     * @return
     */
    List<Dish> list(Long dish);


    /**
     * 条件查询菜品和口味
     *
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);
}


