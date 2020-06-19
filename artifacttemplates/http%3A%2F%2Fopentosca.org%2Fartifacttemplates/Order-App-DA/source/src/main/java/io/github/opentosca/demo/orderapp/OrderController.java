package io.github.opentosca.demo.orderapp;

import java.util.Date;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final SqsQueueSender queueSender;

    @Autowired
    public OrderController(SqsQueueSender queueSender) {
        this.queueSender = queueSender;
    }

    @GetMapping("/")
    public String showIndex(Model model) {
        model.addAttribute("order", new Order());
        return "index";
    }

    @PostMapping("/create-order")
    public String addUser(@Valid Order order, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "index";
        }
        order.setTimestamp(new Date());
        order.setStatus(Order.Status.READY);
        queueSender.send(order);
        model.addAttribute("order", order);
        logger.info("Order has been sent: {}", order);
        return "index";
    }
}
