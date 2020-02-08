package com.redhat.quarkus.cafe.domain;

import com.redhat.quarkus.cafe.infrastructure.OrdersService;
import com.redhat.quarkus.cafe.infrastructure.DashboardService;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class Cafe {

    @Inject
    OrdersService ordersService;

    @Inject
    @RestClient
    DashboardService dashboardService;

    Jsonb jsonb = JsonbBuilder.create();

    //TODO Create and persist an Order
    public void orderIn(CreateOrderCommand createOrderCommand) throws ExecutionException, InterruptedException {

        List<OrderEvent> allEvents = new ArrayList<>();
        createOrderCommand.beverages.ifPresent(beverages -> {
            allEvents.addAll(createOrderCommand.beverages.get().stream().map(b -> new BeverageOrderInEvent(createOrderCommand.id, b.name, b.item)).collect(Collectors.toList()));
        });
        createOrderCommand.kitchenOrders.ifPresent(foods -> {
            allEvents.addAll(createOrderCommand.kitchenOrders.get().stream().map(f -> new KitchenOrderInEvent(createOrderCommand.id, f.name, f.item)).collect(Collectors.toList()));
        });

        System.out.println("\n" + allEvents.size() + "\n");

/*
        CompletableFuture.runAsync(() -> {
            ordersService.publishBeveargeOrders(allEvents);
        }).thenRun(() -> { //dashboardService.updatedDashboard(convertJson(allEvents));
            System.out.println("update dashboard");})
                .get();
*/

        ordersService.publishBeveargeOrders(allEvents).get();
//                .thenCompose(fn(allEvents)).get();
/*
        return kafkaService.updateOrders(allEvents)
                .thenApply(v -> {
            return allEvents;
        }).thenCompose(this::ordersIn);
*/
    }

    private Function fn(List<OrderEvent> events) {
        dashboardService.updatedDashboard(convertJson(events));
        return null;
    }

    private List<DashboardUpdate> convertJson(List<OrderEvent> orderEvents) {
        return orderEvents.stream()
                .map(orderEvent -> {
            System.out.println("\nConverting: " + orderEvent.toString() +"\n");
            OrderStatus status;
            switch(orderEvent.eventType){
                case BEVERAGE_ORDER_IN:
                    status = OrderStatus.IN_QUEUE;
                    break;
                case BEVERAGE_ORDER_UP:
                    status = OrderStatus.READY;
                    break;
                case KITCHEN_ORDER_IN:
                    status = OrderStatus.IN_QUEUE;
                    break;
                case KITCHEN_ORDER_UP:
                    status = OrderStatus.READY;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown status" + orderEvent.eventType);
            }
            return new DashboardUpdate(
                    orderEvent.orderId,
                    orderEvent.itemId,
                    orderEvent.name,
                    orderEvent.item,
                    status);
        }).collect(Collectors.toList());
    }

    /*
        Convert the OrderEvent JSON to the JSON that the Web UI expects and call the REST endpoint
     */
    private CompletableFuture<List<OrderEvent>> ordersIn(final List<OrderEvent> orderEvents) {

        return CompletableFuture.supplyAsync(() -> {

            List<DashboardUpdate> dashboardUpdates = orderEvents.stream()
                    .map(orderEvent -> {
                        System.out.println("\nConverting: " + orderEvent.toString() +"\n");
                        OrderStatus status;
                        switch(orderEvent.eventType){
                            case BEVERAGE_ORDER_IN:
                                status = OrderStatus.IN_QUEUE;
                                break;
                            case BEVERAGE_ORDER_UP:
                                status = OrderStatus.READY;
                                break;
                            case KITCHEN_ORDER_IN:
                                status = OrderStatus.IN_QUEUE;
                                break;
                            case KITCHEN_ORDER_UP:
                                status = OrderStatus.READY;
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown status" + orderEvent.eventType);
                        }
                        return new DashboardUpdate(
                                orderEvent.orderId,
                                orderEvent.itemId,
                                orderEvent.name,
                                orderEvent.item,
                                status);
                    }).collect(Collectors.toList());
            dashboardService.updatedDashboard(dashboardUpdates);
/*
            String json = jsonb.toJson(dashboardUpdates);
            System.out.println("\n"+json+"\n");
*/
            return orderEvents;
        });
    }

    private CompletableFuture<Void> updateDashboard(List<OrderEvent> orderEvents) {

        return CompletableFuture.supplyAsync(() ->{

            dashboardService.updatedDashboard(convertOrderEventsToDashboardUpdates(orderEvents));
            return null;
        });
    }

    private List<DashboardUpdate> convertOrderEventsToDashboardUpdates(List<OrderEvent> orderEvents){

        return orderEvents.stream()
                .map(orderEvent -> {
                    System.out.println("\nConverting: " + orderEvent.toString() +"\n");
                    OrderStatus status;
                    switch(orderEvent.eventType){
                        case BEVERAGE_ORDER_IN:
                            status = OrderStatus.IN_QUEUE;
                            break;
                        case BEVERAGE_ORDER_UP:
                            status = OrderStatus.READY;
                            break;
                        case KITCHEN_ORDER_IN:
                            status = OrderStatus.IN_QUEUE;
                            break;
                        case KITCHEN_ORDER_UP:
                            status = OrderStatus.READY;
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown status" + orderEvent.eventType);
                    }
                    return new DashboardUpdate(
                            orderEvent.orderId,
                            orderEvent.itemId,
                            orderEvent.name,
                            orderEvent.item,
                            status);
                }).collect(Collectors.toList());
    }


    public void orderUp(List<OrderEvent> orderEvents) {

        dashboardService.updatedDashboard(convertOrderEventsToDashboardUpdates(orderEvents));
    }
}
