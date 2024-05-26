package kitchenpos.orders.eatin.infra;

import kitchenpos.orders.eatin.domain.EatInOrder;
import kitchenpos.orders.eatin.domain.EatInOrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaEatInOrderRepository extends EatInOrderRepository, JpaRepository<EatInOrder, UUID> {
}
