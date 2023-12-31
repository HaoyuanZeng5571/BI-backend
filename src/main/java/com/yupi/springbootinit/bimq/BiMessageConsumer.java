package com.yupi.springbootinit.bimq;

import com.rabbitmq.client.Channel;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.AiManager;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.service.ChartService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    // 指定程序监听的消息队列和确认机制
    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long delieveryTag) {

        if (StringUtils.isNotBlank(message)) {
            // 消息拒绝
            channel.basicNack(delieveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
        }
        long chartId = Long.parseLong(message);

        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            channel.basicNack(delieveryTag,false,false);

            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"图表为空");
        }
        //先修改图标任务状态为"执行中"， 等执行成功后，修改为"已完成"，保存执行结果； 执行失败后，状态修改为"失败"，记录任务失败信息；
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
        if(!b) {
            channel.basicNack(delieveryTag,false,false);
            handleChartUpdateError(chart.getId(),"更新图标执行中状态失败");
            return;
        }

        //  调用AI
        String result = aiManager.doChat(CommonConstant.BIMODELID, buildUserInput(chart));
        String[] splits = result.split("【【【【【");
        if (splits.length < 3) {
            channel.basicNack(delieveryTag,false,false);
            handleChartUpdateError(chart.getId(),"AI 生成错误");
            return;
        }
        String genChart = splits[1];
        String genResult = splits[2];

        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        updateChartResult.setStatus("succeed");
        boolean b1 = chartService.updateById(updateChartResult);
        if(!b1) {
            channel.basicNack(delieveryTag,false,false);
            handleChartUpdateError(chart.getId(),"更新图标成功状态失败");
        }

        // 消息确认
        log.info("receiveMessage message = {}", message);
        channel.basicAck(delieveryTag,false);
    }

    /**
     * 构造用户输入
     * @param chart
     * @return
     */
    public String buildUserInput(Chart chart) {
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();
        // 用户输入
        StringBuilder userInput = new StringBuilder();
        //系统预设
        userInput.append("分析需求：").append("\n");
        //拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "," + "请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append("数据：").append(csvData).append("\n");
        return userInput.toString();
    }

    private void handleChartUpdateError(Long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage(execMessage);
        boolean b = chartService.updateById(updateChartResult);
        if (!b) {
            log.info("更新图标失败状态失败" + chartId);
        }
    }

}
