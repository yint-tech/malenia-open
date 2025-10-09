package cn.iinti.malenia2.api.mitm.data

import com.google.common.base.Strings
import com.google.common.collect.Maps

class ContentType {

    private String mimeType
    private String charset
    private String mainType
    private String subType

    String getMimeType() {
        return mimeType
    }

    String getCharset() {
        return charset
    }

    String getMainType() {
        return mainType
    }

    String getSubType() {
        return subType
    }
    private final static Map<String, ContentType> cache = Maps.newHashMap()

    static ContentType from(String contentTypeString) {
        if (Strings.isNullOrEmpty(contentTypeString)) {
            ContentType ret = new ContentType()
            ret.mainType = "unknown"
            ret.subType = "unknown"
            ret.mimeType = "unknown/unknown"
            ret.charset = "utf8"
            return ret
        }
        synchronized (cache) {
            ContentType contentType = cache.get(contentTypeString)
            if (contentType != null) {
                return contentType
            }
        }
        // text/html;charset=utf8
        contentTypeString = contentTypeString.toLowerCase()
        int splitterIndex = contentTypeString.indexOf(";")
        ContentType ret = new ContentType()
        if (splitterIndex < 0) {
            ret.mimeType = contentTypeString.trim()
        } else {
            ret.mimeType = contentTypeString.substring(0, splitterIndex)
            ret.charset = contentTypeString.substring(splitterIndex + 1).trim().toLowerCase()

            if (ret.charset.startsWith("charset")) {
                String str = ret.charset.substring("charset".length()).trim()
                if (str.startsWith("=")) {
                    str = str.substring(1)
                }
                ret.charset = str
            }
        }

        //parse mainType & subType

        splitterIndex = ret.mimeType.indexOf("/")
        if (splitterIndex > 0) {
            ret.mainType = ret.mimeType.substring(0, splitterIndex).trim()
            ret.subType = ret.mimeType.substring(splitterIndex + 1).trim()
        } else {
            ret.mainType = ret.mimeType
        }
        synchronized (cache) {
            cache.put(contentTypeString, ret)
        }
        ret
    }

    static Hashtable<String, String> mContentTypes = new Hashtable<String, String>()

    static {
        mContentTypes.put("js", "application/javascript")
        mContentTypes.put("json", "application/json")
        mContentTypes.put("png", "image/png")
        mContentTypes.put("jpg", "image/jpeg")
        mContentTypes.put("html", "text/html")
        mContentTypes.put("css", "text/css")
        mContentTypes.put("mp4", "video/mp4")
        mContentTypes.put("mov", "video/quicktime")
        mContentTypes.put("wmv", "video/x-ms-wmv")
    }

    static String getContentType(String path) {
        String type = tryGetContentType(path)
        if (type != null)
            return type
        return "text/plain"
    }

    static String tryGetContentType(String path) {
        int index = path.lastIndexOf(".")
        if (index != -1) {
            String e = path.substring(index + 1)
            String ct = mContentTypes.get(e)
            if (ct != null)
                return ct
        }
        null
    }
}