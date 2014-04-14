package BitcoinServer;

import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebMvcConfig {
    @Bean
    HttpMessageConverters httpMessageConverters() {
        return new HttpMessageConverters(new BitcoinHttpMessageConverter());
    }
}
