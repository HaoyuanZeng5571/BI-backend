package com.yupi.springbootinit.model.vo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yupi.springbootinit.model.entity.Post;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/**
 * 帖子视图
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class PostVO implements Serializable {

    private final static Gson GSON = new Gson();

    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer thumbNum;

    /**
     * 收藏数
     */
    private Integer favourNum;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 创建人信息
     */
    private UserVO user;

    /**
     * 是否已点赞
     */
    private Boolean hasThumb;

    /**
     * 是否已收藏
     */
    private Boolean hasFavour;

    /**
     * 包装类转对象
     *
     * @param chartVO
     * @return
     */
    public static Post voToObj(PostVO chartVO) {
        if (chartVO == null) {
            return null;
        }
        Post chart = new Post();
        BeanUtils.copyProperties(chartVO, chart);
        List<String> tagList = chartVO.getTagList();
        if (tagList != null) {
            chart.setTags(GSON.toJson(tagList));
        }
        return chart;
    }

    /**
     * 对象转包装类
     *
     * @param chart
     * @return
     */
    public static PostVO objToVo(Post chart) {
        if (chart == null) {
            return null;
        }
        PostVO chartVO = new PostVO();
        BeanUtils.copyProperties(chart, chartVO);
        chartVO.setTagList(GSON.fromJson(chart.getTags(), new TypeToken<List<String>>() {
        }.getType()));
        return chartVO;
    }
}
