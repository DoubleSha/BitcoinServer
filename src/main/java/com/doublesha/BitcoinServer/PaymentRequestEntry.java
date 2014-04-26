package com.doublesha.BitcoinServer;

import com.google.bitcoin.core.Address;
import org.bitcoin.protocols.payments.Protos.*;

import java.math.BigInteger;

public class PaymentRequestEntry {

    private String id;
    private PaymentRequest paymentRequest;
    private String paymentRequestHash;
    private String ackMemo;
    private Address addr;
    private BigInteger amount;

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

    public Address getAddr() {
        return addr;
    }

    public void setAddr(Address addr) {
        this.addr = addr;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public String toString() {
        // No point in printing out the hash or payment request since they're just long, inscrutable strings.
        return "{ id: " + getId() + ", addr: " + getAddr()
                + ", amount: " + getAmount() + ", ackMemo: " + getAckMemo() + " }";
    }
}
