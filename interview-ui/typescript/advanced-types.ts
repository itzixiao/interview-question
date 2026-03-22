/**
 * TypeScript 高级类型详解
 *
 * 核心概念：
 * 1. 泛型（Generics）- 类型参数化
 * 2. 条件类型（Conditional Types）- 类型选择
 * 3. 映射类型（Mapped Types）- 批量转换
 * 4. 模板字面量类型（Template Literal Types）- 字符串操作
 * 5. 类型推断与类型守卫
 *
 * 对应文档：docs/18-前端开发/04-TypeScript高级类型与实战.md
 */

// ==================== 1. 泛型（Generics）====================

/**
 * 泛型基础
 *
 * 泛型允许我们在定义函数、接口或类时不指定具体类型，
 * 而是在使用时再指定，从而实现代码复用。
 */

/**
 * 泛型函数示例
 * 创建一个返回数组最后一项的函数
 * @template T 数组元素类型
 * @param arr 输入数组
 * @returns 数组最后一项
 */
function last<T>(arr: T[]): T | undefined {
    return arr[arr.length - 1];
}

// 使用示例
const lastNumber = last([1, 2, 3]);        // 类型推断为 number
const lastString = last(['a', 'b', 'c']);  // 类型推断为 string

/**
 * 泛型接口示例
 * 定义一个通用的响应结构
 */
interface ApiResponse<T> {
    code: number;
    message: string;
    data: T;
    timestamp: number;
}

// 使用泛型接口
type UserResponse = ApiResponse<{ id: number; name: string }>;
type ListResponse<T> = ApiResponse<{ list: T[]; total: number }>;

/**
 * 泛型约束
 * 限制泛型必须满足某些条件
 */
interface HasLength {
    length: number;
}

// T 必须具有 length 属性
function logLength<T extends HasLength>(arg: T): T {
    console.log(arg.length);
    return arg;
}

logLength('hello');     // ✓ 字符串有 length
logLength([1, 2, 3]);   // ✓ 数组有 length
// logLength(123);      // ✗ 数字没有 length

/**
 * 多个泛型参数
 */
function map<T, U>(array: T[], fn: (item: T) => U): U[] {
    return array.map(fn);
}

const numbers = [1, 2, 3];
const strings = map(numbers, n => n.toString());  // string[]

// ==================== 2. 条件类型（Conditional Types）====================

/**
 * 条件类型基础语法
 * T extends U ? X : Y
 * 如果 T 可以赋值给 U，则类型为 X，否则为 Y
 */

type IsString<T> = T extends string ? true : false;

type A = IsString<string>;   // true
type B = IsString<number>;   // false

/**
 * 分布式条件类型
 * 当条件类型作用于联合类型时，会分布式应用
 */
type ToArray<T> = T extends any ? T[] : never;

type StringOrNumberArray = ToArray<string | number>;
// 结果：string[] | number[]（不是 (string | number)[]）

/**
 * infer 关键字
 * 在条件类型中推断类型
 */

// 提取函数返回类型
type ReturnType<T> = T extends (...args: any[]) => infer R ? R : never;

function getUser() {
    return {id: 1, name: '张三'};
}

type UserType = ReturnType<typeof getUser>;  // { id: number; name: string }

// 提取 Promise 的返回值类型
type Awaited<T> = T extends Promise<infer R> ? R : T;

type PromiseResult = Awaited<Promise<string>>;  // string

// 提取数组元素类型
type ElementType<T> = T extends (infer E)[] ? E : never;

type Num = ElementType<number[]>;  // number

// ==================== 3. 映射类型（Mapped Types）====================

/**
 * 映射类型基础
 * 基于旧类型创建新类型，通过映射每个属性
 */

// 将所有属性变为可选
type Partial<T> = {
    [P in keyof T]?: T[P];
};

// 将所有属性变为必需
type Required<T> = {
    [P in keyof T]-?: T[P];
};

// 将所有属性变为只读
type Readonly<T> = {
    readonly [P in keyof T]: T[P];
};

// 使用示例
interface Person {
    name: string;
    age: number;
    email?: string;
}

type PartialPerson = Partial<Person>;
// { name?: string; age?: number; email?: string }

type RequiredPerson = Required<Person>;
// { name: string; age: number; email: string }

/**
 * 自定义映射类型
 */

// 将所有属性变为可空
type Nullable<T> = {
    [P in keyof T]: T[P] | null;
};

// 提取特定类型的属性
type PickByType<T, U> = {
    [P in keyof T as T[P] extends U ? P : never]: T[P];
};

interface Mixed {
    name: string;
    age: number;
    isActive: boolean;
    count: number;
}

type StringProps = PickByType<Mixed, string>;   // { name: string }
type NumberProps = PickByType<Mixed, number>;   // { age: number; count: number }

/**
 * 重映射键名（TypeScript 4.1+）
 */
type Getters<T> = {
    [P in keyof T as `get${Capitalize<string & P>}`]: () => T[P];
};

type PersonGetters = Getters<Person>;
// { getName: () => string; getAge: () => number; getEmail: () => string | undefined }

// ==================== 4. 模板字面量类型（Template Literal Types）====================

/**
 * 模板字面量类型
 * 基于字符串字面量构建新类型
 */

type EventName<T extends string> = `on${Capitalize<T>}`;

type ClickEvent = EventName<'click'>;      // 'onClick'
type HoverEvent = EventName<'hover'>;      // 'onHover'

/**
 * 实际应用：事件处理器类型
 */
type Events = 'click' | 'hover' | 'focus' | 'blur';
type EventHandlers = {
    [K in Events as `on${Capitalize<K>}`]: (event: Event) => void;
};
// 结果：
// {
//     onClick: (event: Event) => void;
//     onHover: (event: Event) => void;
//     onFocus: (event: Event) => void;
//     onBlur: (event: Event) => void;
// }

/**
 * 字符串操作类型
 */

// 字符串拼接
type Concat<A extends string, B extends string> = `${A}${B}`;
type HelloWorld = Concat<'Hello, ', 'World'>;  // 'Hello, World'

// 提取路径参数
type ExtractParams<T extends string> =
    T extends `${infer Start}:${infer Param}/${infer Rest}`
        ? { [K in Param]: string } & ExtractParams<`/${Rest}`>
        : T extends `${infer Start}:${infer Param}`
            ? { [K in Param]: string }
            : {};

type UserParams = ExtractParams<'/users/:id/posts/:postId'>;
// { id: string } & { postId: string }

// ==================== 5. 类型守卫（Type Guards）====================

/**
 * 类型守卫函数
 * 用于在运行时收窄类型范围
 */

// typeof 类型守卫
function processValue(value: string | number) {
    if (typeof value === 'string') {
        // TypeScript 知道这里是 string
        return value.toUpperCase();
    } else {
        // TypeScript 知道这里是 number
        return value.toFixed(2);
    }
}

// instanceof 类型守卫
class Dog {
    bark() {
        console.log('Woof!');
    }
}

class Cat {
    meow() {
        console.log('Meow!');
    }
}

function makeSound(animal: Dog | Cat) {
    if (animal instanceof Dog) {
        animal.bark();  // TypeScript 知道是 Dog
    } else {
        animal.meow();  // TypeScript 知道是 Cat
    }
}

// 自定义类型守卫函数
interface Fish {
    swim: () => void;
}

interface Bird {
    fly: () => void;
}

// 返回值类型中的 "pet is Fish" 就是类型谓词
function isFish(pet: Fish | Bird): pet is Fish {
    return (pet as Fish).swim !== undefined;
}

function move(pet: Fish | Bird) {
    if (isFish(pet)) {
        pet.swim();  // TypeScript 知道是 Fish
    } else {
        pet.fly();   // TypeScript 知道是 Bird
    }
}

// in 操作符类型守卫
interface Admin {
    role: 'admin';
    permissions: string[];
}

interface User {
    role: 'user';
    subscription: string;
}

function checkAccess(person: Admin | User) {
    if ('permissions' in person) {
        // TypeScript 知道是 Admin
        console.log('权限：', person.permissions);
    } else {
        // TypeScript 知道是 User
        console.log('订阅：', person.subscription);
    }
}

// ==================== 6. 实用工具类型 ====================

/**
 * 常用的自定义工具类型
 */

// 深度只读
type DeepReadonly<T> = {
    readonly [P in keyof T]: T[P] extends object ? DeepReadonly<T[P]> : T[P];
};

// 深度可选
type DeepPartial<T> = {
    [P in keyof T]?: T[P] extends object ? DeepPartial<T[P]> : T[P];
};

// 提取对象中的函数类型属性
type FunctionProperties<T> = {
    [P in keyof T as T[P] extends Function ? P : never]: T[P];
};

// 提取对象中的非函数类型属性
type NonFunctionProperties<T> = {
    [P in keyof T as T[P] extends Function ? never : P]: T[P];
};

// 扁平化对象类型（将嵌套对象展开）
type Flatten<T> = {
    [K in keyof T]: T[K];
};

// 可选链式访问类型
type DeepNullable<T> = {
    [P in keyof T]?: DeepNullable<T[P]> | null;
};

// ==================== 7. 实战示例 ====================

/**
 * 示例1：API 响应类型封装
 */
interface ApiError {
    code: string;
    message: string;
}

type ApiResult<T> =
    | { success: true; data: T }
    | { success: false; error: ApiError };

async function fetchUser(id: number): Promise<ApiResult<User>> {
    try {
        const response = await fetch(`/api/users/${id}`);
        if (!response.ok) {
            return {
                success: false,
                error: {code: 'NOT_FOUND', message: '用户不存在'}
            };
        }
        const data = await response.json();
        return {success: true, data};
    } catch (error) {
        return {
            success: false,
            error: {code: 'NETWORK_ERROR', message: '网络错误'}
        };
    }
}

/**
 * 示例2：表单验证类型
 */
type ValidationRule<T> = {
    required?: boolean;
    minLength?: number;
    maxLength?: number;
    pattern?: RegExp;
    validator?: (value: T) => string | undefined;
};

type FormSchema<T> = {
    [K in keyof T]: ValidationRule<T[K]>;
};

interface LoginForm {
    username: string;
    password: string;
    remember: boolean;
}

const loginSchema: FormSchema<LoginForm> = {
    username: {
        required: true,
        minLength: 3,
        maxLength: 20
    },
    password: {
        required: true,
        minLength: 6,
        validator: (value) => {
            if (!/[A-Z]/.test(value)) {
                return '密码必须包含大写字母';
            }
        }
    },
    remember: {}
};

/**
 * 示例3：Redux Action 类型
 */
type Action<T extends string, P = void> = P extends void
    ? { type: T }
    : { type: T; payload: P };

// 定义具体的 Action 类型
type IncrementAction = Action<'INCREMENT'>;
type SetCountAction = Action<'SET_COUNT', number>;
type SetUserAction = Action<'SET_USER', { id: number; name: string }>;

// 联合类型
type CounterAction = IncrementAction | SetCountAction;

// 导出所有类型
export type {
    ApiResponse,
    ApiResult,
    Person,
    EventHandlers,
    ValidationRule,
    FormSchema,
    LoginForm
};
