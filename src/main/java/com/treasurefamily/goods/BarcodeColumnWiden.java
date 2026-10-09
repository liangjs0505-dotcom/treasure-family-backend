package com.treasurefamily.goods;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 已有库的条码列较短。Hibernate 不会把旧列自动加长，启动时扩到当前上限。 */
@Component
public class BarcodeColumnWiden implements ApplicationRunner {

    private final DataSource dataSource;

    public BarcodeColumnWiden(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            widen(connection, "goods", "barcode", false);
            widen(connection, "barcode_catalog", "barcode", true);
        }
    }

    private static void widen(Connection connection, String table, String column, boolean required) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        String actualTable = findTable(meta, table);
        if (actualTable == null) {
            return;
        }
        int size = columnSize(meta, actualTable, column);
        if (size < 0 || size >= BarcodeCodes.MAX_LENGTH) {
            return;
        }
        boolean mysql = meta.getDatabaseProductName().toLowerCase(Locale.ROOT).contains("mysql");
        String nullability = required ? " NOT NULL" : "";
        String sql = mysql
                ? "ALTER TABLE " + actualTable + " MODIFY " + column + " VARCHAR(" + BarcodeCodes.MAX_LENGTH + ")" + nullability
                : "ALTER TABLE " + actualTable + " ALTER COLUMN " + column + " VARCHAR(" + BarcodeCodes.MAX_LENGTH + ")" + nullability;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String findTable(DatabaseMetaData meta, String table) throws SQLException {
        String found = lookupTable(meta, table);
        if (found != null) {
            return found;
        }
        return lookupTable(meta, table.toUpperCase(Locale.ROOT));
    }

    private static String lookupTable(DatabaseMetaData meta, String table) throws SQLException {
        try (ResultSet tables = meta.getTables(null, null, table, new String[] {"TABLE"})) {
            if (tables.next()) {
                return tables.getString("TABLE_NAME");
            }
        }
        return null;
    }

    private static int columnSize(DatabaseMetaData meta, String table, String column) throws SQLException {
        try (ResultSet columns = meta.getColumns(null, null, table, null)) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return columns.getInt("COLUMN_SIZE");
                }
            }
        }
        return -1;
    }
}
