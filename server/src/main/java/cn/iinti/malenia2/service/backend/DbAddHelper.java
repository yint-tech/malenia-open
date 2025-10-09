package cn.iinti.malenia2.service.backend;

import cn.iinti.malenia2.entity.EntityBase;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.dao.DuplicateKeyException;

import java.util.function.Supplier;

public class DbAddHelper {
    public static <T extends EntityBase> void createOrAdd(BaseMapper<T> baseMapper, QueryWrapper<T> uniqueQuery, Supplier<T> creator,
                                                          String dbFieldKey, double addValue
    ) {
        T one = baseMapper.selectOne(uniqueQuery);
        if (one == null) {
            one = creator.get();
            try {
                baseMapper.insert(one);
                return;
            } catch (DuplicateKeyException e) {
                one = baseMapper.selectOne(uniqueQuery);
            }
        }

        String updateExp = addValue > 0 ? "+" + addValue : String.valueOf(addValue);

        baseMapper.update(null, new UpdateWrapper<T>().eq(EntityBase.ID, one.getId())
                .setSql(true, dbFieldKey + "=" + dbFieldKey + " " + updateExp));
    }
}
