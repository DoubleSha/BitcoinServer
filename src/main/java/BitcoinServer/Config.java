package BitcoinServer;

import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {
    @Bean
    public PeerGroup mainNetPeerGroup() {
        return new PeerGroup(MainNetParams.get());
    }

    @Bean
    public PeerGroup testNetPeerGroup() {
        return new PeerGroup(TestNet3Params.get());
    }
}
