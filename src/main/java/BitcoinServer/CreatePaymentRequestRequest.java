package BitcoinServer;

import org.apache.commons.codec.digest.DigestUtils;

import java.math.BigInteger;

public class CreatePaymentRequestRequest {

    private String address;
    private BigInteger amount;
    private String memo;
    private String ackMemo;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getAckMemo() {
        return ackMemo;
    }

    public void setAckMemo(String ackMemo) {
        this.ackMemo = ackMemo;
    }

    @Override
    public String toString() {
        return "{ address: " + getAddress()
                + ", amount: " + getAmount()
                + ", memo: " + getMemo()
                + ", ackMemo: " + getAckMemo() + " }";
    }

    public String hash() {
        return DigestUtils.shaHex("" + getAddress() + getAmount() + getMemo() + getAckMemo());
    }
}
