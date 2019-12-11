package com.atguigu.gmall.user.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class UserController {

    @Reference
    private UserService userService;

//    @RequestMapping(path = "/add")
//    @ResponseBody
//    public Result add(UmsMember umsMember){
//        try {
//            userService.add(umsMember);
//            return new Result(true, "添加成功");
//        }catch (Exception e){
//            e.printStackTrace();
//            return new Result(false, "添加失败");
//        }
//    }

    @RequestMapping(path = "/getUserById")
    @ResponseBody
    public UmsMember getUserById(String id){
        UmsMember umsMember = userService.getUserById(id);
        return umsMember;
    }

    @RequestMapping(path = "/getReceiveAddressByMemberId")
    @ResponseBody
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId){
        List<UmsMemberReceiveAddress> umsMemberReceiveAddressList = userService.getReceiveAddressByMemberId(memberId);
        return umsMemberReceiveAddressList;
    }

    @RequestMapping(path = "/getAllUser")
    @ResponseBody
    public List<UmsMember> getAllUser(){
        List<UmsMember> umsMembers = userService.getAllUser();
        return umsMembers;
    }

    @RequestMapping(path = "/index")
    @ResponseBody
    public String index(){
        return "hello, user";
    }
}
