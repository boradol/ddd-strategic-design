package kitchenpos;

import kitchenpos.menus.domain.Menu;
import kitchenpos.menus.domain.MenuGroup;
import kitchenpos.menus.domain.MenuProduct;
import kitchenpos.common.domain.OrderType;
import kitchenpos.orders.delivery.domain.DeliveryOrder;
import kitchenpos.orders.delivery.domain.DeliveryOrderLineItem;
import kitchenpos.orders.delivery.domain.DeliveryOrderStatus;
import kitchenpos.orders.eatin.domain.EatInOrder;
import kitchenpos.orders.eatin.domain.EatInOrderLineItem;
import kitchenpos.orders.eatin.domain.EatInOrderStatus;
import kitchenpos.orders.eatin.domain.OrderTable;
import kitchenpos.orders.takeout.domain.TakeoutOrder;
import kitchenpos.orders.takeout.domain.TakeoutOrderLineItem;
import kitchenpos.orders.takeout.domain.TakeoutOrderStatus;
import kitchenpos.products.domain.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Fixtures {
    public static final UUID INVALID_ID = new UUID(0L, 0L);

    public static Menu menu() {
        return menu(19_000L, true, menuProduct());
    }

    public static Menu menu(final long price, final MenuProduct... menuProducts) {
        return menu(price, false, menuProducts);
    }

    public static Menu menu(final long price, final boolean displayed, final MenuProduct... menuProducts) {
        final Menu menu = new Menu();
        menu.setId(UUID.randomUUID());
        menu.setName("후라이드+후라이드");
        menu.setPrice(BigDecimal.valueOf(price));
        menu.setMenuGroup(menuGroup());
        menu.setDisplayed(displayed);
        menu.setMenuProducts(Arrays.asList(menuProducts));
        return menu;
    }

    public static MenuGroup menuGroup() {
        return menuGroup("두마리메뉴");
    }

    public static MenuGroup menuGroup(final String name) {
        final MenuGroup menuGroup = new MenuGroup();
        menuGroup.setId(UUID.randomUUID());
        menuGroup.setName(name);
        return menuGroup;
    }

    public static MenuProduct menuProduct() {
        final MenuProduct menuProduct = new MenuProduct();
        menuProduct.setSeq(new Random().nextLong());
        menuProduct.setProduct(product());
        menuProduct.setQuantity(2L);
        return menuProduct;
    }

    public static MenuProduct menuProduct(final Product product, final long quantity) {
        final MenuProduct menuProduct = new MenuProduct();
        menuProduct.setSeq(new Random().nextLong());
        menuProduct.setProduct(product);
        menuProduct.setQuantity(quantity);
        return menuProduct;
    }

    public static DeliveryOrder deliveryOrder(final OrderType type, final DeliveryOrderStatus status) {
        final DeliveryOrder order = new DeliveryOrder();
        order.setId(UUID.randomUUID());
        order.setType(type);
        order.setStatus(status);
        order.setOrderDateTime(LocalDateTime.of(2020, 1, 1, 12, 0));
        order.setDeliveryOrderLineItems(List.of(deliveryOrderLineItem()));
        return order;
    }

    public static DeliveryOrder deliveryOrder(final DeliveryOrderStatus status) {
        return deliveryOrder(status, null);
    }

    public static DeliveryOrder deliveryOrder(final DeliveryOrderStatus status, final String deliveryAddress) {
        final DeliveryOrder order = new DeliveryOrder();
        order.setId(UUID.randomUUID());
        order.setStatus(status);
        order.setOrderDateTime(LocalDateTime.of(2020, 1, 1, 12, 0));
        order.setDeliveryOrderLineItems(List.of(deliveryOrderLineItem()));
        order.setDeliveryAddress(deliveryAddress);
        return order;
    }

    public static EatInOrder eatInOrder(final EatInOrderStatus status) {
        return eatInOrder(status, null);
    }

    public static EatInOrder eatInOrder(final EatInOrderStatus status, final OrderTable orderTable) {
        final EatInOrder order = new EatInOrder();
        order.setId(UUID.randomUUID());
        order.setStatus(status);
        order.setOrderDateTime(LocalDateTime.of(2020, 1, 1, 12, 0));
        order.setEatInOrderLineItems(List.of(eatInOrderLineItem()));
        order.setOrderTable(orderTable);
        return order;
    }

    public static TakeoutOrder takeoutOrder(final TakeoutOrderStatus status) {
        final TakeoutOrder order = new TakeoutOrder();
        order.setId(UUID.randomUUID());
        order.setStatus(status);
        order.setOrderDateTime(LocalDateTime.of(2020, 1, 1, 12, 0));
        order.setTakeoutOrderLineItems(List.of(takeoutOrderLineItem()));
        return order;
    }

    public static DeliveryOrderLineItem deliveryOrderLineItem() {
        final DeliveryOrderLineItem orderLineItem = new DeliveryOrderLineItem();
        orderLineItem.setSeq(new Random().nextLong());
        orderLineItem.setMenu(menu());
        return orderLineItem;
    }

    public static EatInOrderLineItem eatInOrderLineItem() {
        final EatInOrderLineItem orderLineItem = new EatInOrderLineItem();
        orderLineItem.setSeq(new Random().nextLong());
        orderLineItem.setMenu(menu());
        return orderLineItem;
    }

    public static TakeoutOrderLineItem takeoutOrderLineItem() {
        final TakeoutOrderLineItem orderLineItem = new TakeoutOrderLineItem();
        orderLineItem.setSeq(new Random().nextLong());
        orderLineItem.setMenu(menu());
        return orderLineItem;
    }

    public static OrderTable orderTable() {
        return orderTable(false, 0);
    }

    public static OrderTable orderTable(final boolean occupied, final int numberOfGuests) {
        final OrderTable orderTable = new OrderTable();
        orderTable.setId(UUID.randomUUID());
        orderTable.setName("1번");
        orderTable.setNumberOfGuests(numberOfGuests);
        orderTable.setOccupied(occupied);
        return orderTable;
    }

    public static Product product() {
        return product("후라이드", 16_000L);
    }

    public static Product product(final String name, final long price) {
        final Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setPrice(BigDecimal.valueOf(price));
        return product;
    }
}
