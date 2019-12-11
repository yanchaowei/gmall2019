package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.JwtUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    private UserService userService;

    @RequestMapping(path = "vertify")
    @ResponseBody
    public String vertify(String token, String currentIp) {

        //通过jwt校验token真假
        Map<String, String> map = new HashMap<>();

        Map<String, Object> decode = JwtUtil.decode(token, "2019gmall1932218", currentIp);

        if (decode != null) {
            map.put("status", "success");
            map.put("memberId", (String) decode.get("memberId"));
            map.put("nickname", (String) decode.get("nickname"));
        }else {
            map.put("status", "fail");
        }

        return JSON.toJSONString(map);
    }

    /**
     * 1、从search页面直接点击登陆（带上参数returnUrl），到达登陆页面
     * 2、在登陆页面填入用户信息，提交
     * 3、认证中心验证用户信息是否有效，有效则颁发一个token，
     * 4、根据returnUrl返回登陆页面，并且携带认证中心颁发的token
     * @param modelMap
     * @return
     */
    @RequestMapping(path = "login")
    @ResponseBody
    public String login(UmsMember umsMember, ModelMap modelMap) {

        UmsMember loginMember = userService.login(umsMember);

        if (loginMember == null) {
            //缓存和数据库中均查不到该用户
            //用户名或密码输入错误
            return "用户名或密码输入错误";
        }else {
            //通过jwt获取token

        }


        return "token";
    }

    @RequestMapping(path = "index")
    @LoginRequired(loginSuccess = true)
    public String index(String returnUrl, ModelMap modelMap) {

        //将返回地址写入请求
        modelMap.put("returnUrl", returnUrl);

        return "index";
    }
}
