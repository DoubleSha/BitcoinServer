package BitcoinServer;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
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
    private final String cqlQueryForPaymentRequestByHash = "SELECT * FROM payment_requests WHERE payment_request_hash = :HASH";
    private final String cqlInsertPaymentRequest =
            "INSERT INTO payment_requests (id,payment_request_hash,payment_request,ack_memo) VALUES (:ID,:HASH,:PAYMENT_REQUEST,:ACK_MEMO)";

    @Autowired
    private CqlTemplate cqlTemplate;

    public PaymentRequest findPaymentRequestById(final String id) throws InvalidProtocolBufferException {
        PaymentRequest paymentRequest = cqlTemplate.query(new PreparedStatementCreator() {
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
            }, new ResultSetExtractor<PaymentRequest>() {
                @Override
                public @Nullable PaymentRequest extractData(ResultSet resultSet) throws DriverException, DataAccessException {
                    if (resultSet == null || resultSet.isExhausted())
                        return null;
                    String encodedPaymentRequest = resultSet.one().getString("payment_request");
                    try {
                        return PaymentRequest.newBuilder().mergeFrom(Hex.hexToBytes(encodedPaymentRequest)).build();
                    } catch (InvalidProtocolBufferException e) {
                        throw new PaymentRequestDbServiceException("Failed to parse PaymentRequest", e);
                    }
                }
            });
        if (id == null)
            log.debug("No result found for payment_request id {}", id);
        else
            log.debug("Found payment_request {} for payment_request_hash {}", paymentRequest, id);
        return paymentRequest;
    }

    public String findPaymentRequestIdByHash(final String hash) throws InvalidProtocolBufferException {
        String id = cqlTemplate.query(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Session session) throws DriverException {
                    return session.prepare(cqlQueryForPaymentRequestByHash);
                }
            }, new PreparedStatementBinder() {
                @Override
                public BoundStatement bindValues(PreparedStatement ps) throws DriverException {
                    BoundStatement bs = new BoundStatement(ps);
                    bs.setString("HASH", hash);
                    return bs;
                }
            }, new ResultSetExtractor<String>() {
                @Override
                public @Nullable String extractData(ResultSet resultSet) throws DriverException, DataAccessException {
                    if (resultSet == null || resultSet.isExhausted())
                        return null;
                    return resultSet.one().getString("id");
                }
            });
        if (id == null)
            log.debug("No entry found for payment_request_hash {}", hash);
        else
            log.debug("Found payment_request_id {} for payment_request_hash {}", id, hash);
        return id;
    }

    public void insertPaymentRequest(final String id, final String hash, final PaymentRequest paymentRequest, @Nullable final String ackMemo) {
        cqlTemplate.query(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Session session) throws DriverException {
                    return session.prepare(cqlInsertPaymentRequest);
                }
            }, new PreparedStatementBinder() {
                @Override
                public BoundStatement bindValues(PreparedStatement ps) throws DriverException {
                    BoundStatement bs = new BoundStatement(ps);
                    bs.setString("ID", id);
                    bs.setString("HASH", hash);
                    bs.setString("PAYMENT_REQUEST", Hex.bytesToHex(paymentRequest.toByteArray()));
                    if (ackMemo != null)
                        bs.setString("ACK_MEMO", ackMemo);
                    return bs;
                }
            }, new ResultSetExtractor<ResultSet>() {
                @Override
                public @Nullable ResultSet extractData(ResultSet resultSet) throws DriverException, DataAccessException {
                    return resultSet;
                }
            });
        log.debug("Inserted payment_request with id {} ackMemo {}", id, ackMemo);
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
