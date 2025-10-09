function loadNotice() {
    var docNoticeDiv = document.getElementById("docNotice");
    if (docNoticeDiv) {
        let docNotice = window['_docNotice'];
        if (docNotice) {
            docNoticeDiv.innerHTML = docNotice;
        } else {
            const xhr = new XMLHttpRequest();
            const url = "/malenia-api/system/docNotice";
            xhr.onreadystatechange = function () {
                if (this.readyState === 4 && this.status === 200) {
                    window['_docNotice'] = JSON.parse(xhr.responseText).data;
                    docNoticeDiv.innerHTML = window['_docNotice'];
                }
            };
            xhr.open("GET", url);
            xhr.send();
        }
    }
}

function maleniaFix() {
    //console.log("xxxxx");
    /* 如果域名不是 malenia.iinti.cn，那么说明不是主网站，需要讲域名进行修改 */
    let needReplaceHost = window.location.host !== 'malenia.iinti.cn';
    let host = window.location.host;
    if (host.indexOf(':') > 0) {
        host = host.slice(0, host.indexOf(':'));
    }
    let username, password;
    /*  如果用户设置了密码，那么把代理账号密码设置为真实的账号密码 */
    let maleniaUser = localStorage.getItem('Malenia-USER');
    if (maleniaUser) {
        let maleniaUserObj = JSON.parse(maleniaUser);
        username = maleniaUserObj['authAccount'] ? maleniaUserObj['authAccount'] : maleniaUserObj['userName'];
        password = maleniaUserObj['authPwd'] ? maleniaUserObj['authPwd'] : maleniaUserObj['passwd'];
    } else {
        // 提示需要完成登陆
        let contentDiv = document.getElementsByClassName('vp-page-title');
        if (contentDiv && contentDiv.length > 0) {
            contentDiv = contentDiv[0];
            let loginA = document.createElement('a');
            loginA.setAttribute("href", "/index.html");
            loginA.setAttribute("target", "_blank");
            loginA.setAttribute("style", "margin-bottom:20px; display:block;font-size:3em;color:red;")

            loginA.innerText = "完成登陆后查看文档效果更佳(点击登陆)";
            contentDiv.prepend(loginA);
        }
    }
    let needReplaceAuthAccount = username && password;
    if (!needReplaceAuthAccount && !needReplaceHost) {
        return;
    }


    function escapeRegExp(string) {
        return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); // $& means the whole matched string
    }

    function replaceAll(str, match, replacement) {
        return str.replace(new RegExp(escapeRegExp(match), 'g'), () => replacement);
    }

    function handleReplace(els) {
        for (let i in els) {
            let spanContent = els[i].innerHTML;
            if (!spanContent) {
                continue;
            }

            //console.log("spanContent :" + spanContent);
            if (needReplaceHost && spanContent.indexOf('malenia.iinti.cn') >= 0) {
                spanContent = replaceAll(spanContent, 'malenia.iinti.cn', host);
            }
            if (needReplaceAuthAccount && spanContent.indexOf('yourProxyAccount') >= 0) {
                spanContent = replaceAll(spanContent, 'yourProxyAccount', username);
            }
            if (needReplaceAuthAccount && spanContent.indexOf('yourProxyPassword') >= 0) {
                spanContent = replaceAll(spanContent, 'yourProxyPassword', password);
            }

            els[i].innerHTML = spanContent;
        }

    }

    let spans = document.getElementsByTagName('span');
    handleReplace(spans);
    let codes = document.getElementsByTagName('code');
    handleReplace(codes);
}

window.onload = function () {
    maleniaFix();
    loadNotice();
}
