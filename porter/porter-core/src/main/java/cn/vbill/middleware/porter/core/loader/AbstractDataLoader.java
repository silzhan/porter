/*
 * Copyright ©2018 vbill.cn.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package cn.vbill.middleware.porter.core.loader;

import cn.vbill.middleware.porter.common.client.LoadClient;
import cn.vbill.middleware.porter.common.client.MetaQueryClient;
import cn.vbill.middleware.porter.common.db.meta.TableSchema;
import cn.vbill.middleware.porter.core.event.etl.ETLRow;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * @author: zhangkewei[zhang_kw@suixingpay.com]
 * @date: 2018年02月09日 15:08
 * @version: V1.0
 * @review: zhangkewei[zhang_kw@suixingpay.com]/2018年02月09日 15:08
 */
public abstract class AbstractDataLoader implements DataLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDataLoader.class);
    protected static final String TIME_TAKEN_FORMAT = "yyyy-MM-dd HH:mm:ss.S";
    private volatile LoadClient loadClient;
    private volatile MetaQueryClient metaQueryClient;
    //更新转插入策略开关
    private volatile boolean insertOnUpdateError = true;

    /**
     * 获取PluginName
     *
     * @date 2018/8/8 下午5:59
     * @param: []
     * @return: java.lang.String
     */
    protected abstract String getPluginName();

    @Override
    public void setLoadClient(LoadClient loadClient) {
        this.loadClient = loadClient;
    }

    /**
     * 获取LoadClient
     *
     * @date 2018/8/8 下午5:59
     * @param: []
     * @return: T
     */
    public <T> T getLoadClient() {
        return (T) loadClient;
    }

    @Override
    public void setMetaQueryClient(MetaQueryClient metaQueryClient) {
        this.metaQueryClient = metaQueryClient;
    }

    @Override
    public boolean isMatch(String loaderName) {
        return getPluginName().equals(loaderName);
    }

    @Override
    public void shutdown() throws Exception {
        if (!loadClient.isPublic()) {
            loadClient.shutdown();
        }
        if (!metaQueryClient.isPublic()) {
            metaQueryClient.shutdown();
        }
    }

    @Override
    public void startup() throws Exception {
        loadClient.start();
        metaQueryClient.start();
    }

    @Override
    public int getDataCount(String schema, String table, String updateDateColumn, Date startTime, Date endTime) {
        return metaQueryClient.getDataCount(schema, table, updateDateColumn, startTime, endTime);
    }

    @Override
    public TableSchema findTable(String finalSchema, String finalTable) throws Exception {
        return metaQueryClient.getTable(finalSchema, finalTable);
    }

    public boolean isInsertOnUpdateError() {
        return insertOnUpdateError;
    }

    public void setInsertOnUpdateError(boolean insertOnUpdateError) {
        this.insertOnUpdateError = insertOnUpdateError;
    }

    @Override
    public String getClientInfo() {
        StringBuffer clientInfo = new StringBuffer();
        if (null != metaQueryClient && metaQueryClient != loadClient) {
            clientInfo.append("元数据->").append(metaQueryClient.getClientInfo()).append(System.lineSeparator()).append("\t");
        }
        clientInfo.append("载入源->").append(loadClient.getClientInfo());
        return clientInfo.toString();
    }

    /**
     * 输出TimeTaken
     *
     * @date 2018/8/8 下午6:00
     * @param: [row]
     * @return: void
     */
    protected void printTimeTaken(ETLRow row) {
        try {
            LOGGER.info("消息处理耗时->trail操作:{},存储kafka:{},kafka消费:{},数据库载入:{}",
                    DateFormatUtils.format(row.getOpTime(), TIME_TAKEN_FORMAT),
                    DateFormatUtils.format(row.getConsumerTime(), TIME_TAKEN_FORMAT),
                    DateFormatUtils.format(row.getConsumedTime(), TIME_TAKEN_FORMAT),
                    DateFormatUtils.format(System.currentTimeMillis(), TIME_TAKEN_FORMAT));
        } catch (Throwable e) {
            LOGGER.error("输出消息输出耗时出错", e);
        }
    }
}
