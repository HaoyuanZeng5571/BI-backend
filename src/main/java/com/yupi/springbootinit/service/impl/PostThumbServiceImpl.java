package com.yupi.springbootinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.mapper.PostThumbMapper;
import com.yupi.springbootinit.model.entity.Post;
import com.yupi.springbootinit.model.entity.PostThumb;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.service.PostService;
import com.yupi.springbootinit.service.PostThumbService;
import javax.annotation.Resource;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 帖子点赞服务实现
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Service
public class PostThumbServiceImpl extends ServiceImpl<PostThumbMapper, PostThumb>
        implements PostThumbService {

    @Resource
    private PostService chartService;

    /**
     * 点赞
     *
     * @param chartId
     * @param loginUser
     * @return
     */
    @Override
    public int doPostThumb(long chartId, User loginUser) {
        // 判断实体是否存在，根据类别获取实体
        Post chart = chartService.getById(chartId);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 是否已点赞
        long userId = loginUser.getId();
        // 每个用户串行点赞
        // 锁必须要包裹住事务方法
        PostThumbService chartThumbService = (PostThumbService) AopContext.currentProxy();
        synchronized (String.valueOf(userId).intern()) {
            return chartThumbService.doPostThumbInner(userId, chartId);
        }
    }

    /**
     * 封装了事务的方法
     *
     * @param userId
     * @param chartId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int doPostThumbInner(long userId, long chartId) {
        PostThumb chartThumb = new PostThumb();
        chartThumb.setUserId(userId);
        chartThumb.setPostId(chartId);
        QueryWrapper<PostThumb> thumbQueryWrapper = new QueryWrapper<>(chartThumb);
        PostThumb oldPostThumb = this.getOne(thumbQueryWrapper);
        boolean result;
        // 已点赞
        if (oldPostThumb != null) {
            result = this.remove(thumbQueryWrapper);
            if (result) {
                // 点赞数 - 1
                result = chartService.update()
                        .eq("id", chartId)
                        .gt("thumbNum", 0)
                        .setSql("thumbNum = thumbNum - 1")
                        .update();
                return result ? -1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        } else {
            // 未点赞
            result = this.save(chartThumb);
            if (result) {
                // 点赞数 + 1
                result = chartService.update()
                        .eq("id", chartId)
                        .setSql("thumbNum = thumbNum + 1")
                        .update();
                return result ? 1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        }
    }

}




