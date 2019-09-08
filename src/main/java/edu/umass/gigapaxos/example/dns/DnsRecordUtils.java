package edu.umass.gigapaxos.example.dns;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class DnsRecordUtils {

    /**
     * @see io.netty.handler.codec.dns.DefaultDnsRecordEncoder
     */
    public static void encodeName(String name, ByteBuf buf) {
        if (".".equals(name)) {
            // Root domain
            buf.writeByte(0);
            return;
        }

        final String[] labels = name.split("\\.");
        for (String label : labels) {
            final int labelLen = label.length();
            if (labelLen == 0) {
                // zero-length label means the end of the name.
                break;
            }

            buf.writeByte(labelLen);
            ByteBufUtil.writeAscii(buf, label);
        }

        buf.writeByte(0); // marks end of name field
    }
}
