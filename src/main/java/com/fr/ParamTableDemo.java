package com.fr;


import com.fr.base.FRContext;
import com.fr.data.AbstractTableData;
import com.fr.data.impl.Connection;
import com.fr.file.DatasourceManager;
import com.fr.log.FineLoggerFactory;
import com.fr.stable.ParameterProvider;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

public class ParamTableDemo extends AbstractTableData {
    private String[] columnNames;
    private int columnNum = 10;
    private int colNum = 0;
    private ArrayList valueList = null;

    public ParamTableDemo() {
        this.columnNames = new String[this.columnNum];

        for(int i = 0; i < this.columnNum; ++i) {
            this.columnNames[i] = "column#" + String.valueOf(i);
        }

    }

    @Override
    public int getColumnCount() {
        return this.columnNum;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return this.columnNames[columnIndex];
    }

    @Override
    public int getRowCount() {
        this.init();
        return this.valueList.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        this.init();
        return columnIndex >= this.colNum ? null : ((Object[])((Object[])this.valueList.get(rowIndex)))[columnIndex];
    }

    private void init() {
        if (this.valueList == null) {
            String tableName = ((ParameterProvider)((Collection)this.parameters.get()).toArray()[0]).getValue().toString();
            String sql = "select * from " + tableName;
            FineLoggerFactory.getLogger().info("Query SQL of ParamTableDataDemo: \n" + sql);
            this.valueList = new ArrayList();
            Connection conn = DatasourceManager.getInstance().getConnection("FRDemo");

            try {
                java.sql.Connection con = conn.createConnection();
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                ResultSetMetaData rsmd = rs.getMetaData();
                this.colNum = rsmd.getColumnCount();
                Object[] objArray = null;

                while(rs.next()) {
                    objArray = new Object[this.colNum];

                    for(int i = 0; i < this.colNum; ++i) {
                        objArray[i] = rs.getObject(i + 1);
                    }

                    this.valueList.add(objArray);
                }

                rs.close();
                stmt.close();
                con.close();
            } catch (Exception var10) {
                var10.printStackTrace();
            }

        }
    }

    public java.sql.Connection getConnection() {
        String driverName = "org.sqlite.JDBC";
        String url = "jdbc:sqlite:////Applications//FineReport10_325//webapps//webroot//help//FRDemo.db";
        String username = "";
        String password = "";

        try {
            Class.forName(driverName);
            java.sql.Connection con = DriverManager.getConnection(url, username, password);
            return con;
        } catch (Exception var7) {
            var7.printStackTrace();
            return null;
        }
    }

    @Override
    public void release() throws Exception {
        super.release();
        this.valueList = null;
    }
}
