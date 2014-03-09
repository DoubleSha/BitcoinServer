package BitcoinServer;

import com.BitcoinServer.Protos;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.VerificationException;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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

    @RequestMapping(value = "/broadcast", method = RequestMethod.POST)
    public ByteString broadcast(HttpServletRequest request) {
        Protos.BroadcastResponse.Builder response = Protos.BroadcastResponse.newBuilder();
        Protos.TransactionList txList;
        try {
            txList = Protos.TransactionList.newBuilder().mergeFrom(request.getInputStream()).build();
        } catch (IOException e) {
            response.setError("Failed to read transaction list: " + e);
            return response.build().toByteString();
        }
        NetworkParameters params = null;
        PeerGroup peerGroup = null;
        if (!txList.hasNetwork() || txList.getNetwork().equals("main")) {
            peerGroup = mainNetPeerGroup;
            params = MainNetParams.get();
        }
        else if (txList.getNetwork().equals("test")) {
            peerGroup = testNetPeerGroup;
            params = TestNet3Params.get();
        }
        if (params == null || peerGroup == null) {
            response.setError("Invalid network");
            return response.build().toByteString();
        }
        ArrayList<Transaction> txs = new ArrayList<Transaction>();
        // Decode and validate all transactions.
        for (ByteString encodedTx : txList.getTransactionsList()) {
            Transaction tx = null;
            try {
                tx = new Transaction(params, encodedTx.toByteArray());
                tx.verify();
                txs.add(tx);
            } catch (VerificationException e) {
                response.setError("Invalid transaction: " + e);
                return response.build().toByteString();
            }
        }
        if (txs.isEmpty()) {
            response.setError("No transactions");
            return response.build().toByteString();
        }
        // Transactions are valid, now broadcast them.
        try {
            for (Transaction tx : txs)
                peerGroup.broadcastTransaction(tx).get();
        } catch (InterruptedException e) {
            response.setError("Failed to send transactions: " + e);
        } catch (ExecutionException e) {
            response.setError("Failed to send transactions: " + e);
        }
        return response.build().toByteString();
    }
}
