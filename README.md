
# uknw-auth-checker-api

This is the API microservice that provides service to CSPs to check that trader holds NOP waiver.

## Running the service

> `sbt run`

The service runs on port `9070` by default.

## Running dependencies

Using [service manager](https://github.com/hmrc/service-manager)
with the service manager profile `UKNW_AUTH_CHECKER_API` will start
the UKNW auth checker API.

> `sm --start UKNW_AUTH_CHECKER_API`

## Running tests

### Unit tests

> `sbt test`

### Integration tests

> `sbt it:test`


### All tests

This is a sbt command alias specific to this project. It will run a scala format
check, run a scala style check, run unit tests, run integration tests and produce a coverage report.
> `sbt runAllChecks`

> ### Pre-Commit

This is a sbt command alias specific to this project. It will run a scala format , run a scala fix, 
run unit tests, run integration tests and produce a coverage report.
> `sbt runAllChecks`

### Format all

This is a sbt command alias specific to this project. It will run a scala format
check in the app, tests, and integration tests
> `sbt fmtAll`

### Fix all

This is a sbt command alias specific to this project. It will run the scala fix 
linter/reformatter in the app, tests, and integration tests
> `sbt fixAll`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

### Requesting data from the Stub API

To request data from the stub API please use the `.bru` files that can be found in `.bruno` and do not change any of those files as the stub API returns an error `500` on any unexpected requests.

To add a new expected request please refer to the [uknw-auth-checker-api-stub
/README.md](https://github.com/hmrc/uknw-auth-checker-api-stub?tab=readme-ov-file#add-new-expected-requests)