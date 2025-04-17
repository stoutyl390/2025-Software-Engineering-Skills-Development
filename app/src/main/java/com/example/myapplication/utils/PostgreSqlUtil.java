package com.example.myapplication.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgreSqlUtil {
    // 动态参数示例（实际应从配置或输入获取）
    private static final String IP = "rm-cn-qzy481ha70002gvo.rwlb.rds.aliyuncs.com";
    private static final String PORT = "3306";
    private static final String DB_NAME = "db_001";
    private static final String USER = "user1";
    private static final String PASSWORD = "341628219Lhl";

    private static final String URL = "jdbc:mysql://" + IP + ":" + PORT + "/" + DB_NAME+ "?useSSL=false&serverTimezone=UTC";

    public static Connection openConnection() throws SQLException {
        try {
            Class.forName("com.mysql.jdbc.Driver");//org.postgresql.Driver
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
    }
}