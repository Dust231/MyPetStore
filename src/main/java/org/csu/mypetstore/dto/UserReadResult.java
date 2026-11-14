package org.csu.mypetstore.dto;

/**
 * 用户展示信息，只承载页面可见字段，不含密码散列等敏感数据。
 */
public record UserReadResult(
        Long id,
        String username,
        String displayName,
        String role,
        String status
) {
}
