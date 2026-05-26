package br.com.realize.digitalbank.repository;

import br.com.realize.digitalbank.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
