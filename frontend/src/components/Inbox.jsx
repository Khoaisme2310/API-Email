import React, { useEffect, useState } from "react";
import { getInbox } from "../services/api";

const Inbox = () => {
    const [emails, setEmails] = useState([]);

    useEffect(() => {
        const fetchEmails = async () => {
            try {
                const username = localStorage.getItem("username");
                const response = await getInbox(username);
                setEmails(response);
            } catch (error) {
                console.error("Error fetching inbox:", error);
            }
        };

        fetchEmails();
    }, []);

    const handleDownload = async (emailId, attachmentName) => {
        try {
            const response = await fetch(`/email/download/${emailId}`);
            if (!response.ok) {
                throw new Error("Failed to download the file");
            }
            const blob = await response.blob();
            const link = document.createElement('a');
            link.href = URL.createObjectURL(blob);
            link.download = attachmentName;  // Tên file tải về
            link.click();
        } catch (error) {
            console.error("Error downloading file:", error);
        }
    };

    return (
        <div>
            <h2>Inbox</h2>
            <ul>
                {emails.map((email, index) => (
                    <li key={index}>
                        <p><strong>From:</strong> {email.senderUsername}</p>
                        <p><strong>Subject:</strong> {email.subject}</p>
                        <p><strong>Content:</strong> {email.content}</p>
                        {email.hasAttachment && (
                            <div>
                                <p><strong>Attachment:</strong> {email.attachmentName}</p>
                                <button onClick={() => handleDownload(email.id, email.attachmentName)}>
                                    Download
                                </button>
                            </div>
                        )}
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default Inbox;
