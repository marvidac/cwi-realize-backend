package br.com.realize.digitalbank.repository;

import br.com.realize.digitalbank.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
}
