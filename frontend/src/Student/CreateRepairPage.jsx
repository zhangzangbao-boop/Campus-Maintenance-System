import React, { useState } from 'react';
import {
  Card,
  Form,
  Input,
  Select,
  Button,
  Upload,
  message
} from 'antd';
import {
  UploadOutlined
} from '@ant-design/icons';
import { repairService } from '../services/repairService';

const { Option } = Select;
const { TextArea } = Input;

const CreateRepairPage = ({ currentUser, onSubmitSuccess }) => {
  const [form] = Form.useForm();
  const [fileList, setFileList] = useState([]);
  const [submitting, setSubmitting] = useState(false);

  // 处理文件上传
  const handleUploadChange = ({ fileList: newFileList }) => {
    setFileList(newFileList);
  };

  // 处理文件删除
  const handleRemove = (file) => {
    const newFileList = fileList.filter(item => item.uid !== file.uid);
    setFileList(newFileList);
  };

  // 处理表单提交
  const handleFormSubmit = async (values) => {
    setSubmitting(true);
    try {
      // 创建一个包含所有字段的对象
      const orderData = {
        ...values,
        studentId: currentUser?.studentID,
        locationText: values.location,
        categoryId: getCategoryID(values.category)
      };

      console.log('提交的报修数据:', orderData);
      
      // 调用服务创建报修
      const newOrder = await repairService.createRepairOrder(orderData, fileList);
      console.log('创建报修成功:', newOrder);
      
      message.success('报修申请提交成功！');
      
      // 重置表单
      form.resetFields();
      setFileList([]);
      
      // 通知父组件刷新数据
      if (onSubmitSuccess) {
        onSubmitSuccess();
      }
    } catch (error) {
      console.error('提交报修申请失败:', error);
      message.error('提交报修申请失败，请重试！');
    } finally {
      setSubmitting(false);
    }
  };

  // 获取分类ID
  const getCategoryID = (category) => {
    const categoryMap = {
      waterAndElectricity: 1,
      networkIssues: 2,
      furnitureRepair: 3,
      applianceIssues: 4,
      publicFacilities: 5
    };
    return categoryMap[category] || 1;
  };

  // 上传组件配置
  const uploadProps = {
    beforeUpload: (file) => {
      const isImage = file.type.startsWith('image/');
      if (!isImage) {
        message.error('只能上传图片文件!');
        return Upload.LIST_IGNORE;
      }
      
      const isLt5M = file.size / 1024 / 1024 < 5;
      if (!isLt5M) {
        message.error('图片大小不能超过5MB!');
        return Upload.LIST_IGNORE;
      }
      
      return false;
    },
    fileList,
    onChange: handleUploadChange,
    onRemove: handleRemove,
    multiple: true,
    accept: 'image/*',
  };

  return (
    <div
      style={{
        padding: "20px 0",
      }}
    >
      <Card
        title={<span style={{ fontSize: "16px", fontWeight: "600", color: "#1f1f1f" }}>创建报修申请</span>}
        style={{
          width: "100%",
          maxWidth: "800px",
          margin: "0 auto",
          boxShadow: "0 2px 8px rgba(15, 82, 186, 0.06)",
          borderRadius: "8px",
          border: "none",
          background: "#FFFFFF",
        }}
        headStyle={{
          background: "#FFFFFF",
          borderBottom: "1px solid #e8e8e8",
          padding: "16px 20px",
        }}
        styles={{ body: {
          padding: "20px",
          background: "#F8FAFC",
        }}}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleFormSubmit}
          autoComplete="off"
          style={{
            minHeight: "min-content",
          }}
        >
          <Form.Item
            label={<span style={{ fontSize: "15px", fontWeight: "500", color: "#1f1f1f" }}>报修标题</span>}
            name="title"
            rules={[{ required: true, message: "请输入报修标题!" }]}
          >
            <Input placeholder="请输入报修标题" />
          </Form.Item>

          <Form.Item
            label={<span style={{ fontSize: "15px", fontWeight: "500", color: "#1f1f1f" }}>报修分类</span>}
            name="category"
            rules={[{ required: true, message: "请选择报修分类!" }]}
          >
            <Select placeholder="请选择报修分类">
              <Option value="waterAndElectricity">水电维修</Option>
              <Option value="networkIssues">网络故障</Option>
              <Option value="furnitureRepair">家具维修</Option>
              <Option value="applianceIssues">电器故障</Option>
              <Option value="publicFacilities">公共设施</Option>
            </Select>
          </Form.Item>

          <Form.Item
            label={<span style={{ fontSize: "15px", fontWeight: "500", color: "#1f1f1f" }}>具体位置</span>}
            name="location"
            rules={[{ required: true, message: "请输入具体位置!" }]}
          >
            <Input placeholder="例如：3栋502寝室" />
          </Form.Item>

          <Form.Item
            label={<span style={{ fontSize: "15px", fontWeight: "500", color: "#1f1f1f" }}>问题描述</span>}
            name="description"
            rules={[{ required: true, message: "请输入问题描述!" }]}
          >
            <TextArea
              rows={6}
              placeholder="请详细描述您遇到的问题..."
              maxLength={500}
              showCount
            />
          </Form.Item>

          <Form.Item
            label={<span style={{ fontSize: "15px", fontWeight: "500", color: "#1f1f1f" }}>紧急程度</span>}
            name="priority"
            rules={[{ required: true, message: '请选择紧急程度!' }]}
            initialValue="low"
          >
            <Select placeholder="请选择紧急程度">
              <Option value="low">低</Option>
              <Option value="medium">中</Option>
              <Option value="high">高</Option>
            </Select>
          </Form.Item>

          <Form.Item
            label={<span style={{ fontSize: "15px", fontWeight: "500", color: "#1f1f1f" }}>上传相关图片</span>}
            extra={<span style={{ fontSize: "13px", color: "#8c8c8c" }}>支持上传多张图片，每张图片大小不超过5MB</span>}
          >
            <Upload
              {...uploadProps}
              listType="picture"
              showUploadList={{
                showPreviewIcon: true,
                showRemoveIcon: true,
              }}
            >
              <Button icon={<UploadOutlined />}>选择图片</Button>
            </Upload>
          </Form.Item>

          <Form.Item
            style={{
              textAlign: "center",
              marginTop: "24px",
              marginBottom: 0,
              flexShrink: 0,
            }}
          >
            <Button
              type="primary"
              htmlType="submit"
              loading={submitting}
              style={{
                width: "140px",
                marginRight: "16px",
              }}
            >
              提交报修申请
            </Button>
            <Button
              style={{
                width: "100px",
              }}
              onClick={() => {
                form.resetFields();
                setFileList([]);
              }}
            >
              重置
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default CreateRepairPage;