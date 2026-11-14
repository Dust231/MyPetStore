package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.csu.mypetstore.dto.UserReadResult;
import org.csu.mypetstore.entity.User;
import org.csu.mypetstore.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户查询服务：只做数据读取与展示字段裁剪，不涉及 HTTP 与登录判定。
 */
@Service
public class UserService {

    private final UserMapper userMapper;

    /** 构造器注入，单构造器无需 @Autowired */
    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 按登录名查询用户展示信息。
     * username 列唯一，故最多返回一条；查不到返回空 Optional，DAO 异常原样抛出。
     */
    public Optional<UserReadResult> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                // 只取展示字段，SQL 中不出现 password_hash
                .select(User::getId, User::getUsername, User::getDisplayName,
                        User::getRole, User::getStatus)
                .eq(User::getUsername, username);

        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return Optional.empty();
        }
        return Optional.of(new UserReadResult(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus()));
    }
}
