package BitcoinServer;

import com.google.protobuf.ByteString;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class MainController {
    @RequestMapping(value = "/broadcast_transactions", method = RequestMethod.POST)
    public ByteString broadcastTransactions() throws IOException {

        return ByteString.copyFromUtf8("fail");
    }
}
