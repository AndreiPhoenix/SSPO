package com.ordermanagement.service;

import com.ordermanagement.dto.OrderDTO;
import com.ordermanagement.dto.PaymentDTO;
import com.ordermanagement.entity.*;
import com.ordermanagement.exception.*;
import com.ordermanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Процесс 1: Приём заказа
     */
    @Transactional
    public OrderDTO createOrder(OrderDTO orderDTO) {
        log.info("Создание нового заказа для покупателя ID: {}", orderDTO.getCustomerId());

        // Валидация данных покупателя
        Customer customer = customerRepository.findById(orderDTO.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Покупатель не найден с id: " + orderDTO.getCustomerId()));

        // Проверка корректности товаров
        validateOrderItems(orderDTO.getItems());

        // Создание заказа в статусе "Новый"
        Order order = convertToEntity(orderDTO);
        order.setCustomer(customer);
        order.setStatus(Order.OrderStatus.NEW);

        // Сохранение в базу данных
        Order savedOrder = orderRepository.save(order);
        log.info("Заказ создан с ID: {}", savedOrder.getId());

        return convertToDTO(savedOrder);
    }

    /**
     * Процесс 2: Проверка и резервирование
     */
    @Transactional
    public OrderDTO reserveOrderItems(Long orderId) throws ReservationException {
        log.info("Резервирование товаров для заказа ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ не найден с id: " + orderId));

        if (order.getStatus() != Order.OrderStatus.NEW) {
            throw new IllegalStateException("Невозможно резервировать товары для заказа в статусе: " + order.getStatus());
        }

        List<Reservation> reservations = new ArrayList<>();

        // Для каждого товара в заказе:
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            Integer requiredQuantity = item.getQuantity();

            // Проверить наличие на складе
            if (!productService.checkProductAvailability(product.getId(), requiredQuantity)) {
                throw new InsufficientStockException(
                        "Недостаточно товара: " + product.getName() +
                                ". Доступно: " + product.getQuantity() +
                                ", требуется: " + requiredQuantity);
            }

            // Если достаточно - зарезервировать
            boolean reserved = productService.reserveProductQuantity(product.getId(), requiredQuantity);
            if (!reserved) {
                throw new ReservationException("Не удалось зарезервировать товар: " + product.getName());
            }

            // Создаем запись о резервировании
            Reservation reservation = Reservation.builder()
                    .order(order)
                    .product(product)
                    .quantityReserved(requiredQuantity)
                    .status(Reservation.ReservationStatus.ACTIVE)
                    .build();

            reservations.add(reservation);
        }

        // Сохраняем все резервирования
        reservationRepository.saveAll(reservations);

        // Обновить статус заказа
        order.setStatus(Order.OrderStatus.RESERVED);
        Order updatedOrder = orderRepository.save(order);

        log.info("Товары для заказа ID: {} успешно зарезервированы", orderId);

        return convertToDTO(updatedOrder);
    }

    /**
     * Процесс 3: Обработка оплаты
     */
    @Transactional
    public OrderDTO processOrderPayment(Long orderId, PaymentDTO paymentDTO) {
        log.info("Обработка оплаты для заказа ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ не найден с id: " + orderId));

        if (order.getStatus() != Order.OrderStatus.RESERVED) {
            throw new IllegalStateException("Невозможно оплатить заказ в статусе: " + order.getStatus());
        }

        // Отправить запрос в платёжную систему (имитация)
        boolean paymentSuccess = processPaymentThroughGateway(paymentDTO);

        if (paymentSuccess) {
            // Обработать ответ
            Payment payment = Payment.builder()
                    .order(order)
                    .amount(order.getTotalAmount())
                    .paymentMethod(paymentDTO.getPaymentMethod())
                    .status(Payment.PaymentStatus.COMPLETED)
                    .transactionId(paymentDTO.getTransactionId())
                    .notes(paymentDTO.getNotes())
                    .build();

            paymentRepository.save(payment);

            // Обновить статус оплаты
            order.setStatus(Order.OrderStatus.PAID);

            // Обновить статус резервирований
            List<Reservation> reservations = reservationRepository.findByOrderId(orderId);
            reservations.forEach(r -> r.setStatus(Reservation.ReservationStatus.COMPLETED));
            reservationRepository.saveAll(reservations);

            Order updatedOrder = orderRepository.save(order);

            log.info("Оплата для заказа ID: {} успешно обработана", orderId);

            return convertToDTO(updatedOrder);
        } else {
            // Если оплата не прошла
            throw new PaymentException("Оплата не прошла. Пожалуйста, проверьте данные и попробуйте снова.");
        }
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ не найден с id: " + id));
        return convertToDTO(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ не найден с id: " + orderId));

        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);

        return convertToDTO(updatedOrder);
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ не найден с id: " + orderId));

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Заказ уже отменен");
        }

        // Освобождаем резервирования
        List<Reservation> reservations = reservationRepository.findByOrderId(orderId);
        for (Reservation reservation : reservations) {
            if (reservation.getStatus() == Reservation.ReservationStatus.ACTIVE) {
                productService.releaseProductQuantity(
                        reservation.getProduct().getId(),
                        reservation.getQuantityReserved()
                );
                reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
            }
        }
        reservationRepository.saveAll(reservations);

        // Обновляем статус заказа
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("Заказ ID: {} отменен", orderId);
    }

    private void validateOrderItems(List<OrderDTO.OrderItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Заказ должен содержать хотя бы один товар");
        }

        for (OrderDTO.OrderItemDTO itemDTO : items) {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Товар не найден с id: " + itemDTO.getProductId()));

            if (itemDTO.getQuantity() <= 0) {
                throw new IllegalArgumentException("Количество товара должно быть больше 0");
            }

            if (itemDTO.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Цена товара должна быть больше 0");
            }

            // Проверяем, совпадает ли цена с ценой товара в базе
            if (itemDTO.getUnitPrice().compareTo(product.getPrice()) != 0) {
                log.warn("Цена товара ID: {} в заказе ({}) отличается от цены в базе ({})",
                        product.getId(), itemDTO.getUnitPrice(), product.getPrice());
            }
        }
    }

    private boolean processPaymentThroughGateway(PaymentDTO paymentDTO) {
        // Имитация вызова платежного шлюза
        // В реальной системе здесь был бы вызов API платежной системы
        log.info("Имитация вызова платежного шлюза для суммы: {}", paymentDTO.getAmount());

        // Возвращаем true в 95% случаев для тестирования
        return Math.random() > 0.05;
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setCustomerId(order.getCustomer().getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setNotes(order.getNotes());

        if (order.getItems() != null) {
            List<OrderDTO.OrderItemDTO> itemDTOs = order.getItems().stream()
                    .map(item -> {
                        OrderDTO.OrderItemDTO itemDTO = new OrderDTO.OrderItemDTO();
                        itemDTO.setProductId(item.getProduct().getId());
                        itemDTO.setQuantity(item.getQuantity());
                        itemDTO.setUnitPrice(item.getUnitPrice());
                        return itemDTO;
                    })
                    .collect(Collectors.toList());
            dto.setItems(itemDTOs);
        }

        return dto;
    }

    private Order convertToEntity(OrderDTO dto) {
        Order order = Order.builder()
                .status(dto.getStatus() != null ? dto.getStatus() : Order.OrderStatus.NEW)
                .notes(dto.getNotes())
                .build();

        if (dto.getItems() != null) {
            for (OrderDTO.OrderItemDTO itemDTO : dto.getItems()) {
                Product product = productRepository.findById(itemDTO.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Товар не найден с id: " + itemDTO.getProductId()));

                OrderItem item = OrderItem.builder()
                        .product(product)
                        .quantity(itemDTO.getQuantity())
                        .unitPrice(itemDTO.getUnitPrice())
                        .build();

                order.addItem(item);
            }
        }

        return order;
    }

    // Внедряем ProductService
    private final ProductService productService;
}