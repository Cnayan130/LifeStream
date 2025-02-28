package top.principlecreativity.lifestream.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import top.principlecreativity.lifestream.entity.Log;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {

    /**
     * 查询用户的活动日志
     * @param userId 用户ID
     * @param limit 限制返回数量
     * @return 日志列表
     */
    @Query(value = "SELECT l FROM Log l WHERE l.userId = :userId ORDER BY l.timestamp DESC")
    List<Log> findByUserIdOrderByTimestampDesc(@Param("userId") Long userId, int limit);

    /**
     * 查询特定类型和状态的日志
     * @param type 日志类型
     * @param successful 是否成功
     * @param limit 限制返回数量
     * @return 日志列表
     */
    @Query(value = "SELECT l FROM Log l WHERE l.type = :type AND l.successful = :successful ORDER BY l.timestamp DESC")
    List<Log> findByTypeAndSuccessfulOrderByTimestampDesc(
            @Param("type") String type,
            @Param("successful") boolean successful,
            int limit);

    /**
     * 根据时间范围查询日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志列表
     */
    List<Log> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询最近的系统错误
     * @param pageable 分页信息
     * @return 错误日志列表
     */
    List<Log> findByTypeAndSuccessfulFalseOrderByTimestampDesc(String type, Pageable pageable);

    /**
     * 查询内容相关的日志
     * @param contentType 内容类型前缀
     * @param contentId 内容ID
     * @return 日志列表
     */
    @Query("SELECT l FROM Log l WHERE l.type LIKE CONCAT(:contentType, '_%') AND l.contentId = :contentId ORDER BY l.timestamp DESC")
    List<Log> findContentLogs(@Param("contentType") String contentType, @Param("contentId") Long contentId);
}