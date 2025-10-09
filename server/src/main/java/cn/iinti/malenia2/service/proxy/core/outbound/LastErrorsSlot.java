package cn.iinti.malenia2.service.proxy.core.outbound;

import cn.iinti.malenia2.service.base.trace.utils.ThrowablePrinter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LastErrorsSlot {
    private final ErrorRecord[] lastErrors = new ErrorRecord[10];
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public void appendErrorRecord(String hint) {
        appendErrorRecord(hint, null);
    }

    public void appendErrorRecord(String hint, Throwable error) {
        int index = currentIndex.incrementAndGet();
        lastErrors[index % lastErrors.length] = new ErrorRecord(error, hint);
        if (index > 8192) {
            currentIndex.compareAndSet(index, 0);
        }
    }

    private record ErrorRecord(Throwable error, String hint) {
    }

    private List<ErrorRecord> getLastErrorsInner() {
        List<ErrorRecord> errors = new ArrayList<>();
        for (int i = 0; i < lastErrors.length; i++) {
            ErrorRecord lastError = lastErrors[(i + currentIndex.get()) % lastErrors.length];
            if (lastError == null) {
                break;
            }
            errors.add(lastError);
        }
        return errors;
    }

    public String getLastErrors() {
        List<ErrorRecord> lastErrorsInner = getLastErrorsInner();
        List<String> msgList = new LinkedList<>();
        for (ErrorRecord errorRecord : lastErrorsInner) {
            if (errorRecord.hint != null) {
                msgList.add(errorRecord.hint);
            }
            if (errorRecord.error != null) {
                ThrowablePrinter.printStackTrace(msgList, errorRecord.error);
            }
        }
        return String.join("\n", msgList);
    }

}
