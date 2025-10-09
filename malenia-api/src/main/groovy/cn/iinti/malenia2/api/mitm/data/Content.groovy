package cn.iinti.malenia2.api.mitm.data

import cn.iinti.malenia2.api.mitm.http.HttpMessage
import com.alibaba.fastjson.JSON
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.charset.UnsupportedCharsetException

class Content {
    public Object o

    public final HttpMessage message

    Content(HttpMessage message) {
        this.message = message
    }

    JSON json() {
        if (o instanceof JSON) {
            return (JSON) o
        }
        if (o == null) {
            o = JSON.parse(string())
            return (JSON) o
        }

        if (o instanceof String) {
            o = JSON.parse((String) o)
            return (JSON) o
        }

        if (o instanceof byte[]) {
            o = JSON.parse(new String(o as byte[]))
        }

        throw new IllegalStateException("unknown mode: " + o.getClass().getName())
    }

    Document jsoup() {
        if (o instanceof Document) {
            return (Document) o
        }
        if (o == null) {
            String htmlContent = string().trim()
            o = Jsoup.parse(htmlContent, message.getUrl())
            return (Document) o
        }

        if (o instanceof String) {
            o = Jsoup.parse((String) o, message.getUrl())
            return (Document) o
        }
        throw new IllegalStateException("unknown mode: " + o.getClass().getName())
    }


    String string() {
        if (o == null) {
            o = createString()
            return (String) o
        }
        if (o instanceof String) {
            return (String) o
        }
        if (o instanceof JSON) {
            return o.toString()
        }
        if (o instanceof Document) {
            return o.toString()
        }
        throw new IllegalStateException("unknown mode: " + o.getClass().getName());
    }


    Content setModel(Object model) {
        o = model
        this
    }


    /**
     * 在body中注入js，期待他是一个js文件或者html<br/>
     * 1. 如果原body是js文件，那么注入数据放到前面
     * 2. 如果原body是html文件，那么注入数据放到head的script标签中
     * @param jsData 注入的js内容
     * @return
     */
    Content injectJs(Object jsData, boolean force = false) {
        ContentType contentType = getContentType()
        boolean canSafeInject = true
        if (contentType == null) {
            canSafeInject = false
        } else if ("text/html" != contentType.getMimeType()
                && "application/javascript" != contentType.getMimeType()) {
            canSafeInject = false
        }
        if (!canSafeInject && !force) {
            return this
        }

        if (jsData instanceof byte[]) {
            jsData = new String((byte[]) jsData)
        }

        if ((contentType != null && "text/html" == contentType.getMimeType())
                || string().trim().startsWith("<!DOCTYPE html>")
        ) {
            Document document = jsoup()
            document
                    .head()
                    .prependElement("script")
                    .attr("type", "application/javascript")
                    .appendChild(new DataNode(jsData.toString()))
        } else {
            // js和其他模式，直接转化为string
            setModel(jsData + ";\n" + string())
        }
        this
    }

    /**
     * 框架在用户callback调用完成之后会检查状态，并调用这个方法抽取数据
     * @return
     */
    byte[] serialize() {
        if (o == null) {
            return message.getBytes()
        }
        if (o instanceof byte[]) {
            return (byte[]) o
        }
        try {
            def charset = getContentType().getCharset()
            if (charset == null) {
                charset = "utf8"
            }
            return o.toString().getBytes(charset)
        } catch (UnsupportedEncodingException ignored) {
            return o.toString().getBytes(StandardCharsets.UTF_8)
        }
    }


    ContentType getContentType() {
        ContentType.from(message.getHeaders().get("Content-Type"))
    }

    private String createString() {
        byte[] content = message.getBytes()
        if (content == null) {
            return null
        }
        Charset charset = StandardCharsets.UTF_8
        ContentType contentType = getContentType()
        if (contentType != null) {
            var charsetStr = contentType.getCharset()
            if (charsetStr != null) {
                try {
                    charset = Charset.forName(charsetStr)
                } catch (UnsupportedCharsetException ignore) {
                }
            }
        }
        String str = new String(content, charset)
        if (str.startsWith(UTF8_BOM)) {
            str = str.substring(1)
        }
        return str
    }

    private static final String UTF8_BOM = "\uFEFF"
}



