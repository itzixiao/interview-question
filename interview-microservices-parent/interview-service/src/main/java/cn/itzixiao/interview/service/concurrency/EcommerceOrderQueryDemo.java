package cn.itzixiao.interview.service.concurrency;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * CompletableFuture 实战案例：电商订单查询
 * 
 * @author itzixiao
 * @date 2026-03-17
 */
public class EcommerceOrderQueryDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("========== 电商订单详情页查询 ==========");
        
        OrderQueryService service = new OrderQueryService();
        CompletableFuture<OrderDetailVO> result = service.getOrderDetail("ORDER-20260317001");
        
        // 异步处理结果
        result.thenAccept(vo -> {
            System.out.println("\n========== 订单详情 ==========");
            System.out.println("用户：" + vo.getUser().getName() + " (" + vo.getUser().getPhone() + ")");
            System.out.println("订单：" + vo.getOrder().getId() + " - " + vo.getOrder().getAmount() + "元 (" + vo.getOrder().getStatus() + ")");
            System.out.println("商品：" + vo.getProduct().getName() + " x" + vo.getProduct().getQuantity());
            System.out.println("物流：" + vo.getLogistics().getCompany() + " - " + vo.getLogistics().getStatus());
        }).exceptionally(ex -> {
            System.err.println("查询失败：" + ex.getMessage());
            return null;
        });
        
        Thread.sleep(3000);
    }
    
    /**
     * 订单查询服务
     */
    static class OrderQueryService {
        
        private final ExecutorService executor = Executors.newFixedThreadPool(4);
        
        public CompletableFuture<OrderDetailVO> getOrderDetail(String orderId) {
            long startTime = System.currentTimeMillis();
            
            // 1. 并行查询各个服务
            CompletableFuture<User> userFuture = queryUserService(orderId);
            CompletableFuture<Order> orderFuture = queryOrderService(orderId);
            CompletableFuture<Product> productFuture = queryProductService(orderId);
            CompletableFuture<Logistics> logisticsFuture = queryLogisticsService(orderId);
            
            // 2. 等待所有查询完成并组合
            return CompletableFuture.allOf(userFuture, orderFuture, productFuture, logisticsFuture)
                .thenApply(v -> {
                    // 3. 组装 VO 对象
                    OrderDetailVO vo = new OrderDetailVO();
                    vo.setUser(userFuture.join());
                    vo.setOrder(orderFuture.join());
                    vo.setProduct(productFuture.join());
                    vo.setLogistics(logisticsFuture.join());
                    
                    long costTime = System.currentTimeMillis() - startTime;
                    System.out.println("【性能】查询耗时：" + costTime + "ms (串行预计需要 630ms)");
                    return vo;
                })
                .exceptionally(ex -> {
                    System.err.println("查询异常：" + ex.getMessage());
                    throw new RuntimeException("订单查询失败", ex);
                });
        }
        
        private CompletableFuture<User> queryUserService(String orderId) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("[用户服务] 查询完成");
                return new User("张三", "138****1234");
            }, executor);
        }
        
        private CompletableFuture<Order> queryOrderService(String orderId) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("[订单服务] 查询完成");
                return new Order(orderId, 299.99, "已支付");
            }, executor);
        }
        
        private CompletableFuture<Product> queryProductService(String orderId) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(180);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("[商品服务] 查询完成");
                return new Product("iPhone 15", 1);
            }, executor);
        }
        
        private CompletableFuture<Logistics> queryLogisticsService(String orderId) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("[物流服务] 查询完成");
                return new Logistics("顺丰速运", "运输中");
            }, executor);
        }
    }
    
    /**
     * 订单详情 VO
     */
    static class OrderDetailVO {
        private User user;
        private Order order;
        private Product product;
        private Logistics logistics;
        
        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
        public Order getOrder() { return order; }
        public void setOrder(Order order) { this.order = order; }
        public Product getProduct() { return product; }
        public void setProduct(Product product) { this.product = product; }
        public Logistics getLogistics() { return logistics; }
        public void setLogistics(Logistics logistics) { this.logistics = logistics; }
    }
    
    /**
     * 用户实体
     */
    static class User {
        private String name;
        private String phone;
        
        public User(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }
        
        public String getName() { return name; }
        public String getPhone() { return phone; }
    }
    
    /**
     * 订单实体
     */
    static class Order {
        private String id;
        private Double amount;
        private String status;
        
        public Order(String id, Double amount, String status) {
            this.id = id;
            this.amount = amount;
            this.status = status;
        }
        
        public String getId() { return id; }
        public Double getAmount() { return amount; }
        public String getStatus() { return status; }
    }
    
    /**
     * 商品实体
     */
    static class Product {
        private String name;
        private Integer quantity;
        
        public Product(String name, Integer quantity) {
            this.name = name;
            this.quantity = quantity;
        }
        
        public String getName() { return name; }
        public Integer getQuantity() { return quantity; }
    }
    
    /**
     * 物流实体
     */
    static class Logistics {
        private String company;
        private String status;
        
        public Logistics(String company, String status) {
            this.company = company;
            this.status = status;
        }
        
        public String getCompany() { return company; }
        public String getStatus() { return status; }
    }
}
