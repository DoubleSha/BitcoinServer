package BitcoinServer;

import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.List;

@Configuration
public class Config extends WebMvcConfigurationSupport {
    @Bean
    public PeerGroup mainNetPeerGroup() {
        PeerGroup peerGroup = new PeerGroup(MainNetParams.get());
        peerGroup.startAndWait();
        return peerGroup;
    }

    @Bean
    public PeerGroup testNetPeerGroup() {
        PeerGroup peerGroup = new PeerGroup(TestNet3Params.get());
        peerGroup.startAndWait();
        return peerGroup;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new BitcoinHttpMessageConverter());
        addDefaultHttpMessageConverters(converters);
    }
}
