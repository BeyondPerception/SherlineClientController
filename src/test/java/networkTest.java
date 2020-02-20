import java.util.Arrays;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import javafx.stage.Stage;
import ml.dent.net.SherlineNetworkClient;
import ml.dent.net.SimpleNetworkClient;

public class networkTest {

	static SherlineNetworkClient	client;
	static Supplier<Boolean>		test;
	static Stage					window;

	public static void main(String[] args) throws Exception {
////		client = new SherlineNetworkClient("bounceserver.ml", 443);
//		client.enableSSL(true);
//		client.enableProxy(true);
//		client.setInternalPort(1111);
//		ChannelFuture cf = client.connect(new Test());
//
//		while (!cf.isDone()) {
//		}

		SimpleNetworkClient client = new SimpleNetworkClient("bounceserver.ml", 443, '1');
		client.enableProxy(true);
		client.enableSSL(true);
		client.setInternalPort(1111);
		client.connect(new Test());

		while (!client.isConnectionActive())
			;
		while (!client.isProxyEstablished())
			;

		Thread.sleep(1000);

		for (byte i = -5; i <= 10; i++) {
			while (!client.isWritable())
				System.out.println("Waiting");
			;
			System.out.println("Writing " + i);
			client.write(i);
			client.flush();
		}

		client.write((byte) 97);
		client.flush();

//		client.disconnect();
	}
}

class Test extends ChannelInboundHandlerAdapter {
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("Connected");
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("Read");
		ByteBuf buf = (ByteBuf) msg;
		byte[] bytes = new byte[buf.readableBytes()];
		buf.readBytes(bytes);
		System.out.println(Arrays.toString(bytes));
		super.channelRead(ctx, msg);
	}
}