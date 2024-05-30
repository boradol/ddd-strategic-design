package kitchenpos.orders.application.takeout;

import kitchenpos.menus.application.InMemoryMenuRepository;
import kitchenpos.menus.domain.MenuRepository;
import kitchenpos.common.domain.OrderType;
import kitchenpos.orders.takeout.application.TakeoutOrderService;
import kitchenpos.orders.takeout.domain.TakeoutOrder;
import kitchenpos.orders.takeout.domain.TakeoutOrderLineItem;
import kitchenpos.orders.takeout.domain.TakeoutOrderRepository;
import kitchenpos.orders.takeout.domain.TakeoutOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static kitchenpos.Fixtures.INVALID_ID;
import static kitchenpos.Fixtures.menu;
import static kitchenpos.Fixtures.menuProduct;
import static kitchenpos.Fixtures.takeoutOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("포장 주문")
class TakeoutOrderServiceTest {
    private TakeoutOrderRepository orderRepository;
    private MenuRepository menuRepository;
    private TakeoutOrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryTakeoutOrderRepository();
        menuRepository = new InMemoryMenuRepository();
        orderService = new TakeoutOrderService(orderRepository, menuRepository);
    }

    @DisplayName("1개 이상의 등록된 메뉴로 포장 주문을 등록할 수 있다.")
    @Test
    void createTakeoutOrder() {
        final UUID menuId = menuRepository.save(menu(19_000L, true, menuProduct())).getId();
        final TakeoutOrder expected = createOrderRequest(OrderType.TAKEOUT, createOrderLineItemRequest(menuId, 19_000L, 3L));
        final TakeoutOrder actual = orderService.create(expected);
        assertThat(actual).isNotNull();
        assertAll(
                () -> assertThat(actual.getId()).isNotNull(),
                () -> assertThat(actual.getType()).isEqualTo(expected.getType()),
                () -> assertThat(actual.getStatus()).isEqualTo(TakeoutOrderStatus.WAITING),
                () -> assertThat(actual.getOrderDateTime()).isNotNull(),
                () -> assertThat(actual.getTakeoutOrderLineItems()).hasSize(1)
        );
    }

    @DisplayName("주문 유형이 매장주문이 아니면 등록할 수 없다.")
    @NullSource
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = "TAKEOUT")
    @ParameterizedTest
    void create(final OrderType type) {
        final UUID menuId = menuRepository.save(menu(19_000L, true, menuProduct())).getId();
        final TakeoutOrder expected = createOrderRequest(type, createOrderLineItemRequest(menuId, 19_000L, 3L));
        assertThatThrownBy(() -> orderService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("메뉴가 없으면 등록할 수 없다.")
    @MethodSource("orderLineItems")
    @ParameterizedTest
    void create(final List<TakeoutOrderLineItem> orderLineItems) {
        final TakeoutOrder expected = createOrderRequest(OrderType.TAKEOUT, orderLineItems);
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
        final TakeoutOrder expected = createOrderRequest(
                OrderType.TAKEOUT, createOrderLineItemRequest(menuId, 19_000L, quantity)
        );
        assertThatThrownBy(() -> orderService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("숨겨진 메뉴는 주문할 수 없다.")
    @Test
    void createNotDisplayedMenuOrder() {
        final UUID menuId = menuRepository.save(menu(19_000L, false, menuProduct())).getId();
        final TakeoutOrder expected = createOrderRequest(OrderType.TAKEOUT, createOrderLineItemRequest(menuId, 19_000L, 3L));
        assertThatThrownBy(() -> orderService.create(expected))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("주문한 메뉴의 가격은 실제 메뉴 가격과 일치해야 한다.")
    @Test
    void createNotMatchedMenuPriceOrder() {
        final UUID menuId = menuRepository.save(menu(19_000L, true, menuProduct())).getId();
        final TakeoutOrder expected = createOrderRequest(OrderType.TAKEOUT, createOrderLineItemRequest(menuId, 16_000L, 3L));
        assertThatThrownBy(() -> orderService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문을 수락한다.")
    @Test
    void accept() {
        final UUID orderId = orderRepository.save(takeoutOrder(TakeoutOrderStatus.WAITING)).getId();
        final TakeoutOrder actual = orderService.accept(orderId);
        assertThat(actual.getStatus()).isEqualTo(TakeoutOrderStatus.ACCEPTED);
    }

    @DisplayName("대기 중인 주문만 수락할 수 있다.")
    @EnumSource(value = TakeoutOrderStatus.class, names = "WAITING", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    void accept(final TakeoutOrderStatus status) {
        final UUID orderId = orderRepository.save(takeoutOrder(status)).getId();
        assertThatThrownBy(() -> orderService.accept(orderId))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("주문을 포장주문 고객님에게 제공한다.")
    @Test
    void serve() {
        final UUID orderId = orderRepository.save(takeoutOrder(TakeoutOrderStatus.ACCEPTED)).getId();
        final TakeoutOrder actual = orderService.serve(orderId);
        assertThat(actual.getStatus()).isEqualTo(TakeoutOrderStatus.SERVED);
    }

    @DisplayName("수락된 주문만 포장주문 고객님에게 제공할 수 있다.")
    @EnumSource(value = TakeoutOrderStatus.class, names = "ACCEPTED", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    void serve(final TakeoutOrderStatus status) {
        final UUID orderId = orderRepository.save(takeoutOrder(status)).getId();
        assertThatThrownBy(() -> orderService.serve(orderId))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("주문을 완료한다.")
    @Test
    void complete() {
        final TakeoutOrder expected = orderRepository.save(takeoutOrder(TakeoutOrderStatus.SERVED));
        final TakeoutOrder actual = orderService.complete(expected.getId());
        assertThat(actual.getStatus()).isEqualTo(TakeoutOrderStatus.COMPLETED);
    }

    @DisplayName("제공된 주문만 완료할 수 있다.")
    @EnumSource(value = TakeoutOrderStatus.class, names = "SERVED", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    void completeTakeoutAndEatInOrder(final TakeoutOrderStatus status) {
        final UUID orderId = orderRepository.save(takeoutOrder(status)).getId();
        assertThatThrownBy(() -> orderService.complete(orderId))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("주문의 목록을 조회할 수 있다.")
    @Test
    void findAll() {
        orderRepository.save(takeoutOrder(TakeoutOrderStatus.SERVED));
        orderRepository.save(takeoutOrder(TakeoutOrderStatus.COMPLETED));
        final List<TakeoutOrder> actual = orderService.findAll();
        assertThat(actual).hasSize(2);
    }

    private TakeoutOrder createOrderRequest(final OrderType orderType, final List<TakeoutOrderLineItem> orderLineItems) {
        final TakeoutOrder order = new TakeoutOrder();
        order.setType(orderType);
        order.setTakeoutOrderLineItems(orderLineItems);
        return order;
    }

    private TakeoutOrder createOrderRequest(
            final OrderType type,
            final TakeoutOrderLineItem... orderLineItems
    ) {
        final TakeoutOrder order = new TakeoutOrder();
        order.setType(type);
        order.setTakeoutOrderLineItems(Arrays.asList(orderLineItems));
        return order;
    }

    private static TakeoutOrderLineItem createOrderLineItemRequest(final UUID menuId, final long price, final long quantity) {
        final TakeoutOrderLineItem orderLineItem = new TakeoutOrderLineItem();
        orderLineItem.setSeq(new Random().nextLong());
        orderLineItem.setMenuId(menuId);
        orderLineItem.setPrice(BigDecimal.valueOf(price));
        orderLineItem.setQuantity(quantity);
        return orderLineItem;
    }
}
