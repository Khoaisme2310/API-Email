import React, { useState } from "react";
import axios from "axios";

const SendMailWithAttachment = () => {
    const [emailData, setEmailData] = useState({
        senderUsername: localStorage.getItem("username"),
        receiverUsername: "",
        subject: "",
        content: "",
    });
    const [file, setFile] = useState(null);
    const [error, setError] = useState("");

    const handleChange = (e) => {
        const { name, value } = e.target;
        setEmailData({ ...emailData, [name]: value });
    };

    const handleFileChange = (e) => {
        const selectedFile = e.target.files[0];
        if (selectedFile) {
            setFile(selectedFile);
            setError("");
        }
    };

    const handleSend = async () => {
        if (!emailData.receiverUsername || !emailData.subject || !emailData.content) {
            setError("Please fill in all required fields.");
            return;
        }

        const formData = new FormData();
        formData.append("senderUsername", emailData.senderUsername);
        formData.append("receiverUsername", emailData.receiverUsername);
        formData.append("subject", emailData.subject);
        formData.append("content", emailData.content);
        if (file) {
            formData.append("file", file);
        }

        try {
            await axios.post("http://localhost:8080/email/sendfile", formData, {
                headers: {
                    "Content-Type": "multipart/form-data",
                },
            });
            alert("Email sent successfully!");
        } catch (err) {
            setError("Error sending email. Please try again.");
        }
    };

    return (
        <div>
            <h2>Send Email with Attachment</h2>
            <input
                type="text"
                name="receiverUsername"
                placeholder="Receiver Email/Username"
                value={emailData.receiverUsername}
                onChange={handleChange}
                required
            />
            <input
                type="text"
                name="subject"
                placeholder="Subject"
                value={emailData.subject}
                onChange={handleChange}
                required
            />
            <textarea
                name="content"
                placeholder="Email Content"
                value={emailData.content}
                onChange={handleChange}
                required
            />
            <input type="file" onChange={handleFileChange} />
            {file && <p>Selected File: {file.name}</p>}
            {error && <p style={{ color: "red" }}>{error}</p>}
            <button onClick={handleSend}>Send Email</button>
        </div>
    );
};

export default SendMailWithAttachment;
