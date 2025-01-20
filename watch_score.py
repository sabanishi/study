import sqlite3

dbname = ('hal_repair_not_separate.db')
conn = sqlite3.connect(dbname)
cursor = conn.cursor()

sql = """SELECT * FROM scores"""
cursor.execute(sql)

while True:
    row = cursor.fetchone()
    if row is None:
        break
    hash = row[0]
    score = row[1]
    pattern_sql = """SELECT * FROM patterns WHERE hash = ?"""
    new_cursor = conn.cursor()
    new_cursor.execute(pattern_sql, (hash,))
    pattern = new_cursor.fetchone()
    print(pattern)
    #何か入力があるまで待つ
    input()