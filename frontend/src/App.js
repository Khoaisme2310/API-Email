import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Home from "./components/Home";
import Login from "./components/Login";
import Register from "./components/Register";
import Dashboard from "./components/Dashboard";
import SendMail from "./components/SendEmail";
import SendMailWithAttachment from "./components/SendMailWithAttachment";
import Inbox from "./components/Inbox";

const App = () => {
    return (
        <Router>
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/send-mail" element={<SendMail />} />
                <Route path="/send-mail-with-attachment" element={<SendMailWithAttachment />} />
                <Route path="/inbox" element={<Inbox />} />
            </Routes>
        </Router>
    );
};

export default App;
