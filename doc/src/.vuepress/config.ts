import {defineUserConfig} from 'vuepress'
import {head} from "./configs";
import theme from "./theme.js";
import viteBundler from "@vuepress/bundler-vite";

export default defineUserConfig({
    base: "/malenia-doc/",
    head,
    lang: 'zh-CN',
    title: 'malenia',
    description: '代理ip网关平台',
    bundler: viteBundler(),
    theme
})