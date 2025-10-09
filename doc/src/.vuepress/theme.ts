import {hopeTheme} from "vuepress-theme-hope";
import {zhNavbar, zhSidebar} from "./configs";

export default hopeTheme({
    hostname: "https://github.com/yint-tech",

    logo: "/images/logo.png",
    docsDir: "src",

    locales: {
        "/": {
            // navbar
            navbar: zhNavbar,
            // sidebar
            sidebar: zhSidebar,
            footer:
                `因体关联产品 
                 | <a href="http://majora.iinti.cn/majora-doc" target="_blank">Majora</a>
                 | <a href="http://sekiro.iinti.cn/sekiro-doc" target="_blank">Sekiro</a>
                 | <a href="http://malenia.iinti.cn/malenia-doc/index.html" target="_blank">Malenia</a>
`,
            copyright: "版权所有 © 2022-至今 <a href=\"https://iinti.cn\" target=\"_blank\">iinti</a>",
            displayFooter: true,
            // page meta
            metaLocales: {
                editLink: "在 GitHub 上编辑此页",
            },
        },
    },

    plugins: {
        icon: {
            assets: "iconify"
        },
        comment: {
            provider: "Giscus",
            comment: true,
            repo: "yint-tech/iinit-portal-discussions",
            repoId: "R_kgDOK-wbaQ",
            category: "Announcements",
            categoryId: "DIC_kwDOK-wbac4CcItW"
        },
    },
    markdown: {
        codeTabs: true
    }
});
