<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><OfficeBuilding/></el-icon> 组织架构
          </span>
        </div>
      </template>

      <el-row :gutter="20">
        <!-- 部门树 -->
        <el-col :span="8">
          <el-card shadow="hover" class="dept-tree-card">
            <template #header>
              <div class="dept-header">
                <span>部门列表</span>
                <el-tag type="info" size="small">{{ deptList.length }} 个部门</el-tag>
                <el-button type="primary" size="small" @click="openDeptDialog(null)">
                  <el-icon>
                    <Plus/>
                  </el-icon>
                  新增部门
                </el-button>
              </div>
            </template>
            <el-tree
                :data="deptTree"
                :props="{ label: 'deptName', children: 'children' }"
                node-key="id"
                default-expand-all
                highlight-current
                @node-click="handleDeptClick"
            >
              <template #default="{ node, data }">
                <span class="dept-node">
                  <el-icon v-if="data.parentId === 0"><OfficeBuilding/></el-icon>
                  <el-icon v-else><Folder/></el-icon>
                  <span style="margin-left: 5px">{{ node.label }}</span>
                  <el-tag v-if="data.managerName" type="success" size="small" style="margin-left: 8px">
                    {{ data.managerName }}
                  </el-tag>
                  <span v-else class="no-manager-tag">无经理</span>
                </span>
              </template>
            </el-tree>
          </el-card>
        </el-col>

        <!-- 部门详情 -->
        <el-col :span="16">
          <el-card shadow="hover" v-if="selectedDept">
            <template #header>
              <div class="detail-header">
                <span>部门详情 - {{ selectedDept.deptName }}</span>
                <div>
                  <el-button type="primary" size="small" @click="openDeptDialog(selectedDept)">
                    <el-icon>
                      <Edit/>
                    </el-icon>
                    编辑
                  </el-button>
                  <el-button type="danger" size="small" @click="handleDeleteDept">
                    <el-icon>
                      <Delete/>
                    </el-icon>
                    删除
                  </el-button>
                </div>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="部门ID">{{ selectedDept.id }}</el-descriptions-item>
              <el-descriptions-item label="部门名称">{{ selectedDept.deptName }}</el-descriptions-item>
              <el-descriptions-item label="上级部门">
                {{ getParentDeptName(selectedDept.parentId) }}
              </el-descriptions-item>
              <el-descriptions-item label="排序">{{ selectedDept.sort }}</el-descriptions-item>
              <el-descriptions-item label="部门经理" :span="2">
                <div class="manager-cell">
                  <el-tag v-if="selectedDept.managerName" type="success">{{ selectedDept.managerName }}</el-tag>
                  <span v-else class="text-gray">暂未设置</span>
                  <el-button type="primary" link size="small" @click="openSetManagerDialog">
                    设置经理
                  </el-button>
                </div>
              </el-descriptions-item>
            </el-descriptions>

            <el-divider/>

            <div class="user-header">
              <h4>部门成员 ({{ deptUsers.length }} 人)</h4>
              <el-button type="primary" size="small" @click="openUserDialog(null)">
                <el-icon>
                  <Plus/>
                </el-icon>
                新增用户
              </el-button>
            </div>
            <el-table :data="deptUsers" style="margin-top: 15px" v-loading="userLoading">
              <el-table-column type="index" width="50"/>
              <el-table-column prop="username" label="用户名" width="120"/>
              <el-table-column prop="realName" label="姓名" width="120"/>
              <el-table-column prop="email" label="邮箱"/>
              <el-table-column prop="phone" label="电话" width="130"/>
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="setAsManager(row)">设为经理</el-button>
                  <el-button type="primary" link size="small" @click="openUserDialog(row)">编辑</el-button>
                  <el-button type="danger" link size="small" @click="handleDeleteUser(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <el-empty v-else description="请选择部门查看详情"/>
        </el-col>
      </el-row>
    </el-card>

    <!-- 部门编辑对话框 -->
    <el-dialog v-model="deptDialogVisible" :title="deptForm.id ? '编辑部门' : '新增部门'" width="500px">
      <el-form :model="deptForm" label-width="80px">
        <el-form-item label="部门名称" required>
          <el-input v-model="deptForm.deptName" placeholder="请输入部门名称"/>
        </el-form-item>
        <el-form-item label="上级部门">
          <el-select v-model="deptForm.parentId" placeholder="无（顶级部门）" clearable style="width: 100%">
            <el-option label="无（顶级部门）" :value="0"/>
            <el-option v-for="dept in deptList" :key="dept.id" :label="dept.deptName" :value="dept.id"
                       :disabled="dept.id === deptForm.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="部门经理">
          <el-select v-model="deptForm.managerId" placeholder="请选择部门经理" clearable style="width: 100%">
            <el-option v-for="user in allUsers" :key="user.id" :label="`${user.realName} (${user.username})`"
                       :value="user.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="deptForm.sort" :min="0" :max="999"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDept" :loading="deptSaving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 用户编辑对话框 -->
    <el-dialog v-model="userDialogVisible" :title="userForm.id ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="userForm" label-width="80px">
        <el-form-item label="用户名" required>
          <el-input v-model="userForm.username" placeholder="请输入用户名" :disabled="!!userForm.id"/>
        </el-form-item>
        <el-form-item label="密码" :required="!userForm.id">
          <el-input v-model="userForm.password" type="password"
                    :placeholder="userForm.id ? '留空则不修改密码' : '请输入密码'"/>
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名"/>
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="userForm.deptId" placeholder="请选择部门" style="width: 100%">
            <el-option v-for="dept in deptList" :key="dept.id" :label="dept.deptName" :value="dept.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" placeholder="请输入邮箱"/>
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="userForm.phone" placeholder="请输入电话"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser" :loading="userSaving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 设置部门经理对话框 -->
    <el-dialog v-model="managerDialogVisible" title="设置部门经理" width="400px">
      <el-form label-width="80px">
        <el-form-item label="当前部门">
          <el-input :value="selectedDept?.deptName" disabled/>
        </el-form-item>
        <el-form-item label="选择经理">
          <el-select v-model="selectedManagerId" placeholder="请选择部门经理" style="width: 100%">
            <el-option v-for="user in deptUsers" :key="user.id" :label="`${user.realName} (${user.username})`"
                       :value="user.id"/>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="managerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSetManager" :loading="managerSaving">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
  createDept,
  createUser,
  deleteDept,
  deleteUser,
  getDeptList,
  getUserList,
  setDeptManager,
  updateDept,
  updateUser
} from '@/api/workflow'
import {Delete, Edit, Folder, OfficeBuilding, Plus} from "@element-plus/icons-vue";

const deptList = ref([])
const selectedDept = ref(null)
const deptUsers = ref([])
const allUsers = ref([])
const userLoading = ref(false)

// 部门对话框
const deptDialogVisible = ref(false)
const deptForm = ref({})
const deptSaving = ref(false)

// 用户对话框
const userDialogVisible = ref(false)
const userForm = ref({})
const userSaving = ref(false)

// 设置经理对话框
const managerDialogVisible = ref(false)
const selectedManagerId = ref(null)
const managerSaving = ref(false)

// 构建部门树
const deptTree = computed(() => {
  const map = {}
  const roots = []

  deptList.value.forEach(dept => {
    map[dept.id] = {...dept, children: []}
  })

  deptList.value.forEach(dept => {
    if (dept.parentId === 0) {
      roots.push(map[dept.id])
    } else if (map[dept.parentId]) {
      map[dept.parentId].children.push(map[dept.id])
    }
  })

  return roots
})

onMounted(async () => {
  await loadDeptList()
  await loadAllUsers()
})

async function loadDeptList() {
  try {
    const res = await getDeptList()
    deptList.value = res.data || []
    console.log('[Organization] 加载部门列表:', deptList.value.length, '个部门')
  } catch (e) {
    ElMessage.error('加载部门列表失败')
    console.error('[Organization] 加载部门失败:', e)
  }
}

async function loadAllUsers() {
  try {
    const res = await getUserList()
    allUsers.value = res.data || []
  } catch (e) {
    console.error('[Organization] 加载用户列表失败:', e)
  }
}

function handleDeptClick(dept) {
  selectedDept.value = dept
  loadDeptUsers(dept.id)
}

async function loadDeptUsers(deptId) {
  userLoading.value = true
  try {
    const res = await getUserList({deptId})
    deptUsers.value = res.data || []
  } catch (e) {
    deptUsers.value = []
  } finally {
    userLoading.value = false
  }
}

function getParentDeptName(parentId) {
  if (parentId === 0) return '无（顶级部门）'
  const parent = deptList.value.find(d => d.id === parentId)
  return parent ? parent.deptName : '未知'
}

// ============ 部门 CRUD ============

function openDeptDialog(dept) {
  if (dept) {
    deptForm.value = {...dept}
  } else {
    deptForm.value = {parentId: 0, sort: 0}
  }
  deptDialogVisible.value = true
}

async function saveDept() {
  if (!deptForm.value.deptName) {
    ElMessage.warning('请输入部门名称')
    return
  }

  deptSaving.value = true
  try {
    if (deptForm.value.id) {
      await updateDept(deptForm.value.id, deptForm.value)
      ElMessage.success('更新成功')
    } else {
      await createDept(deptForm.value)
      ElMessage.success('创建成功')
    }
    deptDialogVisible.value = false
    await loadDeptList()
    // 刷新选中的部门
    if (selectedDept.value) {
      const updated = deptList.value.find(d => d.id === selectedDept.value.id)
      if (updated) selectedDept.value = updated
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    deptSaving.value = false
  }
}

async function handleDeleteDept() {
  if (!selectedDept.value) return

  try {
    await ElMessageBox.confirm(`确定删除部门 "${selectedDept.value.deptName}" 吗？`, '删除确认', {
      type: 'warning'
    })

    await deleteDept(selectedDept.value.id)
    ElMessage.success('删除成功')
    selectedDept.value = null
    deptUsers.value = []
    await loadDeptList()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

// ============ 设置部门经理 ============

function openSetManagerDialog() {
  selectedManagerId.value = selectedDept.value?.managerId
  managerDialogVisible.value = true
}

async function setAsManager(user) {
  try {
    await ElMessageBox.confirm(`确定将 "${user.realName}" 设为部门经理吗？`, '设置确认')

    await setDeptManager(selectedDept.value.id, user.id)
    ElMessage.success('设置成功')
    await loadDeptList()
    // 更新选中部门
    const updated = deptList.value.find(d => d.id === selectedDept.value.id)
    if (updated) selectedDept.value = updated
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '设置失败')
    }
  }
}

async function confirmSetManager() {
  if (!selectedManagerId.value) {
    ElMessage.warning('请选择部门经理')
    return
  }

  managerSaving.value = true
  try {
    await setDeptManager(selectedDept.value.id, selectedManagerId.value)
    ElMessage.success('设置成功')
    managerDialogVisible.value = false
    await loadDeptList()
    const updated = deptList.value.find(d => d.id === selectedDept.value.id)
    if (updated) selectedDept.value = updated
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '设置失败')
  } finally {
    managerSaving.value = false
  }
}

// ============ 用户 CRUD ============

function openUserDialog(user) {
  if (user) {
    userForm.value = {...user, password: ''}
  } else {
    userForm.value = {deptId: selectedDept.value?.id}
  }
  userDialogVisible.value = true
}

async function saveUser() {
  if (!userForm.value.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!userForm.value.realName) {
    ElMessage.warning('请输入真实姓名')
    return
  }
  if (!userForm.value.id && !userForm.value.password) {
    ElMessage.warning('请输入密码')
    return
  }

  userSaving.value = true
  try {
    if (userForm.value.id) {
      await updateUser(userForm.value.id, userForm.value)
      ElMessage.success('更新成功')
    } else {
      await createUser(userForm.value)
      ElMessage.success('创建成功')
    }
    userDialogVisible.value = false
    await loadDeptUsers(selectedDept.value.id)
    await loadAllUsers()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    userSaving.value = false
  }
}

async function handleDeleteUser(user) {
  try {
    await ElMessageBox.confirm(`确定删除用户 "${user.realName}" 吗？`, '删除确认', {
      type: 'warning'
    })

    await deleteUser(user.id)
    ElMessage.success('删除成功')
    await loadDeptUsers(selectedDept.value.id)
    await loadAllUsers()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}
</script>

<style scoped>
.card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dept-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dept-tree-card {
  min-height: 500px;
}

.dept-node {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.no-manager-tag {
  margin-left: 8px;
  color: #f56c6c;
  font-size: 12px;
}

.text-gray {
  color: #999;
}

.manager-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

h4 {
  margin: 0;
  font-size: 14px;
  color: #666;
}
</style>
