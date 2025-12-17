const BASE_URL = "http://localhost:8080/api";

const request = async (endpoint, options = {}) => {
  const token = localStorage.getItem("token");

  const headers = { ...options.headers };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  if (!(options.body instanceof FormData) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  const url = `${BASE_URL}${endpoint}`;
  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let errorMessage = `请求失败: ${response.status}`;
    const contentType = response.headers.get("content-type");

    if (contentType && contentType.includes("application/json")) {
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
      } catch (e) {
        console.warn("无法解析 JSON 错误响应:", e);
      }
    } else {
      try {
        const text = await response.text();
        console.warn("非 JSON 响应:", text.substring(0, 100));
        errorMessage = `服务器返回了非 JSON 数据 (${response.status})`;
      } catch (e) {
        console.warn("无法读取错误响应文本:", e);
      }
    }

    if (response.status === 401) {
      console.warn("Token 已过期或无效");
    }

    throw new Error(errorMessage);
  }
  const contentType = response.headers.get("content-type");
  if (!contentType || !contentType.includes("application/json")) {
    const text = await response.text();
    console.warn("API 返回了非 JSON 数据:", text.substring(0, 200));
    throw new SyntaxError("服务器返回了非 JSON 数据");
  }
  return response.json();
};

const toQueryString = (params) => {
  if (!params) return "";
  const searchParams = new URLSearchParams();
  Object.keys(params).forEach((key) => {
    if (
      params[key] !== undefined &&
      params[key] !== null &&
      params[key] !== ""
    ) {
      searchParams.append(key, params[key]);
    }
  });
  const str = searchParams.toString();
  return str ? `?${str}` : "";
};

const api = {
  request,
  auth: {
    login: ({ userId, password }) =>
      request("/auth/login", {
        // 修正路径，添加 /auth 前缀
        method: "POST",
        body: JSON.stringify({ userId, password }),
      }),
    register: (userData) =>
      request("/auth/register", {
        method: "POST",
        body: JSON.stringify(userData),
      }),
  },

  users: {
    getMe: () => request("/users/me"), // 修正路径，添加 /users 前缀

    //用户信息更新（含头像）
    // TODO: 需要后端 UserController 提供 PUT /api/users/me 接口
    updateMe: (formData) =>
      request("/users/me", {
        method: "PUT",
        body: formData,
      }),

    // 使用已有的 GET /api/users?role=STUDENT 接口替代需要新增的接口
    getStudents: (params) => {
      const queryParams = { ...params, role: "STUDENT" };
      return request(`/users${toQueryString(queryParams)}`);
    },

    // 使用已有的 GET /api/users?role=REPAIRMAN 接口替代需要新增的接口
    getRepairmen: (params) => {
      const queryParams = { ...params, role: "REPAIRMAN" };
      return request(`/users${toQueryString(queryParams)}`);
    },
  },

  student: {
    createOrder: (formData) =>
      request("/repair-orders", {
        method: "POST",
        body: formData,
      }),

    getMyOrders: (params) =>
      request(`/repair-orders/my${toQueryString(params)}`),

    getOrderDetail: (id) => request(`/repair-orders/${id}`),

    deleteOrder: (id) =>
      request(`/repair-orders/${id}`, {
        method: "DELETE",
      }),

    // 修正评价接口数据结构，后端接收的是 score 和 comment 字段
    evaluateOrder: (id, data) =>
      request(`/repair-orders/${id}/evaluate`, {
        method: "POST",
        body: JSON.stringify({
          score: data.rating, // 前端传入的 rating 映射为后端的 score
          comment: data.feedback, // 前端传入的 feedback 映射为后端的 comment
        }),
      }),
  },

  repairman: {
    getMyTasks: (params) => request(`/tasks/my${toQueryString(params)}`),

    // 修正 startTask 调用，使用后端已有的 status 更新接口
    startTask: (id, estimatedCompletionTime) =>
      request(`/tasks/${id}/status`, {
        method: "PUT",
        body: JSON.stringify({
          status: "PROCESSING", // 设置状态为处理中
          estimatedCompletionTime: estimatedCompletionTime, // 预计完成时间
        }),
      }),

    completeTask: (id, notes) =>
      request(`/tasks/${id}/complete`, {
        method: "PUT", // 后端是 PUT 方法
        body: JSON.stringify({
          status: "COMPLETED", // 明确指定完成状态
          processNotes: notes, // 备注信息
        }),
      }),

    getTaskDetail: (id) => request(`/tasks/${id}`),
  },

  admin: {
    // 使用后端 AdminController 提供的接口
    getAllOrders: (params) =>
      request(`/admin/repair-orders${toQueryString(params)}`),

    // 修正分配工单接口，使用后端已有的接口
    assignOrder: (orderId, repairmanId) =>
      request(`/admin/repair-orders/${orderId}/assign`, {
        method: "PUT", // 后端是 PUT 方法
        body: JSON.stringify({ repairmanId }), // 确保字段名与后端一致
      }),

    // 修正驳回工单接口，使用通用状态更新接口
    rejectOrder: (orderId, reason) =>
      request(`/admin/repair-orders/${orderId}/status`, {
        method: "PUT",
        body: JSON.stringify({
          status: "REJECTED", // 设置为已驳回状态
          processNotes: reason, // 驳回原因作为处理备注
        }),
      }),

    
    updateUser: (id, phone) =>
      request(`/admin/users/${id}`, {
        method: "PUT",
        body: JSON.stringify({ phone }),
      }),

    resetPassword: (id) =>
      request(`/admin/users/${id}/reset-password`, {
        method: "POST",
      }),

    getAllFeedbacks: (params) =>
      request(`/admin/feedbacks${toQueryString(params)}`),

    deleteFeedback: (id) =>
      request(`/admin/feedbacks/${id}`, {
        method: "DELETE",
      }),

    getStatsCategory: () => request("/admin/stats/category"),

    //TODO: 需要后端AdminController 添加 GET /api/admin/stats/location
    getStatsLocation: () => request("/admin/stats/location"),

    //TODO: 需要后端AdminController 添加 GET /api/admin/stats/repairman-rating
    getStatsRepairmanRating: () => request("/admin/stats/repairman-rating"),
  },

  common: {
    uploadImages: (files) => {
      const formData = new FormData();
      if (Array.isArray(files)) {
        files.forEach((file) => formData.append("files[]", file));
      } else {
        formData.append("files[]", files);
      }

      return request("/upload/images", {
        method: "POST",
        body: formData,
      });
    },
  },
};

export default api;
