package com.ordermanagement.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ordermanagement.entity.Order.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDTO {

    private Long id;

    @NotNull(message = "ID покупателя обязательно")
    private Long customerId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderDate;

    private OrderStatus status;

    private BigDecimal totalAmount;

    @Size(max = 1000, message = "Примечания не должны превышать 1000 символов")
    private String notes;

    @NotEmpty(message = "Заказ должен содержать хотя бы один товар")
    @Valid
    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {

        @NotNull(message = "ID товара обязательно")
        private Long productId;

        @NotNull(message = "Количество обязательно")
        @Min(value = 1, message = "Количество должно быть не менее 1")
        private Integer quantity;

        @NotNull(message = "Цена обязательна")
        @DecimalMin(value = "0.01", message = "Цена должна быть больше 0")
        private BigDecimal unitPrice;
    }
}