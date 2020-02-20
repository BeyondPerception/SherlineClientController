package ml.dent.app;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ml.dent.mill.MillController;
import ml.dent.net.SherlineNetworkClient;
import ml.dent.video.VideoServer;

public class Main extends Application {

	private String	hostname;
	private int		port;
	private int		internalPort;
	private boolean	enableSSL;
	private boolean	enableProxy;

	@Override
	public void start(Stage primaryStage) throws Exception {
		hostname = "bounceserver.ml";
		port = 443;
		internalPort = 1111;
		enableSSL = true;
		enableProxy = true;

		MillController controller = new MillController("localhost", 5007);
		controller.setIni("/home/sherline/Desktop/Sherline4Axis_inch.ini");
		SherlineNetworkClient client = new SherlineNetworkClient(hostname, port, controller);
		client.setController(controller);
		client.enableSSL(enableSSL);
		client.enableProxy(enableProxy);
		client.setInternalPort(internalPort);

		VideoServer videoServer = new VideoServer(hostname, port);
		videoServer.enableSSL(enableSSL);
		videoServer.enableProxy(enableProxy);
		videoServer.setInternalPort(internalPort);
		videoServer.setSource(new URI("rtsp://10.0.0.100:554/1"));
		videoServer.setH264Encoded(true);

		Stage window = primaryStage;

		window.setOnCloseRequest(event -> {
			event.consume();
			window.close();
			controller.stopLinuxCNC();
			while (controller.isConnectionActive())
				;
			client.disconnect();
			while (client.isConnectionActive())
				;
			videoServer.stopCapture();
			videoServer.disconnect();
			while (videoServer.isConnectionActive())
				;
			System.exit(0);
		});

		BorderPane root = new BorderPane();

		/** TOP MENU BAR */
		MenuItem connectionSettings = new MenuItem("Settings...");
		connectionSettings.setOnAction(event -> {
			Stage box = new Stage();

			box.initModality(Modality.APPLICATION_MODAL);
			box.setTitle("Connection Settings");

			GridPane grid = new GridPane();
			grid.setPadding(new Insets(10, 10, 10, 10));
			grid.setVgap(10);
			grid.setHgap(10);

			Label hostLabel = new Label("Server Host:");
			GridPane.setConstraints(hostLabel, 0, 0);

			TextField hostInput = new TextField(this.hostname);
			GridPane.setConstraints(hostInput, 1, 0);

			Label portLabel = new Label("Port:");
			GridPane.setConstraints(portLabel, 0, 1);

			TextField portInput = new TextField(Integer.toString(this.port));
			GridPane.setConstraints(portInput, 1, 1);

			CheckBox enableSsl = new CheckBox();
			GridPane.setConstraints(enableSsl, 1, 2);
			Label sslLabel = new Label("Enable SSL");
			GridPane.setConstraints(sslLabel, 0, 2);
			enableSsl.setOnAction(checkEvent -> {
				if (enableSsl.isSelected()) {
					this.enableSSL = true;
				} else {
					this.enableSSL = false;
				}
				client.enableSSL(this.enableSSL);
				videoServer.enableSSL(this.enableSSL);
			});

			if (this.enableSSL) {
				enableSsl.fire();
			}

			Label internalPortLabel = new Label("Internal Port");
			GridPane.setConstraints(internalPortLabel, 0, 4);

			TextField internalPortInput = new TextField(Integer.toString(this.internalPort));
			GridPane.setConstraints(internalPortInput, 1, 4);
			internalPortInput.setDisable(true);

			CheckBox enableProxy = new CheckBox();
			GridPane.setConstraints(enableProxy, 1, 3);
			Label proxyLabel = new Label("Enable Http(s) Proxy");
			GridPane.setConstraints(proxyLabel, 0, 3);
			enableProxy.setOnAction(checkEvent -> {
				if (enableProxy.isSelected()) {
					internalPortInput.setDisable(false);
					this.enableProxy = true;
				} else {
					internalPortInput.setDisable(true);
					this.enableProxy = false;
				}
				client.enableProxy(this.enableProxy);
				videoServer.enableProxy(this.enableProxy);
			});

			if (this.enableProxy) {
				enableProxy.fire();
			}

			Button saveButton = new Button("Save");
			saveButton.setOnAction(buttonEvent -> {
				String newHost = hostInput.getText().trim();
				String rawPort = portInput.getText().trim();

				// Validity Checks
				if (newHost.isEmpty()) {
					AlertBox.alert("ERROR", "Host name must not be empty", 250, 100, window);
					return;
				}
				if (!rawPort.matches("[0-9]+")) {
					AlertBox.alert("ERROR", "Port must be a number", 250, 100, window);
					return;
				}
				int newPort = Integer.parseInt(rawPort);
				if (newPort < 0 || newPort > 65535) {
					AlertBox.alert("ERROR", "Port must be within range 1-65535", 250, 100, window);
					return;
				}

				if (enableProxy.isSelected()) {
					String rawInternalPort = internalPortInput.getText().trim();
					if (!rawInternalPort.matches("[0-9]+")) {
						AlertBox.alert("ERROR", "Port must be a number", 250, 100, window);
						return;
					}
					int newInternalPort = Integer.parseInt(rawInternalPort);
					if (newInternalPort < 0 || newInternalPort > 65535) {
						AlertBox.alert("ERROR", "Internal port must be within range 1-65535", 250, 100, window);
						return;
					}
					this.internalPort = newInternalPort;
					client.setInternalPort(this.internalPort);
					videoServer.setInternalPort(this.internalPort);
				}

				this.hostname = newHost;
				this.port = newPort;
				client.setHost(this.hostname);
				client.setPort(this.port);
				videoServer.setHost(this.hostname);
				videoServer.setPort(this.port);
				box.close();
			});
			GridPane.setConstraints(saveButton, 5, 5);

			grid.getChildren().addAll(hostLabel, hostInput, portLabel, portInput, saveButton, sslLabel, enableSsl,
					proxyLabel, enableProxy, internalPortLabel, internalPortInput);

			Scene curScene = new Scene(grid, 450, 240);
			box.initOwner(window);
			box.setScene(curScene);
			box.show();
		});

		MenuItem closeConnection = new MenuItem("Close Connection");
		closeConnection.setOnAction(event -> {
			client.disconnect();

			if (!client.isConnectionActive()) {
				closeConnection.setDisable(true);
			}
		});
		closeConnection.setDisable(true);

		MenuItem connect = new MenuItem("Connect");
		connect.setOnAction(event -> {
			if (client.isConnectionActive()) {
				Alert alert = new Alert(AlertType.ERROR, "Connection already active", ButtonType.OK);
				alert.setHeaderText("Error");
				alert.initOwner(window);
				alert.showAndWait();
				return;
			}

			Alert alert = new Alert(AlertType.INFORMATION, "Attempting to connect to the server");
			alert.setTitle("Operation in progress");
			alert.setHeaderText("Please wait...");
			ProgressBar progress = new ProgressBar();
			alert.setGraphic(progress);
			alert.setOnShown(e -> {
				while (!client.connectionAttempted() || !client.proxyConnectionAttempted()) {
				}
				alert.close();
			});
			client.connect();
			alert.initOwner(window);
			alert.show();

			Alert response;
			if (client.isConnectionReady()) {
				closeConnection.setDisable(false);
				client.closeFuture().addListener(future -> {
					closeConnection.setDisable(true);
					if (client.isUnexpectedClose()) {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								Alert unexpectedDisconnectAlert = new Alert(AlertType.ERROR,
										"Unexpected disconnect from server");
								unexpectedDisconnectAlert.setHeaderText("Lost Connection to Server");
								unexpectedDisconnectAlert.initOwner(window);
								unexpectedDisconnectAlert.showAndWait();
							}
						});
					}
				});
				response = new Alert(AlertType.INFORMATION, "Successfully connected to the server", ButtonType.OK);
				response.setTitle("Operation completed successfully");
				response.setHeaderText("Success");
			} else {
				response = new Alert(AlertType.ERROR, client.getCloseReason(), ButtonType.OK);
				response.setTitle("Operation failed to complete");
				response.setHeaderText("Failed to connect to the server");
			}
			response.initOwner(window);
			response.showAndWait();
		});
		Menu connectionMenu = new Menu("_Connect");
		connectionMenu.getItems().addAll(connectionSettings, new SeparatorMenuItem(), connect, closeConnection);

		MenuItem videoSettings = new MenuItem("Video Settings...");
		videoSettings.setOnAction(event -> {
			Stage box = new Stage();

			box.initModality(Modality.APPLICATION_MODAL);
			box.setTitle("Video Settings");

			GridPane grid = new GridPane();
			grid.setPadding(new Insets(10, 10, 10, 10));
			grid.setVgap(8);
			grid.setHgap(10);

			ToggleGroup buttonGroup = new ToggleGroup();

			Label deviceNumberLabel = new Label("Device Path: ");
			TextField deviceNumerInput = new TextField("/dev/video0");
			GridPane.setConstraints(deviceNumberLabel, 0, 2);
			GridPane.setConstraints(deviceNumerInput, 2, 2);

			Label ipCameraURLLabel = new Label("IP Camera URL: ");
			TextField ipCameraURLInput = new TextField("rtsp://10.0.0.100:554/1");
			GridPane.setConstraints(ipCameraURLLabel, 0, 4);
			GridPane.setConstraints(ipCameraURLInput, 2, 4);

			Label tcpHostLabel = new Label("TCP Source Host: ");
			TextField tcpHostInput = new TextField("127.0.0.1");
			GridPane.setConstraints(tcpHostLabel, 0, 6);
			GridPane.setConstraints(tcpHostInput, 2, 6);

			Label tcpPortLabel = new Label("TCP Source Port: ");
			TextField tcpPortInput = new TextField("5100");
			GridPane.setConstraints(tcpPortLabel, 0, 7);
			GridPane.setConstraints(tcpPortInput, 2, 7);

			RadioButton defaultSelect = new RadioButton("Default Device");
			defaultSelect.setOnAction(even -> {
				deviceNumerInput.setDisable(true);
				deviceNumberLabel.setDisable(true);
				tcpHostLabel.setDisable(true);
				tcpHostInput.setDisable(true);
				tcpPortLabel.setDisable(true);
				tcpPortInput.setDisable(true);
				ipCameraURLLabel.setDisable(true);
				ipCameraURLInput.setDisable(true);
			});
			GridPane.setConstraints(defaultSelect, 0, 0);

			RadioButton webcamSelect = new RadioButton("Connected Webcam");
			webcamSelect.setOnAction(even -> {
				deviceNumerInput.setDisable(false);
				deviceNumberLabel.setDisable(false);

				tcpHostLabel.setDisable(true);
				tcpHostInput.setDisable(true);
				tcpPortLabel.setDisable(true);
				tcpPortInput.setDisable(true);
				ipCameraURLLabel.setDisable(true);
				ipCameraURLInput.setDisable(true);
			});
			GridPane.setConstraints(webcamSelect, 0, 1);

			RadioButton ipCameraSelect = new RadioButton("IP Camera");
			ipCameraSelect.setOnAction(even -> {
				ipCameraURLLabel.setDisable(false);
				ipCameraURLInput.setDisable(false);

				deviceNumerInput.setDisable(true);
				deviceNumberLabel.setDisable(true);
				tcpHostLabel.setDisable(true);
				tcpHostInput.setDisable(true);
				tcpPortLabel.setDisable(true);
				tcpPortInput.setDisable(true);
			});
			GridPane.setConstraints(ipCameraSelect, 0, 3);

			RadioButton tcpSourceSelect = new RadioButton("TCP Source");
			tcpSourceSelect.setOnAction(even -> {
				tcpHostLabel.setDisable(false);
				tcpHostInput.setDisable(false);
				tcpPortLabel.setDisable(false);
				tcpPortInput.setDisable(false);

				deviceNumerInput.setDisable(true);
				deviceNumberLabel.setDisable(true);
				ipCameraURLLabel.setDisable(true);
				ipCameraURLInput.setDisable(true);
			});
			GridPane.setConstraints(tcpSourceSelect, 0, 5);

			buttonGroup.getToggles().addAll(defaultSelect, webcamSelect, ipCameraSelect, tcpSourceSelect);

			switch (videoServer.getCameraType()) {
				case WEBCAM:
					webcamSelect.fire();
				case IP_CAMERA:
					ipCameraSelect.fire();
					break;
				case TCPSRC:
					tcpSourceSelect.fire();
					break;
				default:
					defaultSelect.fire();
					break;
			}

			CheckBox ish264Encoded = new CheckBox("Is the incoming video H.264 encoded?");
			ish264Encoded.setOnAction(checkEvent -> {
				if (ish264Encoded.isSelected()) {
					videoServer.setH264Encoded(true);
				} else {
					videoServer.setH264Encoded(false);
				}
			});
			GridPane.setConstraints(ish264Encoded, 0, 9, 5, 1);
			if (videoServer.getH264Encoded()) {
				ish264Encoded.fire();
			}

			Button saveButton = new Button("Save");
			saveButton.setOnAction(even -> {
				if (defaultSelect.isSelected()) {
					videoServer.setSource();
				}
				if (ipCameraSelect.isSelected()) {
					URI uri;
					try {
						uri = new URI(ipCameraURLInput.getText());
					} catch (URISyntaxException e) {
						Alert uriAlert = new Alert(AlertType.ERROR, "URL must be valid", ButtonType.OK);
						uriAlert.setHeaderText("URL Invalid");
						uriAlert.initOwner(box);
						uriAlert.showAndWait();
						return;
					}
					videoServer.setSource(uri);
				}
				if (webcamSelect.isSelected()) {
					String fileLoc = deviceNumerInput.getText();
					if (!new File(fileLoc).exists()) {
						Alert uriAlert = new Alert(AlertType.ERROR, "Device does not exist", ButtonType.OK);
						uriAlert.setHeaderText("Device must be a valid video device");
						uriAlert.initOwner(box);
						uriAlert.showAndWait();
						return;
					}
					videoServer.setSource(fileLoc);
				}
				if (tcpSourceSelect.isSelected()) {
					String host = tcpHostInput.getText();
					String portText = tcpPortInput.getText();
					int port;
					try {
						port = Integer.parseInt(portText);
						if (port < 1 || port > 65535) {
							throw new NumberFormatException();
						}
					} catch (NumberFormatException e) {
						Alert uriAlert = new Alert(AlertType.ERROR, "Invalid port number", ButtonType.OK);
						uriAlert.setHeaderText("Port number must be between 1 and 65565");
						uriAlert.initOwner(box);
						uriAlert.showAndWait();
						return;
					}
					videoServer.setSource(host, port);
				}
				box.close();
			});
			GridPane.setConstraints(saveButton, 10, 10);

			grid.getChildren().addAll(defaultSelect, webcamSelect, deviceNumberLabel, deviceNumerInput, ipCameraSelect,
					ipCameraURLLabel, ipCameraURLInput, tcpSourceSelect, tcpHostLabel, tcpHostInput, tcpPortLabel,
					tcpPortInput, ish264Encoded, saveButton);

			Scene curScene = new Scene(grid, 500, 320);
			box.initOwner(window);
			box.setScene(curScene);
			box.show();
		});

		MenuItem stopStreaming = new MenuItem("Stop Video");
		stopStreaming.setOnAction(event -> {
			videoServer.stopCapture();
			videoServer.disconnect();

			if (!videoServer.isConnectionActive()) {
				stopStreaming.setDisable(true);
			}
		});
		stopStreaming.setDisable(true);

		MenuItem startStreaming = new MenuItem("Start Video");
		startStreaming.setOnAction(event -> {
			if (videoServer.isConnectionActive()) {
				Alert alert = new Alert(AlertType.ERROR, "Video already started", ButtonType.OK);
				alert.setHeaderText("Error");
				alert.initOwner(window);
				alert.showAndWait();
				return;
			}

			Alert alert = new Alert(AlertType.INFORMATION, "Attempting to start streaming");
			alert.setTitle("Operation in progress");
			alert.setHeaderText("Please wait...");
			ProgressBar progress = new ProgressBar();
			alert.setGraphic(progress);
			alert.setOnShown(e -> {
				while (!videoServer.connectionAttempted() || !videoServer.proxyConnectionAttempted()) {
				}
				alert.close();
			});
			videoServer.connect();
			alert.initOwner(window);
			alert.show();

			Alert response;
			if (videoServer.isConnectionReady()) {
				stopStreaming.setDisable(false);
				videoServer.closeFuture().addListener(future -> {
					stopStreaming.setDisable(true);
					if (videoServer.isUnexpectedClose()) {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								Alert unexpectedDisconnectAlert = new Alert(AlertType.ERROR,
										"Unexpected stop of video stream");
								unexpectedDisconnectAlert.setHeaderText("Lost Connection to Server");
								unexpectedDisconnectAlert.initOwner(window);
								unexpectedDisconnectAlert.showAndWait();
							}
						});
					}
				});
				videoServer.startStream();
				response = new Alert(AlertType.INFORMATION, "Successfully started video stream", ButtonType.OK);
				response.setTitle("Operation completed successfully");
				response.setHeaderText("Success");
			} else {
				response = new Alert(AlertType.ERROR, client.getCloseReason(), ButtonType.OK);
				response.setTitle("Operation failed to complete");
				response.setHeaderText("Failed to start video stream");
			}
			response.initOwner(window);
			response.showAndWait();
		});

		Menu videoMenu = new Menu("_Video");
		videoMenu.getItems().addAll(videoSettings, new SeparatorMenuItem(), startStreaming, stopStreaming);

		MenuItem linuxCncSettings = new MenuItem("Settings...");
		linuxCncSettings.setOnAction(event -> {
			Stage box = new Stage();

			GridPane grid = new GridPane();
			grid.setPadding(new Insets(10, 10, 10, 10));
			grid.setVgap(8);
			grid.setHgap(10);

			Label linuxCncLabel = new Label("Linux CNC ini file location");
			GridPane.setConstraints(linuxCncLabel, 0, 0);

			TextField linuxCncInput = new TextField(controller.getIni());
			GridPane.setConstraints(linuxCncInput, 2, 0);

			Button saveButton = new Button("Save");
			GridPane.setConstraints(saveButton, 2, 2);
			GridPane.setHalignment(saveButton, HPos.RIGHT);
			saveButton.setOnAction(buttonEvent -> {
				String fileLoc = linuxCncInput.getText();
				File fileTest = new File(fileLoc);

				if (!fileTest.exists() && !fileLoc.equals("5007")) {
					Alert uriAlert = new Alert(AlertType.ERROR, "INI File does not exist", ButtonType.OK);
					uriAlert.setHeaderText("File location does not exist, must be path to ini file");
					uriAlert.initOwner(box);
					uriAlert.showAndWait();
					return;
				}

				controller.setIni(fileLoc);
				box.close();
			});

			grid.getChildren().addAll(linuxCncLabel, linuxCncInput, saveButton);

			Scene curScene = new Scene(grid, 400, 100);
			box.setScene(curScene);
			box.initOwner(window);
			box.show();
		});

		MenuItem stopLinuxCnc = new MenuItem("Stop Linux CNC");
		stopLinuxCnc.setOnAction(event -> {
			controller.stopLinuxCNC();
			stopLinuxCnc.setDisable(true);
		});
		stopLinuxCnc.setDisable(true);

		MenuItem startLinuxCnc = new MenuItem("Start Linux CNC");
		startLinuxCnc.setOnAction(event -> {
			if (controller.isStarted()) {
				Alert cannotStartTwiceAlert = new Alert(AlertType.ERROR, "LinuxCNC already started");
				cannotStartTwiceAlert.setHeaderText("Already Started");
				cannotStartTwiceAlert.initOwner(window);
				cannotStartTwiceAlert.showAndWait();
				return;
			}
			try {
				controller.startLinuxCNC();
				stopLinuxCnc.setDisable(false);
			} catch (Exception e) {
				AlertBox.alert("ERROR", "Failed to start Linux CNC", 250, 100, window);
				e.printStackTrace();
			}
		});

		Menu linuxCncMenu = new Menu("_LinuxCNC");
		linuxCncMenu.getItems().addAll(linuxCncSettings, new SeparatorMenuItem(), startLinuxCnc, stopLinuxCnc);

		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(connectionMenu, videoMenu, linuxCncMenu);

		Button emergencyStop = new Button("Emergency Stop");
		emergencyStop.setOnAction(event -> {
			controller.stopAllAxis();
		});

		root.setTop(menuBar);
		root.setLeft(emergencyStop);

		Scene scene = new Scene(root, 400, 400);
//		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		window.setScene(scene);
		window.setTitle("Sherline Client Controller");
		window.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
