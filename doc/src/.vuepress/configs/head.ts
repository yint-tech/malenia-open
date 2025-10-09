import type {HeadConfig} from "@vuepress/core";

export const head: HeadConfig[] = [
    ["meta", {name: "application-name", content: "Malenia"}],
    ["meta", {name: "apple-mobile-web-app-title", content: "Malenia"}],
    ["meta", {name: "apple-mobile-web-app-status-bar-style", content: "black"}],
    ["meta", {name: "msapplication-TileColor", content: "#3eaf7c"}],
    ["meta", {name: "theme-color", content: "#3eaf7c"}],
    ["meta", {name: "keywords", content: "因体,iinti"}],
    ["meta", {name: "description", content: "适合小团体的java全栈开发脚手架"}],
    [
        "script",
        {
            src: "/malenia-doc/js/load_notice.js"
        }
    ],
    [
        "link",
        {
            rel: "stylesheet", href: "/malenia-doc/css/malenia.css"
        }
    ]

];
