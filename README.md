# jQAssistant Git Plugin

This is a pluign for jQAssistant that scans git repositories. This is the continuation of the project previously maintained at [github.com/kontext-e/jqassistant-plugins](https://github.com/kontext-e/jqassistant-plugins).

For a documentation of this plugin see [git.adoc](src/main/asciidoc/git.adoc)

## How to install
### jQAssistant 1.12+

Use the jqassistant.yaml as described [here](https://jqassistant.github.io/jqassistant/current/) and add the following 3 lines in the plugins section:

```
    - group-id: de.kontext-e.jqassistant.plugin
      artifact-id: jqassistant.plugin.git
      version: 2.1.0
```

### Prior to jQAssistant 1.12

Download the desired version from maven (e.g. [here](https://mvnrepository.com/artifact/de.kontext-e.jqassistant.plugin/jqassistant.plugin.git)) and put the jar in the jqassistant plugins directory.

## Changelog

- Version 2.2.:
  - If a repository has already been scanned and a range is given that ends in a branch name (or "head" or ends with ".."), the plugin will automatically find the most recent commit in the neo4J database and continue scanning from there. A starting range must still be given, but it will be overwritten.
  - If no range is given, the plugin will now append commits-nodes to an existing graph structure from previous scans, but will still iterate over all commits of the project.

## Known Issues:

- Any sort of deletion of branches/squashing of commits is not yet supported. In such a case, the repository has to be rescanned completely.
- The "range" property (see [git.adoc](src/main/asciidoc/git.adoc)) only supports "two dot"-notation for now.