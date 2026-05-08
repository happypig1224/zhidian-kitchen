package com.zhidian.mapper;

import com.github.pagehelper.Page;
import com.zhidian.annotation.AutoFill;
import com.zhidian.dto.EmployeePageQueryDTO;
import com.zhidian.entity.Employee;
import com.zhidian.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);
    @Insert("insert into employee(name,username,password,phone,sex,id_number,status,create_time,update_time,create_user,update_user)" +
    "values(#{name},#{username},#{password},#{phone},#{sex},#{idNumber},#{status},#{createTime},#{updateTime},#{createUser},#{updateUser})")
    @AutoFill(OperationType.INSERT)
    void save(Employee employee);

    Page<Employee> pageQuery(EmployeePageQueryDTO pageQueryDTO);
    @AutoFill(OperationType.UPDATE)
    void update(Employee employee);

    @Select("select * from employee where id=#{id}")
    Employee getById(Employee employee);
}
