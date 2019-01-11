module edu.sandiego.bcl {
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.web;
        requires generex;
        requires java.string.similarity;

	opens edu.sandiego.bcl to javafx.fxml;
	exports edu.sandiego.bcl;
}