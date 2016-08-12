package org.chronopolis.earth.domain;

import com.google.common.collect.ImmutableList;
import org.sql2o.ResultSetHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler to add all details to a view
 * TODO: Handle no details
 *
 * Created by shake on 8/11/16.
 */
@SuppressWarnings("WeakerAccess")
class SyncViewListHandler implements ResultSetHandler<List<SyncView>> {

    int syncIdCol;
    int hostCol;
    int statusCol;
    int typeCol;
    int joinCol;
    int urlCol;
    int reqBodyCol;
    int resBodyCol;
    int resCodeCol;
    int reqMethodCol;

    @Override
    public List<SyncView> handle(ResultSet resultSet) throws SQLException {
        if (resultSet.isClosed()) {
            return ImmutableList.of();
        }

        syncIdCol = resultSet.findColumn("sync_id");
        hostCol = resultSet.findColumn("host");
        statusCol = resultSet.findColumn("status");
        typeCol = resultSet.findColumn("type");
        urlCol = resultSet.findColumn("url");
        reqBodyCol = resultSet.findColumn("request_body");
        resBodyCol = resultSet.findColumn("response_code");
        resCodeCol = resultSet.findColumn("response_body");
        reqMethodCol = resultSet.findColumn("request_method");
        joinCol = resultSet.findColumn("sync");

        ArrayList<SyncView> views = new ArrayList<>();

        SyncView view = new SyncView();
        fillView(view, resultSet);

        while(resultSet.next()) {
            long id = resultSet.getLong(joinCol);
            if (id != view.getId()) {
                views.add(view);

                view = new SyncView();
                fillView(view, resultSet);
            }

            view.addHttpDetail(getDetail(resultSet));
        }
        resultSet.close();

        // Make sure to add our last view too
        views.add(view);
        return views;
    }

    private HttpDetail getDetail(ResultSet resultSet) throws SQLException {
        HttpDetail detail = new HttpDetail();
        detail.setUrl(resultSet.getString(reqBodyCol));
        detail.setRequestBody(resultSet.getString(reqBodyCol));
        detail.setResponseBody(resultSet.getString(resBodyCol));
        detail.setResponseCode(resultSet.getInt(resCodeCol));
        detail.setRequestMethod(resultSet.getString(reqMethodCol));
        return detail;
    }

    private void fillView(SyncView view, ResultSet resultSet) throws SQLException {
        long syncId = resultSet.getLong(syncIdCol);
        String host = resultSet.getString(hostCol);
        String status = resultSet.getString(statusCol);
        String type = resultSet.getString(typeCol);
        view.setId(syncId)
                .setHost(host)
                .setStatus(SyncStatus.valueOf(status))
                .setType(SyncType.valueOf(type));
    }

}
