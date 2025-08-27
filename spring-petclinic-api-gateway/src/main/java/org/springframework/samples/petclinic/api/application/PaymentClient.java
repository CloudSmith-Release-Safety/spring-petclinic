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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.samples.petclinic.api.dto.PaymentAdd;
import org.springframework.samples.petclinic.api.dto.PaymentDetail;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentClient {
    private final WebClient.Builder webClientBuilder;
    
    @Value("${payment.service.jwt.token:}")
    private String jwtToken;

    /**
     * Get payments with JWT authentication support
     * Updated to handle new authentication requirements from payment service v7.0.0
     */
    public Flux<PaymentDetail> getPayments(final int ownerId, final int petId) {
        return webClientBuilder.build().get()
                .uri("http://payment-service/owners/{ownerId}/pets/{petId}/payments", ownerId, petId)
                .headers(headers -> {
                    if (jwtToken != null && !jwtToken.isEmpty()) {
                        headers.setBearerAuth(jwtToken);
                    }
                })
                .retrieve()
                .onStatus(HttpStatus.UNAUTHORIZED::equals, this::handleAuthenticationError)
                .bodyToFlux(PaymentDetail.class)
                .onErrorResume(WebClientResponseException.class, this::handlePaymentServiceError);
    }

    /**
     * Get payment by ID with JWT authentication
     * Updated for new authentication error handling
     */
    public Mono<PaymentDetail> getPaymentById(final int ownerId, final int petId, final String paymentId) {
        return webClientBuilder.build().get()
                .uri("http://payment-service/owners/{ownerId}/pets/{petId}/payments/{paymentId}", ownerId, petId, paymentId)
                .headers(headers -> {
                    if (jwtToken != null && !jwtToken.isEmpty()) {
                        headers.setBearerAuth(jwtToken);
                    }
                })
                .retrieve()
                .onStatus(HttpStatus.UNAUTHORIZED::equals, this::handleAuthenticationError)
                .bodyToMono(PaymentDetail.class)
                .onErrorResume(WebClientResponseException.class, this::handlePaymentServiceError);
    }

    /**
     * Add payment with JWT authentication
     * Updated to handle new authentication requirements
     */
    public Mono<PaymentDetail> addPayment(final int ownerId, final int petId, final PaymentAdd paymentAdd) {
        return webClientBuilder.build().post()
                .uri("http://payment-service/owners/{ownerId}/pets/{petId}/payments", ownerId, petId)
                .headers(headers -> {
                    if (jwtToken != null && !jwtToken.isEmpty()) {
                        headers.setBearerAuth(jwtToken);
                    }
                })
                .body(Mono.just(paymentAdd), PaymentAdd.class)
                .retrieve()
                .onStatus(HttpStatus.UNAUTHORIZED::equals, this::handleAuthenticationError)
                .bodyToMono(PaymentDetail.class)
                .onErrorResume(WebClientResponseException.class, this::handlePaymentServiceError);
    }

    /**
     * Clean payment table with JWT authentication
     * Updated for new authentication requirements
     */
    public Mono<PaymentDetail> cleanPaymentTable() {
        return webClientBuilder.build().delete()
                .uri("http://payment-service/clean-db")
                .headers(headers -> {
                    if (jwtToken != null && !jwtToken.isEmpty()) {
                        headers.setBearerAuth(jwtToken);
                    }
                })
                .retrieve()
                .onStatus(HttpStatus.UNAUTHORIZED::equals, this::handleAuthenticationError)
                .bodyToMono(PaymentDetail.class)
                .onErrorResume(WebClientResponseException.class, this::handlePaymentServiceError);
    }

    /**
     * Handle authentication errors from payment service v7.0.0
     * New error format: {"error":"authentication_failed","message":"Invalid or expired token"}
     */
    private Mono<? extends Throwable> handleAuthenticationError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .flatMap(errorBody -> {
                    log.error("Payment service authentication failed: {}", errorBody);
                    return Mono.error(new PaymentAuthenticationException(
                            "Payment service requires valid JWT token. Error: " + errorBody));
                });
    }

    /**
     * Handle payment service errors with improved logging
     */
    private <T> Mono<T> handlePaymentServiceError(WebClientResponseException ex) {
        log.error("Payment service error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return Mono.error(new PaymentAuthenticationException(
                    "Payment service authentication failed: " + ex.getResponseBodyAsString()));
        }
        return Mono.error(ex);
    }

    /**
     * Custom exception for payment authentication failures
     */
    public static class PaymentAuthenticationException extends RuntimeException {
        public PaymentAuthenticationException(String message) {
            super(message);
        }
    }
}
