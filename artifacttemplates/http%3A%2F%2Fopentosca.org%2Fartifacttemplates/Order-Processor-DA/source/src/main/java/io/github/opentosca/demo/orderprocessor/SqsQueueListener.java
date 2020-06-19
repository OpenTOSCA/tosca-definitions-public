package io.github.opentosca.demo.orderprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Component;

@Component
public class SqsQueueListener {

    private static final Logger logger = LoggerFactory.getLogger(SqsQueueListener.class);

    private final OrderRepository orderRepository;

    @Autowired
    public SqsQueueListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @SqsListener(value = "${demo.queue}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void queueListener(Order order) {
        logger.info("Processing order {} in status {}...", order.getId(), order.getStatus());
        logger.info("Order details: {}", order.toString());
        orderRepository.save(order);
        logger.info("Order has been saved to database");
    }
}
