package com.atguigu.gulimall.order;


import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallOrderApplicationTests {

	@Resource
	AmqpAdmin amqpAdmin;
	
	@Resource
	private RabbitTemplate rabbitTemplate;
	
    @Test
    public void contextLoads() {
    	
    }

}
