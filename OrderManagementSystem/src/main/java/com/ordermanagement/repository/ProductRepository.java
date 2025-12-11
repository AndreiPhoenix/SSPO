package com.ordermanagement.repository;

import com.ordermanagement.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Найти товары по названию (регистронезависимый поиск)
    List<Product> findByNameContainingIgnoreCase(String name);

    // Найти товары по диапазону цен
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // Найти товары с количеством больше указанного
    List<Product> findByQuantityGreaterThan(Integer quantity);

    // Найти товары с определенным статусом наличия
    @Query("SELECT p FROM Product p WHERE p.quantity > 0 ORDER BY p.name")
    List<Product> findAvailableProducts();

    // Найти товары по ID склада
    List<Product> findByWarehouseId(Long warehouseId);

    // Проверить существование товара по названию
    boolean existsByName(String name);

    // Найти товары по названию и цене
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.price <= :maxPrice")
    List<Product> findByNameAndMaxPrice(@Param("name") String name, @Param("maxPrice") BigDecimal maxPrice);

    // Получить общее количество товаров на складе
    @Query("SELECT SUM(p.quantity) FROM Product p")
    Integer getTotalStockQuantity();

    // Получить общую стоимость всех товаров на складе
    @Query("SELECT SUM(p.price * p.quantity) FROM Product p")
    BigDecimal getTotalStockValue();

    // Резервирование товара (уменьшение количества)
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.quantity = p.quantity - :quantity WHERE p.id = :productId AND p.quantity >= :quantity")
    int reserveProduct(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    // Освобождение товара (увеличение количества)
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.quantity = p.quantity + :quantity WHERE p.id = :productId")
    int releaseProduct(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    // Обновление цены товара
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.price = :price WHERE p.id = :productId")
    int updateProductPrice(@Param("productId") Long productId, @Param("price") BigDecimal price);

    // Обновление количества товара
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.quantity = :quantity WHERE p.id = :productId")
    int updateProductQuantity(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    // Поиск товаров по нескольким критериям
    @Query("SELECT p FROM Product p WHERE " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:minQuantity IS NULL OR p.quantity >= :minQuantity) AND " +
            "(:warehouseId IS NULL OR p.warehouseId = :warehouseId) " +
            "ORDER BY p.name")
    List<Product> searchProducts(
            @Param("name") String name,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minQuantity") Integer minQuantity,
            @Param("warehouseId") Long warehouseId);

    // Получить товары с низким запасом
    @Query("SELECT p FROM Product p WHERE p.quantity <= :threshold ORDER BY p.quantity")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);

    // Проверить доступность товара в нужном количестве
    @Query("SELECT CASE WHEN p.quantity >= :requiredQuantity THEN true ELSE false END " +
            "FROM Product p WHERE p.id = :productId")
    Optional<Boolean> checkAvailability(@Param("productId") Long productId,
                                        @Param("requiredQuantity") Integer requiredQuantity);

    // Получить количество конкретного товара
    @Query("SELECT p.quantity FROM Product p WHERE p.id = :productId")
    Optional<Integer> getProductQuantity(@Param("productId") Long productId);

    // Найти товары по списку ID
    @Query("SELECT p FROM Product p WHERE p.id IN :productIds")
    List<Product> findProductsByIds(@Param("productIds") List<Long> productIds);

    // Получить статистику по товарам
    @Query("SELECT p.warehouseId, COUNT(p) as productCount, SUM(p.quantity) as totalQuantity, " +
            "SUM(p.price * p.quantity) as totalValue " +
            "FROM Product p " +
            "GROUP BY p.warehouseId")
    List<Object[]> getWarehouseStatistics();
}