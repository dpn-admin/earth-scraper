package org.chronopolis.earth.domain.handler;

import org.chronopolis.earth.domain.SyncView;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @deprecated Will be removed by 2.0.0-RELEASE
 * Return a single SyncView from a result set
 *
 * Created by shake on 8/15/16.
 */
@Deprecated
public class SyncViewSingleHandler extends SyncViewHandler {

    // @Override
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
