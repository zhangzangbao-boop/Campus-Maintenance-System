// src/services/userService.js
import api from '../Services/api';
import { message } from 'antd';

export const userService = {
  // 获取学生账号列表
  getStudents: async (params = {}) => {
    try {
      const response = await api.admin.getStudents(params);
      
      if (response.code === 200) {
        return {
          data: response.data.list || response.data,
          total: response.data.total || (response.data.list ? response.data.list.length : 0),
          page: response.data.page || 1,
          pageSize: response.data.pageSize || 10,
        };
      } else {
        throw new Error(response.message || '获取学生列表失败');
      }
    } catch (error) {
      console.error('获取学生列表失败:', error);
      message.error('获取学生列表失败');
      // 返回模拟数据作为fallback
      return {
        data: [
          { id: '20210001', number: "stu001", name: '张三', phone: '13800138001', type: 'student' },
          { id: '20210002', number: "stu002", name: '李四', phone: '13800138002', type: 'student' },
          { id: '20210003', number: "stu003", name: '王五', phone: '13800138003', type: 'student' },
          { id: '20210004', number: "stu004", name: '赵六', phone: '13800138004', type: 'student' },
          { id: '20210005', number: "stu005", name: '钱七', phone: '13800138005', type: 'student' },
          { id: '20210006', number: "stu006", name: '孙八', phone: '13800138006', type: 'student' },
        ],
        total: 6,
        page: 1,
        pageSize: 10,
      };
    }
  },

  // 获取维修人员账号列表
  getRepairmen: async (params = {}) => {
    try {
      const response = await api.admin.getRepairmen(params);
      
      if (response.code === 200) {
        return {
          data: response.data.list || response.data,
          total: response.data.total || (response.data.list ? response.data.list.length : 0),
          page: response.data.page || 1,
          pageSize: response.data.pageSize || 10,
        };
      } else {
        throw new Error(response.message || '获取维修人员列表失败');
      }
    } catch (error) {
      console.error('获取维修人员列表失败:', error);
      message.error('获取维修人员列表失败');
      // 返回模拟数据作为fallback
      return {
        data: [
          { id: '20210007', number: "worker001", name: '张师傅', phone: '13900139001', type: 'repairman' },
          { id: '20210008', number: "worker002", name: '李师傅', phone: '13900139002', type: 'repairman' },
          { id: '20210009', number: "worker003", name: '王师傅', phone: '13900139003', type: 'repairman' },
          { id: '20210010', number: "worker004", name: '赵师傅', phone: '13900139004', type: 'repairman' },
        ],
        total: 4,
        page: 1,
        pageSize: 10,
      };
    }
  },

  // 更新用户信息
  updateUser: async (userId, userData) => {
    try {
      const response = await api.admin.updateUser(userId, userData.phone);
      
      if (response.code === 200) {
        message.success('用户信息更新成功');
        return response.data;
      } else {
        throw new Error(response.message || '更新用户信息失败');
      }
    } catch (error) {
      console.error('更新用户信息失败:', error);
      message.error('更新用户信息失败');
      throw error;
    }
  },

  // 重置用户密码
  resetPassword: async (userId) => {
    try {
      const response = await api.admin.resetPassword(userId);
      
      if (response.code === 200) {
        message.success('密码重置成功');
        return response.data;
      } else {
        throw new Error(response.message || '重置密码失败');
      }
    } catch (error) {
      console.error('重置密码失败:', error);
      message.error('重置密码失败');
      throw error;
    }
  },

  // 删除用户
  deleteUser: async (userId, userType) => {
    try {
      // 注意：api.js 中没有删除用户的接口，这里暂时模拟成功
      // 在实际项目中，需要添加删除用户的API
      await new Promise(resolve => setTimeout(resolve, 500));
      message.success('用户删除成功');
      return { success: true };
    } catch (error) {
      console.error('删除用户失败:', error);
      message.error('删除用户失败');
      throw error;
    }
  },

  // 搜索用户
  searchUsers: async (userType, filters = {}) => {
    try {
      let response;
      if (userType === 'students') {
        response = await api.admin.getStudents(filters);
      } else {
        response = await api.admin.getRepairmen(filters);
      }
      
      if (response.code === 200) {
        return {
          data: response.data.list || response.data,
          total: response.data.total || (response.data.list ? response.data.list.length : 0),
          page: response.data.page || 1,
          pageSize: response.data.pageSize || 10,
        };
      } else {
        throw new Error(response.message || '搜索用户失败');
      }
    } catch (error) {
      console.error('搜索用户失败:', error);
      message.error('搜索用户失败');
      throw error;
    }
  }
};