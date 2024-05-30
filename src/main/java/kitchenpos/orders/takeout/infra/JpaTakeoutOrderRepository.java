package kitchenpos.orders.takeout.infra;

import kitchenpos.orders.takeout.domain.TakeoutOrder;
import kitchenpos.orders.takeout.domain.TakeoutOrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaTakeoutOrderRepository extends TakeoutOrderRepository, JpaRepository<TakeoutOrder, UUID> {
}
