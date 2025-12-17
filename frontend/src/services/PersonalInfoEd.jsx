import React, { useState, useEffect } from 'react';
import { 
  Modal, 
  Form, 
  Input, 
  Button, 
  Avatar, 
  message,
  Upload,
  Space
} from 'antd';
import { 
  UserOutlined, 
  EditOutlined,
  UploadOutlined
} from '@ant-design/icons';
// 引入封装好的 API 服务
import api from './api.jsx'; 

const PersonalInfoEd = ({ visible, onCancel, userInfo, onUpdate }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [avatarUrl, setAvatarUrl] = useState('');
  // 新增：用于存储用户新选择的头像文件
  const [avatarFile, setAvatarFile] = useState(null);

  // 当用户信息或弹窗显示状态变化时，更新表单数据
  useEffect(() => {
    if (visible && userInfo) {
      form.setFieldsValue({
        username: userInfo.username || '',
        email: userInfo.email || '',
        phone: userInfo.phone || '',
        department: userInfo.department || '',
        position: userInfo.position || ''
      });
      setAvatarUrl(userInfo.avatar || '');
      setAvatarFile(null); // 重置选中的文件
    }
  }, [visible, userInfo, form]);

  // 处理表单提交
  const handleSubmit = async (values) => {
    setLoading(true);
    try {
      // 🟢 修改点：构建 FormData 对象以适应后端 API
      const formData = new FormData();
      
      // 添加普通字段
      // 注意：根据API定义，username通常不可改，后端可能只接收可改字段，
      // 但为了保险，我们可以把表单里的都传过去，或者只传 phone/email/avatar
      Object.keys(values).forEach(key => {
        if (values[key] !== undefined && values[key] !== null) {
          formData.append(key, values[key]);
        }
      });

      // 🟢 修改点：如果有新选择的头像文件，添加到 FormData
      if (avatarFile) {
        formData.append('avatar', avatarFile); 
      }

      // 🟢 修改点：调用真实 API
      // PUT /api/users/me
      const updatedUser = await api.auth.updateMe(formData);
      
      // 调用父组件传递的更新函数
      if (onUpdate) {
        onUpdate(updatedUser);
      }
      
      message.success('个人信息更新成功！');
      onCancel(); // 关闭弹窗
    } catch (error) {
      message.error(error.message || '更新失败，请重试');
      console.error('更新个人信息出错:', error);
    } finally {
      setLoading(false);
    }
  };

  // 🟢 修改点：头像上传配置
  const uploadProps = {
    name: 'avatar',
    showUploadList: false,
    // 拦截自动上传，改为手动处理
    beforeUpload: (file) => {
      const isImage = file.type.startsWith('image/');
      if (!isImage) {
        message.error('只能上传图片文件！');
        return Upload.LIST_IGNORE;
      }

      // 1. 保存文件对象，以便稍后在 handleSubmit 中提交
      setAvatarFile(file);

      // 2. 创建本地预览 URL，让用户能立即看到效果
      const previewUrl = URL.createObjectURL(file);
      setAvatarUrl(previewUrl);

      // 返回 false 阻止 AntD 组件自动发起 POST 请求
      return false;
    },
  };

  // 根据用户角色显示不同的默认信息 (保留原有逻辑作为展示兜底)
  const getDefaultUserInfo = (username) => {
    const roleInfo = {
      stu: { 
        department: '计算机学院', 
        position: '学生',
        email: `${username}@student.edu.cn`
      },
      worker: { 
        department: '后勤维修部', 
        position: '维修人员',
        email: `${username}@worker.edu.cn`
      },
      admin: { 
        department: '系统管理部', 
        position: '管理员',
        email: `${username}@admin.edu.cn`
      }
    };
    
    return roleInfo[username] || {};
  };

  // 初始化用户信息
  const initialUserInfo = userInfo || {
    username: 'unknown',
    email: '',
    phone: '',
    department: '',
    position: '',
    ...getDefaultUserInfo(userInfo?.username || 'stu')
  };

  return (
    <Modal
      title={
        <Space>
          <EditOutlined />
          编辑个人信息
        </Space>
      }
      open={visible}
      onCancel={onCancel}
      footer={null}
      width={600}
      destroyOnHidden={true}
      maskClosable={false}
    >
      <div style={{ display: 'flex', gap: 24, marginBottom: 24 }}>
        {/* 头像上传区域 */}
        <div style={{ textAlign: 'center' }}>
          <Avatar
            size={80}
            src={avatarUrl}
            icon={!avatarUrl && <UserOutlined />}
            style={{ marginBottom: 8 }}
          />
          <Upload {...uploadProps}>
            <Button icon={<UploadOutlined />} size="small">
              更换头像
            </Button>
          </Upload>
        </div>

        {/* 表单区域 */}
        <div style={{ flex: 1 }}>
          <Form
            form={form}
            layout="vertical"
            onFinish={handleSubmit}
            initialValues={initialUserInfo}
          >
            <Form.Item
              label="用户名"
              name="username"
              rules={[
                { required: true, message: '请输入用户名!' },
                { min: 2, message: '用户名至少2个字符!' }
              ]}
            >
              <Input 
                prefix={<UserOutlined />} 
                placeholder="请输入用户名"
                disabled={!!userInfo?.username} // 用户名通常不可修改
              />
            </Form.Item>

            <Form.Item
              label="邮箱"
              name="email"
              rules={[
                { required: true, message: '请输入邮箱!' },
                { type: 'email', message: '请输入有效的邮箱地址!' }
              ]}
            >
              <Input placeholder="请输入邮箱地址" />
            </Form.Item>

            <Form.Item
              label="手机号"
              name="phone"
              rules={[
                { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码!' }
              ]}
            >
              <Input placeholder="请输入手机号码" />
            </Form.Item>

            <Form.Item
              label="部门"
              name="department"
            >
              <Input placeholder="请输入所在部门" />
            </Form.Item>

            <Form.Item
              label="职位"
              name="position"
            >
              <Input placeholder="请输入职位" />
            </Form.Item>

            <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
              <Space>
                <Button onClick={onCancel}>
                  取消
                </Button>
                <Button type="primary" htmlType="submit" loading={loading}>
                  保存更改
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </div>
      </div>
    </Modal>
  );
};

export default PersonalInfoEd;