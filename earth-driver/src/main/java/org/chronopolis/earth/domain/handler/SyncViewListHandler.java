package org.chronopolis.earth.domain.handler;

import com.google.common.collect.ImmutableList;
import org.chronopolis.earth.domain.SyncView;
import org.sql2o.ResultSetHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Return a list of SyncViews from a result set
 *
 * Created by shake on 8/11/16.
 */
@SuppressWarnings("WeakerAccess")
public class SyncViewListHandler extends SyncViewHandler implements ResultSetHandler<List<SyncView>> {

    @Override
    public List<SyncView> handle(ResultSet resultSet) throws SQLException {
        if (resultSet.isClosed()) {
            return ImmutableList.of();
        }

        setupColumns(resultSet);
        ArrayList<SyncView> views = new ArrayList<>();

        SyncView view = new SyncView();
        fillView(view, resultSet);
        view.addHttpDetail(getDetail(resultSet));

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

}
