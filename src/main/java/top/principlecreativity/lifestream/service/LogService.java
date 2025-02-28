package top.principlecreativity.lifestream.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.principlecreativity.lifestream.entity.Log;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.repository.LogRepository;
import top.principlecreativity.lifestream.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 系统日志服务
 * 负责记录用户活动和系统事件
 */
@Service
public class LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private HttpServletRequest request;

    /**
     * 记录用户登录事件
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @param successful 是否成功
     */
    @Async
    @Transactional
    public void logUserLogin(Long userId, String ipAddress, String userAgent, boolean successful) {
        try {
            Log log = new Log();
            log.setType("USER_LOGIN");
            log.setUserId(userId);
            log.setIpAddress(ipAddress);
            log.setUserAgent(userAgent);
            log.setTimestamp(LocalDateTime.now());
            log.setDetails(successful ? "登录成功" : "登录失败");
            log.setSuccessful(successful);

            logRepository.save(log);
            logger.info("User login logged: userId={}, successful={}", userId, successful);
        } catch (Exception e) {
            logger.error("Error logging user login", e);
        }
    }

    /**
     * 记录用户注销事件
     * @param userId 用户ID
     */
    @Async
    @Transactional
    public void logUserLogout(Long userId) {
        try {
            String ipAddress = null;
            String userAgent = null;

            if (request != null) {
                ipAddress = getClientIpAddress(request);
                userAgent = request.getHeader("User-Agent");
            }

            Log log = new Log();
            log.setType("USER_LOGOUT");
            log.setUserId(userId);
            log.setIpAddress(ipAddress);
            log.setUserAgent(userAgent);
            log.setTimestamp(LocalDateTime.now());
            log.setDetails("用户注销");
            log.setSuccessful(true);

            logRepository.save(log);
            logger.info("User logout logged: userId={}", userId);
        } catch (Exception e) {
            logger.error("Error logging user logout", e);
        }
    }

    /**
     * 记录用户注册事件
     * @param userId 用户ID
     * @param ipAddress IP地址
     */
    @Async
    @Transactional
    public void logUserRegistration(Long userId, String ipAddress) {
        try {
            Log log = new Log();
            log.setType("USER_REGISTRATION");
            log.setUserId(userId);
            log.setIpAddress(ipAddress);
            log.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
            log.setTimestamp(LocalDateTime.now());
            log.setDetails("新用户注册");
            log.setSuccessful(true);

            logRepository.save(log);
            logger.info("User registration logged: userId={}", userId);
        } catch (Exception e) {
            logger.error("Error logging user registration", e);
        }
    }

    /**
     * 记录内容操作事件
     * @param userId 用户ID
     * @param action 操作类型（如CREATE、UPDATE、DELETE）
     * @param contentType 内容类型（如POST、COMMENT、ALBUM）
     * @param contentId 内容ID
     * @param details 详细信息
     */
    @Async
    @Transactional
    public void logContentAction(Long userId, String action, String contentType, Long contentId, String details) {
        try {
            Log log = new Log();
            log.setType(contentType + "_" + action);
            log.setUserId(userId);

            if (request != null) {
                log.setIpAddress(getClientIpAddress(request));
                log.setUserAgent(request.getHeader("User-Agent"));
            }

            log.setTimestamp(LocalDateTime.now());
            log.setContentId(contentId);
            log.setDetails(details);
            log.setSuccessful(true);

            logRepository.save(log);
            logger.info("Content action logged: userId={}, action={}, contentType={}, contentId={}",
                    userId, action, contentType, contentId);
        } catch (Exception e) {
            logger.error("Error logging content action", e);
        }
    }

    /**
     * 记录系统错误
     * @param source 错误来源
     * @param errorMessage 错误消息
     * @param stackTrace 堆栈跟踪
     */
    @Async
    @Transactional
    public void logSystemError(String source, String errorMessage, String stackTrace) {
        try {
            Log log = new Log();
            log.setType("SYSTEM_ERROR");

            if (request != null) {
                log.setIpAddress(getClientIpAddress(request));
                log.setUserAgent(request.getHeader("User-Agent"));
            }

            log.setTimestamp(LocalDateTime.now());
            log.setDetails(source + ": " + errorMessage);
            log.setStackTrace(stackTrace);
            log.setSuccessful(false);

            logRepository.save(log);
            logger.error("System error logged: source={}, error={}", source, errorMessage);
        } catch (Exception e) {
            logger.error("Error logging system error", e);
        }
    }

    /**
     * 获取用户活动日志
     * @param userId 用户ID
     * @param limit 返回记录数限制
     * @return 日志列表
     */
    @Transactional(readOnly = true)
    public List<Log> getUserActivityLogs(Long userId, int limit) {
        return logRepository.findByUserIdOrderByTimestampDesc(userId, limit);
    }

    /**
     * 获取系统错误日志
     * @param limit 返回记录数限制
     * @return 日志列表
     */
    @Transactional(readOnly = true)
    public List<Log> getSystemErrorLogs(int limit) {
        return logRepository.findByTypeAndSuccessfulOrderByTimestampDesc("SYSTEM_ERROR", false, limit);
    }

    /**
     * 获取客户端IP地址
     * @param request HTTP请求
     * @return IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader != null && !xForwardedForHeader.isEmpty()) {
            return xForwardedForHeader.split(",")[0].trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}