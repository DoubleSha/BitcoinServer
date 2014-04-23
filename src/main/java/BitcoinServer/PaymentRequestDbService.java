package BitcoinServer;

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.DriverException;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.cassandra.utils.Hex;
import org.bitcoin.protocols.payments.Protos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cassandra.core.*;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;

@Service
public class PaymentRequestDbService {

    private final Logger log = LoggerFactory.getLogger(PaymentRequestDbService.class);
    private final String cqlQueryForPaymentRequestById = "SELECT * FROM payment_requests WHERE id = :ID";
    private final String cqlInsertPaymentRequest =
            "INSERT INTO payment_requests (id,payment_request_hash,payment_request,ack_memo) VALUES (:ID,:HASH,:PAYMENT_REQUEST,:ACK_MEMO)";

    @Autowired
    private CqlTemplate cqlTemplate;

    public PaymentRequestEntry findEntryById(final String id) throws InvalidProtocolBufferException {
        PaymentRequestEntry entry = cqlTemplate.query(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Session session) throws DriverException {
                    PreparedStatement ps = session.prepare(cqlQueryForPaymentRequestById);
                    return session.prepare(cqlQueryForPaymentRequestById);
                }
            }, new PreparedStatementBinder() {
                @Override
                public BoundStatement bindValues(PreparedStatement ps) throws DriverException {
                    BoundStatement bs = new BoundStatement(ps);
                    bs.setString("ID", id);
                    return bs;
                }
            }, new ResultSetExtractor<PaymentRequestEntry>() {
                @Override
                public @Nullable PaymentRequestEntry extractData(ResultSet resultSet) throws DriverException, DataAccessException {
                    PaymentRequestEntry entry = new PaymentRequestEntry();
                    if (resultSet == null || resultSet.isExhausted())
                        return null;
                    Row row = resultSet.one();
                    entry.setId(row.getString("id"));
                    String encodedPaymentRequest = row.getString("payment_request");
                    try {
                        entry.setPaymentRequest(PaymentRequest.newBuilder().mergeFrom(Hex.hexToBytes(encodedPaymentRequest)).build());
                    } catch (InvalidProtocolBufferException e) {
                        throw new PaymentRequestDbServiceException("Failed to parse PaymentRequest", e);
                    }
                    entry.setPaymentRequestHash(row.getString("payment_request_hash"));
                    entry.setAckMemo(row.getString("ack_memo"));
                    return entry;
                }
            });
        if (id == null)
            log.debug("No result found for payment_request id {}", id);
        else
            log.debug("Found entry {} for id {}", entry, id);
        return entry;
    }

    public void insertEntry(final PaymentRequestEntry entry) {
        cqlTemplate.query(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Session session) throws DriverException {
                    return session.prepare(cqlInsertPaymentRequest);
                }
            }, new PreparedStatementBinder() {
                @Override
                public BoundStatement bindValues(PreparedStatement ps) throws DriverException {
                    BoundStatement bs = new BoundStatement(ps);
                    bs.setString("ID", entry.getId());
                    bs.setString("HASH", entry.getPaymentRequestHash());
                    bs.setString("PAYMENT_REQUEST", Hex.bytesToHex(entry.getPaymentRequest().toByteArray()));
                    if (entry.getAckMemo() != null)
                        bs.setString("ACK_MEMO", entry.getAckMemo());
                    return bs;
                }
            }, new ResultSetExtractor<ResultSet>() {
                @Override
                public @Nullable ResultSet extractData(ResultSet resultSet) throws DriverException, DataAccessException {
                    return resultSet;
                }
            });
        log.debug("Inserted payment_request with entry {}", entry);
    }

    public static class PaymentRequestDbServiceException extends DataAccessException {

        public PaymentRequestDbServiceException(String msg) {
            super(msg);
        }

        public PaymentRequestDbServiceException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
