# Linux 与 Shell 核心知识点详解

## 一、Linux 核心知识

### 1.1 文件与目录操作

#### 基本命令

```bash
ls -la          # 列出所有文件（含隐藏文件），显示详细信息
cd /path        # 切换目录
pwd             # 显示当前目录
mkdir -p a/b/c  # 递归创建目录
rm -rf dir      # 强制递归删除目录（危险！）
cp -r src dst   # 递归复制目录
mv old new      # 移动/重命名
find / -name "*.log"  # 查找文件
```

### 1.2 文件权限（重要！）

#### 权限表示

```
-rwxr-xr-x  1  user  group  size  date  filename
│└┬┘└┬┘└┬┘
│ │  │  └── 其他用户权限 (r-x = 5)
│ │  └───── 所属组权限   (r-x = 5)
│ └──────── 所有者权限   (rwx = 7)
└────────── 文件类型 (-=普通文件，d=目录，l=链接)
```

#### 权限计算

- r(读) = 4
- w(写) = 2
- x(执行) = 1

**示例：**

```bash
chmod 755 file    # 设置权限为 rwxr-xr-x
chmod +x file     # 添加执行权限
chown user:group file  # 修改文件所有者
```

### 1.3 进程管理

#### 常用命令

```bash
ps -ef           # 查看所有进程
ps aux           # BSD 风格，显示更多信息（CPU、内存占用）
ps -ef | grep java  # 查找 Java 进程
top              # 实时查看进程（按 q 退出）
htop             # 更友好的 top（需安装）
kill PID         # 发送 SIGTERM 信号（优雅终止）
kill -9 PID      # 发送 SIGKILL 信号（强制终止）
nohup cmd &      # 后台运行，忽略挂断信号
jobs             # 查看后台任务
fg %1            # 将后台任务 1 调到前台
```

### 1.4 网络命令

```bash
netstat -tlnp    # 查看监听端口（t=TCP, l=监听，n=数字，p=进程）
ss -tlnp         # 更快的 netstat 替代
lsof -i:8080     # 查看占用 8080 端口的进程
curl http://...  # 发送 HTTP 请求
wget http://...  # 下载文件
ping host        # 测试网络连通性
traceroute host  # 追踪路由
ifconfig         # 查看网卡信息
ip addr          # 更现代的 ifconfig 替代
```

### 1.5 日志查看

```bash
cat file         # 查看整个文件
head -n 100 file # 查看前 100 行
tail -n 100 file # 查看后 100 行
tail -f file     # 实时追踪文件（查看日志常用）
less file        # 分页查看（支持上下翻页）
grep "ERROR" file  # 搜索关键字
grep -C 5 "ERROR" file  # 显示匹配行的前后 5 行
zcat file.gz | grep ...  # 查看压缩日志
```

### 1.6 系统信息

```bash
df -h            # 磁盘使用情况（-h 人性化显示）
du -sh *         # 当前目录各文件大小
free -h          # 内存使用情况
uptime           # 系统运行时间、负载
uname -a         # 系统内核信息
cat /etc/os-release  # 操作系统版本
whoami           # 当前用户
w                # 查看登录用户
```

---

## 二、Shell 脚本

### 2.1 Shell 脚本基础

#### 脚本开头

```bash
#!/bin/bash      # 指定解释器（Shebang）
set -e           # 遇到错误立即退出
set -x           # 打印执行的命令（调试用）
```

### 2.2 变量

#### 变量定义

```bash
NAME="value"
echo $NAME       # 使用变量
echo ${NAME}     # 推荐写法，避免歧义
echo "${NAME}"   # 双引号内可解析变量
echo '${NAME}'   # 单引号内不解析变量
```

#### 特殊变量

| 变量            | 含义              |
|---------------|-----------------|
| `$0`          | 脚本名称            |
| `$1, $2, ...` | 位置参数            |
| `$#`          | 参数个数            |
| `$@`          | 所有参数（作为独立参数）    |
| `$*`          | 所有参数（作为一个字符串）   |
| `$?`          | 上一条命令的返回值（0=成功） |
| `$$`          | 当前 Shell 的 PID  |

### 2.3 条件判断

#### if 语句

```bash
if [ -f "file.txt" ]; then
    echo "文件存在"
elif [ -d "dir" ]; then
    echo "目录存在"
else
    echo "不存在"
fi
```

#### 常用判断条件

```bash
[ -f file ]      # 文件存在且是普通文件
[ -d dir ]       # 目录存在
[ -e path ]      # 路径存在（文件或目录）
[ -z "$var" ]    # 变量为空
[ -n "$var" ]    # 变量非空
[ "$a" = "$b" ]  # 字符串相等（注意空格！）
[ $a -eq $b ]    # 数字相等
[ $a -gt $b ]    # 大于
[ $a -lt $b ]    # 小于
```

### 2.4 循环

#### for 循环

```bash
for i in 1 2 3 4 5; do
    echo $i
done

for file in *.txt; do
    echo "处理：$file"
done
```

#### while 循环

```bash
while [ $count -lt 10 ]; do
    echo $count
    count=$((count + 1))
done

# 逐行读取文件
while read line; do
    echo $line
done < file.txt
```

### 2.5 函数

```bash
function deploy() {
    local env=$1    # 局部变量
    echo "部署到 $env 环境"
    return 0        # 返回状态码
}

deploy "production"
result=$?           # 获取返回值
```

### 2.6 实用脚本：Java 应用部署脚本

```bash
#!/bin/bash
set -e

APP_NAME="my-app"
JAR_FILE="${APP_NAME}.jar"
PID_FILE="${APP_NAME}.pid"

start() {
    if [ -f "$PID_FILE" ]; then
        echo "应用已在运行"
        return 1
    fi
    nohup java -jar $JAR_FILE > app.log 2>&1 &
    echo $! > $PID_FILE
    echo "启动成功，PID: $(cat $PID_FILE)"
}

stop() {
    if [ ! -f "$PID_FILE" ]; then
        echo "应用未运行"
        return 1
    fi
    kill $(cat $PID_FILE) && rm -f $PID_FILE
    echo "停止成功"
}

case "$1" in
    start)  start ;;
    stop)   stop ;;
    restart) stop; start ;;
    *) echo "Usage: $0 {start|stop|restart}" ;;
esac
```

---

## 三、高频面试题汇总

### Linux 面试题

#### **问题 1：如何查看某个端口被哪个进程占用？**

**答：**

1. `netstat -tlnp | grep 8080`
2. `ss -tlnp | grep 8080`
3. `lsof -i:8080`

#### **问题 2:chmod 755 是什么意思？**

**答：**

755 = rwxr-xr-x

- 7(rwx): 所有者可读、可写、可执行
- 5(r-x): 所属组可读、可执行
- 5(r-x): 其他用户可读、可执行

#### **问题 3：如何实时查看日志文件？**

**答：**

```bash
tail -f /var/log/app.log
# 或查看最后 100 行并持续追踪
tail -n 100 -f /var/log/app.log
```

#### **问题 4:kill 和 kill -9 的区别？**

**答：**

- `kill PID`: 发送 SIGTERM 信号，进程可以捕获并优雅退出
- `kill -9 PID`: 发送 SIGKILL 信号，强制终止，无法被捕获

#### **问题 5：如何让程序后台运行，且关闭终端后继续运行？**

**答：**

```bash
nohup java -jar app.jar > app.log 2>&1 &
# nohup: 忽略挂断信号
# > app.log: 输出重定向
# 2>&1: 错误输出重定向到标准输出
# &: 后台运行
```

### Shell 面试题

#### **问题 6:$?表示什么？**

**答：**

上一条命令的返回值（退出状态码）

- 0: 成功
- 非 0: 失败

#### **问题 7：单引号和双引号的区别？**

**答：**

- 单引号：原样输出，不解析变量
  ```bash
  echo '$HOME' → 输出 $HOME
  ```
- 双引号：解析变量
  ```bash
  echo "$HOME" → 输出 /home/user
  ```

#### **问题 8：如何在 Shell 中判断文件是否存在？**

**答：**

```bash
if [ -f "/path/to/file" ]; then
    echo "文件存在"
fi
# -f: 是否是普通文件
# -d: 是否是目录
# -e: 路径是否存在
```

---

## 总结

本文详细介绍了 Linux 和 Shell 的核心知识点：

1. **Linux**：文件权限、进程管理、网络命令、日志查看、系统信息
2. **Shell**：变量、条件判断、循环、函数、实战脚本

每个部分都配有高频面试题及参考答案，帮助理解和应对面试。

---

**维护者：** itzixiao  
**最后更新：** 2026-03-21  
**问题反馈：** 欢迎提 Issue 或 PR
