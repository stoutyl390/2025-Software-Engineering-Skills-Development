package com.example.myapplication.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgreSqlUtil {
    // 动态参数示例（实际应从配置或输入获取）
    private static final String IP = "your_postgres_ip";
    private static final String PORT = "5432";
    private static final String DB_NAME = "your_db_name";
    private static final String USER = "your_username";
    private static final String PASSWORD = "your_password";

    private static final String URL = "jdbc:postgresql://" + IP + ":" + PORT + "/" + DB_NAME;

    public static Connection openConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found", e);
        }
    }
}