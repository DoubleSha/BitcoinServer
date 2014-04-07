package BitcoinServer;

import com.BitcoinServer.Protos;
import com.google.bitcoin.core.*;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

@RestController
public class MainController {
    private final Logger log = LoggerFactory.getLogger(MainController.class);

    @RequestMapping(value = "/broadcast",
                    method = RequestMethod.POST,
                    consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody Protos.BroadcastPaymentResponse broadcast(@RequestBody Protos.BroadcastPayment broadcastPayment) {
        Protos.BroadcastPaymentResponse.Builder response = Protos.BroadcastPaymentResponse.newBuilder();
        org.bitcoin.protocols.payments.Protos.PaymentRequest paymentRequest = broadcastPayment.getPaymentRequest();
        org.bitcoin.protocols.payments.Protos.Payment payment = broadcastPayment.getPayment();
        org.bitcoin.protocols.payments.Protos.PaymentDetails paymentDetails = null;
        NetworkParameters params = null;
        PeerGroup peerGroup = null;
        try {
            paymentDetails = org.bitcoin.protocols.payments.Protos.PaymentDetails.newBuilder().mergeFrom(paymentRequest.getSerializedPaymentDetails()).build();
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
                    for (org.bitcoin.protocols.payments.Protos.Output reqOut : paymentDetails.getOutputsList()) {
                        if (Arrays.equals(txOut.getScriptBytes(), reqOut.getScript().toByteArray())) {
                            txSum += txOut.getValue().doubleValue();
                        }
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
        for (org.bitcoin.protocols.payments.Protos.Output out : paymentDetails.getOutputsList())
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

    @RequestMapping(value = "/send",
                    method = RequestMethod.POST,
                    consumes = "application/bitcoin-payment",
                    produces = "application/bitcoin-paymentack")
    public @ResponseBody org.bitcoin.protocols.payments.Protos.PaymentACK
            send(@RequestBody org.bitcoin.protocols.payments.Protos.Payment payment) throws IOException {
        log.debug("/send {}", payment);
        org.bitcoin.protocols.payments.Protos.PaymentACK.Builder ack = org.bitcoin.protocols.payments.Protos.PaymentACK.newBuilder();
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

    @RequestMapping(value = "/pay", produces = "application/bitcoin-paymentrequest")
    public @ResponseBody org.bitcoin.protocols.payments.Protos.PaymentRequest
            pay(@RequestParam(value = "to", required = true) String to,
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
        org.bitcoin.protocols.payments.Protos.Output.Builder outputBuilder = org.bitcoin.protocols.payments.Protos.Output.newBuilder()
                .setAmount(Utils.toNanoCoins(btc).longValue())
                .setScript(ByteString.copyFrom(ScriptBuilder.createOutputScript(new Address(params, to)).getProgram()));
        org.bitcoin.protocols.payments.Protos.PaymentDetails paymentDetails = org.bitcoin.protocols.payments.Protos.PaymentDetails.newBuilder()
                .setNetwork(network)
                .setTime(System.currentTimeMillis() / 1000L)
                .setPaymentUrl("http://173.8.166.105:8080/send")
                .addOutputs(outputBuilder)
                .setMemo(memo)
                .build();
        return org.bitcoin.protocols.payments.Protos.PaymentRequest.newBuilder()
                .setPaymentDetailsVersion(1)
                .setPkiType("none")
                .setSerializedPaymentDetails(paymentDetails.toByteString())
                .build();
    }
}
