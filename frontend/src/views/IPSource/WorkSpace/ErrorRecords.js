import React, {useContext, useEffect, useState} from "react";
import {AppContext} from "adapter";
import ScrollBar from "react-perfect-scrollbar";

const ErrorRecords = (props) => {
    const {api} = useContext(AppContext);
    useEffect(() => {
        api.lastErrorRecords4IpSource({
            ipSourceKey: props.ipSourceKey
        }).then((res) => {
            setErrorText(res.data)
        })
    }, [props.ipSourceKey]);

    const [errorText, setErrorText] = useState("")
    return (
        <pre>
            {errorText}
        </pre>
    );
}

export default ErrorRecords;