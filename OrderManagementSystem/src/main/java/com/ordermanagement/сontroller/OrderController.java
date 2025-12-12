package com.ordermanagement.сontroller;

import com.ordermanagement.dto.OrderDTO;
import com.ordermanagement.dto.PaymentDTO;
import com.ordermanagement.entity.Order.OrderStatus;
import com.ordermanagement.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Управление заказами", description = "API для работы с заказами")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Получить все заказы")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<OrderDTO> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить заказ по ID")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        OrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Получить заказы покупателя")
    public ResponseEntity<List<OrderDTO>> getOrdersByCustomerId(@PathVariable Long customerId) {
        List<OrderDTO> orders = orderService.getOrdersByCustomerId(customerId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Получить заказы по статусу")
    public ResponseEntity<List<OrderDTO>> getOrdersByStatus(@PathVariable OrderStatus status) {
        List<OrderDTO> orders = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    @Operation(summary = "Создать новый заказ")
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody OrderDTO orderDTO) {
        OrderDTO createdOrder = orderService.createOrder(orderDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @PostMapping("/{orderId}/reserve")
    @Operation(summary = "Зарезервировать товары для заказа")
    public ResponseEntity<OrderDTO> reserveOrderItems(@PathVariable Long orderId) {
        OrderDTO reservedOrder = orderService.reserveOrderItems(orderId);
        return ResponseEntity.ok(reservedOrder);
    }

    @PostMapping("/{orderId}/pay")
    @Operation(summary = "Обработать оплату заказа")
    public ResponseEntity<OrderDTO> processOrderPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentDTO paymentDTO) {
        OrderDTO paidOrder = orderService.processOrderPayment(orderId, paymentDTO);
        return ResponseEntity.ok(paidOrder);
    }

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Обновить статус заказа")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {
        OrderDTO updatedOrder = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Отменить заказ")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }
}