package org.csu.mypetstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.csu.mypetstore.entity.User;

/**
 * store_user 表的数据访问接口。
 * 继承 BaseMapper 即获得单表增删改查能力，无需编写 XML。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
