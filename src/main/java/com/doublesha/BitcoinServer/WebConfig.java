package com.doublesha.BitcoinServer;

import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    @Bean
    HttpMessageConverters httpMessageConverters() {
        return new HttpMessageConverters(new BitcoinHttpMessageConverter());
    }

    @Bean
    PaymentRequestDbService paymentRequestDbService() {
        return new PaymentRequestDbService();
    }
}
