package com.zhidian.mapper;


import com.zhidian.entity.ChatMemory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

/**
* @author 33046
* @description 针对表【chat_memory(聊天记忆表)】的数据库操作 Mapper
* @createDate 2026-03-29 15:29:09
* @Entity com.zhidian.entity.ChatMemory
*/
@Mapper
public interface ChatMemoryMapper {

    @Insert("insert into chat_memory(session_id,message_type,content,create_time,update_time,is_deleted) values(#{sessionId},#{messageType},#{content},#{createTime},#{updateTime},#{isDeleted})")
    void insert(ChatMemory chatMemory);

    @Select("select * from chat_memory where session_id = #{conversationId}")
    List<ChatMemory> selectBySessionId(String conversationId);

    @Delete("delete from chat_memory where session_id = #{conversationId}")
    void deleteBySessionId(String conversationId);
    
    @Select("select distinct session_id from chat_memory where is_deleted = 0")
    Set<String> selectAllSessionIds();

}




