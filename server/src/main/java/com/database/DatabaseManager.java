package com.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.server.Server;

public class DatabaseManager {
   private static final String DATABASE_URL = "jdbc:mysql://0.0.0.0:3307/chat-app";
   private static final String DATABASE_USER_NAME = "demo_java";
   private static final String DATABASE_PASSWORD = "1234";

   public static Connection getConnection() {
      try {
         // The newInstance() call is a work around for some
         // broken Java implementations

         Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
      } catch (Exception ex) {
         Server.logger.info("Can not register driver");
         // handle the error
      }      
      Server.logger.info("getConnection() method enter");
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