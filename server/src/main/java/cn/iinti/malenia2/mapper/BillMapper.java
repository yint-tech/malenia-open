package cn.iinti.malenia2.mapper;

import cn.iinti.malenia2.entity.Bill;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 对账单，记录各个用户和产品的使用详情，按小时为维度进行统计 Mapper 接口
 * </p>
 *
 * @author yint
 * @since 2024-08-12
 */
public interface BillMapper extends BaseMapper<Bill> {

}
