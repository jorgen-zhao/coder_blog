package com.liteiot.gate.utils;

import com.liteiot.api.vo.log.LogInfo;
import com.liteiot.gate.service.LogService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 记录访问日志
 * TODO 改为使用concurrent service托管的方式
 */
@Slf4j
public class DBLog extends Thread {
    private static DBLog dblog = null;
    private static BlockingQueue<LogInfo> logInfoQueue = new LinkedBlockingQueue<LogInfo>(1024);

    public LogService getLogService() {
        return logService;
    }

    public DBLog setLogService(LogService logService) {
        if (this.logService == null) {
            this.logService = logService;
        }
        return this;
    }

    private LogService logService;

    public static synchronized DBLog getInstance() {
        if (dblog == null) {
            dblog = new DBLog();
        }
        return dblog;
    }

    private DBLog() {
        super("CLogOracleWriterThread");
    }

    public void offerQueue(LogInfo logInfo) {
        try {
            logInfoQueue.offer(logInfo);
        } catch (Exception e) {
            log.error("日志写入失败", e);
        }
    }

    @Override
    public void run() {
        List<LogInfo> bufferedLogList = new ArrayList<>(); // 缓冲队列
        while (true) {
            try {
                bufferedLogList.add(logInfoQueue.take());
                logInfoQueue.drainTo(bufferedLogList);
                if (bufferedLogList.size() > 0) {
                    // 写入日志
                    for (LogInfo log : bufferedLogList) {
                        logService.saveLog(log);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 防止缓冲队列填充数据出现异常时不断刷屏
                try {
                    Thread.sleep(1000);
                } catch (Exception eee) {
                }
            } finally {
                if (bufferedLogList.size() > 0) {
                    try {
                        bufferedLogList.clear();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
}