package com.csdn.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * @author ：xwf
 * @date ：Created in 2020-7-30 11:12
 */
public interface WebMapper {

    @Insert("insert into test(id,name) value (#{id,jdbcType=VARCHAR},#{name,jdbcType=VARCHAR})")
    void add(@Param("id") String id, @Param("name") String name);
}
