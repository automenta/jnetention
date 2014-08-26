package p2p;

import java.net.InetSocketAddress;
import java.util.List;
import jnetention.p2p.Message;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 * @author jfk
 * @date Mar 2, 2011 9:18:33 PM
 * @since 1.0
 * 
 */
public class MessageTest {
    /**
     * test the message class
     */
    @Test
    public void testMessage() {
        final Message msg = new Message("a", "b", "c");

        assertEquals("a", msg.getId());
        assertEquals("b", msg.getTopic());
        assertEquals("c", msg.getMessage());

        System.out.println(msg);

        final long sent = msg.sent();

        assertEquals(sent, msg.getSent());

        msg.addSentTo(null);

        final List<InetSocketAddress> sentTo = msg.getSentTo();

        assertEquals(1, sentTo.size());
        assertNull(sentTo.get(0));
    }
}
