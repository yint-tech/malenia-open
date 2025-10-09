---
home: true
title: Malenia代理分销系统
icon: fa6-solid:house
heroImage: /images/logo.png
actions:

  - text: 开始使用
    link: /01_manual/01_proxy_product.html
    type: primary
  - text: 管理员部署
    link: /03_admin/01_system_setup.html
    type: secondary

highlights:
  - header: ip资源收口
    description: 分割代理采购和代理使用，使用ip资源的人看不到采购的账号资产、ip来源，保护资产资源的安全。
    features:
      - title: 监控和报表
        details: ip资源使用可观测、可控制、可扩展。 malenia可记录所有用户的网站访问记录，用以审查操作合规性
      - title: 子账户扩展
        details:  malenia可以无限开辟新的代理子账户，并且每个代理账户拥有自己的授权空间，方便业务方进行二次的子业务二次分发
      - title: 统一和全面的接入规范
        details: 支持完整的http、https、socks5协议，并且实现了这几种协议的自动转换（如https接入，socks5协议走出）。支持丰富和完善的鉴权标准：账户密码、cidr ip来源网段鉴权、通过鉴权携带控制指令等
      - title: 计费和容量控制
        details: 流量或者带宽的使用容量控制，提供完整账单功能

  - header: IP池化
    description: malenia一个复杂的ip池系统，在这个ip池系统构建复杂的策略特性，让你可以优化ip池的使用性能
    features:
      - title: 多ip供应商资源软切
        details: 代理流量分流到不同的ip供应商，且支持动态调控多个上游ip资源池的流量比例。调整过程接入业务无感知
      - title: 失败重试
        details: 在连接创建过程进程ip路由切换，切换时间成本毫秒级，业务可做到无感知的失败兜底
      - title: 多种池化路由策略
        details: 经纬度地理定位最近策略、国家城市范围固定策略、随机策略、用户自主控制策略等
      - title: 池化健康检测
        details: malenia首创IP池化健康状态探测机制，他可以自动识别IP资源池是否发生故障、高压、不可用等情况，并且自动的实现资源下线、兜底、熔断等策略。

  - header: 中间人攻击
    description: malenia在IP池化基础之上提供了中间人攻击拦截的能力。在代理过程可以实现报文的拦截、篡改等，同时拦截篡改过程依然保证高性能
    features:
      - title: 扩展脚本
        details: 使用groovy脚本语法，语言简介，表达力强
      - title: 扩展性能
        details: 基于netty浅层包装，mitm过程依然保持异步IO特性，扩展过程几乎影响系统吞吐
      - title: 池化+mitm
        details: 业界第一个同时支持IP池化+中间人攻击（mitm）的方案，此方案可以给给群控方向（浏览器群控、手机群控）的技术带来创新
      - title: mitm流程跟踪
        details: malenia提供了基于websocket的web调试控制台，同时提供了定制化的代理日志trace组件，可以方便的对mitm过程进行采样观测

  - header: 可靠和性能
    description: malenia深度使用netty，全程考虑性能优化和架构合理性，具备极大的高可用、高吞吐、高性能保障。
    features:
      - title: 异步和NIO
        details: malenia系统的代理核心全程使用NIO的异步事件机制驱动，这代表malenia系统可以达到服务器的性能吞吐上限，即有多大带宽便可以跑多大并发。
      - title: 一致性安全保证
        details: 系统运行机制全程无锁化驱动，从本质上根绝高并发系统可能带来的锁冲突和一致性问题。malenia在netty基础上根据自己业务特性设计了自身事件循环机制
      - title: 避免包聚合
        details: 大部分http网络组件都会聚合完整http报文在进行业务处理，malenia则全程考虑包聚合问题，流量经过系统不停留即转发出系统。转发过程充分利用DMA（即数据转发无CPU干预）
      - title: 内存泄漏可靠性
        details: 对于一个高并发高吞吐系统，内存泄漏风险不可避免，普通业务的一个简单延迟gc问题在malenila都可能带来雪崩。malenia则通过长达4年时间的运行和内存分析，几乎干掉了所有可能的内存泄漏逻辑隐患。
---

<div id="docNotice"></div>

### 一键安装malenia

```bash
# 服务器:启动服务器
# 服务器： 安装docker
yum install -y docker
# 服务器： 下载镜像: 
docker pull registry.cn-beijing.aliyuncs.com/iinti/common:malenia-all-in-one-latest
# 服务器：启动malenia服务器
docker run -d -p 5810:8060 -p 24000-24100:24000-24100 -v ~/m-mysql-data:/var/lib/mysql registry.cn-beijing.aliyuncs.com/iinti/common:malenia-all-in-one-latest

```