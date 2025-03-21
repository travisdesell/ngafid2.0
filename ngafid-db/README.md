# NGAFID Database

This module contains the SQL scripts to generate the database. The scripts are formatted to be read by a tool called
liquibase. Maven will take care of installing and running this, however if you plan to modify the database in any
meaningful way you should read about liquibase here: https://docs.liquibase.com/concepts/introduction-to-liquibase.html

Liquibase allows us to version our database system and incrementally change it. Our table definitions will thus not
always be complete as they may be modified piece-wise. The best practices documentation on the liquibase site will
show you how to address this if it gets out of hand.