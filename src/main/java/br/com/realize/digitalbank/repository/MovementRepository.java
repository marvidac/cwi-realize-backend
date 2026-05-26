package br.com.realize.digitalbank.repository;

import br.com.realize.digitalbank.domain.Movement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementRepository extends JpaRepository<Movement, Long> {

    @EntityGraph(attributePaths = {"account", "transfer"})
    Page<Movement> findByAccountId(Long accountId, Pageable pageable);
}
