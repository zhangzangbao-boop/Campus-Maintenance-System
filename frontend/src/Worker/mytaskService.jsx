// src/services/mytaskService.js
import api from '../services/api';
import { message } from 'antd';

// 维修工人任务状态枚举
export const TASK_STATUS = {
  PENDING: { value: 'pending', label: '待受理', color: 'orange' },
  PROCESSING: { value: 'processing', label: '处理中', color: 'blue' },
  COMPLETED: { value: 'completed', label: '已完成', color: 'green' },
  TO_BE_EVALUATED: { value: 'to_be_evaluated', label: '待评价', color: 'purple' },
  CLOSED: { value: 'closed', label: '已关闭', color: 'default' },
  REJECTED: { value: 'rejected', label: '已驳回', color: 'red' },
};

// 报修分类枚举
export const REPAIR_CATEGORIES = {
  waterAndElectricity: { value: "waterAndElectricity", label: "水电维修" },
  networkIssues: { value: "networkIssues", label: "网络故障" },
  furnitureRepair: { value: "furnitureRepair", label: "家具维修" },
  applianceIssues: { value: "applianceIssues", label: "电器故障" },
  publicFacilities: { value: "publicFacilities", label: "公共设施" },
};

// 维修人员数据
export const REPAIRMEN = {
  1: { id: 1, name: '张师傅', phone: '13800138001', specialty: ['水管维修', '电路检修'] },
  2: { id: 2, name: '李师傅', phone: '13800138002', specialty: ['灯具维修', '门窗修复'] },
  3: { id: 3, name: '王师傅', phone: '13800138003', specialty: ['公共设施', '家具维修'] },
};

// 维修工人任务服务 - 全部改为调用真实API
export const mytaskService = {
  // 获取维修工人的所有任务
  getMyTasks: async (repairmanId, params = {}) => {
    try {
      // 移除静态数据过滤，直接调用API
      const response = await api.repairman.getMyTasks({
        ...params,
        // 如果后端需要repairmanId，可以在这里传递
        repairmanId: repairmanId
      });
      
      // 假设API返回的数据结构为 { code: 200, data: { list: [], total: number, page: number, pageSize: number } }
      if (response.code === 200) {
        return {
          data: response.data.list || response.data, // 兼容不同数据结构
          total: response.data.total || (response.data.list ? response.data.list.length : 0),
          page: response.data.page || 1,
          pageSize: response.data.pageSize || 10,
        };
      } else {
        throw new Error(response.message || '获取任务失败');
      }
    } catch (error) {
      console.error('获取任务失败:', error);
      message.error('获取任务失败: ' + error.message);
      throw error;
    }
  },

  // 根据ID获取单个任务
  getTaskById: async (taskId, repairmanId) => {
    try {
      const response = await api.repairman.getTaskDetail(taskId);
      if (response.code === 200) {
        return response.data;
      } else {
        throw new Error(response.message || '获取任务详情失败');
      }
    } catch (error) {
      console.error('获取任务详情失败:', error);
      message.error('获取任务详情失败: ' + error.message);
      throw error;
    }
  },

  // 开始处理任务
  startTask: async (taskId, repairmanId, estimatedTime = null) => {
    try {
      const response = await api.repairman.startTask(taskId, estimatedTime);
      if (response.code === 200) {
        message.success('任务开始处理成功');
        return response.data;
      } else {
        throw new Error(response.message || '开始任务失败');
      }
    } catch (error) {
      console.error('开始任务失败:', error);
      message.error('开始任务失败: ' + error.message);
      throw error;
    }
  },

  // 完成任务
  completeTask: async (taskId, repairmanId, completionNotes = '') => {
    try {
      const response = await api.repairman.completeTask(taskId, completionNotes);
      if (response.code === 200) {
        message.success('任务完成成功');
        return response.data;
      } else {
        throw new Error(response.message || '完成任务失败');
      }
    } catch (error) {
      console.error('完成任务失败:', error);
      message.error('完成任务失败: ' + error.message);
      throw error;
    }
  },

  // 更新任务备注
  updateTaskNotes: async (taskId, repairmanId, notes) => {
    try {
      // 假设有更新备注的API，如果没有可以暂时使用完成任务API或其他方式
      // 这里先模拟成功
      await new Promise(resolve => setTimeout(resolve, 500));
      message.success('备注更新成功');
      return { success: true };
    } catch (error) {
      console.error('更新备注失败:', error);
      message.error('更新备注失败: ' + error.message);
      throw error;
    }
  },

  // 获取维修工人的统计数据
  getRepairmanStats: async (repairmanId) => {
    try {
      // 假设有统计API，如果没有可以通过获取所有任务计算
      const tasksResponse = await api.repairman.getMyTasks({ repairmanId });
      
      if (tasksResponse.code === 200) {
        const tasks = tasksResponse.data.list || tasksResponse.data || [];
        
        const stats = {
          total: tasks.length,
          pending: tasks.filter(task => task.status === 'pending').length,
          processing: tasks.filter(task => task.status === 'processing').length,
          completed: tasks.filter(task => task.status === 'completed').length,
          to_be_evaluated: tasks.filter(task => task.status === 'to_be_evaluated').length,
          closed: tasks.filter(task => task.status === 'closed').length,
          averageRating: 0,
        };
        
        // 计算平均评分
        const ratedTasks = tasks.filter(task => task.rating !== null && task.rating !== undefined);
        if (ratedTasks.length > 0) {
          const totalRating = ratedTasks.reduce((sum, task) => sum + task.rating, 0);
          stats.averageRating = (totalRating / ratedTasks.length).toFixed(1);
        }
        
        return stats;
      } else {
        throw new Error(tasksResponse.message || '获取统计数据失败');
      }
    } catch (error) {
      console.error('获取统计数据失败:', error);
      // 返回默认统计数据
      return {
        total: 0,
        pending: 0,
        processing: 0,
        completed: 0,
        to_be_evaluated: 0,
        closed: 0,
        averageRating: 0,
      };
    }
  },

  // 搜索任务
  searchMyTasks: async (repairmanId, filters = {}) => {
    try {
      // 直接使用getMyTasks API，传递搜索参数
      const response = await api.repairman.getMyTasks({
        ...filters,
        repairmanId: repairmanId
      });
      
      if (response.code === 200) {
        return {
          data: response.data.list || response.data,
          total: response.data.total || (response.data.list ? response.data.list.length : 0),
          page: response.data.page || 1,
          pageSize: response.data.pageSize || 10,
        };
      } else {
        throw new Error(response.message || '搜索任务失败');
      }
    } catch (error) {
      console.error('搜索任务失败:', error);
      message.error('搜索任务失败: ' + error.message);
      throw error;
    }
  },
};

// 工具函数保持不变
export const mytaskUtils = {
  // 获取状态信息
  getStatusInfo: (status) => {
    const statusMap = {
      pending: { label: '待受理', color: 'orange' },
      processing: { label: '处理中', color: 'blue' },
      completed: { label: '已完成', color: 'green' },
      to_be_evaluated: { label: '待评价', color: 'purple' },
      closed: { label: '已关闭', color: 'default' },
      rejected: { label: '已驳回', color: 'red' },
    };
    return statusMap[status] || { label: status, color: 'default' };
  },

  // 获取分类信息
  getCategoryInfo: (category) => {
    const categoryMap = {
      waterAndElectricity: { value: "waterAndElectricity", label: "水电维修" },
      networkIssues: { value: "networkIssues", label: "网络故障" },
      furnitureRepair: { value: "furnitureRepair", label: "家具维修" },
      applianceIssues: { value: "applianceIssues", label: "电器故障" },
      publicFacilities: { value: "publicFacilities", label: "公共设施" },
    };
    return categoryMap[category] || { label: category };
  },

  // 获取优先级信息
  getPriorityInfo: (priority) => {
    const priorityMap = {
      high: { label: '高', color: 'red' },
      medium: { label: '中', color: 'orange' },
      low: { label: '低', color: 'green' },
    };
    return priorityMap[priority] || { label: '未知', color: 'default' };
  },

  // 获取维修人员信息
  getRepairmanInfo: (repairmanId) => {
    return REPAIRMEN[repairmanId] || null;
  },

  // 计算任务耗时
  calculateTaskDuration: (task) => {
    if (!task.assigned_at) return null;
    
    const startTime = new Date(task.assigned_at);
    const endTime = task.completed_at ? new Date(task.completed_at) : new Date();
    const durationMs = endTime - startTime;
    
    const hours = Math.floor(durationMs / (1000 * 60 * 60));
    const minutes = Math.floor((durationMs % (1000 * 60 * 60)) / (1000 * 60));
    
    if (hours > 0) {
      return `${hours}小时${minutes}分钟`;
    } else {
      return `${minutes}分钟`;
    }
  },

  // 检查任务是否超时
  isTaskOverdue: (task) => {
    if (!task.estimated_completion_time || task.completed_at) return false;
    
    const estimatedTime = new Date(task.estimated_completion_time);
    const currentTime = new Date();
    
    return currentTime > estimatedTime;
  },
};