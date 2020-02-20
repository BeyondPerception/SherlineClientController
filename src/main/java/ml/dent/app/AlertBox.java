package ml.dent.app;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AlertBox {
	public static void alert(String title, String message, int width, int height, Stage parent) {
		Stage window = new Stage();

		window.initModality(Modality.APPLICATION_MODAL);
		window.setTitle(title);
		window.setMinHeight(height);
		window.setMinWidth(width);

		Label label = new Label();
		label.setText(message);

		Button closeButton = new Button("OK");
		closeButton.setOnAction(event -> {
			window.close();
		});

		VBox layout = new VBox();
		layout.getChildren().addAll(label, closeButton);
		layout.setAlignment(Pos.CENTER);

		Scene scene = new Scene(layout);
		window.initOwner(parent);
		window.setScene(scene);
		window.showAndWait();
	}
}
