package kitchenpos.orders.delivery.application;

import kitchenpos.menus.domain.Menu;
import kitchenpos.menus.domain.MenuRepository;
import kitchenpos.orders.common.OrderType;
import kitchenpos.orders.delivery.domain.DeliveryOrder;
import kitchenpos.orders.delivery.domain.DeliveryOrderLineItem;
import kitchenpos.orders.delivery.domain.DeliveryOrderRepository;
import kitchenpos.orders.delivery.domain.DeliveryOrderStatus;
import kitchenpos.orders.delivery.infra.DeliveryAgencyClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Service
public class DeliveryOrderService {
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final MenuRepository menuRepository;
    private final DeliveryAgencyClient deliveryAgencyClient;

    public DeliveryOrderService(
            final DeliveryOrderRepository deliveryOrderRepository,
            final MenuRepository menuRepository,
            final DeliveryAgencyClient deliveryAgencyClient
    ) {
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.menuRepository = menuRepository;
        this.deliveryAgencyClient = deliveryAgencyClient;
    }

    @Transactional
    public DeliveryOrder create(final DeliveryOrder request) {
        final OrderType type = request.getType();
        if (Objects.isNull(type) || type != OrderType.DELIVERY) {
            throw new IllegalArgumentException();
        }
        final List<DeliveryOrderLineItem> orderLineItemRequests = request.getDeliveryOrderLineItems();
        if (Objects.isNull(orderLineItemRequests) || orderLineItemRequests.isEmpty()) {
            throw new IllegalArgumentException();
        }
        final List<Menu> menus = menuRepository.findAllByIdIn(
                orderLineItemRequests.stream()
                        .map(DeliveryOrderLineItem::getMenuId)
                        .toList()
        );
        if (menus.size() != orderLineItemRequests.size()) {
            throw new IllegalArgumentException();
        }
        final List<DeliveryOrderLineItem> orderLineItems = new ArrayList<>();
        for (final DeliveryOrderLineItem orderLineItemRequest : orderLineItemRequests) {
            final long quantity = orderLineItemRequest.getQuantity();
            if (quantity < 0) {
                throw new IllegalArgumentException();
            }
            final Menu menu = menuRepository.findById(orderLineItemRequest.getMenuId())
                    .orElseThrow(NoSuchElementException::new);
            if (!menu.isDisplayed()) {
                throw new IllegalStateException();
            }
            if (menu.getPrice().compareTo(orderLineItemRequest.getPrice()) != 0) {
                throw new IllegalArgumentException();
            }
            final DeliveryOrderLineItem orderLineItem = new DeliveryOrderLineItem();
            orderLineItem.setMenu(menu);
            orderLineItem.setQuantity(quantity);
            orderLineItems.add(orderLineItem);
        }
        DeliveryOrder order = new DeliveryOrder();
        order.setId(UUID.randomUUID());
        order.setStatus(DeliveryOrderStatus.WAITING);
        order.setOrderDateTime(LocalDateTime.now());
        order.setDeliveryOrderLineItems(orderLineItems);
        final String deliveryAddress = request.getDeliveryAddress();
        if (Objects.isNull(deliveryAddress) || deliveryAddress.isEmpty()) {
            throw new IllegalArgumentException();
        }
        order.setDeliveryAddress(deliveryAddress);
        return deliveryOrderRepository.save(order);
    }

    @Transactional
    public DeliveryOrder accept(final UUID orderId) {
        final DeliveryOrder order = getOrder(orderId);
        if (order.getStatus() != DeliveryOrderStatus.WAITING) {
            throw new IllegalStateException();
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (final DeliveryOrderLineItem orderLineItem : order.getDeliveryOrderLineItems()) {
            sum = orderLineItem.getMenu()
                    .getPrice()
                    .multiply(BigDecimal.valueOf(orderLineItem.getQuantity()));
        }
        deliveryAgencyClient.requestDelivery(orderId, sum, order.getDeliveryAddress());
        order.setStatus(DeliveryOrderStatus.ACCEPTED);
        return order;
    }

    @Transactional
    public DeliveryOrder serve(final UUID orderId) {
        final DeliveryOrder order = getOrder(orderId);
        if (order.getStatus() != DeliveryOrderStatus.ACCEPTED) {
            throw new IllegalStateException();
        }
        order.setStatus(DeliveryOrderStatus.SERVED);
        return order;
    }

    @Transactional
    public DeliveryOrder startDelivery(final UUID orderId) {
        final DeliveryOrder order = getOrder(orderId);
        if (order.getStatus() != DeliveryOrderStatus.SERVED) {
            throw new IllegalStateException();
        }
        order.setStatus(DeliveryOrderStatus.DELIVERING);
        return order;
    }

    @Transactional
    public DeliveryOrder completeDelivery(final UUID orderId) {
        final DeliveryOrder order = getOrder(orderId);
        if (order.getStatus() != DeliveryOrderStatus.DELIVERING) {
            throw new IllegalStateException();
        }
        order.setStatus(DeliveryOrderStatus.DELIVERED);
        return order;
    }

    @Transactional
    public DeliveryOrder complete(final UUID orderId) {
        final DeliveryOrder order = getOrder(orderId);
        final DeliveryOrderStatus status = order.getStatus();
        if (status != DeliveryOrderStatus.DELIVERED) {
            throw new IllegalStateException();
        }
        order.setStatus(DeliveryOrderStatus.COMPLETED);
        return order;
    }

    @Transactional(readOnly = true)
    public List<DeliveryOrder> findAll() {
        return deliveryOrderRepository.findAll();
    }

    private DeliveryOrder getOrder(UUID orderId) {
        return deliveryOrderRepository.findById(orderId)
                .orElseThrow(NoSuchElementException::new);
    }
}
