
import React, { useState, useEffect } from "react";
import { Layout, Menu, Avatar, Space, Card } from "antd";
import {
  UserOutlined,
  AppstoreOutlined,
  EditOutlined,
  LogoutOutlined,
} from "@ant-design/icons";
import MyTask from "./myTask.jsx"; // 导入我的任务组件
import Dropdown from "antd/es/dropdown/dropdown.js";
import PersonalInfoEd from "../Services/PersonalInfoEd.jsx";

const { Sider, Content, Header } = Layout;

const WorkerHome = () => {
  const [currentMenu, setCurrentMenu] = useState("my-tasks");
  const [collapsed, setCollapsed] = useState(false);
  // 新增状态：控制个人信息编辑弹窗显示
  const [personalInfoModalVisible, setPersonalInfoModalVisible] =
    useState(false);
  // 新增状态：当前维修工信息 - 从localStorage读取
  const [currentWorker, setCurrentWorker] = useState(() => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      const user = JSON.parse(storedUser);
      return {
        username: user.nickname || user.userId || "维修工",
        userId: user.userId,
        email: user.email || "worker@repair.com",
        phone: user.contactPhone || "",
        department: "维修部",
        position: "维修工",
        role: user.role,
      };
    }
    return {
      username: "维修工",
      email: "worker@repair.com",
      phone: "",
      department: "维修部",
      position: "维修工",
    };
  });

  // 侧边栏菜单配置 - 只有"我的任务"一项
  const sideMenuItems = [
    {
      key: "my-tasks",
      icon: <AppstoreOutlined />,
      label: "我的任务",
    },
  ];
  // 头像下拉菜单项
  const avatarMenuItems = [
    {
      key: "edit-profile",
      icon: <EditOutlined />,
      label: "编辑个人信息",
    },
    {
      type: "divider",
    },
    {
      key: "logout",
      icon: <LogoutOutlined />,
      label: "退出登录",
      danger: true,
    },
  ];

  // 处理菜单点击
  const handleMenuClick = (e) => {
    console.log("点击了菜单:", e.key);
    setCurrentMenu(e.key);
  };

  // 处理头像下拉菜单点击
  const handleAvatarMenuClick = (e) => {
    if (e.key === "edit-profile") {
      console.log("编辑个人信息");
      setPersonalInfoModalVisible(true);
    } else if (e.key === "logout") {
      console.log("退出登录");
      // 清除本地存储
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      // 跳转到登录页面
      window.location.href = '/login';
    }
  };

  //处理个人信息更新
  const handlePersonalInfoUpdate = (updatedInfo) => {
    setCurrentWorker(updatedInfo);
    console.log("更新后的个人信息:", updatedInfo);
  };

  return (
    <Layout style={{ minHeight: "100vh", background: "#FFFFFF" }}>
      {/* 侧边栏 */}
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        style={{
          position: "fixed",
          zIndex: 100,
          height: "100vh",
          left: 0,
          top: 0,
          bottom: 0,
          background: "#0F52BA",
          boxShadow: "none",
        }}
      >
        <div
          style={{
            height: "40px",
            margin: "16px 12px",
            background: "rgba(255, 255, 255, 0.08)",
            borderRadius: "6px",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "white",
            fontWeight: "600",
            fontSize: "15px",
          }}
        >
          {collapsed ? "维修" : "维修工系统"}
        </div>

        <Menu
          selectedKeys={[currentMenu]}
          mode="inline"
          items={sideMenuItems}
          onClick={handleMenuClick}
          style={{
            background: "transparent",
            border: "none",
          }}
          theme="dark"
        />
      </Sider>

      <Layout style={{ marginLeft: collapsed ? 80 : 200, transition: "margin-left 0.2s", background: "#FFFFFF" }}>
        {/* 顶部Header */}
        <Header
          style={{
            padding: "0 24px",
            background: "#FFFFFF",
            borderBottom: "1px solid #e8e8e8",
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            position: "fixed",
            zIndex: 99,
            right: 0,
            top: 0,
            height: "56px",
            boxShadow: "none",
          }}
        >
          <div style={{ fontSize: "15px", fontWeight: "600", color: "#1f1f1f" }}>
            维修工工作台
          </div>

          <Space size="middle">
            <span style={{ fontSize: "15px", color: "#5c5c5c" }}>欢迎，{currentWorker.username}</span>
            <Dropdown
              menu={{
                items: avatarMenuItems,
                onClick: handleAvatarMenuClick,
              }}
              placement="bottomRight"
              arrow
            >
              <Avatar
                size={32}
                icon={<UserOutlined />}
                style={{
                  cursor: "pointer",
                  background: "#0F52BA",
                }}
              />
            </Dropdown>
          </Space>
        </Header>

        {/* 内容区域 */}
        <Content
          style={{
            margin: "76px 20px 20px 20px",
            padding: 0,
            background: "#FFFFFF",
            minHeight: "calc(100vh - 96px)",
          }}
        >
          <div style={{
            background: "#F8FAFC",
            borderRadius: "8px",
            padding: "20px",
            minHeight: "calc(100vh - 136px)",
          }}>
            {currentMenu === "my-tasks" && <MyTask />}
          </div>
        </Content>
      </Layout>

      {/* 个人信息编辑弹窗 */}
      <PersonalInfoEd
        visible={personalInfoModalVisible}
        onCancel={() => setPersonalInfoModalVisible(false)}
        userInfo={currentWorker}
        onUpdate={handlePersonalInfoUpdate}
      />
    </Layout>
  );
};

export default WorkerHome;
