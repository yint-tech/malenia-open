import {sidebar} from "vuepress-theme-hope";

export const zhSidebar = sidebar({
    "/01_manual/": [
        "01_proxy_product.md", "02_auth.md", "03_balance.md", "05_api",
        {
            text: "mitm",
            children: [
                "06_mitm_01_basic.md", "06_mitm_02_api.md", "06_mitm_03_demos.md", "06_mitm_04_foundation.md",
            ]
        }
    ], "/02_code_demo": [
        "", "01_Java.md", "02_Python.md", "03_Go.md", "04_CSharp.md", "05_NodeJs.md", "06_PHP.md", "07_CPP.md",
    ],
    "/03_admin": [
        "01_system_setup.md", "02_ip_source.md", "03_create_product.md", "04_account_manager.md", "05_develop.md",
        "06_ip_source_advance.md", "07_support_online_charge.md"
    ],
    "/04_component": [
        "01_speed_up.md", "02_pool_health.md", "03_user_session.md"
    ],
});
