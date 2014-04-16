package BitcoinServer;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;

public class BitcoinHttpMessageConverter extends AbstractHttpMessageConverter<Message> {

    public BitcoinHttpMessageConverter() {
        setSupportedMediaTypes(Arrays.asList(new MediaType("application", "bitcoin-paymentrequest"),
                                             new MediaType("application", "bitcoin-payment"),
                                             new MediaType("application", "bitcoin-paymentack"),
                                             MediaType.APPLICATION_OCTET_STREAM));
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Message.class.isAssignableFrom(clazz);
    }

    @Override
    protected Message readInternal(Class clazz, HttpInputMessage inputMessage) {
        Assert.isAssignable(Message.class, clazz, "BitcoinHttpMessageConverter can only deserialize protobuf objects");
        try {
            InputStream in = inputMessage.getBody();
            Method newBuilder = clazz.getMethod("newBuilder");
            GeneratedMessage.Builder<?> builder = (GeneratedMessage.Builder<?>)newBuilder.invoke(clazz);
            Message msg = builder.mergeFrom(in).build();
            return msg;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void writeInternal(Message obj, HttpOutputMessage outputMessage) {
        Assert.isTrue(obj instanceof Message, "BitcoinHttpMessageConverter can only serialize protobuf objects");
        try {
            OutputStream out = outputMessage.getBody();
            Message msg = (Message)obj;
            out.write(msg.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
