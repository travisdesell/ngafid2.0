# Contributing to the NGAFID

Currently, there are two primary tracking branches in the ngafid2.0 repository: `stable` and `main`.
`stable` is, unsurprisingly, the stable version of the website that is in production. `main` is the in-development
branch and may contain bugs, as well as incomplete or broken functionality.

The `stable` branch is reserved for more mature changes and critical bug fixes.
Feature branches should generally be based off of `main`. Once merged and tested enough to be considered mature, the
changes can be pulled into stable (this should be done by merging with main up until a certain commit).
Some bugs may exist in both `stable` and `main` -- your patch may be applicable to both branches with a simple rebase,
but if not you may need to implement the fix twice.

# Workflow

The workflow is relatively simple:

1. Pick the parent branch your branch will be based upon, either `main` or `stable`.
2. Create a new branch in the following format: `<parent-branch>/<your-name>/<feature-identifier>`. So for example, if
   I (Josh) wanted to add a `cool-feature` to the `main` branch I would create a branched named `main/josh/cool-feature`
   and base it on `main`.
3. Implement and test your changes. Ensure your code is (1) linted and (2) formatted (see below).
4. Create a pull request. After review your changes will be merged.
5. At some point in time these changes will be considered "mature", at which point they may be merged into `stable` and
   eventually deployed. Deployment on the main server should generally require no additional steps outside of
   recompilation of the java
   source and react modules, or creation of new tables via liquibase.

## Linting

A tool called spotbug works with maven to automatically identify and enumerate code that contains problems of various
kinds. You can run it with:

```
mvn spotbugs:check
```

These concerns should be properly addressed before requesting a review

## Formatting

Our codebase will (eventually) be automatically formatted using a tool called `spotless`.

```angular2html
mvn spotless:check # To search for formatting issues
mvn spotless:apply # To automatically search for and fix formatting issues.
```