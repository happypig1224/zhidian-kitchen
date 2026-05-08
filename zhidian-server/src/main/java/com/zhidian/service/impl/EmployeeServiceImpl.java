package com.zhidian.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zhidian.constant.MessageConstant;
import com.zhidian.constant.PasswordConstant;
import com.zhidian.constant.StatusConstant;
import com.zhidian.context.BaseContext;
import com.zhidian.dto.EmployeeDTO;
import com.zhidian.dto.EmployeeLoginDTO;
import com.zhidian.dto.EmployeePageQueryDTO;
import com.zhidian.entity.Employee;
import com.zhidian.exception.AccountNotFoundException;
import com.zhidian.exception.PasswordErrorException;
import com.zhidian.mapper.EmployeeMapper;
import com.zhidian.result.PageResult;
import com.zhidian.result.Result;
import com.zhidian.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();
        //1.根据用户名查询数据库
        Employee employee = employeeMapper.getByUsername(username);
        if(employee == null){
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        password=DigestUtils.md5DigestAsHex(password.getBytes());
        System.out.println("password = " + password);
        System.out.println("employee.getPassword() = " + employee.getPassword());
        System.out.println(password.equals(employee.getPassword()));
        if (!password.equals(employee.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        return employee;
    }

    /**
     * 新增员工
     * @param employeeDTO
     */
    public Result save(EmployeeDTO employeeDTO) throws SQLIntegrityConstraintViolationException {

        Employee employee=new Employee();
        BeanUtils.copyProperties(employeeDTO,employee);
        employee.setStatus(StatusConstant.ENABLE);
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        //employee.setCreateTime(LocalDateTime.now());
        //employee.setUpdateTime(LocalDateTime.now());
        // TODO 后续修改userid
        //employee.setCreateUser(BaseContext.getCurrentId());
        //employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.save(employee);
        return Result.success();
    }

    @Override
    public PageResult page(EmployeePageQueryDTO pageQueryDTO) {
        PageHelper.startPage(pageQueryDTO.getPage(),pageQueryDTO.getPageSize());
        Page<Employee> page=employeeMapper.pageQuery(pageQueryDTO);
        long total = page.getTotal();
        List<Employee> result = page.getResult();
        return new PageResult(total,result);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
       Employee employee=Employee.builder()
                       .id(id).status(status).build();
        employeeMapper.update(employee);
    }

    @Override
    public Employee getById(Long id) {
        Employee employee=new Employee();
        employee.setId(id);
        employee.setPassword("********");
        return employeeMapper.getById(employee);
    }

    @Override
    public void update(EmployeeDTO employeeDTO) {
       Employee employee=new Employee();

       BeanUtils.copyProperties(employeeDTO,employee);
        System.out.println("employee"+employee);
        //使用了公共字段填充
       employee.setUpdateTime(LocalDateTime.now());
       employee.setUpdateUser(BaseContext.getCurrentId());
       employeeMapper.update(employee);

    }


}
