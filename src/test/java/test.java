//import java.util.Scanner;
//
//import org.freedesktop.gstreamer.Gst;
//import org.freedesktop.gstreamer.Pipeline;
//
//import io.netty.util.Version;
//
///**
// * Miminal working video example to stream video over network
// * 
// * @author ronak
// */
//public class test {
//	public static void main(String args[]) throws Exception {
//		Gst.init(new Version(1, 16));
//		System.out.println("GST finished initialization.");
//
//		Scanner s = new Scanner(System.in);
//		Pipeline pipeline = (Pipeline) Gst
//				.parseLaunch("uridecodebin uri=rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov "
//						+ "! videoconvert " + "! x264enc ! mpegtsmux" + "! tcpserversink host=0.0.0.0 port=1111");
//		pipeline.play();
//		s.nextLine();
//		Gst.deinit();
//	}
//}