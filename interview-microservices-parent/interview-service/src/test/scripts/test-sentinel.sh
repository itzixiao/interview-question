#!/bin/bash
# ============================================================
# Sentinel 接口测试脚本
# 使用前请先启动应用：http://localhost:8081
# 运行方式：bash test-sentinel.sh
# ============================================================

BASE_URL="http://localhost:8081"
PASS=0
FAIL=0

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

print_header() {
    echo ""
    echo -e "${CYAN}============================================${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}============================================${NC}"
}

invoke_api() {
    local name="$1"
    local url="$2"
    local result
    local http_code

    http_code=$(curl -s -o /tmp/api_body.txt -w "%{http_code}" --connect-timeout 5 --max-time 10 "$url")
    result=$(cat /tmp/api_body.txt)

    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "  ${GREEN}[PASS] $name${NC}"
        echo -e "  ${GRAY}       状态码: $http_code${NC}"
        echo -e "  ${GRAY}       响应: $result${NC}"
        PASS=$((PASS + 1))
    else
        echo -e "  ${RED}[FAIL] $name${NC}"
        echo -e "  ${GRAY}       状态码: $http_code${NC}"
        echo -e "  ${GRAY}       响应: $result${NC}"
        FAIL=$((FAIL + 1))
    fi
}

# ============================================================
# 1. 基础接口检查
# ============================================================
print_header "1. 基础接口检查"

invoke_api "服务健康检查 /interview/health"  "$BASE_URL/interview/health"
invoke_api "服务信息 /interview/info"         "$BASE_URL/interview/info"
invoke_api "Sentinel 信息 /sentinel/info"     "$BASE_URL/sentinel/info"

# ============================================================
# 2. 简单限流测试（sentinelHello）
# 请在 Sentinel 控制台配置：资源名=sentinelHello，QPS=2
# ============================================================
print_header "2. 简单限流测试 /sentinel/hello"
echo -e "${YELLOW}  提示：在控制台配置 sentinelHello QPS=2 后，第3次请求应被限流${NC}"

for i in $(seq 1 5); do
    http_code=$(curl -s -o /tmp/api_body.txt -w "%{http_code}" --connect-timeout 3 --max-time 5 "$BASE_URL/sentinel/hello")
    body=$(cat /tmp/api_body.txt)
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "  ${GREEN}第${i}次请求 -> 状态:${http_code} | ${body}${NC}"
    else
        echo -e "  ${RED}第${i}次请求 -> 状态:${http_code} | ${body}${NC}"
    fi
    sleep 0.1
done

# ============================================================
# 3. 热点参数限流测试（sentinelHot）
# 请在 Sentinel 控制台配置热点规则：资源名=sentinelHot，参数索引=0，QPS=2
# ============================================================
print_header "3. 热点参数限流测试 /sentinel/hot"
echo -e "${YELLOW}  测试 userId=vip001（热点用户）连续访问 4 次${NC}"

for i in $(seq 1 4); do
    http_code=$(curl -s -o /tmp/api_body.txt -w "%{http_code}" --connect-timeout 3 --max-time 5 \
        "$BASE_URL/sentinel/hot?userId=vip001&type=1")
    body=$(cat /tmp/api_body.txt)
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "  ${GREEN}第${i}次请求(vip001) -> 状态:${http_code} | ${body}${NC}"
    else
        echo -e "  ${RED}第${i}次请求(vip001) -> 状态:${http_code} | ${body}${NC}"
    fi
    sleep 0.1
done

echo ""
echo -e "${YELLOW}  测试 userId=normal001（普通用户）访问 1 次${NC}"
http_code=$(curl -s -o /tmp/api_body.txt -w "%{http_code}" --connect-timeout 3 --max-time 5 \
    "$BASE_URL/sentinel/hot?userId=normal001&type=1")
body=$(cat /tmp/api_body.txt)
if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
    echo -e "  ${GREEN}普通用户请求 -> 状态:${http_code} | ${body}${NC}"
else
    echo -e "  ${RED}普通用户请求 -> 状态:${http_code} | ${body}${NC}"
fi

# ============================================================
# 4. 慢调用降级测试（sentinelSlow）
# 请在 Sentinel 控制台配置降级规则：资源名=sentinelSlow，最大RT=100ms，比例=0.5，最小请求数=5
# ============================================================
print_header "4. 慢调用降级测试 /sentinel/slow（接口延迟500ms）"
echo -e "${YELLOW}  配置降级规则后，连续请求将触发熔断${NC}"

for i in $(seq 1 6); do
    start_ms=$(date +%s%3N)
    http_code=$(curl -s -o /tmp/api_body.txt -w "%{http_code}" --connect-timeout 5 --max-time 15 \
        "$BASE_URL/sentinel/slow")
    end_ms=$(date +%s%3N)
    elapsed=$((end_ms - start_ms))
    body=$(cat /tmp/api_body.txt)
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "  ${GREEN}第${i}次请求 -> 状态:${http_code} | 耗时:${elapsed}ms | ${body}${NC}"
    else
        echo -e "  ${RED}第${i}次请求 -> 状态:${http_code} | 耗时:${elapsed}ms | ${body}${NC}"
    fi
done

# ============================================================
# 5. 异常比例降级测试（sentinelError）
# 请在 Sentinel 控制台配置降级规则：资源名=sentinelError，策略=异常比例，比例=0.5，最小请求数=5
# ============================================================
print_header "5. 异常比例降级测试 /sentinel/error"
echo -e "${YELLOW}  前3次正常，后5次触发异常，观察降级效果${NC}"

echo ""
echo -e "${WHITE}  -- 正常请求 --${NC}"
for i in $(seq 1 3); do
    http_code=$(curl -s -o /tmp/api_body.txt -w "%{http_code}" --connect-timeout 3 --max-time 5 \
        "$BASE_URL/sentinel/error?fail=false")
    body=$(cat /tmp/api_body.txt)
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "  ${GREEN}第${i}次(正常) -> ${body}${NC}"
    else
        echo -e "  ${RED}第${i}次(正常) -> 状态:${http_code}${NC}"
    fi
done

echo ""
echo -e "${WHITE}  -- 异常请求 --${NC}"
for i in $(seq 1 5); do
    http_code=$(curl -s -o /tmp/api_body.txt -w "%{http_code}" --connect-timeout 3 --max-time 5 \
        "$BASE_URL/sentinel/error?fail=true")
    body=$(cat /tmp/api_body.txt)
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "  ${GREEN}第${i}次(异常) -> ${body}${NC}"
    else
        echo -e "  ${RED}第${i}次(异常) -> 状态:${http_code} | ${body}${NC}"
    fi
done

echo ""
echo -e "${WHITE}  -- 熔断后请求（等待1s后） --${NC}"
sleep 1
for i in $(seq 1 2); do
    http_code=$(curl -s -o /tmp/api_body.txt -w "%{http_code}" --connect-timeout 3 --max-time 5 \
        "$BASE_URL/sentinel/error?fail=false")
    body=$(cat /tmp/api_body.txt)
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "  ${GREEN}熔断后第${i}次 -> ${body}${NC}"
    else
        echo -e "  ${RED}熔断后第${i}次 -> 状态:${http_code} | ${body}${NC}"
    fi
done

# ============================================================
# 6. 面试接口限流测试（每个接口请求5次）
# ============================================================
print_header "6. 面试题接口限流测试（每个接口请求5次）"

repeat_api() {
    local name="$1"
    local url="$2"
    local times=5
    echo -e "  ${CYAN}--- $name ---${NC}"
    echo -e "  ${YELLOW}  提示：配置 QPS=2 的流控规则后，第3次起应被限流${NC}"
    for i in $(seq 1 $times); do
        http_code=$(curl -s -o /tmp/api_body.txt -w "%{http_code}" --connect-timeout 5 --max-time 10 "$url")
        body=$(cat /tmp/api_body.txt)
        if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
            echo -e "  ${GREEN}第${i}次 -> 状态:${http_code} | ${body}${NC}"
            PASS=$((PASS + 1))
        else
            echo -e "  ${RED}第${i}次 -> 状态:${http_code} | ${body}${NC}"
            FAIL=$((FAIL + 1))
        fi
        # 无间隔连续请求，确保在1秒统计窗口内超过QPS阈值
    done
    echo ""
}

repeat_api "获取面试题列表"           "$BASE_URL/interview/questions"
repeat_api "获取面试题列表(带分类)"   "$BASE_URL/interview/questions?category=java"
repeat_api "获取面试题详情"           "$BASE_URL/interview/question/1"

# ============================================================
# 测试结果汇总
# ============================================================
print_header "测试结果汇总"
echo -e "  ${GREEN}通过: $PASS${NC}"
echo -e "  ${RED}失败: $FAIL${NC}"
echo ""
echo -e "${YELLOW}  提示：${NC}"
echo -e "${YELLOW}  1. 限流相关测试需要先在 Sentinel 控制台配置规则${NC}"
echo -e "${YELLOW}  2. 控制台地址：http://localhost:8858${NC}"
echo -e "${YELLOW}  3. 账号密码：sentinel / sentinel${NC}"
echo ""

# 清理临时文件
rm -f /tmp/api_body.txt
