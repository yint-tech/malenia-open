# Malenia

代理转发网关平台,使用文档: http://malenia.iinti.cn/malenia-doc/


# 构建和运行
## 本地调试
- 后端
  - 启动docker环境，自动拉起mysql数据库：``server/src/main/resource/develop-malenia/docker-compose-local.yaml``
  - 启动后端：``server/src/main/java/cn/iinti/malenia2/MaleniaMain.java``
- 前端：运行``frontend/package.json``即可
- 文档：运行``doc/package.json``即可
## 生产构建
安装jdk17，并且确保您的电脑生效的java版本为jdk17
- Windows ：``gradlew.bat distZip``
- 非Windows：``./gradlew distZip``

成功构建后，产出文件为：``server/build/distributions/MaleniaMain-2.2.zip``

## 许可

* 本项目由因体公司维护，项目代码完全开源
  > 本仓库为因体公司内部仓库镜像而来，然而内部仓库和社区相比仅仅增加一些和公司相关的部署配置文件
* 本项目仅对非商业用途免费
  * 您可以免费下载项目代码，用于学习研究、个人使用、公司临时(一个月以内)使用
  * 如果您公司/组织，经修改本项目构建了代理销售系统，则您需要联系我们购买对应授权
  * 如果您公司/组织，在公司内部使用本项目构建代理网关控制系统，则您需要联系我们购买对应授权
  * 获得授权之后您可以随意修改和定制本系统，也可以随时获得我们的商业支持，包括但不限于定制开发建议、问题排查、升级和迁移等