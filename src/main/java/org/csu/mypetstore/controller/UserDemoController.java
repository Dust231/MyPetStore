package org.csu.mypetstore.controller;

import org.csu.mypetstore.dto.UserReadResult;
import org.csu.mypetstore.service.UserService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 用户查询演示接口，仅在 dev 环境生效；不属于登录流程。
 */
@Controller
@Profile("dev")
public class UserDemoController {

    private final UserService userService;

    /** 构造器注入，单构造器无需 @Autowired */
    public UserDemoController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 按登录名查询用户展示信息。
     * 存在返回 200 + JSON，不存在返回 404；数据访问异常不在此处捕获，交由容器按 500 处理。
     */
    @GetMapping("/demo/user")
    @ResponseBody
    public ResponseEntity<UserReadResult> findUser(
            @RequestParam(defaultValue = "alice") String username) {
        return userService.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
