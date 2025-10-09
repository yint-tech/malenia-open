package cn.iinti.malenia2.service.proxy.core.ssl;

/**
 * Thrown when load or create ssl context error
 */
public class SSLContextException extends RuntimeException {
    public SSLContextException(Throwable cause) {
        super(cause);
    }
}
