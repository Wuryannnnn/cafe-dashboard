package com.sell.repository;

import com.sell.dataobject.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Integer> {
    List<ExpenseCategory> findAllByOrderBySortOrderAscCategoryIdAsc();
}
