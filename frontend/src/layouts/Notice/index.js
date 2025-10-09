import React, {useEffect, useState} from "react";
import moment from "moment";
import apis from "apis";

import {createUseStyles, useTheme} from "react-jss";

const useStyles = createUseStyles({
    flexGrow: {
        flexGrow: 1
    },
    notice: {
        position: "absolute",
        top: "32px",
        transform: "translateY(-50%)",
        left: 0,
        right: 0,
        margin: "0 auto",
        fontSize: 12,
        textAlign: "center"
    }
});
/**
 * 本组件将会逐步过期放弃，在malenia开源环境中，不用调用系统授权相关组件，
 * 在node的编译参数增加环境变量ENABLE_AMS_NOTICE后，malenia将会使用下一代的授权组件加载流程。
 * 在ENABLE_AMS_NOTICE的流程中，通过Dom插桩、js插桩的方法预留扩展点，然后交由AMS组件在代码加固过程中植入真正的授权组件逻辑
 */
const Notice = () => {

    const theme = useTheme();
    const classes = useStyles({theme});

    const [intPushMsg, setIntPushMsg] = useState("");
    const [certificate, setCertificate] = useState({});

    useEffect(() => {
        apis.getIntPushMsg().then(res => {
            setIntPushMsg(res);
        }).catch(() => {
        });
        apis.getNowCertificate().then(res => {
            setCertificate(res);
        }).catch(() => {
        });
    }, []);

    const noticeBtn = (
        <div className={classes.notice}>
            {intPushMsg ? (<div>{intPushMsg}</div>) : null}
            {
                certificate.expire ?
                    (<div>
                        过期时间：<strong>{moment(new Date(Number(certificate.expire))).format("YYYY-MM-DD HH:mm")}</strong>、
                        {certificate.user === "0" ? (<strong>试用版本</strong>) :
                            (<p>授权人：<strong>{certificate.user}</strong></p>)
                        }

                    </div>) : <></>
            }

        </div>
    );

    return (
        <div className={classes.flexGrow}>{noticeBtn}</div>
    );
};

export default Notice;
