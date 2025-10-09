package cn.iinti.malenia2.api.tools;

/**
 * 由系统提供的工具服务，注意不是工具类，而是中间件服务。
 * 如文件资源访问
 */
public interface Api {

    /**
     * 获取资产中心的文件，一般在注入的适合需要获取静态文件
     *
     * @param key
     * @return
     */
    byte[] getResource(String key);

    /**
     * 获取http工具
     *
     * @return http操作工具类
     */
    Http getHttp();

    AsyncHttp getAsyncHttp();

    void log(String msg);

    void log(String msg, Throwable throwable);
}