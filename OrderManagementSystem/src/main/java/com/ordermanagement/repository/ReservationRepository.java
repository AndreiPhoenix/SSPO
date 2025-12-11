package com.ordermanagement.repository;

import com.ordermanagement.entity.Reservation;
import com.ordermanagement.entity.Reservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByOrderId(Long orderId);

    List<Reservation> findByProductId(Long productId);

    List<Reservation> findByStatus(ReservationStatus status);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'ACTIVE' AND r.expirationDate < :now")
    List<Reservation> findExpiredReservations(@Param("now") LocalDateTime now);

    @Query("SELECT SUM(r.quantityReserved) FROM Reservation r WHERE r.product.id = :productId AND r.status = 'ACTIVE'")
    Integer getTotalReservedQuantity(@Param("productId") Long productId);

    @Modifying
    @Query("UPDATE Reservation r SET r.status = 'EXPIRED' WHERE r.id = :id AND r.status = 'ACTIVE'")
    int expireReservation(@Param("id") Long id);

    Optional<Reservation> findByOrderIdAndProductId(Long orderId, Long productId);
}