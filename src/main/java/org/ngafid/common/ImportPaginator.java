/**
 * Paginator for imports and uploads
 * <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT Computer Science</a>
 */

package org.ngafid.common;

import java.util.*;
import org.ngafid.flights.Upload;
import org.ngafid.Database;
import java.sql.SQLException;

public class ImportPaginator extends Paginator{
    private int fleetID;

    /**
     * Constructor
     * @param bufferSize the size of each page buffer
     * @param fleetID the id of the fleet being accessed
     * @throws SQLException of there is an error in the database
     */
    public ImportPaginator(int bufferSize, int fleetID) throws SQLException{
        super(bufferSize);
        this.fleetID = fleetID;
        super.setNumElements(Upload.getNumUploads(Database.getConnection(), fleetID));
    }

    /**
     * Constructor
     * @param startIndex the page number to start at (almost always 0)
     * @param bufferSize the size of each page buffer
     * @param fleetID the id of the fleet being accessed
     * @throws SQLException of there is an error in the database
     */
    public ImportPaginator(int startIndex, int bufferSize, int fleetID) throws SQLException{
        super(startIndex, bufferSize);
        this.fleetID = fleetID;
        super.setNumElements(Upload.getNumUploads(Database.getConnection(), fleetID));
    }

    /**
     * {inheritDoc}
     */
    public Page currentPage() throws SQLException{
        List<Upload> uploadData = Upload.getUploads(Database.getConnection(), fleetID, new String[]{"IMPORTED", "ERROR"}, super.limitString());
        return new Page(numPages, uploadData, currentIndex);
    }

    /**
     * {inheritDoc}
     */
    public String paginatorType(){
        return "Import paginator";
    }
}
