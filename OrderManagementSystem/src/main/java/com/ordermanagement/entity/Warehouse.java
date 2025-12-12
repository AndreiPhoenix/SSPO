package com.ordermanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "warehouses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;

    private Integer capacity;

    @OneToMany(mappedBy = "warehouseId", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<Product> products = new ArrayList<>();
}