import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

public class fxTest extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		Alert alert = new Alert(AlertType.INFORMATION, "Attempting to connect to the server", ButtonType.CANCEL);
		alert.setTitle("Operation in progress");
		alert.setHeaderText("Please wait...");
		ProgressIndicator progress = new ProgressIndicator();
		alert.setGraphic(progress);
		Label result = new Label();

		Task<Void> task = new Task<Void>() {
			{
				setOnFailed(a -> {
					alert.close();
					updateMessage("Failed to connect to the server");
				});
				setOnSucceeded(a -> {
					alert.close();
					updateMessage("Connection to the server successfull");
				});
				setOnCancelled(a -> {
					alert.close();
					updateMessage("Operation cancelled");
				});
			}

			public Void call() {
				updateMessage("Connecting");

				while (true) {
				}
			}
		};
		result.textProperty().unbind();
		result.textProperty().bind(task.messageProperty());

		Thread thread = new Thread(task);
		thread.start();

		Stage window = new Stage();

		alert.initOwner(window);
		alert.showAndWait();

		window.setScene(new Scene(new BorderPane()));
		window.show();
	}

	public static void main(String[] args) {
		launch(args);
	}

}
