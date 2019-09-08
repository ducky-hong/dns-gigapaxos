package edu.umass.gigapaxos.example.dns;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.dns.*;
import io.netty.util.NetUtil;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DnsQueryHandler extends SimpleChannelInboundHandler<DatagramDnsQuery> {

    private static final String RECONFIGURATOR_ADDRESS = System.getProperty("reconfigurator", "127.0.0.1:3300");

    private OkHttpClient client = new OkHttpClient();

    private AtomicInteger idCounter = new AtomicInteger(10000);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramDnsQuery msg) throws Exception {
        DatagramDnsResponse res = new DatagramDnsResponse(msg.recipient(), msg.sender(), msg.id());

        DnsRecord questionRecord = msg.recordAt(DnsSection.QUESTION);
        res.addRecord(DnsSection.QUESTION, questionRecord);

        String serviceName = questionRecord.name().split("\\.")[0];
        String address = null;
        long ttl = 0;
        try {
            address = getActiveReplicaAddress(serviceName, msg.sender());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (address == null) {
            address = "1.1.1.1";
        }

        ByteBuf content = ctx.alloc().buffer();
        content.writeBytes(NetUtil.createByteArrayFromIpAddressString(address));
        res.addRecord(DnsSection.ANSWER, new DefaultDnsRawRecord(questionRecord.name(),
                DnsRecordType.A, ttl, content));

        ctx.write(res);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private String getActiveReplicaAddress(String serviceName, InetSocketAddress sender) throws IOException {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host(RECONFIGURATOR_ADDRESS.split(":")[0])
                .port(Integer.parseInt(RECONFIGURATOR_ADDRESS.split(":")[1]))
                .addQueryParameter("IS_QUERY", "true")
                .addQueryParameter("RECURSIVE_REDIRECT", "true")
                .addQueryParameter("CREATE_TIME", String.valueOf(System.currentTimeMillis()))
                .addQueryParameter("TYPE", "236")
                .addQueryParameter("NAME", serviceName)
                .addQueryParameter("QID", String.valueOf(idCounter.getAndIncrement()))
                .build();

        Response res = client.newCall(new Request.Builder().url(url).get().build()).execute();

        String json = res.body().string();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> m = new Gson().fromJson(json, type);
        List<String> replicas = (List<String>) m.get("ACTIVE_REPLICAS");
        final List<String> replicaAddresses = replicas.stream().map(this::extractHostAddress).collect(Collectors.toList());
        return GeoIpUtils.getClosestAddress(sender.getHostString(), replicaAddresses);
    }

    private String extractHostAddress(String activeReplicaAddress) {
        return activeReplicaAddress.replace("/", "").split(":")[0];
    }
}
