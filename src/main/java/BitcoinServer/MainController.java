package BitcoinServer;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class MainController {
    @RequestMapping(value = "/create_payment_request", method = RequestMethod.POST)
    public ByteArray createPaymentRequest() throws IOException {
        return "fail";
    }

    @RequestMapping(value = "/broadcast_tx", method = RequestMethod.POST)
    public ByteArray broadcastTx() throws IOException {
        return "fail";
    }
}
