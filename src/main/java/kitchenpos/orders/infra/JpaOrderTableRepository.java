package kitchenpos.orders.infra;

import kitchenpos.orders.domain.OrderTable;
import kitchenpos.orders.domain.OrderTableRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaOrderTableRepository extends OrderTableRepository, JpaRepository<OrderTable, UUID> {
}
