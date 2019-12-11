package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {
    List<UmsMember> getAllUser();

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);

    UmsMember getUserById(String id);

    void add(UmsMember umsMember);

    UmsMember login(UmsMember umsMember);

    List<UmsMemberReceiveAddress> getUserAddressList(String memberId);

    UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId);
}
