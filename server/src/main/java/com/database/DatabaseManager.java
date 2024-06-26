package com.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.server.Server;

public class DatabaseManager {
   private static final String DATABASE_URL = "jdbc:mysql://0.0.0.0:3307/chat-app";
   private static final String DATABASE_USER_NAME = "root";
   private static final String DATABASE_PASSWORD = "root";

   public static Connection getConnection() {
      try {
         Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
      } catch (Exception ex) {
         Server.logger.info("Can not register driver");
         // handle the error
      } 
      try {
         return DriverManager.getConnection(DATABASE_URL, DATABASE_USER_NAME, DATABASE_PASSWORD);
      } catch (SQLException ex) {
         printSQLException(ex);
         return null; // or handle the exception accordingly
      }
   }

   public static void printSQLException(SQLException ex) {
      for (Throwable e : ex) {
         if (e instanceof SQLException) {
            e.printStackTrace(System.err);
            System.err.println("SQLState: " + ((SQLException) e).getSQLState());
            System.err.println("Error Code: " + ((SQLException) e).getErrorCode());
            System.err.println("Message: " + e.getMessage());
            Throwable t = ex.getCause();
            while (t != null) {
               System.out.println("Cause: " + t);
               t = t.getCause();
            }
         }
      }
   }
}
