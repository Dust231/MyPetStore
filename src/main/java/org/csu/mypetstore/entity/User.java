package org.csu.mypetstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 教学账号：对应 store_user 表。
 * 列名下划线转驼峰由 map-underscore-to-camel-case 自动完成，无需 @TableField。
 */
@Getter
@Setter
@TableName("store_user")
public class User {

    /** id BIGINT AUTO_INCREMENT，数据库自增，插入时不赋值 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** username VARCHAR(50)，唯一，创建后不修改 */
    private String username;

    /** password_hash VARCHAR(255)，BCrypt 散列，不保存明文 */
    private String passwordHash;

    /** display_name VARCHAR(50) */
    private String displayName;

    /** role VARCHAR(16)：CUSTOMER 或 ADMIN */
    private String role;

    /** status VARCHAR(16)：ENABLED 或 DISABLED，默认 ENABLED */
    private String status;

    /** email VARCHAR(100)，可空 */
    private String email;

    /** phone VARCHAR(30)，可空 */
    private String phone;

    /** created_at DATETIME，默认 CURRENT_TIMESTAMP */
    private LocalDateTime createdAt;

    /** updated_at DATETIME，ON UPDATE CURRENT_TIMESTAMP */
    private LocalDateTime updatedAt;
}
