This is an implementation for a client to the Balustrade API for DPN. 

Build it with maven. Run jar, run.

### Development

Maven and Spring provide the necessary resources to run this in development. By using `mvn spring-boot:run`, 
the main class will be executed and the service will run as configured (see: configuration). Triggers can be 
run for starting synchronization and replication jobs, with syncs attempting each node and replications
requiring a specific id.

### Configuration

EarthDriver attempts to read from an `application.yml` configuration file. This can be set with spring's launch
options if you wish to use a different name or location. A sample application.yml is provided when installing
via rpm.

#### Configuration Options

* `earth.name`: The name of the local node namespace
* `earth.stage`: The staging area to rsync bags to
* `earth.disableSNI`: Boolean flag for disabling SSL Certification validation (not recommened)
* `earth.cron.replicate`: The cron trigger for issuing querying active replications
* `earth.cron.sync`: The cron trigger for synchronizing nodes
* `earth.cron.ingest`: Chronopolis specific ingest configuration
* `earth.cron.dpn.local.auth-key`: Authentication token for the local node's dpn api
* `earth.cron.dpn.local.api-root`: The http endpoint of the local node's dpn api
* `earth.cron.dpn.local.name`: The name of the local node namespace
* `earth.cron.dpn.remote`: A list of remote dpn nodes to connect to
* `earth.cron.dpn.remote.auth-key`: Authentication token for the remote node's dpn api
* `earth.cron.dpn.remote.api-root`: The http endpoint of the remote node's dpn api
* `earth.cron.dpn.remote.name`: The name of the remote node namespace

* `spring.profiles.active`: Active profiles (possible: production, cli, sync)
* `logging.file`: The log file to write to

