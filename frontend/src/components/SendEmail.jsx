import React, { useState, useEffect } from 'react';
import axios from 'axios';

const SendMail = () => {
    const [formData, setFormData] = useState({
        senderUsername: '',
        recipient: '',
        subject: '',
        body: '',
        type: 'internal',
    });
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        // Lấy thông tin từ localStorage
        const email = localStorage.getItem('email');
        const username = localStorage.getItem('username');
        if (!email || !username) {
            alert('You are not logged in. Redirecting to login page...');
            window.location.href = '/login'; // Chuyển hướng nếu không có thông tin
        } else {
            setFormData((prev) => ({ ...prev, senderUsername: username })); // Cập nhật username vào formData
        }
    }, []);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        try {
            const credentials = btoa(`${localStorage.getItem('email')}:${localStorage.getItem('password')}`);

            if (formData.type === 'internal') {
                // Gửi email nội bộ
                await axios.post('http://localhost:8080/email/send', {
                    senderUsername: formData.senderUsername,
                    receiverUsername: formData.recipient,
                    subject: formData.subject,
                    content: formData.body,
                }, {
                    headers: {
                        'Authorization': `Basic ${credentials}`, // Basic Auth trong headers
                    }
                });
            } else {
                // Gửi email ra ngoài
                await axios.post('http://localhost:8080/email/sendgmail', {
                    senderUsername: formData.senderUsername,
                    receiverUsername: formData.recipient,
                    subject: formData.subject,
                    content: formData.body,
                }, {
                    headers: {
                        'Authorization': `Basic ${credentials}`,
                    }
                });
            }
            alert('Email sent successfully!');
        } catch (error) {
            console.error('Error sending email:', error);
            alert(`Failed to send email: ${error.response?.data || error.message}`);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <h2>Send Email</h2>
            {loading ? (
                <p>Sending email...</p>
            ) : (
                <form onSubmit={handleSubmit}>
                    <input
                        name="recipient"
                        placeholder="Recipient (username or email)"
                        value={formData.recipient}
                        onChange={handleChange}
                        required
                    />
                    <input
                        name="subject"
                        placeholder="Subject"
                        value={formData.subject}
                        onChange={handleChange}
                        required
                    />
                    <textarea
                        name="body"
                        placeholder="Body"
                        value={formData.body}
                        onChange={handleChange}
                        required
                    />
                    <select name="type" value={formData.type} onChange={handleChange}>
                        <option value="internal">Internal</option>
                        <option value="external">External</option>
                    </select>
                    <button type="submit">Send</button>
                </form>
            )}
        </div>
    );
};

export default SendMail;
