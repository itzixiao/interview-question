#!/bin/bash
# ============================================================
# Gateway 接口测试脚本
# 使用前请先启动：
#   - interview-gateway  http://localhost:8080
#   - interview-service  http://localhost:8081
# 运行方式：bash test-gateway.sh
# ============================================================

GATEWAY_URL="http://localhost:8080"
SERVICE_URL="http://localhost:8081"
PASS=0
FAIL=0

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
WHITE='\033[1;37m'
NC='\033[0m'

print_header() {
    echo ""
    echo -e "${CYAN}============================================${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}============================================${NC}"
}

# 基础请求（可选抺 Token）
invoke_api() {
    local name="$1"
    local url="$2"
    local token="$3"  # 为空则不带 Authorization 头
    local curl_opts="-s -o /tmp/gw_body.txt -w \"%{http_code}\" --connect-timeout 5 --max-time 10"

    if [ -n "$token" ]; then
        http_code=$(curl -s -o /tmp/gw_body.txt -w "%{http_code}" \
            --connect-timeout 5 --max-time 10 \
            -H "Authorization: Bearer ${token}" \
            "$url")
    else
        http_code=$(curl -s -o /tmp/gw_body.txt -w "%{http_code}" \
            --connect-timeout 5 --max-time 10 \
            "$url")
    fi
    body=$(cat /tmp/gw_body.txt)

    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "  ${GREEN}[PASS] $name${NC}"
        echo -e "  ${GRAY}       状态码: $http_code${NC}"
        echo -e "  ${GRAY}       响应: $body${NC}"
        PASS=$((PASS + 1))
    else
        echo -e "  ${RED}[FAIL] $name${NC}"
        echo -e "  ${GRAY}       状态码: $http_code${NC}"
        echo -e "  ${GRAY}       响应: $body${NC}"
        FAIL=$((FAIL + 1))
    fi
}

# 无 Token 请求（测试鉴权拦截）
invoke_no_auth() {
    local name="$1"
    local url="$2"
    local expect_code="${3:-401}"

    http_code=$(curl -s -o /tmp/gw_body.txt -w "%{http_code}" \
        --connect-timeout 5 --max-time 10 \
        "$url")
    body=$(cat /tmp/gw_body.txt)

    if [ "$http_code" -eq "$expect_code" ]; then
        echo -e "  ${GREEN}[PASS] $name（期望被拦截 $expect_code，实际 $http_code）${NC}"
        PASS=$((PASS + 1))
    else
        echo -e "  ${RED}[FAIL] $name（期望 $expect_code，实际 $http_code）${NC}"
        echo -e "  ${GRAY}       响应: $body${NC}"
        FAIL=$((FAIL + 1))
    fi
}

# ============================================================
# 1. 服务启动检查
# ============================================================
print_header "1. 服务启动检查"

echo -e "${YELLOW}  检查 Gateway（8080）是否启动...${NC}"
gw_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "$GATEWAY_URL/api/interview/health" \
    -H "Authorization: Bearer test-token")
if [ "$gw_code" != "000" ]; then
    echo -e "  ${GREEN}[PASS] Gateway 端口 8080 已响应${NC}"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}[FAIL] Gateway 未启动，请先启动 interview-gateway${NC}"
    FAIL=$((FAIL + 1))
    echo ""
    echo -e "${RED}  !! Gateway 未启动，终止测试 !!${NC}"
    exit 1
fi

echo -e "${YELLOW}  检查 Service（8081）是否启动...${NC}"
svc_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "$SERVICE_URL/interview/health")
if [ "$svc_code" -ge 200 ] && [ "$svc_code" -lt 300 ]; then
    echo -e "  ${GREEN}[PASS] Service 端口 8081 已启动，状态码: $svc_code${NC}"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}[FAIL] Service 未正常响应，状态码: $svc_code${NC}"
    FAIL=$((FAIL + 1))
fi

# ============================================================
# 2. 鉴权过滤器测试
# ============================================================
print_header "2. 鉴权过滤器测试（AuthGlobalFilter）"

echo -e "${YELLOW}  /api/interview/** 已加入白名单，无需 Token 可访问${NC}"
for path in "/api/interview/health" "/api/interview/questions" "/api/interview/info"; do
    http_code=$(curl -s -o /tmp/gw_body.txt -w "%{http_code}" --connect-timeout 5 --max-time 10 \
        "$GATEWAY_URL$path")
    body=$(cat /tmp/gw_body.txt)
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "  ${GREEN}[PASS] 无 Token 访问 $path 返回 $http_code${NC}"
        PASS=$((PASS + 1))
    else
        echo -e "  ${RED}[FAIL] 无 Token 访问 $path 返回 $http_code${NC}"
        echo -e "  ${GRAY}       响应: $body${NC}"
        FAIL=$((FAIL + 1))
    fi
done

echo ""
echo -e "${YELLOW}  非白名单路径（/api/other/）无鉴权 Token 且路由不存在，应返回 404${NC}"
invoke_no_auth "非白名单 /api/other/test 路由不存在" "$GATEWAY_URL/api/other/test" 404

# ============================================================
# 3. 路由转发测试（每个接口请求5次）
# ============================================================
print_header "3. 路由转发测试（Gateway → interview-service，每个接口请求5次）"
echo -e "${YELLOW}  路由规则: /api/interview/** → StripPrefix=1 → /interview/**（无需 Token）${NC}"

repeat_api() {
    local name="$1"
    local url="$2"
    local times=5
    echo -e "  ${CYAN}--- $name ---${NC}"
    for i in $(seq 1 $times); do
        http_code=$(curl -s -o /tmp/gw_body.txt -w "%{http_code}" --connect-timeout 5 --max-time 10 "$url")
        body=$(cat /tmp/gw_body.txt)
        if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
            echo -e "  ${GREEN}第${i}次 -> 状态:${http_code} | ${body}${NC}"
            PASS=$((PASS + 1))
        else
            echo -e "  ${RED}第${i}次 -> 状态:${http_code} | ${body}${NC}"
            FAIL=$((FAIL + 1))
        fi
    done
    echo ""
}

repeat_api "health检查 /api/interview/health"           "$GATEWAY_URL/api/interview/health"
repeat_api "服务信息 /api/interview/info"              "$GATEWAY_URL/api/interview/info"
repeat_api "面试题列表 /api/interview/questions"       "$GATEWAY_URL/api/interview/questions"
repeat_api "面试题分类 /api/interview/questions?category=java" \
    "$GATEWAY_URL/api/interview/questions?category=java"
repeat_api "面试题详情 /api/interview/question/1"        "$GATEWAY_URL/api/interview/question/1"

# ============================================================
# 4. 响应头检查（X-Response-From: Gateway）
# ============================================================
print_header "4. 响应头检查（AddResponseHeader 过滤器）"
echo -e "${YELLOW}  验证 Gateway 是否添加了 X-Response-From 响应头${NC}"

headers=$(curl -s -D - -o /dev/null --connect-timeout 5 --max-time 10 \
    "$GATEWAY_URL/api/interview/health")

if echo "$headers" | grep -qi "X-Response-From: Gateway"; then
    echo -e "  ${GREEN}[PASS] 响应头包含 X-Response-From: Gateway${NC}"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}[FAIL] 响应头未找到 X-Response-From: Gateway${NC}"
    echo -e "  ${GRAY}       实际响应头:${NC}"
    echo "$headers" | grep -v "^$" | while read line; do
        echo -e "  ${GRAY}         $line${NC}"
    done
    FAIL=$((FAIL + 1))
fi

# ============================================================
# 5. 不存在的路由（404）
# ============================================================
print_header "5. 不存在路由测试"
echo -e "${YELLOW}  请求未配置路由的路径，应返回 404${NC}"

invoke_no_auth "未匹配路由 /api/unknown/test" "$GATEWAY_URL/api/unknown/test" 404

# ============================================================
# 6. 路由转发性能（耗时统计）
# ============================================================
print_header "6. 路由转发耗时统计（连续5次）"
echo -e "${YELLOW}  对比直连 Service vs 经过 Gateway 的耗时${NC}"

echo ""
echo -e "${WHITE}  -- 直连 Service（8081）--${NC}"
for i in $(seq 1 5); do
    start_ms=$(date +%s%3N)
    curl -s -o /dev/null --connect-timeout 5 --max-time 10 "$SERVICE_URL/interview/health"
    end_ms=$(date +%s%3N)
    elapsed=$((end_ms - start_ms))
    echo -e "  ${GRAY}第${i}次直连耗时: ${elapsed}ms${NC}"
done

echo ""
echo -e "${WHITE}  -- 经过 Gateway（8080）--${NC}"
for i in $(seq 1 5); do
    start_ms=$(date +%s%3N)
    curl -s -o /dev/null --connect-timeout 5 --max-time 10 \
        -H "Authorization: Bearer test-token" \
        "$GATEWAY_URL/api/interview/health"
    end_ms=$(date +%s%3N)
    elapsed=$((end_ms - start_ms))
    echo -e "  ${GRAY}第${i}次经Gateway耗时: ${elapsed}ms${NC}"
done

# ============================================================
# 测试结果汇总
# ============================================================
print_header "测试结果汇总"
echo -e "  ${GREEN}通过: $PASS${NC}"
echo -e "  ${RED}失败: $FAIL${NC}"
echo ""
echo -e "${YELLOW}  说明：${NC}"
echo -e "${YELLOW}  1. Gateway 地址：$GATEWAY_URL${NC}"
echo -e "${YELLOW}  2. Service 地址：$SERVICE_URL${NC}"
echo -e "${YELLOW}  3. 路由规则：/api/interview/** → http://localhost:8081${NC}"
echo -e "${YELLOW}  4. 鉴权：请求头需携带 Authorization（登录/注册路径除外）${NC}"
echo ""

rm -f /tmp/gw_body.txt
