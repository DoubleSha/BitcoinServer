package BitcoinServer;

import com.BitcoinServer.Protos.*;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.bitcoin.core.*;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.cassandra.utils.Hex;
import org.apache.commons.codec.binary.Base64;
import org.bitcoin.protocols.payments.Protos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cassandra.core.CqlTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.xml.bind.ValidationException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

@RestController
public class MainController {
    private final Logger log = LoggerFactory.getLogger(MainController.class);
    private final String BASE_URL = "http://misito.net/";
//    private final String BASE_URL = "http://173.8.166.105:8080/";

    @Autowired
    private CqlTemplate cqlTemplate;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView index() {
        return new ModelAndView("index.html");
    }

    @RequestMapping(value = "/create",
                    method = RequestMethod.POST,
                    consumes = MediaType.APPLICATION_JSON_VALUE,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody CreatePaymentRequestResponse createPaymentRequest(@RequestBody CreatePaymentRequestRequest request)
            throws URISyntaxException, AddressFormatException, InvalidProtocolBufferException, ValidationException {
        CreatePaymentRequestResponse response = new CreatePaymentRequestResponse();
        if (request == null)
            throw new ValidationException("Invalid CreatePaymentRequestRequest " + request);
        String hash = request.hash();
        // TODO: Enable this.
//        String existingId = findPaymentRequestIdByHash(hash);
//        if (existingId != null) {
//            log.info("Created payment request using existing entry {}", existingId);
//            response.setUri(new URI("bitcoin:?r=" + BASE_URL + "p" + existingId));
//            return response;
//        }
        String id = uniquePaymentRequestId(hash);
        if (id == null)
            throw new ValidationException("Could not generate unique id");
        PaymentRequest paymentRequest = newPaymentRequest(request, id);
        insertPaymentRequest(id, hash, paymentRequest, request.getAckMemo());
        response.setUri(new URI("bitcoin:?r=" + BASE_URL + "p" + id));
        log.info("/create request {} response {}", request, response);
        return response;
    }

    final private static char[] hexArray = "0123456789abcdef".toCharArray();
    private String uniquePaymentRequestId(String hash) throws InvalidProtocolBufferException {
        String baseId = new String(Base64.encodeBase64(hash.getBytes())).substring(0, 6);
        String id = baseId;
        PaymentRequest paymentRequest = findPaymentRequest(id);
        int hexIndex = 0;
        while (paymentRequest != null && hexIndex < 16) {
            log.warn("Duplication payment_request_id found {}. Trying hexIndex {}", id, hexIndex);
            id = baseId + hexArray[hexIndex++];
            paymentRequest = findPaymentRequest(id);
        }
        if (hexIndex >= 16) {
            log.error("Could not create new unique payment_request_id for hash {}", hash);
            return null;
        }
        log.debug("Created new payment_request_id {}", id);
        return id;
    }

    @RequestMapping(value = "/p{id}",
                    method = RequestMethod.GET,
                    produces = "application/bitcoin-paymentrequest")
    public @ResponseBody PaymentRequest getPaymentRequest(@PathVariable String id)
            throws VerificationException, InvalidProtocolBufferException {
        System.out.println("id " + id);
        PaymentRequest paymentRequest = findPaymentRequest(id);
        if (paymentRequest == null)
            throw new VerificationException("No PaymentRequest found for id " + id);
        return paymentRequest;
    }

    @RequestMapping(value = "/pay/{id}",
                    method = RequestMethod.POST,
                    consumes = "application/bitcoin-payment",
                    produces = "application/bitcoin-paymentack")
    public @ResponseBody PaymentACK
            pay(@RequestBody Payment payment, @PathVariable String id) throws IOException {
        log.info("/pay id {} payment {}", id, payment);
        PaymentACK.Builder ack = PaymentACK.newBuilder();
        ack.setMemo("Thank you for your payment! It is being processed by the bitcoin network.");
        ack.setPayment(payment);
        PaymentRequest paymentRequest = findPaymentRequest(id);
        if (paymentRequest == null)
            throw new VerificationException("Could not find entry for " + id);
        PaymentDetails paymentDetails = PaymentDetails.newBuilder().mergeFrom(paymentRequest.getSerializedPaymentDetails()).build();
        NetworkParameters params = null;
        if (paymentDetails.getNetwork() == null || paymentDetails.getNetwork().equals("main"))
            params = MainNetParams.get();
        else if (paymentDetails.getNetwork().equals("test"))
            params = TestNet3Params.get();
        if (params == null) {
            log.error("Entry for id {} has Invalid network {}", id, paymentDetails.getNetwork());
            throw new VerificationException("Invalid network " + paymentDetails.getNetwork());
        }
        // Decode and validate all transactions.
        ArrayList<Transaction> txs = new ArrayList<Transaction>();
        for (ByteString encodedTx : payment.getTransactionsList()) {
            Transaction tx = null;
            try {
                tx = new Transaction(params, encodedTx.toByteArray());
                tx.verify();
                txs.add(tx);
            } catch (VerificationException e) {
                log.info("Error: Invalid transaction {}", e);
                ack.setMemo("Error: Invalid transaction " + e);
                return ack.build();
            }
        }
        if (txs.isEmpty()) {
            ack.setMemo("Error: Empty transactions");
            return ack.build();
        }
        // Transactions are valid, now broadcast them.
        PeerGroup peerGroup = new PeerGroup(params);
        try {
            peerGroup.addPeerDiscovery(new DnsDiscovery(params));
            peerGroup.startAndWait();
            for (Transaction tx : txs)
                peerGroup.broadcastTransaction(tx).get();
        } catch (InterruptedException e) {
            ack.setMemo("Failed to send transactions: " + e);
        } catch (ExecutionException e) {
            ack.setMemo("Failed to send transactions: " + e);
        }
        peerGroup.stopAndWait();
        return ack.build();
    }

    private PaymentRequest newPaymentRequest(CreatePaymentRequestRequest createRequest, String id)
            throws AddressFormatException, VerificationException {
        // TODO: Ask the PaymentRequestNotary server for the payment request instead of creating it here.
        NetworkParameters params = null;
        if (createRequest.getNetwork() == null || createRequest.getNetwork().equals("main"))
            params = MainNetParams.get();
        else if (createRequest.getNetwork().equals("test"))
            params = TestNet3Params.get();
        if (params == null)
            throw new VerificationException("Invalid network " + createRequest.getNetwork());
        Output.Builder outputBuilder = Output.newBuilder()
                .setAmount(createRequest.getAmount().longValue())
                .setScript(ByteString.copyFrom(ScriptBuilder.createOutputScript(new Address(params, createRequest.getAddress())).getProgram()));
        PaymentDetails paymentDetails = PaymentDetails.newBuilder()
                .setNetwork(createRequest.getNetwork())
                .setTime(System.currentTimeMillis() / 1000L)
                .setPaymentUrl(BASE_URL + "/pay/" + id)
                .addOutputs(outputBuilder)
                .setMemo(createRequest.getNetwork())
                .build();
        return PaymentRequest.newBuilder()
                .setPaymentDetailsVersion(1)
                .setPkiType("none")
                .setSerializedPaymentDetails(paymentDetails.toByteString())
                .build();
    }

    private PaymentRequest findPaymentRequest(String id) throws InvalidProtocolBufferException {
        log.debug("Querying for payment_request with id {}", id);
        ResultSet result = cqlTemplate.query(cqlQueryPaymentRequest(id));
        if (result == null || result.isExhausted()) {
            log.debug("No result found for payment_request id {}", id);
            return null;
        }
        Row row = result.one();
        return PaymentRequest.newBuilder().mergeFrom(Hex.hexToBytes(row.getString("payment_request"))).build();
    }

    private String findPaymentRequestIdByHash(String hash) throws InvalidProtocolBufferException {
        log.debug("Querying for payment_request with hash {}", hash);
        ResultSet result = cqlTemplate.query(cqlQueryPaymentRequestByHash(hash));
        if (result == null || result.isExhausted()) {
            log.debug("No result found for payment_request hash {}", hash);
            return null;
        }
        Row row = result.one();
        return row.getString("id");
    }

    private void insertPaymentRequest(String id, String hash, PaymentRequest paymentRequest, String ackMemo) {
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

    private String cqlInsertPaymentRequest(String id, String hash, PaymentRequest paymentRequest, String ackMemo) {
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

    @RequestMapping(value = "/broadcast",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody BroadcastPaymentResponse broadcast(@RequestBody BroadcastPayment broadcastPayment) {
        BroadcastPaymentResponse.Builder response = BroadcastPaymentResponse.newBuilder();
        PaymentRequest paymentRequest = broadcastPayment.getPaymentRequest();
        Payment payment = broadcastPayment.getPayment();
        PaymentDetails paymentDetails = null;
        NetworkParameters params = null;
        PeerGroup peerGroup = null;
        try {
            paymentDetails = PaymentDetails.newBuilder().mergeFrom(paymentRequest.getSerializedPaymentDetails()).build();
        } catch (InvalidProtocolBufferException e) {
            response.setError("Invalid PaymentDetails " + e);
            log.info("Broadcast failed with error: {} payment: {}", response.getError(), broadcastPayment);
            return response.build();
        }
        if (paymentDetails == null) {
            response.setError("Invalid PaymentDetails");
            log.info("Broadcast failed with error: {} payment: {}", response.getError(), broadcastPayment);
            return response.build();
        }
        if (!paymentDetails.hasNetwork() || paymentDetails.getNetwork().equals("main"))
            params = MainNetParams.get();
        else if (paymentDetails.getNetwork().equals("test"))
            params = TestNet3Params.get();
        if (params == null) {
            response.setError("Invalid network");
            log.info("Broadcast failed with error: {} payment: {}", response.getError(), broadcastPayment);
            return response.build();
        }
        // Decode and validate all transactions.
        ArrayList<Transaction> txs = new ArrayList<Transaction>();
        double txSum = 0;
        for (ByteString encodedTx : payment.getTransactionsList()) {
            Transaction tx = null;
            try {
                tx = new Transaction(params, encodedTx.toByteArray());
                tx.verify();
                txs.add(tx);
                for (TransactionOutput txOut : tx.getOutputs()) {
                    for (Output reqOut : paymentDetails.getOutputsList()) {
                        if (Arrays.equals(txOut.getScriptBytes(), reqOut.getScript().toByteArray()))
                            txSum += txOut.getValue().doubleValue();
                    }
                }
            } catch (VerificationException e) {
                response.setError("Invalid transaction " + e);
                log.info("Broadcast failed with error: {} payment: {}", response.getError(), broadcastPayment);
                return response.build();
            }
        }
        if (txs.isEmpty()) {
            response.setError("Empty transactions");
            log.info("Broadcast failed with error: {} payment: {}", response.getError(), broadcastPayment);
            return response.build();
        }
        if (txSum == 0) {
            response.setError("No transactions matching outputs in PaymentRequest");
            log.info("Broadcast failed with error: {} payment: {}", response.getError(), broadcastPayment);
            return response.build();
        }
        // Verify that the value of the Payment is what we expect.
        double outSum = 0;
        for (Output out : paymentDetails.getOutputsList())
            outSum += out.getAmount();
        if (txSum != outSum) {
            response.setError("Transaction value: " + txSum + " does not match expected PaymentRequest value: " + outSum);
            log.info("Broadcast failed with error: {} payment: {}", response.getError(), broadcastPayment);
            return response.build();
        }
        // Transactions are valid, now broadcast them.
        try {
            peerGroup = new PeerGroup(params);
            peerGroup.addPeerDiscovery(new DnsDiscovery(params));
            peerGroup.startAndWait();
            for (Transaction tx : txs)
                peerGroup.broadcastTransaction(tx).get();
        } catch (InterruptedException e) {
            response.setError("Failed to send transactions " + e);
        } catch (ExecutionException e) {
            response.setError("Failed to send transactions " + e);
        }
        peerGroup.stopAndWait();
        if (response.hasError())
            log.info("Broadcast failed with error: {} payment: {}", response.getError(), broadcastPayment);
        else
            log.info("Broadcast succeeded payment: {}", broadcastPayment);
        return response.build();
    }

    @RequestMapping(value = "/test_pay",
                    method = RequestMethod.POST,
                    consumes = "application/bitcoin-payment",
                    produces = "application/bitcoin-paymentack")
    public @ResponseBody PaymentACK
            testPay(@RequestBody Payment payment) throws IOException {
        log.debug("/test_pay payment {}", payment);
        PaymentACK.Builder ack = PaymentACK.newBuilder();
        ack.setMemo("Thank you for your payment! It is being processed by the bitcoin network.");
        ack.setPayment(payment);
        NetworkParameters params = TestNet3Params.get();
        // Decode and validate all transactions.
        ArrayList<Transaction> txs = new ArrayList<Transaction>();
        for (ByteString encodedTx : payment.getTransactionsList()) {
            Transaction tx = null;
            try {
                tx = new Transaction(params, encodedTx.toByteArray());
                tx.verify();
                txs.add(tx);
            } catch (VerificationException e) {
                ack.setMemo("Error: Invalid transaction " + e);
                return ack.build();
            }
        }
        if (txs.isEmpty()) {
            ack.setMemo("Error: Empty transactions");
            return ack.build();
        }
        // Transactions are valid, now broadcast them.
        PeerGroup peerGroup = new PeerGroup(params);
        try {
            peerGroup.addPeerDiscovery(new DnsDiscovery(params));
            peerGroup.startAndWait();
            for (Transaction tx : txs)
                peerGroup.broadcastTransaction(tx).get();
        } catch (InterruptedException e) {
            ack.setMemo("Failed to send transactions: " + e);
        } catch (ExecutionException e) {
            ack.setMemo("Failed to send transactions: " + e);
        }
        peerGroup.stopAndWait();
        return ack.build();
    }

    @RequestMapping(value = "/test_create", produces = "application/bitcoin-paymentrequest")
    public @ResponseBody PaymentRequest
            testCreate(@RequestParam(value = "to", required = true) String to,
                       @RequestParam(value = "amount", required = true) String btc,
                       @RequestParam(value = "network", required = false) String network,
                       @RequestParam(value = "memo", required = false) String memo)
                               throws AddressFormatException, VerificationException {
        NetworkParameters params = null;
        if (network == null || network.equals("main"))
            params = MainNetParams.get();
        else if (network.equals("test"))
            params = TestNet3Params.get();
        if (params == null)
            throw new VerificationException("Invalid network " + network);
        Output.Builder outputBuilder = Output.newBuilder()
                .setAmount(Utils.toNanoCoins(btc).longValue())
                .setScript(ByteString.copyFrom(ScriptBuilder.createOutputScript(new Address(params, to)).getProgram()));
        PaymentDetails paymentDetails = PaymentDetails.newBuilder()
                .setNetwork(network)
                .setTime(System.currentTimeMillis() / 1000L)
                .setPaymentUrl(BASE_URL + "test_pay")
                .addOutputs(outputBuilder)
                .setMemo(memo)
                .build();
        return PaymentRequest.newBuilder()
                .setPaymentDetailsVersion(1)
                .setPkiType("none")
                .setSerializedPaymentDetails(paymentDetails.toByteString())
                .build();
    }
}
