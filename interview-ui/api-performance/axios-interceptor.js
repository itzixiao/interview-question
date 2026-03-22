/**
 * Axios 拦截器与 API 封装
 *
 * 功能模块：
 * 1. 请求拦截器 - 统一处理请求配置
 * 2. 响应拦截器 - 统一处理响应和错误
 * 3. 请求取消 - 防止重复请求
 * 4. 请求重试 - 网络错误自动重试
 * 5. 缓存机制 - GET 请求缓存
 *
 * 对应文档：docs/18-前端开发/06-前后端联调最佳实践.md
 */

import axios from 'axios';
import {message} from 'antd'; // 或使用其他 UI 库

// ==================== 1. 基础配置 ====================

/**
 * 创建 Axios 实例
 * 使用实例而不是全局 axios，避免配置污染
 */
const apiClient = axios.create({
    // API 基础 URL
    baseURL: process.env.REACT_APP_API_BASE_URL || '/api',

    // 请求超时时间（毫秒）
    timeout: 10000,

    // 请求头配置
    headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    },

    // 跨域请求是否携带 cookie
    withCredentials: true
});

// ==================== 2. 请求拦截器 ====================

/**
 * 请求拦截器
 * 在请求发送前统一处理请求配置
 */
apiClient.interceptors.request.use(
    (config) => {
        // 1. 添加认证令牌
        const token = localStorage.getItem('access_token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        // 2. 添加时间戳防止缓存（针对 GET 请求）
        if (config.method === 'get' && config.cache !== true) {
            config.params = {
                ...config.params,
                _t: Date.now()
            };
        }

        // 3. 显示加载状态（可配置）
        if (config.showLoading !== false) {
            // 显示全局 loading
            window.dispatchEvent(new CustomEvent('showLoading'));
        }

        // 4. 请求日志（开发环境）
        if (process.env.NODE_ENV === 'development') {
            console.log(`[Request] ${config.method?.toUpperCase()} ${config.url}`, config);
        }

        return config;
    },
    (error) => {
        // 请求发送失败
        console.error('[Request Error]', error);
        return Promise.reject(error);
    }
);

// ==================== 3. 响应拦截器 ====================

/**
 * 响应拦截器
 * 统一处理响应数据和错误
 */
apiClient.interceptors.response.use(
    (response) => {
        // 1. 隐藏加载状态
        if (response.config.showLoading !== false) {
            window.dispatchEvent(new CustomEvent('hideLoading'));
        }

        // 2. 响应日志（开发环境）
        if (process.env.NODE_ENV === 'development') {
            console.log(`[Response] ${response.config.url}`, response.data);
        }

        // 3. 处理标准响应格式
        const {code, data, message: msg} = response.data;

        // 假设后端返回格式：{ code: 0, data: ..., message: '...' }
        if (code === 0 || code === 200) {
            return data;
        }

        // 业务错误
        return Promise.reject(new Error(msg || '请求失败'));
    },
    (error) => {
        // 隐藏加载状态
        if (error.config?.showLoading !== false) {
            window.dispatchEvent(new CustomEvent('hideLoading'));
        }

        // 处理不同类型的错误
        return handleApiError(error);
    }
);

/**
 * 统一错误处理函数
 * @param {Error} error - Axios 错误对象
 */
function handleApiError(error) {
    // 1. 请求取消
    if (axios.isCancel(error)) {
        console.log('[Request Cancelled]', error.message);
        return Promise.reject(error);
    }

    // 2. 网络错误
    if (!error.response) {
        message.error('网络连接失败，请检查网络');
        return Promise.reject(new Error('网络连接失败'));
    }

    // 3. 根据状态码处理
    const {status, data} = error.response;

    switch (status) {
        case 400:
            message.error(data.message || '请求参数错误');
            break;
        case 401:
            // 未授权，清除 token 并跳转登录
            localStorage.removeItem('access_token');
            message.error('登录已过期，请重新登录');
            window.location.href = '/login';
            break;
        case 403:
            message.error('没有权限执行此操作');
            break;
        case 404:
            message.error('请求的资源不存在');
            break;
        case 500:
            message.error('服务器内部错误');
            break;
        case 502:
        case 503:
            message.error('服务暂时不可用');
            break;
        default:
            message.error(data.message || '请求失败');
    }

    return Promise.reject(error);
}

// ==================== 4. 请求取消机制 ====================

/**
 * 请求取消管理器
 * 用于管理正在进行的请求，防止重复请求
 */
class RequestCancelManager {
    constructor() {
        // 存储正在进行的请求
        // key: 请求标识, value: CancelToken
        this.pendingRequests = new Map();
    }

    /**
     * 生成请求标识
     * @param {Object} config - Axios 请求配置
     * @returns {string} 请求标识
     */
    generateKey(config) {
        const {method, url, params, data} = config;
        return `${method}_${url}_${JSON.stringify(params)}_${JSON.stringify(data)}`;
    }

    /**
     * 添加请求
     * @param {Object} config - Axios 请求配置
     */
    add(config) {
        const key = this.generateKey(config);

        // 如果存在相同的请求，取消之前的
        this.remove(key);

        // 创建新的取消令牌
        const source = axios.CancelToken.source();
        config.cancelToken = source.token;
        this.pendingRequests.set(key, source);
    }

    /**
     * 移除请求
     * @param {string} key - 请求标识
     */
    remove(key) {
        if (this.pendingRequests.has(key)) {
            const source = this.pendingRequests.get(key);
            source.cancel('取消重复请求');
            this.pendingRequests.delete(key);
        }
    }

    /**
     * 根据配置移除请求
     * @param {Object} config - Axios 请求配置
     */
    removeByConfig(config) {
        const key = this.generateKey(config);
        this.remove(key);
    }

    /**
     * 清空所有请求
     */
    clear() {
        this.pendingRequests.forEach(source => {
            source.cancel('清空所有请求');
        });
        this.pendingRequests.clear();
    }
}

const cancelManager = new RequestCancelManager();

// 添加请求拦截器，自动处理请求取消
apiClient.interceptors.request.use((config) => {
    // 如果配置中启用了取消重复请求
    if (config.cancelDuplicate !== false) {
        cancelManager.add(config);
    }
    return config;
});

// 添加响应拦截器，请求完成后移除
apiClient.interceptors.response.use(
    (response) => {
        cancelManager.removeByConfig(response.config);
        return response;
    },
    (error) => {
        if (error.config) {
            cancelManager.removeByConfig(error.config);
        }
        return Promise.reject(error);
    }
);

// ==================== 5. 请求重试机制 ====================

/**
 * 请求重试配置
 */
const retryConfig = {
    retries: 3,              // 最大重试次数
    retryDelay: 1000,        // 重试延迟（毫秒）
    retryCondition: (error) => {
        // 只在网络错误或 5xx 错误时重试
        return !error.response || error.response.status >= 500;
    }
};

/**
 * 带重试的请求
 * @param {Object} config - Axios 请求配置
 * @param {Object} options - 重试配置
 */
async function requestWithRetry(config, options = retryConfig) {
    const {retries, retryDelay, retryCondition} = options;
    let lastError;

    for (let i = 0; i <= retries; i++) {
        try {
            return await apiClient(config);
        } catch (error) {
            lastError = error;

            // 最后一次尝试失败，抛出错误
            if (i === retries) {
                throw error;
            }

            // 检查是否应该重试
            if (retryCondition(error)) {
                console.log(`请求失败，${retryDelay}ms 后第 ${i + 1} 次重试...`);
                await delay(retryDelay);
            } else {
                // 不符合重试条件，直接抛出
                throw error;
            }
        }
    }

    throw lastError;
}

/**
 * 延迟函数
 * @param {number} ms - 毫秒
 */
function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// ==================== 6. 缓存机制 ====================

/**
 * 简单的内存缓存
 */
class MemoryCache {
    constructor() {
        this.cache = new Map();
    }

    /**
     * 生成缓存键
     */
    generateKey(config) {
        return `${config.url}_${JSON.stringify(config.params)}`;
    }

    /**
     * 获取缓存
     */
    get(key) {
        const item = this.cache.get(key);
        if (!item) return null;

        // 检查是否过期
        if (Date.now() > item.expireTime) {
            this.cache.delete(key);
            return null;
        }

        return item.data;
    }

    /**
     * 设置缓存
     */
    set(key, data, ttl = 60000) {  // 默认缓存 1 分钟
        this.cache.set(key, {
            data,
            expireTime: Date.now() + ttl
        });
    }

    /**
     * 清除缓存
     */
    clear() {
        this.cache.clear();
    }
}

const memoryCache = new MemoryCache();

/**
 * 带缓存的 GET 请求
 * @param {string} url - 请求地址
 * @param {Object} params - 查询参数
 * @param {Object} options - 配置选项
 */
async function cachedGet(url, params = {}, options = {}) {
    const {cache = true, ttl = 60000, ...axiosOptions} = options;

    const config = {
        method: 'get',
        url,
        params,
        ...axiosOptions
    };

    // 如果启用缓存
    if (cache) {
        const cacheKey = memoryCache.generateKey(config);
        const cachedData = memoryCache.get(cacheKey);

        if (cachedData) {
            console.log('[Cache Hit]', url);
            return cachedData;
        }

        // 请求数据并缓存
        const data = await apiClient(config);
        memoryCache.set(cacheKey, data, ttl);
        return data;
    }

    return apiClient(config);
}

// ==================== 7. API 封装 ====================

/**
 * 统一的 API 请求方法
 */
const api = {
    /**
     * GET 请求
     */
    get(url, params, config = {}) {
        return apiClient.get(url, {params, ...config});
    },

    /**
     * 带缓存的 GET 请求
     */
    getCached(url, params, options) {
        return cachedGet(url, params, options);
    },

    /**
     * POST 请求
     */
    post(url, data, config = {}) {
        return apiClient.post(url, data, config);
    },

    /**
     * PUT 请求
     */
    put(url, data, config = {}) {
        return apiClient.put(url, data, config);
    },

    /**
     * DELETE 请求
     */
    delete(url, config = {}) {
        return apiClient.delete(url, config);
    },

    /**
     * 上传文件
     */
    upload(url, file, onProgress) {
        const formData = new FormData();
        formData.append('file', file);

        return apiClient.post(url, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            },
            onUploadProgress: (progressEvent) => {
                if (onProgress && progressEvent.total) {
                    const percent = Math.round(
                        (progressEvent.loaded * 100) / progressEvent.total
                    );
                    onProgress(percent);
                }
            }
        });
    },

    /**
     * 带重试的请求
     */
    requestWithRetry(config, options) {
        return requestWithRetry(config, options);
    },

    /**
     * 取消所有请求
     */
    cancelAll() {
        cancelManager.clear();
    },

    /**
     * 清除缓存
     */
    clearCache() {
        memoryCache.clear();
    }
};

// ==================== 8. 使用示例 ====================

// 基础使用
async function fetchUserList() {
    try {
        const users = await api.get('/users', {page: 1, size: 10});
        console.log('用户列表：', users);
    } catch (error) {
        console.error('获取失败：', error);
    }
}

// 带缓存的请求
async function fetchUserDetail(userId) {
    const user = await api.getCached(`/users/${userId}`, {}, {
        cache: true,
        ttl: 300000  // 缓存 5 分钟
    });
    return user;
}

// 上传文件
async function uploadAvatar(file) {
    const result = await api.upload('/upload/avatar', file, (percent) => {
        console.log(`上传进度：${percent}%`);
    });
    return result;
}

// 导出
export {api, apiClient, cancelManager, memoryCache};
export default api;
