package ml.dent.net;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import ml.dent.mill.MillController;

public class SherlineNetworkClient extends SimpleNetworkClient {

	private MillController controller;

	public SherlineNetworkClient(String host, int port, MillController controller) {
		super(host, port, '0');
		this.controller = controller;
	}

	@Override
	public ChannelFuture connect() {
		return super.connect(new SherlineInboundHandler(), new SherlineOutboundHandler());
	}

	@Override
	public ChannelFuture connect(ChannelHandler... channelHandlers) {
		ChannelHandler[] newHandlers = new ChannelHandler[channelHandlers.length + 2];
		newHandlers[0] = new SherlineOutboundHandler();
		newHandlers[1] = new SherlineInboundHandler();
		for (int i = 2; i < newHandlers.length; i++) {
			newHandlers[i] = channelHandlers[i - 2];
		}

		return super.connect(newHandlers);
	}

	public void setController(MillController c) {
		controller = c;
	}

	public MillController getController() {
		return controller;
	}

	private static class Markers {
		public static final byte	PING_REQUEST	= 0x7f;
		public static final byte	PING_RESPONSE	= 0x7e;
		public static final byte	STOP			= 0x65;
		public static final byte	SPEED			= 0x73;
		public static final byte	JOG				= 0x6a;
		public static final byte	AXIS			= 0x61;
		public static final byte	MSG				= 0x6d;
		public static final byte	ESC_MSG			= 0x00;
	}

	private enum PingState {
		PING_SENT,
		PING_RECEIVED
	}

	private PingState	pingState;

	private String		closeReason;

	public String getCloseReason() {
		return closeReason;
	}

	private class SherlineInboundHandler extends ChannelInboundHandlerAdapter {
		Timer						pingTimer;
		ConcurrentLinkedQueue<Byte>	incoming;

		public SherlineInboundHandler() {
			incoming = new ConcurrentLinkedQueue<Byte>();

			pingTimer = new Timer(true);
			pingTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					if (pingState == PingState.PING_SENT && controller.isMillAccessible()) {
						controller.stopAllAxis();
						writeAndFlush(Markers.PING_REQUEST);
					}
					if (isConnectionActive()) {
						if (pingState == PingState.PING_RECEIVED) {
							writeAndFlush(Markers.PING_REQUEST);
							pingState = PingState.PING_SENT;
						}
					}
				}
			}, 0L, 5000L);
			pingState = PingState.PING_RECEIVED;

			Runnable processor = new Runnable() {
				@Override
				public void run() {
					while (true) {
						byte b = getNextByte();
						if (!controller.isMillAccessible()) {
							continue;
						}

						switch (b) {
							case Markers.PING_RESPONSE:
								pingState = PingState.PING_RECEIVED;
								break;
							case Markers.AXIS:
								controller.setAxis(getNextByte());
								break;
							case Markers.SPEED:
								controller.setSpeed(getNextByte());
								break;
							case Markers.JOG:
								controller.jog(getNextByte());
								break;
							case Markers.STOP:
								controller.stop();
								break;
							default:
								controller.stop();
						}
					}
				}
			};
			Thread processingThread = new Thread(processor);
			processingThread.setDaemon(true);
			processingThread.start();
		}

		/**
		 * Blocks until the next byte is available
		 * 
		 * @return the next incoming byte
		 */
		private byte getNextByte() {
			while (incoming.isEmpty())
				;
			return incoming.poll();
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			System.out.println("Successfully Connected to Server");
			getChannel().closeFuture().addListener(future -> {
				if (controller.isMillAccessible()) {
					controller.stopAllAxis();
				}
			});
			super.channelActive(ctx);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			ByteBuf buf = (ByteBuf) msg;
			byte[] bytes = new byte[buf.readableBytes()];
			buf.readBytes(bytes);

			for (byte b : bytes) {
				incoming.offer(b);
			}

			super.channelRead(ctx, msg);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			cause.printStackTrace();
			ctx.close();
		}
	}

	private class SherlineOutboundHandler extends ChannelOutboundHandlerAdapter {
		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			super.write(ctx, msg, promise);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			cause.printStackTrace();
			ctx.close();
			closeReason = cause.getLocalizedMessage();
		}
	}
}