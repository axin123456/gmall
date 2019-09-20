package com.atguigu.gmall.interceptor;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.constants.WebConst;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static com.atguigu.gmall.constants.WebConst.VERIFY_URL;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = null;
        token = request.getParameter("newToken");
        if (token != null) {
            //把token放到cookie中
            CookieUtil.setCookie(request, response, "token", token, WebConst.cookieMaxAge, false);
        } else {
            //token="";//from cookie
            token = CookieUtil.getCookieValue(request, "token", false);
        }
        //从token中取出用户信息
        Map userMap = new HashMap();
        if (token != null) {
            userMap = getUserMapFromToken(token);
            String nickName = (String) userMap.get("nickName");
            request.setAttribute("nickName", nickName);
        }


        //判断用户是否需要登录
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequire loginRequire = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if (loginRequire != null) {
            if (token != null) {
                //把token发给认证中心认证
                String currentIp = request.getHeader("X-forwarded-for");
                String result = HttpClientUtil.doGet(VERIFY_URL + "?token=" + token + "&currentIp=" + currentIp);
                if ("success".equals(result)) {
                    String userId = (String) userMap.get("userId");
                    request.setAttribute("userId", userId);
                    return true;
                } else if (!loginRequire.autoRedirect()) {
                    return true;
                } else {
                    redirct(request, response);
                    return false;
                }
            } else {
                //重定向 重新登录
                redirct(request, response);
                return false;
            }
        }
        return true;
    }

    private void redirct(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestURL = request.getRequestURL().toString();
        String encodeURL = URLEncoder.encode(requestURL, "UTF-8");
        response.sendRedirect(WebConst.LOGIN_URL + "? =" + encodeURL);
    }

    private Map getUserMapFromToken(String token) {
        String userBase64 = StringUtils.substringBetween(token, ".");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] userBytes = base64UrlCodec.decode(userBase64);
        String userJson = new String(userBytes);
        Map userMap = JSON.parseObject(userJson, Map.class);
        return userMap;
    }
}
