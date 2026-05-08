package com.zhidian.controller.admin;

import com.zhidian.constant.JwtClaimsConstant;
import com.zhidian.dto.EmployeeDTO;
import com.zhidian.dto.EmployeeLoginDTO;
import com.zhidian.dto.EmployeePageQueryDTO;
import com.zhidian.entity.Employee;
import com.zhidian.properties.JwtProperties;
import com.zhidian.result.PageResult;
import com.zhidian.result.Result;
import com.zhidian.service.EmployeeService;
import com.zhidian.utils.JwtUtil;
import com.zhidian.vo.EmployeeLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Tag(name = "Admin:员工相关接口")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    @Operation(summary = "员工登录")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success();
    }

    @PostMapping
    @Operation(summary = "新增员工")
    public Result save(@RequestBody EmployeeDTO employeeDTO) throws SQLIntegrityConstraintViolationException {
        return employeeService.save(employeeDTO);
    }


    @Operation(summary = "分页查询员工")
    @GetMapping("/page")
    public Result<PageResult> page(EmployeePageQueryDTO pageQueryDTO) {
        PageResult pageResult = employeeService.page(pageQueryDTO);
        return Result.success(pageResult
        );
    }

    @PostMapping("/status/{status}")
    @Operation(summary = "启用或禁用员工账号")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("启用/禁用的账号是:{},{}", status, id);
        employeeService.startOrStop(status, id);
        return Result.success();
    }

    @GetMapping("{id}")
    @Operation(summary = "根据员工ID查询员工")
    public Result<Employee> getById(@PathVariable Long id) {
        Employee employee = employeeService.getById(id);
        return Result.success(employee);
    }
    @PutMapping
    @Operation(summary = "编辑员工信息")
    public Result update(@RequestBody EmployeeDTO employeeDTO){
         employeeService.update(employeeDTO);
         return Result.success();
    }
}
