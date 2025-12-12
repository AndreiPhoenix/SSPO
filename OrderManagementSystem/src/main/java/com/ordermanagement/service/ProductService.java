package com.ordermanagement.service;

import com.ordermanagement.dto.ProductDTO;
import com.ordermanagement.entity.Product;
import com.ordermanagement.exception.ResourceNotFoundException;
import com.ordermanagement.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден с id: " + id));
        return convertToDTO(product);
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = convertToEntity(productDTO);
        Product savedProduct = productRepository.save(product);
        return convertToDTO(savedProduct);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден с id: " + id));

        existingProduct.setName(productDTO.getName());
        existingProduct.setDescription(productDTO.getDescription());
        existingProduct.setPrice(productDTO.getPrice());
        existingProduct.setQuantity(productDTO.getQuantity());
        existingProduct.setWarehouseId(productDTO.getWarehouseId());

        Product updatedProduct = productRepository.save(existingProduct);
        return convertToDTO(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Товар не найден с id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> searchProducts(String name, BigDecimal minPrice, BigDecimal maxPrice) {
        if (name != null && minPrice != null && maxPrice != null) {
            return productRepository.findByNameContainingIgnoreCase(name).stream()
                    .filter(p -> p.getPrice().compareTo(minPrice) >= 0 && p.getPrice().compareTo(maxPrice) <= 0)
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else if (name != null) {
            return productRepository.findByNameContainingIgnoreCase(name).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else if (minPrice != null && maxPrice != null) {
            return productRepository.findByPriceBetween(minPrice, maxPrice).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
        return getAllProducts();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getAvailableProducts() {
        return productRepository.findAvailableProducts().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean reserveProductQuantity(Long productId, Integer quantity) {
        int rowsAffected = productRepository.reserveProduct(productId, quantity);
        return rowsAffected > 0;
    }

    @Transactional
    public void releaseProductQuantity(Long productId, Integer quantity) {
        productRepository.releaseProduct(productId, quantity);
    }

    @Transactional(readOnly = true)
    public boolean checkProductAvailability(Long productId, Integer requiredQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден с id: " + productId));
        return product.getQuantity() >= requiredQuantity;
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setQuantity(product.getQuantity());
        dto.setWarehouseId(product.getWarehouseId());
        return dto;
    }

    private Product convertToEntity(ProductDTO dto) {
        return Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .quantity(dto.getQuantity())
                .warehouseId(dto.getWarehouseId())
                .build();
    }
}