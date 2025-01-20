import sqlite3

dbname = ('hal_repair_not_check.db')
conn = sqlite3.connect(dbname)
cursor = conn.cursor()

sql = """ALTER TABLE patterns ADD COLUMN is_child_useful BIT DEFAULT 0"""
cursor.execute(sql)