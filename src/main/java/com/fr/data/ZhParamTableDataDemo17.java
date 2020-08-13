package com.fr.data;

import com.fr.json.JSONArray;
import com.fr.json.JSONObject;
import com.fr.log.FineLoggerFactory;
import com.fr.log.FineLoggerProvider;
import com.fr.stable.ParameterProvider;
import okhttp3.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * 带参数的程序数据集Demo
 *
 * @author fanruan
 */
public class ZhParamTableDataDemo17 extends AbstractTableData {
    static FineLoggerProvider logger = FineLoggerFactory.getLogger();
    /**
     * 列名数组，保存程序数据集所有列名
     */
    private String[] columnNames;
    /**
     * 定义程序数据集的列数量
     */
    private int columnNum = 4;
    /**
     * 保存查询表的实际列数量
     */
    private int colNum = 4;
    /**
     * 保存查询得到列值
     */
    private ArrayList valueList = null;


    // 设置连接超时时间(单位毫秒)
    private static int CONNECTION_TIME_OUT = 30000;
    // 设置读数据超时时间(单位毫秒)
    private static int SO_TIME_OUT = 30000;

    /**
     * 构造函数，定义表结构，该表有10个数据列，列名为column#0，column#1，。。。。。。column#9
     */
    public ZhParamTableDataDemo17() {

    }

    private HashMap<String, Object> detectParameters() {
        Iterator iterator = parameters.get().iterator();

        HashMap<String, Object> hashMap = new HashMap<>();
        while (iterator.hasNext()) {
            ParameterProvider next = (ParameterProvider) iterator.next();
            String name = next.getName();
            logger.debug("next.getName()" + name);
            Object value = next.getValue();
            logger.debug("next.getValue()" + value);
            hashMap.put(name, value);
        }
        return hashMap;
    }

    private void initClumns() {
        if (columnNames != null) {
            return;
        }

        logger.debug(parameters.toString());
        logger.debug("parameters.getNameSpace()" + "=" + parameters.getNameSpace());
        logger.debug("parameters.getProperty()" + "=" + parameters.getProperty());
        logger.debug("parameters.getKey()" + "=" + parameters.getKey());
        logger.debug("parameters.getData()" + "=" + parameters.getData());
        logger.debug("parameters.get()" + "=" + parameters.get());
        logger.debug("parameters.get().isEmpty()" + "=" + parameters.get().isEmpty());
        logger.debug("parameters.get().toArray()" + "=" + parameters.get().toArray());

        Iterator iterator = parameters.get().iterator();

        String indexName = null;
        while (iterator.hasNext()) {
            ParameterProvider next = (ParameterProvider) iterator.next();
            String name = next.getName();
            logger.debug("next.getName()" + name);
            Object value = next.getValue();
            logger.debug("next.getValue()" + value);
            if ("indexName".equals(name)) {
                indexName = value.toString();
            }
        }


        logger.debug("indexName：" + indexName);

        try {

            Connection con = getConnection();
            String sql = "select * from col_table where index_name = ?";
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, indexName);
            ResultSet rs = stmt.executeQuery();
            // 获得记录的详细信息，然后获得总列数
            ResultSetMetaData rsmd = rs.getMetaData();
            colNum = rsmd.getColumnCount();

//            columnNames = new String[columnNum];
            ArrayList<String> list = new ArrayList<>();

            while (rs.next()) {
                String fileName = rs.getString("name");
                logger.debug("fileName=" + fileName);
                list.add(fileName);
            }

            columnNames = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                columnNames[i] = list.get(i);
            }
            columnNum = columnNames.length;
            // 释放数据库资源
            rs.close();
            stmt.close();
            con.close();
            // 打印一共取到的数据行数量

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 实现其他四个方法
     *
     * @return columnNum
     */
    @Override
    public int getColumnCount() {
        logger.debug("------------getColumnCount start------------");
        initClumns();
        logger.debug("------------getColumnCount end------------");
        return columnNum;

    }

    @Override
    public String getColumnName(int columnIndex) {
        logger.debug("------------getColumnName start------------");
        initClumns();
        logger.debug("------------getColumnName end------------");
        return columnNames[columnIndex];
    }

    @Override
    public int getRowCount() {
        logger.debug("------------getColumnName start------------");
        init();
        logger.debug("------------getColumnName end------------");
        return valueList.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        logger.debug("------------getValueAt start------------");
        init();
        if (columnIndex >= colNum) {
            return null;
        }
        logger.debug("------------getValueAt end------------");
        return ((Object[]) valueList.get(rowIndex))[columnIndex];
    }


    private static String okRequest(String request) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, request);

        Response response = null;
        String responseStr = null;
        try {
            response = httpPost("http://127.0.0.1:8903/book/queryFromFR", requestBody);
            responseStr = new String(response.body().bytes(), "utf-8");
            System.out.println(responseStr);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return responseStr;

    }

    public static Response httpPost(String url, RequestBody requestBody) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client
                .newCall(request)
                .execute();
        if (response.isSuccessful()) {
            return response;
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

    /**
     * 准备数据
     */
    private void init() {
        HashMap<String, Object> hashMap = detectParameters();
        JSONObject object = JSONObject.create(hashMap);

        String requestParameter = object.toString();
        logger.debug("requestParameter=" + requestParameter);

        try{
            String response = okRequest(requestParameter);
            JSONArray jsonArray = new JSONArray(response);

            valueList = new ArrayList();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Object[] datas = new Object[columnNames.length];
                for (int j = 0; j < columnNames.length; j++) {
                    String data = jsonObject.getString(columnNames[j]);
                    datas[j] = data;
                }
                valueList.add(datas);
            }
        }catch (Exception e){
            e.printStackTrace();
        }


        logger.debug("valueList：" + valueList);
    }


    /**
     * 获取数据库连接 driverName和 url 可以换成您需要的
     *
     * @return Connection
     */
    public Connection getConnection() {

        String driverName = "com.mysql.jdbc.Driver";
        String url = "jdbc:mysql://bd-test-cdh-243-25:3306/test?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull";
        String username = "kafka_eagle";
        String password = "kafka_eagle";
        Connection con;
        try {
            Class.forName(driverName);
            con = DriverManager.getConnection(url, username, password);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return con;
    }


    /**
     * 释放一些资源，因为可能会有重复调用，所以需释放valueList，将上次查询的结果释放掉
     *
     * @throws Exception e
     */
    @Override
    public void release() throws Exception {
        super.release();
        this.valueList = null;
    }

    public static void main(String[] args) throws IOException {
        okRequest("xx");
    }


}