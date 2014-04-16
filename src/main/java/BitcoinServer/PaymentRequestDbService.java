package BitcoinServer;

import com.datastax.driver.core.ResultSet;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.cassandra.utils.Hex;
import org.bitcoin.protocols.payments.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cassandra.core.CqlTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentRequestDbService {
    private final Logger log = LoggerFactory.getLogger(PaymentRequestDbService.class);

    @Autowired
    private CqlTemplate cqlTemplate;

    public Protos.PaymentRequest findPaymentRequestById(String id) throws InvalidProtocolBufferException {
        ResultSet result = cqlTemplate.query(cqlQueryPaymentRequest(id));
        if (result == null || result.isExhausted()) {
            log.debug("No result found for payment_request id {}", id);
            return null;
        }
        String encodedPaymentRequest = result.one().getString("payment_request");
        Protos.PaymentRequest paymentRequest = Protos.PaymentRequest.newBuilder().mergeFrom(Hex.hexToBytes(encodedPaymentRequest)).build();
        log.debug("Found payment_request {} for payment_request_hash {}", paymentRequest, id);
        return paymentRequest;
    }

    public String findPaymentRequestIdByHash(String hash) throws InvalidProtocolBufferException {
        ResultSet result = cqlTemplate.query(cqlQueryPaymentRequestByHash(hash));
        if (result == null || result.isExhausted()) {
            log.debug("No result found for payment_request hash {}", hash);
            return null;
        }
        String id = result.one().getString("id");
        log.debug("Found payment_request_id {} for payment_request_hash {}", id, hash);
        return id;
    }

    public void insertPaymentRequest(String id, String hash, Protos.PaymentRequest paymentRequest, String ackMemo) {
        log.debug("Inserting payment_request with id {}", id);
        cqlTemplate.execute(cqlInsertPaymentRequest(id, hash, paymentRequest, ackMemo));
    }

    private String cqlQueryPaymentRequest(String id) {
        // TODO: Sanitize inputs.
        return "SELECT * FROM payment_requests WHERE id='" + id + "';";
    }

    private String cqlQueryPaymentRequestByHash(String hash) {
        // TODO: Sanitize inputs.
        return "SELECT * FROM payment_requests WHERE payment_request_hash='" + hash + "';";
    }

    private String cqlInsertPaymentRequest(String id, String hash, Protos.PaymentRequest paymentRequest, String ackMemo) {
        // TODO: Sanitize inputs.
        String keys = "id,payment_request_hash,payment_request";
        String values = "'" + id + "','" + hash + "','" + Hex.bytesToHex(paymentRequest.toByteArray()) + "'";
        if (ackMemo != null) {
            keys += ",ack_memo";
            values += ",'" + ackMemo + "'";
        }
        System.out.println("keys " + keys + " values " + values);
        return "INSERT INTO payment_requests (" + keys + ") VALUES (" + values + ");";
    }
}
