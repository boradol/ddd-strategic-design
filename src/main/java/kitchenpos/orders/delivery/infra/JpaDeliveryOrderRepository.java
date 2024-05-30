package kitchenpos.orders.delivery.infra;

import kitchenpos.orders.delivery.domain.DeliveryOrder;
import kitchenpos.orders.delivery.domain.DeliveryOrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaDeliveryOrderRepository extends DeliveryOrderRepository, JpaRepository<DeliveryOrder, UUID> {
}
