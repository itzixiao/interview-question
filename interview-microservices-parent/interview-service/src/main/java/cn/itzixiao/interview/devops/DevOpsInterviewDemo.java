package cn.itzixiao.interview.devops;

/**
 * DevOps 核心知识点详解 - Linux、Shell、Docker、Jenkins CI/CD、Nginx
 *
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────────────────┐
 * │                           DevOps 技术体系                                     │
 * ├──────────────────────────────────────────────────────────────────────────────┤
 * │                                                                              │
 * │    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                     │
 * │    │   Linux     │    │   Docker    │    │   Jenkins   │                     │
 * │    │   操作系统  │    │   容器化    │    │   CI/CD     │                     │
 * │    └──────┬──────┘    └──────┬──────┘    └──────┬──────┘                     │
 * │           │                  │                  │                            │
 * │    ┌──────▼──────┐    ┌──────▼──────┐    ┌──────▼──────┐                     │
 * │    │ Shell脚本   │    │ 镜像/容器   │    │ Pipeline    │                     │
 * │    │ 文件权限    │    │ 网络/存储   │    │ 自动部署    │                     │
 * │    │ 进程管理    │    │ Dockerfile  │    │ 触发构建    │                     │
 * │    └─────────────┘    └─────────────┘    └─────────────┘                     │
 * │                              │                                               │
 * │                       ┌──────▼──────┐                                        │
 * │                       │   Nginx     │                                        │
 * │                       │ 反向代理    │                                        │
 * │                       │ 负载均衡    │                                        │
 * │                       └─────────────┘                                        │
 * └──────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @author itzixiao
 */
public class DevOpsInterviewDemo {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║       DevOps 核心知识点 - Linux、Shell、Docker、Jenkins、Nginx            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝\n");

        // 第一部分：Linux 核心知识
        demonstrateLinux();

        // 第二部分：Shell 脚本
        demonstrateShell();

        // 第三部分：Docker 容器化
        demonstrateDocker();

        // 第四部分：Jenkins CI/CD
        demonstrateJenkins();

        // 第五部分：Nginx 反向代理与负载均衡
        demonstrateNginx();

        // 第六部分：高频面试题
        printInterviewQuestions();
    }

    // ==================== 第一部分：Linux 核心知识 ====================

    private static void demonstrateLinux() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第一部分：Linux 核心知识                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【1.1 文件与目录操作】\n");

        System.out.println("基本命令：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  ls -la          # 列出所有文件（含隐藏文件），显示详细信息              │");
        System.out.println("│  cd /path        # 切换目录                                             │");
        System.out.println("│  pwd             # 显示当前目录                                         │");
        System.out.println("│  mkdir -p a/b/c  # 递归创建目录                                         │");
        System.out.println("│  rm -rf dir      # 强制递归删除目录（危险！）                           │");
        System.out.println("│  cp -r src dst   # 递归复制目录                                         │");
        System.out.println("│  mv old new      # 移动/重命名                                          │");
        System.out.println("│  find / -name \"*.log\"  # 查找文件                                      │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【1.2 文件权限（重要！）】\n");

        System.out.println("权限表示：rwx = 读(4) + 写(2) + 执行(1)");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  -rwxr-xr-x  1  user  group  size  date  filename                      │");
        System.out.println("│  │└┬┘└┬┘└┬┘                                                            │");
        System.out.println("│  │ │  │  └── 其他用户权限 (r-x = 5)                                    │");
        System.out.println("│  │ │  └───── 所属组权限   (r-x = 5)                                    │");
        System.out.println("│  │ └──────── 所有者权限   (rwx = 7)                                    │");
        System.out.println("│  └────────── 文件类型 (-=普通文件, d=目录, l=链接)                      │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  chmod 755 file    # 设置权限为 rwxr-xr-x                              │");
        System.out.println("│  chmod +x file     # 添加执行权限                                       │");
        System.out.println("│  chmod u+w file    # 给所有者添加写权限                                 │");
        System.out.println("│  chown user:group file  # 修改文件所有者                               │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【1.3 进程管理】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  ps -ef           # 查看所有进程                                        │");
        System.out.println("│  ps aux           # BSD风格，显示更多信息（CPU、内存占用）              │");
        System.out.println("│  ps -ef | grep java  # 查找Java进程                                    │");
        System.out.println("│  top              # 实时查看进程（按q退出）                             │");
        System.out.println("│  htop             # 更友好的top（需安装）                               │");
        System.out.println("│  kill PID         # 发送SIGTERM信号（优雅终止）                        │");
        System.out.println("│  kill -9 PID      # 发送SIGKILL信号（强制终止）                        │");
        System.out.println("│  nohup cmd &      # 后台运行，忽略挂断信号                              │");
        System.out.println("│  jobs             # 查看后台任务                                        │");
        System.out.println("│  fg %1            # 将后台任务1调到前台                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【1.4 网络命令】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  netstat -tlnp    # 查看监听端口（t=TCP, l=监听, n=数字, p=进程）       │");
        System.out.println("│  ss -tlnp         # 更快的netstat替代                                   │");
        System.out.println("│  lsof -i:8080     # 查看占用8080端口的进程                              │");
        System.out.println("│  curl http://...  # 发送HTTP请求                                        │");
        System.out.println("│  wget http://...  # 下载文件                                            │");
        System.out.println("│  ping host        # 测试网络连通性                                      │");
        System.out.println("│  traceroute host  # 追踪路由                                            │");
        System.out.println("│  ifconfig         # 查看网卡信息                                        │");
        System.out.println("│  ip addr          # 更现代的ifconfig替代                                │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【1.5 日志查看】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  cat file         # 查看整个文件                                        │");
        System.out.println("│  head -n 100 file # 查看前100行                                         │");
        System.out.println("│  tail -n 100 file # 查看后100行                                         │");
        System.out.println("│  tail -f file     # 实时追踪文件（查看日志常用）                        │");
        System.out.println("│  less file        # 分页查看（支持上下翻页）                            │");
        System.out.println("│  grep \"ERROR\" file  # 搜索关键字                                       │");
        System.out.println("│  grep -i \"error\" file  # 忽略大小写                                    │");
        System.out.println("│  grep -C 5 \"ERROR\" file  # 显示匹配行的前后5行                        │");
        System.out.println("│  zcat file.gz | grep ...  # 查看压缩日志                               │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【1.6 系统信息】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  df -h            # 磁盘使用情况（-h 人性化显示）                       │");
        System.out.println("│  du -sh *         # 当前目录各文件大小                                  │");
        System.out.println("│  free -h          # 内存使用情况                                        │");
        System.out.println("│  uptime           # 系统运行时间、负载                                  │");
        System.out.println("│  uname -a         # 系统内核信息                                        │");
        System.out.println("│  cat /etc/os-release  # 操作系统版本                                   │");
        System.out.println("│  whoami           # 当前用户                                            │");
        System.out.println("│  w                # 查看登录用户                                        │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第二部分：Shell 脚本 ====================

    private static void demonstrateShell() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第二部分：Shell 脚本                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【2.1 Shell 脚本基础】\n");

        System.out.println("脚本开头：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  #!/bin/bash      # 指定解释器（Shebang）                               │");
        System.out.println("│  set -e           # 遇到错误立即退出                                    │");
        System.out.println("│  set -x           # 打印执行的命令（调试用）                            │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【2.2 变量】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  # 变量定义（等号两边不能有空格！）                                     │");
        System.out.println("│  NAME=\"value\"                                                          │");
        System.out.println("│  echo $NAME       # 使用变量                                            │");
        System.out.println("│  echo ${NAME}     # 推荐写法，避免歧义                                  │");
        System.out.println("│  echo \"${NAME}\"   # 双引号内可解析变量                                 │");
        System.out.println("│  echo '${NAME}'   # 单引号内不解析变量                                  │");
        System.out.println("│                                                                        │");
        System.out.println("│  # 特殊变量                                                             │");
        System.out.println("│  $0               # 脚本名称                                            │");
        System.out.println("│  $1, $2, ...      # 位置参数                                            │");
        System.out.println("│  $#               # 参数个数                                            │");
        System.out.println("│  $@               # 所有参数（作为独立参数）                            │");
        System.out.println("│  $*               # 所有参数（作为一个字符串）                          │");
        System.out.println("│  $?               # 上一条命令的返回值（0=成功）                        │");
        System.out.println("│  $$               # 当前Shell的PID                                      │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【2.3 条件判断】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  # if 语句                                                              │");
        System.out.println("│  if [ -f \"file.txt\" ]; then                                            │");
        System.out.println("│      echo \"文件存在\"                                                   │");
        System.out.println("│  elif [ -d \"dir\" ]; then                                               │");
        System.out.println("│      echo \"目录存在\"                                                   │");
        System.out.println("│  else                                                                  │");
        System.out.println("│      echo \"不存在\"                                                     │");
        System.out.println("│  fi                                                                    │");
        System.out.println("│                                                                        │");
        System.out.println("│  # 常用判断条件                                                         │");
        System.out.println("│  [ -f file ]      # 文件存在且是普通文件                                │");
        System.out.println("│  [ -d dir ]       # 目录存在                                            │");
        System.out.println("│  [ -e path ]      # 路径存在（文件或目录）                              │");
        System.out.println("│  [ -z \"$var\" ]    # 变量为空                                           │");
        System.out.println("│  [ -n \"$var\" ]    # 变量非空                                           │");
        System.out.println("│  [ \"$a\" = \"$b\" ]  # 字符串相等（注意空格！）                           │");
        System.out.println("│  [ $a -eq $b ]    # 数字相等                                            │");
        System.out.println("│  [ $a -gt $b ]    # 大于                                                │");
        System.out.println("│  [ $a -lt $b ]    # 小于                                                │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【2.4 循环】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  # for 循环                                                             │");
        System.out.println("│  for i in 1 2 3 4 5; do                                                │");
        System.out.println("│      echo $i                                                           │");
        System.out.println("│  done                                                                  │");
        System.out.println("│                                                                        │");
        System.out.println("│  for file in *.txt; do                                                 │");
        System.out.println("│      echo \"处理: $file\"                                                │");
        System.out.println("│  done                                                                  │");
        System.out.println("│                                                                        │");
        System.out.println("│  # while 循环                                                           │");
        System.out.println("│  while [ $count -lt 10 ]; do                                           │");
        System.out.println("│      echo $count                                                       │");
        System.out.println("│      count=$((count + 1))                                              │");
        System.out.println("│  done                                                                  │");
        System.out.println("│                                                                        │");
        System.out.println("│  # 逐行读取文件                                                         │");
        System.out.println("│  while read line; do                                                   │");
        System.out.println("│      echo $line                                                        │");
        System.out.println("│  done < file.txt                                                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【2.5 函数】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  # 定义函数                                                             │");
        System.out.println("│  function deploy() {                                                   │");
        System.out.println("│      local env=$1    # 局部变量                                        │");
        System.out.println("│      echo \"部署到 $env 环境\"                                           │");
        System.out.println("│      return 0        # 返回状态码                                       │");
        System.out.println("│  }                                                                     │");
        System.out.println("│                                                                        │");
        System.out.println("│  # 调用函数                                                             │");
        System.out.println("│  deploy \"production\"                                                   │");
        System.out.println("│  result=$?           # 获取返回值                                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【2.6 实用脚本示例：Java 应用部署脚本】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  #!/bin/bash                                                           │");
        System.out.println("│  set -e                                                                │");
        System.out.println("│                                                                        │");
        System.out.println("│  APP_NAME=\"my-app\"                                                     │");
        System.out.println("│  JAR_FILE=\"${APP_NAME}.jar\"                                            │");
        System.out.println("│  PID_FILE=\"${APP_NAME}.pid\"                                            │");
        System.out.println("│                                                                        │");
        System.out.println("│  start() {                                                             │");
        System.out.println("│      if [ -f \"$PID_FILE\" ]; then                                       │");
        System.out.println("│          echo \"应用已在运行\"                                           │");
        System.out.println("│          return 1                                                      │");
        System.out.println("│      fi                                                                │");
        System.out.println("│      nohup java -jar $JAR_FILE > app.log 2>&1 &                        │");
        System.out.println("│      echo $! > $PID_FILE                                               │");
        System.out.println("│      echo \"启动成功，PID: $(cat $PID_FILE)\"                            │");
        System.out.println("│  }                                                                     │");
        System.out.println("│                                                                        │");
        System.out.println("│  stop() {                                                              │");
        System.out.println("│      if [ ! -f \"$PID_FILE\" ]; then                                     │");
        System.out.println("│          echo \"应用未运行\"                                             │");
        System.out.println("│          return 1                                                      │");
        System.out.println("│      fi                                                                │");
        System.out.println("│      kill $(cat $PID_FILE) && rm -f $PID_FILE                          │");
        System.out.println("│      echo \"停止成功\"                                                   │");
        System.out.println("│  }                                                                     │");
        System.out.println("│                                                                        │");
        System.out.println("│  case \"$1\" in                                                          │");
        System.out.println("│      start)  start ;;                                                  │");
        System.out.println("│      stop)   stop ;;                                                   │");
        System.out.println("│      restart) stop; start ;;                                           │");
        System.out.println("│      *) echo \"Usage: $0 {start|stop|restart}\" ;;                      │");
        System.out.println("│  esac                                                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第三部分：Docker 容器化 ====================

    private static void demonstrateDocker() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第三部分：Docker 容器化                             ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【3.1 Docker 核心概念】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  镜像（Image）                                                          │");
        System.out.println("│  - 只读模板，包含运行应用所需的一切                                     │");
        System.out.println("│  - 由多个层（Layer）组成，共享相同层                                    │");
        System.out.println("│                                                                        │");
        System.out.println("│  容器（Container）                                                      │");
        System.out.println("│  - 镜像的运行实例                                                       │");
        System.out.println("│  - 可读写层 + 镜像层                                                    │");
        System.out.println("│  - 生命周期：创建 → 运行 → 暂停 → 停止 → 删除                          │");
        System.out.println("│                                                                        │");
        System.out.println("│  仓库（Registry）                                                       │");
        System.out.println("│  - 存储和分发镜像的服务                                                 │");
        System.out.println("│  - Docker Hub（公共）、Harbor（私有）                                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【3.2 Docker 常用命令】\n");

        System.out.println("镜像操作：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  docker images            # 列出本地镜像                                │");
        System.out.println("│  docker pull nginx:latest # 拉取镜像                                    │");
        System.out.println("│  docker build -t myapp:v1 .  # 构建镜像                                 │");
        System.out.println("│  docker push myapp:v1     # 推送镜像                                    │");
        System.out.println("│  docker rmi image_id      # 删除镜像                                    │");
        System.out.println("│  docker tag old:v1 new:v1 # 给镜像打标签                                │");
        System.out.println("│  docker save -o file.tar image  # 导出镜像                              │");
        System.out.println("│  docker load -i file.tar  # 导入镜像                                    │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("容器操作：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  docker run -d -p 8080:80 --name web nginx  # 后台运行容器              │");
        System.out.println("│    -d: 后台运行                                                         │");
        System.out.println("│    -p: 端口映射（主机:容器）                                            │");
        System.out.println("│    --name: 容器名称                                                     │");
        System.out.println("│    -v: 挂载卷（主机目录:容器目录）                                      │");
        System.out.println("│    -e: 环境变量                                                         │");
        System.out.println("│    --restart=always: 自动重启                                           │");
        System.out.println("│                                                                        │");
        System.out.println("│  docker ps                # 查看运行中的容器                            │");
        System.out.println("│  docker ps -a             # 查看所有容器                                │");
        System.out.println("│  docker start/stop/restart container  # 启停容器                       │");
        System.out.println("│  docker rm container      # 删除容器                                    │");
        System.out.println("│  docker exec -it container bash  # 进入容器                             │");
        System.out.println("│  docker logs -f container # 查看容器日志                                │");
        System.out.println("│  docker inspect container # 查看容器详情                                │");
        System.out.println("│  docker cp file container:/path  # 复制文件到容器                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【3.3 Dockerfile 编写】\n");

        System.out.println("Spring Boot 应用 Dockerfile 示例：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  # 基础镜像                                                             │");
        System.out.println("│  FROM openjdk:17-jdk-slim                                              │");
        System.out.println("│                                                                        │");
        System.out.println("│  # 维护者信息                                                           │");
        System.out.println("│  LABEL maintainer=\"itzixiao@example.com\"                               │");
        System.out.println("│                                                                        │");
        System.out.println("│  # 设置工作目录                                                         │");
        System.out.println("│  WORKDIR /app                                                          │");
        System.out.println("│                                                                        │");
        System.out.println("│  # 设置时区                                                             │");
        System.out.println("│  ENV TZ=Asia/Shanghai                                                  │");
        System.out.println("│  RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime                    │");
        System.out.println("│                                                                        │");
        System.out.println("│  # 复制JAR包（利用Docker层缓存）                                        │");
        System.out.println("│  COPY target/*.jar app.jar                                             │");
        System.out.println("│                                                                        │");
        System.out.println("│  # 暴露端口                                                             │");
        System.out.println("│  EXPOSE 8080                                                           │");
        System.out.println("│                                                                        │");
        System.out.println("│  # JVM参数                                                              │");
        System.out.println("│  ENV JAVA_OPTS=\"-Xms512m -Xmx512m\"                                     │");
        System.out.println("│                                                                        │");
        System.out.println("│  # 启动命令                                                             │");
        System.out.println("│  ENTRYPOINT [\"sh\", \"-c\", \"java $JAVA_OPTS -jar app.jar\"]              │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【3.4 Docker Compose】\n");

        System.out.println("docker-compose.yml 示例：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  version: '3.8'                                                        │");
        System.out.println("│                                                                        │");
        System.out.println("│  services:                                                             │");
        System.out.println("│    app:                                                                │");
        System.out.println("│      build: .                                                          │");
        System.out.println("│      ports:                                                            │");
        System.out.println("│        - \"8080:8080\"                                                   │");
        System.out.println("│      environment:                                                      │");
        System.out.println("│        - SPRING_PROFILES_ACTIVE=prod                                   │");
        System.out.println("│        - DB_HOST=mysql                                                 │");
        System.out.println("│      depends_on:                                                       │");
        System.out.println("│        - mysql                                                         │");
        System.out.println("│        - redis                                                         │");
        System.out.println("│      networks:                                                         │");
        System.out.println("│        - app-network                                                   │");
        System.out.println("│                                                                        │");
        System.out.println("│    mysql:                                                              │");
        System.out.println("│      image: mysql:8.0                                                  │");
        System.out.println("│      environment:                                                      │");
        System.out.println("│        MYSQL_ROOT_PASSWORD: root123                                    │");
        System.out.println("│      volumes:                                                          │");
        System.out.println("│        - mysql-data:/var/lib/mysql                                     │");
        System.out.println("│      networks:                                                         │");
        System.out.println("│        - app-network                                                   │");
        System.out.println("│                                                                        │");
        System.out.println("│    redis:                                                              │");
        System.out.println("│      image: redis:7                                                    │");
        System.out.println("│      networks:                                                         │");
        System.out.println("│        - app-network                                                   │");
        System.out.println("│                                                                        │");
        System.out.println("│  volumes:                                                              │");
        System.out.println("│    mysql-data:                                                         │");
        System.out.println("│                                                                        │");
        System.out.println("│  networks:                                                             │");
        System.out.println("│    app-network:                                                        │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("Docker Compose 命令：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  docker-compose up -d      # 启动所有服务                               │");
        System.out.println("│  docker-compose down       # 停止并删除容器                             │");
        System.out.println("│  docker-compose ps         # 查看服务状态                               │");
        System.out.println("│  docker-compose logs -f    # 查看日志                                   │");
        System.out.println("│  docker-compose exec app bash  # 进入容器                               │");
        System.out.println("│  docker-compose restart app    # 重启服务                               │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【3.5 Docker 网络】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  网络模式：                                                              │");
        System.out.println("│  - bridge（默认）：容器通过虚拟网桥通信                                  │");
        System.out.println("│  - host：容器使用主机网络（性能最好，无隔离）                            │");
        System.out.println("│  - none：无网络                                                         │");
        System.out.println("│  - container：共享其他容器的网络                                        │");
        System.out.println("│                                                                        │");
        System.out.println("│  docker network ls                     # 列出网络                       │");
        System.out.println("│  docker network create mynet           # 创建网络                       │");
        System.out.println("│  docker run --network=mynet ...        # 指定网络                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第四部分：Jenkins CI/CD ====================

    private static void demonstrateJenkins() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第四部分：Jenkins CI/CD                             ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【4.1 CI/CD 概念】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  CI（持续集成）                                                         │");
        System.out.println("│  - 频繁合并代码到主干                                                   │");
        System.out.println("│  - 自动构建、自动测试                                                   │");
        System.out.println("│  - 快速发现问题                                                         │");
        System.out.println("│                                                                        │");
        System.out.println("│  CD（持续交付/部署）                                                    │");
        System.out.println("│  - 持续交付：自动化发布到测试/预发环境，手动发布到生产                  │");
        System.out.println("│  - 持续部署：全自动发布到生产环境                                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【4.2 Jenkins Pipeline】\n");

        System.out.println("Jenkinsfile（声明式Pipeline）：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  pipeline {                                                            │");
        System.out.println("│      agent any                                                         │");
        System.out.println("│                                                                        │");
        System.out.println("│      environment {                                                     │");
        System.out.println("│          DOCKER_REGISTRY = 'registry.example.com'                      │");
        System.out.println("│          IMAGE_NAME = 'my-app'                                         │");
        System.out.println("│      }                                                                 │");
        System.out.println("│                                                                        │");
        System.out.println("│      stages {                                                          │");
        System.out.println("│          stage('拉取代码') {                                            │");
        System.out.println("│              steps {                                                   │");
        System.out.println("│                  git branch: 'main',                                   │");
        System.out.println("│                      url: 'https://github.com/xxx/xxx.git'             │");
        System.out.println("│              }                                                         │");
        System.out.println("│          }                                                             │");
        System.out.println("│                                                                        │");
        System.out.println("│          stage('编译') {                                                │");
        System.out.println("│              steps {                                                   │");
        System.out.println("│                  sh 'mvn clean package -DskipTests'                    │");
        System.out.println("│              }                                                         │");
        System.out.println("│          }                                                             │");
        System.out.println("│                                                                        │");
        System.out.println("│          stage('单元测试') {                                            │");
        System.out.println("│              steps {                                                   │");
        System.out.println("│                  sh 'mvn test'                                         │");
        System.out.println("│              }                                                         │");
        System.out.println("│              post {                                                    │");
        System.out.println("│                  always {                                              │");
        System.out.println("│                      junit 'target/surefire-reports/*.xml'             │");
        System.out.println("│                  }                                                     │");
        System.out.println("│              }                                                         │");
        System.out.println("│          }                                                             │");
        System.out.println("│                                                                        │");
        System.out.println("│          stage('构建镜像') {                                            │");
        System.out.println("│              steps {                                                   │");
        System.out.println("│                  sh '''                                                │");
        System.out.println("│                      docker build -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER} .│");
        System.out.println("│                      docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER}     │");
        System.out.println("│                  '''                                                   │");
        System.out.println("│              }                                                         │");
        System.out.println("│          }                                                             │");
        System.out.println("│                                                                        │");
        System.out.println("│          stage('部署到测试环境') {                                      │");
        System.out.println("│              steps {                                                   │");
        System.out.println("│                  sh '''                                                │");
        System.out.println("│                      ssh user@test-server \"docker pull ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER}\"│");
        System.out.println("│                      ssh user@test-server \"docker-compose up -d\"      │");
        System.out.println("│                  '''                                                   │");
        System.out.println("│              }                                                         │");
        System.out.println("│          }                                                             │");
        System.out.println("│                                                                        │");
        System.out.println("│          stage('部署到生产环境') {                                      │");
        System.out.println("│              when {                                                    │");
        System.out.println("│                  branch 'main'                                         │");
        System.out.println("│              }                                                         │");
        System.out.println("│              input {                                                   │");
        System.out.println("│                  message '是否部署到生产环境？'                         │");
        System.out.println("│              }                                                         │");
        System.out.println("│              steps {                                                   │");
        System.out.println("│                  sh 'deploy-to-production.sh'                          │");
        System.out.println("│              }                                                         │");
        System.out.println("│          }                                                             │");
        System.out.println("│      }                                                                 │");
        System.out.println("│                                                                        │");
        System.out.println("│      post {                                                            │");
        System.out.println("│          success {                                                     │");
        System.out.println("│              echo '构建成功！'                                          │");
        System.out.println("│          }                                                             │");
        System.out.println("│          failure {                                                     │");
        System.out.println("│              echo '构建失败！'                                          │");
        System.out.println("│              // 发送通知                                                │");
        System.out.println("│          }                                                             │");
        System.out.println("│      }                                                                 │");
        System.out.println("│  }                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【4.3 Jenkins 触发方式】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 手动触发：在Jenkins界面点击Build                                    │");
        System.out.println("│                                                                        │");
        System.out.println("│  2. 定时触发（Cron表达式）：                                            │");
        System.out.println("│     triggers {                                                         │");
        System.out.println("│         cron('H 2 * * *')  // 每天凌晨2点                              │");
        System.out.println("│     }                                                                  │");
        System.out.println("│                                                                        │");
        System.out.println("│  3. Git Webhook 触发：                                                  │");
        System.out.println("│     - 代码推送时自动触发构建                                            │");
        System.out.println("│     - 需要在Git仓库配置Webhook URL                                      │");
        System.out.println("│                                                                        │");
        System.out.println("│  4. 轮询SCM：                                                           │");
        System.out.println("│     triggers {                                                         │");
        System.out.println("│         pollSCM('H/5 * * * *')  // 每5分钟检查一次                     │");
        System.out.println("│     }                                                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第五部分：Nginx 反向代理与负载均衡 ====================

    private static void demonstrateNginx() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第五部分：Nginx 反向代理与负载均衡                       ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【5.1 Nginx 核心概念】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  正向代理 vs 反向代理                                                   │");
        System.out.println("│                                                                        │");
        System.out.println("│  正向代理：代理客户端，服务端不知道真实客户端                           │");
        System.out.println("│    客户端 → [代理] → 服务端                                            │");
        System.out.println("│    例如：VPN、翻墙                                                      │");
        System.out.println("│                                                                        │");
        System.out.println("│  反向代理：代理服务端，客户端不知道真实服务端                           │");
        System.out.println("│    客户端 → [Nginx] → 后端服务器集群                                   │");
        System.out.println("│    例如：负载均衡、隐藏后端服务器                                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【5.2 Nginx 常用命令】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  nginx                    # 启动                                        │");
        System.out.println("│  nginx -s stop            # 快速停止                                    │");
        System.out.println("│  nginx -s quit            # 优雅停止                                    │");
        System.out.println("│  nginx -s reload          # 重新加载配置（热更新）                      │");
        System.out.println("│  nginx -t                 # 测试配置文件语法                            │");
        System.out.println("│  nginx -v                 # 查看版本                                    │");
        System.out.println("│  nginx -V                 # 查看版本和编译参数                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【5.3 Nginx 配置结构】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  # 全局块                                                               │");
        System.out.println("│  worker_processes auto;     # 工作进程数（建议设为CPU核心数）           │");
        System.out.println("│                                                                        │");
        System.out.println("│  # events块                                                             │");
        System.out.println("│  events {                                                              │");
        System.out.println("│      worker_connections 1024;  # 单个worker最大连接数                  │");
        System.out.println("│      use epoll;                # 使用epoll模型（Linux）                │");
        System.out.println("│  }                                                                     │");
        System.out.println("│                                                                        │");
        System.out.println("│  # http块                                                               │");
        System.out.println("│  http {                                                                │");
        System.out.println("│      include mime.types;                                               │");
        System.out.println("│      default_type application/octet-stream;                            │");
        System.out.println("│                                                                        │");
        System.out.println("│      # 日志格式                                                         │");
        System.out.println("│      log_format main '$remote_addr - $request - $status';              │");
        System.out.println("│      access_log /var/log/nginx/access.log main;                        │");
        System.out.println("│                                                                        │");
        System.out.println("│      # server块（虚拟主机）                                             │");
        System.out.println("│      server {                                                          │");
        System.out.println("│          listen 80;                                                    │");
        System.out.println("│          server_name example.com;                                      │");
        System.out.println("│                                                                        │");
        System.out.println("│          # location块（路由规则）                                       │");
        System.out.println("│          location / {                                                  │");
        System.out.println("│              root /usr/share/nginx/html;                               │");
        System.out.println("│              index index.html;                                         │");
        System.out.println("│          }                                                             │");
        System.out.println("│      }                                                                 │");
        System.out.println("│  }                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【5.4 反向代理配置】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  server {                                                              │");
        System.out.println("│      listen 80;                                                        │");
        System.out.println("│      server_name api.example.com;                                      │");
        System.out.println("│                                                                        │");
        System.out.println("│      location / {                                                      │");
        System.out.println("│          proxy_pass http://127.0.0.1:8080;  # 后端服务地址             │");
        System.out.println("│                                                                        │");
        System.out.println("│          # 传递请求头                                                   │");
        System.out.println("│          proxy_set_header Host $host;                                  │");
        System.out.println("│          proxy_set_header X-Real-IP $remote_addr;                      │");
        System.out.println("│          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;  │");
        System.out.println("│          proxy_set_header X-Forwarded-Proto $scheme;                   │");
        System.out.println("│                                                                        │");
        System.out.println("│          # 超时设置                                                     │");
        System.out.println("│          proxy_connect_timeout 30s;                                    │");
        System.out.println("│          proxy_send_timeout 60s;                                       │");
        System.out.println("│          proxy_read_timeout 60s;                                       │");
        System.out.println("│      }                                                                 │");
        System.out.println("│  }                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【5.5 负载均衡配置】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  # 定义后端服务器组                                                     │");
        System.out.println("│  upstream backend {                                                    │");
        System.out.println("│      # 负载均衡策略                                                     │");
        System.out.println("│      # 默认：轮询（round-robin）                                        │");
        System.out.println("│      # ip_hash;           # IP哈希（会话保持）                          │");
        System.out.println("│      # least_conn;        # 最少连接                                    │");
        System.out.println("│      # random;            # 随机                                        │");
        System.out.println("│                                                                        │");
        System.out.println("│      server 192.168.1.101:8080 weight=3;  # 权重                       │");
        System.out.println("│      server 192.168.1.102:8080 weight=2;                               │");
        System.out.println("│      server 192.168.1.103:8080 weight=1 backup;  # 备份服务器          │");
        System.out.println("│      server 192.168.1.104:8080 down;      # 标记为不可用               │");
        System.out.println("│                                                                        │");
        System.out.println("│      # 健康检查（商业版才有完整支持）                                   │");
        System.out.println("│      # 开源版可用第三方模块或被动检查                                   │");
        System.out.println("│  }                                                                     │");
        System.out.println("│                                                                        │");
        System.out.println("│  server {                                                              │");
        System.out.println("│      listen 80;                                                        │");
        System.out.println("│      server_name api.example.com;                                      │");
        System.out.println("│                                                                        │");
        System.out.println("│      location / {                                                      │");
        System.out.println("│          proxy_pass http://backend;  # 使用upstream                    │");
        System.out.println("│      }                                                                 │");
        System.out.println("│  }                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【5.6 负载均衡策略对比】\n");

        System.out.println("┌─────────────────┬────────────────────────────────────────────────────────┐");
        System.out.println("│ 策略             │ 说明                                                   │");
        System.out.println("├─────────────────┼────────────────────────────────────────────────────────┤");
        System.out.println("│ 轮询（默认）      │ 依次分配请求，适合服务器性能相近                       │");
        System.out.println("├─────────────────┼────────────────────────────────────────────────────────┤");
        System.out.println("│ weight          │ 按权重分配，权重大的分配更多请求                       │");
        System.out.println("├─────────────────┼────────────────────────────────────────────────────────┤");
        System.out.println("│ ip_hash         │ 按客户端IP哈希，同一IP访问同一服务器（会话保持）       │");
        System.out.println("├─────────────────┼────────────────────────────────────────────────────────┤");
        System.out.println("│ least_conn      │ 分配给连接数最少的服务器                               │");
        System.out.println("├─────────────────┼────────────────────────────────────────────────────────┤");
        System.out.println("│ url_hash        │ 按URL哈希，同一URL访问同一服务器（缓存友好）           │");
        System.out.println("└─────────────────┴────────────────────────────────────────────────────────┘\n");

        System.out.println("【5.7 location 匹配规则】\n");

        System.out.println("优先级从高到低：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  location = /api { }        # 精确匹配，优先级最高                      │");
        System.out.println("│  location ^~ /static/ { }   # 前缀匹配，匹配后停止搜索正则              │");
        System.out.println("│  location ~ \\.php$ { }      # 正则匹配（区分大小写）                   │");
        System.out.println("│  location ~* \\.(jpg|png)$ { }  # 正则匹配（不区分大小写）              │");
        System.out.println("│  location /api { }          # 前缀匹配                                  │");
        System.out.println("│  location / { }             # 默认匹配                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第六部分：高频面试题 ====================

    private static void printInterviewQuestions() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第六部分：高频面试题                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        // Linux 面试题
        System.out.println("==================== Linux 面试题 ====================\n");

        System.out.println("【问题1】如何查看某个端口被哪个进程占用？");
        System.out.println("答：");
        System.out.println("1. netstat -tlnp | grep 8080");
        System.out.println("2. ss -tlnp | grep 8080");
        System.out.println("3. lsof -i:8080\n");

        System.out.println("【问题2】chmod 755 是什么意思？");
        System.out.println("答：");
        System.out.println("755 = rwxr-xr-x");
        System.out.println("- 7(rwx): 所有者可读、可写、可执行");
        System.out.println("- 5(r-x): 所属组可读、可执行");
        System.out.println("- 5(r-x): 其他用户可读、可执行\n");

        System.out.println("【问题3】如何实时查看日志文件？");
        System.out.println("答：");
        System.out.println("tail -f /var/log/app.log");
        System.out.println("# 或查看最后100行并持续追踪");
        System.out.println("tail -n 100 -f /var/log/app.log\n");

        System.out.println("【问题4】kill 和 kill -9 的区别？");
        System.out.println("答：");
        System.out.println("- kill PID: 发送 SIGTERM 信号，进程可以捕获并优雅退出");
        System.out.println("- kill -9 PID: 发送 SIGKILL 信号，强制终止，无法被捕获\n");

        System.out.println("【问题5】如何让程序后台运行，且关闭终端后继续运行？");
        System.out.println("答：");
        System.out.println("nohup java -jar app.jar > app.log 2>&1 &");
        System.out.println("# nohup: 忽略挂断信号");
        System.out.println("# > app.log: 输出重定向");
        System.out.println("# 2>&1: 错误输出重定向到标准输出");
        System.out.println("# &: 后台运行\n");

        // Shell 面试题
        System.out.println("==================== Shell 面试题 ====================\n");

        System.out.println("【问题6】$? 表示什么？");
        System.out.println("答：");
        System.out.println("上一条命令的返回值（退出状态码）");
        System.out.println("- 0: 成功");
        System.out.println("- 非0: 失败\n");

        System.out.println("【问题7】单引号和双引号的区别？");
        System.out.println("答：");
        System.out.println("- 单引号: 原样输出，不解析变量");
        System.out.println("  echo '$HOME' → 输出 $HOME");
        System.out.println("- 双引号: 解析变量");
        System.out.println("  echo \"$HOME\" → 输出 /home/user\n");

        System.out.println("【问题8】如何在Shell中判断文件是否存在？");
        System.out.println("答：");
        System.out.println("if [ -f \"/path/to/file\" ]; then");
        System.out.println("    echo \"文件存在\"");
        System.out.println("fi");
        System.out.println("# -f: 是否是普通文件");
        System.out.println("# -d: 是否是目录");
        System.out.println("# -e: 路径是否存在\n");

        // Docker 面试题
        System.out.println("==================== Docker 面试题 ====================\n");

        System.out.println("【问题9】Docker镜像和容器的区别？");
        System.out.println("答：");
        System.out.println("- 镜像: 只读模板，包含运行应用所需的文件系统和配置");
        System.out.println("- 容器: 镜像的运行实例，有独立的可写层");
        System.out.println("- 类比: 镜像是类，容器是对象\n");

        System.out.println("【问题10】COPY和ADD的区别？");
        System.out.println("答：");
        System.out.println("- COPY: 只复制本地文件到镜像");
        System.out.println("- ADD: 除了复制，还支持：");
        System.out.println("  1. 自动解压tar包");
        System.out.println("  2. 从URL下载文件");
        System.out.println("推荐使用COPY，更明确\n");

        System.out.println("【问题11】CMD和ENTRYPOINT的区别？");
        System.out.println("答：");
        System.out.println("- CMD: 容器启动时的默认命令，可被docker run参数覆盖");
        System.out.println("- ENTRYPOINT: 固定的执行程序，docker run参数作为参数传递");
        System.out.println("最佳实践: ENTRYPOINT定义可执行程序，CMD定义默认参数\n");

        System.out.println("【问题12】如何减小Docker镜像体积？");
        System.out.println("答：");
        System.out.println("1. 使用更小的基础镜像（alpine）");
        System.out.println("2. 多阶段构建（multi-stage build）");
        System.out.println("3. 合并RUN命令，减少层数");
        System.out.println("4. 清理不必要的文件（apt clean）");
        System.out.println("5. 使用.dockerignore排除不需要的文件\n");

        System.out.println("【问题13】Docker网络模式有哪些？");
        System.out.println("答：");
        System.out.println("- bridge（默认）: 虚拟网桥，容器通过NAT访问外网");
        System.out.println("- host: 使用主机网络，性能最好但无隔离");
        System.out.println("- none: 无网络");
        System.out.println("- container: 共享其他容器的网络\n");

        // Jenkins 面试题
        System.out.println("==================== Jenkins 面试题 ====================\n");

        System.out.println("【问题14】什么是CI/CD？");
        System.out.println("答：");
        System.out.println("CI（持续集成）：频繁合并代码，自动构建、测试，快速发现问题");
        System.out.println("CD（持续交付）：自动化发布到测试/预发环境");
        System.out.println("CD（持续部署）：自动化发布到生产环境\n");

        System.out.println("【问题15】Jenkins Pipeline有哪两种语法？");
        System.out.println("答：");
        System.out.println("1. 声明式Pipeline（推荐）：结构化语法，更易读");
        System.out.println("   pipeline { stages { stage { steps { } } } }");
        System.out.println("2. 脚本式Pipeline：更灵活，用Groovy语法");
        System.out.println("   node { stage { } }\n");

        System.out.println("【问题16】Jenkins如何触发构建？");
        System.out.println("答：");
        System.out.println("1. 手动触发");
        System.out.println("2. 定时触发（Cron表达式）");
        System.out.println("3. Git Webhook（代码推送时）");
        System.out.println("4. 轮询SCM");
        System.out.println("5. 其他Job触发\n");

        // Nginx 面试题
        System.out.println("==================== Nginx 面试题 ====================\n");

        System.out.println("【问题17】正向代理和反向代理的区别？");
        System.out.println("答：");
        System.out.println("- 正向代理：代理客户端，服务端不知道真实客户端（如VPN）");
        System.out.println("- 反向代理：代理服务端，客户端不知道真实服务端（如Nginx负载均衡）\n");

        System.out.println("【问题18】Nginx负载均衡有哪些策略？");
        System.out.println("答：");
        System.out.println("1. 轮询（默认）：依次分配");
        System.out.println("2. weight：按权重分配");
        System.out.println("3. ip_hash：按IP哈希，实现会话保持");
        System.out.println("4. least_conn：最少连接数");
        System.out.println("5. url_hash：按URL哈希\n");

        System.out.println("【问题19】Nginx location匹配优先级？");
        System.out.println("答（从高到低）：");
        System.out.println("1. = 精确匹配");
        System.out.println("2. ^~ 前缀匹配，匹配后不再搜索正则");
        System.out.println("3. ~ 正则匹配（区分大小写）");
        System.out.println("4. ~* 正则匹配（不区分大小写）");
        System.out.println("5. 普通前缀匹配");
        System.out.println("6. / 默认匹配\n");

        System.out.println("【问题20】如何实现Nginx热更新配置？");
        System.out.println("答：");
        System.out.println("nginx -t          # 先测试配置语法");
        System.out.println("nginx -s reload   # 平滑重载配置");
        System.out.println("# 不会中断正在处理的请求\n");

        System.out.println("==========================================================================\n");
    }
}
