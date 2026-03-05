package cn.itzixiao.interview.java;

import java.lang.reflect.*;
import java.util.Arrays;

/**
 * Java 反射机制深入理解
 *
 * 反射原理：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 类加载时，JVM 创建 Class 对象（包含类的完整元数据）        │
 * │  2. 通过 Class 对象可以获取：                                 │
 * │     - 构造器（Constructor）                                  │
 * │     - 方法（Method）                                         │
 * │     - 字段（Field）                                          │
 * │     - 注解（Annotation）                                     │
 * │  3. 可以动态创建对象、调用方法、修改字段                      │
 * └─────────────────────────────────────────────────────────────┘
 *
 * 反射性能问题：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 类型检查：绕过编译时类型检查，运行时检查                  │
 * │  2. 安全性检查：每次反射调用都有安全检查开销                  │
 * │  3. 无法内联：JIT 编译器无法对反射调用进行优化                │
 * │  优化：setAccessible(true) 关闭安全检查                      │
 * └─────────────────────────────────────────────────────────────┘
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

    public static void main(String[] args) throws Exception {
        ReflectionDeepDiveDemo demo = new ReflectionDeepDiveDemo();
        demo.getClassObject();
        demo.classMethods();
        demo.constructorReflection();
        demo.methodReflection();
        demo.fieldReflection();
        demo.annotationReflection();
        demo.dynamicProxy();
        demo.performanceTest();
        demo.breakSingleton();
    }
}
