package com.sky.test;

import com.github.pagehelper.Page;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.impl.DishServiceImpl;
import com.sky.vo.DishVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DishServiceImpl.
 *
 * Scope: service-layer logic only.
 * All mapper dependencies are mocked — no Spring context, no DB.
 *
 * Run: mvn test -pl sky-server -Dtest=DishServiceImplTest
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DishService — unit tests")
class DishServiceImplTest {

    // ── mocks ──────────────────────────────────────────────────────────────
    @Mock private DishMapper         dishMapper;
    @Mock private DishFlavorMapper   dishFlavorMapper;
    @Mock private SetmealMapper      setmealMapper;
    @Mock private SetmealDishMapper  setmealDishMapper;

    @InjectMocks
    private DishServiceImpl dishService;

    // ── shared fixtures ────────────────────────────────────────────────────
    private Dish       sampleDish;
    private DishFlavor sampleFlavor;
    private DishDTO    sampleDishDTO;

    @BeforeEach
    void setUp() {
        sampleDish = Dish.builder()
                .id(1L)
                .name("Kung Pao Chicken")
                .categoryId(10L)
                .price(new BigDecimal("28.00"))
                .status(StatusConstant.DISABLE)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        sampleFlavor = DishFlavor.builder()
                .id(1L)
                .dishId(1L)
                .name("Spiciness")
                .value("[\"mild\",\"medium\",\"hot\"]")
                .build();

        sampleDishDTO = new DishDTO();
        sampleDishDTO.setName("Kung Pao Chicken");
        sampleDishDTO.setCategoryId(10L);
        sampleDishDTO.setPrice(new BigDecimal("28.00"));
        sampleDishDTO.setStatus(StatusConstant.DISABLE);
        sampleDishDTO.setFlavors(List.of(sampleFlavor));
    }




    // saveWithFlavor
    @Nested
    @DisplayName("saveWithFlavor")
    class SaveWithFlavor {

        @Test
        @DisplayName("inserts dish and its flavors in a single transaction")
        void savesWithFlavors() {
            // simulate mapper setting the generated key on the entity
            doAnswer(inv -> {
                Dish d = inv.getArgument(0);
                d.setId(99L);
                return null;
            }).when(dishMapper).insert(any(Dish.class));

            dishService.saveWithFlavor(sampleDishDTO);

            verify(dishMapper, times(1)).insert(any(Dish.class));

            ArgumentCaptor<List<DishFlavor>> captor = ArgumentCaptor.forClass(List.class);
            verify(dishFlavorMapper, times(1)).insertBatch(captor.capture());

            List<DishFlavor> savedFlavors = captor.getValue();
            assertThat(savedFlavors).hasSize(1);
            // flavor must carry the newly generated dish id
            assertThat(savedFlavors.get(0).getDishId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("skips flavor insert when flavor list is null")
        void skipsFlavorInsertWhenNull() {
            sampleDishDTO.setFlavors(null);
            doAnswer(inv -> { ((Dish) inv.getArgument(0)).setId(99L); return null; })
                    .when(dishMapper).insert(any(Dish.class));

            dishService.saveWithFlavor(sampleDishDTO);

            verify(dishMapper, times(1)).insert(any(Dish.class));
            verify(dishFlavorMapper, never()).insertBatch(any());
        }

        @Test
        @DisplayName("skips flavor insert when flavor list is empty")
        void skipsFlavorInsertWhenEmpty() {
            sampleDishDTO.setFlavors(Collections.emptyList());
            doAnswer(inv -> { ((Dish) inv.getArgument(0)).setId(99L); return null; })
                    .when(dishMapper).insert(any(Dish.class));

            dishService.saveWithFlavor(sampleDishDTO);

            verify(dishFlavorMapper, never()).insertBatch(any());
        }
    }




    // pageQuery
    @Nested
    @DisplayName("pageQuery")
    class PageQueryTests {

        @Test
        @DisplayName("returns total count and current page records from mapper")
        void returnsPaginatedResult() {
            DishPageQueryDTO queryDTO = new DishPageQueryDTO();
            queryDTO.setPage(1);
            queryDTO.setPageSize(10);

            DishVO dishVO = new DishVO();
            dishVO.setId(1L);
            dishVO.setName("Kung Pao Chicken");

            Page<DishVO> mockPage = new Page<>();
            mockPage.setTotal(3L);
            mockPage.addAll(List.of(dishVO));

            when(dishMapper.pageQuery(queryDTO)).thenReturn(mockPage);

            PageResult result = dishService.pageQuery(queryDTO);

            assertThat(result.getTotal()).isEqualTo(3L);
            assertThat(result.getRecords()).hasSize(1);
        }
    }




    // deleteBatch
    @Nested
    @DisplayName("deleteBatch")
    class DeleteBatch {

        @Test
        @DisplayName("deletes dishes and their flavors when all checks pass")
        void deletesSuccessfully() {
            Dish disabledDish = Dish.builder()
                    .id(1L).status(StatusConstant.DISABLE).build();

            when(dishMapper.getById(1L)).thenReturn(disabledDish);
            when(setmealDishMapper.getSetmealIdsByDishIds(List.of(1L)))
                    .thenReturn(Collections.emptyList());

            dishService.deleteBatch(List.of(1L));

            verify(dishMapper,       times(1)).deleteById(1L);
            verify(dishFlavorMapper, times(1)).deleteByDishId(1L);
        }

        @Test
        @DisplayName("throws DeletionNotAllowedException when dish is on sale")
        void throwsWhenDishIsOnSale() {
            Dish onSaleDish = Dish.builder()
                    .id(1L).status(StatusConstant.ENABLE).build();
            when(dishMapper.getById(1L)).thenReturn(onSaleDish);

            assertThatThrownBy(() -> dishService.deleteBatch(List.of(1L)))
                    .isInstanceOf(DeletionNotAllowedException.class)
                    .hasMessage(MessageConstant.DISH_ON_SALE);

            verify(dishMapper,       never()).deleteById(any());
            verify(dishFlavorMapper, never()).deleteByDishId(any());
        }

        @Test
        @DisplayName("throws DeletionNotAllowedException when dish is linked to a setmeal")
        void throwsWhenDishLinkedToSetmeal() {
            Dish disabledDish = Dish.builder()
                    .id(1L).status(StatusConstant.DISABLE).build();
            when(dishMapper.getById(1L)).thenReturn(disabledDish);
            when(setmealDishMapper.getSetmealIdsByDishIds(List.of(1L)))
                    .thenReturn(List.of(100L));

            assertThatThrownBy(() -> dishService.deleteBatch(List.of(1L)))
                    .isInstanceOf(DeletionNotAllowedException.class)
                    .hasMessage(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);

            verify(dishMapper,       never()).deleteById(any());
            verify(dishFlavorMapper, never()).deleteByDishId(any());
        }

        @Test
        @DisplayName("deletes multiple dishes in a single batch")
        void deletesMultipleDishes() {
            Dish d1 = Dish.builder().id(1L).status(StatusConstant.DISABLE).build();
            Dish d2 = Dish.builder().id(2L).status(StatusConstant.DISABLE).build();

            when(dishMapper.getById(1L)).thenReturn(d1);
            when(dishMapper.getById(2L)).thenReturn(d2);
            when(setmealDishMapper.getSetmealIdsByDishIds(List.of(1L, 2L)))
                    .thenReturn(Collections.emptyList());

            dishService.deleteBatch(List.of(1L, 2L));

            verify(dishMapper,       times(1)).deleteById(1L);
            verify(dishMapper,       times(1)).deleteById(2L);
            verify(dishFlavorMapper, times(1)).deleteByDishId(1L);
            verify(dishFlavorMapper, times(1)).deleteByDishId(2L);
        }
    }





    // getByIdWithFlavor
    @Nested
    @DisplayName("getByIdWithFlavor")
    class GetByIdWithFlavor {

        @Test
        @DisplayName("returns DishVO with dish data and its flavors merged")
        void returnsDishVOWithFlavors() {
            when(dishMapper.getById(1L)).thenReturn(sampleDish);
            when(dishFlavorMapper.getByDishId(1L)).thenReturn(List.of(sampleFlavor));

            DishVO result = dishService.getByIdWithFlavor(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Kung Pao Chicken");
            assertThat(result.getFlavors()).hasSize(1);
            assertThat(result.getFlavors().get(0).getName()).isEqualTo("Spiciness");
        }

        @Test
        @DisplayName("returns DishVO with empty flavor list when no flavors exist")
        void returnsDishVOWithNoFlavors() {
            when(dishMapper.getById(1L)).thenReturn(sampleDish);
            when(dishFlavorMapper.getByDishId(1L)).thenReturn(Collections.emptyList());

            DishVO result = dishService.getByIdWithFlavor(1L);

            assertThat(result.getFlavors()).isEmpty();
        }
    }





    // updateWithFlavor
    @Nested
    @DisplayName("updateWithFlavor")
    class UpdateWithFlavor {

        @Test
        @DisplayName("updates dish, deletes old flavors, inserts new ones")
        void updatesWithNewFlavors() {
            sampleDishDTO.setId(1L);
            DishFlavor newFlavor = DishFlavor.builder()
                    .name("Temperature").value("[\"cold\",\"warm\",\"hot\"]").build();
            sampleDishDTO.setFlavors(List.of(newFlavor));

            dishService.updateWithFlavor(sampleDishDTO);

            verify(dishMapper,       times(1)).update(any(Dish.class));
            verify(dishFlavorMapper, times(1)).deleteByDishId(1L);

            ArgumentCaptor<List<DishFlavor>> captor = ArgumentCaptor.forClass(List.class);
            verify(dishFlavorMapper, times(1)).insertBatch(captor.capture());
            assertThat(captor.getValue().get(0).getDishId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("deletes old flavors but skips insert when new list is empty")
        void deletesOldFlavorsWhenNewListIsEmpty() {
            sampleDishDTO.setId(1L);
            sampleDishDTO.setFlavors(Collections.emptyList());

            dishService.updateWithFlavor(sampleDishDTO);

            verify(dishMapper,       times(1)).update(any(Dish.class));
            verify(dishFlavorMapper, times(1)).deleteByDishId(1L);
            verify(dishFlavorMapper, never()).insertBatch(any());
        }
    }





    // startOrStop
    @Nested
    @DisplayName("startOrStop")
    class StartOrStop {

        @Test
        @DisplayName("enables a dish without touching setmeals")
        void enablesDish() {
            dishService.startOrStop(StatusConstant.ENABLE, 1L);

            ArgumentCaptor<Dish> captor = ArgumentCaptor.forClass(Dish.class);
            verify(dishMapper, times(1)).update(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(StatusConstant.ENABLE);

            // enabling a dish must NOT affect any setmeal
            verify(setmealDishMapper, never()).getSetmealIdsByDishIds(any());
            verify(setmealMapper,     never()).update(any());
        }

        @Test
        @DisplayName("disables dish and cascades to linked setmeals")
        void disablesDishAndCascadesToSetmeals() {
            when(setmealDishMapper.getSetmealIdsByDishIds(List.of(1L)))
                    .thenReturn(List.of(100L, 200L));

            dishService.startOrStop(StatusConstant.DISABLE, 1L);

            // dish itself is updated
            ArgumentCaptor<Dish> dishCaptor = ArgumentCaptor.forClass(Dish.class);
            verify(dishMapper, times(1)).update(dishCaptor.capture());
            assertThat(dishCaptor.getValue().getStatus()).isEqualTo(StatusConstant.DISABLE);

            // linked setmeals are also disabled
            ArgumentCaptor<Setmeal> setmealCaptor = ArgumentCaptor.forClass(Setmeal.class);
            verify(setmealMapper, times(2)).update(setmealCaptor.capture());

            List<Setmeal> updatedSetmeals = setmealCaptor.getAllValues();
            assertThat(updatedSetmeals)
                    .extracting(Setmeal::getStatus)
                    .containsOnly(StatusConstant.DISABLE);
            assertThat(updatedSetmeals)
                    .extracting(Setmeal::getId)
                    .containsExactlyInAnyOrder(100L, 200L);
        }

        @Test
        @DisplayName("disables dish with no linked setmeals — no setmeal update fired")
        void disablesDishWithNoSetmeals() {
            when(setmealDishMapper.getSetmealIdsByDishIds(List.of(1L)))
                    .thenReturn(Collections.emptyList());

            dishService.startOrStop(StatusConstant.DISABLE, 1L);

            verify(dishMapper,    times(1)).update(any(Dish.class));
            verify(setmealMapper, never()).update(any());
        }
    }



    // list (by categoryId)
    @Nested
    @DisplayName("list")
    class ListByCategoryId {

        @Test
        @DisplayName("queries only ENABLE dishes under the given category")
        void queriesEnabledDishesOnly() {
            when(dishMapper.list(any(Dish.class))).thenReturn(List.of(sampleDish));

            List<Dish> result = dishService.list(10L);

            assertThat(result).hasSize(1);

            ArgumentCaptor<Dish> captor = ArgumentCaptor.forClass(Dish.class);
            verify(dishMapper, times(1)).list(captor.capture());
            assertThat(captor.getValue().getCategoryId()).isEqualTo(10L);
            assertThat(captor.getValue().getStatus()).isEqualTo(StatusConstant.ENABLE);
        }

        @Test
        @DisplayName("returns empty list when no dishes found")
        void returnsEmptyList() {
            when(dishMapper.list(any(Dish.class))).thenReturn(Collections.emptyList());

            List<Dish> result = dishService.list(99L);

            assertThat(result).isEmpty();
        }
    }




    // listWithFlavor
    @Nested
    @DisplayName("listWithFlavor")
    class ListWithFlavor {

        @Test
        @DisplayName("merges flavors into each DishVO")
        void mergesFlavorsIntoDishVOs() {
            Dish d1 = Dish.builder().id(1L).name("Dish A").build();
            Dish d2 = Dish.builder().id(2L).name("Dish B").build();

            DishFlavor f1 = DishFlavor.builder().dishId(1L).name("Spice").build();
            DishFlavor f2 = DishFlavor.builder().dishId(2L).name("Sugar").build();

            when(dishMapper.list(any(Dish.class))).thenReturn(Arrays.asList(d1, d2));
            when(dishFlavorMapper.getByDishId(1L)).thenReturn(List.of(f1));
            when(dishFlavorMapper.getByDishId(2L)).thenReturn(List.of(f2));

            Dish queryDish = new Dish();
            List<DishVO> result = dishService.listWithFlavor(queryDish);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getFlavors().get(0).getName()).isEqualTo("Spice");
            assertThat(result.get(1).getFlavors().get(0).getName()).isEqualTo("Sugar");

            // flavor mapper called once per dish
            verify(dishFlavorMapper, times(1)).getByDishId(1L);
            verify(dishFlavorMapper, times(1)).getByDishId(2L);
        }

        @Test
        @DisplayName("returns empty list when no dishes match the query")
        void returnsEmptyWhenNoDishes() {
            when(dishMapper.list(any(Dish.class))).thenReturn(Collections.emptyList());

            List<DishVO> result = dishService.listWithFlavor(new Dish());

            assertThat(result).isEmpty();
            verify(dishFlavorMapper, never()).getByDishId(any());
        }
    }
}
