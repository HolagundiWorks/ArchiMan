# Built-in demo company

`company/ArchiMan-Demo-Company.archimandb.json` is the fictional Studio Holagundi practice shipped inside the Android app. Schema version is 28. The Practice screen lists the file index and opens it only after confirmation. Opening replaces the practice stored on the phone.

The file is plain JSON. It is not the password-encrypted SQLite package described in older backup notes. Names, registrations, and amounts are fictional.

Release builds do not seed this practice automatically. Debug builds still seed it when the database is empty. Either build can open the file from Practice → Demo.
