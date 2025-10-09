package cn.iinti.malenia2.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
public class CommonUtils {
    public static final byte[] UN_SUPPORT_PROTOCOL_MSG = "malenia unknown protocol".getBytes();
    public static final ZoneOffset zoneOffset = ZoneOffset.of("+8");

    public static String throwableToString(Throwable throwable) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (PrintStream printStream = new PrintStream(byteArrayOutputStream)) {
            throwable.printStackTrace(printStream);
        }
        return byteArrayOutputStream.toString();
    }


    public long dateTimeToTimestamp(LocalDateTime ldt) {
        return ldt.toInstant(zoneOffset).toEpochMilli();
    }


    public static File forceMkdir(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IllegalStateException("can not create dir: " + dir.getAbsolutePath());
                //log.warn("can not create dir:{}", dir.getAbsolutePath());
            }
        }
        return dir;
    }
}
