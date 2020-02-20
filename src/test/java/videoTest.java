import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Scanner;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Element;

import ml.dent.video.VideoServer;

public class videoTest {

	public static void main(String[] args) throws URISyntaxException {
		VideoServer server = new VideoServer("localhost", 1111);
		server.setSource(new URI("rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_175k.mov"));
//		server.setSource(new URI("file:///home/ronak/Downloads/legend.mp4"));
//		server.setSource(new URI("rtsp://10.0.0.100:554/1"));

		server.connect();

		while (!server.isConnectionActive()) {
			System.out.println("Connecting");
		}

		server.startStream();

		Scanner file = new Scanner(System.in);

		file.nextLine();

//		List<Element> e = b.getElements();
//		System.out.println(e);
//
//		for (Element ele : e) {
//			if (ele instanceof Bin) {
//				System.out.println(ele + ": " + ((Bin) ele).getElementsRecursive());
//			} else {
//				System.out.println(ele);
//			}
//		}

		file.nextLine();

		server.stopCapture();
		server.disconnect();
	}
}
