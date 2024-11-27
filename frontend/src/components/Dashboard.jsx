import React from 'react';
import { Link } from 'react-router-dom';

const Dashboard = () => {
    const username = localStorage.getItem('username');

    if (!username) {
        return <p>You need to login first.</p>;
    }

    return (
        <div>
            <h2>Welcome, {username}</h2>
            <Link to="/send-mail">
                <button>Send Mail</button>
            </Link>
            <Link to="/inbox">
                <button>Inbox</button>
            </Link>
            <Link to="/send-mail-with-attachment">
                <button>Send Mail with Attachment</button>
            </Link>
        </div>
    );
};

export default Dashboard;
