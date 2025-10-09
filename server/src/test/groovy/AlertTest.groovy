import cn.iinti.malenia2.service.base.alert.EventScript
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

// 此文件用于对实现自定义的系统webhook
// 用户可以通过配置扩展脚本的方式实现系统内部变脸和外部逻辑的交互
// 一个常用的场景是通过本脚本实现一些报警功能

@BaseScript EventScript _base
def DD_WEBHOOK = "https://oapi.dingtalk.com/robot/send?access_token=xxxxToken"

onSensitiveOperation {
    def json = new JsonBuilder()
    json {
        msgtype "text"
        text {
            content "【敏感接口访问】${message}"
        }
    }
    // 用户的敏感接口操作，通过webhook发送到钉钉机器人
    httpPost DD_WEBHOOK, json
}

onDiskPoor {
    def json = new JsonBuilder()
    json {
        msgtype "text"
        text {
            content "【磁盘不足】总大小： ${totalSpace()} 当前可用:${freeSpace()}"
        }
    }
    // 用户的敏感接口操作，通过webhook发送到钉钉机器人
    httpPost DD_WEBHOOK, json
}

// 监听metric，使用mql语法，是一种通用的感知系统内部指标进行报警的机制
mqlSubscribe {
    id "磁盘用量报警"
    mql """
可用空间 = metric(disk.free);
总空间 = metric(disk.total);

可用空间 = aggregate(可用空间,'serverId','path'); 
总空间 = aggregate(总空间,'serverId','path')

磁盘使用率 = ((总空间 - 可用空间) * 100) / 总空间;

可用空间 = 可用空间/1048576; 
总空间 = 总空间/1048576; 

show(可用空间,总空间,磁盘使用率);
"""
    cron "0 9 * * * ?"
    onMetric {
        if (getMetricValue("磁盘使用率") < 24) {
            def json = new JsonBuilder()
            json {
                msgtype "text"
                text {
                    content "【磁盘不足】总大小： ${getMetricValue("总空间")} 当前可用:${getMetricValue("可用空间")}"
                }
            }
            // 用户的敏感接口操作，通过webhook发送到钉钉机器人
            httpPost DD_WEBHOOK, json
            return
        }
    }
}