module com.rubinin.seng3320assign2 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.rubinin.seng3320assign2 to javafx.fxml;
    exports com.rubinin.seng3320assign2;
}