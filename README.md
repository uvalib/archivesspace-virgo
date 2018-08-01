A utility to generate solr index records and marc records (for the purpose of circulation) for materials described in ArchivesSpace.

# Configure the application
```cp config.properties.example config.properties ```

# Build the application
```mvn clean install dependency:copy-dependencies```

# Run the application
```java -cp target/as-to-virgo-1.0-SNAPSHOT.jar:target/dependency/* edu.virginia.lib.indexing.tools.IndexRecords```
