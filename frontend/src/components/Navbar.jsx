import React from 'react';
import { Link } from 'react-router-dom';

const Navbar = () => (
    <nav>
        <ul>
            <li><Link to="/inbox">Inbox</Link></li>
            <li><Link to="/send-email">Send Email</Link></li>
            <li><Link to="/register">Register</Link></li>
            <li><Link to="/login">Login</Link></li>
        </ul>
    </nav>
);

export default Navbar;
