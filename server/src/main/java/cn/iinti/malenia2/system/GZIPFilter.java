package cn.iinti.malenia2.system;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.zip.GZIPInputStream;

@Slf4j
@Component
public class GZIPFilter implements Filter {

    private static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String CONTENT_ENCODING_TYPE = "gzip";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String encodeType = httpServletRequest.getHeader(CONTENT_ENCODING);
        if (CONTENT_ENCODING_TYPE.equalsIgnoreCase(encodeType)) {
            UnZIPRequestWrapper unZIPRequestWrapper = new UnZIPRequestWrapper(httpServletRequest);
            filterChain.doFilter(unZIPRequestWrapper, servletResponse);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Bean
    public FilterRegistrationBean<GZIPFilter> gzipFilterRegistrationBean() {
        FilterRegistrationBean<GZIPFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(this);
        registration.setName("gzipFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }


    public static class UnZIPRequestWrapper extends HttpServletRequestWrapper {
        private final ServletInputStreamWrapper servletInputStreamWrapper;

        /**
         * Constructs a request object wrapping the given request.
         *
         * @param request The request to wrap
         * @throws IllegalArgumentException if the request is null
         */
        public UnZIPRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            servletInputStreamWrapper = new ServletInputStreamWrapper(request.getInputStream());
        }

        @Override
        public ServletInputStream getInputStream() {
            return servletInputStreamWrapper;
        }
    }

    private static class ServletInputStreamWrapper extends ServletInputStream {

        private final GZIPInputStream gzipInputStream;

        private final ServletInputStream servletInputStream;

        private ServletInputStreamWrapper(ServletInputStream servletInputStream) throws IOException {
            this.servletInputStream = servletInputStream;
            this.gzipInputStream = new GZIPInputStream(servletInputStream);
        }

        @Override
        public boolean isFinished() {
            return servletInputStream.isFinished();
        }

        @Override
        public boolean isReady() {
            return servletInputStream.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            servletInputStream.setReadListener(listener);
        }

        ///// for GZIPInputStream  /////
        @Override
        public int read(@NotNull byte[] buf, int off, int len) throws IOException {
            return gzipInputStream.read(buf, off, len);
        }

        @Override
        public void close() throws IOException {
            gzipInputStream.close();
        }

        @Override
        public int read() throws IOException {
            return gzipInputStream.read();
        }

        @Override
        public int available() throws IOException {
            return gzipInputStream.available();
        }

        @Override
        public long skip(long n) throws IOException {
            return gzipInputStream.skip(n);
        }

        @Override
        public boolean markSupported() {
            return gzipInputStream.markSupported();
        }

        @Override
        public void mark(int readlimit) {
            gzipInputStream.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            gzipInputStream.reset();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return gzipInputStream.read(b);
        }
    }
}
