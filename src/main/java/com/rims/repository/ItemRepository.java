package com.rims.repository;

import com.rims.model.Category;
import com.rims.model.Item;
import com.rims.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByItemCode(String itemCode);

    boolean existsByItemCode(String itemCode);

    List<Item> findByActiveTrueOrderByCreatedAtDesc();

    List<Item> findByCategoryAndActiveTrueOrderByCreatedAtDesc(Category category);

    List<Item> findBySellerOrderByCreatedAtDesc(User seller);

    List<Item> findBySellerAndActiveTrueOrderByCreatedAtDesc(User seller);

    @Query("SELECT i FROM Item i WHERE i.active = true AND (" +
           "LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.tags) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.itemCode) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Item> searchItems(@Param("query") String query);

    @Query("SELECT i FROM Item i WHERE i.active = true AND i.category.slug = :categorySlug AND (" +
           "LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.tags) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Item> searchItemsWithCategory(@Param("query") String query, @Param("categorySlug") String categorySlug);

    @Query("SELECT i FROM Item i WHERE i.active = true AND LOWER(i.tags) LIKE LOWER(CONCAT('%', :tag, '%'))")
    List<Item> findByTag(@Param("tag") String tag);

    long countByActiveTrue();
}
