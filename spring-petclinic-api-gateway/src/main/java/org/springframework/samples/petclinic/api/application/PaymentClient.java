/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.samples.petclinic.api.application;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import org.springframework.samples.petclinic.api.dto.PaymentAdd;
import org.springframework.samples.petclinic.api.dto.PaymentDetail;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PaymentClient {
    private final WebClient.Builder webClientBuilder;

    public Flux<PaymentDetail> getPayments(final int ownerId, final int petId) {
        return webClientBuilder.build().get()
                .uri("http://payment-service/owners/{ownerId}/pets/{petId}/payments", ownerId, petId)
                .retrieve()
                .bodyToFlux(PaymentDetail.class)
                .map(this::normalizePaymentResponse);
    }

    public Mono<PaymentDetail> getPaymentById(final int ownerId, final int petId, final String paymentId) {
        return webClientBuilder.build().get()
                .uri("http://payment-service/owners/{ownerId}/pets/{petId}/payments/{paymentId}", ownerId, petId,
                        paymentId)
                .retrieve()
                .bodyToMono(PaymentDetail.class)
                .map(this::normalizePaymentResponse);
    }

    public Mono<PaymentDetail> addPayment(final int ownerId, final int petId, final PaymentAdd paymentAdd) {
        return webClientBuilder.build().post()
                .uri("http://payment-service/owners/{ownerId}/pets/{petId}/payments", ownerId, petId)
                .body(Mono.just(paymentAdd), PaymentAdd.class)
                .retrieve()
                .bodyToMono(PaymentDetail.class)
                .map(this::normalizePaymentResponse);
    }

    /**
     * Normalize payment response to handle both old and new field formats
     * Supports transition from old format {id, amount} to new format {paymentId, totalAmount}
     */
    private PaymentDetail normalizePaymentResponse(PaymentDetail payment) {
        // Handle new response format if present
        if (payment.getId() == null && payment.getPaymentId() != null) {
            payment.setId(payment.getPaymentId());
        }
        if (payment.getAmount() == null && payment.getTotalAmount() != null) {
            payment.setAmount(payment.getTotalAmount());
        }
        return payment;
    }

    public Mono<PaymentDetail> cleanPaymentTable() {
        return webClientBuilder.build().delete()
                .uri("http://payment-service/clean-db")
                .retrieve()
                .bodyToMono(PaymentDetail.class);
    }
}
