// src/components/MyTask.jsx
import React, { useState, useEffect } from 'react';
import { 
  Table, Tag, Button, Space, Select, Input, 
  Modal, Form,  Card, Row, Col, 
  Statistic, Progress, Descriptions, Image
} from 'antd';
import { SearchOutlined, PlayCircleOutlined, CheckCircleOutlined,
  EditOutlined, EyeOutlined, ClockCircleOutlined,
  ExclamationCircleOutlined, StarOutlined
} from '@ant-design/icons';
import { mytaskService, mytaskUtils } from './mytaskService.jsx';

const { Option } = Select;
const { Search } = Input;

const MyTask = () => {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [stats, setStats] = useState(null);
  const [filters, setFilters] = useState({
    status: 'all',
    category: 'all',
    priority: 'all',
    keyword: '',
  });
  const [startModalVisible, setStartModalVisible] = useState(false);
  const [completeModalVisible, setCompleteModalVisible] = useState(false);
  const [notesModalVisible, setNotesModalVisible] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [selectedTask, setSelectedTask] = useState(null);
  const [startForm] = Form.useForm();
  const [completeForm] = Form.useForm();
  const [notesForm] = Form.useForm();

  // 当前维修工人ID - 从登录信息中获取
  const currentRepairmanId = 1; // 实际项目中应该从用户信息中获取

  // 加载任务列表
  const loadTasks = async (searchFilters = {}) => {
    setLoading(true);
    try {
      const result = await mytaskService.getMyTasks(currentRepairmanId, {
        ...filters,
        ...searchFilters,
      });
      setTasks(result.data);
    } catch (error) {
      console.error('加载任务失败:', error);
      // 错误信息已经在service中显示，这里不需要重复显示
    } finally {
      setLoading(false);
    }
  };

  // 加载统计数据
  const loadStats = async () => {
    try {
      const statsData = await mytaskService.getRepairmanStats(currentRepairmanId);
      setStats(statsData);
    } catch (error) {
      console.error('加载统计数据失败:', error);
    }
  };

  // 处理筛选条件变化
  const handleFilterChange = (key, value) => {
    const newFilters = { ...filters, [key]: value };
    setFilters(newFilters);
    loadTasks(newFilters);
  };

  // 处理关键词搜索
  const handleSearch = (value) => {
    const newFilters = { ...filters, keyword: value };
    setFilters(newFilters);
    loadTasks(newFilters);
  };

  // 打开开始任务模态框
  const handleStartClick = (task) => {
    setSelectedTask(task);
    setStartModalVisible(true);
    startForm.setFieldsValue({
      estimated_completion_time: task.estimated_completion_time || '',
    });
  };

  // 打开完成任务模态框
  const handleCompleteClick = (task) => {
    setSelectedTask(task);
    setCompleteModalVisible(true);
    completeForm.setFieldsValue({
      notes: task.notes || '',
    });
  };

  // 打开更新备注模态框
  const handleNotesClick = (task) => {
    setSelectedTask(task);
    setNotesModalVisible(true);
    notesForm.setFieldsValue({
      notes: task.notes || '',
    });
  };

  // 打开详情模态框
  const handleDetailClick = async (task) => {
    try {
      // 从API获取任务详情
      const taskDetail = await mytaskService.getTaskById(task.id, currentRepairmanId);
      setSelectedTask(taskDetail);
      setDetailModalVisible(true);
    } catch (error) {
      console.error('获取任务详情失败:', error);
    }
  };

  // 处理开始任务
  const handleStartSubmit = async (values) => {
    try {
      await mytaskService.startTask(
        selectedTask.id, 
        currentRepairmanId, 
        values.estimated_completion_time
      );
      setStartModalVisible(false);
      loadTasks(); // 刷新任务列表
      loadStats(); // 刷新统计数据
    } catch (error) {
      console.error('开始任务失败:', error);
      // 错误信息已经在service中显示
    }
  };

  // 处理完成任务
  const handleCompleteSubmit = async (values) => {
    try {
      await mytaskService.completeTask(
        selectedTask.id, 
        currentRepairmanId, 
        values.notes
      );
      setCompleteModalVisible(false);
      loadTasks(); // 刷新任务列表
      loadStats(); // 刷新统计数据
    } catch (error) {
      console.error('完成任务失败:', error);
      // 错误信息已经在service中显示
    }
  };

  // 处理更新备注
  const handleNotesSubmit = async (values) => {
    try {
      await mytaskService.updateTaskNotes(
        selectedTask.id, 
        currentRepairmanId, 
        values.notes
      );
      setNotesModalVisible(false);
      loadTasks(); // 刷新任务列表
    } catch (error) {
      console.error('更新备注失败:', error);
      // 错误信息已经在service中显示
    }
  };

  // 初始化加载数据
  useEffect(() => {
    loadTasks();
    loadStats();
  }, []);

  // 表格列定义
  const columns = [
    {
      title: '任务ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '报修分类',
      dataIndex: 'category',
      key: 'category',
      width: 100,
      render: (category) => {
        const categoryInfo = mytaskUtils.getCategoryInfo(category);
        return categoryInfo.label;
      },
    },
    {
      title: '具体位置',
      dataIndex: 'location',
      key: 'location',
      width: 120,
    },
    {
      title: '问题描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      width: 200,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status, record) => {
        const statusInfo = mytaskUtils.getStatusInfo(status);
        const isOverdue = mytaskUtils.isTaskOverdue(record);
        
        return (
          <Space direction="vertical" size="small">
            <Tag color={statusInfo.color}>{statusInfo.label}</Tag>
            {isOverdue && (
              <Tag color="red" icon={<ExclamationCircleOutlined />}>
                已超时
              </Tag>
            )}
          </Space>
        );
      },
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 80,
      render: (priority) => {
        const priorityInfo = mytaskUtils.getPriorityInfo(priority);
        return <Tag color={priorityInfo.color}>{priorityInfo.label}</Tag>;
      },
    },
    {
      title: '学生信息',
      dataIndex: 'studentName',
      key: 'studentName',
      width: 120,
      render: (name, record) => (
        <div>
          <div>{name}</div>
          <div style={{ fontSize: '12px', color: '#666' }}>
            {record.contactPhone}
          </div>
        </div>
      ),
    },
    {
      title: '分配时间',
      dataIndex: 'assigned_at',
      key: 'assigned_at',
      width: 150,
      render: (time) => time || '未分配',
    },
    {
      title: '预计完成',
      dataIndex: 'estimated_completion_time',
      key: 'estimated_completion_time',
      width: 150,
      render: (time) => time || '未设置',
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_, record) => {
        const isPending = record.status === 'pending';
        const isProcessing = record.status === 'processing';
        
        return (
          <Space size="small" direction="vertical">
            <Space size="small">
              {isPending && (
                <Button 
                  type="primary" 
                  size="small"
                  icon={<PlayCircleOutlined />}
                  onClick={() => handleStartClick(record)}
                >
                  开始处理
                </Button>
              )}
              {isProcessing && (
                <Button 
                  type="primary" 
                  size="small"
                  icon={<CheckCircleOutlined />}
                  onClick={() => handleCompleteClick(record)}
                >
                  完成任务
                </Button>
              )}
              <Button 
                size="small"
                icon={<EditOutlined />}
                onClick={() => handleNotesClick(record)}
              >
                备注
              </Button>
            </Space>
            <Button 
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleDetailClick(record)}
            >
              详情
            </Button>
          </Space>
        );
      },
    },
  ];

  return (
    <div
      style={{ padding: 24, background: '#ffffffff'}}>
      <h2>我的任务</h2>
      
      {/* 统计卡片 */}
      {stats && (
        <Card style={{ marginBottom: 16 }}>
          <Row gutter={16}>
            <Col span={4}>
              <Statistic title="总任务数" value={stats.total} />
            </Col>
            <Col span={4}>
              <Statistic title="待受理" value={stats.pending} valueStyle={{ color: '#faad14' }} />
            </Col>
            <Col span={4}>
              <Statistic title="处理中" value={stats.processing} valueStyle={{ color: '#1890ff' }} />
            </Col>
            <Col span={4}>
              <Statistic title="待评价" value={stats.to_be_evaluated} valueStyle={{ color: '#722ed1' }} />
            </Col>
            <Col span={4}>
              <Statistic title="已完成" value={stats.completed} valueStyle={{ color: '#52c41a' }} />
            </Col>
            <Col span={4}>
              <Statistic 
                title="平均评分" 
                value={stats.averageRating} 
                prefix={<StarOutlined />}
                valueStyle={{ color: '#fadb14' }}
              />
            </Col>
          </Row>
        </Card>
      )}

      {/* 搜索和筛选区域 */}
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col span={6}>
            <div style={{ marginBottom: 8 }}>状态筛选</div>
            <Select
              style={{ width: '100%' }}
              value={filters.status}
              onChange={(value) => handleFilterChange('status', value)}
            >
              <Option value="all">全部状态</Option>
              <Option value="pending">待受理</Option>
              <Option value="processing">处理中</Option>
              <Option value="to_be_evaluated">待评价</Option>
              <Option value="completed">已完成</Option>
            </Select>
          </Col>

          <Col span={6}>
            <div style={{ marginBottom: 8 }}>分类筛选</div>
            <Select style={{ width: '100%' }} value={filters.category}
              onChange={(value) => handleFilterChange('category', value)}
            >
              <Option value="all">全部分类</Option>
              <Option value="waterAndElectricity">水电维修</Option>
              <Option value="networkIssues">网络故障</Option>
              <Option value="furnitureRepair">家具维修</Option>
              <Option value="applianceIssues">电器故障</Option>
              <Option value="publicFacilities">公共设施</Option>
            </Select>
          </Col>

          <Col span={6}>
            <div style={{ marginBottom: 8 }}>优先级筛选</div>
            <Select
              style={{ width: '100%' }}
              value={filters.priority}
              onChange={(value) => handleFilterChange('priority', value)}
            >
              <Option value="all">全部优先级</Option>
              <Option value="high">高</Option>
              <Option value="medium">中</Option>
              <Option value="low">低</Option>
            </Select>
          </Col>

          <Col span={6}>
            <div style={{ marginBottom: 8 }}>关键词搜索</div>
            <Search
              placeholder="搜索位置、描述或学生姓名..."
              allowClear
              enterButton={<SearchOutlined />}
              onSearch={handleSearch}
              onChange={(e) => {
                if (!e.target.value) {
                  handleSearch('');
                }
              }}
            />
          </Col>
        </Row>
      </Card>

      {/* 任务表格 */}
      <Table
        columns={columns}
        dataSource={tasks}
        rowKey="id"
        loading={loading}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total, range) => 
            `第 ${range[0]}-${range[1]} 条，共 ${total} 条`
        }}
        scroll={{ x: 1500 }}
      />

      {/* 开始任务模态框 */}
      <Modal
        title="开始处理任务"
        open={startModalVisible}
        onCancel={() => setStartModalVisible(false)}
        footer={null}
        width={600}
      >
        {selectedTask && (
          <Form
            form={startForm}
            layout="vertical"
            onFinish={handleStartSubmit}
          >
            <Descriptions column={1} bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="任务ID">{selectedTask.id}</Descriptions.Item>
              <Descriptions.Item label="报修位置">{selectedTask.location}</Descriptions.Item>
              <Descriptions.Item label="问题描述">{selectedTask.description}</Descriptions.Item>
              <Descriptions.Item label="学生信息">
                {selectedTask.studentName} ({selectedTask.contactPhone})
              </Descriptions.Item>
            </Descriptions>

            <Form.Item
              label="预计完成时间"
              name="estimated_completion_time"
              rules={[{ required: true, message: '请选择预计完成时间!' }]}
            >
              <Input type="datetime-local" />
            </Form.Item>
            
            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit" icon={<PlayCircleOutlined />}>
                  开始处理
                </Button>
                <Button onClick={() => setStartModalVisible(false)}>
                  取消
                </Button>
              </Space>
            </Form.Item>
          </Form>
        )}
      </Modal>

      {/* 完成任务模态框 */}
      <Modal
        title="完成任务"
        open={completeModalVisible}
        onCancel={() => setCompleteModalVisible(false)}
        footer={null}
        width={600}
      >
        {selectedTask && (
          <Form
            form={completeForm}
            layout="vertical"
            onFinish={handleCompleteSubmit}
          >
            <Descriptions column={1} bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="任务ID">{selectedTask.id}</Descriptions.Item>
              <Descriptions.Item label="报修位置">{selectedTask.location}</Descriptions.Item>
              <Descriptions.Item label="问题描述">{selectedTask.description}</Descriptions.Item>
            </Descriptions>

            <Form.Item
              label="完成备注"
              name="notes"
              rules={[{ required: true, message: '请输入完成备注!' }]}
            >
              <Input.TextArea 
                rows={4} 
                placeholder="请描述维修过程和结果..."
              />
            </Form.Item>
            
            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit" icon={<CheckCircleOutlined />}>
                  确认完成
                </Button>
                <Button onClick={() => setCompleteModalVisible(false)}>
                  取消
                </Button>
              </Space>
            </Form.Item>
          </Form>
        )}
      </Modal>

      {/* 更新备注模态框 */}
      <Modal
        title="更新任务备注"
        open={notesModalVisible}
        onCancel={() => setNotesModalVisible(false)}
        footer={null}
        width={500}
      >
        {selectedTask && (
          <Form
            form={notesForm}
            layout="vertical"
            onFinish={handleNotesSubmit}
          >
            <Form.Item
              label="任务备注"
              name="notes"
            >
              <Input.TextArea 
                rows={4} 
                placeholder="请输入任务备注信息..."
              />
            </Form.Item>
            
            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit">
                  保存备注
                </Button>
                <Button onClick={() => setNotesModalVisible(false)}>
                  取消
                </Button>
              </Space>
            </Form.Item>
          </Form>
        )}
      </Modal>

      {/* 任务详情模态框 */}
      <Modal
        title="任务详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>
        ]}
        width={700}
      >
        {selectedTask && (
          <Descriptions column={2} bordered>
            <Descriptions.Item label="任务ID" span={1}>
              {selectedTask.id}
            </Descriptions.Item>
            <Descriptions.Item label="状态" span={1}>
              <Tag color={mytaskUtils.getStatusInfo(selectedTask.status).color}>
                {mytaskUtils.getStatusInfo(selectedTask.status).label}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="报修分类" span={1}>
              {mytaskUtils.getCategoryInfo(selectedTask.category).label}
            </Descriptions.Item>
            <Descriptions.Item label="优先级" span={1}>
              <Tag color={mytaskUtils.getPriorityInfo(selectedTask.priority).color}>
                {mytaskUtils.getPriorityInfo(selectedTask.priority).label}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="具体位置" span={2}>
              {selectedTask.location}
            </Descriptions.Item>
            <Descriptions.Item label="问题描述" span={2}>
              {selectedTask.description}
            </Descriptions.Item>
            <Descriptions.Item label="学生信息" span={1}>
              {selectedTask.studentName} (学号: {selectedTask.studentID})
            </Descriptions.Item>
            <Descriptions.Item label="联系电话" span={1}>
              {selectedTask.contactPhone}
            </Descriptions.Item>
            <Descriptions.Item label="提交时间" span={1}>
              {selectedTask.created_at}
            </Descriptions.Item>
            <Descriptions.Item label="分配时间" span={1}>
              {selectedTask.assigned_at || '未分配'}
            </Descriptions.Item>
            <Descriptions.Item label="预计完成时间" span={1}>
              {selectedTask.estimated_completion_time || '未设置'}
            </Descriptions.Item>
            <Descriptions.Item label="实际完成时间" span={1}>
              {selectedTask.completed_at || '未完成'}
            </Descriptions.Item>
            <Descriptions.Item label="任务耗时" span={1}>
              {mytaskUtils.calculateTaskDuration(selectedTask) || '未开始'}
            </Descriptions.Item>
            <Descriptions.Item label="是否超时" span={1}>
              {mytaskUtils.isTaskOverdue(selectedTask) ? (
                <Tag color="red" icon={<ExclamationCircleOutlined />}>已超时</Tag>
              ) : (
                <Tag color="green">正常</Tag>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="维修备注" span={2}>
              {selectedTask.notes || '无'}
            </Descriptions.Item>
            {selectedTask.images && selectedTask.images.length > 0 && (
              <Descriptions.Item label="报修图片" span={2}>
                <Image.PreviewGroup>
                  {selectedTask.images.map((img, index) => (
                    <Image
                      key={index}
                      width={100}
                      src={img}
                      placeholder={
                        <div style={{ width: 100, height: 100, background: '#f5f5f5' }} />
                      }
                    />
                  ))}
                </Image.PreviewGroup>
              </Descriptions.Item>
            )}
            {selectedTask.rating && (
              <Descriptions.Item label="用户评分" span={1}>
                <Space>
                  <StarOutlined style={{ color: '#fadb14' }} />
                  {selectedTask.rating} 分
                </Space>
              </Descriptions.Item>
            )}
            {selectedTask.feedback && (
              <Descriptions.Item label="用户反馈" span={2}>
                {selectedTask.feedback}
              </Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Modal>
    </div>
  );
};

export default MyTask;