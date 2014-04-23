package BitcoinServer;

import com.google.bitcoin.core.*;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.codec.binary.Base64;
import org.bitcoin.protocols.payments.Protos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.xml.bind.ValidationException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@RestController
public class MainController {

    private final Logger log = LoggerFactory.getLogger(MainController.class);
    private final String BASE_URL = "http://dblsha.com/";
//    private final String BASE_URL = "http://173.8.166.105:8080/";
    final private static char[] hexArray = "0123456789abcdef".toCharArray();

    @Autowired
    private PaymentRequestDbService paymentRequestDbService;

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
        String id = paymentRequestIdFromHash(hash);
        int hexIndex = 0;
        PaymentRequestEntry existingEntry = paymentRequestDbService.findEntryById(id);
        while (existingEntry != null && hexIndex < 16) {
            if (hash.equals(existingEntry.getPaymentRequestHash())) {
                log.info("/create Using existing entry with id {}", existingEntry.getId());
                response.setUri(bitcoinUriFromId(existingEntry.getId()));
                return response;
            }
            if (hexIndex == 0)
                log.warn("/create Duplication PaymentRequestEntry found for id {}", id);
            else
                log.warn("/create Duplication PaymentRequestEntry found for id {}{}", id, hexArray[hexIndex]);
            existingEntry = paymentRequestDbService.findEntryById(id + hexArray[hexIndex]);
            if (existingEntry == null) {
                id = id + hexArray[hexIndex];
                break;
            }
            hexIndex++;
        }
        if (hexIndex >= 16)
            throw new VerificationException("Failed to create new entry for id " + id);
        PaymentRequestEntry entry = new PaymentRequestEntry();
        PaymentRequest paymentRequest = newPaymentRequest(request, id);
        entry.setId(id);
        entry.setPaymentRequestHash(hash);
        entry.setPaymentRequest(paymentRequest);
        entry.setAckMemo(request.getAckMemo());
        paymentRequestDbService.insertEntry(entry);
        response.setUri(bitcoinUriFromId(id));
        log.info("/create Succeeded! request {} response {}", request, response);
        return response;
    }

    @RequestMapping(value = "/p{id}",
                    method = RequestMethod.GET,
                    produces = "application/bitcoin-paymentrequest")
    public @ResponseBody PaymentRequest getPaymentRequest(@PathVariable String id)
            throws VerificationException, InvalidProtocolBufferException {
        PaymentRequestEntry entry = paymentRequestDbService.findEntryById(id);
        if (entry == null || entry.getPaymentRequest() == null)
            throw new VerificationException("No PaymentRequest found for id " + id);
        // TODO: Verify the payment request.
        log.info("Serving PaymentRequest for id {}", id);
        return entry.getPaymentRequest();
    }

    @RequestMapping(value = "/pay/{id}",
                    method = RequestMethod.POST,
                    consumes = "application/bitcoin-payment",
                    produces = "application/bitcoin-paymentack")
    public @ResponseBody PaymentACK pay(@RequestBody Payment payment, @PathVariable String id) throws IOException {
        log.info("/pay id {} payment {}", id, payment);
        PaymentRequestEntry entry = paymentRequestDbService.findEntryById(id);
        if (entry == null || entry.getPaymentRequest() == null) {
            log.error("Entry for id {} has null PaymentRequest", id);
            throw new VerificationException("No PaymentRequest found for id " + id);
        }
        PaymentACK.Builder ack = PaymentACK.newBuilder();
        ack.setPayment(payment);
        if (entry.getAckMemo() != null)
            ack.setMemo(entry.getAckMemo());
        PaymentDetails paymentDetails = PaymentDetails.newBuilder().mergeFrom(entry.getPaymentRequest().getSerializedPaymentDetails()).build();
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

    private String paymentRequestIdFromHash(String hash) {
        return new String(Base64.encodeBase64(hash.getBytes())).substring(0, 6);
    }

    private URI bitcoinUriFromId(String id) throws URISyntaxException {
        return new URI("bitcoin:?r=" + BASE_URL + "p" + id);
    }

    private PaymentRequest newPaymentRequest(CreatePaymentRequestRequest createRequest, String id)
            throws AddressFormatException, VerificationException {
        // TODO: Ask the PaymentRequestNotary server for the payment request instead of creating it here.
        Address addr = new Address(null, createRequest.getAddress());
        NetworkParameters params = addr.getParameters();
        String network = null;
        if (params.equals(MainNetParams.get()))
            network = "main";
        else if (params.equals(TestNet3Params.get()))
            network = "test";
        if (network == null)
            throw new VerificationException("Invalid network " + network);
        Output.Builder outputBuilder = Output.newBuilder()
                .setAmount(createRequest.getAmount().longValue())
                .setScript(ByteString.copyFrom(ScriptBuilder.createOutputScript(addr).getProgram()));
        PaymentDetails paymentDetails = PaymentDetails.newBuilder()
                .setNetwork(network)
                .setTime(System.currentTimeMillis() / 1000L)
                .setPaymentUrl(BASE_URL + "/pay/" + id)
                .addOutputs(outputBuilder)
                .setMemo(createRequest.getMemo())
                .build();
        return PaymentRequest.newBuilder()
                .setPaymentDetailsVersion(1)
                .setPkiType("none")
                .setSerializedPaymentDetails(paymentDetails.toByteString())
                .build();
    }
}
