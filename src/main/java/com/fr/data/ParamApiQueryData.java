package com.fr.data;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fr.log.FineLoggerFactory;
import com.fr.stable.ParameterProvider;
import com.fr.third.org.apache.http.HttpEntity;
import com.fr.third.org.apache.http.client.ClientProtocolException;
import com.fr.third.org.apache.http.client.methods.CloseableHttpResponse;
import com.fr.third.org.apache.http.client.methods.HttpPost;
import com.fr.third.org.apache.http.entity.StringEntity;
import com.fr.third.org.apache.http.impl.client.CloseableHttpClient;
import com.fr.third.org.apache.http.impl.client.HttpClients;
import com.fr.third.org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;

public class ParamApiQueryData extends AbstractTableData {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    /**
     * 列名数组，保存程序数据集所有列名
     */
    private String[] columnNames;
    /**
     * 定义程序数据集的列数量
     */
    private int columnNum = 10;
    /**
     * 保存查询表的实际列数量
     */
    private int colNum = 0;
    /**
     * 保存查询得到列值
     */
    private ArrayList valueList = null;

    public ParamApiQueryData() {
        columnNames = new String[columnNum];
        for (int i = 0; i < columnNum; i++) {
            columnNames[i] = "column#" + String.valueOf(i);
        }
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    //获取列的名称为数组中第一行的值
    @Override
    public String getColumnName(int columnIndex)  {
        return columnNames[columnIndex];
    }

    //获取行数为数据的长度-1
    @Override
    public int getRowCount() {
        this.getApiQueryData();
        return valueList.size();
    }

    //获取值
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        this.getApiQueryData();
        if (columnIndex >= colNum) {
            return null;
        }
        return ((Object[]) valueList.get(rowIndex))[columnIndex];
    }

    public void getApiQueryData() {
        // 确保只被执行一次
        if (valueList != null) {
            return;
        }
        valueList = new ArrayList();
        try {
            String url = "http://dompapi.bluemoon.com.cn/bd-domp-service/domp/getData";
            //String url = "http://dompapi.bluemoon.com.cn/ubu-report-admin-service/reportGroup/getAllReportGroup";
            JSONObject paramMap = new JSONObject();
            for (int k =0 ;k<parameters.get().toArray().length; k++){
                String paramKey = ((ParameterProvider) (parameters.get().toArray())[k]).getName();
                String paramValue = ((ParameterProvider) (parameters.get().toArray())[k]).getValue().toString();
                paramMap.put(paramKey,paramValue);
                FineLoggerFactory.getLogger().info("程序数据集参数,"+paramKey+":"+paramValue);
            }
            JSONObject json =doPostJson(url,paramMap);
            JSONArray jsonArray = json.getJSONArray("respData");
            if (jsonArray != null && !jsonArray.isEmpty()){
                JSONObject data1 = jsonArray.getJSONObject(0);
                Iterator<String> keys = data1.keySet().iterator();
                columnNames = new String[data1.size()];
                int columnSort = 0;
                while(keys.hasNext()){
                    String key = keys.next();
                    columnNames[columnSort] = key;
                    columnSort++;
                }
                colNum = columnSort;
                FineLoggerFactory.getLogger().info("程序数据集列,"+columnNames);

                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonData = jsonArray.getJSONObject(i);
                    String column[] = new String[columnSort];
                    for (int j = 0; j < columnSort; j++){
                        String columnValue = jsonData.getString(columnNames[j]);
                        column[j] = columnValue;
                    }
                    valueList.add(column);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }

    /**
     * json参数方式POST提交
     * @param url
     * @param params
     * @return
     */
    public static JSONObject doPostJson(String url, JSONObject params){
        JSONObject jsonResult = null;
        // 1. 获取默认的client实例
        CloseableHttpClient client = HttpClients.createDefault();
        // 2. 创建httppost实例
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-Type", "application/json;charset=utf-8"); //添加请求头
        try {
            httpPost.setEntity(new StringEntity(params.toJSONString(),"utf-8"));
            CloseableHttpResponse resp = client.execute(httpPost);
            try {
                // 7. 获取响应entity
                HttpEntity respEntity = resp.getEntity();
                String strResult = EntityUtils.toString(respEntity, "UTF-8");
                // 把json字符串转换成json对象
                jsonResult = JSONObject.parseObject(strResult);
            } finally {
                resp.close();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return jsonResult;
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

    public static void main(String[] args) {
        new ParamApiQueryData().getApiQueryData();
        for (int i = 0; i < new ParamApiQueryData().getRowCount(); i++) {
            for (int j = 0; j < new ParamApiQueryData().getColumnCount(); j++) {
                System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaa");
                System.out.println(new ParamApiQueryData().getValueAt(i,j));
            }
        }
    }
}