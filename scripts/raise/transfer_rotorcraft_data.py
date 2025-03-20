#!/usr/bin/python
from datetime import datetime

import MySQLdb
import os
import os.path
import pysftp


db_password = os.environ["NGAFID_DB_PASSWORD"]
sftp_password = os.environ["RAISE_PASSWORD"]

db = MySQLdb.connect(
    host="127.0.0.1",  # your host, usually localhost
    user="ngafid_user",  # your username
    passwd=db_password,  # your password
    db="ngafid",  # name of the data base
)

today = datetime.today().strftime("%Y-%m-%d")

source_tail_filename = f"./system_ids_to_tails__{today}.csv"
target_tail_filename = f"system_ids_to_tails__{today}.csv"

print(f"system ids to tails source (local) filename is   '{source_tail_filename}'\n")
print(f"system ids to tails target (on sftp) filename is '{target_tail_filename}'\n")

with open(source_tail_filename, "w") as f:
    f.write("#fleet_id, system_id, tail number, confirmed\n")

    cur = db.cursor()
    cur.execute("SELECT fleet_id, system_id, tail, confirmed FROM tails")

    for row in cur.fetchall():
        fleet_id = row[0]
        system_id = row[1]
        tail = row[2]
        confirmed = row[3]

        f.write(f"{fleet_id}, {system_id}, {tail}, {confirmed}\n")

file_size = os.path.getsize(source_tail_filename)

print(f"wrote {file_size} bytes for the system ids to tail file\n")

try:
    with pysftp.Connection(
        host="sftp.rotorcraft.asias.info",
        username="service_ngafid_dto_upload@ngafid.org",
        password=sftp_password,
        log=f"./pysftp_{today}.log",
    ) as sftp:

        result = sftp.put(source_tail_filename)  # Upload the file

        print(f"result from writing source tail file: '{result}'")

        cur = db.cursor()
        cur.execute(
            "SELECT DISTINCT(upload_id) FROM flights WHERE airframe_type_id = (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')"
        )

        archive_base = os.environ["NGAFID_ARCHIVE_DIR"]

        count = 0
        for row in cur.fetchall():
            upload_id = row[0]
            print(f"upload id: {upload_id}")

            upload_cur = db.cursor()
            upload_cur.execute(
                "SELECT uploader_id, fleet_id, filename, sent_to_raise FROM uploads WHERE id = %s",
                (upload_id,),
            )
            for upload_row in upload_cur.fetchall():
                uploader_id = upload_row[0]
                fleet_id = upload_row[1]
                filename = upload_row[2]
                sent_to_raise = int(upload_row[3])
                if sent_to_raise == 1:
                    # don't need to reupload
                    continue

                print(
                    f"\tupload_id: {upload_id}, fleet_id: {fleet_id}, uploader_id: {uploader_id}, filename: '{filename}'"
                )

                source_file = (
                    f"{archive_base}/{fleet_id}/{uploader_id}/{upload_id}__{filename}"
                )
                target_file = (
                    f"/raise-prod-dto-ngafid/{fleet_id}__{upload_id}__{filename}"
                )

                if not os.path.isfile(source_file):
                    print(f"\tERROR: source file: '{source_file}' doest not exist!")
                    exit(1)

                print(f"\tsource file: '{source_file}'")
                print(f"\ttarget file: '{target_file}'")
                print(f"\tsent to raise? {sent_to_raise}")

                try:
                    result = sftp.put(source_file, target_file)
                    print(f"\tresult from sftp.put was: '{result}'")

                except:
                    e = sys.exc_info()
                    print(f"SFTP put raised exception: {e}")
                    exit(1)

                else:
                    # upload completed without exception
                    print("\tupdating upload in database")
                    update_cur = db.cursor()
                    update_cur.execute(
                        "UPDATE uploads SET sent_to_raise = 1, contains_rotorcraft = 1 WHERE id = %s",
                        (upload_id,),
                    )
                    db.commit()

                count += 1
            print("")

except:
    print("Connection failure.")
    e = sys.exc_info()
    print("Exception: {0}".format(e))

db.close()
