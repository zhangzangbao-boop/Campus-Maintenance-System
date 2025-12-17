// src/services/repairService.js
import api from './api';
import { message } from 'antd';

// 工单状态枚举
export const REPAIR_STATUS = {
  PENDING: { value: "pending", label: "待受理", color: "orange" },
  PROCESSING: { value: "processing", label: "处理中", color: "blue" },
  COMPLETED: { value: "completed", label: "已完成", color: "green" },
  TO_BE_EVALUATED: {value: "to_be_evaluated",label: "待评价", color: "purple"},
  CLOSED: { value: "closed", label: "已关闭", color: "default" },
  REJECTED: { value: "rejected", label: "已驳回", color: "red" },
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
  1: { id: 1, name: "张师傅" },
  2: { id: 2, name: "李师傅" },
  3: { id: 3, name: "王师傅" },
};

// 11.18添加紧急程度类型
export const priority_LEVELS = {
  LOW: { value: "low", label: "一般", color: "blue" },
  MEDIUM: { value: "medium", label: "较紧急", color: "orange" },
  HIGH: { value: "high", label: "紧急", color: "red" },
};

// 数据服务方法 - 全部改为调用API
export const repairService = {
  // 获取所有工单（管理员端使用）
  getRepairOrders: async (params = {}) => {
    try {
      const response = await api.admin.getAllOrders(params);
      
      if (response.code === 200) {
        return {
          data: response.data.list || response.data,
          total: response.data.total || (response.data.list ? response.data.list.length : 0),
          page: response.data.page || 1,
          pageSize: response.data.pageSize || 10,
        };
      } else {
        throw new Error(response.message || '获取工单失败');
      }
    } catch (error) {
      console.error('获取工单失败:', error);
      message.error('获取工单失败: ' + error.message);
      throw error;
    }
  },

  // 获取我的报修（学生端使用）
  getMyRepairOrders: async (params = {}) => {
    try {
      const response = await api.student.getMyOrders(params);
      
      if (response.code === 200) {
        return {
          data: response.data.list || response.data,
          total: response.data.total || (response.data.list ? response.data.list.length : 0),
          page: response.data.page || 1,
          pageSize: response.data.pageSize || 10,
        };
      } else {
        throw new Error(response.message || '获取我的报修失败');
      }
    } catch (error) {
      console.error('获取我的报修失败:', error);
      message.error('获取我的报修失败: ' + error.message);
      throw error;
    }
  },

  // 根据ID获取单个工单
  getRepairOrderById: async (id) => {
    try {
      const response = await api.student.getOrderDetail(id);
      if (response.code === 200) {
        return response.data;
      } else {
        throw new Error(response.message || '获取工单详情失败');
      }
    } catch (error) {
      console.error('获取工单详情失败:', error);
      message.error('获取工单详情失败: ' + error.message);
      throw error;
    }
  },

  // 创建新工单
  createRepairOrder: async (orderData) => {
    try {
      // 如果是FormData（包含文件上传）
      if (orderData instanceof FormData) {
        const response = await api.student.createOrder(orderData);
        if (response.code === 200) {
          message.success('报修申请提交成功！');
          return response.data;
        } else {
          throw new Error(response.message || '创建工单失败');
        }
      } else {
        // 如果是普通对象数据
        const formData = new FormData();
        
        // 添加文本字段
        Object.keys(orderData).forEach(key => {
          if (key !== 'images' && orderData[key] !== undefined && orderData[key] !== null) {
            formData.append(key, orderData[key]);
          }
        });
        
        // 添加图片文件
        if (orderData.images && Array.isArray(orderData.images)) {
          orderData.images.forEach((file, index) => {
            formData.append('images', file);
            message.log('Appending file to FormData:', file, 'at index', index);
          });
        }
        
        const response = await api.student.createOrder(formData);
        if (response.code === 200) {
          message.success('报修申请提交成功！');
          return response.data;
        } else {
          throw new Error(response.message || '创建工单失败');
        }
      }
    } catch (error) {
      console.error('创建工单失败:', error);
      message.error('创建工单失败: ' + error.message);
      throw error;
    }
  },

  // 更新工单状态
  updateRepairOrderStatus: async (id, status, repairmanId = null) => {
    try {
      // 根据状态调用不同的API
      let response;
      if (status === 'processing' && repairmanId) {
        response = await api.admin.assignOrder(id, repairmanId);
      } else {
        // 其他状态更新可能需要单独的API，这里暂时模拟
        await new Promise(resolve => setTimeout(resolve, 500));
        response = { code: 200, data: { id, status, repairmanId } };
      }
      
      if (response.code === 200) {
        message.success('工单状态更新成功');
        return response.data;
      } else {
        throw new Error(response.message || '更新工单状态失败');
      }
    } catch (error) {
      console.error('更新工单状态失败:', error);
      message.error('更新工单状态失败: ' + error.message);
      throw error;
    }
  },

  // 获取维修人员列表
  getRepairmen: async () => {
    try {
      const response = await api.admin.getRepairmen();
      if (response.code === 200) {
        return response.data;
      } else {
        throw new Error(response.message || '获取维修人员列表失败');
      }
    } catch (error) {
      console.error('获取维修人员列表失败:', error);
      // 返回默认维修人员列表作为fallback
      return Object.values(REPAIRMEN);
    }
  },

  // 分配维修人员
  assignRepairman: async (orderId, repairmanId) => {
    try {
      const response = await api.admin.assignOrder(orderId, repairmanId);
      if (response.code === 200) {
        message.success('维修人员分配成功');
        return response.data;
      } else {
        throw new Error(response.message || '分配维修人员失败');
      }
    } catch (error) {
      console.error('分配维修人员失败:', error);
      message.error('分配维修人员失败: ' + error.message);
      throw error;
    }
  },

  // 驳回工单
  rejectRepairOrder: async (orderId, reason) => {
    try {
      const response = await api.admin.rejectOrder(orderId, reason);
      if (response.code === 200) {
        message.success('工单已驳回');
        return response.data;
      } else {
        throw new Error(response.message || '驳回工单失败');
      }
    } catch (error) {
      console.error('驳回工单失败:', error);
      message.error('驳回工单失败: ' + error.message);
      throw error;
    }
  },

  // 搜索工单
  searchRepairOrders: async (filters = {}) => {
    try {
      // 使用getRepairOrders API进行搜索
      const response = await api.admin.getAllOrders(filters);
      
      if (response.code === 200) {
        return {
          data: response.data.list || response.data,
          total: response.data.total || (response.data.list ? response.data.list.length : 0),
          page: response.data.page || 1,
          pageSize: response.data.pageSize || 10,
        };
      } else {
        throw new Error(response.message || '搜索工单失败');
      }
    } catch (error) {
      console.error('搜索工单失败:', error);
      message.error('搜索工单失败: ' + error.message);
      throw error;
    }
  },

  // 搜索我的报修（学生端）
  searchMyRepairOrders: async (filters = {}) => {
    try {
      const response = await api.student.getMyOrders(filters);
      
      if (response.code === 200) {
        return {
          data: response.data.list || response.data,
          total: response.data.total || (response.data.list ? response.data.list.length : 0),
          page: response.data.page || 1,
          pageSize: response.data.pageSize || 10,
        };
      } else {
        throw new Error(response.message || '搜索我的报修失败');
      }
    } catch (error) {
      console.error('搜索我的报修失败:', error);
      message.error('搜索我的报修失败: ' + error.message);
      throw error;
    }
  },

  // 删除报修（学生端）
  deleteRepairOrder: async (orderId) => {
    try {
      const response = await api.student.deleteOrder(orderId);
      if (response.code === 200) {
        message.success('报修单删除成功');
        return response.data;
      } else {
        throw new Error(response.message || '删除报修单失败');
      }
    } catch (error) {
      console.error('删除报修单失败:', error);
      message.error('删除报修单失败: ' + error.message);
      throw error;
    }
  },

  // 提交评价（学生端）
  evaluateRepairOrder: async (orderId, rating, feedback) => {
    try {
      const response = await api.student.evaluateOrder(orderId, { rating, feedback });
      if (response.code === 200) {
        message.success('评价提交成功');
        return response.data;
      } else {
        throw new Error(response.message || '提交评价失败');
      }
    } catch (error) {
      console.error('提交评价失败:', error);
      message.error('提交评价失败: ' + error.message);
      throw error;
    }
  },
};

// 工具函数
export const repairUtils = {
  // 获取状态信息
  getStatusInfo: (status) => {
    const statusMap = {
      pending: { label: "待受理", color: "orange" },
      processing: { label: "处理中", color: "blue" },
      completed: { label: "已完成", color: "green" },
      to_be_evaluated: { label: "待评价", color: "purple" },
      closed: { label: "已关闭", color: "default" },
      rejected: { label: "已驳回", color: "red" },
    };
    return statusMap[status] || { label: status, color: "default" };
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

  // 获取维修人员信息
  getRepairmanInfo: (repairmanId) => {
    const repairmen = {
      1: { id: 1, name: "张师傅" },
      2: { id: 2, name: "李师傅" },
      3: { id: 3, name: "王师傅" },
    };
    return repairmen[repairmanId] || null;
  },

  // 11.18获取紧急程度信息
  getpriorityInfo: (priority) => {
    const priorityMap = {
      low: { label: "低", color: "blue" },
      medium: { label: "中", color: "orange" },
      high: { label: "高", color: "red" },
    };
    return priorityMap[priority] || { label: priority, color: "default" };
  },
};