package com.tienda.gestion.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductoJpaRepository extends JpaRepository<ProductoEntity, Long> {
    @Query("select p from ProductoEntity p where p.stock <= p.stockMinimo order by p.nombre, p.id")
    List<ProductoEntity> buscarStockBajo();

    @Query("select p from ProductoEntity p where p.stock = 0 order by p.nombre, p.id")
    List<ProductoEntity> buscarSinStock();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductoEntity p where p.id = :id")
    Optional<ProductoEntity> bloquearPorId(@Param("id") Long id);
}
