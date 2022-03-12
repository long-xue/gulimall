package io.renren.modules.job.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @ClassName knowledgeTask
 * @Description TODO
 * @Author lonng-xue
 * @Date 2021/12/17/017 21:45
 **/

@Component("knowledgeTask")
public class knowledgeTask implements ITask {
    private Logger logger = LoggerFactory.getLogger(getClass());
    /*
     * @Author long-xue
     * @Description 定時爬取數據
     * @Param [params]
     * @Return void
     **/
    @Override
    public void run(String params) {
        logger.debug("定時爬取數據任务正在执行，参数为：{}", params);
    }
}
