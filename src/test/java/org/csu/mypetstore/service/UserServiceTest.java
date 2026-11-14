package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.csu.mypetstore.dto.UserReadResult;
import org.csu.mypetstore.entity.User;
import org.csu.mypetstore.mapper.UserMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserService 单元测试：Mapper 用 Mockito 打桩，Service 为真实实例。
 * 不加载 Spring 容器、不启动 Web 服务、不连接数据库。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    /**
     * LambdaQueryWrapper 构建条件时要读实体元数据（列名、主键），
     * 脱离 Spring 时这份缓存没有被 Mapper 注册流程填充，
     * 因此只为 User 手工初始化一次 TableInfo，不启动容器。
     */
    @BeforeAll
    static void initUserTableInfoOnce() {
        if (TableInfoHelper.getTableInfo(User.class) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            assistant.setCurrentNamespace(UserMapper.class.getName());
            TableInfoHelper.initTableInfo(assistant, User.class);
        }
    }

    @Test
    void returnsMappedUserReadResultWhenUserFound() {
        UserService userService = new UserService(userMapper);
        when(userMapper.selectOne(any())).thenReturn(user(1L, "alice", "林同学", "CUSTOMER", "ENABLED"));

        Optional<UserReadResult> result = userService.findByUsername("alice");

        assertThat(result).isPresent();
        UserReadResult userReadResult = result.get();
        assertThat(userReadResult.id()).isEqualTo(1L);
        assertThat(userReadResult.username()).isEqualTo("alice");
        assertThat(userReadResult.displayName()).isEqualTo("林同学");
        assertThat(userReadResult.role()).isEqualTo("CUSTOMER");
        assertThat(userReadResult.status()).isEqualTo("ENABLED");
        verify(userMapper).selectOne(any());
    }

    @Test
    void returnsEmptyOptionalWhenUserNotFound() {
        UserService userService = new UserService(userMapper);
        when(userMapper.selectOne(any())).thenReturn(null);

        Optional<UserReadResult> result = userService.findByUsername("not-exists");

        assertThat(result).isEmpty();
        verify(userMapper).selectOne(any());
    }

    @Test
    void propagatesSameDataAccessExceptionFromMapper() {
        UserService userService = new UserService(userMapper);
        DataAccessResourceFailureException failure =
                new DataAccessResourceFailureException("模拟数据库不可用");
        when(userMapper.selectOne(any())).thenThrow(failure);

        assertThatThrownBy(() -> userService.findByUsername("alice"))
                .isSameAs(failure);
        verify(userMapper).selectOne(any());
    }

    private static User user(Long id, String username, String displayName, String role, String status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("$2a$10$not-used-in-read-result");
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setStatus(status);
        user.setEmail(username + "@example.test");
        return user;
    }
}
