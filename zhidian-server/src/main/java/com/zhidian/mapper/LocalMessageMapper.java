package com.zhidian.mapper;

import com.zhidian.entity.LocalMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LocalMessageMapper {
    
    /**
     * 插入消息
     */
    @Insert("INSERT INTO local_message (business_id, message_type, content, status, retry_count, max_retry_count, next_execute_time, create_time, update_time) " +
            "VALUES (#{businessId}, #{messageType}, #{content}, #{status}, #{retryCount}, #{maxRetryCount}, #{nextExecuteTime}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(LocalMessage localMessage);
    
    /**
     * 查询待处理的消息
     */
    @Select("SELECT * FROM local_message WHERE status = 'PENDING' AND next_execute_time <= NOW() ORDER BY create_time ASC LIMIT 100")
    List<LocalMessage> selectPendingMessages();
    
    /**
     * 更新消息状态
     */
    @Update("UPDATE local_message SET status = #{status}, retry_count = #{retryCount}, next_execute_time = #{nextExecuteTime}, " +
            "update_time = #{updateTime}, process_time = #{processTime}, error_message = #{errorMessage} WHERE id = #{id}")
    void updateStatus(LocalMessage localMessage);
    
    /**
     * 根据业务ID和消息类型查询消息
     */
    @Select("SELECT * FROM local_message WHERE business_id = #{businessId} AND message_type = #{messageType} ORDER BY create_time DESC LIMIT 1")
    LocalMessage selectByBusinessIdAndType(@Param("businessId") Long businessId, @Param("messageType") String messageType);
}