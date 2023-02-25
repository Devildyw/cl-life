package top.devildyw.cl_dianping.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.devildyw.cl_dianping.core.entity.SeckillVoucher;
import top.devildyw.cl_dianping.core.mapper.SeckillVoucherMapper;
import top.devildyw.cl_dianping.core.service.ISeckillVoucherService;


/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:35
 */
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

}
