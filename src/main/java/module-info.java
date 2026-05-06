module Online_auction_system {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;
    requires java.sql;
    requires mysql.connector.j;

    // FXMLLoader cần reflection vào các package controller/client để inject @FXML
    opens controller to javafx.fxml;
    opens client    to javafx.fxml;

    exports client;
    exports controller;
    exports model;
    exports model.user;
    exports model.item;
    exports model.auction;
    exports model.auction.observer;
    exports model.auction.exception;
    exports model.factory;
    exports model.manager;
    exports protocol;
    exports server;
}
