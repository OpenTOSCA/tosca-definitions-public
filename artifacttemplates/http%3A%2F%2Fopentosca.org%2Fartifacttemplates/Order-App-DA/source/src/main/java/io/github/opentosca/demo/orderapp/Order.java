package io.github.opentosca.demo.orderapp;

import java.util.Date;

import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @NotBlank(message = "This is a required field")
    private String orderNo;

    private Date timestamp;

    private Status status = Status.PREPARING;

    public enum Status {
        PAYMENT_EXPECTED, PAID, PREPARING, READY, TAKEN;
    }
}
