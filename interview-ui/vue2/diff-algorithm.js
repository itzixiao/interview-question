/**
 * Vue2 Diff 算法实现
 *
 * 核心原理：
 * 1. 同层比较：只比较同一层级的节点，不跨级比较
 * 2. 双端比较：使用新旧节点列表的头尾指针进行四种比较策略
 * 3. key 复用：通过 key 属性识别可复用的节点
 *
 * 四种比较策略：
 * - 旧头 vs 新头
 * - 旧尾 vs 新尾
 * - 旧头 vs 新尾
 * - 旧尾 vs 新头
 *
 * 对应文档：docs/18-前端开发/01-Vue2核心原理详解.md
 */

/**
 * 创建虚拟 DOM 节点
 * @param {string} tag - 标签名
 * @param {Object} data - 属性数据（包含 key、class、style 等）
 * @param {Array} children - 子节点数组
 * @param {string} text - 文本内容
 * @returns {Object} VNode 对象
 */
function createVNode(tag, data = {}, children = [], text = undefined) {
    return {
        tag,        // 标签名，如 'div'、'p'
        data,       // 属性数据，如 { key: '1', class: 'item' }
        children,   // 子节点数组
        text,       // 文本内容（文本节点时使用）
        key: data.key,  // 唯一标识，用于 Diff 优化
        elm: undefined  // 对应的真实 DOM 元素（渲染后填充）
    };
}

/**
 * 判断两个节点是否是相同节点
 * 判断依据：key 相同且标签名相同
 *
 * @param {Object} vnode1 - 旧节点
 * @param {Object} vnode2 - 新节点
 * @returns {boolean}
 */
function sameVnode(vnode1, vnode2) {
    return (
        vnode1.key === vnode2.key &&  // key 必须相同
        vnode1.tag === vnode2.tag     // 标签名必须相同
    );
}

/**
 * 创建真实 DOM 元素
 * @param {Object} vnode - 虚拟节点
 * @returns {Element} 真实 DOM 元素
 */
function createElm(vnode) {
    const {tag, data, children, text} = vnode;

    if (text !== undefined) {
        // 文本节点
        vnode.elm = document.createTextNode(text);
    } else {
        // 元素节点
        const elm = document.createElement(tag);
        vnode.elm = elm;

        // 设置属性
        if (data) {
            for (const key in data) {
                if (key !== 'key') {  // key 不是真实 DOM 属性
                    elm.setAttribute(key, data[key]);
                }
            }
        }

        // 递归创建子节点
        if (children) {
            for (let i = 0; i < children.length; i++) {
                elm.appendChild(createElm(children[i]));
            }
        }
    }

    return vnode.elm;
}

/**
 * Diff 算法核心函数 - 对比新旧虚拟 DOM，更新真实 DOM
 *
 * @param {Object} oldVnode - 旧的虚拟节点
 * @param {Object} vnode - 新的虚拟节点
 * @returns {Object} 更新后的虚拟节点
 */
function patch(oldVnode, vnode) {
    // 如果新旧节点是相同节点，执行更新
    if (sameVnode(oldVnode, vnode)) {
        patchVnode(oldVnode, vnode);
    } else {
        // 不是相同节点，直接替换
        const oldElm = oldVnode.elm;
        const parentElm = oldElm.parentNode;

        // 创建新的 DOM 元素
        createElm(vnode);

        // 插入新节点
        if (parentElm) {
            parentElm.insertBefore(vnode.elm, oldElm);
            // 移除旧节点
            parentElm.removeChild(oldElm);
        }
    }

    return vnode;
}

/**
 * 对比并更新两个相同节点的差异
 *
 * @param {Object} oldVnode - 旧节点
 * @param {Object} vnode - 新节点
 */
function patchVnode(oldVnode, vnode) {
    const elm = (vnode.elm = oldVnode.elm);

    // 如果新旧节点完全相同，直接返回
    if (oldVnode === vnode) {
        return;
    }

    const oldCh = oldVnode.children;
    const ch = vnode.children;

    // 情况1：新节点是文本节点
    if (vnode.text !== undefined) {
        if (oldVnode.text !== vnode.text) {
            // 文本不同，直接更新文本内容
            elm.textContent = vnode.text;
        }
    } else {
        // 情况2：新节点是元素节点
        if (oldCh && ch) {
            // 都有子节点，执行 Diff 对比子节点
            updateChildren(elm, oldCh, ch);
        } else if (ch) {
            // 旧节点没有子节点，新节点有子节点
            // 清空旧节点的文本，添加新子节点
            if (oldVnode.text) {
                elm.textContent = '';
            }
            for (let i = 0; i < ch.length; i++) {
                elm.appendChild(createElm(ch[i]));
            }
        } else if (oldCh) {
            // 旧节点有子节点，新节点没有
            // 移除所有子节点
            for (let i = 0; i < oldCh.length; i++) {
                elm.removeChild(oldCh[i].elm);
            }
        } else if (oldVnode.text) {
            // 旧节点是文本，新节点是空元素
            elm.textContent = '';
        }
    }
}

/**
 * 更新子节点列表 - Diff 算法的核心
 * 使用双端比较策略
 *
 * @param {Element} parentElm - 父 DOM 元素
 * @param {Array} oldCh - 旧子节点数组
 * @param {Array} newCh - 新子节点数组
 */
function updateChildren(parentElm, oldCh, newCh) {
    // 定义四个指针
    let oldStartIdx = 0;        // 旧头指针
    let oldEndIdx = oldCh.length - 1;  // 旧尾指针
    let newStartIdx = 0;        // 新头指针
    let newEndIdx = newCh.length - 1;  // 新尾指针

    // 获取头尾节点
    let oldStartVnode = oldCh[0];
    let oldEndVnode = oldCh[oldEndIdx];
    let newStartVnode = newCh[0];
    let newEndVnode = newCh[newEndIdx];

    // 用于存储 key 到旧节点索引的映射，优化查找效率
    let oldKeyToIdx;

    // 循环比较，直到某一端的指针相遇
    while (oldStartIdx <= oldEndIdx && newStartIdx <= newEndIdx) {
        // 跳过 undefined 的节点（已经被处理过的节点会被设为 undefined）
        if (!oldStartVnode) {
            oldStartVnode = oldCh[++oldStartIdx];
        } else if (!oldEndVnode) {
            oldEndVnode = oldCh[--oldEndIdx];
        }
        // 策略1：旧头 vs 新头
        else if (sameVnode(oldStartVnode, newStartVnode)) {
            // 相同节点，递归更新
            patchVnode(oldStartVnode, newStartVnode);
            // 指针同时向中间移动
            oldStartVnode = oldCh[++oldStartIdx];
            newStartVnode = newCh[++newStartIdx];
        }
        // 策略2：旧尾 vs 新尾
        else if (sameVnode(oldEndVnode, newEndVnode)) {
            patchVnode(oldEndVnode, newEndVnode);
            oldEndVnode = oldCh[--oldEndIdx];
            newEndVnode = newCh[--newEndIdx];
        }
        // 策略3：旧头 vs 新尾
        else if (sameVnode(oldStartVnode, newEndVnode)) {
            patchVnode(oldStartVnode, newEndVnode);
            // 将旧头移动到旧尾之后（对应新尾的位置）
            parentElm.insertBefore(oldStartVnode.elm, oldEndVnode.elm.nextSibling);
            oldStartVnode = oldCh[++oldStartIdx];
            newEndVnode = newCh[--newEndIdx];
        }
        // 策略4：旧尾 vs 新头
        else if (sameVnode(oldEndVnode, newStartVnode)) {
            patchVnode(oldEndVnode, newStartVnode);
            // 将旧尾移动到旧头之前（对应新头的位置）
            parentElm.insertBefore(oldEndVnode.elm, oldStartVnode.elm);
            oldEndVnode = oldCh[--oldEndIdx];
            newStartVnode = newCh[++newStartIdx];
        }
        // 以上四种策略都不匹配
        else {
            // 创建 key 到索引的映射表（如果还没有创建）
            if (!oldKeyToIdx) {
                oldKeyToIdx = createKeyToOldIdx(oldCh, oldStartIdx, oldEndIdx);
            }

            // 在新节点的 key 中查找对应的旧节点索引
            const idxInOld = newStartVnode.key
                ? oldKeyToIdx[newStartVnode.key]
                : findIdxInOld(newStartVnode, oldCh, oldStartIdx, oldEndIdx);

            if (idxInOld === undefined) {
                // 没找到，说明是新节点，需要创建
                parentElm.insertBefore(createElm(newStartVnode), oldStartVnode.elm);
            } else {
                // 找到了，移动节点
                const vnodeToMove = oldCh[idxInOld];
                patchVnode(vnodeToMove, newStartVnode);
                // 将旧节点设为 undefined，表示已处理
                oldCh[idxInOld] = undefined;
                parentElm.insertBefore(vnodeToMove.elm, oldStartVnode.elm);
            }

            // 新头指针向后移动
            newStartVnode = newCh[++newStartIdx];
        }
    }

    // 处理剩余节点
    if (oldStartIdx > oldEndIdx) {
        // 新节点还有剩余，需要添加
        const refElm = newCh[newEndIdx + 1] ? newCh[newEndIdx + 1].elm : null;
        for (let i = newStartIdx; i <= newEndIdx; i++) {
            parentElm.insertBefore(createElm(newCh[i]), refElm);
        }
    } else if (newStartIdx > newEndIdx) {
        // 旧节点还有剩余，需要删除
        for (let i = oldStartIdx; i <= oldEndIdx; i++) {
            if (oldCh[i]) {
                parentElm.removeChild(oldCh[i].elm);
            }
        }
    }
}

/**
 * 创建 key 到旧节点索引的映射表
 * 用于快速查找 key 对应的旧节点
 *
 * @param {Array} children - 子节点数组
 * @param {number} beginIdx - 开始索引
 * @param {number} endIdx - 结束索引
 * @returns {Object} key 到索引的映射表
 */
function createKeyToOldIdx(children, beginIdx, endIdx) {
    const map = {};
    for (let i = beginIdx; i <= endIdx; i++) {
        const key = children[i] && children[i].key;
        if (key) {
            map[key] = i;
        }
    }
    return map;
}

/**
 * 在没有 key 的情况下查找节点在旧数组中的索引
 *
 * @param {Object} node - 要查找的节点
 * @param {Array} oldCh - 旧子节点数组
 * @param {number} start - 开始索引
 * @param {number} end - 结束索引
 * @returns {number|undefined}
 */
function findIdxInOld(node, oldCh, start, end) {
    for (let i = start; i <= end; i++) {
        if (oldCh[i] && sameVnode(oldCh[i], node)) {
            return i;
        }
    }
}

// ==================== 使用示例 ====================

// 创建旧虚拟 DOM
const oldVnode = createVNode('div', {id: 'app'}, [
    createVNode('p', {key: 'a'}, [], 'A'),
    createVNode('p', {key: 'b'}, [], 'B'),
    createVNode('p', {key: 'c'}, [], 'C')
]);

// 创建新虚拟 DOM（B 移动到最前面）
const newVnode = createVNode('div', {id: 'app'}, [
    createVNode('p', {key: 'b'}, [], 'B'),
    createVNode('p', {key: 'a'}, [], 'A'),
    createVNode('p', {key: 'c'}, [], 'C'),
    createVNode('p', {key: 'd'}, [], 'D')  // 新增 D
]);

// 渲染旧虚拟 DOM 到真实 DOM
const container = document.createElement('div');
document.body.appendChild(container);
container.appendChild(createElm(oldVnode));

console.log('旧 DOM：', container.innerHTML);

// 执行 Diff 更新
patch(oldVnode, newVnode);

console.log('新 DOM：', container.innerHTML);

// 导出模块
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {createVNode, patch, sameVnode, updateChildren};
}
