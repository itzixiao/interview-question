package cn.itzixiao.interview.java;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Java 自定义注解详解
 * <p>
 * 本示例涵盖：
 * 1. 元注解详解（@Target, @Retention, @Documented, @Inherited）
 * 2. 自定义注解的定义语法
 * 3. 注解属性定义与默认值
 * 4. 注解处理器实现（反射处理注解）
 * 5. 实战案例：标记注解、权限控制、参数校验、日志记录
 * <p>
 * 注解本质：注解是一种特殊的接口，继承自 java.lang.annotation.Annotation
 * 编译后生成：public interface MyAnnotation extends Annotation { ... }
 *
 * @author itzixiao
 * @version 1.0
 */
public class CustomAnnotationDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("\n");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println("█                                                                   █");
        System.out.println("█                  Java 自定义注解深入讲解                          █");
        System.out.println("█                                                                   █");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println();

        // 1. 元注解详解
        demonstrateMetaAnnotations();

        // 2. 自定义注解基础
        demonstrateBasicAnnotations();

        // 3. 注解处理器
        demonstrateAnnotationProcessor();

        // 4. 实战案例：权限控制
        demonstratePermissionControl();

        // 5. 实战案例：参数校验
        demonstrateParameterValidation();

        // 6. 实战案例：日志记录
        demonstrateLoggingAnnotation();

        // 7. 高频面试题
        demonstrateInterviewQuestions();

        System.out.println("\n");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println("█                        全部演示完成                             █");
        System.out.println("███████████████████████████████████████████████████████████████████");
    }

    // ==================== 1. 元注解详解 ====================

    private static void demonstrateMetaAnnotations() {
        System.out.println("========== 1. 元注解详解 ==========\n");

        System.out.println("【什么是元注解？】");
        System.out.println("  元注解是用于修饰注解的注解，定义注解的行为和生命周期\n");

        System.out.println("【四大元注解一览】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  元注解           │  作用           │  取值范围                     │");
        System.out.println("├─────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  @Target          │  注解作用目标    │  TYPE, FIELD, METHOD...     │");
        System.out.println("│  @Retention       │  注解生命周期    │  SOURCE, CLASS, RUNTIME     │");
        System.out.println("│  @Documented      │  生成JavaDoc     │  无属性                      │");
        System.out.println("│  @Inherited       │  允许子类继承    │  无属性                      │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        // @Target 详解
        System.out.println("【@Target - 指定注解作用目标】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  ElementType 常量      │  作用目标                                   │");
        System.out.println("├─────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  TYPE                  │  类、接口、枚举                              │");
        System.out.println("│  FIELD                 │  字段（包括枚举常量）                        │");
        System.out.println("│  METHOD                │  方法                                       │");
        System.out.println("│  PARAMETER             │  方法参数                                    │");
        System.out.println("│  CONSTRUCTOR           │  构造方法                                    │");
        System.out.println("│  LOCAL_VARIABLE        │  局部变量                                    │");
        System.out.println("│  ANNOTATION_TYPE       │  注解类型                                    │");
        System.out.println("│  PACKAGE               │  包                                         │");
        System.out.println("│  TYPE_PARAMETER        │  类型参数（泛型） Java 8+                    │");
        System.out.println("│  TYPE_USE              │  类型使用处 Java 8+                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        // @Retention 详解
        System.out.println("【@Retention - 指定注解生命周期】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  RetentionPolicy  │  生命周期        │  应用场景                     │");
        System.out.println("├─────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  SOURCE           │  源码级别        │  编译时检查，如@Override      │");
        System.out.println("│  CLASS            │  字节码级别      │  编译时处理，默认策略        │");
        System.out.println("│  RUNTIME          │  运行时级别      │  运行时反射读取，最常用      │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【示例：定义多种作用目标的注解】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 只能用于方法                                                   │");
        System.out.println("│  @Target(ElementType.METHOD)                                       │");
        System.out.println("│  @Retention(RetentionPolicy.RUNTIME)                               │");
        System.out.println("│  public @interface MyMethodAnnotation { }                          │");
        System.out.println("│                                                                    │");
        System.out.println("│  // 可用于类、方法、字段                                           │");
        System.out.println("│  @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})│");
        System.out.println("│  @Retention(RetentionPolicy.RUNTIME)                               │");
        System.out.println("│  public @interface MyMultiAnnotation { }                           │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 2. 自定义注解基础 ====================

    private static void demonstrateBasicAnnotations() {
        System.out.println("========== 2. 自定义注解基础 ==========\n");

        System.out.println("【注解定义语法】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  public @interface 注解名 {                                        │");
        System.out.println("│      // 注解属性（方法形式定义）                                    │");
        System.out.println("│      类型 属性名() default 默认值;                                 │");
        System.out.println("│  }                                                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【属性定义规则】");
        System.out.println("  1. 属性以方法形式定义，无参数，有返回类型");
        System.out.println("  2. 可以使用 default 关键字指定默认值");
        System.out.println("  3. 只有一个属性时，建议命名为 value（可省略属性名）");
        System.out.println("  4. 属性类型支持：基本类型、String、Class、枚举、注解、以上类型的数组\n");

        System.out.println("【示例 1：标记注解（无属性）】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @Target(ElementType.METHOD)                                       │");
        System.out.println("│  @Retention(RetentionPolicy.RUNTIME)                               │");
        System.out.println("│  public @interface Deprecated { }                                  │");
        System.out.println("│                                                                    │");
        System.out.println("│  // 使用                                                            │");
        System.out.println("│  @Deprecated                                                       │");
        System.out.println("│  public void oldMethod() { }                                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【示例 2：单值注解】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @Target(ElementType.FIELD)                                        │");
        System.out.println("│  @Retention(RetentionPolicy.RUNTIME)                               │");
        System.out.println("│  public @interface Column {                                        │");
        System.out.println("│      String value();  // 唯一属性，使用时可省略属性名              │");
        System.out.println("│  }                                                                 │");
        System.out.println("│                                                                    │");
        System.out.println("│  // 使用                                                            │");
        System.out.println("│  @Column(\"user_name\")  // 等价于 @Column(value = \"user_name\")     │");
        System.out.println("│  private String userName;                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【示例 3：多属性注解】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @Target(ElementType.METHOD)                                       │");
        System.out.println("│  @Retention(RetentionPolicy.RUNTIME)                               │");
        System.out.println("│  public @interface ApiOperation {                                  │");
        System.out.println("│      String value();                    // 接口描述                │");
        System.out.println("│      String notes() default \"\";        // 备注说明                 │");
        System.out.println("│      String[] tags() default {};       // 标签分组                 │");
        System.out.println("│      boolean hidden() default false;   // 是否隐藏                 │");
        System.out.println("│  }                                                                 │");
        System.out.println("│                                                                    │");
        System.out.println("│  // 使用                                                            │");
        System.out.println("│  @ApiOperation(value = \"获取用户\", tags = {\"用户模块\"})           │");
        System.out.println("│  public User getUser(Long id) { }                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【示例 4：枚举属性注解】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  public enum SensitiveType {                                       │");
        System.out.println("│      PHONE, ID_CARD, BANK_CARD, EMAIL, ADDRESS                    │");
        System.out.println("│  }                                                                 │");
        System.out.println("│                                                                    │");
        System.out.println("│  @Target(ElementType.FIELD)                                        │");
        System.out.println("│  @Retention(RetentionPolicy.RUNTIME)                               │");
        System.out.println("│  public @interface Sensitive {                                     │");
        System.out.println("│      SensitiveType value();                                        │");
        System.out.println("│  }                                                                 │");
        System.out.println("│                                                                    │");
        System.out.println("│  // 使用                                                            │");
        System.out.println("│  @Sensitive(SensitiveType.PHONE)                                   │");
        System.out.println("│  private String phone;                                             │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 3. 注解处理器 ====================

    private static void demonstrateAnnotationProcessor() throws Exception {
        System.out.println("========== 3. 注解处理器实现 ==========\n");

        System.out.println("【核心 API】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Class/Method/Field 方法          │  说明                          │");
        System.out.println("├─────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  isAnnotationPresent(Class)       │  判断是否存在指定注解          │");
        System.out.println("│  getAnnotation(Class)             │  获取指定注解                  │");
        System.out.println("│  getAnnotations()                 │  获取所有注解                  │");
        System.out.println("│  getDeclaredAnnotations()         │  获取直接声明的注解            │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        // 演示：处理类上的注解
        System.out.println("【演示：处理类上的注解】");
        Class<?> clazz = UserService.class;

        // 判断注解存在
        boolean hasAnnotation = clazz.isAnnotationPresent(MyService.class);
        System.out.println("  isAnnotationPresent(MyService.class): " + hasAnnotation);

        // 获取注解
        if (hasAnnotation) {
            MyService myService = clazz.getAnnotation(MyService.class);
            System.out.println("  注解值: value = " + myService.value());
            System.out.println("  注解值: description = " + myService.description());
        }
        System.out.println();

        // 演示：处理方法上的注解
        System.out.println("【演示：处理方法上的注解】");
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(MyLog.class)) {
                MyLog myLog = method.getAnnotation(MyLog.class);
                System.out.println("  方法: " + method.getName());
                System.out.println("    操作: " + myLog.operation());
                System.out.println("    描述: " + myLog.description());
            }
        }
        System.out.println();

        // 演示：处理字段上的注解
        System.out.println("【演示：处理字段上的注解】");
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(MyField.class)) {
                MyField myField = field.getAnnotation(MyField.class);
                System.out.println("  字段: " + field.getName());
                System.out.println("    名称: " + myField.name());
                System.out.println("    长度: " + myField.length());
                System.out.println("    必填: " + myField.required());
            }
        }
        System.out.println();
    }

    // ==================== 4. 实战案例：权限控制 ====================

    private static void demonstratePermissionControl() {
        System.out.println("========== 4. 实战案例：权限控制注解 ==========\n");

        System.out.println("【场景说明】");
        System.out.println("  实现方法级别的权限控制，类似 Spring Security 的 @PreAuthorize\n");

        // 定义权限注解
        System.out.println("【定义权限注解】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @Target(ElementType.METHOD)                                       │");
        System.out.println("│  @Retention(RetentionPolicy.RUNTIME)                               │");
        System.out.println("│  public @interface RequirePermission {                             │");
        System.out.println("│      String[] value();     // 需要的权限                           │");
        System.out.println("│      Logical logical() default Logical.AND; // 逻辑关系            │");
        System.out.println("│  }                                                                 │");
        System.out.println("│                                                                    │");
        System.out.println("│  public enum Logical { AND, OR }                                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        // 模拟权限校验
        System.out.println("【模拟权限校验】");
        simulatePermissionCheck("admin", new String[]{"user:read", "user:write", "user:delete"});
        simulatePermissionCheck("user", new String[]{"user:read"});
        System.out.println();
    }

    private static void simulatePermissionCheck(String username, String[] userPermissions) {
        System.out.println("用户: " + username + "，拥有权限: " + Arrays.toString(userPermissions));

        // 模拟调用带权限注解的方法
        Method[] methods = AdminController.class.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(RequirePermission.class)) {
                RequirePermission rp = method.getAnnotation(RequirePermission.class);
                String[] requiredPerms = rp.value();

                boolean hasPermission;
                if (rp.logical() == Logical.AND) {
                    // AND 逻辑：需要拥有所有权限
                    hasPermission = Arrays.asList(userPermissions).containsAll(Arrays.asList(requiredPerms));
                } else {
                    // OR 逻辑：拥有任意一个权限即可
                    hasPermission = Arrays.stream(requiredPerms)
                            .anyMatch(p -> Arrays.asList(userPermissions).contains(p));
                }

                System.out.println("  方法 " + method.getName() + " 需要: " + Arrays.toString(requiredPerms) +
                        " (" + rp.logical() + ") → " + (hasPermission ? "✅ 通过" : "❌ 拒绝"));
            }
        }
        System.out.println();
    }

    // ==================== 5. 实战案例：参数校验 ====================

    private static void demonstrateParameterValidation() throws Exception {
        System.out.println("========== 5. 实战案例：参数校验注解 ==========\n");

        System.out.println("【场景说明】");
        System.out.println("  实现类似 JSR-303 Bean Validation 的参数校验功能\n");

        // 测试校验
        System.out.println("【测试参数校验】\n");

        // 测试 1：正常数据
        UserDTO validUser = new UserDTO();
        validUser.setUsername("zhangsan");
        validUser.setEmail("zhangsan@example.com");
        validUser.setAge(25);
        validUser.setPhone("13812345678");
        System.out.println("测试数据 1：" + validUser);
        List<String> errors1 = validateObject(validUser);
        System.out.println("校验结果：" + (errors1.isEmpty() ? "✅ 通过" : "❌ 失败 - " + errors1));
        System.out.println();

        // 测试 2：异常数据
        UserDTO invalidUser = new UserDTO();
        invalidUser.setUsername("ab");  // 太短
        invalidUser.setEmail("invalid-email");  // 格式错误
        invalidUser.setAge(200);  // 超出范围
        invalidUser.setPhone("123");  // 格式错误
        System.out.println("测试数据 2：" + invalidUser);
        List<String> errors2 = validateObject(invalidUser);
        System.out.println("校验结果：" + (errors2.isEmpty() ? "✅ 通过" : "❌ 失败"));
        errors2.forEach(e -> System.out.println("  - " + e));
        System.out.println();
    }

    private static List<String> validateObject(Object obj) throws Exception {
        List<String> errors = new ArrayList<>();
        Class<?> clazz = obj.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(obj);

            // @NotNull 校验
            if (field.isAnnotationPresent(NotNull.class)) {
                if (value == null) {
                    NotNull notNull = field.getAnnotation(NotNull.class);
                    errors.add(field.getName() + ": " + notNull.message());
                }
            }

            // @Length 校验
            if (field.isAnnotationPresent(Length.class) && value != null) {
                Length length = field.getAnnotation(Length.class);
                String strValue = value.toString();
                if (strValue.length() < length.min() || strValue.length() > length.max()) {
                    errors.add(field.getName() + ": " + length.message());
                }
            }

            // @Range 校验
            if (field.isAnnotationPresent(Range.class) && value != null) {
                Range range = field.getAnnotation(Range.class);
                int intValue = (Integer) value;
                if (intValue < range.min() || intValue > range.max()) {
                    errors.add(field.getName() + ": " + range.message());
                }
            }

            // @Pattern 校验
            if (field.isAnnotationPresent(Pattern.class) && value != null) {
                Pattern pattern = field.getAnnotation(Pattern.class);
                if (!java.util.regex.Pattern.matches(pattern.regex(), value.toString())) {
                    errors.add(field.getName() + ": " + pattern.message());
                }
            }
        }

        return errors;
    }

    // ==================== 6. 实战案例：日志记录 ====================

    private static void demonstrateLoggingAnnotation() {
        System.out.println("========== 6. 实战案例：日志记录注解 ==========\n");

        System.out.println("【场景说明】");
        System.out.println("  通过注解 + AOP 实现方法执行日志记录\n");

        System.out.println("【定义日志注解】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @Target(ElementType.METHOD)                                       │");
        System.out.println("│  @Retention(RetentionPolicy.RUNTIME)                               │");
        System.out.println("│  public @interface Log {                                           │");
        System.out.println("│      String value() default \"\";        // 操作描述                │");
        System.out.println("│      LogLevel level() default LogLevel.INFO;  // 日志级别          │");
        System.out.println("│      boolean recordParams() default true;  // 是否记录参数         │");
        System.out.println("│      boolean recordResult() default true;  // 是否记录返回值       │");
        System.out.println("│  }                                                                 │");
        System.out.println("│                                                                    │");
        System.out.println("│  public enum LogLevel { DEBUG, INFO, WARN, ERROR }                │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【模拟 AOP 处理】");
        simulateLoggingAOP();
        System.out.println();
    }

    private static void simulateLoggingAOP() {
        // 模拟调用带日志注解的方法
        OrderService orderService = new OrderService();

        System.out.println("【调用 createOrder 方法】");
        invokeWithLogging(orderService, "createOrder", "ORD-001", "iPhone 15");

        System.out.println("\n【调用 cancelOrder 方法】");
        invokeWithLogging(orderService, "cancelOrder", "ORD-001");
    }

    private static void invokeWithLogging(Object target, String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }

            Method method = target.getClass().getMethod(methodName, paramTypes);

            if (method.isAnnotationPresent(Log.class)) {
                Log logAnnotation = method.getAnnotation(Log.class);

                // 记录方法开始
                System.out.println("[" + logAnnotation.level() + "] 开始执行: " + logAnnotation.value());
                if (logAnnotation.recordParams()) {
                    System.out.println("  参数: " + Arrays.toString(args));
                }

                // 执行方法
                long start = System.currentTimeMillis();
                Object result = method.invoke(target, args);
                long elapsed = System.currentTimeMillis() - start;

                // 记录方法结束
                System.out.println("  耗时: " + elapsed + "ms");
                if (logAnnotation.recordResult() && result != null) {
                    System.out.println("  返回: " + result);
                }
            } else {
                method.invoke(target, args);
            }
        } catch (Exception e) {
            System.out.println("  异常: " + e.getMessage());
        }
    }

    // ==================== 7. 高频面试题 ====================

    private static void demonstrateInterviewQuestions() {
        System.out.println("========== 7. 高频面试题 ==========\n");

        // 问题 1
        System.out.println("【问题 1】注解的本质是什么？");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  答：                                                               │");
        System.out.println("│  注解本质上是一种特殊的接口，继承自 java.lang.annotation.Annotation│");
        System.out.println("│                                                                    │");
        System.out.println("│  编译后的字节码：                                                   │");
        System.out.println("│  public interface MyAnnotation extends Annotation {                │");
        System.out.println("│      String value();                                               │");
        System.out.println("│  }                                                                 │");
        System.out.println("│                                                                    │");
        System.out.println("│  这也是为什么注解属性定义采用方法形式的原因。                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        // 问题 2
        System.out.println("【问题 2】@Retention 的三个策略有什么区别？");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  答：                                                               │");
        System.out.println("│                                                                    │");
        System.out.println("│  ┌─────────────────────────────────────────────────────────────┐   │");
        System.out.println("│  │  策略      │  存在阶段    │  反射可读  │  典型应用          │   │");
        System.out.println("│  ├─────────────────────────────────────────────────────────────┤   │");
        System.out.println("│  │  SOURCE    │  源码        │  ❌        │  @Override         │   │");
        System.out.println("│  │  CLASS     │  字节码      │  ❌        │  默认策略          │   │");
        System.out.println("│  │  RUNTIME   │  运行时      │  ✅        │  自定义注解        │   │");
        System.out.println("│  └─────────────────────────────────────────────────────────────┘   │");
        System.out.println("│                                                                    │");
        System.out.println("│  RUNTIME 是最常用的策略，可以通过反射读取注解信息。                │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        // 问题 3
        System.out.println("【问题 3】如何获取注解的属性值？");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  答：                                                               │");
        System.out.println("│                                                                    │");
        System.out.println("│  // 1. 获取类上的注解                                              │");
        System.out.println("│  MyAnnotation anno = clazz.getAnnotation(MyAnnotation.class);      │");
        System.out.println("│  String value = anno.value();  // 调用属性方法获取值               │");
        System.out.println("│                                                                    │");
        System.out.println("│  // 2. 获取方法上的注解                                            │");
        System.out.println("│  Method method = clazz.getMethod(\"doSomething\");                  │");
        System.out.println("│  MyAnnotation anno = method.getAnnotation(MyAnnotation.class);     │");
        System.out.println("│                                                                    │");
        System.out.println("│  // 3. 获取字段上的注解                                            │");
        System.out.println("│  Field field = clazz.getDeclaredField(\"name\");                    │");
        System.out.println("│  MyAnnotation anno = field.getAnnotation(MyAnnotation.class);      │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        // 问题 4
        System.out.println("【问题 4】注解可以继承吗？");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  答：                                                               │");
        System.out.println("│  默认情况下，注解不能继承。但可以通过 @Inherited 元注解实现继承。  │");
        System.out.println("│                                                                    │");
        System.out.println("│  @Inherited                                                        │");
        System.out.println("│  @Target(ElementType.TYPE)                                         │");
        System.out.println("│  @Retention(RetentionPolicy.RUNTIME)                               │");
        System.out.println("│  public @interface MyAnnotation { }                                │");
        System.out.println("│                                                                    │");
        System.out.println("│  @MyAnnotation                                                     │");
        System.out.println("│  class Parent { }                                                  │");
        System.out.println("│  class Child extends Parent { }  // Child 也拥有 @MyAnnotation    │");
        System.out.println("│                                                                    │");
        System.out.println("│  注意：@Inherited 只对类继承有效，接口和方法的继承不生效。         │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        // 问题 5
        System.out.println("【问题 5】注解和接口的区别？");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  答：                                                               │");
        System.out.println("│                                                                    │");
        System.out.println("│  ┌─────────────────────────────────────────────────────────────┐   │");
        System.out.println("│  │  对比项        │  注解                    │  接口            │   │");
        System.out.println("│  ├─────────────────────────────────────────────────────────────┤   │");
        System.out.println("│  │  定义关键字    │  @interface             │  interface       │   │");
        System.out.println("│  │  方法实现      │  不支持                 │  支持            │   │");
        System.out.println("│  │  默认继承      │  Annotation             │  Object          │   │");
        System.out.println("│  │  属性定义      │  方法形式，必须有默认值  │  方法定义        │   │");
        System.out.println("│  │  使用方式      │  @注解名                 │  implements      │   │");
        System.out.println("│  │  作用          │  元数据标记              │  定义行为契约    │   │");
        System.out.println("│  └─────────────────────────────────────────────────────────────┘   │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        // 问题 6
        System.out.println("【问题 6】如何设计一个通用的注解？");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  答：设计通用注解需要考虑以下几点：                                 │");
        System.out.println("│                                                                    │");
        System.out.println("│  1. 合理设置 @Target：根据实际使用场景选择                         │");
        System.out.println("│  2. 使用 RUNTIME 策略：便于运行时反射读取                          │");
        System.out.println("│  3. 提供 value 属性：单属性时可省略属性名                          │");
        System.out.println("│  4. 合理设置默认值：减少使用时的配置量                             │");
        System.out.println("│  5. 添加 @Documented：生成 JavaDoc 文档                            │");
        System.out.println("│  6. 考虑可扩展性：预留扩展属性                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 自定义注解定义 ====================

    /**
     * 类级别服务注解
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface MyService {
        String value() default "";

        String description() default "";
    }

    /**
     * 方法级别日志注解
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface MyLog {
        String operation() default "";

        String description() default "";
    }

    /**
     * 字段级别注解
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface MyField {
        String name() default "";

        int length() default 255;

        boolean required() default false;
    }

    /**
     * 权限注解
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface RequirePermission {
        String[] value();

        Logical logical() default Logical.AND;
    }

    /**
     * 权限逻辑枚举
     */
    public enum Logical {
        AND, OR
    }

    /**
     * 非空校验注解
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface NotNull {
        String message() default "字段不能为空";
    }

    /**
     * 长度校验注解
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface Length {
        int min() default 0;

        int max() default Integer.MAX_VALUE;

        String message() default "长度不符合要求";
    }

    /**
     * 范围校验注解
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface Range {
        int min() default Integer.MIN_VALUE;

        int max() default Integer.MAX_VALUE;

        String message() default "数值超出范围";
    }

    /**
     * 正则校验注解
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface Pattern {
        String regex();

        String message() default "格式不正确";
    }

    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    /**
     * 日志记录注解
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface Log {
        String value() default "";

        LogLevel level() default LogLevel.INFO;

        boolean recordParams() default true;

        boolean recordResult() default true;
    }

    // ==================== 演示用的类 ====================

    /**
     * 用户服务类 - 演示类级别注解
     */
    @MyService(value = "userService", description = "用户服务")
    static class UserService {

        @MyField(name = "用户ID", length = 32, required = true)
        private String userId;

        @MyField(name = "用户名", length = 50, required = true)
        private String username;

        @MyLog(operation = "查询用户", description = "根据ID查询用户信息")
        public String getUserById(String id) {
            return "User-" + id;
        }

        @MyLog(operation = "创建用户", description = "创建新用户")
        public void createUser(String name) {
            System.out.println("创建用户: " + name);
        }
    }

    /**
     * 管理控制器 - 演示权限注解
     */
    static class AdminController {

        @RequirePermission("user:read")
        public void getUser(Long id) {
            System.out.println("获取用户: " + id);
        }

        @RequirePermission(value = {"user:read", "user:write"}, logical = Logical.AND)
        public void updateUser(Long id, String name) {
            System.out.println("更新用户: " + id);
        }

        @RequirePermission(value = {"user:delete", "admin:all"}, logical = Logical.OR)
        public void deleteUser(Long id) {
            System.out.println("删除用户: " + id);
        }
    }

    /**
     * 用户 DTO - 演示参数校验注解
     */
    static class UserDTO {
        @NotNull(message = "用户名不能为空")
        @Length(min = 3, max = 20, message = "用户名长度必须在3-20之间")
        private String username;

        @NotNull(message = "邮箱不能为空")
        @Pattern(regex = "^[A-Za-z0-9+_.-]+@(.+)$", message = "邮箱格式不正确")
        private String email;

        @Range(min = 1, max = 150, message = "年龄必须在1-150之间")
        private Integer age;

        @Pattern(regex = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        // Getters and Setters
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        @Override
        public String toString() {
            return "UserDTO{username='" + username + "', email='" + email +
                    "', age=" + age + ", phone='" + phone + "'}";
        }
    }

    /**
     * 订单服务 - 演示日志注解
     */
    static class OrderService {

        @Log(value = "创建订单", level = LogLevel.INFO)
        public String createOrder(String orderId, String product) {
            // 模拟业务处理
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "订单创建成功: " + orderId;
        }

        @Log(value = "取消订单", level = LogLevel.WARN, recordResult = false)
        public void cancelOrder(String orderId) {
            // 模拟业务处理
            System.out.println("  执行取消订单: " + orderId);
        }
    }
}
