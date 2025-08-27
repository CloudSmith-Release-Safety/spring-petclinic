package org.springframework.samples.petclinic.api.dto;

import java.util.Date;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class PaymentDetail {

    private String id;
    
    // Support new response format field names
    @JsonProperty("paymentId")
    private String paymentId;

    private int petId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date paymentDate;

    private Double amount;
    
    // Support new response format field names
    @JsonProperty("totalAmount") 
    private Double totalAmount;

    private String notes;
}
