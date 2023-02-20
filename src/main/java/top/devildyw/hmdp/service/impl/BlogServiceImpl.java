package top.devildyw.hmdp.service.impl;

import top.devildyw.hmdp.entity.Blog;
import top.devildyw.hmdp.mapper.BlogMapper;
import top.devildyw.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

}
