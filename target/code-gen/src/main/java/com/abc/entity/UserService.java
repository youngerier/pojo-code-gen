package com.abc.entity;

import java.util.List;

/**
 * 用户实体
 * 服务接口
 */
public interface UserService {
    /**
     * 创建User
     * @param userDTO User数据传输对象
     * @return 创建的User对象
     */
    UserDTO createUser(UserDTO userDTO);

    /**
     * 根据ID查询User
     * @param id 主键ID
     * @return 对应的User对象
     */
    UserDTO getUserById(long id);

    /**
     * 查询所有User
     * @return User对象列表
     */
    List<UserDTO> getAllUsers();

    /**
     * 更新User
     * @param id 主键ID
     * @param userDTO User数据传输对象
     * @return 更新后的User对象
     */
    UserDTO updateUser(long id, UserDTO userDTO);

    /**
     * 删除User
     * @param id 主键ID
     * @return 是否删除成功
     */
    boolean deleteUser(long id);
}
