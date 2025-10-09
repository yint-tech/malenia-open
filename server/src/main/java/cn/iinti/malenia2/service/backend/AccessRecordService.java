package cn.iinti.malenia2.service.backend;

import cn.iinti.malenia2.MaleniaMain;
import cn.iinti.malenia2.entity.AccessRecord;
import cn.iinti.malenia2.entity.AccessRecordHistory;
import cn.iinti.malenia2.mapper.AccessRecordHistoryMapper;
import cn.iinti.malenia2.mapper.AccessRecordMapper;
import cn.iinti.malenia2.service.base.config.Settings;
import cn.iinti.malenia2.service.base.env.Environment;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AccessRecordService {
    @Resource
    private AccessRecordHistoryMapper accessRecordHistoryMapper;

    @Resource
    private AccessRecordMapper accessRecordMapper;

    @Scheduled(cron = "30 15 * * * *")
    public void scheduleAccessRecordTask() {
        if (Environment.isLocalDebug) {
            return;
        }
        // 向前推进一个小时，这样才能得到完整数据
        LocalDateTime saveHour = LocalDateTime.now().minusHours(1);

        AccessRecordCollector.fetchAccessRecords(saveHour,
                value ->
                        // 这里要切换线程，因为AccessRecordCollector里面的线程虽然也是异步的，
                        // 但是AccessRecordCollector的线程将会持有session对象，session持有了大量网络相关数据结构，如果AccessRecordCollector的线程在排队，则可能导致网络相关的组件无法GC
                        // 而网络组件持有了大量的共享内存buffer资源，一旦网络组件延迟gc了，就会导致内存无法完成回收，这样很容易导致内存溢出
                        //
                        // 此处历史上就因为db数据跑太久导致的db压力大，最后导致的上述问题，引发了雪崩
                        MaleniaMain.getShardThread().post(() -> {
                            saveAccessRecord(value.v);
                            // 对于超过7天的访问记录，移动到新表，对于7天之前的记录，只备份，不提供访问接口
                            // 访问记录主要是用来实现审计
                            // 要不然会卡mysql
                            backup();
                        }));
    }

    @Scheduled(cron = "30 35 4 * * *")
    public void scheduleCleanLongDaysAgo() {
        Integer value = Settings.maxAccessRecordSaveDays.value;
        value = Math.max(value, 1);
        value = Math.min(value, 5000);

        while (true) {
            int removeCount = accessRecordHistoryMapper.delete(new QueryWrapper<AccessRecordHistory>()
                    .le(AccessRecordHistory.CREATE_TIME, LocalDateTime.now().minusDays(value))
                    .last("limit 2048"));
            if (removeCount <= 0) {
                break;
            }
        }
    }

    private void backup() {
        LocalDateTime localDateTime = LocalDateTime.now().minusDays(7);
        while (true) {
            List<AccessRecord> list = accessRecordMapper.selectList(new QueryWrapper<AccessRecord>()
                    .lt(AccessRecord.CREATE_TIME, localDateTime).last("limit 256"));
            if (list.isEmpty()) {
                break;
            }

            for (AccessRecord accessRecord : list) {
                AccessRecordHistory accessRecordHistory = new AccessRecordHistory();
                BeanUtils.copyProperties(accessRecord, accessRecordHistory);
                accessRecordHistory.setId(null);
                try {
                    accessRecordHistoryMapper.insert(accessRecordHistory);
                } catch (DuplicateKeyException ignore) {
                } catch (DataAccessException dataAccessException) {
                    log.warn("dataAccessException", dataAccessException);
                }
            }
            accessRecordMapper.deleteBatchIds(list.stream().map(AccessRecord::getId).collect(Collectors.toList()));
        }
    }

    void saveAccessRecord(Collection<AccessRecordCollector.AccessRecordData> accessRecordDataCollection) {
        for (AccessRecordCollector.AccessRecordData accessRecordData : accessRecordDataCollection) {
            try {
                DbAddHelper.createOrAdd(accessRecordMapper,
                        new QueryWrapper<AccessRecord>().eq(AccessRecord.RECORD_MD5, accessRecordData.getMd5()),
                        () -> transform(accessRecordData),
                        AccessRecord.ACCESS_COUNT,
                        accessRecordData.getCount()
                );
            } catch (Throwable throwable) {
                log.error("error", throwable);
            }
        }
    }


    private AccessRecord transform(AccessRecordCollector.AccessRecordData accessRecordData) {
        AccessRecord accessRecord = new AccessRecord();
        accessRecord.setAccessCount(accessRecordData.getCount());
        accessRecord.setAccessUser(accessRecordData.getUser());
        accessRecord.setRecordMd5(accessRecordData.getMd5());
        accessRecord.setTargetHost(accessRecordData.getTargetHost());
        accessRecord.setRecordTime(accessRecordData.getHour());
        return accessRecord;
    }
}
