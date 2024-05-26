package kitchenpos.orders.infra;

import kitchenpos.orders.domain.Order;
import kitchenpos.orders.domain.OrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaOrderRepository extends OrderRepository, JpaRepository<Order, UUID> {
}
