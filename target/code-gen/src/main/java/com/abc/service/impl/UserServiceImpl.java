package com.abc.service.impl;

import com.abc.entity.User;
import com.abc.repository.UserRepository;
import com.abc.service.UserService;
import java.lang.Override;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 用户实体
 * 服务实现类
 */
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(User user) {
        userRepository.insert(user);
        return user;
    }

    @Override
    public User getUserById(long id) {
        return userRepository.selectOneById(id);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.selectAll();
    }

    @Override
    public User updateUser(User user) {
        userRepository.update(user);
        return user;
    }

    @Override
    public boolean deleteUser(long id) {
        return userRepository.deleteById(id) > 0;
    }
}
