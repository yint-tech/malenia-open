import {navbar} from "vuepress-theme-hope";

export const zhNavbar = navbar([
    {
        text: "用户手册",
        icon: "fa6-solid:book",
        prefix: "/01_manual/",
        children: [
            {
                text: "产品",
                link: "01_proxy_product.md",
            },
            {
                text: "鉴权",
                link: "02_auth.md",
            }, {
                text: "充值",
                link: "03_balance.md",
            },
            {
                text: "api",
                link: "03_balance.md",
            }, {
                text: "mitm",
                children: [
                    "06_mitm_01_basic", "06_mitm_02_api", "06_mitm_03_demos", "06_mitm_04_foundation",
                ]
            }
        ]
    }, {
        text: "多语言样例",
        icon: "fa6-solid:laptop-code",
        prefix: "/02_code_demo/",
        children: [
            {
                text: "Java",
                link: "01_Java.md",
            },
            {
                text: "Python",
                link: "02_Python.md",
            },
            {
                text: "Go",
                link: "03_Go.md",
            },
            {
                text: "C#",
                link: "04_CSharp.md",
            },
            {
                text: "NodeJs",
                link: "05_NodeJs.md",
            },
            {
                text: "PHP",
                link: "06_PHP.md",
            },
            {
                text: "C++",
                link: "07_CPP.md",
            },

        ]
    },
    {
        text: "管理员",
        icon: "dashicons:admin-users",
        prefix: "/03_admin/",
        children: [
            {
                text: "系统安装",
                link: "01_system_setup",
            },
            {
                text: "定义IP源",
                link: "02_ip_source",
            },
            {
                text: "创建代理产品",
                link: "03_create_product",
            },
            {
                text: "账号管理",
                link: "04_account_manager",
            }, {
                text: "开发者",
                link: "05_develop",
            }, {
                text: "IP源高级",
                link: "06_ip_source_advance",
            }, {
                text: "在线支付",
                link: "07_support_online_charge",
            },
        ]
    }, {
        text: "内部机制模型",
        icon: "dashicons:admin-users",
        prefix: "/04_component/",
        children: [
            {
                text: "池化加速",
                link: "01_speed_up",
            },
            {
                text: "健康评估",
                link: "02_pool_health",
            }, {
                text: "session保持",
                link: "03_user_session",
            },

        ]
    }
]);
