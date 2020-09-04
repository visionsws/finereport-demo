package com.fr.data;

import com.fr.base.Parameter;
import com.fr.json.JSONArray;
import com.fr.json.JSONObject;
import com.fr.log.FineLoggerFactory;
import com.fr.log.FineLoggerProvider;
import com.fr.stable.ParameterProvider;
import com.fr.third.alibaba.druid.support.json.JSONUtils;
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
@SuppressWarnings("unchecked")
public class ParamTableToApiData extends AbstractTableData {
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
    public ParamTableToApiData() {

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
            //这个是col_table表的列数
            //colNum = rsmd.getColumnCount();

//            columnNames = new String[columnNum];
            ArrayList<String> list = new ArrayList<>();

            while (rs.next()) {
                String fileName = rs.getString("name");
                logger.debug("fileName=" + fileName);
                list.add(fileName);
            }
            colNum = list.size();
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


    private static JSONArray okRequest(String request) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, request);

        Response response = null;
        JSONArray responseStr = null;
        try {
            response = httpPost("https://dompapi.bluemoon.com.cn/bd-demo/finereport/getData", requestBody);
            String responseRes = new String(response.body().bytes(), "utf-8");
            JSONObject resJson = new JSONObject(responseRes);
            if (resJson.getInt("code") == 200){
                responseStr = resJson.getJSONArray("content");
            }
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
        // 确保只被执行一次
        if (valueList != null) {
            return;
        }
        HashMap<String, Object> hashMap = detectParameters();
        JSONObject object = JSONObject.create(hashMap);

        String requestParameter = object.toString();
        logger.debug("requestParameter=" + requestParameter);

        try{
            JSONArray jsonArray = okRequest(requestParameter);
            //JSONArray jsonArray = new JSONArray(response);

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
        String url = "jdbc:mysql://192.168.243.20:9097/demo_test?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull";
        String username = "root";
        String password = "b#12345678";
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
        ParamTableToApiData demo = new ParamTableToApiData();

        ParameterProvider parameterProvider = new Parameter();
        parameterProvider.setName("indexName");
        parameterProvider.setValue("sap_test");
        demo.parameters.add(parameterProvider);

        ParameterProvider pp1 = new Parameter();
        pp1.setName("MATERIAL_ID1");
        pp1.setValue("10001063");
        demo.parameters.add(pp1);

        ParameterProvider pp2 = new Parameter();
        pp2.setName("MATERIAL_ID2");
        pp2.setValue("");
        demo.parameters.add(pp2);

        ParameterProvider pp3 = new Parameter();
        pp3.setName("WERKS1");
        pp3.setValue("2000");
        demo.parameters.add(pp3);

        ParameterProvider pp4 = new Parameter();
        pp4.setName("WERKS2");
        pp4.setValue("");
        demo.parameters.add(pp4);

        ParameterProvider pp6 = new Parameter();
        pp6.setName("INSP_DATE1");
        pp6.setValue("20170513");
        demo.parameters.add(pp6);

        ParameterProvider pp5 = new Parameter();
        pp5.setName("INSP_DATE2");
        pp5.setValue("");
        demo.parameters.add(pp5);



        int count = demo.getColumnCount();
        for (int i=0;i<count;i++){
            System.out.println(demo.getColumnName(i));
        }
        int rowCount = demo.getRowCount();
        for (int j=0;j<rowCount;j++){
            Object obj = demo.valueList.get(j);
            for (int k=0;k<count;k++){
                Object obj2 = demo.getValueAt(j,k);
                System.out.println(obj2.toString());;
            }
        }
        //okRequest("xx");
    }


}