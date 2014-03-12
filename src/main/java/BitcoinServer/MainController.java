package BitcoinServer;

import com.BitcoinServer.Protos;
import com.google.bitcoin.core.*;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@RestController
public class MainController {
    @Autowired
    PeerGroup mainNetPeerGroup;

    @Autowired
    PeerGroup testNetPeerGroup;

    @RequestMapping(value = "/broadcast",
                    method = RequestMethod.POST,
                    consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] broadcast(HttpServletRequest request) {
        Protos.BroadcastPaymentResponse.Builder response = Protos.BroadcastPaymentResponse.newBuilder();
        Protos.BroadcastPayment broadcastPayment;
        try {
            broadcastPayment = Protos.BroadcastPayment.newBuilder().mergeFrom(request.getInputStream()).build();
        } catch (IOException e) {
            response.setError("Failed to read BroadcastPayment " + e);
            return response.build().toByteArray();
        }
        org.bitcoin.protocols.payments.Protos.PaymentRequest paymentRequest = broadcastPayment.getPaymentRequest();
        org.bitcoin.protocols.payments.Protos.Payment payment = broadcastPayment.getPayment();
        org.bitcoin.protocols.payments.Protos.PaymentDetails paymentDetails = null;
        NetworkParameters params = null;
        PeerGroup peerGroup = null;
        try {
            paymentDetails = org.bitcoin.protocols.payments.Protos.PaymentDetails.newBuilder().mergeFrom(paymentRequest.getSerializedPaymentDetails()).build();
        } catch (InvalidProtocolBufferException e) {
            response.setError("Invalid PaymentDetails " + e);
            return response.build().toByteArray();
        }
        if (paymentDetails == null) {
            response.setError("Invalid PaymentDetails");
            return response.build().toByteArray();
        }
        if (!paymentDetails.hasNetwork() || paymentDetails.getNetwork().equals("main")) {
            peerGroup = mainNetPeerGroup;
            params = MainNetParams.get();
        }
        else if (paymentDetails.getNetwork().equals("test")) {
            peerGroup = testNetPeerGroup;
            params = TestNet3Params.get();
        }
        if (params == null || peerGroup == null) {
            response.setError("Invalid network");
            return response.build().toByteArray();
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
                for (TransactionOutput output : tx.getOutputs())
                    txSum += output.getValue().doubleValue();
            } catch (VerificationException e) {
                response.setError("Invalid transaction " + e);
                return response.build().toByteArray();
            }
        }
        if (txs.isEmpty() || txSum == 0) {
            response.setError("Empty transactions");
            return response.build().toByteArray();
        }
        // Verify that the value of the Payment is what we expect.
        double outSum = 0;
        for (org.bitcoin.protocols.payments.Protos.Output out : paymentDetails.getOutputsList())
            outSum += out.getAmount();
        if (txSum != outSum) {
            response.setError("Transaction value: " + txSum + " does not match expected PaymentRequest value: " + outSum);
            return response.build().toByteArray();
        }
        // Transactions are valid, now broadcast them.
        try {
            for (Transaction tx : txs)
                peerGroup.broadcastTransaction(tx).get();
        } catch (InterruptedException e) {
            response.setError("Failed to send transactions " + e);
        } catch (ExecutionException e) {
            response.setError("Failed to send transactions " + e);
        }
        return response.build().toByteArray();
    }

    @RequestMapping(value = "/send",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] send(HttpServletRequest request) throws IOException {
        org.bitcoin.protocols.payments.Protos.PaymentACK.Builder ack = org.bitcoin.protocols.payments.Protos.PaymentACK.newBuilder();
        org.bitcoin.protocols.payments.Protos.Payment payment;
        payment = org.bitcoin.protocols.payments.Protos.Payment.newBuilder().mergeFrom(request.getInputStream()).build();
        ack.setPayment(payment);
        NetworkParameters params = TestNet3Params.get();
        PeerGroup peerGroup = testNetPeerGroup;
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
                return ack.build().toByteArray();
            }
        }
        if (txs.isEmpty()) {
            ack.setMemo("Error: Empty transactions");
            return ack.build().toByteArray();
        }
        // Transactions are valid, now broadcast them.
        try {
            for (Transaction tx : txs)
                peerGroup.broadcastTransaction(tx).get();
        } catch (InterruptedException e) {
            ack.setMemo("Failed to send transactions: " + e);
        } catch (ExecutionException e) {
            ack.setMemo("Failed to send transactions: " + e);
        }
        return ack.build().toByteArray();
    }

    @RequestMapping(value = "/pay", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] pay(@RequestParam(value = "to") String to,
                      @RequestParam(value = "amount") String btc,
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
                .setPaymentUrl("173.8.166.105:8080/send")
                .addOutputs(outputBuilder)
                .setMemo(memo)
                .build();
        return org.bitcoin.protocols.payments.Protos.PaymentRequest.newBuilder()
                .setPaymentDetailsVersion(1)
                .setPkiType("none")
                .setSerializedPaymentDetails(paymentDetails.toByteString())
                .build().toByteArray();
    }
}
