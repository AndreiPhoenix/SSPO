package com.ordermanagement.dto;

import com.ordermanagement.entity.Payment.PaymentMethod;
import com.ordermanagement.entity.Payment.PaymentStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentDTO {

    private Long id;

    @NotNull(message = "ID заказа обязательно")
    private Long orderId;

    @NotNull(message = "Сумма обязательна")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
    private BigDecimal amount;

    @NotNull(message = "Способ оплаты обязателен")
    private PaymentMethod paymentMethod;

    private PaymentStatus status;

    @Size(max = 100, message = "ID транзакции не должен превышать 100 символов")
    private String transactionId;

    @Size(max = 500, message = "Примечания не должны превышать 500 символов")
    private String notes;
}