# malenia项目开放sdk

这是malenia项目内置的sdk模块，用于给代理网关系统进行编程，项目为java工程，扩展能力使用groovy语言，
用户需要简单了解groovy语言之后才能深入运用本sdk。

- 为了获得编程提示，请使用java的编译器打开本工程（如idea）
- 相关的demo在``src/test/groovy``目录下，用户可以自定在demo基础上编辑
- sdk内容本身不可更改，他和malenia正式发布版本绑定，用户仅能修改``src/test/groovy``下的资源，其他模块代码修改无法应用到生产（sdk本身在malenia服务器中被定义，此处只是一个copy）

## MITM

进行流量编辑

## IP源自定义

管理员使用，用户解析或者下载代理IP资源，适配多种不同的IP供应商