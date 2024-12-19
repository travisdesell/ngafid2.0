#!/usr/bin/python
from datetime import datetime

import MySQLdb
import os
import os.path
import pysftp


db_password = os.environ["NGAFID_DB_PASSWORD"]


db = MySQLdb.connect(
    host="127.0.0.1",  # your host, usually localhost
    user="ngafid_user",  # your username
    passwd=db_password,  # your password
    db="ngafid",  # name of the data base
)

print(f"year, n_tails, n_flights")
for year in range(2018, datetime.now().year + 1):
    cur = db.cursor()
    cur.execute(
        "SELECT COUNT(DISTINCT(system_id)) FROM flights WHERE (SELECT id FROM uploads WHERE flights.upload_id = uploads.id AND uploads.sent_to_raise = 1 and YEAR(uploads.end_time) = %s)",
        (year,),
    )

    n_tails = None
    for row in cur.fetchall():
        n_tails = row[0]

    cur = db.cursor()
    cur.execute(
        """SELECT count(id) FROM flights WHERE 
            (SELECT id FROM uploads WHERE flights.upload_id = uploads.id AND uploads.sent_to_raise = 1)
            AND YEAR(flights.end_time) = %s 
            """,
        (year,),
    )
    # cur.execute("SELECT SUM(n_valid_flights + n_warning_flights + n_error_flights) FROM uploads WHERE sent_to_raise = 1 AND YEAR(end_time) = %s", (year,));

    n_flights = None
    for row in cur.fetchall():
        n_flights = row[0]

    print(f"{year}, {n_tails}, {n_flights}")
