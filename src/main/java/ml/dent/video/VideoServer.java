package ml.dent.video;

import java.net.URI;
import java.nio.ByteBuffer;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.FlowReturn;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Sample;
import org.freedesktop.gstreamer.Version;
import org.freedesktop.gstreamer.elements.AppSink;

import ml.dent.net.SimpleNetworkClient;

public class VideoServer extends SimpleNetworkClient {

	public enum Camera {
		WEBCAM,
		IP_CAMERA,
		TCPSRC,
		DEFAULT
	}

	// Describes whether the stream read from the camera will already be h264
	// encoded
	private boolean	h264Encoded;

	private Camera	cameraType;
	private String	source;

	public VideoServer(String host, int port, String deviceName) {
		this(host, port, Camera.WEBCAM);
		source = deviceName;
	}

	public VideoServer(String host, int port, URI uri) {
		this(host, port, Camera.IP_CAMERA);
		source = uri.toString();
	}

	public VideoServer(String host, int port, String tcpHost, int tcpPort) {
		this(host, port, Camera.TCPSRC);
		source = tcpHost + ":" + tcpPort;
	}

	/**
	 * Uses the default webcam as the camera source
	 */
	public VideoServer(String host, int port) {
		this(host, port, Camera.DEFAULT);
	}

	private VideoServer(String host, int port, Camera type) {
		super(host, port, '1');
		cameraType = type;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String deviceName) {
		source = deviceName;
		cameraType = Camera.WEBCAM;
	}

	public void setSource(URI uri) {
		source = uri.toString();
		cameraType = Camera.IP_CAMERA;
	}

	public void setSource(String tcpHost, int tcpPort) {
		source = tcpHost + ":" + tcpPort;
		cameraType = Camera.TCPSRC;
	}

	/**
	 * Uses the default webcam as the camera source
	 */
	public void setSource() {
		cameraType = Camera.DEFAULT;
	}

	public void setH264Encoded(boolean enc) {
		h264Encoded = enc;
	}

	public boolean getH264Encoded() {
		return h264Encoded;
	}

	public Camera getCameraType() {
		return cameraType;
	}

	private Pipeline pipeline;

	/**
	 * Starts the video stream by initializing Gstreamer and sends stream over the
	 * network. Blocks until connection is established
	 * 
	 * @return
	 */
	public void startStream() {
		if (!isConnectionReady()) {
			throw new IllegalStateException("Cannot start stream, connection not ready!");
		}

		Gst.init();

		while (!Gst.isInitialized())
			;

		String parseString;
		switch (cameraType) {
			case IP_CAMERA:
				parseString = "urisourcebin uri=" + source;
				parseString += " ! rtpjitterbuffer ! queue ! rtph264depay";
				break;
			case WEBCAM:
				parseString = "v4l2src device=" + source;
				break;
			case TCPSRC:
				int colonIndex = source.indexOf(":");
				String host = source.substring(0, colonIndex);
				String port = source.substring(colonIndex + 1);
				parseString = "tcpclientsrc host=" + host + " port=" + port;
				break;
			default:
				parseString = "v4l2src";
		}

//		if (h264Encoded) {
		parseString += " ! h264parse ! queue ! mpegtsmux ! queue ! appsink name=sink sync=false";
//		} else {
//			parseString += " ! queue ! videoconvert ! queue ! x264enc tune=\"zerolatency\" ! queue ! appsink name=sink";
//		}

		pipeline = (Pipeline) Gst.parseLaunch(parseString);

		pipeline.getBus().connect((Bus.ERROR) (source, code, message) -> {
			System.out.println("Error Source: " + source.getName());
			System.out.println("Error Code: " + code);
			System.out.println("Error Message: " + message);
		});
		pipeline.getBus().connect((Bus.WARNING) (source, code, message) -> {
			System.out.println("Warn Source: " + source.getName());
			System.out.println("Warn Code: " + code);
			System.out.println("Warn Message: " + message);
		});

		AppSink sink = (AppSink) pipeline.getElementByName("sink");
		sink.set("emit-signals", true);
		sink.connect(new AppSink.NEW_SAMPLE() {
			@Override
			public FlowReturn newSample(AppSink elem) {
				Sample sample = elem.pullSample();
				ByteBuffer buf = sample.getBuffer().map(false);

				byte[] bytes = new byte[buf.remaining()];
				buf.get(bytes);

				for (byte b : bytes) {
					write(b);
				}
				flush();

				sample.dispose();
				return FlowReturn.OK;
			}
		});
		sink.connect(new AppSink.NEW_PREROLL() {
			@Override
			public FlowReturn newPreroll(AppSink elem) {
				Sample sample = elem.pullPreroll();
				ByteBuffer buf = sample.getBuffer().map(false);

				byte[] bytes = new byte[buf.remaining()];
				buf.get(bytes);

				for (byte b : bytes) {
					write(b);
				}
				flush();

				sample.dispose();
				return FlowReturn.OK;
			}
		});

		pipeline.play();
	}

	public void stopCapture() {
		if (pipeline == null) {
			return;
		}
		pipeline.stop();
		pipeline.close();
		pipeline = null;
		Gst.deinit();
		while (Gst.isInitialized())
			;
	}
}
