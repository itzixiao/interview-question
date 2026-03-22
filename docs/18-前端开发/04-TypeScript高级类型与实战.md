# TypeScript 高级类型与实战

## 一、TypeScript vs Java 类型系统对比

| 特性         | Java        | TypeScript            |
|------------|-------------|-----------------------|
| **类型检查时机** | 编译期         | 编译期（转译时）              |
| **类型擦除**   | 是（运行时无泛型信息） | 是（编译为 JS 后无类型）        |
| **泛型约束**   | `extends`   | `extends`             |
| **联合类型**   | 不支持（需用继承）   | `A \| B` 原生支持         |
| **交叉类型**   | 不支持         | `A & B` 原生支持          |
| **类型推断**   | 有限（钻石操作符）   | 强大（上下文推断）             |
| **条件类型**   | 不支持         | `T extends U ? X : Y` |
| **映射类型**   | 不支持         | `{ [K in T]: V }`     |

## 二、高级类型体操

### 2.1 条件类型

```typescript
// 基础条件类型
type IsString<T> = T extends string ? true : false;

type A = IsString<'hello'>;  // true
type B = IsString<123>;      // false

// 分布式条件类型（对联合类型分发）
type ToArray<T> = T extends any ? T[] : never;
type StrOrNumArray = ToArray<string | number>;
// string[] | number[]，而不是 (string | number)[]

// 阻止分布式（使用元组包裹）
type ToArrayNonDist<T> = [T] extends [any] ? T[] : never;
type MixedArray = ToArrayNonDist<string | number>;
// (string | number)[]

// infer 推断类型
type ReturnType<T> = T extends (...args: any[]) => infer R ? R : never;
type FnReturn = ReturnType<() => string>; // string

// 提取数组元素类型
type ElementType<T> = T extends (infer E)[] ? E : never;
type Num = ElementType<number[]>; // number

// 提取 Promise 返回值
type Awaited<T> = T extends Promise<infer R> ? R : T;
type AsyncResult = Awaited<Promise<string>>; // string
```

### 2.2 映射类型

```typescript
// 基础映射类型
type Readonly<T> = {
    readonly [P in keyof T]: T[P];
};

type Partial<T> = {
    [P in keyof T]?: T[P];
};

type Required<T> = {
    [P in keyof T]-?: T[P]; // -? 移除可选
};

type Pick<T, K extends keyof T> = {
    [P in K]: T[P];
};

type Omit<T, K extends keyof any> = Pick<T, Exclude<keyof T, K>>;

type Record<K extends keyof any, T> = {
    [P in K]: T;
};

// 使用示例
interface User {
    id: number;
    name: string;
    email: string;
    age: number;
}

type UserPreview = Pick<User, 'id' | 'name'>;
// { id: number; name: string; }

type UserUpdate = Partial<Omit<User, 'id'>>;
// { name?: string; email?: string; age?: number; }

type UserDictionary = Record<string, User>;

// 重映射（TS 4.1+）
type Getters<T> = {
    [K in keyof T as `get${Capitalize<string & K>}`]: () => T[K];
};

type UserGetters = Getters<User>;
// {
//     getId: () => number;
//     getName: () => string;
//     getEmail: () => string;
//     getAge: () => number;
// }

// 过滤属性
type RemoveKindField<T> = {
    [K in keyof T as Exclude<K, 'kind'>]: T[K];
};

interface Circle {
    kind: 'circle';
    radius: number;
}

type ShapeInfo = RemoveKindField<Circle>; // { radius: number }
```

### 2.3 模板字面量类型

```typescript
// 基础模板字面量
type EventName<T extends string> = `on${Capitalize<T>}`;
type ClickEvent = EventName<'click'>; // 'onClick'

// 联合类型组合
type Vertical = 'top' | 'bottom';
type Horizontal = 'left' | 'right';
type Position = `${Vertical}-${Horizontal}`;
// 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right'

// 结合映射类型
type CSSVariable<T extends string> = `--${T}`;
type ThemeKeys = 'primary' | 'secondary' | 'danger';
type CSSVars = CSSVariable<ThemeKeys>;
// '--primary' | '--secondary' | '--danger'

// 实际应用：API 路由类型
type APIEndpoints = {
    users: {
        'GET /users': { response: User[] };
        'GET /users/:id': { params: { id: string }; response: User };
        'POST /users': { body: CreateUserDTO; response: User };
    };
    posts: {
        'GET /posts': { response: Post[] };
        'GET /posts/:id': { params: { id: string }; response: Post };
    };
};

// 提取 HTTP 方法
type ExtractMethod<T extends string> = 
    T extends `${infer Method} ${infer _}` ? Method : never;

type UserMethods = ExtractMethod<keyof APIEndpoints['users']>;
// 'GET' | 'POST'
```

### 2.4 递归类型

```typescript
// 深度只读
type DeepReadonly<T> = {
    readonly [P in keyof T]: T[P] extends object 
        ? DeepReadonly<T[P]> 
        : T[P];
};

interface Nested {
    a: {
        b: {
            c: string;
        };
    };
}

type DeepNested = DeepReadonly<Nested>;
// 所有层级都变为 readonly

// 深度 Partial
type DeepPartial<T> = {
    [P in keyof T]?: T[P] extends object 
        ? DeepPartial<T[P]> 
        : T[P];
};

// 扁平化类型
type Flatten<T> = T extends (infer U)[] ? U : T;

type NestedArray = number[][];
type FlatArray = Flatten<NestedArray>; // number[]

// 路径类型（用于 lodash get）
type Path<T, K extends keyof T = keyof T> = 
    K extends string 
        ? T[K] extends object 
            ? `${K}` | `${K}.${Path<T[K]>}` 
            : `${K}`
        : never;

interface Data {
    user: {
        name: string;
        address: {
            city: string;
        };
    };
}

type DataPath = Path<Data>;
// 'user' | 'user.name' | 'user.address' | 'user.address.city'
```

## 三、类型守卫与 narrowing

```typescript
// typeof 类型守卫
function processValue(value: string | number) {
    if (typeof value === 'string') {
        // value 被收窄为 string
        return value.toUpperCase();
    }
    // value 被收窄为 number
    return value.toFixed(2);
}

// instanceof 类型守卫
class Dog {
    bark() { return 'Woof!'; }
}

class Cat {
    meow() { return 'Meow!'; }
}

function makeSound(animal: Dog | Cat) {
    if (animal instanceof Dog) {
        return animal.bark();
    }
    return animal.meow();
}

// in 操作符类型守卫
interface Car {
    drive(): void;
}

interface Boat {
    sail(): void;
}

function move(vehicle: Car | Boat) {
    if ('drive' in vehicle) {
        vehicle.drive();
    } else {
        vehicle.sail();
    }
}

// 自定义类型守卫（谓词函数）
interface Fish {
    swim(): void;
    kind: 'fish';
}

interface Bird {
    fly(): void;
    kind: 'bird';
}

function isFish(animal: Fish | Bird): animal is Fish {
    return (animal as Fish).swim !== undefined;
}

function interact(animal: Fish | Bird) {
    if (isFish(animal)) {
        animal.swim(); // TypeScript 知道这是 Fish
    } else {
        animal.fly();  // TypeScript 知道这是 Bird
    }
}

// 判别联合类型（Discriminated Unions）
interface Square {
    kind: 'square';
    size: number;
}

interface Rectangle {
    kind: 'rectangle';
    width: number;
    height: number;
}

interface Circle {
    kind: 'circle';
    radius: number;
}

type Shape = Square | Rectangle | Circle;

function getArea(shape: Shape): number {
    switch (shape.kind) {
        case 'square':
            return shape.size ** 2;
        case 'rectangle':
            return shape.width * shape.height;
        case 'circle':
            return Math.PI * shape.radius ** 2;
        default:
            // 穷尽检查
            const _exhaustiveCheck: never = shape;
            return _exhaustiveCheck;
    }
}
```

## 四、泛型实战模式

```typescript
// 1. 工厂模式
type Constructor<T> = new (...args: any[]) => T;

function createInstance<T>(Constructor: Constructor<T>, ...args: any[]): T {
    return new Constructor(...args);
}

class User {
    constructor(public name: string) {}
}

const user = createInstance(User, 'John');

// 2. 混入模式（Mixin）
type GConstructor<T = {}> = new (...args: any[]) => T;

function Timestamped<TBase extends GConstructor>(Base: TBase) {
    return class extends Base {
        timestamp = Date.now();
        getTimestamp() {
            return this.timestamp;
        }
    };
}

function Activatable<TBase extends GConstructor>(Base: TBase) {
    return class extends Base {
        isActive = false;
        activate() {
            this.isActive = true;
        }
        deactivate() {
            this.isActive = false;
        }
    };
}

const TimestampedUser = Timestamped(User);
const ActivatableTimestampedUser = Activatable(TimestampedUser);

const mixedUser = new ActivatableTimestampedUser('Jane');
mixedUser.activate();
console.log(mixedUser.getTimestamp());

// 3. 类型安全的 EventEmitter
interface EventMap {
    'user:login': { userId: string; timestamp: number };
    'user:logout': { userId: string };
    'error': Error;
}

class TypedEventEmitter<Events extends Record<string, any>> {
    private listeners: {
        [K in keyof Events]?: Array<(data: Events[K]) => void>;
    } = {};

    on<K extends keyof Events>(
        event: K, 
        listener: (data: Events[K]) => void
    ): void {
        if (!this.listeners[event]) {
            this.listeners[event] = [];
        }
        this.listeners[event]!.push(listener);
    }

    emit<K extends keyof Events>(event: K, data: Events[K]): void {
        this.listeners[event]?.forEach(listener => listener(data));
    }

    off<K extends keyof Events>(
        event: K, 
        listener: (data: Events[K]) => void
    ): void {
        if (this.listeners[event]) {
            this.listeners[event] = this.listeners[event]!.filter(
                l => l !== listener
            );
        }
    }
}

const emitter = new TypedEventEmitter<EventMap>();
emitter.on('user:login', ({ userId, timestamp }) => {
    console.log(`User ${userId} logged in at ${timestamp}`);
});
emitter.emit('user:login', { userId: '123', timestamp: Date.now() });

// 4. 类型安全的 API 客户端
type APIResponse<T> = {
    data: T;
    status: number;
    message: string;
};

interface APIEndpoints {
    '/users': {
        GET: { response: User[] };
        POST: { body: CreateUserDTO; response: User };
    };
    '/users/:id': {
        GET: { params: { id: string }; response: User };
        PUT: { params: { id: string }; body: UpdateUserDTO; response: User };
        DELETE: { params: { id: string }; response: void };
    };
}

type ExtractParams<T extends string> = 
    T extends `${infer _}/:${infer Param}/${infer Rest}`
        ? { [K in Param | keyof ExtractParams<`/${Rest}`>]: string }
        : T extends `${infer _}/:${infer Param}`
            ? { [K in Param]: string }
            : {};

class APIClient {
    async request<
        Path extends keyof APIEndpoints,
        Method extends keyof APIEndpoints[Path]
    >(
        path: Path,
        method: Method,
        ...args: APIEndpoints[Path][Method] extends { params: infer P; body: infer B }
            ? [params: P, body: B]
            : APIEndpoints[Path][Method] extends { params: infer P }
                ? [params: P]
                : APIEndpoints[Path][Method] extends { body: infer B }
                    ? [body: B]
                    : []
    ): Promise<APIResponse<APIEndpoints[Path][Method] extends { response: infer R } ? R : void>> {
        // 实现...
        return fetch(path as string).then(r => r.json());
    }
}

const client = new APIClient();
// 类型安全的 API 调用
const users = await client.request('/users', 'GET');
const user = await client.request('/users/:id', 'GET', { id: '123' });
```

## 五、与 Vue/React 结合

```typescript
// ===== Vue 3 + TypeScript =====
import {ref, computed, defineComponent, PropType} from 'vue';

// 定义 Props 类型
interface UserProps {
    id: string;
    name: string;
    email: string;
    role: 'admin' | 'user' | 'guest';
}

// 使用 defineComponent 获得类型推断
export default defineComponent({
    props: {
        user: {
            type: Object as PropType<UserProps>,
            required: true
        },
        editable: {
            type: Boolean,
            default: false
        }
    },
    setup(props) {
        // props 类型推断
        const userName = computed(() => props.user.name.toUpperCase());

        // ref 类型推断
        const count = ref<number>(0);
        const userList = ref<UserProps[]>([]);

        // 方法
        const updateUser = (newUser: Partial<UserProps>) => {
            // ...
        };

        return {userName, count, userList, updateUser};
    }
});

// 更好的方式：Script Setup
defineProps<{
    user: UserProps;
    editable?: boolean;
}>();

const emit = defineEmits<{
    update: [user: UserProps];
    delete: [id: string];
}>();

// ===== React + TypeScript =====
import React, {useState, useCallback, useEffect} from 'react';

// 组件 Props
interface ButtonProps {
    variant?: 'primary' | 'secondary' | 'danger';
    size?: 'sm' | 'md' | 'lg';
    disabled?: boolean;
    onClick?: (event: React.MouseEvent<HTMLButtonElement>) => void;
    children: React.ReactNode;
}

// FC 方式（不推荐，有隐式 children）
const Button: React.FC<ButtonProps> = ({
                                           variant = 'primary',
                                           size = 'md',
                                           children,
                                           ...props
                                       }) => {
    return (
        <button
            className = {`btn btn-${variant} btn-${size}`
}
    {...
        props
    }
>
    {
        children
    }
    </button>
)
    ;
};

// 推荐方式：明确返回类型
function Button2({
                     variant = 'primary',
                     size = 'md',
                     children,
                     ...props
                 }: ButtonProps): JSX.Element {
    return (
        <button
            className = {`btn btn-${variant} btn-${size}`
}
    {...
        props
    }
>
    {
        children
    }
    </button>
)
    ;
}

// 泛型组件
interface ListProps<T> {
    items: T[];
    renderItem: (item: T, index: number) => React.ReactNode;
    keyExtractor: (item: T) => string | number;
}

function List<T>({items, renderItem, keyExtractor}: ListProps<T>): JSX.Element {
    return (
        <ul>
            {
                items.map((item, index) => (
                    <li key = {keyExtractor(item)} >
                        {renderItem(item, index)}
                        < /li>
                ))
            }
        < /ul>
    );
}

// 使用
interface User {
    id: string;
    name: string;
}

<List<User>
    items = {users}
renderItem = {(user)
=>
<span>{user.name} < /span>}
keyExtractor = {(user)
=>
user.id
}
/>

// 自定义 Hook 类型
function useLocalStorage<T>(key: string, initialValue: T): [T, (value: T | ((prev: T) => T)) => void] {
    const [storedValue, setStoredValue] = useState<T>(() => {
        try {
            const item = window.localStorage.getItem(key);
            return item ? JSON.parse(item) : initialValue;
        } catch {
            return initialValue;
        }
    });

    const setValue = useCallback((value: T | ((prev: T) => T)) => {
        try {
            const valueToStore = value instanceof Function ? value(storedValue) : value;
            setStoredValue(valueToStore);
            window.localStorage.setItem(key, JSON.stringify(valueToStore));
        } catch (error) {
            console.error(error);
        }
    }, [key, storedValue]);

    return [storedValue, setValue];
}

// 使用
const [name, setName] = useLocalStorage<string>('name', '');
```

---

## 六、高频面试题

**问题 1：TypeScript 中的 any、unknown、never 有什么区别？**

**答：**

| 类型          | 含义            | 使用场景          |
|-------------|---------------|---------------|
| **any**     | 任意类型，关闭类型检查   | 迁移旧代码、第三方库无类型 |
| **unknown** | 未知类型，安全版的 any | 需要类型守卫后使用     |
| **never**   | 永不存在的类型       | 穷尽检查、抛出错误的函数  |

```typescript
// any - 无类型检查
let a: any = 4;
a.toFixed(); // 编译通过，运行可能报错

// unknown - 需要类型检查
let b: unknown = 4;
b.toFixed(); // 编译错误
if (typeof b === 'number') {
    b.toFixed(); // OK
}

// never - 用于穷尽检查
type Shape = Circle | Square;
function getArea(shape: Shape) {
    switch (shape.kind) {
        case 'circle': return Math.PI * shape.radius ** 2;
        case 'square': return shape.size ** 2;
        default: 
            const _exhaustive: never = shape; // 如果新增类型会报错
            return _exhaustive;
    }
}
```

---

**问题 2：什么是协变、逆变、双变和抗变？**

**答：**

```typescript
// 协变（Covariance）：子类型可以赋值给父类型
interface Animal { name: string; }
interface Dog extends Animal { bark(): void; }

let animals: Animal[] = [];
let dogs: Dog[] = [{ name: 'Buddy', bark: () => {} }];

animals = dogs; // OK，数组是协变的

// 逆变（Contravariance）：函数参数是逆变的
type AnimalHandler = (animal: Animal) => void;
type DogHandler = (dog: Dog) => void;

let handleAnimal: AnimalHandler = (a) => console.log(a.name);
let handleDog: DogHandler = (d) => d.bark();

handleDog = handleAnimal; // OK，函数参数是逆变的
// handleAnimal = handleDog; // Error

// TypeScript 中的配置
// strictFunctionTypes: true 启用严格函数类型检查（函数参数逆变）
```

---

**问题 3：如何提取 Promise 的返回值类型？**

**答：**

```typescript
// 使用 infer
type Awaited<T> = T extends Promise<infer R> ? R : T;

// 递归处理嵌套 Promise
type DeepAwaited<T> = T extends Promise<infer R> 
    ? DeepAwaited<R> 
    : T;

type Result = DeepAwaited<Promise<Promise<string>>>; // string

// 实际应用
async function fetchUser(): Promise<{ id: string; name: string }> {
    return { id: '1', name: 'John' };
}

type FetchUserReturn = Awaited<ReturnType<typeof fetchUser>>;
// { id: string; name: string }
```

---

**问题 4：TypeScript 装饰器原理是什么？**

**答：**

装饰器是实验性特性，本质是高阶函数：

```typescript
// 类装饰器
function Component(options: { selector: string }) {
    return function<T extends { new (...args: any[]): {} }>(constructor: T) {
        return class extends constructor {
            selector = options.selector;
        };
    };
}

@Component({ selector: 'app-root' })
class AppComponent {}

// 方法装饰器
function Log(target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    const originalMethod = descriptor.value;
    
    descriptor.value = function(...args: any[]) {
        console.log(`Calling ${propertyKey} with`, args);
        return originalMethod.apply(this, args);
    };
}

class Example {
    @Log
    greet(name: string) {
        return `Hello ${name}`;
    }
}
```

---

**问题 5：如何实现一个类型安全的深度 Partial？**

**答：**

```typescript
type DeepPartial<T> = {
    [P in keyof T]?: T[P] extends Array<infer U>
        ? Array<DeepPartial<U>>
        : T[P] extends object
            ? DeepPartial<T[P]>
            : T[P];
};

interface Company {
    name: string;
    address: {
        city: string;
        street: string;
    };
    employees: Array<{
        name: string;
        role: string;
    }>;
}

type PartialCompany = DeepPartial<Company>;
// 所有层级都变为可选
```

---

**问题 6：TypeScript 与 Java 泛型的主要区别？**

**答：**

| 特性        | Java                       | TypeScript                  |
|-----------|----------------------------|-----------------------------|
| **类型擦除**  | 完全擦除                       | 编译后擦除                       |
| **原始类型**  | 不支持（需用包装类）                 | 支持（`number` 包含 `int/float`） |
| **联合类型**  | 不支持                        | `string \| number`          |
| **泛型约束**  | `T extends Number`         | `T extends number`          |
| **通配符**   | `? extends T`, `? super T` | 条件类型替代                      |
| **类型推断**  | 钻石操作符 `<>`                 | 强大的上下文推断                    |
| **泛型默认值** | 不支持                        | `T = string`                |

**TypeScript 优势：**

1. 联合/交叉类型更灵活
2. 条件类型实现复杂类型逻辑
3. 类型推断更智能
4. 映射类型生成新类型
