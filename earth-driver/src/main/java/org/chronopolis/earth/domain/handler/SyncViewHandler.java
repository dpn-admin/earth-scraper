package org.chronopolis.earth.domain.handler;

import org.chronopolis.earth.domain.HttpDetail;
import org.chronopolis.earth.domain.SyncStatus;
import org.chronopolis.earth.domain.SyncType;
import org.chronopolis.earth.domain.SyncView;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @deprecated Will be removed by 2.0.0-RELEASE
 * Base for our SyncView handlers. Populates the column ids, creates SyncViews and HttpDetails.
 *
 * Created by shake on 8/11/16.
 */
@Deprecated
public class SyncViewHandler {
    int joinCol;
    private int idCol;
    private int urlCol;
    private int hostCol;
    private int typeCol;
    private int statusCol;
    private int reqBodyCol;
    private int resBodyCol;
    private int resCodeCol;
    private int reqMethodCol;

    void fillView(SyncView view, ResultSet resultSet) throws SQLException {
        long syncId = resultSet.getLong(idCol);
        String host = resultSet.getString(hostCol);
        String status = resultSet.getString(statusCol);
        String type = resultSet.getString(typeCol);
        view.setId(syncId)
                .setHost(host)
                .setStatus(SyncStatus.valueOf(status))
                .setType(SyncType.valueOf(type));
    }

    void setupColumns(ResultSet resultSet) throws SQLException {
        idCol = resultSet.findColumn("sync_id");
        hostCol = resultSet.findColumn("host");
        statusCol = resultSet.findColumn("status");
        typeCol = resultSet.findColumn("type");
        urlCol = resultSet.findColumn("url");
        reqBodyCol = resultSet.findColumn("request_body");
        reqMethodCol = resultSet.findColumn("request_method");
        resBodyCol = resultSet.findColumn("response_body");
        resCodeCol = resultSet.findColumn("response_code");
        joinCol = resultSet.findColumn("sync");
    }

    HttpDetail getDetail(ResultSet resultSet) throws SQLException {
        HttpDetail detail = new HttpDetail();
        detail.setUrl(resultSet.getString(urlCol));
        detail.setRequestBody(resultSet.getString(reqBodyCol));
        detail.setResponseBody(resultSet.getString(resBodyCol));
        detail.setResponseCode(resultSet.getInt(resCodeCol));
        detail.setRequestMethod(resultSet.getString(reqMethodCol));
        return detail;
    }

}
