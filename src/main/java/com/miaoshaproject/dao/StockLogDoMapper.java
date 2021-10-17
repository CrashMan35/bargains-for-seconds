package com.miaoshaproject.dao;

import com.miaoshaproject.dataobject.StockLogDo;
import org.springframework.stereotype.Repository;

@Repository
public interface StockLogDoMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Thu Apr 23 22:53:12 CST 2020
     */
    int deleteByPrimaryKey(String stockLogId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Thu Apr 23 22:53:12 CST 2020
     */
    int insert(StockLogDo record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Thu Apr 23 22:53:12 CST 2020
     */
    int insertSelective(StockLogDo record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Thu Apr 23 22:53:12 CST 2020
     */
    StockLogDo selectByPrimaryKey(String stockLogId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Thu Apr 23 22:53:12 CST 2020
     */
    int updateByPrimaryKeySelective(StockLogDo record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Thu Apr 23 22:53:12 CST 2020
     */
    int updateByPrimaryKey(StockLogDo record);
}