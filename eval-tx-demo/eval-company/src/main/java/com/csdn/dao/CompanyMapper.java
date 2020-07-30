package com.csdn.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * @author ：xwf
 * @date ：Created in 2020-7-30 11:12
 */
public interface CompanyMapper {

    @Insert("insert into company(id,name) value (#{id,jdbcType=VARCHAR},#{name,jdbcType=VARCHAR})")
    void addCompany(@Param("id") String id, @Param("name") String name);
}
