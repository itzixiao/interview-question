package cn.itzixiao.interview.java.basic;

/**
 * Java 基础数据类型详解
 * <p>
 * ┌─────────────────────────────────────────────────────────────┐
 * │  基本数据类型（8种）                                          │
 * │  ┌──────────┬─────────┬────────────────┬───────────────┐   │
 * │  │   类型    │  位数   │     范围        │    默认值      │   │
 * │  ├──────────┼─────────┼────────────────┼───────────────┤   │
 * │  │   byte   │   8     │   -128 ~ 127   │       0       │   │
 * │  │  short   │   16    │  -32768 ~ 32767│       0       │   │
 * │  │   int    │   32    │   -2^31 ~ 2^31-1│      0       │   │
 * │  │   long   │   64    │   -2^63 ~ 2^63-1│      0L      │   │
 * │  │  float   │   32    │  IEEE 754      │      0.0f    │   │
 * │  │  double  │   64    │  IEEE 754      │      0.0d    │   │
 * │  │   char   │   16    │  0 ~ 65535     │     '\u0000' │   │
 * │  │  boolean │   1     │  true/false    │     false    │   │
 * │  └──────────┴─────────┴────────────────┴───────────────┘   │
 * ├─────────────────────────────────────────────────────────────┤
 * │  引用数据类型                                                │
 * │  - 类（Class）                                               │
 * │  - 接口（Interface）                                          │
 * │  - 数组（Array）                                              │
 * │  - 枚举（Enum）                                               │
 * └─────────────────────────────────────────────────────────────┘
 */
public class JavaDataTypesDemo {

    public static void main(String[] args) {
        System.out.println("========== Java 基础数据类型 ==========\n");

        demonstratePrimitiveTypes();
        demonstrateTypeConversion();
        demonstrateWrapperTypes();
        demonstrateAutoBoxing();
        demonstrateCacheMechanism();
    }

    /**
     * 1. 基本数据类型
     */
    public static void demonstratePrimitiveTypes() {
        System.out.println("【1. 基本数据类型】\n");

        // 整数类型
        byte b = 127;                    // -128 ~ 127
        short s = 32767;                 // -32768 ~ 32767
        int i = 2147483647;              // -2^31 ~ 2^31-1
        long l = 9223372036854775807L;   // -2^63 ~ 2^63-1，需要加 L

        System.out.println("byte: " + b + " (8位，-128 ~ 127)");
        System.out.println("short: " + s + " (16位，-32768 ~ 32767)");
        System.out.println("int: " + i + " (32位，约 ±21亿)");
        System.out.println("long: " + l + " (64位，需要加 L)");

        // 浮点类型
        float f = 3.1415926f;            // 32位，需要加 f
        double d = 3.141592653589793;    // 64位，默认

        System.out.println("\nfloat: " + f + " (32位，需要加 f)");
        System.out.println("double: " + d + " (64位，默认浮点类型)");

        // 字符类型
        char c1 = 'A';                   // 单个字符
        char c2 = 65;                    // ASCII 码
        char c3 = '\u0041';              // Unicode

        System.out.println("\nchar: '" + c1 + "' (16位，Unicode字符)");
        System.out.println("char(65): '" + c2 + "'");
        System.out.println("char(\\u0041): '" + c3 + "'");

        // 布尔类型
        boolean flag = true;
        System.out.println("\nboolean: " + flag + " (true/false)");

        System.out.println("\n【注意】");
        System.out.println("  - 基本数据类型存储的是值本身");
        System.out.println("  - 存储在栈中（方法内局部变量）");
        System.out.println("  - 作为成员变量时，存储在堆中\n");
    }

    /**
     * 2. 类型转换
     */
    public static void demonstrateTypeConversion() {
        System.out.println("【2. 类型转换】\n");

        // 自动类型转换（小 → 大）
        byte b = 10;
        short s = b;      // byte → short
        int i = s;        // short → int
        long l = i;       // int → long
        float f = l;      // long → float
        double d = f;     // float → double

        System.out.println("自动类型转换（小 → 大）:");
        System.out.println("byte(" + b + ") → short(" + s + ") → int(" + i + ") → long(" + l + ")");
        System.out.println("→ float(" + f + ") → double(" + d + ")");

        // 强制类型转换（大 → 小）
        System.out.println("\n强制类型转换（大 → 小）:");
        double d2 = 123.456;
        int i2 = (int) d2;    // 截断小数部分
        System.out.println("double " + d2 + " → int " + i2);

        int i3 = 130;
        byte b2 = (byte) i3;  // 溢出，-126
        System.out.println("int " + i3 + " → byte " + b2 + " (溢出!)");

        // 表达式类型提升
        System.out.println("\n表达式类型提升:");
        byte a = 10;
        byte c = 20;
        // byte sum = a + c;  // 编译错误！a + c 结果是 int
        byte sum = (byte) (a + c);
        System.out.println("byte + byte = int，需要强制转换");

        System.out.println("\n【类型转换规则】");
        System.out.println("  1. 自动转换：小范围 → 大范围（安全）");
        System.out.println("  2. 强制转换：大范围 → 小范围（可能丢失精度）");
        System.out.println("  3. 表达式中自动提升为 int 或更大类型\n");
    }

    /**
     * 3. 包装类
     */
    public static void demonstrateWrapperTypes() {
        System.out.println("【3. 包装类】\n");

        // 基本类型 → 包装类
        System.out.println("基本类型 → 包装类:");
        Integer intObj = Integer.valueOf(100);
        Double doubleObj = Double.valueOf(3.14);
        Boolean boolObj = Boolean.valueOf(true);
        Character charObj = Character.valueOf('A');

        System.out.println("Integer: " + intObj);
        System.out.println("Double: " + doubleObj);
        System.out.println("Boolean: " + boolObj);
        System.out.println("Character: " + charObj);

        // 包装类 → 基本类型
        System.out.println("\n包装类 → 基本类型:");
        int i = intObj.intValue();
        double d = doubleObj.doubleValue();
        boolean b = boolObj.booleanValue();
        char c = charObj.charValue();

        System.out.println("int: " + i);
        System.out.println("double: " + d);
        System.out.println("boolean: " + b);
        System.out.println("char: " + c);

        // 常用方法
        System.out.println("\n【包装类常用方法】");
        System.out.println("Integer.parseInt(\"123\"): " + Integer.parseInt("123"));
        System.out.println("Integer.toBinaryString(10): " + Integer.toBinaryString(10));
        System.out.println("Integer.toHexString(255): " + Integer.toHexString(255));
        System.out.println("Integer.MAX_VALUE: " + Integer.MAX_VALUE);
        System.out.println("Integer.MIN_VALUE: " + Integer.MIN_VALUE);
        System.out.println("Integer.SIZE: " + Integer.SIZE + " bits");

        System.out.println("\n【包装类特点】");
        System.out.println("  - 包装类是对象，存储在堆中");
        System.out.println("  - 提供了丰富的方法");
        System.out.println("  - 集合中只能使用包装类\n");
    }

    /**
     * 4. 自动装箱与拆箱
     */
    public static void demonstrateAutoBoxing() {
        System.out.println("【4. 自动装箱与拆箱】\n");

        // 自动装箱（基本类型 → 包装类）
        Integer i = 100;  // 编译后: Integer.valueOf(100)
        System.out.println("自动装箱: Integer i = 100");

        // 自动拆箱（包装类 → 基本类型）
        int n = i;        // 编译后: i.intValue()
        System.out.println("自动拆箱: int n = i");

        // 运算时的自动拆箱
        Integer a = 100;
        Integer b = 200;
        Integer sum = a + b;  // 先拆箱，计算，再装箱
        System.out.println("\nInteger 运算: " + a + " + " + b + " = " + sum);

        // 注意事项
        System.out.println("\n【注意事项】");

        // 1. 空指针风险
        Integer nullInt = null;
        try {
            int x = nullInt;  // 自动拆箱，NPE！
        } catch (NullPointerException e) {
            System.out.println("1. 空指针风险: Integer null 自动拆箱会 NPE");
        }

        // 2. 性能影响
        System.out.println("\n2. 性能影响:");
        long start = System.currentTimeMillis();
        Integer sum2 = 0;
        for (int j = 0; j < 1000000; j++) {
            sum2 += j;  // 频繁装箱拆箱
        }
        System.out.println("   使用 Integer: " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        int sum3 = 0;
        for (int j = 0; j < 1000000; j++) {
            sum3 += j;  // 基本类型，无装箱
        }
        System.out.println("   使用 int: " + (System.currentTimeMillis() - start) + " ms");

        System.out.println("\n【建议】循环计算使用基本类型\n");
    }

    /**
     * 5. 缓存机制
     */
    public static void demonstrateCacheMechanism() {
        System.out.println("【5. 包装类缓存机制】\n");

        // Integer 缓存（-128 ~ 127）
        System.out.println("Integer 缓存（-128 ~ 127）:");

        Integer a = 100;
        Integer b = 100;
        System.out.println("Integer a = 100, Integer b = 100");
        System.out.println("a == b: " + (a == b));  // true，缓存同一对象

        Integer c = 200;
        Integer d = 200;
        System.out.println("\nInteger c = 200, Integer d = 200");
        System.out.println("c == d: " + (c == d));  // false，超出缓存范围

        // 其他包装类的缓存
        System.out.println("\n【其他包装类缓存】");
        System.out.println("Byte: -128 ~ 127");
        System.out.println("Short: -128 ~ 127");
        System.out.println("Long: -128 ~ 127");
        System.out.println("Character: 0 ~ 127");
        System.out.println("Boolean: true, false");
        System.out.println("Float/Double: 无缓存");

        // 比较建议
        System.out.println("\n【比较建议】");
        System.out.println("  - 使用 equals() 比较值");
        System.out.println("  - 或使用 intValue() 后比较");
        System.out.println("  - 避免使用 == 比较包装类\n");
    }
}
