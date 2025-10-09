package cn.iinti.malenia2.api.mitm.http

interface HttpVersion extends Comparable<HttpVersion> {
    String getProtocolName()

    int getMajorVersion()

    int getMinorVersion()

    String getText()

    boolean isKeepAliveDefault()
}
