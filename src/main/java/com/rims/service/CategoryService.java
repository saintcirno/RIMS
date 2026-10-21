package com.rims.service;

import com.rims.model.Category;
import com.rims.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug);
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category getOrCreateOtherCategory() {
        return categoryRepository.findBySlug("other")
                .orElseGet(() -> categoryRepository.save(new Category(
                        "Other",
                        "other",
                        "General equipment, specialized tools, accessories, and unclassified rental assets.",
                        "package"
                )));
    }
}
