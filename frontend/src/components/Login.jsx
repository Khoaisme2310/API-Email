import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

const Login = () => {
    const [formData, setFormData] = useState({ email: '', password: '' });
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData({ ...formData, [name]: value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            // Mã hóa thông tin đăng nhập bằng Base64
            const credentials = btoa(`${formData.email}:${formData.password}`);
            const response = await axios.post(
                'http://localhost:8080/auth/login',
                {}, // Body trống
                {
                    headers: {
                        'Authorization': `Basic ${credentials}`, // Gửi thông tin Basic Auth
                    },
                }
            );

            const user = response.data; // Lấy thông tin người dùng từ backend

            // Lưu thông tin vào localStorage
            localStorage.setItem('username', user.username || '');
            localStorage.setItem('email', user.email || '');
            localStorage.setItem('userInfo', JSON.stringify(user));

            alert('Login successful!');
            navigate('/dashboard'); // Chuyển hướng sau khi đăng nhập thành công
        } catch (err) {
            setError(err.response?.data || 'Login failed. Please try again.');
        }
    };

    return (
        <div>
            <h2>Login</h2>
            <form onSubmit={handleSubmit}>
                <input
                    name="email"
                    placeholder="Email"
                    value={formData.email}
                    onChange={handleChange}
                    required
                />
                <input
                    name="password"
                    type="password"
                    placeholder="Password"
                    value={formData.password}
                    onChange={handleChange}
                    required
                />
                <button type="submit">Login</button>
            </form>
            {error && <p style={{ color: 'red' }}>{error}</p>}
        </div>
    );
};

export default Login;
