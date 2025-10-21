package com.yupi.example.common.service;

import com.yupi.example.common.model.User;

public interface UserService {

    /**
     * 获取用户信息
     *
     * @param user
     * @return 用户信息
     */
    User getUser(User user);


    /**
     * 新方法-获取数字
     */
    default short getNumber(){
        return 1;
    }

}
