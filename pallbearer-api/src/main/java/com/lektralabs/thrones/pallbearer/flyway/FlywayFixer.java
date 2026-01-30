package com.lektralabs.thrones.pallbearer.flyway;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * A handy class to help fix/revert Flyway Migrations
 */
public class FlywayFixer {

    private final Boolean enabled;
    private final List<String> sqlList;

    public FlywayFixer(boolean enabled, List<String> sqlList){
        this.enabled = enabled;
        this.sqlList = sqlList;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void fix(DataSource dataSource) {
        if (enabled) {
            innerFix(dataSource);
        }
    }

    public void innerFix(DataSource dataSource){
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();

            for (String sql : sqlList) {
                System.out.println("\nFlywayFixer running \n" + sql + "\n\n");
                stmt.execute(sql);
            }

        } catch (SQLException e) {
            e.printStackTrace(System.err);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // gulp
                }
            }

        }
    }

}
