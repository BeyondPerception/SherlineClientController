package ml.dent.mill;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;

import org.apache.commons.net.telnet.TelnetClient;

public class MillController {

	private String			hostname;
	private int				port;

	private int				axis;
	private int				speed;

	private TelnetClient	client;
	private PrintWriter		out;
//	private BufferedReader in;

	private String			iniLocation;

	private boolean			started;

	public MillController(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;

		client = new TelnetClient();
		axis = 0;
		speed = 1;
	}

	public MillController() {
		hostname = "localhost";
		port = 5007;

		client = new TelnetClient();
		axis = 0;
		speed = 1;
	}

	public void setSpeed(int x) {
		speed = x;
	}

	public int getSpeed() {
		return speed;
	}

	public void setAxis(int x) {
		axis = x;
	}

	public int getAxis() {
		return axis;
	}

	/**
	 * Sends the initialization sequence to linuxcnc to prime it for control
	 */
	public void initControl() throws SocketException, IOException {
		while (true) {
			try {
				client.connect(hostname, port);
				break;
			} catch (SocketException e) {
				// wait
			}
		}
		out = new PrintWriter(client.getOutputStream(), true);
//		in = new BufferedReader(new InputStreamReader(client.getInputStream()));

		write("hello EMC localhost 1");
		write("set enable EMCTOO");
		write("set estop off");
		write("set machine on");
		write("set mode manual");
		write("set echo off");
	}

	public boolean isMillAccessible() {
		return client.isConnected();
	}

	public void stopAllAxis() {
		write("set jog_stop 0");
		write("set jog_stop 1");
		write("set jog_stop 2");
		write("set jog_stop 3");
	}

	/**
	 * jogs the currently selected axis
	 */
	public void jog(int dir) {
		int dirSign = (int) Math.signum(dir);
		StringBuilder command = new StringBuilder();
		command.append("set jog ").append(axis).append(" ");
		if (axis == 3) {
			command.append(speed * 12 * dirSign);
		} else {
			command.append(speed * dirSign);
		}
		write(command.toString());
	}

	public void stop() {
		stopAllAxis();
	}

	private Process linuxCNC;

	public void startLinuxCNC() throws IOException {
		started = true;
		linuxCNC = new ProcessBuilder("/bin/bash", "-c", "/usr/bin/linuxcnc " + iniLocation).start();
		while (!linuxCNC.isAlive())
			;
		initControl();
	}

	public void stopLinuxCNC() {
		if (out == null || linuxCNC == null) {
			return;
		}
		try {
			write("shutdown");
			client.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// TODO Properly kill(-9) process by pid, bash is getting started, spawning the
		// linuxcnc process, and exiting. Since the process has exited, it thinks it
		// successfully destroys the process, but the child process is still alive
		// TEMPORARY BLOCK
		try {
			new ProcessBuilder("killall", "nc").start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TEMPORARY BLOCK
		linuxCNC.destroy();
		started = false;
	}

	public boolean isConnectionActive() {
		return client.isConnected();
	}

	public boolean isStarted() {
		return started;
	}

	public void setIni(String ini) {
		iniLocation = ini;
	}

	public String getIni() {
		return iniLocation;
	}

	public void write(String s) {
		if (out == null) {
			throw new IllegalStateException("Control of mill has yet to be initialized");
		}
		out.println(s);
		out.flush();
	}
}
