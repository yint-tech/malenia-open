import React from "react";
import SetLoginPassword from "./SetLoginPassword";
import AuthUserConfig from "./AuthUserConfig";
import Base from "./Base"
import {Divider} from "@mui/material";

const UserDashboard = () => {
    return (
        <div>
            <Base/>
            <Divider/>
            <SetLoginPassword/>
            <Divider/>
            <AuthUserConfig/>
        </div>
    );
};
export default UserDashboard;
