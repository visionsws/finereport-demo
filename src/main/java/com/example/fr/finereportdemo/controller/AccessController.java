package com.example.fr.finereportdemo.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fr.stable.collections.CollectionUtils;
import com.fr.third.jodd.util.StringUtil;
import com.fr.third.springframework.web.bind.annotation.RequestBody;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class AccessController {

    /**
     * 查询分页
     */
    @PostMapping("/queryFromFR")
    public String queryFromFR(@RequestBody String paramJsonStr) {
        JSONObject jsonObject = JSONObject.parseObject(paramJsonStr);
        //logger.debug(paramJsonStr);

        String indexName = jsonObject.getString("indexName");
        String parameter = "{\n" +
                "    \"allColumns\": [\n" +
                "        \"name\",\n" +
                "        \"id\",\n" +
                "        \"userId\"\n" +
                "    ],\n" +
                "    \"dateFieldnames\": [],\n" +
                "    \"index\": \"test_secondary_index\",\n" +
                "    \"keywords\": [\n" +
                "        \"原理\"\n" +
                "    ],\n" +
                "        \"findFieldNames\": [\"description\"],\n" +
                "    \"types\": [],\n" +
                "    \"pagenum\":1,\n" +
                "    \"pagesize\":2\n" +
                "}";
        JSONObject jsonObject1 = JSONObject.parseObject(parameter);
        jsonObject1.put("index",indexName);
        ArrayList list = new ArrayList<String>();
        list.add(jsonObject.getString("name"));
        jsonObject1.put("keywords", list);
        return queryPage(jsonObject1.toJSONString());
    }

    /**
     * 查询分页
     */
    @PostMapping("/queryPage")
    public String queryPage(@RequestBody String paramJsonStr) {
        JSONObject jsonObject = JSONObject.parseObject(paramJsonStr);
        List<String> keywords = JSONArray.parseArray(jsonObject.getString("keywords"), String.class);
        String index = jsonObject.getString("index");
        List<String> types = JSONArray.parseArray(jsonObject.getString("types"), String.class);
        List<String> findFieldNames = JSONArray.parseArray(jsonObject.getString("findFieldNames"), String.class);
        List<String> dateFieldNames = JSONArray.parseArray(jsonObject.getString("dateFieldNames"), String.class);
        List<String> allColumns = JSONArray.parseArray(jsonObject.getString("allColumns"), String.class);
        Long startTime = jsonObject.getLong("startTime");
        Long endTime = jsonObject.getLong("endTime");
        Integer pagenum = jsonObject.getInteger("pagenum");
        Integer pagesize = jsonObject.getInteger("pagesize");


        if (StringUtil.isEmpty(index)
                || CollectionUtils.isEmpty(keywords)
                || CollectionUtils.isEmpty(allColumns)
                || CollectionUtils.isEmpty(findFieldNames)
                || pagenum == null
                || pagesize == null) {
            return "参数错误, 请检查 index、keywords、allColumns、findFieldNames、pagenum、pagesize";
        }

        try {
//            jsonObjectPageEntity = bookService.queryWithTerm(keywords, indexs, types, findFieldNames, dateFieldNames, allColumns, startTime, endTime, pagenum, pagesize);
            String s = "";
            //s = bookService.queryAllData(keywords, index, types, findFieldNames, allColumns, pagenum, pagesize);
            System.out.println(s);
            return s;
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }

    }
}
