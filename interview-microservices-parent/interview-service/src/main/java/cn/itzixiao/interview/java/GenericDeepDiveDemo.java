package cn.itzixiao.interview.java;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java 泛型深入详解
 * 
 * 【一、泛型概述】
 * 泛型是 JDK5 引入的特性，允许在定义类、接口、方法时使用类型参数。
 * 核心作用：
 * 1. 类型安全：编译期检查类型，避免 ClassCastException
 * 2. 代码复用：一套代码适用于多种类型
 * 3. 消除强制转换：编译器自动插入类型转换
 * 
 * 【二、类型擦除（Type Erasure）】
 * Java 泛型是通过类型擦除实现的伪泛型：
 * - 编译期：泛型信息存在，进行类型检查
 * - 运行期：泛型信息被擦除，只保留原始类型（Raw Type）
 * 
 * 【三、擦除规则】
 * 1. <T> 无界类型参数 → 擦除为 Object
 * 2. <T extends Number> 有界类型参数 → 擦除为边界类型 Number
 * 3. <T extends Comparable<T>> 多边界 → 擦除为第一个边界类型
 * 
 * 【四、PECS原则】
 * Producer Extends, Consumer Super
 * - 生产者（? extends T）：集合给你数据 → 你只能读
 * - 消费者（? super T）：集合接受数据 → 你只能写
 * 
 * @author interview-question
 */
public class GenericDeepDiveDemo {

    /**
     * 用于演示通过反射获取字段泛型信息的示例字段
     * 
     * 运行时可以通过 Field.getGenericType() 获取到 Map<String, Integer>
     * 这是因为字段的泛型信息被 Signature 属性保留在字节码中
     */
    private Map<String, Integer> genericField = new HashMap<>();

    public static void main(String[] args) throws Exception {
        System.out.println("========== Java 泛型深入详解 ==========\n");

        // 1. 泛型基础：演示泛型类、泛型方法、泛型接口的基本用法
        demonstrateGenericBasics();

        // 2. 类型擦除演示：验证Java泛型的伪泛型特性
        demonstrateTypeErasure();

        // 3. 查看运行时实际的泛型代码：通过反射获取Signature属性保留的泛型信息
        demonstrateRuntimeGenericInfo();

        // 4. 泛型的编译期检查：理解编译期与运行期的不同行为
        demonstrateCompileTimeCheck();

        // 5. 泛型边界与擦除规则：演示有界类型参数的擦除机制
        demonstrateGenericBounds();

        // 6. 泛型通配符：重点掌握PECS原则
        demonstrateWildcards();

        // 7. 泛型方法的类型推断：理解编译器的类型推断机制
        demonstrateTypeInference();

        // 8. 泛型与数组：理解为什么不能创建泛型数组
        demonstrateGenericArray();

        // 9. 高频面试题：总结泛型相关考点
        showInterviewQuestions();
    }

    /**
     * 1. 泛型基础演示
     * 
     * 展示泛型的三种使用方式：
     * - 泛型类：类定义时指定类型参数，如 Box<T>
     * - 泛型方法：方法定义时指定类型参数，如 <T> T identity(T t)
     * - 泛型接口：接口定义时指定类型参数，如 Repository<T>
     * 
     * 同时展示原始类型（Raw Type）的风险：
     * - 失去类型安全保护
     * - 编译器只能给出警告，无法完全阻止类型错误
     */
    private static void demonstrateGenericBasics() {
        System.out.println("【一、泛型基础】\n");

        // ==================== 泛型类演示 ====================
        // Box<T> 是一个泛型类，使用时指定具体类型
        // 编译器会在编译时进行类型检查
        Box<String> stringBox = new Box<>();
        stringBox.setValue("Hello");
        System.out.println("泛型类 Box<String>: " + stringBox.getValue());

        Box<Integer> intBox = new Box<>();
        intBox.setValue(123);
        System.out.println("泛型类 Box<Integer>: " + intBox.getValue());

        // ==================== 泛型方法演示 ====================
        // 泛型方法可以在调用时自动推断类型参数
        // <T> String arrayToString(T... array) 中的 T 由传入参数决定
        System.out.println("\n泛型方法:");
        String[] strArray = {"A", "B", "C"};
        Integer[] intArray = {1, 2, 3};
        System.out.println("  arrayToString(strArray): " + arrayToString(strArray));
        System.out.println("  arrayToString(intArray): " + arrayToString(intArray));

        // ==================== 泛型接口演示 ====================
        // 泛型接口的实现类需要指定具体类型
        // StringRepository implements Repository<String>
        System.out.println("\n泛型接口:");
        Repository<String> stringRepo = new StringRepository();
        System.out.println("  Repository<String>.get(): " + stringRepo.get());

        // ==================== 原始类型演示 ====================
        // 原始类型：不指定类型参数的泛型类
        // 风险：失去类型安全，编译器只能给出警告
        System.out.println("\n原始类型（Raw Type）:\n");
        @SuppressWarnings("rawtypes")
        Box rawBox = new Box();  // 没有指定类型参数
        rawBox.setValue("Raw Type");
        System.out.println("  Raw Box: " + rawBox.getValue());
        System.out.println("  警告：失去类型安全保护！\n");
    }

    /**
     * 2. 类型擦除演示
     * 
     * 【核心概念】
     * Java 泛型是"伪泛型"，通过类型擦除实现：
     * - 编译期：泛型信息完整保留，进行严格的类型检查
     * - 运行期：泛型信息被擦除，只保留原始类型（Raw Type）
     * 
     * 【为什么这样设计？】
     * 为了向后兼容 JDK5 之前的代码，Java 选择在字节码层面擦除泛型信息。
     * 这与 C++ 的模板（真泛型）有本质区别。
     * 
     * 【擦除后的影响】
     * 1. List<String> 和 List<Integer> 运行时是同一个类
     * 2. 不能用 instanceof 检查泛型类型
     * 3. 不能创建泛型数组
     */
    private static void demonstrateTypeErasure() {
        System.out.println("【二、类型擦除演示】\n");

        // ==================== 擦除机制说明 ====================
        System.out.println("类型擦除机制：");
        System.out.println("  编译前：List<String> list = new ArrayList<String>();");
        System.out.println("  编译后：List list = new ArrayList();  // 泛型信息被擦除\n");

        // ==================== 验证类型擦除 ====================
        // 核心验证：不同泛型类型的对象在运行时是同一个类
        // 这证明了泛型信息在运行时被擦除
        List<String> stringList = new ArrayList<>();
        List<Integer> intList = new ArrayList<>();

        System.out.println("验证类型擦除：");
        System.out.println("  stringList.getClass() = " + stringList.getClass().getName());
        System.out.println("  intList.getClass() = " + intList.getClass().getName());
        System.out.println("  stringList.getClass() == intList.getClass(): " 
            + (stringList.getClass() == intList.getClass()));
        System.out.println("  结论：运行时 List<String> 和 List<Integer> 是同一个类！\n");

        // ==================== 编译器插入的类型转换 ====================
        // 虽然泛型被擦除，但编译器会自动插入类型转换保证类型安全
        // 源码：String s = list.get(0);
        // 编译后：String s = (String) list.get(0);  // 编译器自动插入
        System.out.println("编译器自动插入的类型转换：");
        System.out.println("  源代码：String s = list.get(0);");
        System.out.println("  编译后：String s = (String) list.get(0);  // 编译器插入\n");
    }

    /**
     * 3. 查看运行时实际的泛型信息
     * 
     * 虽然类型擦除会擦除大部分泛型信息，但以下地方保留了泛型签名（Signature 属性）：
     * 1. 父类泛型信息：通过 getGenericSuperclass() 获取
     * 2. 字段泛型信息：通过 Field.getGenericType() 获取
     * 3. 方法泛型信息：通过 Method.getGenericReturnType() 等获取
     * 
     * 【为什么能保留？】
     * 编译器在字节码中通过 Signature 属性记录了泛型签名。
     * 这是反射API能够获取泛型信息的基础。
     * 
     * 【局限】
     * 局部变量的泛型信息在运行时完全丢失，无法获取。
     */
    private static void demonstrateRuntimeGenericInfo() throws Exception {
        System.out.println("【三、查看运行时实际的泛型信息】\n");

        // ==================== 1. 通过父类获取泛型信息 ====================
        // 创建一个有具体泛型类型的子类
        // 关键：通过继承保留泛型信息
        class StringList extends ArrayList<String> {}
        
        System.out.println("1. 通过 getGenericSuperclass() 查看父类泛型：");
        Type genericSuperclass = StringList.class.getGenericSuperclass();
        System.out.println("   StringList 的父类类型: " + genericSuperclass);
        
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericSuperclass;
            Type[] actualTypeArguments = pt.getActualTypeArguments();
            System.out.println("   实际类型参数: " + Arrays.toString(actualTypeArguments));
            System.out.println("   获取到了 String！这是通过 Signature 属性保留的。\n");
        }

        // ==================== 2. 通过字段获取泛型信息 ====================
        // 字段的泛型信息也会被 Signature 属性保留
        System.out.println("2. 通过字段查看泛型类型：");
        Field mapField = GenericDeepDiveDemo.class.getDeclaredField("genericField");
        System.out.println("   字段声明: " + mapField.getGenericType());
        
        if (mapField.getGenericType() instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) mapField.getGenericType();
            System.out.println("   原始类型: " + pt.getRawType());
            System.out.println("   类型参数: " + Arrays.toString(pt.getActualTypeArguments()) + "\n");
        }

        // ==================== 3. 通过方法获取泛型信息 ====================
        // 方法的返回类型和参数类型的泛型信息也会保留
        System.out.println("3. 通过方法查看泛型信息：");
        Method method = GenericDeepDiveDemo.class.getDeclaredMethod("genericMethod", List.class);
        System.out.println("   方法返回类型: " + method.getGenericReturnType());
        System.out.println("   方法参数类型: " + Arrays.toString(method.getGenericParameterTypes()) + "\n");

        // ==================== 4. 局部变量无法获取 ====================
        // 局部变量的泛型信息在运行时完全丢失
        // 因为字节码中不保留局部变量的泛型签名
        System.out.println("4. 局部变量的泛型信息在运行时丢失：");
        List<String> localList = new ArrayList<>();
        System.out.println("   局部变量 localList 的运行时类型: " + localList.getClass());
        System.out.println("   无法通过反射获取局部变量的泛型类型 <String>\n");
    }

    /**
     * 用于演示的方法
     * 运行时可以通过反射获取其泛型签名信息
     */
    private List<String> genericMethod(List<Integer> list) {
        return new ArrayList<>();
    }

    /**
     * 4. 泛型的编译期检查
     * 
     * 【泛型的工作时机】
     * | 阶段 | 泛型信息 | 操作 |
     * |------|----------|------|
     * | 源码阶段 | 完整保留 | 类型检查、错误报告 |
     * | 编译阶段 | 部分擦除 | 插入类型转换、桥接方法 |
     * | 字节码阶段 | 仅保留签名 | Signature 属性记录 |
     * | 运行阶段 | 完全擦除 | 只知道原始类型 |
     * 
     * 【原始类型的危险】
     * 使用原始类型会失去类型安全保护，编译器只能给出警告。
     */
    private static void demonstrateCompileTimeCheck() {
        System.out.println("【四、泛型的编译期检查】\n");

        // ==================== 泛型工作时机 ====================
        System.out.println("编译期检查时机：");
        System.out.println("  源码阶段：完整保留，类型检查、错误报告");
        System.out.println("  编译阶段：部分擦除，插入类型转换、桥接方法");
        System.out.println("  字节码阶段：仅保留签名（Signature 属性）");
        System.out.println("  运行阶段：完全擦除，只知道原始类型\n");

        // ==================== 编译期检查示例 ====================
        // 编译器会在编译时进行类型检查
        System.out.println("编译期检查示例：");
        System.out.println("  list.add(\"hello\");     // 编译通过");
        System.out.println("  list.add(123);         // 编译错误：类型不匹配\n");

        // ==================== 原始类型的危险 ====================
        // 原始类型绕过了编译器的类型检查
        System.out.println("原始类型的运行时问题：");
        @SuppressWarnings("unchecked")
        List rawList = new ArrayList();
        rawList.add("string");
        rawList.add(123);  // 编译警告，但可以通过
        System.out.println("  原始类型破坏了类型安全，但编译器无法完全阻止\n");
    }

    /**
     * 5. 泛型边界与擦除规则
     * 
     * 【擦除规则】
     * 1. 无界类型参数 <T> → 擦除为 Object
     *    - 编译后 T 被替换为 Object
     *    - 所有对 T 的操作都变成了对 Object 的操作
     * 
     * 2. 有界类型参数 <T extends Number> → 擦除为边界类型 Number
     *    - 编译后 T 被替换为 Number
     *    - 可以调用 Number 的方法
     * 
     * 3. 多边界 <T extends Number & Comparable> → 擦除为第一个边界
     *    - 编译后 T 被替换为第一个边界类型
     *    - 用于保证类型安全和方法调用
     * 
     * 【桥接方法】
     * 当子类泛型实现父类泛型接口时，编译器会生成桥接方法保证多态正确性。
     */
    private static void demonstrateGenericBounds() {
        System.out.println("【五、泛型边界与擦除规则】\n");

        // ==================== 无界类型参数 ====================
        System.out.println("无界类型参数 <T>：");
        System.out.println("  擦除为 Object\n");

        // ==================== 有界类型参数 ====================
        System.out.println("有界类型参数 <T extends Number>：");
        System.out.println("  擦除为边界类型 Number\n");

        // ==================== 多边界类型参数 ====================
        System.out.println("多边界类型参数 <T extends Number & Comparable>：");
        System.out.println("  擦除为第一个边界类型 Number\n");

        // ==================== 验证擦除后的类型 ====================
        // 虽然使用时指定了不同的类型参数，但运行时是同一个类
        NumberBox<Integer> intNumBox = new NumberBox<>();
        NumberBox<Double> doubleNumBox = new NumberBox<>();
        
        System.out.println("验证擦除后的类型：");
        System.out.println("  NumberBox<Integer>.class == NumberBox<Double>.class: " 
            + (intNumBox.getClass() == doubleNumBox.getClass()) + "\n");

        // ==================== 桥接方法演示 ====================
        // 桥接方法：编译器生成的合成方法，用于保证多态的正确性
        // 当子类指定了泛型接口的具体类型时，编译器会生成一个Object版本的方法
        System.out.println("桥接方法（Bridge Method）：");
        System.out.println("  当子类泛型实现父类泛型接口时，编译器会生成桥接方法");
        for (Method m : StringRepository.class.getDeclaredMethods()) {
            System.out.println("    " + m.getName() + " -> " + m.getReturnType().getName() 
                + (m.isBridge() ? " (桥接方法)" : ""));
        }
        System.out.println();
    }

    /**
     * 6. 泛型通配符
     * 
     * 【三种通配符】
     * 1. <?> 无界通配符：表示未知类型
     * 2. <? extends T> 上界通配符：表示 T 或 T 的子类（生产者）
     * 3. <? super T> 下界通配符：表示 T 或 T 的父类（消费者）
     * 
     * 【核心原则：PECS】
     * Producer Extends, Consumer Super
     * - 生产者（只读）：使用 ? extends T
     * - 消费者（只写）：使用 ? super T
     */
    private static void demonstrateWildcards() {
        System.out.println("【六、泛型通配符】\n");

        // ==================== 上界通配符演示 ====================
        List<Integer> integers = Arrays.asList(1, 2, 3);
        List<Double> doubles = Arrays.asList(1.1, 2.2, 3.3);
        List<Number> numbers = new ArrayList<>(Arrays.asList(1, 2, 3));

        System.out.println("上界通配符 <? extends T>：");
        System.out.println("  - 可以读，不能写（除了 null）");
        // sumOfList 接受任何 Number 子类的列表
        double sum = sumOfList(integers) + sumOfList(doubles);
        System.out.println("  sumOfList(integers) + sumOfList(doubles) = " + sum + "\n");

        // ==================== 下界通配符演示 ====================
        System.out.println("下界通配符 <? super T>：");
        System.out.println("  - 可以写，读取类型是 Object");
        // addNumbers 可以向任何 Integer 父类的列表添加元素
        addNumbers(numbers, 5);
        System.out.println("  addNumbers(numbers, 5) 后: " + numbers + "\n");

        // ==================== PECS 原则详解 ====================
        // 详细演示见 demonstratePECSDetailed() 方法
        demonstratePECSDetailed();
    }

    /**
     * PECS 原则详细演示
     * 
     * 【PECS = Producer Extends, Consumer Super】
     * 
     * 这是泛型通配符使用的核心原则，理解的关键在于视角转换：
     * - "生产者/消费者"是从【集合的视角】来看的，不是从"你的视角"
     * 
     * 【生产者 = ? extends T】
     * - 集合生产数据给你的代码消费
     * - 你的代码 ← 数据 ← 集合
     * - 你只能从集合中读取数据
     * - 为什么不能写？因为无法保证类型安全（可能写入不兼容的类型）
     * 
     * 【消费者 = ? super T】
     * - 集合消费你代码产生的数据
     * - 你的代码 → 数据 → 集合
     * - 你只能向集合中写入数据
     * - 为什么不能读？因为读取类型不确定（可能是任何父类）
     * 
     * 【记忆口诀】
     * 餐厅类比：
     * - 厨师（生产者）：做菜给你吃 → 你只能读取/消费他的菜品 → ? extends T
     * - 顾客（消费者）：你做菜给他吃 → 你只能写入/提供菜品给他 → ? super T
     */
    private static void demonstratePECSDetailed() {
        System.out.println("==================================================");
        System.out.println("PECS 原则深入理解");
        System.out.println("==================================================\n");

        System.out.println("核心概念：Producer Extends, Consumer Super\n");
        System.out.println("关键：从【集合的视角】理解生产者/消费者，不是从你的视角！\n");

        // ==================== 场景一：生产者 = ? extends T = 只读 ====================
        // 集合是"生产者"，它生产数据给你的代码消费
        // 数据流向：集合 → 你的代码
        System.out.println("【场景一：生产者 = ? extends T = 只读】");
        System.out.println("----------------------------------------");
        System.out.println("视角：集合是生产者，它生产数据给你的代码消费\n");
        System.out.println("  你的代码 ← 数据 ← 集合（生产者）\n");

        // 创建不同类型的 Number 列表
        List<Integer> intList = Arrays.asList(1, 2, 3, 4, 5);
        List<Double> doubleList = Arrays.asList(1.5, 2.5, 3.5);

        System.out.println("示例：从不同类型的Number列表中读取数据求和\n");
        System.out.println("  List<Integer> intList = [1, 2, 3, 4, 5]");
        System.out.println("  List<Double> doubleList = [1.5, 2.5, 3.5]");
        System.out.println("  ");
        System.out.println("  // 集合作为生产者，可以接受Integer或Double列表");
        System.out.println("  double sum1 = sumFromProducer(intList);    // " + sumFromProducer(intList));
        System.out.println("  double sum2 = sumFromProducer(doubleList); // " + sumFromProducer(doubleList));
        System.out.println();

        // 为什么只能读不能写？类型安全分析
        System.out.println("为什么只能读不能写？");
        System.out.println("  List<Integer> integers = new ArrayList<>();");
        System.out.println("  List<? extends Number> numbers = integers;");
        System.out.println("  ");
        System.out.println("  // 假设能写入：");
        System.out.println("  // numbers.add(3.14);  // Double也是Number子类");
        System.out.println("  // 但底层是Integer列表！放入Double会类型错误");
        System.out.println("  // 编译器禁止写入，保证类型安全\n");

        // ==================== 场景二：消费者 = ? super T = 只写 ====================
        // 集合是"消费者"，它消费你代码产生的数据
        // 数据流向：你的代码 → 集合
        System.out.println("【场景二：消费者 = ? super T = 只写】");
        System.out.println("----------------------------------------");
        System.out.println("视角：集合是消费者，它消费你代码产生的数据\n");
        System.out.println("  你的代码 → 数据 → 集合（消费者）\n");

        // 创建可以接受 Integer 的不同列表
        List<Number> numberList = new ArrayList<>();
        List<Object> objectList = new ArrayList<>();

        System.out.println("示例：向不同类型的列表中写入Integer数据\n");
        System.out.println("  List<Number> numberList = new ArrayList<>();");
        System.out.println("  List<Object> objectList = new ArrayList<>();");
        System.out.println("  ");
        System.out.println("  // 集合作为消费者，可以接受Number或Object列表");
        System.out.println("  writeToConsumer(numberList, 10, 20, 30);");
        writeToConsumer(numberList, 10, 20, 30);
        System.out.println("  writeToConsumer(objectList, 40, 50, 60);");
        writeToConsumer(objectList, 40, 50, 60);
        System.out.println("  ");
        System.out.println("  numberList: " + numberList);
        System.out.println("  objectList: " + objectList);
        System.out.println();

        // 为什么只能写不能读？类型不确定分析
        System.out.println("为什么只能写不能读？");
        System.out.println("  List<Object> objects = new ArrayList<>();");
        System.out.println("  objects.add(\"hello\");  // 放入String");
        System.out.println("  List<? super Integer> integers = objects;");
        System.out.println("  ");
        System.out.println("  // 读取时：");
        System.out.println("  // Integer i = integers.get(0);  // 实际是String!");
        System.out.println("  // 只能当Object读取：Object obj = integers.get(0);");
        System.out.println("  // 编译器无法保证读出的是Integer\n");

        // ==================== 形象类比：餐厅 ====================
        System.out.println("【形象类比：餐厅】");
        System.out.println("----------------------------------------");
        System.out.println("  厨师（生产者）：做菜给你吃 → 你只能读取/消费他的菜品 → ? extends T");
        System.out.println("  顾客（消费者）：你做菜给他吃 → 你只能写入/提供菜品给他 → ? super T\n");

        // ==================== 记忆口诀 ====================
        System.out.println("【记忆口诀】");
        System.out.println("----------------------------------------");
        System.out.println("  PECS = Producer Extends, Consumer Super");
        System.out.println("  ");
        System.out.println("  从集合的视角看：");
        System.out.println("  - 集合给你数据   = 集合是生产者 = 你只能读");
        System.out.println("  - 集合接受数据   = 集合是消费者 = 你只能写");
        System.out.println("  - 又读又写       = 用精确类型 T\n");
    }

    /**
     * 生产者方法：从集合中读取数据
     * 
     * 【为什么使用 ? extends Number？】
     * 因为这个方法的目的是从集合中"读取"数据进行计算。
     * 集合作为"生产者"，向我们提供数据。
     * 
     * 【使用 ? extends Number 的好处】
     * 1. 可以接受 List<Integer>、List<Double>、List<Long> 等任何 Number 子类列表
     * 2. 读取出来的元素可以安全地当作 Number 使用
     * 
     * 【限制】
     * - 不能向 producer 添加任何元素（除了 null）
     * - 因为编译器无法确定 producer 实际指向的是什么类型的列表
     * 
     * @param producer 数据生产者，可以是任何 Number 子类的列表
     * @return 列表中所有元素的和
     */
    private static double sumFromProducer(List<? extends Number> producer) {
        double sum = 0;
        for (Number n : producer) {  // 安全地读取，因为一定是 Number 或其子类
            sum += n.doubleValue();
        }
        // producer.add(1);  // 编译错误！不能写入
        // 原因：producer 可能是 List<Double>，写入 Integer 会导致类型错误
        return sum;
    }

    /**
     * 消费者方法：向集合中写入数据
     * 
     * 【为什么使用 ? super Integer？】
     * 因为这个方法的目的是向集合中"写入"数据。
     * 集合作为"消费者"，接受我们产生的数据。
     * 
     * 【使用 ? super Integer 的好处】
     * 1. 可以向 List<Integer>、List<Number>、List<Object> 等任何 Integer 父类列表写入
     * 2. Integer 可以安全地添加到任何 Integer 或其父类的列表中
     * 
     * 【限制】
     * - 从 consumer 读取的元素只能当作 Object 使用
     * - 因为编译器无法确定 consumer 实际指向的是什么类型的列表
     * 
     * @param consumer 数据消费者，可以是任何 Integer 父类的列表
     * @param values 要写入的 Integer 值
     */
    @SafeVarargs
    private static void writeToConsumer(List<? super Integer> consumer, Integer... values) {
        for (Integer v : values) {
            consumer.add(v);  // 安全地写入，因为 Integer 可以放入任何 Integer 父类的列表
        }
        // Integer i = consumer.get(0);  // 编译错误！读出是Object
        // 原因：consumer 可能是 List<Object>，里面可能存着 String 等其他类型
        // 只能这样读取：Object obj = consumer.get(0);
    }

    /**
     * 辅助方法：使用上界通配符的求和方法
     * 
     * 这是 PECS 中 Producer（生产者）的典型应用
     */
    private static double sumOfList(List<? extends Number> list) {
        double sum = 0;
        for (Number n : list) sum += n.doubleValue();
        return sum;
    }

    /**
     * 辅助方法：使用下界通配符的添加方法
     * 
     * 这是 PECS 中 Consumer（消费者）的典型应用
     */
    private static void addNumbers(List<? super Integer> list, int n) {
        for (int i = 1; i <= n; i++) list.add(i);
    }

    /**
     * 7. 泛型方法的类型推断
     * 
     * 【类型推断的三种方式】
     * 1. 显式指定类型参数：GenericDeepDiveDemo.<String>method()
     * 2. 编译器根据参数类型推断：根据传入参数推断
     * 3. 目标类型推断：根据赋值或返回类型推断
     * 
     * 【Java 7 之后】
     * 菱形语法 <> 允许编译器自动推断类型参数
     * List<String> list = new ArrayList<>();  // 编译器推断为 ArrayList<String>
     */
    private static void demonstrateTypeInference() {
        System.out.println("【七、泛型方法的类型推断】\n");

        System.out.println("类型推断方式：");
        System.out.println("  1. 显式指定类型参数：<String>method()");
        System.out.println("  2. 编译器推断：根据参数类型");
        System.out.println("  3. 目标类型推断：根据赋值或返回类型\n");

        // 方式1：显式指定类型参数
        String s1 = GenericDeepDiveDemo.<String>identity("hello");
        System.out.println("显式指定: GenericDeepDiveDemo.<String>identity(\"hello\") = " + s1);

        // 方式2：编译器根据参数类型推断
        String s2 = identity("world");
        System.out.println("编译器推断: identity(\"world\") = " + s2);

        // 方式3：目标类型推断
        List<String> list = createList();
        System.out.println("目标类型推断: List<String> list = createList()\n");
    }

    /**
     * 泛型方法示例：返回传入的值
     */
    private static <T> T identity(T t) { return t; }
    
    /**
     * 泛型方法示例：创建空列表
     */
    private static <T> List<T> createList() { return new ArrayList<>(); }

    /**
     * 8. 泛型与数组
     * 
     * 【为什么不能创建泛型数组？】
     * 
     * 核心原因：数组是协变的，泛型是不变的
     * 
     * 【协变 vs 不变】
     * - 协变：Object[] 可以存储 String[]（如果允许泛型数组会出问题）
     * - 不变：List<Object> 不能存储 List<String>
     * 
     * 【类型安全问题】
     * 假设允许创建泛型数组：
     * List<String>[] stringLists = new List<String>[1];  // 假设合法
     * Object[] objects = stringLists;  // 数组协变，合法
     * objects[0] = new ArrayList<Integer>();  // 泛型擦除，运行时通过
     * String s = stringLists[0].get(0);  // ClassCastException!
     * 
     * 【解决方案】
     * 1. 使用 List 代替数组：List<List<String>>
     * 2. 使用原始类型数组 + @SuppressWarnings（不推荐）
     */
    private static void demonstrateGenericArray() {
        System.out.println("【八、泛型与数组】\n");

        System.out.println("不能创建泛型数组的原因：");
        System.out.println("  数组是协变的，而泛型是不变的");
        System.out.println("  如果允许创建泛型数组，会产生类型安全问题\n");

        System.out.println("解决方案：");
        System.out.println("  1. 使用 List 代替数组");
        System.out.println("  2. 使用原始类型数组 + @SuppressWarnings\n");

        // 方案2示例：使用原始类型数组（不推荐，仅用于演示）
        @SuppressWarnings("unchecked")
        List<String>[] array = (List<String>[]) new List[10];
        array[0] = new ArrayList<>();
        array[0].add("test");
        System.out.println("方案示例: List<String>[] array = (List<String>[]) new List[10]");
        System.out.println("array[0] = " + array[0] + "\n");
    }

    /**
     * 9. 高频面试题
     * 
     * 总结泛型相关的核心面试问题，涵盖：
     * 1. 类型擦除原理
     * 2. 泛型运行时特性
     * 3. PECS 原则
     * 4. 桥接方法
     * 5. 泛型与其他语言的对比
     */
    private static void showInterviewQuestions() {
        System.out.println("【九、高频面试题】\n");

        // Q1: 类型擦除
        System.out.println("Q1: 什么是类型擦除？为什么 Java 要这样设计？");
        System.out.println("答案：");
        System.out.println("  类型擦除：编译时泛型信息存在，运行时被擦除");
        System.out.println("  设计原因：向后兼容、迁移平滑、实现简单\n");

        // Q2: List<String> 和 List<Integer> 运行时区别
        System.out.println("Q2: List<String> 和 List<Integer> 在运行时有什么区别？");
        System.out.println("答案：运行时完全没有区别！都擦除为 List\n");

        // Q3: 运行时获取泛型信息
        System.out.println("Q3: 泛型信息在运行时还能获取吗？");
        System.out.println("答案：通过 Signature 属性可获取父类、字段、方法的泛型信息\n");

        // Q4: 泛型数组
        System.out.println("Q4: 为什么不能创建泛型数组？");
        System.out.println("答案：数组协变+泛型不变=类型安全问题\n");

        // Q5: PECS 原则（重点）
        System.out.println("Q5: 什么是 PECS 原则？为什么只读是生产者，只写是消费者？");
        System.out.println("答案：Producer Extends, Consumer Super");
        System.out.println("  关键：从集合的视角理解，不是从你的视角！");
        System.out.println("  ");
        System.out.println("  生产者（? extends T）：集合给你数据 → 你只能读");
        System.out.println("  消费者（? super T）：集合接受数据 → 你只能写");
        System.out.println("  ");
        System.out.println("  类比：厨师生产菜品给你吃（只读），顾客消费你做的菜（只写）\n");

        // Q6: 桥接方法
        System.out.println("Q6: 什么是桥接方法？");
        System.out.println("答案：编译器生成的合成方法，保证多态正确性\n");

        // Q7: 泛型工作时机
        System.out.println("Q7: 泛型的工作时机是什么？");
        System.out.println("答案：编译期检查+擦除，运行期只有原始类型\n");

        // Q8: Java 泛型 vs C++ 模板
        System.out.println("Q8: 泛型和 C++ 模板有什么区别？");
        System.out.println("答案：Java 泛型是伪泛型（类型擦除），C++ 模板是真泛型\n");
    }

    // ==================== 内部类定义 ====================

    /**
     * 泛型类示例：一个简单的盒子类
     * 
     * <T> 是类型参数，可以在使用时指定任意类型
     * 
     * 使用示例：
     * - Box<String> stringBox = new Box<>();
     * - Box<Integer> intBox = new Box<>();
     */
    static class Box<T> {
        private T value;
        public void setValue(T value) { this.value = value; }
        public T getValue() { return value; }
    }

    /**
     * 有界泛型类示例：只能存储 Number 及其子类
     * 
     * <T extends Number> 限定了类型参数的上界
     * - 只能使用 Number 或其子类（Integer、Double、Long 等）
     * - 擦除后类型变为 Number，而不是 Object
     * 
     * 使用示例：
     * - NumberBox<Integer> intBox = new NumberBox<>();  // 正确
     * - NumberBox<Double> doubleBox = new NumberBox<>();  // 正确
     * - NumberBox<String> strBox = new NumberBox<>();  // 编译错误！
     */
    static class NumberBox<T extends Number> {
        private T value;
        public void setValue(T value) { this.value = value; }
        public T getValue() { return value; }
    }

    /**
     * 泛型接口示例：仓库接口
     * 
     * 定义了一个简单的泛型接口，返回指定类型的数据
     */
    interface Repository<T> { T get(); }

    /**
     * 泛型接口实现示例
     * 
     * StringRepository 实现了 Repository<String>，指定了具体类型
     * 
     * 【桥接方法说明】
     * 编译器会为这个类生成一个桥接方法：
     * - public Object get() { return get(); }  // 桥接方法
     * - public String get() { return "..."; }  // 实际方法
     * 
     * 桥接方法保证了多态的正确性，使得 Repository<Object>.get() 可以正确调用
     */
    static class StringRepository implements Repository<String> {
        @Override
        public String get() { return "Hello from StringRepository"; }
    }

    @SafeVarargs
    private static <T> String arrayToString(T... array) { return Arrays.toString(array); }
}
