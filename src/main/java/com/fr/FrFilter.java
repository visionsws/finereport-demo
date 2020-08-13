package com.fr;

import com.alibaba.fastjson.JSONObject;
import com.fr.data.NetworkHelper;
import com.fr.decision.mobile.terminal.TerminalHandler;
import com.fr.decision.webservice.utils.DecisionServiceConstants;
import com.fr.decision.webservice.v10.login.LoginService;
import com.fr.decision.webservice.v10.login.TokenResource;
import com.fr.log.FineLoggerFactory;
import com.fr.security.JwtUtils;
import com.fr.stable.StringUtils;
import com.fr.stable.web.Device;
import com.fr.third.org.apache.http.HttpEntity;
import com.fr.third.org.apache.http.client.ClientProtocolException;
import com.fr.third.org.apache.http.client.methods.CloseableHttpResponse;
import com.fr.third.org.apache.http.client.methods.HttpPost;
import com.fr.third.org.apache.http.entity.StringEntity;
import com.fr.third.org.apache.http.impl.client.CloseableHttpClient;
import com.fr.third.org.apache.http.impl.client.HttpClients;
import com.fr.third.org.apache.http.util.EntityUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by Zed on 2018/9/11.
 */
public class FrFilter implements Filter {

    public FrFilter() {

    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;
        HttpSession session = req.getSession(true);
        FineLoggerFactory.getLogger().info("fr filter URL:" + req.getRequestURI());

        //含有viewLet的才是报表，没有的可能是一些js文件
        String viewLet = req.getParameter("viewlet");
        if(viewLet == null){
            filterChain.doFilter(req, res);
        }else {
            String fineAuthToken = TokenResource.COOKIE.getToken(req);
            String token  = req.getParameter("token");
            try{
                FineLoggerFactory.getLogger().info("fineAuthToken:"+ fineAuthToken);
                FineLoggerFactory.getLogger().info("token:"+ token);
                if (StringUtils.isEmpty(token)){
                    token = (String)session.getAttribute("moonToken");
                }
                //token不存在，或者token过期了，走后台登录方法
                if (!StringUtils.isEmpty(token)) {
                    JSONObject json = loginByToken(token);
                    String respCode = json.getString("respCode");
                    if ("100".equals(respCode)){
                        String userId = json.getString("userId");
                        FineLoggerFactory.getLogger().info("userId:" + userId);
                        login(req, res, session, userId);
                        session.setAttribute("moonUserId",userId);
                        session.setAttribute("moonToken",token);
                        filterChain.doFilter(req, res);
                    }else {
                        if(StringUtils.isEmpty(fineAuthToken)){
                            res.sendRedirect("/webroot/decision/login");
                        }else{
                            filterChain.doFilter(req, res);
                        }
                    }
                }else {
                    if (fineAuthToken != null && checkTokenValid(req,fineAuthToken)) {
                        //放行
                        filterChain.doFilter(req, res);
                    }else {
                        filterChain.doFilter(req, res);
                    }
                }
            } catch (Exception e) {
                FineLoggerFactory.getLogger().error(e.getMessage(), e);
            }
        }
    }

    /**
     * 通过蓝书通token来认证登陆信息
     */
    private JSONObject loginByToken(String token){
        //获取链接中传递过来的token
        FineLoggerFactory.getLogger().info("token:" + token);
        JSONObject paramMap = new JSONObject();
        paramMap.put("token",token);
        JSONObject json =doPostJson("http://domp.bluemoon.com.cn/bd-domp-service/user/getUserIdByToken",paramMap);
        FineLoggerFactory.getLogger().info("tokenUrl login result:"+json.toJSONString());
        return json;
    }

    /**
     * 后台登录方法
     */
    private void login(HttpServletRequest req, HttpServletResponse res, HttpSession session, String username) throws Exception {
        String token = LoginService.getInstance().login(req, res, username);
        session.setAttribute(DecisionServiceConstants.FINE_AUTH_TOKEN_NAME, token);
        req.setAttribute(DecisionServiceConstants.FINE_AUTH_TOKEN_NAME, token);
        FineLoggerFactory.getLogger().info("fr FrFilter is over with username is ###" + username);
    }

    /**
     * 校验token是否有效
     */
    private boolean checkTokenValid(HttpServletRequest req, String token) {
        try {
            String currentUserName = JwtUtils.parseJWT(token).getSubject();
            if (StringUtils.isEmpty(currentUserName)) {
                FineLoggerFactory.getLogger().info("token不正确：" + token);
                return false;
            }
            Device device = NetworkHelper.getDevice(req);
            LoginService.getInstance().loginStatusValid(token, TerminalHandler.getTerminal(req, device));
            FineLoggerFactory.getLogger().info("校验token成功,userName:" + currentUserName);
            return true;
        } catch (Exception ignore) {
        }

        return false;
    }

    @Override
    public void destroy() {

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


}
