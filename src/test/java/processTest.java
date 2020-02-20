import java.io.IOException;

public class processTest {
	public static void main(String[] args) throws IOException, InterruptedException {
		Process linuxCNC = new ProcessBuilder("/usr/bin/nc", "-k", "-l", "5007").start();
		Thread.sleep(5000);
		linuxCNC.destroy();
		while (linuxCNC.isAlive()) {
			linuxCNC.destroy();
			System.out.println(linuxCNC.isAlive());
		}
		System.out.println(linuxCNC.isAlive());
	}
}
