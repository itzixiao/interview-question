/**
 * 虚拟列表（Virtual List）实现
 *
 * 核心原理：
 * 1. 只渲染可视区域内的列表项，而非全部数据
 * 2. 通过计算滚动位置动态更新可视区域
 * 3. 使用 padding 或 transform 保持滚动条高度和位置
 *
 * 性能优势：
 * - 大幅减少 DOM 节点数量
 * - 降低内存占用
 * - 提升滚动性能
 *
 * 适用场景：
 * - 长列表（千条以上数据）
 * - 表格数据展示
 * - 聊天记录、日志展示
 *
 * 对应文档：docs/18-前端开发/07-前端性能优化全链路.md
 */

import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import PropTypes from 'prop-types';

/**
 * 固定高度虚拟列表
 *
 * @param {Array} items - 数据列表
 * @param {number} itemHeight - 每项高度（像素）
 * @param {number} containerHeight - 容器高度（像素）
 * @param {Function} renderItem - 渲染每项的函数
 */
function FixedSizeVirtualList({
                                  items,
                                  itemHeight = 50,
                                  containerHeight = 400,
                                  renderItem
                              }) {
    // 列表容器的 ref
    const containerRef = useRef(null);

    // 滚动位置
    const [scrollTop, setScrollTop] = useState(0);

    // 计算总高度
    const totalHeight = items.length * itemHeight;

    // 计算可视区域能显示的项目数（多渲染一些作为缓冲）
    const visibleCount = Math.ceil(containerHeight / itemHeight);
    const bufferCount = 3;  // 上下各多渲染 3 项

    // 计算起始索引
    const startIndex = Math.max(0, Math.floor(scrollTop / itemHeight) - bufferCount);

    // 计算结束索引
    const endIndex = Math.min(
        items.length,
        Math.ceil((scrollTop + containerHeight) / itemHeight) + bufferCount
    );

    // 计算偏移量
    const offsetY = startIndex * itemHeight;

    // 可视区域的数据
    const visibleItems = useMemo(() => {
        return items.slice(startIndex, endIndex).map((item, index) => ({
            ...item,
            _index: startIndex + index  // 保留原始索引
        }));
    }, [items, startIndex, endIndex]);

    // 滚动事件处理
    const handleScroll = useCallback((e) => {
        setScrollTop(e.target.scrollTop);
    }, []);

    // 使用 requestAnimationFrame 优化滚动性能
    useEffect(() => {
        const container = containerRef.current;
        if (!container) return;

        let ticking = false;

        const onScroll = (e) => {
            if (!ticking) {
                window.requestAnimationFrame(() => {
                    handleScroll(e);
                    ticking = false;
                });
                ticking = true;
            }
        };

        container.addEventListener('scroll', onScroll);
        return () => container.removeEventListener('scroll', onScroll);
    }, [handleScroll]);

    return (
        <div
            ref={containerRef}
            style={{
                height: containerHeight,
                overflow: 'auto',
                position: 'relative',
                border: '1px solid #ddd'
            }}
        >
            {/* 占位元素，用于撑开滚动条 */}
            <div style={{height: totalHeight, position: 'relative'}}>
                {/* 可视区域内容 */}
                <div
                    style={{
                        position: 'absolute',
                        top: offsetY,
                        left: 0,
                        right: 0
                    }}
                >
                    {visibleItems.map((item) => (
                        <div
                            key={item._index}
                            style={{
                                height: itemHeight,
                                boxSizing: 'border-box',
                                borderBottom: '1px solid #eee'
                            }}
                        >
                            {renderItem(item, item._index)}
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}

FixedSizeVirtualList.propTypes = {
    items: PropTypes.array.isRequired,
    itemHeight: PropTypes.number,
    containerHeight: PropTypes.number,
    renderItem: PropTypes.func.isRequired
};

/**
 * 不定高度虚拟列表
 *
 * 适用于每项高度不固定的场景（如聊天消息、评论列表）
 */
function VariableSizeVirtualList({
                                     items,
                                     containerHeight = 400,
                                     estimateItemHeight = 50,  // 预估每项高度
                                     renderItem,
                                     getItemKey  // 获取每项唯一 key 的函数
                                 }) {
    const containerRef = useRef(null);
    const [scrollTop, setScrollTop] = useState(0);

    // 缓存每项的高度和位置
    const [measurements, setMeasurements] = useState([]);
    const itemElementsRef = useRef(new Map());

    // 计算总高度
    const totalHeight = useMemo(() => {
        if (measurements.length === 0) {
            return items.length * estimateItemHeight;
        }
        const lastMeasurement = measurements[measurements.length - 1];
        return lastMeasurement.top + lastMeasurement.height;
    }, [measurements, items.length, estimateItemHeight]);

    // 查找起始索引
    const startIndex = useMemo(() => {
        return findStartIndex(measurements, scrollTop);
    }, [measurements, scrollTop]);

    // 查找结束索引
    const endIndex = useMemo(() => {
        return findEndIndex(measurements, scrollTop + containerHeight);
    }, [measurements, scrollTop, containerHeight]);

    // 测量每项高度
    const measureElement = useCallback((index, element) => {
        if (!element) return;

        const height = element.getBoundingClientRect().height;

        setMeasurements(prev => {
            // 如果高度没有变化，不更新
            if (prev[index] && prev[index].height === height) {
                return prev;
            }

            const newMeasurements = [...prev];
            newMeasurements[index] = {
                height,
                top: index === 0 ? 0 : newMeasurements[index - 1].top + newMeasurements[index - 1].height
            };

            // 更新后续项的位置
            for (let i = index + 1; i < items.length; i++) {
                if (newMeasurements[i]) {
                    newMeasurements[i] = {
                        ...newMeasurements[i],
                        top: newMeasurements[i - 1].top + newMeasurements[i - 1].height
                    };
                }
            }

            return newMeasurements;
        });
    }, [items.length]);

    // 可视区域数据
    const visibleItems = useMemo(() => {
        const start = Math.max(0, startIndex - 3);
        const end = Math.min(items.length, endIndex + 3);

        return items.slice(start, end).map((item, idx) => ({
            ...item,
            _index: start + idx,
            _style: measurements[start + idx] ? {
                position: 'absolute',
                top: measurements[start + idx].top,
                left: 0,
                right: 0
            } : {
                position: 'absolute',
                top: (start + idx) * estimateItemHeight,
                left: 0,
                right: 0
            }
        }));
    }, [items, startIndex, endIndex, measurements, estimateItemHeight]);

    // 滚动处理
    const handleScroll = useCallback((e) => {
        setScrollTop(e.target.scrollTop);
    }, []);

    return (
        <div
            ref={containerRef}
            style={{
                height: containerHeight,
                overflow: 'auto',
                position: 'relative',
                border: '1px solid #ddd'
            }}
            onScroll={handleScroll}
        >
            <div style={{height: totalHeight, position: 'relative'}}>
                {visibleItems.map((item) => (
                    <div
                        key={getItemKey ? getItemKey(item) : item._index}
                        ref={el => {
                            if (el) {
                                itemElementsRef.current.set(item._index, el);
                                measureElement(item._index, el);
                            }
                        }}
                        style={item._style}
                    >
                        {renderItem(item, item._index)}
                    </div>
                ))}
            </div>
        </div>
    );
}

/**
 * 二分查找起始索引
 */
function findStartIndex(measurements, scrollTop) {
    if (measurements.length === 0) return 0;

    let start = 0;
    let end = measurements.length - 1;

    while (start <= end) {
        const mid = Math.floor((start + end) / 2);
        const measurement = measurements[mid];

        if (measurement.top <= scrollTop) {
            if (mid === measurements.length - 1 || measurements[mid + 1].top > scrollTop) {
                return mid;
            }
            start = mid + 1;
        } else {
            end = mid - 1;
        }
    }

    return 0;
}

/**
 * 查找结束索引
 */
function findEndIndex(measurements, scrollBottom) {
    if (measurements.length === 0) return 0;

    let start = 0;
    let end = measurements.length - 1;

    while (start <= end) {
        const mid = Math.floor((start + end) / 2);
        const measurement = measurements[mid];

        if (measurement.top + measurement.height >= scrollBottom) {
            if (mid === 0 || measurements[mid - 1].top + measurements[mid - 1].height < scrollBottom) {
                return mid;
            }
            end = mid - 1;
        } else {
            start = mid + 1;
        }
    }

    return measurements.length - 1;
}

VariableSizeVirtualList.propTypes = {
    items: PropTypes.array.isRequired,
    containerHeight: PropTypes.number,
    estimateItemHeight: PropTypes.number,
    renderItem: PropTypes.func.isRequired,
    getItemKey: PropTypes.func
};

/**
 * 使用示例：简单列表
 */
function SimpleListExample() {
    // 生成测试数据
    const items = useMemo(() => {
        return Array.from({length: 10000}, (_, i) => ({
            id: i,
            title: `Item ${i + 1}`,
            description: `这是第 ${i + 1} 项的描述信息`
        }));
    }, []);

    // 渲染每项
    const renderItem = (item) => (
        <div style={{padding: '10px'}}>
            <strong>{item.title}</strong>
            <p style={{margin: '5px 0 0', color: '#666'}}>
                {item.description}
            </p>
        </div>
    );

    return (
        <div>
            <h3>固定高度虚拟列表（10000 项）</h3>
            <FixedSizeVirtualList
                items={items}
                itemHeight={60}
                containerHeight={400}
                renderItem={renderItem}
            />
        </div>
    );
}

/**
 * 使用示例：聊天消息列表
 */
function ChatListExample() {
    const messages = useMemo(() => {
        return Array.from({length: 1000}, (_, i) => ({
            id: `msg-${i}`,
            sender: i % 2 === 0 ? '用户A' : '用户B',
            content: `这是第 ${i + 1} 条消息${'。'.repeat(Math.random() * 10 + 1)}`,
            time: new Date(Date.now() - i * 60000).toLocaleTimeString()
        }));
    }, []);

    const renderMessage = (msg) => (
        <div style={{
            padding: '10px',
            backgroundColor: msg.sender === '用户A' ? '#e3f2fd' : '#f5f5f5',
            borderRadius: '8px',
            margin: '5px'
        }}>
            <div style={{display: 'flex', justifyContent: 'space-between'}}>
                <strong>{msg.sender}</strong>
                <span style={{color: '#999', fontSize: '12px'}}>{msg.time}</span>
            </div>
            <p style={{margin: '5px 0 0'}}>{msg.content}</p>
        </div>
    );

    return (
        <div>
            <h3>不定高度虚拟列表 - 聊天消息（1000 条）</h3>
            <VariableSizeVirtualList
                items={messages}
                containerHeight={400}
                estimateItemHeight={80}
                renderItem={renderMessage}
                getItemKey={(msg) => msg.id}
            />
        </div>
    );
}

// 导出组件
export {
    FixedSizeVirtualList,
    VariableSizeVirtualList,
    SimpleListExample,
    ChatListExample
};

export default FixedSizeVirtualList;
