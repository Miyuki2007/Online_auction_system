module Online_auction_system {
    // ===== JavaFX =====
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    // ===== Database =====
    requires java.sql;
    requires java.naming;
    requires mysql.connector.j;

    // ===== Mở reflection cho FXMLLoader inject @FXML =====
    opens controller to javafx.fxml;
    opens controller.seller to javafx.fxml;
    opens controller.bidder to javafx.fxml;
    opens controller.admin to javafx.fxml;
    // ===== Exports - cho phép module khác import =====
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
    exports protocol.requests;
    exports protocol.responses;
    exports server;
    exports dao;
}