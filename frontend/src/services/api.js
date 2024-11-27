import axios from 'axios';

const BASE_URL = 'http://localhost:8080';

const api = axios.create({
    baseURL: BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Hàm tạo Basic Auth header
const getAuthHeaders = (email, password) => {
    const credentials = btoa(`${email}:${password}`);
    return {
        'Authorization': `Basic ${credentials}`,
    };
};

// Đăng ký tài khoản
export const registerUser = async (userData) => {
    try {
        const response = await api.post('/auth/register', userData);
        return response.data;
    } catch (error) {
        throw error.response?.data || error.message;
    }
};

// Đăng nhập
export const loginUser = async (email, password) => {
    try {
        const response = await api.post('/auth/login', {}, {
            headers: getAuthHeaders(email, password),
        });
        return response.data;
    } catch (error) {
        throw error.response?.data || error.message;
    }
};

// Gửi email nội bộ
export const sendInternalEmail = async (emailData) => {
    try {
        const response = await api.post('/email/send', emailData, {
            headers: getAuthHeaders(localStorage.getItem('email'), localStorage.getItem('password')),
        });
        return response.data;
    } catch (error) {
        throw error.response?.data || error.message;
    }
};

// Gửi email qua Gmail
export const sendExternalEmail = async (emailData) => {
    try {
        const response = await api.post('/email/sendgmail', emailData, {
            headers: getAuthHeaders(localStorage.getItem('email'), localStorage.getItem('password')),
        });
        return response.data;
    } catch (error) {
        throw error.response?.data || error.message;
    }
};

// Lấy danh sách email trong inbox
export const getInbox = async (receiverUsername) => {
    try {
        const response = await api.get('/email/inbox', {
            params: { receiverUsername },
            headers: getAuthHeaders(localStorage.getItem('email'), localStorage.getItem('password')),
        });
        return response.data;
    } catch (error) {
        throw error.response?.data || error.message;
    }
};
export const sendEmailWithAttachment = async (emailData, file) => {
    const formData = new FormData();
    formData.append("senderUsername", emailData.senderUsername);
    formData.append("receiverUsername", emailData.receiverUsername);
    formData.append("subject", emailData.subject);
    formData.append("content", emailData.content);
    if (file) {
        formData.append("file", file);
    }

    try {
        const response = await api.post("/email/sendfile", formData, {
            headers: {
                "Content-Type": "multipart/form-data",
            },
        });
        return response.data;
    } catch (error) {
        throw error.response?.data || error.message;
    }
};
export default api;
