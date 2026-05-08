package com.zhidian.service;

import com.zhidian.dto.EmployeeDTO;
import com.zhidian.dto.EmployeeLoginDTO;
import com.zhidian.dto.EmployeePageQueryDTO;
import com.zhidian.entity.Employee;
import com.zhidian.result.PageResult;
import com.zhidian.result.Result;

import java.sql.SQLIntegrityConstraintViolationException;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);


    Result save(EmployeeDTO employeeDTO) throws SQLIntegrityConstraintViolationException;


    PageResult page(EmployeePageQueryDTO pageQueryDTO);

    void startOrStop(Integer status, Long id);

    Employee getById(Long id);

    void update(EmployeeDTO employeeDTO);
}
