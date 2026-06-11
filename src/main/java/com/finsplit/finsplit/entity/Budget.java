package com.finsplit.finsplit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "budgets",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "category_id", "month_year"}
        )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Format: "2026-06"  (yyyy-MM)
    @Column(name = "month_year", nullable = false)
    private String monthYear;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal limitAmount;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
