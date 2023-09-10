package com.devsuperior.dscatalog.services;

import com.devsuperior.dscatalog.dto.ProductDTO;
import com.devsuperior.dscatalog.entities.Category;
import com.devsuperior.dscatalog.entities.Product;
import com.devsuperior.dscatalog.repositories.CategoryRepository;
import com.devsuperior.dscatalog.repositories.ProductRepository;
import com.devsuperior.dscatalog.services.exceptions.DatabaseException;
import com.devsuperior.dscatalog.services.exceptions.ResourceNotFoundException;
import com.devsuperior.dscatalog.tests.Factory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class ProductServiceTests {

    @InjectMocks
    private ProductService service;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private long existingId;
    private long nonExistingId;
    private long dependentId;
    private PageImpl<Product> page;
    private Product product;

    private Category category;
    private ProductDTO productDTO;

    @BeforeEach
    void setup() throws Exception {
        existingId = 1L;
        nonExistingId = 2L;
        dependentId = 3L;
        product = Factory.createProduct();
        productDTO = Factory.createProductDTO();
        category = Factory.createCategory();
        page = new PageImpl<>(List.of(product));

        doNothing().when(productRepository).deleteById(existingId);
        doThrow(DataIntegrityViolationException.class).when(productRepository).deleteById(dependentId);

        when(productRepository.existsById(existingId)).thenReturn(true);
        when(productRepository.existsById(nonExistingId)).thenReturn(false);
        when(productRepository.existsById(dependentId)).thenReturn(true);

        when(productRepository.findAll((Pageable) ArgumentMatchers.any())).thenReturn(page);
        when(productRepository.findById(existingId)).thenReturn(Optional.of(product));
        when(productRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        when(productRepository.getReferenceById(existingId)).thenReturn(product);
        when(productRepository.getReferenceById(nonExistingId)).thenThrow(EntityNotFoundException.class);

        when(categoryRepository.getReferenceById(existingId)).thenReturn(category);
        when(categoryRepository.getReferenceById(nonExistingId)).thenThrow(EntityNotFoundException.class);
    }

    @Test
    public void findAllPagedShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<ProductDTO> result = service.findAllPaged(pageable);

        assertNotNull(result);
        verify(productRepository).findAll(pageable);
    }

    @Test
    public void findByIdShouldReturnProductDTOWhenIdExists() {
        ProductDTO product = service.findById(existingId);

        Assertions.assertNotNull(product);
    }

    @Test
    public void findByIdShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {
        assertThrows(ResourceNotFoundException.class, () -> {
            service.findById(nonExistingId);
        });
    }

    @Test
    public void updateShouldReturnProductDTOWhenIdExists() {
        ProductDTO result = service.update(existingId, productDTO);

        Assertions.assertNotNull(result);
    }

    @Test
    public void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExists() {
        assertThrows(ResourceNotFoundException.class, () -> {
            service.update(nonExistingId, productDTO);
        });
    }

    @Test
    public void deleteShouldDoNothingWhenIdExists() {
        assertDoesNotThrow(() -> {
            service.delete(existingId);
        });
    }

    @Test
    public void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {
        assertThrows(ResourceNotFoundException.class, () -> {
            service.delete(nonExistingId);
        });
    }

    @Test
    public void deleteShouldThrowDatabaseExceptionWhenDependentId() {
        assertThrows(DatabaseException.class, () -> {
            service.delete(dependentId);
        });
    }
}
