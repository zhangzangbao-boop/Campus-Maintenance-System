// src/services/feedbackService.js
import api from '../Services/api';
import { message } from 'antd';

export const feedbackService = {
  // 获取所有评价
  getAllFeedbacks: async (params = {}) => {
    try {
      const response = await api.admin.getAllFeedbacks(params);
      
      if (response.code === 200) {
        return response.data.list || response.data || [];
      } else {
        throw new Error(response.message || '获取评价列表失败');
      }
    } catch (error) {
      console.error('获取评价列表失败:', error);
      message.error('获取评价列表失败');
      // 返回模拟数据作为fallback
      return [
        {
          id: 1,
          repairOrderId: 1,
          studentId: '001',
          studentName: '张三',
          repairmanId: 1,
          repairmanName: '张师傅',
          rating: 5,
          comment: '张师傅非常专业，很快就找到了问题所在！',
          createdAt: '2025-11-16 14:20:00',
        },
        {
          id: 2,
          repairOrderId: 2,
          studentId: '002',
          studentName: '李四',
          repairmanId: 2,
          repairmanName: '李师傅',
          rating: 3,
          comment: '问题解决了，但是维修过程中弄脏了墙面。',
          createdAt: '2025-11-17 09:15:00',
        },
        {
          id: 3,
          repairOrderId: 3,
          studentId: '003',
          studentName: '王五',
          repairmanId: 3,
          repairmanName: '王师傅',
          rating: 4,
          comment: '',
          createdAt: '2025-11-18 16:45:00',
        },
      ];
    }
  },

  // 根据ID删除评价
  deleteFeedback: async (feedbackId) => {
    try {
      const response = await api.admin.deleteFeedback(feedbackId);
      
      if (response.code === 200) {
        message.success('评价删除成功');
        return true;
      } else {
        throw new Error(response.message || '删除评价失败');
      }
    } catch (error) {
      console.error('删除评价失败:', error);
      message.error('删除评价失败: ' + error.message);
      throw error;
    }
  },

  // 获取维修人员信息（备用方法，如果API返回的数据中不包含维修人员姓名）
  getRepairmanInfo: (repairmanId) => {
    const repairmen = {
      1: { id: 1, name: '张师傅' },
      2: { id: 2, name: '李师傅' },
      3: { id: 3, name: '王师傅' },
      4: { id: 4, name: '赵师傅' },
    };
    return repairmen[repairmanId] || { id: repairmanId, name: '未知维修工' };
  },
};