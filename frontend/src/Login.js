import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

const BASE_URL = 'http://localhost:8080/auth/login';

const Login = () => {
    const [formData, setFormData] = useState({ username: '', password: '' });
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await axios.post(BASE_URL, formData);
            localStorage.setItem('username', formData.username); // Lưu trạng thái người đăng nhập
            navigate('/dashboard'); // Chuyển đến Dashboard
        } catch (err) {
            setError(err.response?.data || 'Login failed.');
        }
    };
    const handleLogin = async (e) => {
        e.preventDefault();
        console.log("Login data being sent:", { email, password }); // In ra thông tin login

        try {
            const response = await loginUser({ email, password });
            alert('Login successful!');
            console.log(response);
        } catch (err) {
            setError(err);
        }
    };
    return (
        <div>
            <h2>Login</h2>
            <form onSubmit={handleSubmit}>
                <input
                    name="username"
                    onChange={handleChange}
                    placeholder="Username"
                    required
                />
                <input
                    name="password"
                    type="password"
                    onChange={handleChange}
                    placeholder="Password"
                    required
                />
                <button type="submit">Login</button>
            </form>
            {error && <p style={{ color: 'red' }}>{error}</p>}
        </div>
    );
};

export default Login;
