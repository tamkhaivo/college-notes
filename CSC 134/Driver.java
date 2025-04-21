import java.sql.*;
import oracle.jdbc.OracleConnection;

class Driver {
    public static void main(String[] args) throws Exception {
        DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
        Connection con = DriverManager.getConnection("jdbc:oracle:thin:@//sabzevi2.homeip.net:1521/FREEPDB1", "csus",
                "student");
        Statement st = con.createStatement();
        try {
            st.executeQuery("drop table test");
        }
        catch (SQLException s) {

        }
        st.executeQuery("create table test (col1 number, col2 number)");
        st.executeQuery("insert into test values (1, 2)");
        ResultSet rs = st.executeQuery("select * from test");
        while (rs.next()) {
            System.out.println(rs.getInt(1) + " " + rs.getInt(2));
        }
        con.close();
    }
}