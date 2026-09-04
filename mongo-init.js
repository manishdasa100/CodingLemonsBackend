// Runs once, only when /data/db is empty. Creates the application user that
// application-dev.properties authenticates as. The root user (mongoadmin) is
// created by the entrypoint from MONGO_INITDB_ROOT_* before this runs.
//
// Keep the credentials here in sync with:
//   app/src/main/resources/application-dev.properties
//   app/src/main/resources/application-demo.properties
db.getSiblingDB("coding_lemon_dev").createUser({
  user: "cl_backend_dev",
  pwd: "backendPass",
  roles: [{ role: "readWrite", db: "coding_lemon_dev" }]
});
