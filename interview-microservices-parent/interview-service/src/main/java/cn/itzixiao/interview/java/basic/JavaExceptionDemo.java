package cn.itzixiao.interview.java.basic;

/**
 * Java 异常机制详解
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Java 异常体系                                                 │
 * │                                                               │
 * │  Throwable                                                    │
 * │    ├── Error（错误）                                           │
 * │    │   ├── OutOfMemoryError                                   │
 * │    │   ├── StackOverflowError                                 │
 * │    │   └── VirtualMachineError                                │
 * │    │                                                           │
 * │    └── Exception（异常）                                       │
 * │        ├── RuntimeException（非受检异常）                       │
 * │        │   ├── NullPointerException                           │
 * │        │   ├── ArrayIndexOutOfBoundsException                 │
 * │        │   ├── ClassCastException                             │
 * │        │   ├── IllegalArgumentException                       │
 * │        │   └── IllegalStateException                          │
 * │        │                                                       │
 * │        └── Checked Exception（受检异常）                       │
 * │            ├── IOException                                    │
 * │            ├── SQLException                                   │
 * │            ├── ClassNotFoundException                         │
 * │            └── InterruptedException                           │
 * └─────────────────────────────────────────────────────────────┘
 */
public class JavaExceptionDemo {

    public static void main(String[] args) {
        System.out.println("========== Java异常机制 ==========\n");
    
        demonstrateExceptionHierarchy();
        demonstrateTryCatchFinally();
        demonstrateThrowsKeyword();
        demonstrateThrowKeyword();
        demonstrateCustomException();
        demonstrateTryWithResources();
        demonstrateExceptionBestPractices();
            
        System.out.println("\n========== 演示完成 ==========");
    }

    /**
     * 1. 异常体系演示
     */
    public static void demonstrateExceptionHierarchy() {
        System.out.println("【1. 异常体系】\n");

        // Error: JVM 无法处理的严重错误
        try {
            System.out.println("尝试创建死循环导致 StackOverflowError:");
            recursiveMethod();
        } catch (StackOverflowError e) {
            System.out.println("捕获到 StackOverflowError: " + e.getMessage());
        }

        // RuntimeException: 运行时异常，无需显式声明
        try {
            String str = null;
            str.length();  // NullPointerException
        } catch (NullPointerException e) {
            System.out.println("\n捕获到 NullPointerException: " + e.getMessage());
        }

        // Checked Exception: 编译时异常，必须处理
        try {
            Class.forName("com.example.NonExistentClass");
        } catch (ClassNotFoundException e) {
            System.out.println("\n捕获到 ClassNotFoundException: " + e.getMessage());
        }

        System.out.println("\n【异常分类】");
        System.out.println("  - Error: JVM 级别错误，程序不应尝试捕获");
        System.out.println("  - RuntimeException: 非受检异常，运行时自动抛出");
        System.out.println("  - Checked Exception: 受检异常，编译时强制要求处理\n");
    }

    private static void recursiveMethod() {
        recursiveMethod();  // 无限递归
    }

    /**
     * 2. try-catch-finally 详解
     */
    public static void demonstrateTryCatchFinally() {
        System.out.println("【2. try-catch-finally】\n");

        int result = divide(10, 0);
        System.out.println("10 / 0 的结果：" + result);

        System.out.println("\n【执行流程】");
        System.out.println("  1. try 块中的代码被执行");
        System.out.println("  2. 如果发生异常，跳转到匹配的 catch 块");
        System.out.println("  3. finally 块总是执行（除了 System.exit()）");
        System.out.println("  4. 多个 catch 块按顺序匹配，子类在前，父类在后");
    }

    public static int divide(int a, int b) {
        int result = 0;
        try {
            System.out.println("执行除法运算：" + a + " / " + b);
            result = a / b;  // ArithmeticException
            System.out.println("计算成功");
        } catch (ArithmeticException e) {
            System.out.println("捕获到算术异常：" + e.getMessage());
            result = -1;  // 返回默认值
        } catch (Exception e) {
            System.out.println("捕获到其他异常：" + e.getMessage());
            result = -999;
        } finally {
            System.out.println("finally 块总是执行，用于清理资源");
        }
        return result;
    }

    /**
     * 3. throws 关键字（声明异常）
     */
    public static void demonstrateThrowsKeyword() {
        System.out.println("【3. throws 关键字】\n");

        try {
            readFile("nonexistent.txt");
        } catch (Exception e) {
            System.out.println("调用方法时处理异常：" + e.getClass().getSimpleName());
        }

        System.out.println("\n【throws 的作用】");
        System.out.println("  - 在方法签名中声明可能抛出的异常");
        System.out.println("  - 将异常处理责任交给调用者");
        System.out.println("  - 只用于 Checked Exception\n");
    }

    public static void readFile(String filename) throws Exception {
        if (!filename.endsWith(".txt")) {
            throw new IllegalArgumentException("文件名必须以.txt 结尾");
        }
        // 模拟文件读取
        throw new java.io.FileNotFoundException("文件不存在：" + filename);
    }

    /**
     * 4. throw 关键字（抛出异常）
     */
    public static void demonstrateThrowKeyword() {
        System.out.println("【4. throw 关键字】\n");

        try {
            validateAge(-5);
        } catch (IllegalArgumentException e) {
            System.out.println("捕获到非法参数异常：" + e.getMessage());
        }

        System.out.println("\n【throw vs throws】");
        System.out.println("  - throw: 用于方法内部，实际抛出一个异常对象");
        System.out.println("  - throws: 用于方法签名，声明可能抛出的异常类型\n");
    }

    public static void validateAge(int age) {
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("年龄必须在 0-150 之间，当前：" + age);
        }
        System.out.println("年龄验证通过：" + age);
    }

    /**
     * 5. 自定义异常
     */
    public static void demonstrateCustomException() {
        System.out.println("【5. 自定义异常】\n");

        try {
            User user = new User("Alice", -1);
            registerUser(user);
        } catch (InvalidUserException e) {
            System.out.println("注册失败：" + e.getMessage());
            System.out.println("错误码：" + e.getErrorCode());
        }

        System.out.println("\n【自定义异常规范】");
        System.out.println("  - 继承 Exception 或 RuntimeException");
        System.out.println("  - 提供无参构造和带 message 的构造");
        System.out.println("  - 可添加自定义字段（如错误码）");
        System.out.println("  - 业务异常建议继承 RuntimeException\n");
    }

    public static void registerUser(User user) throws InvalidUserException {
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new InvalidUserException("E001", "用户名不能为空");
        }
        if (user.getAge() < 0) {
            throw new InvalidUserException("E002", "年龄不能为负数");
        }
        System.out.println("用户注册成功：" + user.getName());
    }

    static class User {
        private String name;
        private int age;

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
    }

    static class InvalidUserException extends RuntimeException {
        private final String errorCode;

        public InvalidUserException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    /**
     * 6. try-with-resources（JDK7+）
     */
    public static void demonstrateTryWithResources() {
        System.out.println("【6. try-with-resources】\n");

        // JDK7+ 语法，自动关闭实现了 AutoCloseable 的资源
        try (MyResource resource = new MyResource()) {
            resource.doWork();
            System.out.println("资源使用完毕，自动关闭");
        } catch (Exception e) {
            System.out.println("操作异常：" + e.getMessage());
        }

        System.out.println("\n【try-with-resources 优势】");
        System.out.println("  - 自动调用 close() 方法");
        System.out.println("  - 避免资源泄漏");
        System.out.println("  - 代码更简洁\n");
    }

    static class MyResource implements AutoCloseable {
        public void doWork() {
            System.out.println("正在使用资源...");
        }

        @Override
        public void close() throws Exception {
            System.out.println("资源已关闭");
        }
    }

    /**
     * 7. 异常处理最佳实践
     */
    public static void demonstrateExceptionBestPractices() {
        System.out.println("【7. 异常处理最佳实践】\n");

        // 1. 不要捕获 Exception 后什么都不做
        System.out.println("【1. 不要吞掉异常】");
        try {
            riskyOperation();
        } catch (Exception e) {
            // ❌ 错误做法：空 catch 块
            // System.out.println("出错了，但我不告诉你发生了什么");
            
            // ✅ 正确做法：记录日志或重新抛出
            System.err.println("操作失败：" + e.getMessage());
            e.printStackTrace();
        }

        // 2. 不要使用异常控制流程
        System.out.println("\n【2. 不要用异常控制正常流程】");
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            // ❌ 错误做法：用异常判断数组边界
            // try { array[i]; } catch (ArrayIndexOutOfBoundsException e) {}
            
            // ✅ 正确做法：先检查边界
            if (i < 10) {
                int[] array = new int[10];
                array[i] = i;
            }
        }
        System.out.println("正常流程耗时：" + (System.currentTimeMillis() - start) + "ms");

        // 3. 异常链（保留原始异常）
        System.out.println("\n【3. 使用异常链】");
        try {
            lowerLevelOperation();
        } catch (HighLevelExceptionWrapper e) {
            System.out.println("高层异常：" + e.getMessage());
            System.out.println("原始原因：" + e.getCause().getMessage());
        }

        // 4. 精确捕获异常
        System.out.println("\n【4. 精确捕获异常】");
        try {
            specificExceptions();
        } catch (java.io.FileNotFoundException e) {
            System.out.println("文件未找到：" + e.getMessage());
        } catch (java.io.IOException e) {
            System.out.println("IO 异常：" + e.getMessage());
        }

        // 5. finally 中的 return 陷阱
        System.out.println("\n【5. finally 中不要 return】");
        int result = finallyReturnTrap();
        System.out.println("返回值：" + result + " (应该是 1，但 finally 的 return 覆盖了它)");

        System.out.println("\n【最佳实践总结】");
        System.out.println("  ✅ 针对具体异常编程");
        System.out.println("  ✅ 记录异常堆栈信息");
        System.out.println("  ✅ 使用 try-with-resources");
        System.out.println("  ✅ 保持异常链完整性");
        System.out.println("  ❌ 不要吞掉异常");
        System.out.println("  ❌ 不要用异常控制流程");
        System.out.println("  ❌ 不要在 finally 中 return\n");
    }

    public static void riskyOperation() {
        throw new RuntimeException("模拟风险操作");
    }

    public static void lowerLevelOperation() {
        try {
            throw new IllegalStateException("底层操作失败");
        } catch (IllegalStateException e) {
            throw new HighLevelExceptionWrapper("高层业务异常", e);
        }
    }

    static class HighLevelExceptionWrapper extends RuntimeException {
        public HighLevelExceptionWrapper(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static void specificExceptions() throws java.io.IOException {
        throw new java.io.FileNotFoundException("特定文件未找到");
    }

    public static int finallyReturnTrap() {
        try {
            System.out.println("try 块准备返回 1");
            return 1;
        } catch (Exception e) {
            System.out.println("catch 块准备返回 2");
            return 2;
        } finally {
            System.out.println("finally 块准备返回 3");
            return 3;  // ❌ 这会覆盖 try/catch 的返回值！
        }
    }
}
