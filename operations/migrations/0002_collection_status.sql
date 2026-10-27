CREATE TABLE collection_status (source TEXT PRIMARY KEY CHECK(source IN ('activity','health','website')), received_at INTEGER NOT NULL);
