package com.abc.mapper;

import com.abc.dto.UserDTO;
import com.abc.entity.User;
import com.abc.model.request.UserRequest;
import com.abc.model.response.UserResponse;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * 用户实体
 * 对象转换器
 */
@Mapper
public interface UserMapper {
    UserDTO toDto(User user);

    User toEntity(UserDTO userDTO);

    User toEntity(UserRequest userRequest);

    UserResponse toResponse(User user);

    List<UserDTO> toDtoList(List<User> userList);

    List<UserResponse> toResponseList(List<User> userList);
}
