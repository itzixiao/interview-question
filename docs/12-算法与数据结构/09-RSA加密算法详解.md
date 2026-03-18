# RSA加密算法详解

> **文档信息**
> - **难度等级**：⭐⭐⭐⭐（中高级）
> - **学习时长**：建议1-1.5天
> - **面试频率**：⭐⭐⭐⭐（高）
> - **配套代码**：`interview-algorithm/crypto/RSAAlgorithm.java`（408行，12种加密功能）
> - **关联文档**：[05-哈希算法详解](./05-%E5%93%88%E5%B8%8C%E7%AE%97%E6%B3%95%E8%AF%A6%E8%A7%A3.md)

## 一、算法概述

### 1.1 什么是RSA

RSA算法由Ron Rivest、Adi Shamir和Leonard Adleman于1977年提出，是**第一个既能用于数据加密也能用于数字签名的非对称加密算法**，也是目前应用最广泛的公钥加密算法之一。

**核心特点**：
- ✅ 非对称加密：公钥加密，私钥解密
- ✅ 数字签名：私钥签名，公钥验证
- ✅ 基于数学难题：大整数分解难题
- ❌ 计算速度慢：比对称加密慢100-1000倍

### 1.2 核心原理

基于**大整数分解的数学难题**：
```
正向计算（容易）：
  选择两个大质数 p 和 q
  计算 n = p × q（几秒钟）

逆向计算（困难）：
  已知 n，分解出 p 和 q
  2048位的n，用超级计算机需要数亿年
```

**密钥生成步骤**：
```
1. 选择两个大质数 p 和 q
   （实际应用中选择512位或1024位的质数）

2. 计算 n = p × q
   （n称为模数，也是密钥长度）

3. 计算欧拉函数 φ(n) = (p-1) × (q-1)

4. 选择公钥指数 e
   - 条件：1 < e < φ(n) 且 gcd(e, φ(n)) = 1
   - 常用值：3, 17, 65537（0x10001）

5. 计算私钥指数 d
   - 条件：d × e ≡ 1 (mod φ(n))
   - 即：d是e关于模φ(n)的模反元素
   - 使用扩展欧几里得算法计算
```

### 1.3 密钥对

| 密钥类型 | 组成 | 用途 | 公开性 |
|----------|------|------|--------|
| **公钥** | (e, n) | 加密数据、验证签名 | 公开 |
| **私钥** | (d, n) | 解密数据、生成签名 | 保密 |

**为什么安全？**
```
已知公钥(e, n)，要计算私钥d，需要知道φ(n)
要计算φ(n) = (p-1)(q-1)，需要知道p和q
要从n分解出p和q，就是大整数分解难题
```

## 二、数学基础

### 2.1 加密解密公式

```
加密: c = m^e mod n
解密: m = c^d mod n

其中:
- m: 明文（消息）
- c: 密文
- (e, n): 公钥
- (d, n): 私钥
```

### 2.2 密钥生成示例（小数字）

```
p = 61, q = 53
n = p × q = 3233
φ(n) = (p-1) × (q-1) = 3120

e = 17 （选择公钥指数）
d = 2753 （计算私钥指数，满足 e×d ≡ 1 mod φ(n)）

公钥: (17, 3233)
私钥: (2753, 3233)

加密: m = 123
c = 123^17 mod 3233 = 855

解密: 
m = 855^2753 mod 3233 = 123 ✓
```

## 三、Java实现

### 3.1 密钥生成

```java
public static KeyPair generateKeyPair(int keySize) throws Exception {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(keySize);  // 推荐2048或4096位
    return keyGen.generateKeyPair();
}
```

### 3.2 加密解密

```java
// 加密（使用公钥）
public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.ENCRYPT_MODE, publicKey);
    byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(encryptedBytes);
}

// 解密（使用私钥）
public static String decrypt(String cipherText, PrivateKey privateKey) throws Exception {
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.DECRYPT_MODE, privateKey);
    byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(cipherText));
    return new String(decryptedBytes, StandardCharsets.UTF_8);
}
```

### 3.3 数字签名

```java
// 签名（使用私钥）
public static String sign(String message, PrivateKey privateKey) throws Exception {
    Signature signature = Signature.getInstance("SHA256withRSA");
    signature.initSign(privateKey);
    signature.update(message.getBytes(StandardCharsets.UTF_8));
    byte[] signedBytes = signature.sign();
    return Base64.getEncoder().encodeToString(signedBytes);
}

// 验证签名（使用公钥）
public static boolean verify(String message, String signature, PublicKey publicKey) throws Exception {
    Signature sig = Signature.getInstance("SHA256withRSA");
    sig.initVerify(publicKey);
    sig.update(message.getBytes(StandardCharsets.UTF_8));
    return sig.verify(Base64.getDecoder().decode(signature));
}
```

## 四、安全增强

### 4.1 RSA-OAEP（推荐）

```java
// OAEP是更安全的填充方案
public static String encryptOAEP(String plainText, PublicKey publicKey) throws Exception {
    Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
    cipher.init(Cipher.ENCRYPT_MODE, publicKey);
    byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(encryptedBytes);
}
```

### 4.2 密钥序列化

```java
// 公钥转Base64
public static String publicKeyToBase64(PublicKey publicKey) {
    return Base64.getEncoder().encodeToString(publicKey.getEncoded());
}

// Base64转公钥
public static PublicKey base64ToPublicKey(String base64Key) throws Exception {
    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
    X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePublic(spec);
}
```

## 五、RSA特点与限制

### 5.1 特点

| 特点 | 说明 |
|------|------|
| 非对称加密 | 公钥加密，私钥解密 |
| 速度慢 | 比对称加密慢100-1000倍 |
| 适合小数据 | 通常用于加密对称密钥 |
| 安全性高 | 基于大数分解难题 |

### 5.2 密钥长度与性能

| 密钥长度 | 安全性 | 加密速度 | 密文长度 |
|----------|--------|----------|----------|
| 1024位 | 低（已不推荐） | 快 | 128字节 |
| 2048位 | 中（当前标准） | 中等 | 256字节 |
| 4096位 | 高 | 慢 | 512字节 |

## 六、实际应用场景

### 6.1 HTTPS/TLS握手

1. 客户端发送支持的加密算法列表
2. 服务器返回证书（包含公钥）
3. 客户端生成随机对称密钥
4. 客户端用公钥加密对称密钥发送给服务器
5. 双方使用对称密钥通信

### 6.2 数字证书

- 证书包含：公钥、持有者信息、颁发机构签名
- 用于验证网站身份

### 6.3 区块链

- 比特币、以太坊使用ECDSA（类似RSA的椭圆曲线算法）
- 用于交易签名验证

## 七、面试高频题详解

### 题1：RSA为什么安全？⭐⭐⭐⭐⭐

**数学基础**：大整数分解难题

```
问题：给定大整数n，分解出它的质因数p和q

难度对比：
┌─────────────────────────────────────────────────┐
│ 正向计算（乘法）                                  │
│ p = 大质数（1024位）                              │
│ q = 大质数（1024位）                              │
│ n = p × q = 2048位                               │
│ 计算时间：毫秒级                                  │
├─────────────────────────────────────────────────┤
│ 逆向计算（分解）                                  │
│ 已知n（2048位），求p和q                           │
│ 目前最快算法：数域筛法（NFS）                      │
│ 估计时间：数亿年（使用超级计算机）                  │
└─────────────────────────────────────────────────┘
```

**密钥长度与安全等级**：
| 密钥长度 | 安全等级 | 预计破解时间 | 推荐用途 |
|----------|----------|--------------|----------|
| 1024位 | 低 | 已可破解 | 不再使用 |
| 2048位 | 中 | 安全至2030年 | 当前标准 |
| 4096位 | 高 | 长期安全 | 高安全场景 |

### 题2：RSA和对称加密的区别？⭐⭐⭐⭐⭐

| 对比维度 | RSA（非对称） | AES（对称） |
|----------|---------------|-------------|
| **密钥** | 公钥+私钥 | 单一密钥 |
| **速度** | 慢（比AES慢100-1000倍） | 快 |
| **数据大小** | 只能加密小数据（<密钥长度） | 可加密任意大小数据 |
| **用途** | 密钥交换、数字签名 | 大数据加密 |
| **安全性基础** | 大数分解难题 | 无有效攻击方法 |

**实际应用方案（混合加密）**：
```
1. 生成随机的AES密钥（会话密钥）
2. 用AES加密大数据（速度快）
3. 用RSA加密AES密钥（安全传输密钥）
4. 传输：RSA加密后的AES密钥 + AES加密后的数据

HTTPS/TLS就是使用这种混合加密方案
```

### 题3：RSA加密的数据长度限制？⭐⭐⭐⭐

**限制原因**：
```
加密公式：c = m^e mod n

明文m必须是小于n的整数
如果m >= n，模运算会丢失信息
```

**计算公式**：
```
最大明文长度 = 密钥长度 / 8 - 填充长度

PKCS#1 v1.5填充：
- 填充长度 = 11字节
- 2048位密钥：256 - 11 = 245字节

OAEP填充（更安全）：
- 填充长度 = 2 × hash长度 + 2
- SHA-256：2 × 32 + 2 = 66字节
- 2048位密钥：256 - 66 = 190字节
```

**解决方案**：
- 大数据加密：使用对称加密（AES）
- RSA仅用于加密对称密钥

### 题4：数字签名和加密的区别？⭐⭐⭐⭐⭐

| 维度 | 加密 | 数字签名 |
|------|------|----------|
| **目的** | 保护数据机密性 | 验证身份和完整性 |
| **密钥使用** | 公钥加密，私钥解密 | 私钥签名，公钥验证 |
| **过程** | 明文 → 密文 → 明文 | 数据 → 签名 → 验证 |
| **作用** | 防止数据被窃取 | 防止数据被篡改、验证发送者 |

**数字签名流程**：
```
发送方：
1. 计算数据的哈希值：hash = SHA256(data)
2. 用私钥加密哈希：signature = RSA_sign(hash, privateKey)
3. 发送：data + signature

接收方：
1. 用公钥解密签名：hash1 = RSA_verify(signature, publicKey)
2. 计算收到数据的哈希：hash2 = SHA256(data)
3. 比较：hash1 == hash2 ? 验证通过 : 验证失败
```

**实际应用**：
- 软件签名（验证软件来源）
- 证书签名（HTTPS证书）
- 区块链交易签名

### 题5：HTTPS/TLS握手过程？⭐⭐⭐⭐⭐

```
客户端                    服务器
  |                         |
  |--------ClientHello----->|  支持的加密算法列表
  |                         |
  |<-------ServerHello------|  选择的加密算法
  |<-------Certificate-----|  服务器证书（含公钥）
  |                         |
  |  验证证书有效性            |
  |  生成随机Pre-master Key  |
  |--------ClientKeyExchange>|  用公钥加密Pre-master Key
  |                         |
  |  双方生成Master Key      |
  |  后续使用对称加密通信      |
  |<======加密数据传输======>|
```

**关键步骤**：
1. 服务器发送证书（包含RSA公钥）
2. 客户端生成随机对称密钥
3. 客户端用RSA公钥加密对称密钥
4. 双方使用对称密钥加密通信数据

## 八、知识图谱

```
RSA加密体系
│
├── 数学基础
│   ├── 大整数分解难题
│   ├── 欧拉函数 φ(n) = (p-1)(q-1)
│   ├── 模幂运算
│   └── 扩展欧几里得算法
│
├── 密钥管理
│   ├── 密钥生成
│   ├── 密钥分发
│   └── 密钥存储
│
├── 核心功能
│   ├── 加密解密
│   │   ├── 公钥加密
│   │   └── 私钥解密
│   │
│   └── 数字签名
│       ├── 私钥签名
│       └── 公钥验证
│
├── 安全增强
│   ├── OAEP填充
│   ├── PSS签名
│   └── 密钥长度选择
│
├── 实际应用
│   ├── HTTPS/TLS
│   ├── SSH
│   ├── 数字证书
│   └── 区块链
│
└── 面试重点
    ├── 安全性原理（大数分解）
    ├── 与对称加密对比
    ├── 数据长度限制
    ├── 加密vs签名区别
    └── HTTPS握手过程
```

## 九、示例代码

完整示例代码位于：`interview-microservices-parent/interview-algorithm/src/main/java/cn/itzixiao/interview/algorithm/crypto/RSAAlgorithm.java`

运行方式：
```bash
mvn exec:java -pl interview-microservices-parent/interview-algorithm \
  -Dexec.mainClass="cn.itzixiao.interview.algorithm.crypto.RSAAlgorithm" -q
```
