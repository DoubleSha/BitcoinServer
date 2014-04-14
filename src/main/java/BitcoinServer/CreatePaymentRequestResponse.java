package BitcoinServer;

import java.net.URI;

public class CreatePaymentRequestResponse {
    private URI uri;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return "{ uri: " + getUri() + " }";
    }
}
