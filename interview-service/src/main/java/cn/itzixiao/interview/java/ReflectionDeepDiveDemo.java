package cn.itzixiao.interview.java;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Java 反射机制深入理解 - 教学型详解
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                           Java 反射机制总览                                 │
 * │                                                                             │
 * │  ┌─────────────────────────────────────────────────────────────────────┐   │
 * │  │                        什么是反射？                                  │   │
 * │  │  反射是 Java 在运行时动态获取类信息并操作类的能力                    │   │
 * │  │  - 运行时获取任意类的属性和方法                                     │   │
 * │  │  - 运行时创建对象实例                                               │   │
 * │  │  - 运行时调用对象方法                                               │   │
 * │  │  - 运行时修改对象属性                                               │   │
 * │  └─────────────────────────────────────────────────────────────────────┘   │
 * │                                                                             │
 * │  ┌─────────────────────────────────────────────────────────────────────┐   │
 * │  │                        反射的核心类                                  │   │
 * │  │                                                                     │   │
 * │  │  java.lang.Class        - 类的元数据（入口）                        │   │
 * │  │  java.lang.reflect.Constructor - 构造器                             │   │
 * │  │  java.lang.reflect.Method      - 方法                               │   │
 * │  │  java.lang.reflect.Field       - 字段                               │   │
 * │  │  java.lang.reflect.Modifier    - 修饰符                             │   │
 * │  │  java.lang.annotation.Annotation - 注解                             │   │
 * │  └─────────────────────────────────────────────────────────────────────┘   │
 * │                                                                             │
 * │  ┌─────────────────────────────────────────────────────────────────────┐   │
 * │  │                        反射的应用场景                                │   │
 * │  │                                                                     │   │
 * │  │  1. 框架开发：Spring IOC、MyBatis ORM                               │   │
 * │  │  2. 动态代理：AOP、RPC 远程调用                                     │   │
 * │  │  3. 注解处理：@Autowired、@RequestMapping                           │   │
 * │  │  4. 序列化/反序列化：JSON、XML                                      │   │
 * │  │  5. IDE 功能：代码提示、自动补全                                    │   │
 * │  │  6. 单元测试：Mock 框架                                             │   │
 * │  └─────────────────────────────────────────────────────────────────────┘   │
 * │                                                                             │
 * │  ┌─────────────────────────────────────────────────────────────────────┐   │
 * │  │                        反射的性能问题                                │   │
 * │  │                                                                     │   │
 * │  │  1. 类型检查：绕过编译时类型检查，运行时动态检查                    │   │
 * │  │  2. 安全检查：每次反射调用都有权限验证开销                          │   │
 * │  │  3. JIT 优化受限：无法内联，无法进行逃逸分析                        │   │
 * │  │  4. 内存开销：Method/Field 对象占用额外内存                         │   │
 * │  │                                                                     │   │
 * │  │  优化方案：                                                         │   │
 * │  │  - setAccessible(true) 关闭安全检查                                │   │
 * │  │  - 缓存 Method/Field 对象                                           │   │
 * │  │  - 使用 MethodHandle (Java 7+)                                      │   │
 * │  │  - 使用 LambdaMetafactory (Java 8+)                                 │   │
 * │  └─────────────────────────────────────────────────────────────────────┘   │
 * └─────────────────────────────────────────────────────────────────────────────┘
 */
public class ReflectionDeepDiveDemo {

    /**
     * 1. 获取 Class 对象的三种方式
     */
    public void getClassObject() throws ClassNotFoundException {
        System.out.println("========== 获取 Class 对象 ==========\n");

        // 方式1：类名.class
        Class<String> clazz1 = String.class;
        System.out.println("【方式1】类名.class");
        System.out.println("  String.class = " + clazz1);

        // 方式2：对象.getClass()
        String str = "Hello";
        Class<? extends String> clazz2 = str.getClass();
        System.out.println("\n【方式2】对象.getClass()");
        System.out.println("  \"Hello\".getClass() = " + clazz2);

        // 方式3：Class.forName()
        Class<?> clazz3 = Class.forName("java.lang.String");
        System.out.println("\n【方式3】Class.forName()");
        System.out.println("  Class.forName(\"java.lang.String\") = " + clazz3);

        // 验证三个 Class 对象是同一个
        System.out.println("\n【验证】三个 Class 对象是否相同：");
        System.out.println("  clazz1 == clazz2: " + (clazz1 == clazz2));
        System.out.println("  clazz2 == clazz3: " + (clazz2 == clazz3));
        System.out.println("  （每个类在 JVM 中只有一个 Class 对象）\n");

        // 基本类型的 Class 对象
        System.out.println("【基本类型的 Class】");
        System.out.println("  int.class = " + int.class);
        System.out.println("  Integer.TYPE = " + Integer.TYPE);
        System.out.println("  int.class == Integer.TYPE: " + (int.class == Integer.TYPE));
        System.out.println("  int.class == Integer.class: " + (int.class == Integer.class) + "\n");
    }

    /**
     * 2. Class 对象常用方法
     */
    public void classMethods() {
        System.out.println("========== Class 对象常用方法 ==========\n");

        Class<?> clazz = Person.class;

        // 类信息
        System.out.println("【类信息】");
        System.out.println("  getName(): " + clazz.getName());           // 全限定名
        System.out.println("  getSimpleName(): " + clazz.getSimpleName()); // 简单名
        System.out.println("  getPackage(): " + clazz.getPackage());     // 包信息
        System.out.println("  getSuperclass(): " + clazz.getSuperclass()); // 父类
        System.out.println("  getInterfaces(): " + Arrays.toString(clazz.getInterfaces())); // 接口\n");

        // 类型判断
        System.out.println("【类型判断】");
        System.out.println("  isInterface(): " + clazz.isInterface());
        System.out.println("  isArray(): " + clazz.isArray());
        System.out.println("  isPrimitive(): " + clazz.isPrimitive());
        System.out.println("  isAnnotation(): " + clazz.isAnnotation());
        System.out.println("  isEnum(): " + clazz.isEnum());
        System.out.println("  isInstance(Object): " + clazz.isInstance(new Person()) + "\n");

        // 修饰符
        System.out.println("【修饰符】");
        int modifiers = clazz.getModifiers();
        System.out.println("  getModifiers(): " + modifiers);
        System.out.println("  isPublic(): " + Modifier.isPublic(modifiers));
        System.out.println("  isFinal(): " + Modifier.isFinal(modifiers));
        System.out.println("  Modifier.toString(): " + Modifier.toString(modifiers) + "\n");
    }

    /**
     * 3. 构造器反射
     */
    public void constructorReflection() throws Exception {
        System.out.println("========== 构造器反射 ==========\n");

        Class<Person> clazz = Person.class;

        // 获取所有构造器
        System.out.println("【获取构造器】");
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            System.out.println("  " + constructor);
        }

        // 获取指定构造器
        System.out.println("\n【获取指定构造器】");
        Constructor<Person> noArgConstructor = clazz.getDeclaredConstructor();
        System.out.println("  无参构造器: " + noArgConstructor);

        Constructor<Person> fullConstructor = clazz.getDeclaredConstructor(String.class, int.class);
        System.out.println("  全参构造器: " + fullConstructor);

        // 创建对象
        System.out.println("\n【创建对象】");
        Person person1 = noArgConstructor.newInstance();
        System.out.println("  无参构造创建: " + person1);

        Person person2 = fullConstructor.newInstance("张三", 25);
        System.out.println("  全参构造创建: " + person2);

        // 访问私有构造器
        System.out.println("\n【访问私有构造器】");
        Constructor<Person> privateConstructor = clazz.getDeclaredConstructor(String.class);
        privateConstructor.setAccessible(true);  // 关闭安全检查
        Person person3 = privateConstructor.newInstance("私有构造");
        System.out.println("  私有构造创建: " + person3 + "\n");
    }

    /**
     * 4. 方法反射
     */
    public void methodReflection() throws Exception {
        System.out.println("========== 方法反射 ==========\n");

        Class<Person> clazz = Person.class;
        Person person = new Person("李四", 30);

        // 获取所有方法
        System.out.println("【获取方法】");
        Method[] methods = clazz.getDeclaredMethods();
        System.out.println("  声明的方法数: " + methods.length);

        // 获取所有公共方法（包括继承的）
        Method[] publicMethods = clazz.getMethods();
        System.out.println("  公共方法数（含继承）: " + publicMethods.length + "\n");

        // 获取指定方法
        System.out.println("【获取指定方法】");
        Method getNameMethod = clazz.getMethod("getName");
        System.out.println("  getMethod(\"getName\"): " + getNameMethod);

        Method setAgeMethod = clazz.getMethod("setAge", int.class);
        System.out.println("  getMethod(\"setAge\", int.class): " + setAgeMethod + "\n");

        // 调用方法
        System.out.println("【调用方法】");
        String name = (String) getNameMethod.invoke(person);
        System.out.println("  invoke getName(): " + name);

        setAgeMethod.invoke(person, 35);
        System.out.println("  invoke setAge(35): " + person + "\n");

        // 调用私有方法
        System.out.println("【调用私有方法】");
        Method privateMethod = clazz.getDeclaredMethod("privateMethod");
        privateMethod.setAccessible(true);
        privateMethod.invoke(person);

        // 调用静态方法
        System.out.println("\n【调用静态方法】");
        Method staticMethod = clazz.getMethod("staticMethod");
        staticMethod.invoke(null);
        System.out.println();
    }

    /**
     * 5. 字段反射
     */
    public void fieldReflection() throws Exception {
        System.out.println("========== 字段反射 ==========\n");

        Class<Person> clazz = Person.class;
        Person person = new Person("王五", 40);

        // 获取所有字段
        System.out.println("【获取字段】");
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            System.out.println("  " + field);
        }

        // 获取指定字段
        System.out.println("\n【获取指定字段】");
        Field nameField = clazz.getDeclaredField("name");
        System.out.println("  name 字段: " + nameField);

        Field ageField = clazz.getDeclaredField("age");
        System.out.println("  age 字段: " + ageField + "\n");

        // 读取字段值
        System.out.println("【读取字段值】");
        nameField.setAccessible(true);
        String name = (String) nameField.get(person);
        System.out.println("  name.get(person): " + name);

        ageField.setAccessible(true);
        int age = (int) ageField.get(person);
        System.out.println("  age.get(person): " + age + "\n");

        // 修改字段值
        System.out.println("【修改字段值】");
        nameField.set(person, "修改后的名字");
        ageField.set(person, 50);
        System.out.println("  修改后: " + person + "\n");

        // 修改静态字段
        System.out.println("【修改静态字段】");
        Field staticField = clazz.getDeclaredField("staticField");
        staticField.setAccessible(true);
        System.out.println("  修改前 staticField: " + staticField.get(null));
        staticField.set(null, "新静态值");
        System.out.println("  修改后 staticField: " + staticField.get(null) + "\n");
    }

    /**
     * 6. 注解反射
     */
    public void annotationReflection() {
        System.out.println("========== 注解反射 ==========\n");

        Class<AnnotatedClass> clazz = AnnotatedClass.class;

        // 获取类上的注解
        System.out.println("【类注解】");
        MyAnnotation classAnnotation = clazz.getAnnotation(MyAnnotation.class);
        if (classAnnotation != null) {
            System.out.println("  注解值: " + classAnnotation.value());
            System.out.println("  描述: " + classAnnotation.description());
        }

        // 获取方法上的注解
        System.out.println("\n【方法注解】");
        try {
            Method method = clazz.getMethod("annotatedMethod");
            MyAnnotation methodAnnotation = method.getAnnotation(MyAnnotation.class);
            if (methodAnnotation != null) {
                System.out.println("  方法注解值: " + methodAnnotation.value());
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        // 获取字段上的注解
        System.out.println("\n【字段注解】");
        try {
            Field field = clazz.getDeclaredField("annotatedField");
            MyAnnotation fieldAnnotation = field.getAnnotation(MyAnnotation.class);
            if (fieldAnnotation != null) {
                System.out.println("  字段注解值: " + fieldAnnotation.value());
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        // 获取所有注解
        System.out.println("\n【获取所有注解】");
        java.lang.annotation.Annotation[] annotations = clazz.getAnnotations();
        System.out.println("  类上的注解数: " + annotations.length);

        // 判断是否有指定注解
        System.out.println("\n【判断注解存在】");
        boolean hasAnnotation = clazz.isAnnotationPresent(MyAnnotation.class);
        System.out.println("  isAnnotationPresent(MyAnnotation.class): " + hasAnnotation + "\n");
    }

    /**
     * 7. 反射应用：动态代理
     */
    public void dynamicProxy() {
        System.out.println("========== 动态代理 ==========\n");

        // 创建真实对象
        RealSubject realSubject = new RealSubject();

        // 创建代理对象
        Subject proxy = (Subject) Proxy.newProxyInstance(
                Subject.class.getClassLoader(),
                new Class[]{Subject.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        System.out.println("  [代理] 方法调用前: " + method.getName());
                        Object result = method.invoke(realSubject, args);
                        System.out.println("  [代理] 方法调用后: " + method.getName());
                        return result;
                    }
                }
        );

        // 通过代理调用方法
        System.out.println("【动态代理调用】");
        proxy.request();
        System.out.println();
    }

    /**
     * 8. 反射性能测试
     */
    public void performanceTest() throws Exception {
        System.out.println("========== 反射性能测试 ==========\n");

        Person person = new Person("性能测试", 20);
        int count = 10000000;

        // 直接调用
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            person.getName();
        }
        long directTime = System.currentTimeMillis() - start;
        System.out.println("【直接调用】" + count + " 次: " + directTime + "ms");

        // 反射调用（未优化）
        Method method = Person.class.getMethod("getName");
        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            method.invoke(person);
        }
        long reflectTime = System.currentTimeMillis() - start;
        System.out.println("【反射调用】" + count + " 次: " + reflectTime + "ms");
        System.out.println("  性能差距: " + (reflectTime / (double) directTime) + " 倍\n");

        // 反射调用（优化：setAccessible）
        method.setAccessible(true);
        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            method.invoke(person);
        }
        long optimizedTime = System.currentTimeMillis() - start;
        System.out.println("【反射调用（优化）】" + count + " 次: " + optimizedTime + "ms");
        System.out.println("  setAccessible(true) 可提升约 " +
                (reflectTime / (double) optimizedTime) + " 倍性能\n");
    }

    /**
     * 9. 反射破坏单例模式
     */
    public void breakSingleton() throws Exception {
        System.out.println("========== 反射破坏单例 ==========\n");

        // 正常获取单例
        Singleton instance1 = Singleton.getInstance();
        Singleton instance2 = Singleton.getInstance();
        System.out.println("【正常获取】");
        System.out.println("  instance1: " + instance1);
        System.out.println("  instance2: " + instance2);
        System.out.println("  是否相同: " + (instance1 == instance2) + "\n");

        // 反射创建新实例
        System.out.println("【反射破坏】");
        Constructor<Singleton> constructor = Singleton.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Singleton instance3 = constructor.newInstance();
        System.out.println("  instance3: " + instance3);
        System.out.println("  是否相同: " + (instance1 == instance3));
        System.out.println("  （反射可以绕过私有构造器创建新实例）\n");
    }

    /**
     * 10. MethodHandle - 高性能反射替代方案
     */
    public void methodHandleDemo() throws Throwable {
        System.out.println("========== MethodHandle 高性能反射 ==========\n");

        System.out.println("【什么是 MethodHandle？】");
        System.out.println("  - Java 7 引入的新特性");
        System.out.println("  - 比反射更快，更接近 JVM 底层");
        System.out.println("  - 支持 JIT 优化（可以内联）");
        System.out.println("  - 类型安全，编译期检查\n");

        Person person = new Person("张三", 25);

        // 获取 MethodHandles.Lookup
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        // 获取 getter 方法
        MethodHandle getNameHandle = lookup.findVirtual(
                Person.class,
                "getName",
                MethodType.methodType(String.class)
        );

        // 获取 setter 方法
        MethodHandle setNameHandle = lookup.findVirtual(
                Person.class,
                "setName",
                MethodType.methodType(void.class, String.class)
        );

        System.out.println("【MethodHandle 调用】");
        String name = (String) getNameHandle.invoke(person);
        System.out.println("  getName: " + name);

        setNameHandle.invoke(person, "李四");
        System.out.println("  setName(\"李四\"): " + person);

        // 性能对比
        System.out.println("\n【性能对比】");
        int count = 1000000;

        // 直接调用
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            person.getName();
        }
        System.out.println("  直接调用: " + (System.currentTimeMillis() - start) + "ms");

        // 反射调用
        Method method = Person.class.getMethod("getName");
        method.setAccessible(true);
        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            method.invoke(person);
        }
        System.out.println("  反射调用: " + (System.currentTimeMillis() - start) + "ms");

        // MethodHandle 调用
        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            getNameHandle.invoke(person);
        }
        System.out.println("  MethodHandle: " + (System.currentTimeMillis() - start) + "ms\n");
    }

    /**
     * 11. 反射修改 final 字段
     */
    public void modifyFinalField() throws Exception {
        System.out.println("========== 反射修改 final 字段 ==========\n");

        System.out.println("【重要说明】");
        System.out.println("  - 反射可以修改 final 字段");
        System.out.println("  - 但基本类型和 String 可能被编译器内联优化");
        System.out.println("  - 导致修改后看起来“没有生效”\n");

        FinalFieldDemo demo = new FinalFieldDemo();
        System.out.println("【修改前】");
        System.out.println("  finalField = " + demo.finalField);
        System.out.println("  getFinalField() = " + demo.getFinalField());

        // 修改 final 字段
        Field field = FinalFieldDemo.class.getDeclaredField("finalField");
        field.setAccessible(true);

        // 移除 final 修饰符（Java 8 可用，Java 12+ 受限）
        // Field modifiersField = Field.class.getDeclaredField("modifiers");
        // modifiersField.setAccessible(true);
        // modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(demo, "修改后的值");

        System.out.println("\n【修改后】");
        System.out.println("  通过反射获取: " + field.get(demo));
        System.out.println("  通过方法获取: " + demo.getFinalField());
        System.out.println("  注意：如果直接访问 demo.finalField，可能显示原值（编译器内联）\n");
    }

    /**
     * 12. 反射操作数组
     */
    public void arrayReflection() {
        System.out.println("========== 反射操作数组 ==========\n");

        // 创建数组
        System.out.println("【动态创建数组】");
        int[] intArray = (int[]) Array.newInstance(int.class, 5);
        System.out.println("  创建 int[5]: " + Arrays.toString(intArray));

        String[] stringArray = (String[]) Array.newInstance(String.class, 3);
        System.out.println("  创建 String[3]: " + Arrays.toString(stringArray));

        // 设置和获取元素
        System.out.println("\n【操作数组元素】");
        Array.set(intArray, 0, 10);
        Array.set(intArray, 1, 20);
        Array.set(intArray, 2, 30);
        System.out.println("  设置后: " + Arrays.toString(intArray));

        int value = (int) Array.get(intArray, 1);
        System.out.println("  获取 index=1: " + value);

        // 获取数组信息
        System.out.println("\n【数组类型信息】");
        Class<?> arrayClass = intArray.getClass();
        System.out.println("  isArray(): " + arrayClass.isArray());
        System.out.println("  getComponentType(): " + arrayClass.getComponentType());
        System.out.println("  Array.getLength(): " + Array.getLength(intArray) + "\n");
    }

    /**
     * 13. 泛型与反射
     */
    public void genericReflection() throws Exception {
        System.out.println("========== 泛型与反射 ==========\n");

        System.out.println("【泛型擦除】");
        System.out.println("  - 编译时：泛型用于类型检查");
        System.out.println("  - 运行时：泛型信息被擦除");
        System.out.println("  - 反射可以绕过泛型检查\n");

        // 演示绕过泛型检查
        java.util.List<String> stringList = new java.util.ArrayList<>();
        stringList.add("正常添加");

        // 通过反射添加 Integer
        Method addMethod = java.util.ArrayList.class.getMethod("add", Object.class);
        addMethod.invoke(stringList, 123);  // 添加 Integer
        addMethod.invoke(stringList, 456);

        System.out.println("【绕过泛型检查】");
        System.out.println("  List<String> 中的元素:");
        for (Object obj : stringList) {
            System.out.println("    " + obj + " (" + obj.getClass().getSimpleName() + ")");
        }

        // 获取泛型信息
        System.out.println("\n【获取泛型参数类型】");
        Field field = GenericClass.class.getDeclaredField("list");
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            System.out.println("  原始类型: " + paramType.getRawType());
            Type[] typeArgs = paramType.getActualTypeArguments();
            System.out.println("  泛型参数: " + Arrays.toString(typeArgs));
        }
        System.out.println();
    }

    // ==================== 高频面试题演示 ====================

    /**
     * 14. 高频面试题 - Class.forName 与 ClassLoader.loadClass 的区别
     */
    public void interviewQuestion1() throws Exception {
        System.out.println("==================== 高频面试题 ====================");
        System.out.println();
        System.out.println("【面试题 1】Class.forName() 与 ClassLoader.loadClass() 的区别？\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Class.forName(className)                              │");
        System.out.println("│  - 默认会初始化类（执行 static 块）                        │");
        System.out.println("│  - 使用当前类的 ClassLoader                            │");
        System.out.println("│  - 常用于加载 JDBC 驱动（驱动需要初始化注册）             │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  Class.forName(className, false, classLoader)          │");
        System.out.println("│  - 不会初始化类（第二个参数 = false）                  │");
        System.out.println("│  - 可以指定 ClassLoader                                 │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  ClassLoader.loadClass(className)                      │");
        System.out.println("│  - 不会初始化类（只进行加载和链接）                    │");
        System.out.println("│  - 常用于延迟加载场景                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        // 演示
        System.out.println("【代码演示】");
        System.out.println("  // JDBC 驱动加载 - 需要初始化（执行 static 块注册驱动）");
        System.out.println("  Class.forName(\"com.mysql.cj.jdbc.Driver\");");
        System.out.println();
        System.out.println("  // Spring 延迟加载 - 不需要立即初始化");
        System.out.println("  classLoader.loadClass(\"com.example.MyBean\");\n");
    }

    /**
     * 15. 高频面试题 - getMethods 与 getDeclaredMethods 的区别
     */
    public void interviewQuestion2() {
        System.out.println("【面试题 2】getMethods() 与 getDeclaredMethods() 的区别？\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  方法                    范围            包含父类      │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  getMethods()           public          ✔             │");
        System.out.println("│  getDeclaredMethods()   所有修饰符        ✖             │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【代码演示】");
        Class<?> clazz = ChildPerson.class;

        Method[] publicMethods = clazz.getMethods();
        System.out.println("  getMethods() 返回 " + publicMethods.length + " 个方法");
        System.out.println("    包含：本类 public + 父类 public + Object public\n");

        Method[] declaredMethods = clazz.getDeclaredMethods();
        System.out.println("  getDeclaredMethods() 返回 " + declaredMethods.length + " 个方法");
        System.out.println("    仅包含本类声明的所有方法（包括 private）\n");

        System.out.println("【同样适用于】");
        System.out.println("  - getFields() vs getDeclaredFields()");
        System.out.println("  - getConstructors() vs getDeclaredConstructors()\n");
    }

    /**
     * 16. 高频面试题 - 反射的优缺点
     */
    public void interviewQuestion3() {
        System.out.println("【面试题 3】反射的优缺点？\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                         优点                              │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  1. 灵活性高：运行时动态加载类、创建对象、调用方法          │");
        System.out.println("│  2. 解耦：不需要编译时依赖具体类                            │");
        System.out.println("│  3. 框架基础：Spring IOC、MyBatis ORM 的核心实现          │");
        System.out.println("│  4. 动态代理：AOP、RPC 的基础                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                         缺点                              │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  1. 性能开销：比直接调用慢 10-20 倍                        │");
        System.out.println("│  2. 安全性问题：可访问私有成员，破坏封装性                  │");
        System.out.println("│  3. 类型安全：绕过编译检查，运行时才发现错误              │");
        System.out.println("│  4. 代码可读性差：代码冗长，难以维护                        │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 17. 高频面试题 - 反射能调用私有方法吗？
     */
    public void interviewQuestion4() throws Exception {
        System.out.println("【面试题 4】反射能调用私有方法吗？怎么调用？\n");

        System.out.println("【答案】可以，通过 setAccessible(true) 关闭安全检查\n");

        System.out.println("【代码演示】");
        Person person = new Person("张三", 25);

        // 获取私有方法
        Method privateMethod = Person.class.getDeclaredMethod("privateMethod");

        // 关闭安全检查
        privateMethod.setAccessible(true);

        // 调用私有方法
        privateMethod.invoke(person);

        System.out.println("\n【注意事项】");
        System.out.println("  1. 必须使用 getDeclaredMethod()，不是 getMethod()");
        System.out.println("  2. 必须调用 setAccessible(true)");
        System.out.println("  3. Java 9+ 模块系统可能限制访问\n");
    }

    /**
     * 18. 高频面试题 - 反射如何提高性能？
     */
    public void interviewQuestion5() throws Exception {
        System.out.println("【面试题 5】反射如何提高性能？\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                    反射性能优化方案                        │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  1. setAccessible(true) - 关闭安全检查，提升 2-4 倍   │");
        System.out.println("│  2. 缓存 Method/Field 对象 - 避免重复获取             │");
        System.out.println("│  3. 使用 MethodHandle (Java 7+) - 接近直接调用       │");
        System.out.println("│  4. 使用 LambdaMetafactory (Java 8+) - 最快          │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        // 缓存演示
        System.out.println("【缓存示例】");
        System.out.println("  // 不推荐：每次都获取 Method");
        System.out.println("  for (int i = 0; i < 1000; i++) {");
        System.out.println("      Method m = clazz.getMethod(\"getName\"); // 每次都新建");
        System.out.println("      m.invoke(obj);");
        System.out.println("  }\n");

        System.out.println("  // 推荐：缓存 Method");
        System.out.println("  Method m = clazz.getMethod(\"getName\"); // 只获取一次");
        System.out.println("  m.setAccessible(true);");
        System.out.println("  for (int i = 0; i < 1000; i++) {");
        System.out.println("      m.invoke(obj); // 复用缓存的 Method");
        System.out.println("  }\n");
    }

    /**
     * 19. 高频面试题 - 反射在 Spring 中的应用
     */
    public void interviewQuestion6() {
        System.out.println("【面试题 6】反射在 Spring 中有哪些应用？\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. IOC 容器 - Bean 的创建和依赖注入                      │");
        System.out.println("│     - 通过反射创建 Bean 实例                              │");
        System.out.println("│     - 通过反射注入属性（@Autowired）                     │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  2. AOP - 动态代理实现                                   │");
        System.out.println("│     - JDK 动态代理（接口）                               │");
        System.out.println("│     - CGLIB 动态代理（类）                              │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  3. 注解处理 - 解析各种注解                              │");
        System.out.println("│     - @RequestMapping 路由映射                         │");
        System.out.println("│     - @Value 配置注入                                  │");
        System.out.println("│     - @Transactional 事务管理                          │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  4. 数据绑定 - 请求参数绑定到对象                        │");
        System.out.println("│     - @RequestBody JSON 转对象                         │");
        System.out.println("│     - @PathVariable 路径参数                            │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        // Spring IOC 简化演示
        System.out.println("【Spring IOC 简化实现】");
        simulateSpringIOC();
    }

    /**
     * 模拟 Spring IOC 容器
     */
    private void simulateSpringIOC() {
        try {
            // 1. 获取类
            Class<?> clazz = Person.class;

            // 2. 创建实例（相当于 Bean 实例化）
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            Object bean = constructor.newInstance();

            // 3. 属性注入（相当于 @Autowired）
            Field nameField = clazz.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(bean, "通过反射注入的名称");

            Field ageField = clazz.getDeclaredField("age");
            ageField.setAccessible(true);
            ageField.set(bean, 18);

            System.out.println("  创建并注入后的 Bean: " + bean + "\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 20. 高频面试题 - 反射能破坏单例模式吗？如何防御？
     */
    public void interviewQuestion7() throws Exception {
        System.out.println("【面试题 7】反射能破坏单例模式吗？如何防御？\n");

        System.out.println("【答案】可以破坏，但有防御方案\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                    防御方案                                │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  1. 枚举单例（推荐）- JVM 保证唯一，反射无法创建          │");
        System.out.println("│  2. 构造器检查 - 第二次调用时抛异常                      │");
        System.out.println("│  3. 使用 SecurityManager - 限制反射访问               │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        // 枚举单例演示
        System.out.println("【枚举单例 - 最佳方案】");
        System.out.println("  public enum EnumSingleton {");
        System.out.println("      INSTANCE;");
        System.out.println("      public void doSomething() { ... }");
        System.out.println("  }");
        System.out.println("  // 使用: EnumSingleton.INSTANCE.doSomething();\n");

        // 尝试反射创建枚举
        System.out.println("【尝试反射破坏枚举单例】");
        try {
            Constructor<?>[] constructors = EnumSingleton.class.getDeclaredConstructors();
            for (Constructor<?> c : constructors) {
                c.setAccessible(true);
                c.newInstance();  // 会抛异常
            }
        } catch (Exception e) {
            System.out.println("  异常: " + e.getCause().getClass().getSimpleName());
            System.out.println("  原因: JVM 禁止通过反射创建枚举实例\n");
        }

        // 构造器检查防御
        System.out.println("【构造器检查防御】");
        System.out.println("  private static boolean created = false;");
        System.out.println("  private Singleton() {");
        System.out.println("      if (created) {");
        System.out.println("          throw new RuntimeException(\"已存在实例，禁止创建\");");
        System.out.println("      }");
        System.out.println("      created = true;");
        System.out.println("  }\n");
    }

    // ==================== 测试用的类和接口 ====================

    @MyAnnotation(value = "类注解", description = "这是一个测试类")
    static class Person {
        private String name;
        private int age;
        private static String staticField = "原始静态值";

        public Person() {
        }

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        private Person(String name) {
            this.name = name;
            this.age = 0;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        private void privateMethod() {
            System.out.println("  私有方法被调用");
        }

        public static void staticMethod() {
            System.out.println("  静态方法被调用");
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + "}";
        }
    }

    // 子类，用于测试 getMethods vs getDeclaredMethods
    static class ChildPerson extends Person {
        private void childPrivateMethod() {}
        public void childPublicMethod() {}
    }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE,
            java.lang.annotation.ElementType.METHOD,
            java.lang.annotation.ElementType.FIELD})
    @interface MyAnnotation {
        String value();
        String description() default "";
    }

    @MyAnnotation(value = "AnnotatedClass", description = "带注解的类")
    static class AnnotatedClass {
        @MyAnnotation(value = "annotatedField")
        private String annotatedField;

        @MyAnnotation(value = "annotatedMethod")
        public void annotatedMethod() {
        }
    }

    interface Subject {
        void request();
    }

    static class RealSubject implements Subject {
        @Override
        public void request() {
            System.out.println("  真实主题执行请求");
        }
    }

    static class Singleton {
        private static final Singleton INSTANCE = new Singleton();

        private Singleton() {
        }

        public static Singleton getInstance() {
            return INSTANCE;
        }
    }

    // 枚举单例 - 反射无法破坏
    enum EnumSingleton {
        INSTANCE;
        public void doSomething() {
            System.out.println("枚举单例方法");
        }
    }

    // Final 字段测试类
    static class FinalFieldDemo {
        private final String finalField = "原始值";
        public String getFinalField() {
            return finalField;
        }
    }

    // 泛型测试类
    static class GenericClass {
        private java.util.List<String> list;
    }

    public static void main(String[] args) throws Throwable {
        ReflectionDeepDiveDemo demo = new ReflectionDeepDiveDemo();
        
        System.out.println("\n");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println("█                                                                   █");
        System.out.println("█                  Java 反射机制深入讲解                           █");
        System.out.println("█                                                                   █");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println();
        
        // 基础部分
        demo.getClassObject();
        demo.classMethods();
        demo.constructorReflection();
        demo.methodReflection();
        demo.fieldReflection();
        demo.annotationReflection();
        demo.dynamicProxy();
        demo.performanceTest();
        demo.breakSingleton();
        
        // 进阶部分
        demo.methodHandleDemo();
        demo.modifyFinalField();
        demo.arrayReflection();
        demo.genericReflection();
        
        // 高频面试题
        demo.interviewQuestion1();
        demo.interviewQuestion2();
        demo.interviewQuestion3();
        demo.interviewQuestion4();
        demo.interviewQuestion5();
        demo.interviewQuestion6();
        demo.interviewQuestion7();
        
        System.out.println("\n");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println("█                        全部演示完成                             █");
        System.out.println("███████████████████████████████████████████████████████████████████");
    }
}
