#!/bin/bash
echo 'shutdown malenia  server'
now_dir=`pwd`

cd `dirname $0`
script_dir=`pwd`


function getPid(){
    if [[ -f ../conf/pid ]]; then
        current_pid=$(cat ../conf/pid)
        echo $(ps -ef | grep "MaleniaMain" | awk '{print $2}' | grep "${current_pid}")
    else
        echo ""
    fi
}

function prepareShutdown() {
    if [[ -f ../conf/application.properties ]]; then
      work_port=`cat ../conf/application.properties | grep 'server.port' | awk -F "=" '{print $2}'`
      if [[ -n ${work_port} ]] ;then
        counter=0
        while [ $counter -lt 10 ]; do
            response=$(curl -s "http://localhost:${work_port}/malenia-api/system/prepareShutdown?internalAPIKey=@internalAPIKey@")
            if [ $? != 0 ] ; then
                # server status bad
                break
            fi
            if [ "$response" = "0" ]; then
                echo "the malenia service can be shutdown safety"
                break
            else
                echo "waiting malenia service shutdown ${counter} times"
            fi
            ((counter++))
            sleep 10
        done
      fi
    fi
}

remote_pid=`getPid`

echo remote_pid:${remote_pid}
if [[ -n "${remote_pid}" ]] ;then
    # 在停机重启的时候，先发送通知给服务器，让服务器有时间可以做一些收尾工作
    prepareShutdown
    echo kill pid ${remote_pid}
    kill -9 ${remote_pid}
    rm -f ../conf/pid
fi

echo "start malenia server"
sleep 2
remote_pid=`getPid`
if [[ -n "${remote_pid}" ]] ;then
    #   被supervisor自动守护
    exit 0
fi


std_log=../logs
if [[ ! -d ${std_log} ]] ;then
  mkdir ${std_log}
fi

# 本台服务器独立的配置
addition=''
if [[ -f ../conf/addition.txt ]] ;then
   addition=`cat ../conf/addition.txt`
fi
echo "addition param:${addition}"

# 如果存在这个文件，那么证明有新的环境变量需要定义
if [[ -f ../conf/MaleniaMain.rc ]] ;then
    source ../conf/MaleniaMain.rc
fi

nohup sh -c "sh MaleniaMain --LogbackDir=${std_log} ${addition} >> ${std_log}/std.log 2>&1 & echo \$! > ../conf/pid" &

sleep 2

remote_pid=`getPid`

echo "remote pid:${remote_pid}"

