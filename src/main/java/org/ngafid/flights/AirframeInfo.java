package org.ngafid.flights;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.sql.Array;
import java.util.List;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.ngafid.routes.ErrorResponse;

public class AirframeInfo {
    public final String systemId;
    public final int fleetId;
    public final String tail;
    public final boolean confirmed;
    public final String airframeName;
    private static final Logger LOG = Logger.getLogger(AirframeInfo.class.getName());

    /**
     * Create a tail object from a resultSet from the database
     *
     * @param resultSet a row from the tails database table
     */
    public AirframeInfo(ResultSet resultSet) throws SQLException {
        systemId = resultSet.getString(1);
        fleetId = resultSet.getInt(2);
        tail = resultSet.getString(3);
        confirmed = resultSet.getBoolean(4);
        airframeName = resultSet.getString(5);
    }

    public String toString() {
        return "Tail " + tail + ", sys. id: " + systemId;
    }

    public static String createStatement( String[] data ) {
        String result = "SELECT * FROM airframe_info WHERE system_id IN (";
        for (int i=0;i<data.length;i++){
            result += "'"+data[i]+"'";
            if(i!=data.length -1){
                result+=",";
            }
        }
        result+=") ORDER BY system_id";
        return result;
    }

    public static ArrayList<AirframeInfo> getAll(Connection connection, String[] systemId) throws SQLException {
        

        ArrayList<AirframeInfo> AirframeInfos = new ArrayList<>();
        LOG.info("Entered getAll");
        String queryString =  createStatement(systemId);
        PreparedStatement query = connection.prepareStatement(queryString);
       
        LOG.info("systemId "+systemId);
        //query.s(1, systemId);
        LOG.info(query.toString());
        //LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();
        
        while (resultSet.next()) {
            //tail existed in the database, return the id
            LOG.info("Entered While");
            AirframeInfos.add(new AirframeInfo(resultSet));
        }
        resultSet.close();
        query.close();

        return AirframeInfos;
    }
}
