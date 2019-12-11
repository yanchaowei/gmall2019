package com.atguigu.gmall.interceptors;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //拦截代码
        //利用反射，根据用户请求的方法的注解判断该方法被访问时是否需要被拦截
        HandlerMethod hm = (HandlerMethod) handler;
        LoginRequired loginRequired = hm.getMethodAnnotation(LoginRequired.class);

        if (loginRequired == null) {
            return true;
        }

        //获取token，进行判断
        String token = "";

        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
        if (StringUtils.isNotBlank(oldToken)) {
            token = oldToken;
        }

        String newToken = request.getParameter("token");
        if (StringUtils.isNotBlank(newToken)) {
            token = newToken;
        }

        //判断需要拦截的方法，当用户为登陆时是否可以访问
        boolean loginSuccess = loginRequired.loginSuccess();
        System.out.println("进入拦截器的拦截方法");

        //请求认证中心对token进行验证，获取用户信息
        String success = "fail";
        Map<String, String> successMap = null;
        if (StringUtils.isNotBlank(token)) {
            String ip = request.getHeader("x-forword-for");// 通过nginx转发的客户端ip
            if (StringUtils.isBlank(ip)) {
                ip = request.getRemoteAddr();// 从request中获取ip
                if (StringUtils.isBlank(ip)) {
                    ip = "127.0.0.1";
                }
            }
            String successJson = HttpclientUtil.doGet("http://passport.gmall.com:8085/vertify?token=" + token + "&currentIp=" + ip);
            successMap = JSON.parseObject(successJson, Map.class);
            success = successMap.get("status");
        }

        if (loginSuccess) {
            //需要验证，并且必须登陆才能访问
            if (!success.equals("success")) {
                //重定向回passport登陆
                StringBuffer requestURL = request.getRequestURL();
                response.sendRedirect("http://passport.gmall.com:8085/index?returnUrl=" + requestURL);
                return false;
            }
            //验证通过，此时需要将cookie覆盖(写在下面）
            //将token携带的用户信息
            request.setAttribute("memberId", successMap.get("memberId"));
            request.setAttribute("nickname", successMap.get("nickname"));
        } else {
            //需要验证，未登陆也能访问
            if (success.equals("success")) {
                //验证通过
                //将token携带的用户信息
                request.setAttribute("memberId", successMap.get("memberId"));
                request.setAttribute("nickname", successMap.get("nickname"));
            }
        }

        //验证通过，此时需要将cookie覆盖
        if (success.equals("success")) {
            CookieUtil.setCookie(request, response, "oldToken", token, 60*60*2, true);
        }

        return true;
    }
}