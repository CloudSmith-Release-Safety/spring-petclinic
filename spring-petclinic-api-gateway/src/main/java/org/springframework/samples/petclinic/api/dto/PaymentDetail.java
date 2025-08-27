package org.springframework.samples.petclinic.api.dto;

import java.util.Date;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class PaymentDetail {

    @JsonProperty("paymentId")
    private String id;

    private int petId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date paymentDate;

    @JsonProperty("totalAmount")
    private Double amount;

    private String notes;
}
