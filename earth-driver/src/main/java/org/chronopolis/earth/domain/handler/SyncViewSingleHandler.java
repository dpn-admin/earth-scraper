package org.chronopolis.earth.domain.handler;

import org.chronopolis.earth.domain.SyncView;
import org.sql2o.ResultSetHandler;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Return a single SyncView from a result set
 *
 * Created by shake on 8/15/16.
 */
public class SyncViewSingleHandler extends SyncViewHandler implements ResultSetHandler<SyncView> {

    @Override
    public SyncView handle(ResultSet resultSet) throws SQLException {
        if (resultSet.isClosed()) {
            return null;
        }

        setupColumns(resultSet);
        SyncView view = new SyncView();

        fillView(view, resultSet);
        view.addHttpDetail(getDetail(resultSet));

        while(resultSet.next()) {
            view.addHttpDetail(getDetail(resultSet));
        }

        resultSet.close();
        return view;
    }

}
