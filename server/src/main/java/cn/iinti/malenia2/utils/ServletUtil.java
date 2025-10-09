package cn.iinti.malenia2.utils;

import cn.iinti.malenia2.entity.CommonRes;
import com.alibaba.fastjson.JSON;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class ServletUtil {
    public static void response(Consumer<HttpServletResponse> consumer) {
        serve((attributes -> {
            consumer.accept(attributes.getResponse());
        }));
    }

    public static void serve(Consumer<ServletRequestAttributes> consumer) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes attributes) {
            consumer.accept(attributes);
        } else {
            throw new IllegalStateException("can not get ServletRequestAttributes from RequestContextHolder");
        }
    }

    public static void writeRes(CommonRes<?> commonRes) {
        response((response) -> {
            response.setContentType("application/json;charset=utf8");
            try {
                ServletOutputStream outputStream = response.getOutputStream();
                outputStream.write(JSON.toJSONBytes(commonRes));
                outputStream.close();
            } catch (IOException e) {
                log.warn("writeRes error", e);
            }
        });

    }


    public interface FileUploadAction<R> {
        R doAction(File file);
    }

    public static <R> R uploadToTempNoCheck(MultipartFile multipartFile, FileUploadAction<R> action) {
        try {
            return uploadToTemp(multipartFile, action);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static <R> R uploadToTemp(MultipartFile multipartFile, FileUploadAction<R> action) throws IOException {
        File file = Files.createTempFile("upload", ".bin").toFile();
        try {
            multipartFile.transferTo(file);
            return action.doAction(file);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    public static void responseFile(File file, String contentType) {
        responseFile(file, contentType, true);
    }

    public static void responseFile(File file, String contentType, boolean download) {
        if (file == null || !file.canRead()) {
            writeRes(CommonRes.failed("system error,filed retrieve failed"));
            return;
        }
        response((httpServletResponse) -> {
            if (httpServletResponse.isCommitted()) {
                // 文件同步可能需要时间，这个过程如果用户已经断开连接了
                return;
            }

            httpServletResponse.setCharacterEncoding("UTF-8");
            if (download) {
                httpServletResponse.setHeader("Content-Disposition", "attachment;filename=" + file.getName());
            }
            httpServletResponse.setHeader("Content-length", String.valueOf(file.length()));
            httpServletResponse.setContentType(contentType);

            try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(httpServletResponse.getOutputStream())) {
                IOUtils.copy(Files.newInputStream(file.toPath()), bufferedOutputStream);
            } catch (IOException e) {
                // 此时已经在写数据了,不能再返回其他数据
                log.error("write download file error", e);
            }
        });
    }

    public static void writeStatus(HttpStatus code, String msg) {
        response((response) -> {
            response.setStatus(code.value());
            response.setContentType("text/plain;charset=utf8");
            try {
                ServletOutputStream outputStream = response.getOutputStream();
                outputStream.write(msg.getBytes(StandardCharsets.UTF_8));
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    //断点续传
    public static void resumeDownload(File file, String contentType, String range) {
        if (file == null || !file.canRead()) {
            writeStatus(HttpStatus.SERVICE_UNAVAILABLE, "system error,filed retrieve failed");
            return;
        }

        response((httpServletResponse) -> {
            long fileLength = file.length();
            long start = 0;
            long end = fileLength - 1;

            //检查 Range 请求头
            if (range != null && range.startsWith("bytes=")) {
                String[] partsOfFile = range.substring("bytes=".length()).split("-");
                start = Long.parseLong(partsOfFile[0]);
            }

            if (httpServletResponse.isCommitted()) {
                //检查 响应包 未被提交
                return;
            }
            httpServletResponse.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            httpServletResponse.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            httpServletResponse.setHeader("Accept-Ranges", "bytes");
            httpServletResponse.setHeader("Content-length", String.valueOf(fileLength));
            httpServletResponse.setContentType(contentType);

            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
                 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(httpServletResponse.getOutputStream())) {
                byte[] buffer = new byte[4096];
                int bytesWrite = 0;
                randomAccessFile.seek(start);

                while ((bytesWrite = randomAccessFile.read(buffer)) != -1) {
                    bufferedOutputStream.write(buffer, 0, bytesWrite);
                    start += bytesWrite;
                }
            } catch (IOException e) {
                log.error("resume download error", e);
            }
        });
    }
    public static Map<String, String> paramsToMap(HttpServletRequest request) {
        Map<String, String> params = new HashMap<String, String>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        return params;
    }
}
