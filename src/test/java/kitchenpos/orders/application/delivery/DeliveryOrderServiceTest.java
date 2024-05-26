package kitchenpos.orders.application.delivery;

import kitchenpos.menus.application.InMemoryMenuRepository;
import kitchenpos.menus.domain.MenuRepository;
import kitchenpos.orders.common.OrderType;
import kitchenpos.orders.delivery.application.DeliveryOrderService;
import kitchenpos.orders.delivery.domain.DeliveryOrder;
import kitchenpos.orders.delivery.domain.DeliveryOrderLineItem;
import kitchenpos.orders.delivery.domain.DeliveryOrderRepository;
import kitchenpos.orders.delivery.domain.DeliveryOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static kitchenpos.Fixtures.INVALID_ID;
import static kitchenpos.Fixtures.deliveryOrder;
import static kitchenpos.Fixtures.menu;
import static kitchenpos.Fixtures.menuProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("배달 주문")
class DeliveryOrderServiceTest {
    private static final String 배달_주소 = "서울시 송파구 위례성대로 2";

    private DeliveryOrderRepository orderRepository;
    private MenuRepository menuRepository;
    private FakeDeliveryAgencyClient deliveryAgencyClient;
    private DeliveryOrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryDeliveryOrderRepository();
        menuRepository = new InMemoryMenuRepository();
        deliveryAgencyClient = new FakeDeliveryAgencyClient();
        orderService = new DeliveryOrderService(orderRepository, menuRepository, deliveryAgencyClient);
    }

    @DisplayName("1개 이상의 등록된 메뉴로 배달 주문을 등록할 수 있다.")
    @Test
    void createDeliveryOrder() {
        final UUID menuId = menuRepository.save(menu(19_000L, true, menuProduct())).getId();
        final DeliveryOrder expected = createOrderRequest(배달_주소, createOrderLineItemRequest(menuId, 19_000L, 3L)
        );
        final DeliveryOrder actual = orderService.create(expected);
        assertThat(actual).isNotNull();
        assertAll(
                () -> assertThat(actual.getId()).isNotNull(),
                () -> assertThat(actual.getType()).isEqualTo(expected.getType()),
                () -> assertThat(actual.getStatus()).isEqualTo(DeliveryOrderStatus.WAITING),
                () -> assertThat(actual.getOrderDateTime()).isNotNull(),
                () -> assertThat(actual.getDeliveryOrderLineItems()).hasSize(1),
                () -> assertThat(actual.getDeliveryAddress()).isEqualTo(expected.getDeliveryAddress())
        );
    }

    @DisplayName("주문 유형이 배달주문이 아니면 생성할 수 없다.")
    @NullSource
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = "DELIVERY")
    @ParameterizedTest
    void create(final OrderType type) {
        final UUID menuId = menuRepository.save(menu(19_000L, true, menuProduct())).getId();
        final DeliveryOrder expected = createOrderRequest(type, createOrderLineItemRequest(menuId, 19_000L, 3L));
        assertThatThrownBy(() -> orderService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("메뉴가 없으면 생성할 수 없다.")
    @MethodSource("orderLineItems")
    @ParameterizedTest
    void create(final List<DeliveryOrderLineItem> orderLineItems) {
        final DeliveryOrder expected = createOrderRequest(orderLineItems);
        assertThatThrownBy(() -> orderService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<Arguments> orderLineItems() {
        return Arrays.asList(
                null,
                Arguments.of(Collections.emptyList()),
                Arguments.of(List.of(createOrderLineItemRequest(INVALID_ID, 19_000L, 3L)))
        );
    }

    @DisplayName("주문 항목의 수량은 0개 이상이어야 한다.")
    @ValueSource(longs = -1L)
    @ParameterizedTest
    void createWithoutEatInOrder(final long quantity) {
        final UUID menuId = menuRepository.save(menu(19_000L, true, menuProduct())).getId();
        final DeliveryOrder expected = createOrderRequest(createOrderLineItemRequest(menuId, 19_000L, quantity)
        );
        assertThatThrownBy(() -> orderService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("배달 주소가 올바르지 않으면 배달 주문을 생성할 수 없다.")
    @NullAndEmptySource
    @ParameterizedTest
    void create(final String deliveryAddress) {
        final UUID menuId = menuRepository.save(menu(19_000L, true, menuProduct())).getId();
        final DeliveryOrder expected = createOrderRequest(
                deliveryAddress, createOrderLineItemRequest(menuId, 19_000L, 3L)
        );
        assertThatThrownBy(() -> orderService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("숨겨진 메뉴는 주문할 수 없다.")
    @Test
    void createNotDisplayedMenuOrder() {
        final UUID menuId = menuRepository.save(menu(19_000L, false, menuProduct())).getId();
        final DeliveryOrder expected = createOrderRequest(createOrderLineItemRequest(menuId, 19_000L, 3L));
        assertThatThrownBy(() -> orderService.create(expected))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("주문한 메뉴의 가격은 실제 메뉴 가격과 일치해야 한다.")
    @Test
    void createNotMatchedMenuPriceOrder() {
        final UUID menuId = menuRepository.save(menu(19_000L, true, menuProduct())).getId();
        final DeliveryOrder expected = createOrderRequest(createOrderLineItemRequest(menuId, 16_000L, 3L));
        assertThatThrownBy(() -> orderService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문을 수락한다.")
    @Test
    void accept() {
        final UUID orderId = orderRepository.save(deliveryOrder(DeliveryOrderStatus.WAITING)).getId();
        final DeliveryOrder actual = orderService.accept(orderId);
        assertThat(actual.getStatus()).isEqualTo(DeliveryOrderStatus.ACCEPTED);
    }

    @DisplayName("대기 중인 주문만 수락할 수 있다.")
    @EnumSource(value = DeliveryOrderStatus.class, names = "WAITING", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    void accept(final DeliveryOrderStatus status) {
        final UUID orderId = orderRepository.save(deliveryOrder(status)).getId();
        assertThatThrownBy(() -> orderService.accept(orderId))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("배달 주문을 접수되면 배달 대행사를 호출한다.")
    @Test
    void acceptDeliveryOrder() {
        final UUID orderId = orderRepository.save(deliveryOrder(DeliveryOrderStatus.WAITING, 배달_주소)).getId();
        final DeliveryOrder actual = orderService.accept(orderId);
        assertAll(
                () -> assertThat(actual.getStatus()).isEqualTo(DeliveryOrderStatus.ACCEPTED),
                () -> assertThat(deliveryAgencyClient.getOrderId()).isEqualTo(orderId),
                () -> assertThat(deliveryAgencyClient.getDeliveryAddress()).isEqualTo(배달_주소)
        );
    }

    @DisplayName("주문을 배달기사님에게 제공한다.")
    @Test
    void serve() {
        final UUID orderId = orderRepository.save(deliveryOrder(DeliveryOrderStatus.ACCEPTED)).getId();
        final DeliveryOrder actual = orderService.serve(orderId);
        assertThat(actual.getStatus()).isEqualTo(DeliveryOrderStatus.SERVED);
    }

    @DisplayName("수락된 주문만 제공할 수 있다.")
    @EnumSource(value = DeliveryOrderStatus.class, names = "ACCEPTED", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    void serve(final DeliveryOrderStatus status) {
        final UUID orderId = orderRepository.save(deliveryOrder(status)).getId();
        assertThatThrownBy(() -> orderService.serve(orderId))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("배달 주문을 시작한다.")
    @Test
    void startDelivery() {
        final UUID orderId = orderRepository.save(deliveryOrder(DeliveryOrderStatus.SERVED, 배달_주소)).getId();
        final DeliveryOrder actual = orderService.startDelivery(orderId);
        assertThat(actual.getStatus()).isEqualTo(DeliveryOrderStatus.DELIVERING);
    }

    @DisplayName("배달기사에게 주문이 제공된 주문만 배달할 수 있다.")
    @EnumSource(value = DeliveryOrderStatus.class, names = "SERVED", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    void startDelivery(final DeliveryOrderStatus status) {
        final UUID orderId = orderRepository.save(deliveryOrder(status, 배달_주소)).getId();
        assertThatThrownBy(() -> orderService.startDelivery(orderId))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("주문을 배달 완료한다.")
    @Test
    void completeDelivery() {
        final UUID orderId = orderRepository.save(deliveryOrder(DeliveryOrderStatus.DELIVERING, 배달_주소)).getId();
        final DeliveryOrder actual = orderService.completeDelivery(orderId);
        assertThat(actual.getStatus()).isEqualTo(DeliveryOrderStatus.DELIVERED);
    }

    @DisplayName("배달 중인 주문만 배달 완료할 수 있다.")
    @EnumSource(value = DeliveryOrderStatus.class, names = "DELIVERING", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    void completeDelivery(final DeliveryOrderStatus status) {
        final UUID orderId = orderRepository.save(deliveryOrder(status, 배달_주소)).getId();
        assertThatThrownBy(() -> orderService.completeDelivery(orderId))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("주문을 완료한다.")
    @Test
    void complete() {
        final DeliveryOrder expected = orderRepository.save(deliveryOrder(DeliveryOrderStatus.DELIVERED, 배달_주소));
        final DeliveryOrder actual = orderService.complete(expected.getId());
        assertThat(actual.getStatus()).isEqualTo(DeliveryOrderStatus.COMPLETED);
    }

    @DisplayName("배달 완료된 주문만 완료할 수 있다.")
    @EnumSource(value = DeliveryOrderStatus.class, names = "DELIVERED", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    void completeDeliveryOrder(final DeliveryOrderStatus status) {
        final UUID orderId = orderRepository.save(deliveryOrder(status, 배달_주소)).getId();
        assertThatThrownBy(() -> orderService.complete(orderId))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("주문의 목록을 조회할 수 있다.")
    @Test
    void findAll() {
        orderRepository.save(deliveryOrder(DeliveryOrderStatus.DELIVERED, 배달_주소));
        orderRepository.save(deliveryOrder(DeliveryOrderStatus.COMPLETED, "서울시 강남구 하하호호로 3"));
        final List<DeliveryOrder> actual = orderService.findAll();
        assertThat(actual).hasSize(2);
    }

    private DeliveryOrder createOrderRequest(
            final String deliveryAddress,
            final DeliveryOrderLineItem... orderLineItems
    ) {
        final DeliveryOrder order = new DeliveryOrder();
        order.setDeliveryAddress(deliveryAddress);
        order.setDeliveryOrderLineItems(Arrays.asList(orderLineItems));
        return order;
    }

    private DeliveryOrder createOrderRequest(final DeliveryOrderLineItem... orderLineItems) {
        return createOrderRequest(Arrays.asList(orderLineItems));
    }

    private DeliveryOrder createOrderRequest(final List<DeliveryOrderLineItem> orderLineItems) {
        final DeliveryOrder order = new DeliveryOrder();
        order.setDeliveryOrderLineItems(orderLineItems);
        return order;
    }

    private DeliveryOrder createOrderRequest(final OrderType type, final DeliveryOrderLineItem... orderLineItems) {
        final DeliveryOrder order = new DeliveryOrder();
        order.setType(type);
        order.setDeliveryOrderLineItems(Arrays.asList(orderLineItems));
        return order;
    }

    private static DeliveryOrderLineItem createOrderLineItemRequest(final UUID menuId, final long price, final long quantity) {
        final DeliveryOrderLineItem orderLineItem = new DeliveryOrderLineItem();
        orderLineItem.setSeq(new Random().nextLong());
        orderLineItem.setMenuId(menuId);
        orderLineItem.setPrice(BigDecimal.valueOf(price));
        orderLineItem.setQuantity(quantity);
        return orderLineItem;
    }
}
