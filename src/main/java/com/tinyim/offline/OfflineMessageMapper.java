package com.tinyim.offline;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinyim.entity.OfflineMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 离线消息 Mapper
 * 基于 MyBatis Plus 实现离线消息的增删改查
 */
@Mapper
public interface OfflineMessageMapper extends BaseMapper<OfflineMessage> {

    /**
     * 查询指定用户的未推送离线消息
     *
     * @param userId 用户ID
     * @return 未推送的离线消息列表，按序列号升序排列
     */
    @Select("SELECT * FROM offline_message WHERE user_id = #{userId} AND pushed = 0 ORDER BY sequence ASC")
    List<OfflineMessage> selectUnpushedByUserId(@Param("userId") Long userId);

    /**
     * 更新离线消息的推送状态
     *
     * @param id       离线消息ID
     * @param pushed   推送状态：0-未推送 1-已推送
     * @param pushTime 推送时间
     * @return 影响行数
     */
    @Update("UPDATE offline_message SET pushed = #{pushed}, push_time = #{pushTime} WHERE id = #{id}")
    int updatePushedStatus(@Param("id") Long id, @Param("pushed") Integer pushed, @Param("pushTime") LocalDateTime pushTime);

    /**
     * 批量更新推送状态
     *
     * @param ids      离线消息ID列表
     * @param pushed   推送状态
     * @param pushTime 推送时间
     * @return 影响行数
     */
    int batchUpdatePushedStatus(@Param("ids") List<Long> ids, @Param("pushed") Integer pushed, @Param("pushTime") LocalDateTime pushTime);

    /**
     * 删除指定时间之前已推送的离线消息
     *
     * @param beforeTime 截止时间
     * @return 删除行数
     */
    @Update("DELETE FROM offline_message WHERE pushed = 1 AND push_time < #{beforeTime}")
    int deletePushedBefore(@Param("beforeTime") LocalDateTime beforeTime);
}
