import java.sql.*;
import oracle.jdbc.OracleConnection;
import java.sql.SQLException;
import java.util.Scanner;

class Driver {
    static Connection dbConnection;
    static Statement SQLStatement;
    static ResultSet SQLResult;
    static Scanner scanner;

    public static void main(String[] args) throws SQLException {
        connectDatabase();
        createTables();
        insertData();
        printTables();
        userMenu();
        deleteTables();
        disconnectDatabase();
    }

    public static void connectDatabase() throws SQLException {
        scanner = new Scanner(System.in);

        System.out.println();
        System.out.println("Connecting to Oracle SQL Server...");
        DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
        dbConnection = DriverManager.getConnection("jdbc:oracle:thin:@//sabzevi2.homeip.net:1521/FREEPDB1", "csus",
                "student");
        SQLStatement = dbConnection.createStatement();
        System.out.println("Connected to Oracle SQL Server!");

    }

    public static void userMenu() {
        try {
            menu();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            scanner.close();
            scanner = new Scanner(System.in);
        }
    }
    
    public static void menu() throws SQLException {
        System.out.println();
        int choice = 0;
        while (true) {
            System.out.println("1) Insert a Book");
            System.out.println("2) Delete a Book");
            System.out.println("3) Update a Book");
            System.out.println("4) View all Book");
            System.out.println("5) Quit");
            String inputLine = scanner.nextLine();
            if (inputLine.length() <= 1) {
                choice = Integer.parseInt(inputLine);
            }

            switch (choice) {
                case 1:
                    insertBook();
                    break;
                case 2:
                    deleteBook();
                    break;
                case 3:
                    updateBook();
                    break;
                case 4:
                    viewAllBooks();
                    break;
                case 5:
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
                    break;
            }
        
        }
    
    }

    public static void createTables() throws SQLException {
        System.out.println("Creating vo_library and vo_book tables...");

        SQLStatement.executeQuery("""
                                    CREATE TABLE vo_library (
                                        libraryID NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                        name VARCHAR2(255),
                                        address VARCHAR2(255))
                                    """);

        SQLStatement.executeQuery("""
                                    CREATE TABLE vo_book (
                                        bookID NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
                                        title VARCHAR2(255), 
                                        author VARCHAR2(255), 
                                        libraryID NUMBER REFERENCES vo_library(libraryID))
                """);

        System.out.println("Completed creating tables!");
    }

    public static void insertData() throws SQLException {
        System.out.println();
        System.out.println("Inserting dummy data into vo_library...");
        SQLStatement.executeUpdate("""
            INSERT INTO vo_library (name, address)
            VALUES ('Central Library', '123 Knowledge Way, Booksville, BK 12345')
            """);

        SQLStatement.executeUpdate("""
            INSERT INTO vo_library (name, address)
            VALUES ('Westside Branch', '456 Reading Rd, Chapterburg, CH 67890')
            """);

        SQLStatement.executeUpdate("""
            INSERT INTO vo_library (name, address)
            VALUES ('Technology Archive', '789 Silicon St, Code City, CC 11235')
            """);

        System.out.println("Library data inserted.");
        System.out.println("Inserting dummy data into vo_book...");

        SQLStatement.executeUpdate("""
            INSERT INTO vo_book (title, author, libraryID)
            VALUES ('The SQL Enigma', 'Dr. Query Parse', 1)
            """);

        SQLStatement.executeUpdate("""
            INSERT INTO vo_book (title, author, libraryID)
            VALUES ('Advanced Java Patterns', 'Prof. Singleton Factory', 1)
            """);

        SQLStatement.executeUpdate("""
            INSERT INTO vo_book (title, author, libraryID)
            VALUES ('Database Design Fundamentals', 'Norma Lizer', 2)
            """);

        SQLStatement.executeUpdate("""
            INSERT INTO vo_book (title, author, libraryID)
            VALUES ('Networking Essentials', 'Connecto Routerson', 2)
            """);

        SQLStatement.executeUpdate("""
            INSERT INTO vo_book (title, author, libraryID)
            VALUES ('Oracle Performance Tuning', 'Sys Adminovich', 3)
            """);

         SQLStatement.executeUpdate("""
            INSERT INTO vo_book (title, author, libraryID)
            VALUES ('History of Booksville', 'Local Historian', 1)
            """); 

        System.out.println("Book Data Inserted!");  
    }

    public static void printTables() throws SQLException {
        printLibraryTable();
        System.out.println();
        printBookTable();
    }

    public static void printLibraryTable() throws SQLException {
        System.out.println();
        SQLResult = SQLStatement.executeQuery("SELECT * FROM vo_library");
        System.out.println("vo_library:");
        System.out.println("libraryID | name | address");
        while (SQLResult.next()) {
            System.out.println(SQLResult.getInt("libraryID") + " | " + SQLResult.getString("name") + " | "
                    + SQLResult.getString("address"));
        }
        System.out.println();

    }

    public static void printBookTable() throws SQLException {
        System.out.println();
        SQLResult = SQLStatement.executeQuery("SELECT * FROM vo_book LEFT OUTER JOIN vo_library ON vo_book.libraryID = vo_library.libraryID");
        System.out.println("vo_book:");
        System.out.println("bookID | Book title | Book author | Library ID | Library name | Library address");
        while (SQLResult.next()) {
            System.out.println(SQLResult.getInt("bookID") + " | " + SQLResult.getString("title") + " | "
                    + SQLResult.getString("author") + " | " + SQLResult.getInt("libraryID") + " | " + SQLResult.getString("name") + " | "
                    + SQLResult.getString("address"));
        }
        System.out.println();

    }

    public static void deleteTables() throws SQLException {
        System.out.println();
        System.out.println("Deleting vo_library and vo_book tables...");
        SQLResult = SQLStatement.executeQuery("DROP TABLE vo_book");
        SQLResult = SQLStatement.executeQuery("DROP TABLE vo_library");
        System.out.println("Completed deleting tables!");

    }
    
    public static void disconnectDatabase() throws SQLException {
        SQLStatement.close();
        dbConnection.close();
        System.out.println();
        System.out.println("Disconnected from Oracle SQL Server!");
        System.out.println("Bye!");
    }

    public static void insertBook() throws SQLException {
        System.out.println();
        System.out.println("--- Insert Book ---");
        System.out.println("Enter book title");
        String title = scanner.nextLine();
        System.out.println("Enter book author");
        String author = scanner.nextLine();
        printLibraryTable();
        System.out.println("Enter library ID");
        int libraryID = Integer.parseInt(scanner.nextLine());
        
        SQLStatement.executeQuery("INSERT INTO vo_book (title, author, libraryID) VALUES ('" + title + "', '" + author + "', " + libraryID + ")");

        System.out.println("Book inserted!");



        printTables();
    }

    public static void deleteBook() throws SQLException {
        System.out.println();
        System.out.println("--- Delete Book ---");
        System.out.println("Enter book title");
        String title = scanner.nextLine();
        System.out.println("Enter book author");
        String author = scanner.nextLine();
        printLibraryTable();
        System.out.println("Enter library ID");
        int libraryID = Integer.parseInt(scanner.nextLine());
        
        SQLStatement.executeQuery("DELETE FROM vo_book WHERE title='" + title + "' AND author='" + author + "' AND libraryID=" + libraryID);

        System.out.println("Book Deleted!");
        printTables();
    }

    public static void updateBook() throws SQLException {
        System.out.println();
        System.out.println("--- Update Book ---");
        System.out.println("Enter book title");
        String title = scanner.nextLine();
        System.out.println("Enter book author");
        String author = scanner.nextLine();
        printLibraryTable();
        System.out.println("Enter library ID");
        int libraryID = Integer.parseInt(scanner.nextLine());
        

        System.out.println("--- NEW Book Information ---");
        System.out.println("Enter new book title");
        String newTitle = scanner.nextLine();
        System.out.println("Enter new book author");
        String newAuthor = scanner.nextLine();
        printLibraryTable();
        System.out.println("Enter new library ID");
        int newLibraryID = Integer.parseInt(scanner.nextLine());
        
        
        SQLStatement.executeQuery("UPDATE vo_book SET title='" + newTitle + "', author='" + newAuthor + "', libraryID=" + newLibraryID + " WHERE title='" + title + "' AND author='" + author + "' AND libraryID=" + libraryID);

        System.out.println("Book Updated!");


        printTables();
    }

    public static void viewAllBooks() throws SQLException {
        System.out.println();
        printTables();
    }
    

}




