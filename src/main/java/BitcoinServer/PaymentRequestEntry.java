package BitcoinServer;

import org.bitcoin.protocols.payments.Protos.*;

public class PaymentRequestEntry {

    private String id;
    private PaymentRequest paymentRequest;
    private String paymentRequestHash;
    private String ackMemo;

    public String getAckMemo() {
        return ackMemo;
    }

    public void setAckMemo(String ackMemo) {
        this.ackMemo = ackMemo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    public void setPaymentRequest(PaymentRequest paymentRequest) {
        this.paymentRequest = paymentRequest;
    }

    public String getPaymentRequestHash() {
        return paymentRequestHash;
    }

    public void setPaymentRequestHash(String paymentRequestHash) {
        this.paymentRequestHash = paymentRequestHash;
    }

    public String toString() {
        // No point in printing out the hash or payment request since they're just long, inscrutable strings.
        return "{ id: " + getId() + ", ackMemo: " + getAckMemo() + " }";
    }
}
