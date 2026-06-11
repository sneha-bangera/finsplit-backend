package com.finsplit.finsplit.service;

import com.finsplit.finsplit.dto.ExpenseDTOs.CategoryResponse;
import com.finsplit.finsplit.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getIcon()))
                .toList();
    }
}
