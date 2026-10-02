**Email Preferences:**

- <span style="text - decoration: underline;">Back-End Setup</span>

&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;1. Create database table with `create_tables.php`

&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;2. Define/Configure Email Types in `EmailTypes.java`

&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;3. Generate Email Types with `generate_email_types.sh`

- <span style="text - decoration: underline;">Front-End Usage</span>
  \
  &ensp;&ensp;**○** Standard Users:
  \
  &ensp;&ensp;&ensp;&ensp;**🞄** Configure their own email preferences inside **Account → My Preferences**
  \
  \
  &ensp;&ensp;**○** Fleet Managers:
  \
  &ensp;&ensp;&ensp;&ensp;**🞄** Configure their users’ preferences in **Account → Manage Fleet**
  \
  &ensp;&ensp;&ensp;&ensp;**🞄** Can configure their own preferences like a standard user
  \
  &ensp;&ensp;&ensp;&ensp;**🞄** Can mass-toggle their users’ preferences with the big checkboxes (☑️ / ⬜)
  \
  \
  &ensp;&ensp;**○** General:
  \
  &ensp;&ensp;&ensp;&ensp;**🞄** Some Email Types are only visible to _admin_ accounts
  \
  &ensp;&ensp;&ensp;&ensp;**🞄** Some Email Types are hidden to _all_ accounts
